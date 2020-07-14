/*
 * CreateKeyResult.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class CreateKeyResult extends JavaScriptObject
{
   protected CreateKeyResult()
   {
   }
   
   // check this value first to see if the operation failed
   // due to the key already existing
   public native final boolean getFailedKeyExists() /*-{
      return this.failed_key_exists;
   }-*/;
   
   public native final int getExitStatus() /*-{
      return this.exit_status;
   }-*/;

   public native final String getOutput() /*-{
      return this.output;
   }-*/; 
}
