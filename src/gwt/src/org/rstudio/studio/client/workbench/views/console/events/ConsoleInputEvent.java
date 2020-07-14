/*
 * ConsoleInputEvent.java
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

import com.google.gwt.event.shared.GwtEvent;

public class ConsoleInputEvent extends GwtEvent<ConsoleInputHandler>
{
   public static final GwtEvent.Type<ConsoleInputHandler> TYPE =
      new GwtEvent.Type<ConsoleInputHandler>();
    
   public ConsoleInputEvent(String input, String console)
   {
      input_ = input;
      console_ = console;
   }
   
   public String getInput()
   {
      return input_;
   }
   
   public String getConsole()
   {
      return console_;
   }
   
   @Override
   protected void dispatch(ConsoleInputHandler handler)
   {
      handler.onConsoleInput(this);
   }

   @Override
   public GwtEvent.Type<ConsoleInputHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String input_;
   private final String console_;
}
