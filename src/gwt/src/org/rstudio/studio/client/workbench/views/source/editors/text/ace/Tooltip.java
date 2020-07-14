/*
 * Tooltip.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Tooltip extends JavaScriptObject
{
   protected Tooltip() {}

   public final native String getTextContent()
   /*-{
      return this.$element.textContent;
   }-*/;

   public final native String getInnerHTML()
   /*-{
       return this.$element.innerHTML;
   }-*/;

   public final native void hide()
   /*-{
      this.hide();
   }-*/;
}


