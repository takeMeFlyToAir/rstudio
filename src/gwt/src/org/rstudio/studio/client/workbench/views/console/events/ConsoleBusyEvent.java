/*
 * ConsoleBusyEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, PBC
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ConsoleBusyEvent 
   extends GwtEvent<ConsoleBusyEvent.Handler>
{
   public static final GwtEvent.Type<ConsoleBusyEvent.Handler> TYPE =
      new GwtEvent.Type<ConsoleBusyEvent.Handler>();
  
   public interface Handler extends EventHandler
   {
      void onConsoleBusy(ConsoleBusyEvent event);
   }
   
   public ConsoleBusyEvent(boolean busy)
   {
      busy_ = busy;
   }
   
   public boolean isBusy()
   {
      return busy_;
   }
         
   @Override
   public Type<ConsoleBusyEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsoleBusy(this);
   }
   
   private boolean busy_;
}
