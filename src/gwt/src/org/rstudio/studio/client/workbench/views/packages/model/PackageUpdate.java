/*
 * PackageUpdate.java
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
package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PackageUpdate extends JavaScriptObject
{
   protected PackageUpdate()
   {
   }
   
   public final native String getPackageName() /*-{
      return this.packageName;
   }-*/;
   
   public final native String getLibPath() /*-{
      return this.libPath;
   }-*/;

   public final native String getInstalled() /*-{
      return this.installed;
   }-*/;

   public final native String getAvailable() /*-{
      return this.available;
   }-*/;
}
