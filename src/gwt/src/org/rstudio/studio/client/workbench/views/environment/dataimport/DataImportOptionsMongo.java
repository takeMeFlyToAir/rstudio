/*
 * DataImportOptionsMongo.java
 *
 * Copyright (C) 2009-16 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

public class DataImportOptionsMongo extends DataImportOptions
{
   protected DataImportOptionsMongo()
   {
   }
   
   public final static native DataImportOptionsMongo create(
      String dataName) /*-{
         return {
            "mode": "mongo",
            "dataName": dataName
       }
   }-*/;
}
