/*
 * ServerAuthHandler.cpp
 *
 * Copyright (C) 2009-12 by RStudio, PBC
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

#ifndef SERVER_AUTH_HANDLER_CPP
#define SERVER_AUTH_HANDLER_CPP

#include <server/auth/ServerAuthHandler.hpp>

#include <boost/algorithm/string.hpp>

#include <core/FileLock.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/FileMode.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/Thread.hpp>

#include <server/ServerConstants.hpp>
#include <server/ServerObject.hpp>
#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

#include <session/SessionScopes.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace auth {
namespace handler {

namespace {

FilePath s_revocationList;
FilePath s_revocationLockFile;

boost::posix_time::ptime cookieExpiration(const std::string& cookie)
{
   std::vector<std::string> cookieParts;
   boost::algorithm::split(cookieParts, cookie, boost::is_any_of("|"));

   if (cookieParts.size() > 1)
   {
      std::string expiration = cookieParts[1];
      std::string expirationStr = http::util::urlDecode(expiration);
      return http::util::parseHttpDate(expirationStr);
   }
   return boost::posix_time::second_clock::universal_time();
}

// map of last user sign in times to prevent users from creating
// inordinate amounts of revocation entries
std::map<std::string, boost::posix_time::ptime> s_loginTimes;

// sorted array of revoked cookies, where the first to expire appears as the first element
// allows for quickly removing the first element
std::deque<RevokedCookie> s_revokedCookies;

// mutex for providing concurrent access to internal structures
// necessary because auth happens on the thread pool
boost::mutex s_mutex;

// global auth handler
Handler s_handler;

void updateCredentialsNotSupported(
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // alias response
   http::Response* pResponse = &(pConnection->response());

   // gwt form panel requires text/html content type
   pResponse->setContentType("text/html");

   // set method not found error
   Error methodNotFoundError(json::errc::MethodNotFound, ERROR_LOCATION);
   json::setJsonRpcError(methodNotFoundError, pResponse);

   // write response
   pConnection->writeResponse();
}

bool isCookieRevoked(const std::string& cookie)
{
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();

   LOCK_MUTEX(s_mutex)
   {
      // check for cookie in revocation list, deleting expired elements as we go
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end();)
      {
         const RevokedCookie& other = *it;
         if (other.cookie == cookie)
            return true;

         if (other.expiration <= now)
         {
            it = s_revokedCookies.erase(it);
            continue;
         }
         else
         {
            ++it;
         }
      }
   }
   END_LOCK_MUTEX

   return false;
}

Error readRevocationList(std::vector<std::string>* pEntries)
{
   // read the current revocation list
   Error error = readStringVectorFromFile(s_revocationList, pEntries);
   if (error)
      return error;

   // remove stale entries from the list
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
   for (int i = pEntries->size() - 1; i >= 0; i--)
   {
      const std::string& cookie = pEntries->at(i);

      boost::posix_time::ptime expirationDate = cookieExpiration(cookie);

      if (expirationDate <= now)
         pEntries->erase(pEntries->begin() + i);
   }

   return Success();
}

} // anonymous namespace

namespace overlay {

Error initialize()
{
   return Success();
}

Error isUserLicensed(const std::string& username,
                     bool* pLicensed)
{
   *pLicensed = true;
   return Success();
}

bool isUserListCookieValid(const std::string& cookieValue)
{
   return true;
}

bool shouldShowUserLicenseWarning()
{
   return false;
}

std::string getUserListCookieValue()
{
   return "9c16856330a7400cbbbba228392a5d83";
}

unsigned int getActiveUserCount()
{
   return 0;
}

json::Array getLicensedUsers()
{
   return json::Array();
}

Error lockUser(boost::asio::io_service& ioService,
               const std::string& username)
{
   return Success();
}

Error unlockUser(boost::asio::io_service& ioService,
                 const std::string& username)
{
   return Success();
}

} // namespace overlay

void onCookieRevoked(const std::string& cookie)
{
}

// uri constants
const char * const kSignIn = "/auth-sign-in";
const char * const kSignOut = "/auth-sign-out";
const char * const kRefreshCredentialsAndContinue = "/auth-refresh-credentials";

void onCookieRevoked(const std::string& cookie);

RevokedCookie::RevokedCookie(const std::string& cookie)
{
   this->cookie = cookie;
   this->expiration = cookieExpiration(cookie);
}

std::string getUserIdentifier(const core::http::Request& request,
                              bool requireUserListCookie,
                              http::Response* pResponse)
{
   if (isCookieRevoked(request.cookieValue(kUserIdCookie)))
      return std::string();

   std::string userIdentifier = s_handler.getUserIdentifier(request, pResponse);
   if (userIdentifier.empty())
      return std::string();

   if (requireUserListCookie)
   {
      if (!overlay::isUserListCookieValid(request.cookieValue(kUserListCookie)))
         return std::string();
   }

   return userIdentifier;
}

std::string userIdentifierToLocalUsername(const std::string& userIdentifier)
{
   return s_handler.userIdentifierToLocalUsername(userIdentifier);
}

bool mainPageFilter(const core::http::Request& request,
                    core::http::Response* pResponse)
{
   return s_handler.mainPageFilter(request, pResponse);
}

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse)
{
   s_handler.signInThenContinue(request, pResponse);
}

void refreshCredentialsThenContinue(
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   s_handler.refreshCredentialsThenContinue(pConnection);
}


// register the auth handler
void registerHandler(const Handler& handler)
{
   // set handler functions
   s_handler = handler;

   // register uri handlers
   uri_handlers::addBlocking(kSignIn, s_handler.signIn);

   uri_handlers::addBlocking(kSignOut,
                             auth::secureHttpHandler(
                                boost::bind(s_handler.signOut, _2, _3)));

   uri_handlers::add(kRefreshCredentialsAndContinue,
                     s_handler.refreshCredentialsThenContinue);

   uri_handlers::add("/auth-update-credentials",
                     s_handler.updateCredentials ?
                        s_handler.updateCredentials :
                        updateCredentialsNotSupported);
}

// is there a handler already registered?
bool isRegistered()
{
   return ! s_handler.getUserIdentifier.empty();
}

bool canSetSignInCookies()
{
   return !s_handler.setSignInCookies.empty();
}

void setSignInCookies(const core::http::Request& request,
                      const std::string& username,
                      bool persist,
                      core::http::Response* pResponse)
{
   s_handler.setSignInCookies(request, username, persist, pResponse);
}

void signOut(const http::Request& request, http::Response* pResponse)
{
   s_handler.signOut(request, pResponse);
}

bool isUserSignInThrottled(const std::string& user)
{
   LOCK_MUTEX(s_mutex)
   {
      auto it = s_loginTimes.find(user);

      boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
      if (it == s_loginTimes.end())
      {
         s_loginTimes.insert({user, now});
         return false;
      }

      if (it->second >
          now - boost::posix_time::seconds(options().authSignInThrottleSeconds()))
      {
         // user is trying to sign back in too quickly
         // prevent the request
         return true;
      }
      else
      {
         // user is fine to sign in - update their last sign in time to now
         it->second = now;
      }

      return false;
   }
   END_LOCK_MUTEX

   return false;
}

void refreshAuthCookies(const std::string& userIdentifier,
                        const http::Request& request,
                        http::Response* pResponse)
{
   if (server::options().authTimeoutMinutes() > 0 &&
       !s_handler.refreshAuthCookies.empty())
   {
      // clear any existing auth cookies first - this method can be invoked multiple
      // times depending on the handler type (for example, an upload handler)
      pResponse->clearCookies();
      bool persist = request.cookieValue(kPersistAuthCookie) == "1" ? true : false;
      s_handler.refreshAuthCookies(request, userIdentifier, persist, pResponse);
   }
}

void insertRevokedCookie(const RevokedCookie& cookie)
{
   // do not insert the cookie if it is already expired
   if (cookie.expiration <= boost::posix_time::second_clock::universal_time())
      return;

   LOCK_MUTEX(s_mutex)
   {
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end(); ++it)
      {
         const RevokedCookie& other = *it;
         if (other.expiration > cookie.expiration)
         {
            s_revokedCookies.insert(it, cookie);
            return;
         }
      }

      // no elements were greater - insert at the end
      s_revokedCookies.insert(s_revokedCookies.end(), cookie);
   }
   END_LOCK_MUTEX
}

void invalidateAuthCookie(const std::string& cookie,
                          ExponentialBackoffPtr backoffPtr)
{
   if (cookie.empty())
      return;

   // create a file lock to gain exclusive access to the revocation list
   boost::shared_ptr<FileLock> lock = FileLock::createDefault();
   ScopedFileLock fileLock(lock, s_revocationLockFile);
   Error error = fileLock.error();
   if (error)
   {
      // if we could not acquire the lock, some other rserver process has
      // attempt to retry the operation on an exponential backoff timer
      if (!backoffPtr)
      {
         backoffPtr = boost::make_shared<ExponentialBackoff>(server::server()->ioService(),
                                                             boost::posix_time::seconds(1),
                                                             boost::posix_time::seconds(15),
                                                             boost::bind(invalidateAuthCookie, cookie, _1));
      }

      bool keepTrying = backoffPtr->next();
      if (!keepTrying)
      {
         LOG_ERROR_MESSAGE("Could not invalidate user auth cookie - could not acquire revocation list lockfile");
         LOG_ERROR(error);
      }

      return;
   }

   // read the current revocation list
   std::vector<std::string> revokedCookies;
   error = readRevocationList(&revokedCookies);
   if (error)
   {
      LOG_ERROR_MESSAGE("Could not invalidate user auth cookie - could not read revocation list");
      LOG_ERROR(error);
      return;
   }

   // add the new entry
   revokedCookies.push_back(cookie);

   // write the new contents to file
   error = writeStringVectorToFile(s_revocationList, revokedCookies);
   if (error)
   {
      LOG_ERROR_MESSAGE("Could not invalidate user auth cookie - could not write to revocation list");
      LOG_ERROR(error);
      return;
   }

   // store the revoked cookie in memory - we only check the memory cache when
   // checking incoming requests to see if the cookie presented has been revoked
   // because it is too expensive to hit the disk every time
   insertRevokedCookie(RevokedCookie(cookie));

   onCookieRevoked(cookie);
}

Error initialize()
{
   // initialize by loading the current contents of the revocation list into memory

   FilePath rootDir = options().authRevocationListDir();
   s_revocationList = rootDir.completeChildPath("revocation-list");
   s_revocationLockFile = rootDir.completeChildPath("revocation-list.lock");

   // first, if we are running as root, attempt to fixup the user/permissions on the
   // revocation list files - we attempt to do this to be more flexible in allowing
   // admins to change the server-user when convenient
   if (core::system::effectiveUserIsRoot())
   {
      if (rootDir.exists())
      {
         Error error = file_utils::changeOwnership(rootDir, options().serverUser());
         if (error)
         {
            error.addProperty("description",
                              "Could not change owner for path " + rootDir.getAbsolutePath() +
                                 ". Is root squash enabled?");
            LOG_ERROR(error);
         }
      }

      if (s_revocationList.exists())
      {
         Error error = file_utils::changeOwnership(s_revocationList, options().serverUser());
         if (error)
         {
            error.addProperty("description",
                              "Could not change owner for path " + s_revocationList.getAbsolutePath() +
                                 ". Is root squash enabled?");
            LOG_ERROR(error);
         }
      }

      // now that we have attempted to fix up permissions on the files, lower privilege
      // we want to ensure the files are created by the server user itself instead of root,
      // as root is problematic (i.e. has no actual permissions) on root squash mounts
      Error error = core::system::temporarilyDropPriv(options().serverUser());
      if (error)
         return error;
   }

   // create revocation list directory and ensure the server user has permission to write to it
   Error error = rootDir.ensureDirectory();
   if (error)
   {
      LOG_ERROR_MESSAGE("Could not create revocation list directory " + rootDir.getAbsolutePath());
      return error;
   }

   // create a file lock to gain exclusive access to the revocation list
   boost::shared_ptr<FileLock> lock = FileLock::createDefault();
   int numTries = 0;

   while (numTries < 30)
   {
      {  // begin scope for ScopedFileLock
         ScopedFileLock fileLock(lock, s_revocationLockFile);
         Error error = fileLock.error();
         if (error)
         {
            // if we could not acquire the lock, some other rserver process has
            // keep trying for some time before giving up
            ++numTries;
            boost::this_thread::sleep(boost::posix_time::seconds(1));
            continue;
         }

         // successfully acquired lock
         // create file if it does not exist
         error = s_revocationList.ensureFile();
         if (error)
         {
            LOG_ERROR_MESSAGE("Could not create revocation list");
            return error;
         }

         // ensure that only the server user can read/write to it, so other users of the system
         // cannot muck with the contents!
         error = core::system::changeFileMode(s_revocationList, core::system::UserReadWriteMode);
         if (error)
         {
            error.addProperty("description", "Could not set revocation file permissions to 600 for file: " + s_revocationList.getAbsolutePath());
            LOG_ERROR(error);
         }

         // read the current revocation list into memory
         std::vector<std::string> revokedCookies;
         error = readRevocationList(&revokedCookies);
         if (error)
         {
            LOG_ERROR_MESSAGE("Could not read revocation list");
            return error;
         }

         for (const std::string& cookie : revokedCookies)
            insertRevokedCookie(RevokedCookie(cookie));

         // write the contents back out to file as stale entries have been removed
         error = writeStringVectorToFile(s_revocationList, revokedCookies);
         if (error)
         {
            LOG_ERROR_MESSAGE("Could not write to revocation list");
            return error;
         }
      }  // end scope for ScopedFileLock

      // now that basic auth has been fully initialized, restore root privilege
      // so the rest of initialization routines have proper permissions
      if (core::system::realUserIsRoot())
      {
         error = core::system::restorePriv();
         if (error)
            return error;
      }

      return overlay::initialize();
   }

   // the file lock could not be acquired
   return systemError(boost::system::errc::resource_unavailable_try_again,
                      "Could not acquire revocation list file lock",
                      ERROR_LOCATION);
}

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_HANDLER_CPP


