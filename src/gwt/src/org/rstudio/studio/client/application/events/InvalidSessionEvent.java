/*
 * InvalidSessionEvent.java
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

import org.rstudio.studio.client.application.model.InvalidSessionInfo;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class InvalidSessionEvent extends GwtEvent<InvalidSessionEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onInvalidSession(InvalidSessionEvent event);
   }
   
   public static final GwtEvent.Type<Handler> TYPE =
      new GwtEvent.Type<Handler>();
   
   public InvalidSessionEvent(InvalidSessionInfo info)
   {
      info_ = info;
   }
   
   public InvalidSessionInfo getInfo()
   {
      return info_;
   }
   
   @Override
   protected void dispatch(Handler handler)
   {
      handler.onInvalidSession(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final InvalidSessionInfo info_;
}
