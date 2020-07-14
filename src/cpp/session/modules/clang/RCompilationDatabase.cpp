/*
 * RCompilationDatabase.cpp
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

#include "RCompilationDatabase.hpp"

#include <algorithm>
#include <gsl/gsl>

#include <boost/format.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/trim_all.hpp>

#include <core/Debug.hpp>
#include <core/Hash.hpp>
#include <core/Algorithm.hpp>
#include <core/PerformanceTimer.hpp>
#include <core/FileSerializer.hpp>

#include <core/r_util/RToolsInfo.hpp>

#include <core/system/ProcessArgs.hpp>
#include <core/system/FileScanner.hpp>

#include <core/libclang/LibClang.hpp>

#include <r/RExec.hpp>
#include <r/RVersionInfo.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "CodeCompletion.hpp"
#include "RSourceIndex.hpp"

using namespace rstudio::core ;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

LibClang& clang()
{
   return libclang::clang();
}


struct SourceCppFileInfo
{
   SourceCppFileInfo() : disableIndexing(false) {}
   bool empty() const { return hash.empty(); }
   std::string hash;
   std::string rcppPkg;
   bool disableIndexing;
};

SourceCppFileInfo sourceCppFileInfo(const core::FilePath& srcPath)
{
   // read file
   std::string contents;
   Error error = core::readStringFromFile(srcPath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return SourceCppFileInfo();
   }

   // info to return
   SourceCppFileInfo info;

   // check for rcpp 11
   boost::regex reRcpp11("#include\\s+<Rcpp11");
   bool isRcpp11 = regex_utils::search(contents, reRcpp11);
   info.rcppPkg = isRcpp11 ? "Rcpp11" : "Rcpp";
   info.hash.append(info.rcppPkg);

   // find dependency attributes
   boost::regex re(
            "^\\s*//\\s*\\[\\[Rcpp::(\\w+)(\\(.*?\\))?\\]\\]\\s*$");
   try
   {
      boost::sregex_token_iterator it(contents.begin(), contents.end(), re, 0);
      boost::sregex_token_iterator end;
      for ( ; it != end; ++it)
      {
         std::string attrib = *it;
         boost::algorithm::trim_all(attrib);
         info.hash.append(attrib);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   // using RcppNT2/Boost.SIMD means don't index (expression templates
   // are too much for the way we do indexing)
   if (boost::algorithm::contains(info.hash, "RcppNT2"))
      info.disableIndexing = true;

   // return info
   return info;
}

std::vector<std::string> extractCompileArgs(const std::string& line)
{
   std::vector<std::string> compileArgs;

   // find arguments libclang might care about
   // (we implement a poor man's shell arguments parser here:
   // consider a true solution using e.g. a tokenizer)
   try
   {
      boost::regex re(
               "([ \\t])"                           // look for preceding space
               "(-(?:isysroot|isystem|std|[IDif]))" // find flags we care about
               "([ \\t]+)?"                         // allow for optional whitespace
               "(\\\"[^\\\"]+\\\"|[^ ]+)");         // parse the argument passed

      boost::sregex_iterator it(line.begin(), line.end(), re);
      boost::sregex_iterator end;
      for ( ; it != end; ++it)
      {
         boost::smatch match = *it;

         std::string whitespace = match[3];
         if (whitespace.empty())
         {
            std::string argument = match[2] + match[4];
            boost::algorithm::replace_all(argument, "\"", "");
            compileArgs.push_back(argument);
         }
         else
         {
            std::string first = match[2];
            boost::algorithm::replace_all(first, "\"", "");
            compileArgs.push_back(first);

            std::string second = match[4];
            boost::algorithm::replace_all(second, "\"", "");
            compileArgs.push_back(second);
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   return compileArgs;
}

std::string extractStdArg(const std::vector<std::string>& args)
{
   for (const std::string& arg : args)
   {
      if (boost::algorithm::starts_with(arg, "-std="))
         return arg;
   }

   return std::string();
}

std::string buildFileHash(const FilePath& filePath)
{
   if (filePath.exists())
   {
      std::ostringstream ostr;
      ostr << filePath.getLastWriteTime();
      return ostr.str();
   }
   else
   {
      return std::string();
   }
}

std::string packageBuildFileHash()
{
   std::ostringstream ostr;
   
   using namespace module_context;
   ostr << buildFileHash(resolveAliasedPath("~/.R/Makevars"));
   ostr << buildFileHash(resolveAliasedPath("~/.R/Makevars.win"));
   
   FilePath buildPath = projects::projectContext().buildTargetPath();
   ostr << buildFileHash(buildPath.completeChildPath("DESCRIPTION"));
   
   FilePath srcPath = buildPath.completeChildPath("src");
   if (srcPath.exists())
   {
      ostr << buildFileHash(srcPath.completeChildPath("Makevars"));
      ostr << buildFileHash(srcPath.completeChildPath("Makevars.win"));
   }
   
   return ostr.str();
}

std::vector<std::string> parseCompilationResults(const std::string& results)
{
   // compile args to return
   std::vector<std::string> compileArgs;

   // break into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, results,
                           boost::algorithm::is_any_of("\r\n"));


   // find the line with the compilation and add it's args
   boost::regex re("-c [^\\.]+\\.c\\w* -o");
   for (const std::string& line : lines)
   {
      if (regex_utils::search(line, re))
      {
         std::vector<std::string> args = extractCompileArgs(line);
         std::copy(args.begin(), args.end(), std::back_inserter(compileArgs));
         break;
      }
   }

   // return the args
   return compileArgs;
}

std::string packagePCH(const std::string& linkingTo)
{
   std::string pch;
   r::exec::RFunction func(".rs.packagePCH", linkingTo);
   Error error = func.call(&pch);
   if (error)
   {
      error.addProperty("linking-to", linkingTo);
      LOG_ERROR(error);
   }

   if (rSourceIndex().verbose() > 0)
   {
      std::cerr << "PACKAGE PCH: " << pch << std::endl;
   }

   return pch;
}

bool packageIsCpp(const std::string& linkingTo, const FilePath& srcDir)
{
   if (boost::algorithm::contains(linkingTo, "Rcpp"))
   {
      return true;
   }
   else
   {
      std::vector<FilePath> allSrcFiles;
      Error error = srcDir.getChildren(allSrcFiles);
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }

      for (const FilePath& srcFile : allSrcFiles)
      {
         std::string ext = srcFile.getExtensionLowerCase();
         if (ext == ".cpp" || ext == ".cc")
            return true;
      }

      return false;
   }
}

std::vector<std::string> includesForLinkingTo(const std::string& linkingTo)
{
   std::vector<std::string> includes;
   r::exec::RFunction func(".rs.includesForLinkingTo", linkingTo);
   Error error = func.call(&includes);
   if (error)
   {
      error.addProperty("linking-to", linkingTo);
      LOG_ERROR(error);
   }
   return includes;
}

} // anonymous namespace


RCompilationDatabase::RCompilationDatabase()
   : usePrecompiledHeaders_(true),
     forceRebuildPrecompiledHeaders_(false),
     restoredCompilationConfig_(false)
{
}

void RCompilationDatabase::updateForCurrentPackage()
{
   // one time restore of compilation config
   if (!restoredCompilationConfig_)
   {
      restorePackageCompilationConfig();
      restoredCompilationConfig_ = true;
   }

   // check hash to see if we can avoid this computation
   std::string buildFileHash = packageBuildFileHash();
   if (buildFileHash == packageBuildFileHash_)
      return;

   // compilation config has changed; rebuild pch
   forceRebuildPrecompiledHeaders_ = true;
   
   // start with base args
   bool isCpp = true;
   core::r_util::RPackageInfo pkgInfo;
   std::vector<std::string> args = packageCompilationArgs(&pkgInfo, &isCpp);
   if (!args.empty())
   {
      // set the args and build file hash (to avoid recomputation)
      packageCompilationConfig_.args = args;
      packageCompilationConfig_.PCH = packagePCH(pkgInfo.linkingTo());
      packageCompilationConfig_.isCpp = isCpp;
      packageBuildFileHash_ = buildFileHash;

      // save them to disk
      savePackageCompilationConfig();
   }

}

std::vector<std::string> RCompilationDatabase::compileArgsForPackage(
      const core::system::Options& env,
      const FilePath& srcDir,
      bool isCpp)
{
   // create a temp dir to call R CMD SHLIB within
   FilePath tempDir = module_context::tempFile(kCompilationDbPrefix, "dir");
   Error error = tempDir.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return {};
   }

   // copy Makevars to tempdir if it exists
   FilePath makevarsPath = srcDir.completeChildPath("Makevars");
   if (makevarsPath.exists())
   {
      Error error = makevarsPath.copy(tempDir.completeChildPath("Makevars"));
      if (error)
      {
         LOG_ERROR(error);
         return {};
      }
   }

   FilePath makevarsWinPath = srcDir.completeChildPath("Makevars.win");
   if (makevarsWinPath.exists())
   {
      Error error = makevarsWinPath.copy(tempDir.completeChildPath("Makevars.win"));
      if (error)
      {
         LOG_ERROR(error);
         return {};
      }
   }

   // try to generate an appropriate name for the C++ source file.
   // if we have Makevars / Makevars.site, they may define OBJECT
   // targets; if we pick a file name not matching any OBJECT target
   // then R CMD SHLIB will fail. (technically this implies that we
   // need OBJECT-specific compilation configs but in practice one
   // often just enumerates each OBJECT explicitly and re-uses the
   // same compilation config for each file)
   std::string ext = isCpp ? ".cpp" : ".c";
   std::string filename = kCompilationDbPrefix + core::system::generateUuid() + ext;

   std::vector<FilePath> children;
   srcDir.getChildren(children);
   for (const FilePath& child : children)
   {
      if (child.getExtension() == ext)
      {
         filename = child.getFilename();
         break;
      }
   }

   // call R CMD SHLIB on a temp file to capture the compilation args
   FilePath tempSrcFile = tempDir.completeChildPath(filename);
   std::vector<std::string> compileArgs = argsForRCmdSHLIB(env, tempSrcFile);

   // remove the tempDir
   error = tempDir.remove();
   if (error)
      LOG_ERROR(error);

   // return the compileArgs
   return compileArgs;
}


