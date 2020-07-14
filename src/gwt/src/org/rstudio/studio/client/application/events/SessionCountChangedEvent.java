/*
 * SessionCountChangedEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SessionCountChangedEvent extends GwtEvent<SessionCountChangedEvent.Handler>
{  
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final int getCount() /*-{ return this.count; }-*/;
    }
   
   public interface Handler extends EventHandler
   {
      void onSessionCountChanged(SessionCountChangedEvent event);
   }
   
   public static final GwtEvent.Type<Handler> TYPE =
      new GwtEvent.Type<Handler>();
   
   public SessionCountChangedEvent(Data data)
   {
      count_ = data.getCount();
   }
   
   public SessionCountChangedEvent(int count)
   {
      count_ = count;
   }
   
   public int getCount()
   {
      return count_;
   }
   
   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSessionCountChanged(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final int count_;
}
