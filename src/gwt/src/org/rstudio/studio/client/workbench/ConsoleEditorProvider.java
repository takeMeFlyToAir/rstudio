/*
 * ConsoleEditorProvider.java
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
package org.rstudio.studio.client.workbench;

import com.google.inject.Singleton;

import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;

@Singleton
public class ConsoleEditorProvider
{
   public void setConsoleEditor(InputEditorDisplay editor)
   {
      consoleEditor_ = editor;
   }
   
   public InputEditorDisplay getConsoleEditor()
   {
      return consoleEditor_;
   }
   
   private InputEditorDisplay consoleEditor_;
}
