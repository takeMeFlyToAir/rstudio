/*
 * SessionOptions.cpp
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

#include <session/SessionOptions.hpp>

#include <boost/algorithm/string/trim.hpp>

#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/ini_parser.hpp>

#include <shared_core/FilePath.hpp>
#include <core/ProgramStatus.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RUserData.hpp>
#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/r_util/RVersionsPosix.hpp>

#include <monitor/MonitorConstants.hpp>

#include <r/session/RSession.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionScopes.hpp>
#include <session/projects/SessionProjectSharing.hpp>

#include "session-config.h"

using namespace rstudio::core ;

namespace rstudio {
namespace session {  

namespace {
const char* const kDefaultPandocPath = "bin/pandoc";
const char* const kDefaultPostbackPath = "bin/postback/rpostback";
const char* const kDefaultRsclangPath = "bin/rsclang";

void ensureDefaultDirectory(std::string* pDirectory,
                            const std::string& userHomePath)
{
   if (*pDirectory != "~")
   {
      FilePath dir = FilePath::resolveAliasedPath(*pDirectory,
                                                  FilePath(userHomePath));
      Error error = dir.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         *pDirectory = "~";
      }
   }
}

} // anonymous namespace

Options& options()
{
   static Options instance ;
   return instance ;
}
   
core::ProgramStatus Options::read(int argc, char * const argv[], std::ostream& osWarnings)
{
   using namespace boost::program_options ;
   
   // get the shared secret
   monitorSharedSecret_ = core::system::getenv(kMonitorSharedSecretEnvVar);
   core::system::unsetenv(kMonitorSharedSecretEnvVar);

   // compute the resource path
   Error error = core::system::installPath("..", argv[0], &resourcePath_);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.getSummary());
      return ProgramStatus::exitFailure();
   }

   // detect running in OSX bundle and tweak resource path
#ifdef __APPLE__
   if (resourcePath_.completePath("Info.plist").exists())
      resourcePath_ = resourcePath_.completePath("Resources");
#endif

   // detect running in x86 directory and tweak resource path
#ifdef _WIN32
   if (resourcePath_.completePath("x86").exists())
   {
      resourcePath_ = resourcePath_.getParent();
   }
#endif
   
   // run tests flag
   options_description runTests("tests");
   runTests.add_options()
         (kRunTestsSessionOption,
          value<bool>(&runTests_)->default_value(false)->implicit_value(true),
          "run unit tests");

   // run an R script
   options_description runScript("script");
   runScript.add_options()
     (kRunScriptSessionOption,
      value<std::string>(&runScript_)->default_value(""),
      "run an R script");

   // verify installation flag
   options_description verify("verify");
   verify.add_options()
     (kVerifyInstallationSessionOption,
     value<bool>(&verifyInstallation_)->default_value(false),
     "verify the current installation");

   // program - name and execution
   options_description program("program");
   program.add_options()
      (kProgramModeSessionOption,
         value<std::string>(&programMode_)->default_value("server"),
         "program mode (desktop or server");
   
   // log -- logging options
   options_description log("log");
   log.add_options()
      ("log-stderr",
      value<bool>(&logStderr_)->default_value(false),
      "write log entries to stderr");

   // agreement
   options_description agreement("agreement");
   agreement.add_options()
      ("agreement-file",
      value<std::string>(&agreementFilePath_)->default_value(""),
      "agreement file");

   // docs url
   options_description docs("docs");
   docs.add_options()
      ("docs-url",
       value<std::string>(&docsURL_)->default_value(""),
       "custom docs url");

   // www options
   options_description www("www") ;
   www.add_options()
      ("www-local-path",
         value<std::string>(&wwwLocalPath_)->default_value("www"),
         "www local path")
      ("www-symbol-maps-path",
         value<std::string>(&wwwSymbolMapsPath_)->default_value(
                                                         "www-symbolmaps"),
         "www symbol maps path")
      (kWwwPortSessionOption,
         value<std::string>(&wwwPort_)->default_value("8787"),
         "port to listen on")
      (kWwwAddressSessionOption,
         value<std::string>(&wwwAddress_)->default_value("127.0.0.1"),
         "address to listen on")
      (kStandaloneSessionOption,
         value<bool>(&standalone_)->default_value(false),
         "run standalone")
      (kVerifySignaturesSessionOption,
         value<bool>(&verifySignatures_)->default_value(false),
         "verify signatures on incoming requests");

   // session options
   std::string saveActionDefault;
   options_description session("session") ;
   session.add_options()
      (kTimeoutSessionOption,
         value<int>(&timeoutMinutes_)->default_value(120),
         "session timeout (minutes)" )
      (kTimeoutSuspendSessionOption,
         value<bool>(&timeoutSuspend_)->default_value(true),
         "whether to suspend on session timeout")
      (kDisconnectedTimeoutSessionOption,
         value<int>(&disconnectedTimeoutMinutes_)->default_value(0),
         "session disconnected timeout (minutes)" )
      ("session-preflight-script",
         value<std::string>(&preflightScript_)->default_value(""),
         "session preflight script")
      ("session-create-public-folder",
         value<bool>(&createPublicFolder_)->default_value(false),
         "automatically create public folder")
      ("session-create-profile",
         value<bool>(&createProfile_)->default_value(false),
         "automatically create .Rprofile")
      ("session-rprofile-on-resume-default",
          value<bool>(&rProfileOnResumeDefault_)->default_value(false),
          "default user setting for running Rprofile on resume")
      ("session-save-action-default",
       value<std::string>(&saveActionDefault)->default_value(""),
         "default save action (yes, no, or ask)")
      ("session-default-working-dir",
       value<std::string>(&defaultWorkingDir_)->default_value("~"),
       "default working directory for new sessions")
      ("session-default-new-project-dir",
       value<std::string>(&defaultProjectDir_)->default_value("~"),
       "default directory for new projects")
      ("show-help-home",
       value<bool>(&showHelpHome_)->default_value(false),
         "show help home page at startup")
      ("session-default-console-term",
       value<std::string>(&defaultConsoleTerm_)->default_value("xterm-256color"),
       "default TERM setting for R console")
      ("session-default-clicolor-force",
       value<bool>(&defaultCliColorForce_)->default_value(true),
       "default CLICOLOR_FORCE setting for R console")
      ("session-quit-child-processes-on-exit",
       value<bool>(&quitChildProcessesOnExit_)->default_value(false),
       "quit child processes on session exit")
      ("session-first-project-template-path",
       value<std::string>(&firstProjectTemplatePath_)->default_value(""),
       "first project template path")
      ("default-rsconnect-server",
       value<std::string>(&defaultRSConnectServer_)->default_value(""),
       "default RStudio Connect server URL")
      (kTerminalPortOption,
       value<std::string>(&terminalPort_)->default_value(""),
       "port to bind the terminal server to")
      (kWebSocketPingInterval,
       value<int>(&webSocketPingSeconds_)->default_value(10),
       "WebSocket keep-alive ping interval (seconds)")
      (kWebSocketConnectTimeout,
       value<int>(&webSocketConnectTimeout_)->default_value(3),
       "WebSocket initial connection timeout (seconds)")
      (kWebSocketLogLevel,
       value<int>(&webSocketLogLevel_)->default_value(0),
       "WebSocket log level (0=none, 1=errors, 2=activity, 3=all)")
      (kWebSocketHandshakeTimeout,
       value<int>(&webSocketHandshakeTimeoutMs_)->default_value(5000),
       "WebSocket protocol handshake timeout (ms)")
      (kPackageOutputInPackageFolder,
       value<bool>(&packageOutputToPackageFolder_)->default_value(false),
       "devtools check and devtools build output to package project folder")
      (kUseSecureCookiesSessionOption,
       value<bool>(&useSecureCookies_)->default_value(false),
       "whether to mark cookies as secure")
      ("restrict-directory-view",
       value<bool>(&restrictDirectoryView_)->default_value(false),
       "whether to restrict the directories that can be viewed in the IDE")
      ("directory-view-whitelist",
       value<std::string>(&directoryViewWhitelist_)->default_value(""),
       "list of directories exempt from directory view restrictions, separated by :")
      (kSessionEnvVarSaveBlacklist,
       value<std::string>(&envVarSaveBlacklist_)->default_value(""),
       "list of environment variables not saved on session suspend, separated by :");

   // allow options
   options_description allow("allow");
   allow.add_options()
      ("allow-vcs-executable-edit",
         value<bool>(&allowVcsExecutableEdit_)->default_value(true),
         "allow editing of vcs executables")
      ("allow-r-cran-repos-edit",
         value<bool>(&allowCRANReposEdit_)->default_value(true),
         "Allow editing of CRAN repository")
      ("allow-vcs",
         value<bool>(&allowVcs_)->default_value(true),
         "allow use of version control features")
      ("allow-package-installation",
         value<bool>(&allowPackageInstallation_)->default_value(true),
         "allow installation of packages from the packages pane")
      ("allow-shell",
         value<bool>(&allowShell_)->default_value(true),
         "allow access to shell dialog")
      ("allow-terminal-websockets",
         value<bool>(&allowTerminalWebsockets_)->default_value(true),
         "allow connection to terminal sessions with websockets")
      ("allow-file-downloads",
         value<bool>(&allowFileDownloads_)->default_value(true),
         "allow file downloads from the files pane")
      ("allow-file-uploads",
         value<bool>(&allowFileUploads_)->default_value(true),
         "allow file uploads from the files pane")
      ("allow-remove-public-folder",
         value<bool>(&allowRemovePublicFolder_)->default_value(true),
         "allow removal of the user public folder")
      ("allow-rpubs-publish",
         value<bool>(&allowRpubsPublish_)->default_value(true),
        "allow publishing content to external services")
      ("allow-external-publish",
         value<bool>(&allowExternalPublish_)->default_value(true),
        "allow publishing content to external services")
      ("allow-publish",
         value<bool>(&allowPublish_)->default_value(true),
        "allow publishing content")
      ("allow-presentation-commands",
         value<bool>(&allowPresentationCommands_)->default_value(false),
       "allow presentation commands")
      ("allow-full-ui",
         value<bool>(&allowFullUI_)->default_value(true),
       "allow full standalone ui mode")
      ("allow-launcher-jobs",
         value<bool>(&allowLauncherJobs_)->default_value(true),
         "allow running jobs via launcher");

   // r options
   bool rShellEscape; // no longer works but don't want to break any
                      // config files which formerly used it
                      // TODO: eliminate this option entirely
   options_description r("r") ;
   r.add_options()
      ("r-core-source",
         value<std::string>(&coreRSourcePath_)->default_value("R"),
         "Core R source path")
      ("r-modules-source", 
         value<std::string>(&modulesRSourcePath_)->default_value("R/modules"),
         "Modules R source path")
      ("r-session-library",
         value<std::string>(&sessionLibraryPath_)->default_value("R/library"),
         "R library path")
      ("r-session-package-archives",
          value<std::string>(&sessionPackageArchivesPath_)->default_value("R/packages"),
         "R package archives path")
      ("r-libs-user",
         value<std::string>(&rLibsUser_)->default_value(""),
         "R user library path")
      ("r-cran-repos",
         value<std::string>(&rCRANUrl_)->default_value(""),
         "Default CRAN repository")
      ("r-cran-repos-file",
         value<std::string>(&rCRANReposFile_)->default_value("/etc/rstudio/repos.conf"),
         "Path to configuration file with default CRAN repositories")
      ("r-cran-repos-url",
         value<std::string>(&rCRANReposUrl_)->default_value(""),
         "URL to configuration file with optional CRAN repositories")
      ("r-auto-reload-source",
         value<bool>(&autoReloadSource_)->default_value(false),
         "Reload R source if it changes during the session")
      ("r-compatible-graphics-engine-version",
         value<int>(&rCompatibleGraphicsEngineVersion_)->default_value(12),
         "Maximum graphics engine version we are compatible with")
      ("r-resources-path",
         value<std::string>(&rResourcesPath_)->default_value("resources"),
         "Directory containing external resources")
      ("r-shell-escape",
         value<bool>(&rShellEscape)->default_value(false),
         "Support shell escape (deprecated, no longer works)")
      ("r-home-dir-override",
         value<std::string>(&rHomeDirOverride_)->default_value(""),
         "Override for R_HOME (used for debug configurations)")
      ("r-doc-dir-override",
         value<std::string>(&rDocDirOverride_)->default_value(""),
         "Override for R_DOC_DIR (used for debug configurations)")
      ("r-restore-workspace",
         value<int>(&rRestoreWorkspace_)->default_value(kRestoreWorkspaceDefault),
         "Override user/project restore workspace setting")
      ("r-run-rprofile",
         value<int>(&rRunRprofile_)->default_value(kRunRprofileDefault),
         "Override user/project .Rprofile run setting");

   // limits options
   options_description limits("limits");
   limits.add_options()
      ("limit-file-upload-size-mb",
       value<int>(&limitFileUploadSizeMb_)->default_value(0),
       "limit of file upload size")
      ("limit-cpu-time-minutes",
       value<int>(&limitCpuTimeMinutes_)->default_value(0),
       "limit on time of top level computations")
      ("limit-xfs-disk-quota",
       value<bool>(&limitXfsDiskQuota_)->default_value(false),
       "limit xfs disk quota");
   
   // external options
   options_description external("external");
   external.add_options()
      ("external-rpostback-path", 
       value<std::string>(&rpostbackPath_)->default_value(kDefaultPostbackPath),
       "Path to rpostback executable")
      ("external-consoleio-path",
       value<std::string>(&consoleIoPath_)->default_value("bin/consoleio.exe"),
       "Path to consoleio executable")
      ("external-gnudiff-path",
       value<std::string>(&gnudiffPath_)->default_value("bin/gnudiff"),
       "Path to gnudiff utilities (windows-only)")
      ("external-gnugrep-path",
       value<std::string>(&gnugrepPath_)->default_value("bin/gnugrep"),
       "Path to gnugrep utilities (windows-only)")
      ("external-msysssh-path",
       value<std::string>(&msysSshPath_)->default_value("bin/msys-ssh-1000-18"),
       "Path to msys_ssh utilities (windows-only)")
      ("external-sumatra-path",
       value<std::string>(&sumatraPath_)->default_value("bin/sumatra"),
       "Path to SumatraPDF (windows-only)")
      ("external-winutils-path",
       value<std::string>(&winutilsPath_)->default_value("bin/winutils"),
       "Path to Hadoop Winutils (windows-only)")
      ("external-hunspell-dictionaries-path",
       value<std::string>(&hunspellDictionariesPath_)->default_value("resources/dictionaries"),
       "Path to hunspell dictionaries")
      ("external-mathjax-path",
        value<std::string>(&mathjaxPath_)->default_value("resources/mathjax-27"),
        "Path to mathjax library")
      ("external-pandoc-path",
        value<std::string>(&pandocPath_)->default_value(kDefaultPandocPath),
        "Path to pandoc binaries")
      ("external-libclang-path",
        value<std::string>(&libclangPath_)->default_value(kDefaultRsclangPath),
        "Path to libclang shared library")
      ("external-libclang-headers-path",
        value<std::string>(&libclangHeadersPath_)->default_value(
                                       "resources/libclang/builtin-headers"),
        "Path to libclang builtin headers")
      ("external-winpty-path",
        value<std::string>(&winptyPath_)->default_value("bin"),
         "Path to winpty binaries");
   
   // git options
   options_description git("git");
   git.add_options()
         ("git-commit-large-file-size",
          value<int>(&gitCommitLargeFileSize_)->default_value(5 * 1024 * 1024),
          "warn when attempting to commit files larger than this size (in bytes; set 0 to disable)");

   // user options (default user identity to current username)
   std::string currentUsername = core::system::username();
   std::string project, scopeId;
   options_description user("user") ;
   user.add_options()
      (kUserIdentitySessionOption "," kUserIdentitySessionOptionShort,
       value<std::string>(&userIdentity_)->default_value(currentUsername),
       "user identity" )
      (kShowUserIdentitySessionOption,
       value<bool>(&showUserIdentity_)->default_value(true),
       "show the user identity")
      (kProjectSessionOption "," kProjectSessionOptionShort,
       value<std::string>(&project)->default_value(""),
       "active project" )
      (kScopeSessionOption "," kScopeSessionOptionShort,
        value<std::string>(&scopeId)->default_value(""),
       "session scope id")
      ("launcher-token",
       value<std::string>(&launcherToken_)->default_value(""),
       "token identifying session launcher");

   // overlay options
   options_description overlay("overlay");
   addOverlayOptions(&overlay);

   // define program options
   FilePath defaultConfigPath("/etc/rstudio/rsession.conf");
   std::string configFile = defaultConfigPath.exists() ?
                            defaultConfigPath.getAbsolutePath() : "";
   core::program_options::OptionsDescription optionsDesc("rsession",
                                                         configFile);

   optionsDesc.commandLine.add(verify);
   optionsDesc.commandLine.add(runTests);
   optionsDesc.commandLine.add(runScript);
   optionsDesc.commandLine.add(program);
   optionsDesc.commandLine.add(log);
   optionsDesc.commandLine.add(agreement);
   optionsDesc.commandLine.add(docs);
   optionsDesc.commandLine.add(www);
   optionsDesc.commandLine.add(session);
   optionsDesc.commandLine.add(allow);
   optionsDesc.commandLine.add(r);
   optionsDesc.commandLine.add(limits);
   optionsDesc.commandLine.add(external);
   optionsDesc.commandLine.add(git);
   optionsDesc.commandLine.add(user);
   optionsDesc.commandLine.add(overlay);

   // define groups included in config-file processing
   optionsDesc.configFile.add(program);
   optionsDesc.configFile.add(log);
   optionsDesc.configFile.add(agreement);
   optionsDesc.configFile.add(docs);
   optionsDesc.configFile.add(www);
   optionsDesc.configFile.add(session);
   optionsDesc.configFile.add(allow);
   optionsDesc.configFile.add(r);
   optionsDesc.configFile.add(limits);
   optionsDesc.configFile.add(external);
   optionsDesc.configFile.add(user);
   optionsDesc.configFile.add(overlay);

   // read configuration
   ProgramStatus status = core::program_options::read(optionsDesc, argc,argv);
   if (status.exit())
      return status;
   
   // make sure the program mode is valid
   if (programMode_ != kSessionProgramModeDesktop &&
       programMode_ != kSessionProgramModeServer)
   {
      LOG_ERROR_MESSAGE("invalid program mode: " + programMode_);
      return ProgramStatus::exitFailure();
   }

   // resolve scope
   scope_ = r_util::SessionScope::fromProjectId(project, scopeId);
   scopeState_ = core::r_util::ScopeValid;

   // call overlay hooks
   resolveOverlayOptions();
   std::string errMsg;
   if (!validateOverlayOptions(&errMsg, osWarnings))
   {
      program_options::reportError(errMsg, ERROR_LOCATION);
      return ProgramStatus::exitFailure();
   }

   // compute program identity
   programIdentity_ = "rsession-" + userIdentity_;

   // provide special home path in temp directory if we are verifying
   bool isLauncherSession = getBoolOverlayOption(kLauncherSessionOption);
   if (verifyInstallation_ && !isLauncherSession)
   {
      // we create a special home directory in server mode (since the
      // user we are running under might not have a home directory)
      // we do not do this for launcher sessions since launcher verification
      // must be run as a specific user with the normal home drive setup
      if (programMode_ == kSessionProgramModeServer)
      {
         verifyInstallationHomeDir_ = "/tmp/rstudio-verify-installation";
         Error error = FilePath(verifyInstallationHomeDir_).ensureDirectory();
         if (error)
         {
            LOG_ERROR(error);
            return ProgramStatus::exitFailure();
         }
         core::system::setenv("R_USER", verifyInstallationHomeDir_);
      }
   }

   // compute user paths
   r_util::SessionType sessionType =
      (programMode_ == kSessionProgramModeDesktop) ?
                                    r_util::SessionTypeDesktop :
                                    r_util::SessionTypeServer;

   r_util::UserDirectories userDirs = r_util::userDirectories(sessionType);
   userHomePath_ = userDirs.homePath;
   userScratchPath_ = userDirs.scratchPath;

   // set HOME if we are in standalone mode (this enables us to reflect
   // R_USER back into HOME on Linux)
   if (standalone())
      core::system::setenv("HOME", userHomePath_);

   // ensure that default working dir and default project dir exist
   ensureDefaultDirectory(&defaultWorkingDir_, userHomePath_);
   ensureDefaultDirectory(&defaultProjectDir_, userHomePath_);

   // session timeout seconds is always -1 in desktop mode
   if (programMode_ == kSessionProgramModeDesktop)
      timeoutMinutes_ = 0;

   // convert string save action default to intenger
   if (saveActionDefault == "yes")
      saveActionDefault_ = r::session::kSaveActionSave;
   else if (saveActionDefault == "no")
      saveActionDefault_ = r::session::kSaveActionNoSave;
   else if (saveActionDefault == "ask" || saveActionDefault.empty())
      saveActionDefault_ = r::session::kSaveActionAsk;
   else
   {
      program_options::reportWarnings(
         "Invalid value '" + saveActionDefault + "' for "
         "session-save-action-default. Valid values are yes, no, and ask.",
         ERROR_LOCATION);
      saveActionDefault_ = r::session::kSaveActionAsk;
   }
   
   // convert relative paths by completing from the app resource path
   resolvePath(resourcePath_, &rResourcesPath_);
   resolvePath(resourcePath_, &agreementFilePath_);
   resolvePath(resourcePath_, &wwwLocalPath_);
   resolvePath(resourcePath_, &wwwSymbolMapsPath_);
   resolvePath(resourcePath_, &coreRSourcePath_);
   resolvePath(resourcePath_, &modulesRSourcePath_);
   resolvePath(resourcePath_, &sessionLibraryPath_);
   resolvePath(resourcePath_, &sessionPackageArchivesPath_);
   resolvePostbackPath(resourcePath_, &rpostbackPath_);
#ifdef _WIN32
   resolvePath(resourcePath_, &consoleIoPath_);
   resolvePath(resourcePath_, &gnudiffPath_);
   resolvePath(resourcePath_, &gnugrepPath_);
   resolvePath(resourcePath_, &msysSshPath_);
   resolvePath(resourcePath_, &sumatraPath_);
   resolvePath(resourcePath_, &winutilsPath_);
   resolvePath(resourcePath_, &winptyPath_);

   // winpty.dll lives next to rsession.exe on a full install; otherwise
   // it lives in a directory named 32 or 64
   core::FilePath pty(winptyPath_);
   std::string completion;
   if (pty.isWithin(resourcePath_))
   {
#ifdef _WIN64
      completion = "winpty.dll";
#else
      completion = "x86/winpty.dll";
#endif
   }
   else
   {
#ifdef _WIN64
      completion = "64/bin/winpty.dll";
#else
      completion = "32/bin/winpty.dll";
#endif
   }
   winptyPath_ = pty.completePath(completion).getAbsolutePath();
#endif // _WIN32
   resolvePath(resourcePath_, &hunspellDictionariesPath_);
   resolvePath(resourcePath_, &mathjaxPath_);
   resolvePath(resourcePath_, &libclangHeadersPath_);
   resolvePandocPath(resourcePath_, &pandocPath_);

   // rsclang
   if (libclangPath_ != kDefaultRsclangPath)
   {
      libclangPath_ += "/5.0.2";
   }
   resolveRsclangPath(resourcePath_, &libclangPath_);

   // shared secret with parent
   secret_ = core::system::getenv("RS_SHARED_SECRET");
   /* SECURITY: Need RS_SHARED_SECRET to be available to
      rpostback. However, we really ought to communicate
      it in a more secure manner than this, at least on
      Windows where even within the same user session some
      processes can have different priviliges (integrity
      levels) than others. For example, using a named pipe
      with proper SACL to retrieve the shared secret, where
      the name of the pipe is in an environment variable. */
   //core::system::unsetenv("RS_SHARED_SECRET");

   // show user home page
   showUserHomePage_ = core::system::getenv(kRStudioUserHomePage) == "1";
   core::system::unsetenv(kRStudioUserHomePage);

   // multi session
   multiSession_ = (programMode_ == kSessionProgramModeDesktop) ||
                   (core::system::getenv(kRStudioMultiSession) == "1");

   // initial working dir override
   initialWorkingDirOverride_ = core::system::getenv(kRStudioInitialWorkingDir);
   core::system::unsetenv(kRStudioInitialWorkingDir);

   // initial environment file override
   initialEnvironmentFileOverride_ = core::system::getenv(kRStudioInitialEnvironment);
   core::system::unsetenv(kRStudioInitialEnvironment);

   // project sharing enabled
   projectSharingEnabled_ =
                core::system::getenv(kRStudioDisableProjectSharing).empty();

   // initial project (can either be a command line param or via env)
   r_util::SessionScope scope = sessionScope();
   if (!scope.empty())
   {
        scopeState_ = r_util::validateSessionScope(
                       scope,
                       userHomePath(),
                       userScratchPath(),
                       session::projectIdToFilePath(userScratchPath(), 
                                 FilePath(getOverlayOption(
                                       kSessionSharedStoragePath))),
                       projectSharingEnabled(),
                       &initialProjectPath_);
   }
   else
   {
      initialProjectPath_ = core::system::getenv(kRStudioInitialProject);
      core::system::unsetenv(kRStudioInitialProject);
   }

   // limit rpc client uid
   limitRpcClientUid_ = -1;
   std::string limitUid = core::system::getenv(kRStudioLimitRpcClientUid);
   if (!limitUid.empty())
   {
      limitRpcClientUid_ = core::safe_convert::stringTo<int>(limitUid, -1);
      core::system::unsetenv(kRStudioLimitRpcClientUid);
   }

   // get R versions path
   rVersionsPath_ = core::system::getenv(kRStudioRVersionsPath);
   core::system::unsetenv(kRStudioRVersionsPath);

   // capture default R version environment variables
   defaultRVersion_ = core::system::getenv(kRStudioDefaultRVersion);
   core::system::unsetenv(kRStudioDefaultRVersion);
   defaultRVersionHome_ = core::system::getenv(kRStudioDefaultRVersionHome);
   core::system::unsetenv(kRStudioDefaultRVersionHome);
   
   // capture auth environment variables
   authMinimumUserId_ = 0;
   if (programMode_ == kSessionProgramModeServer)
   {
      authRequiredUserGroup_ = core::system::getenv(kRStudioRequiredUserGroup);
      core::system::unsetenv(kRStudioRequiredUserGroup);

      authMinimumUserId_ = safe_convert::stringTo<unsigned int>(
                              core::system::getenv(kRStudioMinimumUserId), 100);

#ifndef _WIN32
      r_util::setMinUid(authMinimumUserId_);
#endif
      core::system::unsetenv(kRStudioMinimumUserId);
   }

   // signing key - used for verifying incoming RPC requests
   // in standalone mode
   signingKey_ = core::system::getenv(kRStudioSigningKey);

   if (verifySignatures_)
   {
      // generate our own signing key to be used when posting back to ourselves
      // this key is kept secret within this process and any child processes,
      // and only allows communication from this rsession process and its children
      error = core::system::crypto::generateRsaKeyPair(&sessionRsaPublicKey_, &sessionRsaPrivateKey_);
      if (error)
         LOG_ERROR(error);

      core::system::setenv(kRSessionRsaPublicKey, sessionRsaPublicKey_);
      core::system::setenv(kRSessionRsaPrivateKey, sessionRsaPrivateKey_);
   }

   // load cran options from repos.conf
   FilePath reposFile(rCRANReposFile());
   rCRANMultipleRepos_ = parseReposConfig(reposFile);

   // return status
   return status;
}

