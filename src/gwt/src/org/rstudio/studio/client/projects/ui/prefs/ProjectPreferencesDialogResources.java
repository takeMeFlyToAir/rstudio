/*
 * ProjectPreferencesDialogResources.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface ProjectPreferencesDialogResources extends ClientBundle
{
   static interface Styles extends CssResource
   {
      String panelContainer();
      String buildToolsPanel();
      String workspaceGrid();
      String enableCodeIndexing();
      String useSpacesForTab();
      String numberOfTabs();
      String editingOption();
      String encodingChooser();
      String lineEndings();
      String vcsSelectExtraSpaced();
      String vcsOriginLabel();
      String vcsOriginUrl();
      String vcsNoOriginUrl();
      String buildToolsAdditionalArguments();
      String buildToolsRoxygenize();
      String buildToolsCheckBox();
      String buildToolsDevtools();
      String previewWebsite();
      String directorySelector();
      String websiteOutputFormat();
      String infoLabel();
   }
  
   @Source("ProjectPreferencesDialog.css")
   Styles styles();
  
   @Source("iconBuild_2x.png")
   ImageResource iconBuild2x();

   @Source("iconPackrat_2x.png")
   ImageResource iconPackrat2x();

   @Source("iconRenv_2x.png")
   ImageResource iconRenv2x();
   
   @Source("iconShare_2x.png")
   ImageResource iconShare2x();
   
   static ProjectPreferencesDialogResources INSTANCE = (ProjectPreferencesDialogResources)GWT.create(ProjectPreferencesDialogResources.class);
}
