/*
 * ServerConstants.hpp
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

#ifndef SERVER_CONSTANTS_HPP
#define SERVER_CONSTANTS_HPP

#define kRStudioSessionRequiredHeader "X-RStudio-Session-Required"
#define kServerLocalSocket            "rserver.socket"
#define kServerLocalSocketPathEnvVar  "RS_SERVER_LOCAL_SOCKET_PATH"
#define kServerTmpDir                 "rstudio-rserver"
#define kUserIdCookie                 "user-id"
#define kUserListCookie               "user-list-id"
#define kPersistAuthCookie            "persist-auth"
#define kServerTmpDirEnvVar           "RS_SERVER_TMP_DIR"

#endif // SERVER_CONSTANTS_HPP

