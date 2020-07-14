/*
 * SourceSatellite.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteApplication;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SourceSatellite extends SatelliteApplication
{
   // note that there's special behavior attached to this name in the desktop
   // frame 
   public static final String NAME_PREFIX = "source_window_";
   
   public SourceSatellite(String name)
   {
      name_ = name;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(SourceSatelliteView view,
                          Satellite satellite,
                          Provider<AceThemes> pAceThemes,
                          Provider<UserPrefs> pUserPrefs,
                          ApplicationUncaughtExceptionHandler exHandler,
                          Commands commands)
   {
      initialize(name_, view, satellite, pAceThemes, pUserPrefs, exHandler, commands);
   }
   
   private final String name_;
}
