/*
 * SessionConnections.hpp
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

#ifndef SESSION_CONNECTIONS_HPP
#define SESSION_CONNECTIONS_HPP

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace connections {
   
// should connections be enabled?
bool connectionsEnabled();

// are we enabling them for the first time this session
bool activateConnections();

core::json::Array connectionsAsJson();

core::json::Array activeConnectionsAsJson();

bool isSuspendable();

core::Error initialize();
                       
} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CONNECTIONS_HPP