namespace {

FilePath compilationConfigFilePath()
{
   return module_context::scopedScratchPath().completePath("cpp-compilation-config");
}


} // anonymous namespace

void RCompilationDatabase::savePackageCompilationConfig()
{
   json::Object configJson;
   configJson["args"] = json::toJsonArray(packageCompilationConfig_.args);
   configJson["pch"] = packageCompilationConfig_.PCH;
   configJson["is_cpp"] = packageCompilationConfig_.isCpp;
   configJson["hash"] = packageBuildFileHash_;

   Error error = writeStringToFile(compilationConfigFilePath(), configJson.writeFormatted());
   if (error)
      LOG_ERROR(error);
}

void RCompilationDatabase::restorePackageCompilationConfig()
{
   FilePath configFilePath = compilationConfigFilePath();
   if (!configFilePath.exists())
      return;

   std::string contents;
   Error error = readStringFromFile(compilationConfigFilePath(), &contents);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Value configJson;
   if (configJson.parse(contents) ||
       !json::isType<json::Object>(configJson))
   {
      LOG_ERROR_MESSAGE("Error parsing compilation config: " + contents);
      return;
   }

   json::Array argsJson;
   error = json::readObject(configJson.getObject(),
                            "args", &argsJson,
                            "pch", &packageCompilationConfig_.PCH,
                            "is_cpp", &packageCompilationConfig_.isCpp,
                            "hash", &packageBuildFileHash_);
   if (error)
   {
      error.addProperty("json", contents);
      LOG_ERROR(error);
      return;
   }

   packageCompilationConfig_.args.clear();
   for (const json::Value& argJson : argsJson)
   {
      if (json::isType<std::string>(argJson))
         packageCompilationConfig_.args.push_back(argJson.getString());
   }
}

