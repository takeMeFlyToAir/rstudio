/*
 * RQuit.hpp
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

#ifndef R_SESSION_QUIT_HPP
#define R_SESSION_QUIT_HPP

namespace rstudio {
namespace r {
namespace session {

#ifdef _WIN32
bool win32Quit(const std::string& command, std::string* pErrMsg);
#endif

void quit(bool saveWorkspace, int status);

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_QUIT_HPP
