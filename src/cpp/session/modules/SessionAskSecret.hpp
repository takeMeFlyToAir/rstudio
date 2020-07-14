/*
 * SessionAskSecret.hpp
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

#ifndef SESSION_SESSION_ASK_SECRET_HPP
#define SESSION_SESSION_ASK_SECRET_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules {      
namespace ask_secret {


std::string activeWindow();
void setActiveWindow(const std::string& windowName);

struct SecretInput
{
   SecretInput() : cancelled(false), remember(false), changed(true) {}
   bool cancelled;
   std::string secret;
   bool remember;
   bool changed;
};

core::Error askForSecret(const std::string& name,
                         const std::string& title,
                         const std::string& prompt,
                         bool canRemember,
                         bool hasSecret,
                         SecretInput* pInput);

core::Error initialize();
   
} // namespace ask_secret
} // namepace handlers
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_ASK_SECRET_HPP
