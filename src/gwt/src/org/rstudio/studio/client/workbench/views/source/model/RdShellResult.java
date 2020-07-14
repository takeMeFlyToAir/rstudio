/*
 * RdShellResult.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

/*
 * An RdShellResult will either have a path or have contents (but never both)
 */
public class RdShellResult extends JavaScriptObject
{
   protected RdShellResult()
   {
   }

   public native final String getPath() /*-{
      return this.path;
   }-*/;
   
   public native final String getContents() /*-{
      return this.contents;
   }-*/;
}
