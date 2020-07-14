/*
 * DirectoryListing.java
 *
 * Copyright (C) 2009-15 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.files.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class DirectoryListing extends JavaScriptObject
{
   protected DirectoryListing()
   {
   }
   
   public final native boolean isParentBrowseable() /*-{
      return this.is_parent_browseable;
   }-*/;
   
   public final native JsArray<FileSystemItem> getFiles() /*-{
      return this.files;
   }-*/;
}
