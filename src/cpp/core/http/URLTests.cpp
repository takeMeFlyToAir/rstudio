/*
 * URLTests.cpp
 *
 * Copyright (C) 2018 by RStudio, PBC
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

#include <tests/TestThat.hpp>

#include <core/http/Util.hpp>
#include <core/http/URL.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace util {

test_context("HttpUtil Tests")
{
   test_that("Can parse simple url")
   {
      URL url("http://www.google.com");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "www.google.com");
      expect_true(url.portStr() == "80");
      expect_true(url.port() == 80);
      expect_true(url.path() == std::string());
   }

   test_that("Can parse simple https url")
   {
      URL url("https://www.google.com");

      expect_true(url.isValid());
      expect_true(url.protocol() == "https");
      expect_true(url.hostname() == "www.google.com");
      expect_true(url.portStr() == "443");
      expect_true(url.port() == 443);
      expect_true(url.path() == std::string());
   }
   
   test_that("Can parse url with port")
   {
      URL url("http://test.localhost:5235");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "test.localhost");
      expect_true(url.portStr() == "5235");
      expect_true(url.port() == 5235);
      expect_true(url.path() == std::string());
   }

   test_that("Can parse url with path")
   {
      URL url("http://google.com/search");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "google.com");
      expect_true(url.portStr() == "80");
      expect_true(url.port() == 80);
      expect_true(url.path() == "/search");
   }

   test_that("Can parse complex url")
   {
      URL url("https://localhost:9987/a/long/path/?term=happy&term=dog");

      expect_true(url.isValid());
      expect_true(url.protocol() == "https");
      expect_true(url.hostname() == "localhost");
      expect_true(url.portStr() == "9987");
      expect_true(url.port() == 9987);
      expect_true(url.path() == "/a/long/path/?term=happy&term=dog");
   }

   test_that("Bad urls return parse failure")
   {
      URL url("127.0.0.1");

      expect_false(url.isValid());
   }
}

} // end namespace util
} // end namespace http
} // end namespace core
} // end namespace rstudio