void RCompilationDatabase::updateForSourceCpp(const core::FilePath& srcFile)
{
   // read the the source cpp hash for this file
   SourceCppFileInfo info = sourceCppFileInfo(srcFile);

   // check if we already have the args for this hash value
   std::string filename = srcFile.getAbsolutePath();
   SourceCppHashes::const_iterator it = sourceCppHashes_.find(filename);
   if (it != sourceCppHashes_.end() && it->second == info.hash)
      return;

   // if there is no info then bail
   if (info.empty())
      return;

   // if we are disabling indexing then bail
   if (info.disableIndexing)
   {
      if (rSourceIndex().verbose() > 0)
         std::cerr << "CLANG SKIP INDEXING: " << srcFile << std::endl;

      return;
   }

   // get config
   CompilationConfig config = configForSourceCpp(info.rcppPkg, srcFile);

   // save it
   if (!config.empty())
   {
      // update map
      sourceCppConfigMap_[filename] = config;

      // save hash to prevent recomputation
      sourceCppHashes_[filename] = info.hash;
   }
}


Error RCompilationDatabase::executeSourceCpp(
                                      core::system::Options env,
                                      const std::string& rcppPkg,
                                      const core::FilePath& srcPath,
                                      core::system::ProcessResult* pResult)
{
   // get path to R script
   FilePath rScriptPath;
   Error error = module_context::rScriptPath(&rScriptPath);
   if (error)
      return error;

   // establish options
   core::system::ProcessOptions options;

   // always run as a slave
   std::vector<std::string> args;
   args.push_back("--slave");

   // for packrat projects we execute the profile and set the working
   // directory to the project directory; for other contexts we just
   // propagate the R_LIBS
   if (module_context::packratContext().modeOn)
   {
      options.workingDir = projects::projectContext().directory();
      args.push_back("--no-save");
      args.push_back("--no-restore");
   }
   else
   {
      args.push_back("--vanilla");
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&env, "R_LIBS", libPaths);
   }

   // execute code
   args.push_back("-e");

   // difference sequence depending on the version of Rcpp we are using
   if (rcppPkg == "Rcpp")
   {
      // we try to force --dry-run differently depending on the version of Rcpp
      std::string extraParams;
      if (module_context::isPackageVersionInstalled("Rcpp", "0.11.3"))
         extraParams = ", dryRun = TRUE";
      else
         core::system::setenv(&env, "MAKE", "make --dry-run");

      // add command to arguments
      boost::format fmt("Rcpp::sourceCpp('%1%', showOutput = TRUE%2%)");
      args.push_back(boost::str(fmt % srcPath.getAbsolutePath() % extraParams));
   }
   else
   {
      core::system::setenv(&env, "MAKE", "make --dry-run");
      boost::format fmt("attributes::sourceCpp('%1%', verbose = TRUE)");
      args.push_back(boost::str(fmt % srcPath.getAbsolutePath()));
   }


   // set environment into options
   options.environment = env;

   // execute and capture output
   return core::system::runProgram(
            core::string_utils::utf8ToSystem(rScriptPath.getAbsolutePath()),
            args,
            "",
            options,
            pResult);
}

