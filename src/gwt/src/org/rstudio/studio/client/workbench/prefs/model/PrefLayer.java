/*
 * PrefLayer.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;

public class PrefLayer extends JavaScriptObject
{
   protected PrefLayer()
   {
   }

   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native JsObject getValues() /*-{
      return this.values;
   }-*/;
   
   public final static String LAYER_DEFAULT = "default";
   public final static String LAYER_SYSTEM = "system";
   public final static String LAYER_COMPUTED = "computed";
   public final static String LAYER_USER = "user";
   public final static String LAYER_PROJECT = "project";
}
