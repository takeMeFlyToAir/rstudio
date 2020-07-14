/*
 * ServerPAMAuth.cpp
 *
 * Copyright (C) 2009-19 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include "ServerPAMAuth.hpp"

#include <shared_core/Error.hpp>
#include <core/PeriodicCommand.hpp>
#include <core/Thread.hpp>
#include <core/system/Process.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>

#include <core/http/Cookie.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <core/http/CSRFToken.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <core/text/TemplateFilter.hpp>

#include <monitor/MonitorClient.hpp>

#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerSecureUriHandler.hpp>
#include <server/auth/ServerAuthHandler.hpp>

#include <server/ServerConstants.hpp>
#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>
#include <server/ServerSessionProxy.hpp>

namespace rstudio {
namespace server {
namespace pam_auth {

using namespace rstudio::core;

bool canSetSignInCookies();
bool canStaySignedIn();
void onUserAuthenticated(const std::string& username,
                         const std::string& password);
void onUserUnauthenticated(const std::string& username,
                           bool signedOut = false);

namespace {

void assumeRootPriv()
{
    // RedHat 5 returns PAM_SYSTEM_ERR from pam_authenticate if we're
    // running with geteuid != getuid (as is the case when we temporarily
    // drop privileges). We've also seen kerberos on Ubuntu require
    // priv to work correctly -- so, restore privilliges in the child
    if (core::system::realUserIsRoot())
    {
       Error error = core::system::restorePriv();
       if (error)
       {
          LOG_ERROR(error);
          // intentionally fail forward (see note above)
       }
    }
}

// It's important that URIs be in the root directory, so the cookie
// gets set/unset at the correct scope!
const char * const kDoSignIn = "/auth-do-sign-in";
const char * const kPublicKey = "/auth-public-key";

const char * const kAppUri = "appUri";

const char * const kErrorParam = "error";
const char * const kErrorDisplay = "errorDisplay";
const char * const kErrorMessage = "errorMessage";

const char * const kFormAction = "formAction";

const char * const kStaySignedInDisplay = "staySignedInDisplay";

const char * const kAuthTimeoutMinutes = "authTimeoutMinutes";
const char * const kAuthTimeoutMinutesDisplay = "authTimeoutMinutesDisplay";

const char * const kLoginPageHtml = "loginPageHtml";

enum ErrorType 
{
   kErrorNone,
   kErrorInvalidLogin,
   kErrorServer,
   kErrorUserLicenseLimitReached,
   kErrorUserLicenseSystemUnavailable
};

std::string errorMessage(ErrorType error)
{
   switch (error)
   {
      case kErrorNone:
         return "";
      case kErrorInvalidLogin: 
         return "Incorrect or invalid username/password";
      case kErrorServer:
         return "Temporary server error, please try again";
      case kErrorUserLicenseLimitReached:
         return "The user limit for this license has been reached, or you are not allowed access.";
      case kErrorUserLicenseSystemUnavailable:
         return "The user licensing system is temporarily unavailable. Please try again later.";
   }
   return "";
}

std::string applicationURL(const http::Request& request,
                           const std::string& path = std::string())
{
   return http::URL::uncomplete(
         request.uri(),
         path);
}

std::string applicationSignInURL(const http::Request& request,
                                 const std::string& appUri,
                                 ErrorType error = kErrorNone)
{
   // build fields
   http::Fields fields ;
   if (appUri != "/")
      fields.push_back(std::make_pair(kAppUri, appUri));
   if (error != kErrorNone)
     fields.push_back(std::make_pair(kErrorParam, 
                                     safe_convert::numberToString(error)));

   // build query string
   std::string queryString ;
   if (!fields.empty())
     http::util::buildQueryString(fields, &queryString);

   // generate url
   std::string signInURL = applicationURL(request, auth::handler::kSignIn);
   if (!queryString.empty())
     signInURL += ("?" + queryString);
   return signInURL;
}

std::string getUserIdentifier(const core::http::Request& request)
{
   if (server::options().authNone())
      return core::system::username();
   else
      return core::http::secure_cookie::readSecureCookie(request, kUserIdCookie);
}

std::string userIdentifierToLocalUsername(const std::string& userIdentifier)
{
   static core::thread::ThreadsafeMap<std::string, std::string> cache;
   std::string username = userIdentifier;

   if (cache.contains(userIdentifier)) 
   {
      username = cache.get(userIdentifier);
   }
   else
   {
      // The username returned from this function is eventually used to create
      // a local stream path, so it's important that it agree with the system
      // view of the username (as that's what the session uses to form the
      // stream path), which is why we do a username => username transform
      // here. See case 5413 for details.
      core::system::User user;
      Error error = core::system::User::getUserFromIdentifier(userIdentifier, user);
      if (error)
      {
         // log the error and return the PAM user identifier as a fallback
         LOG_ERROR(error);
      }
      else
      {
         username = user.getUsername();
      }

      // cache the username -- we do this even if the lookup fails since
      // otherwise we're likely to keep hitting (and logging) the error on
      // every request
      cache.set(userIdentifier, username);
   }

   return username;
}

bool mainPageFilter(const http::Request& request,
                    http::Response* pResponse)
{
   // check for user identity, if we have one then allow the request to proceed
   std::string userIdentifier = getUserIdentifier(request);
   if (userIdentifier.empty())
   {
      // otherwise redirect to sign-in
      pResponse->setMovedTemporarily(request, applicationSignInURL(request, request.uri()));
      return false;
   }
   else
   {
      return true;
   }
}

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse)
{
   pResponse->setMovedTemporarily(request, applicationSignInURL(request, request.uri()));
}

void refreshCredentialsThenContinue(
            boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // no silent refresh possible so delegate to sign-in and continue
   signInThenContinue(pConnection->request(),
                      &(pConnection->response()));

   // write response
   pConnection->writeResponse();
}

void signIn(const http::Request& request,
            http::Response* pResponse)
{
   core::http::secure_cookie::remove(request,
      kUserIdCookie,
      "/",
      pResponse,
      boost::algorithm::starts_with(request.absoluteUri(), "https"));

   std::map<std::string,std::string> variables;
   variables["action"] = applicationURL(request, kDoSignIn);
   variables["publicKeyUrl"] = applicationURL(request, kPublicKey);
   
   // setup template variables
   std::string error = request.queryParamValue(kErrorParam);
   variables[kErrorMessage] = errorMessage(static_cast<ErrorType>(
            safe_convert::stringTo<unsigned>(error, kErrorNone)));
   variables[kErrorDisplay] = error.empty() ? "none" : "block";
   variables[kStaySignedInDisplay] = canStaySignedIn() ? "block" : "none";
   int timeoutMinutes = server::options().authTimeoutMinutes();
   variables[kAuthTimeoutMinutesDisplay] = timeoutMinutes > 0 ? "block" : "none";
   variables[kAuthTimeoutMinutes] = safe_convert::numberToString(timeoutMinutes);
   if (server::options().authEncryptPassword())
      variables[kFormAction] = "action=\"javascript:void\" "
                               "onsubmit=\"submitRealForm();return false\"";
   else
      variables[kFormAction] = "action=\"" + variables["action"] + "\" "
                               "onsubmit=\"return verifyMe()\"";


   variables[kAppUri] = request.queryParamValue(kAppUri);

   // include custom login page html
   variables[kLoginPageHtml] = server::options().authLoginPageHtml();

   // get the path to the JS file
   Options& options = server::options();
   FilePath wwwPath(options.wwwLocalPath());
   FilePath signInPath = wwwPath.completePath("templates/encrypted-sign-in.htm");

   text::TemplateFilter filter(variables);

   // don't allow sign-in page to be framed by other domains (clickjacking
   // defense)
   pResponse->setFrameOptionHeaders(options.wwwFrameOrigin());

   pResponse->setFile(signInPath, request, filter);
   pResponse->setContentType("text/html");
}

void publicKey(const http::Request&,
               http::Response* pResponse)
{
   std::string exp, mod;
   core::system::crypto::rsaPublicKey(&exp, &mod);
   pResponse->setNoCacheHeaders();
   pResponse->setBody(exp + ":" + mod);
   pResponse->setContentType("text/plain");
}

void setSignInCookies(const core::http::Request& request,
                      const std::string& username,
                      bool persist,
                      core::http::Response* pResponse,
                      bool reuseCsrf = false)
{
   std::string csrfToken = reuseCsrf ? request.cookieValue(kCSRFTokenCookie) : std::string();

   int staySignedInDays = server::options().authStaySignedInDays();
   int authTimeoutMinutes = server::options().authTimeoutMinutes();

   bool secureCookie = options().authCookiesForceSecure() ||
                       options().getOverlayOption("ssl-enabled") == "1" ||
                       boost::algorithm::starts_with(request.absoluteUri(), "https");

   if (authTimeoutMinutes == 0)
   {
      // legacy auth expiration - users do not idle
      // and stay signed in for multiple days
      // not very secure, but maintained for those users that want this
      boost::optional<boost::gregorian::days> expiry;
      if (persist && canStaySignedIn())
         expiry = boost::gregorian::days(staySignedInDays);
      else
         expiry = boost::none;

      core::http::secure_cookie::set(kUserIdCookie,
                                     username,
                                     request,
                                     boost::posix_time::time_duration(24*staySignedInDays,
                                                                      0,
                                                                      0,
                                                                      0),
                                     expiry,
                                     "/",
                                     pResponse,
                                     secureCookie);

      // set a cookie that is tied to the specific user list we have written
      // if the user list ever has conflicting changes (e.g. a user is locked),
      // the user will be forced to sign back in
      core::http::secure_cookie::set(kUserListCookie,
                                     auth::handler::overlay::getUserListCookieValue(),
                                     request,
                                     boost::posix_time::time_duration(24*staySignedInDays,
                                                                      0,
                                                                      0,
                                                                      0),
                                     expiry,
                                     "/",
                                     pResponse,
                                     secureCookie);

      // add cross site request forgery detection cookie
      core::http::setCSRFTokenCookie(request, expiry, csrfToken, secureCookie, pResponse);
   }
   else
   {
      // new auth expiration - users are forced to sign in
      // after being idle for authTimeoutMinutes amount
      boost::optional<boost::posix_time::time_duration> expiry;
      if (persist)
         expiry = boost::posix_time::minutes(authTimeoutMinutes);
      else
         expiry = boost::none;

      // set the secure user id cookie
      core::http::secure_cookie::set(kUserIdCookie,
                                     username,
                                     request,
                                     boost::posix_time::minutes(authTimeoutMinutes),
                                     expiry,
                                     "/",
                                     pResponse,
                                     secureCookie);

      // set a cookie that is tied to the specific user list we have written
      // if the user list ever has conflicting changes (e.g. a user is locked),
      // the user will be forced to sign back in
      core::http::secure_cookie::set(kUserListCookie,
                                     auth::handler::overlay::getUserListCookieValue(),
                                     request,
                                     boost::posix_time::minutes(authTimeoutMinutes),
                                     expiry,
                                     "/",
                                     pResponse,
                                     secureCookie);

      // set a cookie indicating whether or not we should persist the auth cookie
      // when it is automatically refreshed
      core::http::Cookie persistCookie(request,
                                       kPersistAuthCookie,
                                       persist ? "1" : "0",
                                       "/",
                                       true,
                                       secureCookie);
      persistCookie.setExpires(boost::posix_time::minutes(authTimeoutMinutes));
      pResponse->addCookie(persistCookie);

      core::http::setCSRFTokenCookie(request, expiry, csrfToken, secureCookie, pResponse);
   }
}

void doSignIn(const http::Request& request,
              http::Response* pResponse)
{
   std::string appUri = request.formFieldValue(kAppUri);
   if (appUri.empty())
      appUri = "/";

   bool persist = false;
   std::string username, password;

   if (server::options().authEncryptPassword())
   {
      std::string encryptedValue = request.formFieldValue("v");
      std::string plainText;
      Error error = core::system::crypto::rsaPrivateDecrypt(encryptedValue,
                                                            &plainText);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setMovedTemporarily(
               request,
               applicationSignInURL(request,
                                    appUri,
                                    kErrorServer));
         return;
      }

      size_t splitAt = plainText.find('\n');
      if (splitAt == std::string::npos)
      {
         LOG_ERROR_MESSAGE("Didn't find newline in plaintext");
         pResponse->setMovedTemporarily(
               request,
               applicationSignInURL(request,
                                    appUri,
                                    kErrorServer));
         return;
      }

      persist = request.formFieldValue("persist") == "1";
      username = plainText.substr(0, splitAt);
      password = plainText.substr(splitAt + 1, plainText.size());
   }
   else
   {
      persist = request.formFieldValue("staySignedIn") == "1";
      username = request.formFieldValue("username");
      password = request.formFieldValue("password");
   }

   // tranform to local username
   username = auth::handler::userIdentifierToLocalUsername(username);

   onUserUnauthenticated(username);

   // ensure user is not throttled from logging in
   if (auth::handler::isUserSignInThrottled(username))
   {
      pResponse->setMovedTemporarily(
            request,
            applicationSignInURL(request,
                                 appUri,
                                 kErrorServer));
      return;
   }

   if ( pamLogin(username, password) && server::auth::validateUser(username))
   {
      // ensure user is licensed to use the product
      bool isLicensed = false;
      Error error = auth::handler::overlay::isUserLicensed(username, &isLicensed);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setMovedTemporarily(
               request,
               applicationSignInURL(request,
                                    appUri,
                                    kErrorUserLicenseSystemUnavailable));
         return;
      }
      else
      {
         if (!isLicensed)
         {
            pResponse->setMovedTemporarily(
                  request,
                  applicationSignInURL(request,
                                       appUri,
                                       kErrorUserLicenseLimitReached));
            return;
         }
      }

      if (appUri.size() > 0 && appUri[0] != '/')
         appUri = "/" + appUri;

      setSignInCookies(request, username, persist, pResponse);
      pResponse->setMovedTemporarily(request, appUri);

      // register login with monitor
      using namespace monitor;
      client().logEvent(Event(kAuthScope,
                              kAuthLoginEvent,
                              "",
                              username));

      onUserAuthenticated(username, password);
   }
   else
   {
      // register failed login with monitor
      using namespace monitor;
      client().logEvent(Event(kAuthScope,
                              kAuthLoginFailedEvent,
                              "",
                              username));

      pResponse->setMovedTemporarily(
            request,
            applicationSignInURL(request,
                                 appUri,
                                 kErrorInvalidLogin));
   }
}

void signOut(const http::Request& request,
             http::Response* pResponse)
{
   // validate sign-out request
   if (!core::http::validateCSRFForm(request, pResponse))
      return;

   // register logout with monitor if we have the username
   std::string userIdentifier = getUserIdentifier(request);
   if (!userIdentifier.empty())
   {
      std::string username = userIdentifierToLocalUsername(userIdentifier);

      using namespace monitor;
      client().logEvent(Event(kAuthScope,
                              kAuthLogoutEvent,
                              "",
                              username));

      onUserUnauthenticated(username, true);
   }

   // instruct browser to clear the user's auth cookies
   core::http::secure_cookie::remove(request,
      kUserIdCookie,
      "/",
      pResponse,
      boost::algorithm::starts_with(request.absoluteUri(), "https"));

   if (options().authTimeoutMinutes() > 0)
   {
      core::http::secure_cookie::remove(request,
                                        kPersistAuthCookie,
                                        "/",
                                        pResponse,
                                        boost::algorithm::starts_with(request.absoluteUri(), "https"));
   }

   // invalidate the auth cookie so that it can no longer be used
   auth::handler::invalidateAuthCookie(request.cookieValue(kUserIdCookie));

   pResponse->setMovedTemporarily(request, auth::handler::kSignIn);
}

} // anonymous namespace


bool pamLogin(const std::string& username, const std::string& password)
{
   // get path to pam helper
   FilePath pamHelperPath(server::options().authPamHelperPath());
   if (!pamHelperPath.exists())
   {
      LOG_ERROR_MESSAGE("PAM helper binary does not exist at " +
                           pamHelperPath.getAbsolutePath());
      return false;
   }

   // form args
   std::vector<std::string> args;
   args.push_back(username);
   args.push_back("rstudio");
   args.push_back(server::options().authPamRequirePasswordPrompt() ? "1" : "0");

   // don't try to login with an empty password (this hangs PAM as it waits for input)
   if (password.empty())
   {
      LOG_WARNING_MESSAGE("No PAM password provided for user '" + username + "'; refusing login");
      return false;
   }

   // options (assume priv after fork)
   core::system::ProcessOptions options;
   options.onAfterFork = assumeRootPriv;

   // run pam helper
   core::system::ProcessResult result;
   Error error = core::system::runProgram(
      pamHelperPath.getAbsolutePath(),
      args,
      password,
      options,
      &result);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // check for success
   return result.exitStatus == 0;
}

Error initialize()
{
   // register ourselves as the auth handler
   server::auth::handler::Handler pamHandler;
   pamHandler.getUserIdentifier = boost::bind(getUserIdentifier, _1);
   pamHandler.userIdentifierToLocalUsername = userIdentifierToLocalUsername;
   pamHandler.mainPageFilter = mainPageFilter;
   pamHandler.signInThenContinue = signInThenContinue;
   pamHandler.refreshCredentialsThenContinue = refreshCredentialsThenContinue;
   pamHandler.signIn = signIn;
   pamHandler.signOut = signOut;
   if (canSetSignInCookies())
      pamHandler.setSignInCookies = boost::bind(setSignInCookies, _1, _2, _3, _4, false);
   pamHandler.refreshAuthCookies = boost::bind(setSignInCookies, _1, _2, _3, _4, true);
   auth::handler::registerHandler(pamHandler);

   // add pam-specific auth handlers
   uri_handlers::addBlocking(kDoSignIn, doSignIn);
   uri_handlers::addBlocking(kPublicKey, publicKey);

   // initialize overlay
   Error error = overlay::initialize();
   if (error)
      return error;

   // initialize crypto
   return core::system::crypto::rsaInit();
}

} // namespace pam_auth
} // namespace server
} // namespace rstudio