core::Error RCompilationDatabase::executeRCmdSHLIB(
                                 core::system::Options env,
                                 const core::FilePath& srcPath,
                                 core::system::ProcessResult* pResult)
{
   // get R bin directory
   FilePath rBinDir;
   Error error = module_context::rBinDir(&rBinDir);
   if (error)
      return error;

   // compile the file as dry-run
   module_context::RCommand rCmd(rBinDir);
   rCmd << "SHLIB";
   rCmd << "--dry-run";
   rCmd << srcPath.getFilename();

   // set options and run
   core::system::ProcessOptions options;
   options.workingDir = srcPath.getParent();
   options.environment = env;
   Error result = core::system::runCommand(
            rCmd.shellCommand(),
            options,
            pResult);
   return result;
}

bool RCompilationDatabase::isProjectTranslationUnit(
                                          const std::string& filename) const
{
   using namespace projects;

   if (projectContext().config().buildType != r_util::kBuildTypePackage)
      return false;

   FilePath filePath(filename);
   FilePath pkgPath = projectContext().buildTargetPath();
   FilePath srcDirPath = pkgPath.completeChildPath("src");
   FilePath includePath = pkgPath.completeChildPath("inst/include");
   return
         filePath.isWithin(srcDirPath) ||
         filePath.isWithin(includePath);
}

namespace {

// allow only non-hidden directories and source files through
// the translation filters (we'll remove the directories
// in a separate pass)
bool translationUnitFilter(const FileInfo& fileInfo,
                           const FilePath& pkgSrcDir,
                           const FilePath& pkgIncludeDir)
{
   FilePath filePath(fileInfo.absolutePath());
   if (core::system::isHiddenFile(fileInfo))
      return false;
   else if (filePath.isDirectory())
      return true;
   else
      return isIndexableFile(fileInfo, pkgSrcDir, pkgIncludeDir);
}

bool isNotDirectory(const FileInfo& fileInfo)
{
   return !fileInfo.isDirectory();
}

} // anonymous namespace

