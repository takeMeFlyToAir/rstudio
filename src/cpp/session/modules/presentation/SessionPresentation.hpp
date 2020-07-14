/*
 * SessionPresentation.hpp
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

#ifndef SESSION_PRESENTATION_HPP
#define SESSION_PRESENTATION_HPP

#include <string>

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   namespace http {
      class Request;
      class Response;
   }
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

core::json::Value presentationStateAsJson();

core::Error initialize();
                       
} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_HPP
