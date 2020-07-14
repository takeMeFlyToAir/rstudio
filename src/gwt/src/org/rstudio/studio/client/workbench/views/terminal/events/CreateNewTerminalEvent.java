/*
 * CreateNewTerminalEvent.java
 *
 * Copyright (C) 2019 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.terminal.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateNewTerminalEvent.Handler;

@JavaScriptSerializable
public class CreateNewTerminalEvent extends CrossWindowEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onCreateNewTerminal(CreateNewTerminalEvent event);
   }

   public CreateNewTerminalEvent()
   {
   }

   public CreateNewTerminalEvent(String startingFolder)
   {
      startingFolder_ = startingFolder;
   }

   public CreateNewTerminalEvent(FileSystemItem startingFolder)
   {
      startingFolder_ = startingFolder.getPath();
   }

   public String getStartingFolder()
   {
      return startingFolder_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler createNewTerminalHandler)
   {
      createNewTerminalHandler.onCreateNewTerminal(this);
   }

   private String startingFolder_;

   public static final Type<Handler> TYPE = new Type<>();
}
