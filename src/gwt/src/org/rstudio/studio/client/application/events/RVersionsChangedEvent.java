/*
 * RVersionsChangedEvent.java
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

import org.rstudio.studio.client.application.model.RVersionsInfo;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RVersionsChangedEvent 
                     extends GwtEvent<RVersionsChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onRVersionsChanged(RVersionsChangedEvent event);
   }

   public RVersionsChangedEvent(RVersionsInfo rVersions)
   {
      rVersionsInfo_ = rVersions;
   }
   
   public RVersionsInfo getRVersionsInfo()
   {
      return rVersionsInfo_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRVersionsChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   private final RVersionsInfo rVersionsInfo_;
}
