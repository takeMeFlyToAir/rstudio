/*
 * ExecuteUserCommandEvent.java
 *
 * Copyright (C) 2009-13 by RStudio, PBC
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
package org.rstudio.studio.client.server.remote;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ExecuteUserCommandEvent
   extends GwtEvent<ExecuteUserCommandEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onExecuteUserCommand(ExecuteUserCommandEvent event);
   }
   
   public ExecuteUserCommandEvent(String commandName)
   {
      name_ = commandName;
   }
   
   public String getCommandName()
   {
      return name_;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onExecuteUserCommand(this);
   }
   
   private final String name_;
   public static final Type<Handler> TYPE = new Type<Handler>();
   

}
