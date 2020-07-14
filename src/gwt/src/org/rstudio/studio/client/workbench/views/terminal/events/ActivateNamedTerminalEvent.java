/*
 * ActivateNamedTerminalEvent.java
 *
 * Copyright (C) 2009-18 by RStudio, PBC
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.ActivateNamedTerminalEvent.Handler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ActivateNamedTerminalEvent extends CrossWindowEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onActivateNamedTerminal(ActivateNamedTerminalEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native String getId() /*-{ return this["id"]; }-*/;
   }
  
   public ActivateNamedTerminalEvent()
   {
   }

   public ActivateNamedTerminalEvent(Data data)
   {
      this(data.getId());
   }
   
   public ActivateNamedTerminalEvent(String id)
   {
      id_ = id;
   }

   public String getId()
   {
      return id_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler activateTerminalHandler)
   {
      activateTerminalHandler.onActivateNamedTerminal(this);
   }

   private String id_;

   public static final Type<Handler> TYPE = new Type<>();
}
