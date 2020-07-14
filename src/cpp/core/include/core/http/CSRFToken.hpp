/*
 * CSRFToken.hpp
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

#ifndef CORE_HTTP_CSRF_TOKEN_HPP
#define CORE_HTTP_CSRF_TOKEN_HPP

#include <string>
#include <boost/optional.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#define kCSRFTokenHeader "X-CSRF-Token"
#define kCSRFTokenCookie "csrf-token"

namespace rstudio {
namespace core {
namespace http {

// Adds a CSRF (cross site request forgery) cookie. This is simply a cookie with
// a random value (token).
void setCSRFTokenCookie(const Request& request, 
      const boost::optional<boost::gregorian::days>& expiry,
      const std::string& token,
      bool secure,
      core::http::Response* pResponse);

void setCSRFTokenCookie(const Request& request,
      const boost::optional<boost::posix_time::time_duration>& expiresFromNow,
      const std::string& token,
      bool secure,
      core::http::Response* pResponse);

// Validates an HTTP POST request by ensuring that the submitted fields include
// a valid CSRF token.
bool validateCSRFForm(const Request&, Response*);

// Validates any other HTTP request by ensuring that the CSRF HTTP header matches the accompanying
// token cookie.
bool validateCSRFHeaders(const Request& request);

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_CSRF_TOKEN_HPP