std::vector<std::string> RCompilationDatabase::projectTranslationUnits() const
{
   // units to return
   std::vector<std::string> units;

   using namespace projects;
   using namespace rstudio::core::system;
   if (projectContext().hasProject())
   {
      // setup options for file scanning (including filter)
      FilePath pkgPath = projectContext().buildTargetPath();
      FilePath srcDirPath = pkgPath.completeChildPath("src");
      FilePath includePath = pkgPath.completeChildPath("inst/include");
      FileScannerOptions options;
      options.recursive = true;
      options.filter =
            boost::bind(translationUnitFilter, _1, srcDirPath, includePath);

      // scan the files
      tree<FileInfo> files;
      Error error = scanFiles(FileInfo(pkgPath), options, &files);
      if (error)
         LOG_ERROR(error);

      // copy them to the out vector if they aren't directories
      core::algorithm::copy_transformed_if(
                              files.begin(),
                              files.end(),
                              std::back_inserter(units),
                              isNotDirectory,
                              fileInfoAbsolutePath);
   }

   return units;
}

void RCompilationDatabase::rebuildPackageCompilationDatabase()
{
   packageBuildFileHash_.clear();
}

bool RCompilationDatabase::shouldIndexConfig(const CompilationConfig& config)
{
   // no args
   if (config.args.empty())
      return false;

   // using RcppNT2/Boost.SIMD means don't index (expression templates
   // are too much for the way we do indexing)
   for (const std::string& arg : config.args)
   {
      if (boost::algorithm::contains(arg, "RcppNT2"))
         return false;
   }

   return true;
}


std::vector<std::string> RCompilationDatabase::compileArgsForTranslationUnit(
       const std::string& filename, bool usePrecompiledHeaders)
{
   // args to return
   std::vector<std::string> args;

   // get a file path object
   FilePath filePath(filename);

   // if this is a package source file then return the package args
   CompilationConfig config;
   if (isProjectTranslationUnit(filePath.getAbsolutePath()))
   {
      // (re-)create on demand
      updateForCurrentPackage();

      // if we have args then capture them
      config = packageCompilationConfig_;
   }
   // otherwise lookup in the global dictionary
   else
   {
      // (re-)create on demand
      updateForSourceCpp(filePath);

      // if we have args then capture them
      std::string filename = filePath.getAbsolutePath();
      ConfigMap::const_iterator it = sourceCppConfigMap_.find(filename);
      if (it != sourceCppConfigMap_.end())
         config = it->second;
   }

   // bail if we aren't able to index this config
   if (!shouldIndexConfig(config))
   {
      if (rSourceIndex().verbose() > 0)
         std::cerr << "CLANG SKIP INDEXING: " << filename << std::endl;

      return std::vector<std::string>();
   }

   // copy the args
   std::copy(config.args.begin(), config.args.end(), std::back_inserter(args));

   // add precompiled headers if necessary
   if (usePrecompiledHeaders && usePrecompiledHeaders_ &&
       !config.PCH.empty() && config.isCpp &&
       (filePath.getExtensionLowerCase() != ".c") &&
       (filePath.getExtensionLowerCase() != ".m"))
   {
      // extract any -std= argument
      std::vector<std::string> pchArgs = precompiledHeaderArgs(config);
      std::copy(pchArgs.begin(),
                pchArgs.end(),
                std::back_inserter(args));
   }

   // if this is a .h file and it's a C++ config then force C++ for
   // libclang (this is necessary because many C++ header files in
   // the R ecosystem use .h
   if ((filePath.getExtensionLowerCase() == ".h") && config.isCpp)
   {
      args.push_back("-x");
      args.push_back("c++");
   }

   // return args
   return args;
}

