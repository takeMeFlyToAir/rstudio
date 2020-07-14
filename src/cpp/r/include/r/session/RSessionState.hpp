/*
 * RSessionState.hpp
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

#ifndef R_R_SESSION_STATE_HPP
#define R_R_SESSION_STATE_HPP

#include <string>

#include <boost/function.hpp>

#include <shared_core/Error.hpp>
#include <core/Version.hpp>

namespace rstudio {
namespace core {
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace state {

struct SessionStateInfo
{
   core::Version suspendedRVersion;
   core::Version activeRVersion;
};

bool save(const core::FilePath& statePath,
          bool serverMode,
          bool excludePackages,
          bool disableSaveCompression,
          const std::string& envVarSaveBlacklist);

bool saveMinimal(const core::FilePath& statePath,
                 bool saveGlobalEnvironment);
   

bool rProfileOnRestore(const core::FilePath& statePath);

bool packratModeEnabled(const core::FilePath& statePath);

bool restore(const core::FilePath& statePath, 
             bool serverMode,
             boost::function<core::Error()>* pDeferredRestoreAction,
             std::string* pErrorMessages); 
   
bool destroy(const core::FilePath& statePath);

SessionStateInfo getSessionStateInfo();
     
} // namespace state
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_R_SESSION_STATE_HPP