std::string Options::parseReposConfig(FilePath reposFile)
{
    using namespace boost::property_tree;

    if (!reposFile.exists())
      return "";

   std::shared_ptr<std::istream> pIfs;
   Error error = FilePath(reposFile).openForRead(pIfs);
   if (error)
   {
      core::program_options::reportError("Unable to open repos file: " + reposFile.getAbsolutePath(),
                  ERROR_LOCATION);

      return "";
   }

   try
   {
      ptree pt;
      ini_parser::read_ini(reposFile.getAbsolutePath(), pt);

      if (!pt.get_child_optional("CRAN"))
      {
         LOG_ERROR_MESSAGE("Repos file " + reposFile.getAbsolutePath() + " is missing CRAN entry.");
         return "";
      }

      std::stringstream ss;

      for (ptree::iterator it = pt.begin(); it != pt.end(); it++)
      {
         if (it != pt.begin())
         {
            ss << "|";
         }

         ss << it->first << "|" << it->second.get_value<std::string>();
      }

      return ss.str();
   }
   catch(const std::exception& e)
   {
      core::program_options::reportError(
         "Error reading " + reposFile.getAbsolutePath() + ": " + std::string(e.what()),
        ERROR_LOCATION);

      return "";
   }
}

bool Options::getBoolOverlayOption(const std::string& name)
{
   std::string optionValue = getOverlayOption(name);
   return boost::algorithm::trim_copy(optionValue) == "1";
}

