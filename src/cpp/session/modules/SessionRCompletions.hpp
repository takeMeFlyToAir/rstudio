/*
 * SessionRCompletions.hpp
 *
 * Copyright (C) 2014 by RStudio, PBC
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

#ifndef SESSION_R_COMPLETIONS_HPP
#define SESSION_R_COMPLETIONS_HPP

#include <string>

namespace rstudio {
namespace core {
class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace r_packages {

core::Error initialize();

std::string finishExpression(const std::string& expression);

} // namespace r_completions
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_R_COMPLETIONS_HPP

