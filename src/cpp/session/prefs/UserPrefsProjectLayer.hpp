/*
 * UserPrefsProjectLayer.hpp
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

#ifndef SESSION_USER_PREF_PROJECT_LAYER_HPP
#define SESSION_USER_PREF_PROJECT_LAYER_HPP

#include <session/prefs/PrefLayer.hpp>

namespace rstudio {
namespace session {
namespace prefs {

class UserPrefsProjectLayer: public PrefLayer
{
public:
   UserPrefsProjectLayer();
   core::Error readPrefs() override;
private:
   void onProjectConfigChanged();
};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif
