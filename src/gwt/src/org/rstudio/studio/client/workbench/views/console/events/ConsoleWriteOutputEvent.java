/*
 * ConsoleWriteOutputEvent.java
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
package org.rstudio.studio.client.workbench.views.console.events;

import org.rstudio.studio.client.workbench.views.console.model.ConsoleText;

import com.google.gwt.event.shared.GwtEvent;

public class ConsoleWriteOutputEvent extends GwtEvent<ConsoleWriteOutputHandler>
{
   public static final GwtEvent.Type<ConsoleWriteOutputHandler> TYPE =
      new GwtEvent.Type<ConsoleWriteOutputHandler>();
   
   public ConsoleWriteOutputEvent(ConsoleText output)
   {
      output_ = output;
   }
   
   public String getOutput()
   {
      return output_.text;
   }

   public String getConsole()
   {
      return output_.console;
   }
   
   @Override
   protected void dispatch(ConsoleWriteOutputHandler handler)
   {
      handler.onConsoleWriteOutput(this);
   }

   @Override
   public GwtEvent.Type<ConsoleWriteOutputHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final ConsoleText output_;
}

