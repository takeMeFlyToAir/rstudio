/*
 * InterruptInitiatedEvent.java
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
package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.js.JavaScriptSerializable;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class InterruptStatusEvent extends CrossWindowEvent<InterruptStatusEvent.Handler>
{
   public final static int INTERRUPT_INITIATED = 0;
   public final static int INTERRUPT_COMPLETED = 1;
   
   public interface Handler extends EventHandler
   {
      void onInterruptStatus(InterruptStatusEvent event);
   }

   public InterruptStatusEvent()
   {
      status_ = -1;
   }

   public InterruptStatusEvent(int status)
   {
      status_ = status;
   }
   
   public int getStatus()
   {
      return status_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onInterruptStatus(this);
   }
   
   private final int status_; 
   public static final Type<Handler> TYPE = new Type<Handler>();
}
