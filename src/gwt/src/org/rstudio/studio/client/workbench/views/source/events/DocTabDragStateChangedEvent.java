
/*
 * DocTabDragStateChangedEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DocTabDragStateChangedEvent 
   extends GwtEvent<DocTabDragStateChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onDocTabDragStateChanged(DocTabDragStateChangedEvent e);
   }
   
   public DocTabDragStateChangedEvent(int state)
   {
      state_ = state;
   }

   public int getState()
   {
      return state_;
   }
  
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDocTabDragStateChanged(this);
   }

   private final int state_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
   
   public static final int STATE_NONE = 0;
   public static final int STATE_DRAGGING = 1;
}