void Options::resolvePath(const FilePath& resourcePath,
                          std::string* pPath)
{
   if (!pPath->empty())
      *pPath = resourcePath.completePath(*pPath).getAbsolutePath();
}

#ifdef __APPLE__

void Options::resolvePostbackPath(const FilePath& resourcePath,
                                  std::string* pPath)
{
   // On OSX we keep the postback scripts over in the MacOS directory
   // rather than in the Resources directory -- make this adjustment
   // when the default postback path has been passed
   if (*pPath == kDefaultPostbackPath && programMode() == kSessionProgramModeDesktop)
   {
      FilePath path = resourcePath.getParent().completePath("MacOS/postback/rpostback");
      *pPath = path.getAbsolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

void Options::resolvePandocPath(const FilePath& resourcePath,
                                std::string* pPath)
{
   if (*pPath == kDefaultPandocPath && programMode() == kSessionProgramModeDesktop)
   {
      FilePath path = resourcePath.getParent().completePath("MacOS/pandoc");
      *pPath = path.getAbsolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

void Options::resolveRsclangPath(const FilePath& resourcePath,
                                 std::string* pPath)
{
   if (*pPath == kDefaultRsclangPath && programMode() == kSessionProgramModeDesktop)
   {
      FilePath path = resourcePath.getParent().completePath("MacOS/rsclang");
      *pPath = path.getAbsolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

#else

void Options::resolvePostbackPath(const FilePath& resourcePath,
                                  std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}

void Options::resolvePandocPath(const FilePath& resourcePath,
                                  std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}

void Options::resolveRsclangPath(const FilePath& resourcePath,
                                 std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}
#endif
   
} // namespace session
} // namespace rstudio
