/*
 * ServerOptions.cpp
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

#include <server/ServerOptions.hpp>

#include <fstream>

#include <boost/algorithm/string/trim.hpp>

#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <core/system/PosixUser.hpp>
#include <core/system/PosixSystem.hpp>

#include <monitor/MonitorConstants.hpp>

using namespace rstudio::core ;

namespace std
{
   // needed for boost to compile std::vector<std::string> default value for option
   std::ostream& operator<<(std::ostream &os, const std::vector<std::string> &vec)
   {
      for (auto item : vec)
      {
         os << item << " ";
      }

      return os;
   }
}

namespace rstudio {
namespace server {

namespace {

const char * const kDefaultProgramUser = "rstudio-server";

struct Deprecated
{
   Deprecated()
      : memoryLimitMb(0),
        stackLimitMb(0),
        userProcessLimit(0),
        authPamRequiresPriv(true)
   {
   }

   int memoryLimitMb;
   int stackLimitMb;
   int userProcessLimit;
   bool authPamRequiresPriv;
};

void reportDeprecationWarning(const std::string& option, std::ostream& os)
{
   os << "The option '" << option << "' is deprecated and will be discarded."
      << std::endl;
}

void reportDeprecationWarnings(const Deprecated& userOptions,
                               std::ostream& os)
{
   Deprecated defaultOptions;

   if (userOptions.memoryLimitMb != defaultOptions.memoryLimitMb)
      reportDeprecationWarning("rsession-memory-limit-mb", os);

   if (userOptions.stackLimitMb != defaultOptions.stackLimitMb)
      reportDeprecationWarning("rsession-stack-limit-mb", os);

   if (userOptions.userProcessLimit != defaultOptions.userProcessLimit)
      reportDeprecationWarning("rsession-process-limit", os);

   if (userOptions.authPamRequiresPriv != defaultOptions.authPamRequiresPriv)
      reportDeprecationWarning("auth-pam-requires-priv", os);
}

unsigned int stringToUserId(std::string minimumUserId,
                            unsigned int defaultMinimumId,
                            std::ostream& osWarnings)
{
   try
   {
      return boost::lexical_cast<unsigned int>(minimumUserId);
   }
   catch(boost::bad_lexical_cast&)
   {
      osWarnings << "Invalid value for auth-minimum-user-id '"
                 << minimumUserId << "'. Using default of "
                 << defaultMinimumId << "." << std::endl;

      return defaultMinimumId;
   }
}

unsigned int resolveMinimumUserId(std::string minimumUserId,
                                  std::ostream& osWarnings)
{
   // default for invalid input
   const unsigned int kDefaultMinimumId = 1000;

   // auto-detect if requested
   if (minimumUserId == "auto")
   {
      // if /etc/login.defs exists, scan it and look for a UID_MIN setting
      FilePath loginDefs("/etc/login.defs");
      if (loginDefs.exists())
      {
         const char uidMin[] = "UID_MIN";
         std::ifstream defStream(loginDefs.getAbsolutePath().c_str());
         std::string line;
         while (std::getline(defStream, line))
         {
            if (line.substr(0, sizeof(uidMin) - 1) == uidMin)
            {
               std::string value = boost::algorithm::trim_copy(
                                       line.substr(sizeof(uidMin) + 1));
               return stringToUserId(value, kDefaultMinimumId, osWarnings);
            }
         }
      }

      // none found, return default
      return kDefaultMinimumId;
   }
   else
   {
      return stringToUserId(minimumUserId, kDefaultMinimumId, osWarnings);
   }
}

} // anonymous namespace

Options& options()
{
   static Options instance ;
   return instance ;
}


ProgramStatus Options::read(int argc,
                            char * const argv[],
                            std::ostream& osWarnings)
{
   using namespace boost::program_options ;

   // compute install path
   Error error = core::system::installPath("..", argv[0], &installPath_);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.getSummary());
      return ProgramStatus::exitFailure();
   }

   // compute the resource and binary paths
   FilePath resourcePath = installPath_;
   FilePath binaryPath = installPath_.completeChildPath("bin");

   // detect running in OSX bundle and tweak paths
#ifdef __APPLE__
   if (installPath_.completePath("Info.plist").exists())
   {
      resourcePath = installPath_.completePath("Resources");
      binaryPath = installPath_.completePath("MacOS");
   }
#endif

   // verify installation flag
   options_description verify("verify");
   verify.add_options()
     ("verify-installation",
     value<bool>(&verifyInstallation_)->default_value(false),
     "verify the current installation");

   // special program offline option (based on file existence at 
   // startup for easy bash script enable/disable of offline state)
   serverOffline_ = FilePath("/var/lib/rstudio-server/offline").exists();

   // generate monitor shared secret
   monitorSharedSecret_ = core::system::generateUuid();

   // temporary list of origins in string form - later converted to regex
   std::vector<std::string> wwwAllowedOrigins;

   // program - name and execution
   options_description server("server");
   server.add_options()
      ("server-working-dir",
         value<std::string>(&serverWorkingDir_)->default_value("/"),
         "program working directory")
      ("server-user",
         value<std::string>(&serverUser_)->default_value(kDefaultProgramUser),
         "program user")
      ("server-daemonize",
         value<bool>(&serverDaemonize_)->default_value(
                                      core::system::effectiveUserIsRoot()),
         "run program as daemon")
      ("server-pid-file",
         value<std::string>(&serverPidFile_)->default_value("/var/run/rstudio-server.pid"),
         "location of pid file to write (only in daemon mode)")
      ("server-app-armor-enabled",
        value<bool>(&serverAppArmorEnabled_)->default_value(0),
        "is app armor enabled for this session")
      ("server-set-umask",
         value<bool>(&serverSetUmask_)->default_value(1),
         "set the umask to 022 on startup")
      ("secure-cookie-key-file",
        value<std::string>(&secureCookieKeyFile_)->default_value(""),
        "path override for secure cookie key")
      ("server-data-dir",
         value<std::string>(&serverDataDir_)->default_value("/var/run/rstudio-server"),
         "path to data directory where rstudio server will write run-time state")
      ("server-add-header",
       value<std::vector<std::string>>(&serverAddHeaders_)->default_value(std::vector<std::string>{})->multitoken(),
         "adds a header to all responses from RStudio Server");

   // www - web server options
   options_description www("www") ;
   www.add_options()
      ("www-address",
         value<std::string>(&wwwAddress_)->default_value("0.0.0.0"),
         "server address")
      ("www-port",
         value<std::string>(&wwwPort_)->default_value(""),
         "port to listen on")
      ("www-local-path",
         value<std::string>(&wwwLocalPath_)->default_value("www"),
         "www files path")
      ("www-symbol-maps-path",
         value<std::string>(&wwwSymbolMapsPath_)->default_value(
                                                      "www-symbolmaps"),
        "www symbol maps path")
      ("www-use-emulated-stack",
       value<bool>(&wwwUseEmulatedStack_)->default_value(false),
       "use gwt emulated stack")
      ("www-thread-pool-size",
         value<int>(&wwwThreadPoolSize_)->default_value(2),
         "thread pool size")
      ("www-proxy-localhost",
         value<bool>(&wwwProxyLocalhost_)->default_value(true),
         "proxy requests to localhost ports over main server port")
      ("www-verify-user-agent",
         value<bool>(&wwwVerifyUserAgent_)->default_value(true),
         "verify that the user agent is compatible")
      ("www-frame-origin",
         value<std::string>(&wwwFrameOrigin_)->default_value("none"),
         "allowed origin for hosting frame")
      ("www-enable-origin-check",
         value<bool>(&wwwEnableOriginCheck_)->default_value(false),
         "enable check that ensures request origin is from the host domain")
      ("www-allow-origin",
         value<std::vector<std::string>>(&wwwAllowedOrigins)->default_value(std::vector<std::string>{})->multitoken(),
         "allows requests from this origin, even if it does not match the host domain");

   // rsession
   Deprecated dep;
   options_description rsession("rsession");
   rsession.add_options()
      ("rsession-which-r",
         value<std::string>(&rsessionWhichR_)->default_value(""),
         "path to main R program (e.g. /usr/bin/R)")
      ("rsession-path", 
         value<std::string>(&rsessionPath_)->default_value("rsession"),
         "path to rsession executable")
      ("rldpath-path",
         value<std::string>(&rldpathPath_)->default_value("r-ldpath"),
         "path to r-ldpath script")
      ("rsession-ld-library-path",
         value<std::string>(&rsessionLdLibraryPath_)->default_value(""),
         "default LD_LIBRARY_PATH for rsession")
      ("rsession-config-file",
         value<std::string>(&rsessionConfigFile_)->default_value(""),
         "path to rsession config file")
      ("rsession-proxy-max-wait-secs",
        value<int>(&rsessionProxyMaxWaitSeconds_)->default_value(10),
         "max time to wait when proxying requests to rsession")
      ("rsession-memory-limit-mb",
         value<int>(&dep.memoryLimitMb)->default_value(dep.memoryLimitMb),
         "rsession memory limit (mb) - DEPRECATED")
      ("rsession-stack-limit-mb",
         value<int>(&dep.stackLimitMb)->default_value(dep.stackLimitMb),
         "rsession stack limit (mb) - DEPRECATED")
      ("rsession-process-limit",
         value<int>(&dep.userProcessLimit)->default_value(dep.userProcessLimit),
         "rsession user process limit - DEPRECATED");
   
   // still read depracated options (so we don't break config files)
   std::string authMinimumUserId, authLoginPageHtml;
   options_description auth("auth");
   auth.add_options()
      ("auth-none",
        value<bool>(&authNone_)->default_value(
                                 !core::system::effectiveUserIsRoot()),
        "don't do any authentication")
      ("auth-validate-users",
        value<bool>(&authValidateUsers_)->default_value(
                                 core::system::effectiveUserIsRoot()),
        "validate that authenticated users exist on the target system")
      ("auth-stay-signed-in-days",
        value<int>(&authStaySignedInDays_)->default_value(30),
       "number of days for stay signed in option")
      ("auth-timeout-minutes",
        value<int>(&authTimeoutMinutes_)->default_value(60),
        "number of minutes users will stay logged in while idle before required to sign in again")
      ("auth-encrypt-password",
        value<bool>(&authEncryptPassword_)->default_value(true),
        "encrypt password sent from login form")
      ("auth-login-page-html",
        value<std::string>(&authLoginPageHtml)->default_value("/etc/rstudio/login.html"),
        "path to file containing additional html for login page")
      ("auth-required-user-group",
        value<std::string>(&authRequiredUserGroup_)->default_value(""),
        "limit to users belonging to the specified group")
      ("auth-minimum-user-id",
        value<std::string>(&authMinimumUserId)->default_value("auto"),
        "limit to users with a required minimum user id")
      ("auth-pam-helper-path",
        value<std::string>(&authPamHelperPath_)->default_value("rserver-pam"),
       "path to PAM helper binary")
      ("auth-pam-require-password-prompt",
        value<bool>(&authPamRequirePasswordPrompt_)->default_value(true),
        "whether or not to require the Password: prompt before sending the password via PAM")
      ("auth-pam-requires-priv",
        value<bool>(&dep.authPamRequiresPriv)->default_value(
                                                   dep.authPamRequiresPriv),
        "deprecated: will always be true")
      ("auth-sign-in-throttle-seconds",
        value<int>(&authSignInThrottleSeconds_)->default_value(5),
        "minimum amount of time a user must wait before attempting to sign in again")
      ("auth-revocation-list-dir",
        value<std::string>(&authRevocationListDir_)->default_value(""),
        "path to the directory which contains the revocation list to be used for storing expired auth tokens")
      ("auth-cookies-force-secure",
        value<bool>(&authCookiesForceSecure_)->default_value(false),
        "forces auth cookies to be marked as secure - should be enabled if running an SSL terminator infront of RStudio Server");

   options_description monitor("monitor");
   monitor.add_options()
      (kMonitorIntervalSeconds,
       value<int>(&monitorIntervalSeconds_)->default_value(300),
       "monitoring interval");

   // define program options
   FilePath defaultConfigPath("/etc/rstudio/rserver.conf");
   std::string configFile = defaultConfigPath.exists() ?
                            defaultConfigPath.getAbsolutePath() : "";
   program_options::OptionsDescription optionsDesc("rserver", configFile);

   // overlay hook
   addOverlayOptions(&verify, &server, &www, &rsession, &auth, &monitor);

   optionsDesc.commandLine.add(verify).add(server).add(www).add(rsession).add(auth).add(monitor);
   optionsDesc.configFile.add(server).add(www).add(rsession).add(auth).add(monitor);
 
   // read options
   bool help = false;
   ProgramStatus status = core::program_options::read(optionsDesc,
                                                      argc,
                                                      argv,
                                                      &help);

   // terminate if this was a help request
   if (help)
      return ProgramStatus::exitSuccess();

   // report deprecation warnings
   reportDeprecationWarnings(dep, osWarnings);

   // check auth revocation dir - if unspecified, it should be put under the server data dir
   if (authRevocationListDir_.empty())
      authRevocationListDir_ = serverDataDir_;

   // call overlay hooks
   resolveOverlayOptions();
   std::string errMsg;
   if (!validateOverlayOptions(&errMsg, osWarnings))
   {
      program_options::reportError(errMsg, ERROR_LOCATION);
      return ProgramStatus::exitFailure();
   }

   // exit if the call to read indicated we should -- note we don't do this
   // immediately so that we can allow overlay validation to occur (otherwise
   // a --test-config wouldn't test overlay options)
   if (status.exit())
      return status;

   // rationalize auth settings
   if (authNone_)
      authValidateUsers_ = false;

   // if specified, confirm that the program user exists. however, if the
   // program user is the default and it doesn't exist then allow that to pass,
   // this just means that the user did a simple make install and hasn't setup
   // an rserver user yet. in this case the program will run as root
   if (!serverUser_.empty())
   {
      // if we aren't running as root then forget the programUser
      if (!core::system::realUserIsRoot())
      {
         serverUser_ = "";
      }
      // if there is a program user specified and it doesn't exist....
      else
      {
         system::User user;
         Error error = system::User::getUserFromIdentifier(serverUser_, user);
         if (error || !user.exists())
         {
            if (serverUser_ == kDefaultProgramUser)
            {
               // administrator hasn't created an rserver system account yet
               // so we'll end up running as root
               serverUser_ = "";
            }
            else
            {
               LOG_ERROR_MESSAGE("Server user " + serverUser_ + " does not exist");
               return ProgramStatus::exitFailure();
            }
         }
      }
   }

   // convert relative paths by completing from the system installation
   // path (this allows us to be relocatable)
   resolvePath(resourcePath, &wwwLocalPath_);
   resolvePath(resourcePath, &wwwSymbolMapsPath_);
   resolvePath(binaryPath, &authPamHelperPath_);
   resolvePath(binaryPath, &rsessionPath_);
   resolvePath(binaryPath, &rldpathPath_);
   resolvePath(resourcePath, &rsessionConfigFile_);

   // resolve minimum user id
   authMinimumUserId_ = resolveMinimumUserId(authMinimumUserId, osWarnings);
   core::r_util::setMinUid(authMinimumUserId_);

   // read auth login html
   FilePath loginPageHtmlPath(authLoginPageHtml);
   if (loginPageHtmlPath.exists())
   {
      Error error = core::readStringFromFile(loginPageHtmlPath, &authLoginPageHtml_);
      if (error)
         LOG_ERROR(error);
   }

   // trim any whitespace in allowed origins
   for (std::string& origin : wwwAllowedOrigins)
   {
      try
      {
         // escape domain part separators
         boost::replace_all(origin, ".", "\\.");

         // fix up wildcards
         boost::replace_all(origin, "*", ".*");

         boost::regex re(origin);
         wwwAllowedOrigins_.push_back(re);
      }
      catch (boost::bad_expression&)
      {
         LOG_ERROR_MESSAGE("Specified origin " + origin + " is an invalid domain. "
                           "It will not be available when performing origin safety checks.");
      }
   }

   // return status
   return status;
}

void Options::resolvePath(const FilePath& basePath,
                          std::string* pPath) const
{
   if (!pPath->empty())
      *pPath = basePath.completePath(*pPath).getAbsolutePath();
}

} // namespace server
} // namespace rstudio
