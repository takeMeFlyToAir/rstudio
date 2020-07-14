/*
 * Xdg.cpp
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

#ifdef _WIN32
#include <Windows.h>
#include <ShlObj_core.h>
#include <KnownFolders.h>
#endif

#include <core/Algorithm.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace xdg {
namespace {

FilePath resolveXdgDir(const std::string& envVar, 
#ifdef _WIN32
      const GUID windowsFolderId,
#endif
      const std::string& defaultDir)
{
   FilePath xdgHome;
   std::string env = getenv(envVar);
   if (env.empty())
   {
      // No root specified for xdg home; we will need to generate one.
#ifdef _WIN32
      // On Windows, the default path is in Application Data/Roaming.
      wchar_t *path = nullptr;
      HRESULT hr = ::SHGetKnownFolderPath(
            windowsFolderId,
            0,
            nullptr, // current user
            &path);

      if (hr == S_OK)
      {
         xdgHome = FilePath(std::wstring(path));
      }
      else
      {
         LOG_ERROR_MESSAGE("Unable to retrieve app settings path. HRESULT:  " +
                           safe_convert::numberToString(hr));
      }

      // Free memory if allocated
      if (path != nullptr)
      {
         ::CoTaskMemFree(path);
      }

#endif
      if (xdgHome.isEmpty())
      {
         // Use the default subdir for POSIX. We also use it a fallback on Windows if we couldn't
         // read the app settings path.
         xdgHome = FilePath::resolveAliasedPath(defaultDir, userHomePath());
      }
   }
   else
   {
      // We have a manually specified xdg directory from an environment variable.
      xdgHome = FilePath(env);
   }

   return xdgHome.completePath(
#ifdef _WIN32
      "RStudio"
#else
      "rstudio"
#endif
   );
}

} // anonymous namespace

FilePath userConfigDir()
{
   return resolveXdgDir("XDG_CONFIG_HOME", 
#ifdef _WIN32
         FOLDERID_RoamingAppData,
#endif
         "~/.config"
   );
}

FilePath userDataDir()
{
   return resolveXdgDir("XDG_DATA_HOME", 
#ifdef _WIN32
         FOLDERID_LocalAppData,
#endif
         "~/.local/share"
   );
}

FilePath systemConfigDir()
{
#ifndef _WIN32
   // On POSIX operating systems, it's possible to specify multiple config directories. We don't
   // support reading from multiple directories, so read the list and take the first one that
   // contains an "rstudio" folder.
   std::string env = getenv("XDG_CONFIG_DIRS");
   if (env.find_first_of(":") != std::string::npos)
   {
      std::vector<std::string> dirs = algorithm::split(env, ":");
      for (const std::string& dir: dirs)
      {
         FilePath resolved = FilePath(dir).completePath("rstudio");
         if (resolved.exists())
         {
            return resolved;
         }
      }
   }
#endif
   return resolveXdgDir("XDG_CONFIG_DIRS", 
#ifdef _WIN32
         FOLDERID_ProgramData,
#endif
         "/etc"
   );
}


} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

