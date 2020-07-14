/*
 * PosixUser.hpp
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

#ifndef CORE_SYSTEM_POSIX_USER_HPP
#define CORE_SYSTEM_POSIX_USER_HPP

#include <string>
#include <unistd.h>

#include <shared_core/system/User.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace core {
namespace system {
namespace user {

struct UserIdentity
{
   UidType userId;
   GidType groupId;
};

UserIdentity currentUserIdentity();
   
core::Error socketPeerIdentity(int socket, UserIdentity* pIdentity);
   
} // namespace user
} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_POSIX_USER_HPP

