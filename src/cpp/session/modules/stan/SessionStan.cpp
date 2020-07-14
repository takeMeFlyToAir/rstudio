/*
 * SessionStan.cpp
 *
 * Copyright (C) 2009-18 by RStudio, PBC
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

#include "SessionStan.hpp"

#include <core/Exec.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace stan {

Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionStan.R"));
   return initBlock.execute();
}

} // end namespace stan
} // end namespace modules
} // end namespace session
} // end namespace rstudio