RCompilationDatabase::CompilationConfig
         RCompilationDatabase::configForSourceCpp(const std::string& rcppPkg,
                                                  FilePath srcFile)
{
   // validation: if this is Rcpp11 and we don't have the attributes
   // package then there's no way for us to execute sourceCpp
   using namespace module_context;
   if (rcppPkg == "Rcpp11" && !isPackageInstalled("attributes"))
      return CompilationConfig();

   // validation: if we don't have any version of Rcpp installed then
   // we can't do sourceCpp
   if (rcppPkg == "Rcpp" && !isPackageVersionInstalled("Rcpp", "0.10.1"))
      return CompilationConfig();

   // start with base args
   std::vector<std::string> args = baseCompilationArgs(true);

   // if this is a header file we need to rename it as a temporary .cpp
   // file so that R CMD SHLIB is willing to compile it
   FilePath tempSrcFile = srcFile.getParent().completeChildPath(
      kCompilationDbPrefix + core::system::generateUuid() + ".cpp");
   RemoveOnExitScope removeOnExit(tempSrcFile, ERROR_LOCATION);
   if (SourceIndex::isHeaderFile(srcFile))
   {
      Error error = srcFile.copy(tempSrcFile);
      if (error)
      {
         LOG_ERROR(error);
         return CompilationConfig();
      }
      srcFile = tempSrcFile;
   }

   // execute sourceCpp
   core::system::ProcessResult result;
   core::system::Options env = compilationEnvironment();
   Error error = executeSourceCpp(env, rcppPkg, srcFile, &result);
   if (error)
   {
      LOG_ERROR(error);
      return CompilationConfig();
   }

   // parse the compilation results
   std::vector<std::string> compileArgs = parseCompilationResults(
                                                           result.stdOut);

   std::copy(compileArgs.begin(),
             compileArgs.end(),
             std::back_inserter(args));

   CompilationConfig config;
   config.args = args;
   config.PCH = rcppPkg;
   config.isCpp = true;
   return config;

}

std::vector<std::string> RCompilationDatabase::argsForRCmdSHLIB(
                                          core::system::Options env,
                                          FilePath tempSrcFile)
{
   Error error = core::writeStringToFile(tempSrcFile, "void foo() {}\n");
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }

   // execute R CMD SHLIB
   core::system::ProcessResult result;
   error = executeRCmdSHLIB(env, tempSrcFile, &result);

   // remove the temporary source file
   Error removeError = tempSrcFile.remove();
   if (removeError)
      LOG_ERROR(removeError);

   // process results of R CMD SHLIB
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE("Error performing R CMD SHLIB: " + result.stdErr);
      return std::vector<std::string>();
   }
   else
   {
      // parse the compilation results
      return parseCompilationResults(result.stdOut);
   }
}


std::vector<std::string> RCompilationDatabase::baseCompilationArgs(bool isCpp)
                                                                          const
{
   std::vector<std::string> args = clang().compileArgs(isCpp);

   // add system include headers as reported by compiler
#ifndef _WIN32
   std::vector<std::string> includes;
   discoverSystemIncludePaths(&includes);
   for (auto include : includes)
      args.push_back("-I" + include);
#endif

   return args;
}

std::vector<std::string> RCompilationDatabase::packageCompilationArgs(
      core::r_util::RPackageInfo* pPkgInfo,
      bool* pIsCpp)
{
   // start with base args
   std::vector<std::string> args = baseCompilationArgs(true);

   // read the package description file
   using namespace projects;
   FilePath pkgPath = projectContext().buildTargetPath();
   core::r_util::RPackageInfo pkgInfo;
   Error error = pkgInfo.read(pkgPath);
   if (error)
   {
      LOG_ERROR(error);
      return {};
   }

   // Discover all of the LinkingTo relationships and add -I
   // arguments for them
   if (!pkgInfo.linkingTo().empty())
   {
      // Get includes implied by the LinkingTo field
      std::vector<std::string> includes = includesForLinkingTo(
               pkgInfo.linkingTo());

      // add them to args
      std::copy(includes.begin(), includes.end(), std::back_inserter(args));
   }

   // get the build environment (e.g. Rtools config)
   core::system::Options env = compilationEnvironment();

   // Check for C++11 in SystemRequirements
   if (boost::algorithm::icontains(pkgInfo.systemRequirements(), "C++11"))
   {
      env.push_back(std::make_pair("USE_CXX1X", "1"));
      env.push_back(std::make_pair("USE_CXX11", "1"));
   }
   else if (boost::algorithm::icontains(pkgInfo.systemRequirements(), "C++14"))
   {
      env.push_back(std::make_pair("USE_CXX1Y", "1"));
      env.push_back(std::make_pair("USE_CXX14", "1"));
   }
   else if (boost::algorithm::icontains(pkgInfo.systemRequirements(), "C++17"))
   {
      env.push_back(std::make_pair("USE_CXX1Z", "1"));
      env.push_back(std::make_pair("USE_CXX17", "1"));
   }

   // Run R CMD SHLIB
   FilePath srcDir = pkgPath.completeChildPath("src");
   bool isCpp = packageIsCpp(pkgInfo.linkingTo(), srcDir);
   std::vector<std::string> compileArgs = compileArgsForPackage(env, srcDir, isCpp);
   if (!compileArgs.empty())
   {
      // do path substitutions
      for (std::string arg : compileArgs)
      {
         // do path substitutions
         boost::algorithm::replace_first(
                  arg,
                  "-I..",
                  "-I" + srcDir.getParent().getAbsolutePath());
         boost::algorithm::replace_first(
                  arg,
                  "-I.",
                  "-I" + srcDir.getAbsolutePath());

         args.push_back(arg);
      }
   }

   if (pPkgInfo)
      *pPkgInfo = pkgInfo;

   if (pIsCpp)
      *pIsCpp = isCpp;

   return args;

}

