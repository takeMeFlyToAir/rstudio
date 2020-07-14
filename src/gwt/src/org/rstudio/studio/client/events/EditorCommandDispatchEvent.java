/*
 * EditorCommandDispatchEvent.java
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
package org.rstudio.studio.client.events;

import com.google.gwt.event.shared.EventHandler;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

@JavaScriptSerializable
public class EditorCommandDispatchEvent extends CrossWindowEvent<EditorCommandDispatchEvent.Handler>
{
   public EditorCommandDispatchEvent()
   {
      this(null);
   }
   
   public EditorCommandDispatchEvent(EditorCommandEvent event)
   {
      event_ = event;
   }
   
   public EditorCommandEvent getEvent()
   {
      return event_;
   }
   
   @Override
   public boolean forward()
   {
      return false;
   }
   
   private final EditorCommandEvent event_;
   
   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onEditorCommandDispatch(EditorCommandDispatchEvent event);
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEditorCommandDispatch(this);
   }
   
   public static final Type<Handler> TYPE = new Type<Handler>();

}
