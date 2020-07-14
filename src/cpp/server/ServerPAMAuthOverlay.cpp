/*
 * ServerPAMAuthOverlay.cpp
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

#include "ServerPAMAuth.hpp"

#include <shared_core/Error.hpp>

namespace rstudio {
namespace server {
namespace pam_auth {

bool canSetSignInCookies()
{
   return true;
}

bool canStaySignedIn()
{
   return true;
}

void onUserAuthenticated(const std::string& username,
                         const std::string& password)
{

}

void onUserUnauthenticated(const std::string& username,
                           bool signedOut)
{

}

namespace overlay {

core::Error initialize()
{
   return core::Success();
}

} // namespace overlay
} // namespace pam_auth
} // namespace server
} // namespace rstudio