std::vector<std::string> RCompilationDatabase::rToolsArgs() const
{

#ifdef _WIN32
   if (rToolsArgs_.empty())
   {
      // Rtools 4.0 will set RTOOLS40_HOME
      std::string rtoolsHomeEnvVar;
      auto rVersion = r::version_info::currentRVersion();
      if (rVersion.versionMajor() == 4)
         rtoolsHomeEnvVar = "RTOOLS40_HOME";

      // scan for Rtools
      bool usingMingwGcc49 = module_context::usingMingwGcc49();
      std::vector<core::r_util::RToolsInfo> rTools;
      core::r_util::scanForRTools(usingMingwGcc49, rtoolsHomeEnvVar, &rTools);

      // enumerate them to see if we have a compatible version
      // (go in reverse order for most recent first)
      std::vector<r_util::RToolsInfo>::const_reverse_iterator it = rTools.rbegin();
      for ( ; it != rTools.rend(); ++it)
      {
         if (module_context::isRtoolsCompatible(*it))
         {
            std::vector<std::string> clangArgs = it->clangArgs();
            std::copy(clangArgs.begin(),
                      clangArgs.end(),
                      std::back_inserter(rToolsArgs_));
            break;
         }
      }
   }
#endif

   return rToolsArgs_;
}

core::system::Options RCompilationDatabase::compilationEnvironment() const
{
   // rtools on windows
   core::system::Options env;
   core::system::environment(&env);
#if defined(_WIN32)
   std::string warning;
   module_context::addRtoolsToPathIfNecessary(&env, &warning);
#endif
   return env;
}

namespace {

FilePath precompiledHeaderDir(const std::string& pkgName)
{
   return module_context::tempDir().completeChildPath(
      "rstudio/libclang/precompiled/"
      + pkgName);
}

} // anonymous namespace

