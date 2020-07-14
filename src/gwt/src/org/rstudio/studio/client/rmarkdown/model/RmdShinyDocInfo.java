/*
 * RmdShinyDocInfo.java
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
package org.rstudio.studio.client.rmarkdown.model;

public class RmdShinyDocInfo extends RmdSlideNavigationInfo
{
   protected RmdShinyDocInfo() 
   {
   }

   public final native String getUrl() /*-{
      return this.url;
   }-*/;

   public final native String getFile() /*-{
      return this.target_file;
   }-*/;

   public native final RmdOutputFormat getFormat() /*-{
      return this.output_format;
   }-*/;
   
   public native final String getRuntime() /*-{
      return this.runtime;
   }-*/;
}
