/*
 * RSConnectDirectoryState.java
 *
 * Copyright (C) 2009-14 by RStudio, PBC
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
package org.rstudio.studio.client.rsconnect.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectDirectoryState extends JavaScriptObject
{
   protected RSConnectDirectoryState()
   {
   }
   
   public final native static RSConnectDirectoryState create() /*-{ 
      return { dir_map: {} };
   }-*/;
   
   public final native void addDeployment (
         String dir, 
         RSConnectDeploymentRecord record) /*-{
      this.dir_map[dir] = record;
   }-*/;
   
   public final native RSConnectDeploymentRecord getLastDeployment(String dir) /*-{
      return this.dir_map[dir] ? 
                 this.dir_map[dir] : 
                 null;
   }-*/;
}