std::vector<std::string> RCompilationDatabase::precompiledHeaderArgs(
      const CompilationConfig& config)
{
   // args to return
   std::vector<std::string> args;

   // precompiled header dir
   std::string pkgName = config.PCH;
   FilePath precompiledDir = precompiledHeaderDir(pkgName);

   // further scope to actual path of package (as the locations of the
   // header files must be stable)
   std::string pkgPath;
   Error error = r::exec::RFunction("find.package")
         .addParam(pkgName)
         .addParam("quiet", true)
         .call(&pkgPath);
   
   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }
   
   pkgPath = core::hash::crc32HexHash(pkgPath);
   precompiledDir = precompiledDir.completeChildPath(pkgPath);

   // platform/rcpp version specific directory name
   std::string clangVersion = clang().version().asString();
   std::string platformDir;
   error = r::exec::RFunction(".rs.clangPCHPath")
         .addParam(pkgName)
         .addParam(clangVersion)
         .call(&platformDir);

   if (error)
   {
      LOG_ERROR(error);
      return std::vector<std::string>();
   }

   // if this path doesn't exist then blow away all precompiled paths
   // and re-create this one. this enforces only storing precompiled headers
   // for the current version of R/Rcpp/pkg -- if we didn't do this then the
   // storage cost could really pile up over time (~25MB per PCH)
   FilePath platformPath = precompiledDir.completeChildPath(platformDir);
   if (!platformPath.exists())
   {
      // delete root directory
      Error error = precompiledDir.removeIfExists();
      if (error)
      {
         LOG_ERROR(error);
         return std::vector<std::string>();
      }

      // create platform directory
      error = platformPath.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return std::vector<std::string>();
      }
   }

   // now create the PCH if we need to
   std::string stdArg = extractStdArg(config.args);
   FilePath pchPath = platformPath.completeChildPath(pkgName + stdArg + ".pch");
   if (forceRebuildPrecompiledHeaders_ || !pchPath.exists())
   {
      forceRebuildPrecompiledHeaders_ = false;
      
      // state cpp file for creating precompiled headers
      FilePath cppPath = platformPath.completeChildPath(pkgName + stdArg + ".cpp");
      boost::format fmt("#include <%1%.h>\n");
      std::string contents = boost::str(fmt % pkgName);
      error = core::writeStringToFile(cppPath, contents);
      if (error)
      {
         LOG_ERROR(error);
         return std::vector<std::string>();
      }

      // get common compilation args
      std::vector<std::string> args = config.args;
      
      // add this package's path to the args
      std::vector<std::string> pkgArgs = includesForLinkingTo(pkgName);
      std::copy(pkgArgs.begin(), pkgArgs.end(), std::back_inserter(args));

      // enforce compilation with requested standard
      core::algorithm::expel_if(args, [](const std::string& arg) {
         return arg.find("-std=") == 0;
      });

      // add in '-std' argument (if any)
      if (!stdArg.empty())
         args.push_back(stdArg);

      // create args array
      if (rSourceIndex().verbose() > 0)
      {
         std::cerr << "GENERATING PRECOMPILED HEADERS:" << std::endl;
         core::debug::print(args);
      }

      core::system::ProcessArgs argsArray(args);

      int verboseCompile = (rSourceIndex().verbose() > 1) ? 1 : 0;
      CXIndex index = clang().createIndex(0, verboseCompile);

      CXTranslationUnit tu = clang().parseTranslationUnit(
                            index,
                            cppPath.getAbsolutePath().c_str(),
                            argsArray.args(),
                            gsl::narrow_cast<int>(argsArray.argCount()),
                            nullptr,
                            0,
                            CXTranslationUnit_ForSerialization);
      if (tu == nullptr)
      {
         LOG_ERROR_MESSAGE("Error parsing translation unit " +
                              cppPath.getAbsolutePath());
         clang().disposeIndex(index);

         Error removeError = precompiledDir.removeIfExists();
         if (removeError)
            LOG_ERROR(removeError);

         return std::vector<std::string>();
      }

      int ret = clang().saveTranslationUnit(tu,
                                            pchPath.getAbsolutePath().c_str(),
                                            clang().defaultSaveOptions(tu));
      if (ret != CXSaveError_None)
      {
         boost::format fmt("Error %1% saving translation unit %2%");
         std::string msg = boost::str(fmt % ret % pchPath.getAbsolutePath());
         LOG_ERROR_MESSAGE(msg);

         Error removeError = precompiledDir.removeIfExists();
         if (removeError)
            LOG_ERROR(removeError);
      }

      clang().disposeTranslationUnit(tu);

      clang().disposeIndex(index);
   }

   // return the pch header file args
   args.push_back("-include-pch");
   args.push_back(pchPath.getAbsolutePath());
   return args;
}

core::libclang::CompilationDatabase rCompilationDatabase()
{
   static RCompilationDatabase instance;

   CompilationDatabase compilationDatabase;

   compilationDatabase.hasTranslationUnit =
      boost::bind(&RCompilationDatabase::isProjectTranslationUnit,
                  &instance, _1);
   compilationDatabase.translationUnits =
      boost::bind(&RCompilationDatabase::projectTranslationUnits,
                  &instance);
   compilationDatabase.compileArgsForTranslationUnit =
      boost::bind(&RCompilationDatabase::compileArgsForTranslationUnit,
                  &instance, _1, _2);
   compilationDatabase.rebuildPackageCompilationDatabase =
         boost::bind(&RCompilationDatabase::rebuildPackageCompilationDatabase,
                     &instance);

   return compilationDatabase;
}


} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

