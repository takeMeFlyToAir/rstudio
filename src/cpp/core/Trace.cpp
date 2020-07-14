/*
 * Trace.cpp
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


#include <core/Trace.hpp>

#include <map>

#include <boost/utility.hpp>

#include <core/Thread.hpp>

#include <iostream>

namespace rstudio {
namespace core {
namespace trace {

namespace {

boost::mutex s_traceMutex ;

} // anonymous namespace


void add(void* key, const std::string& functionName)
{
   LOCK_MUTEX(s_traceMutex)
   {
      std::cerr << key << "      " << functionName << std::endl;
   }
   END_LOCK_MUTEX
}

} // namespace trace
} // namespace core
} // namespace rstudio
