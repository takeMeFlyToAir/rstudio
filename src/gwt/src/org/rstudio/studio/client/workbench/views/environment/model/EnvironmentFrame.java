/*
 * EnvironmentFrame.java
 *
 * Copyright (C) 2009-13 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.JavaScriptObject;

public class EnvironmentFrame extends JavaScriptObject
{
   protected EnvironmentFrame() { }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native int getFrame() /*-{
      return this.frame;
   }-*/;
   
   public final native boolean isLocal() /*-{
      return this.local;
   }-*/;
}
