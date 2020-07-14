/*
 * ViewerNavigatedEvent.java
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
package org.rstudio.studio.client.workbench.views.viewer.events;

import org.rstudio.core.client.widget.RStudioFrame;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ViewerNavigatedEvent extends GwtEvent<ViewerNavigatedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onViewerNavigated(ViewerNavigatedEvent event);
   }

   public ViewerNavigatedEvent(String url, RStudioFrame frame)
   {
      url_ = url;
      frame_ = frame;
   }
   
   public String getURL()
   {
      return url_;
   }
   
   public RStudioFrame getFrame()
   {
      return frame_;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onViewerNavigated(this);
   }
   
   private final String url_;
   private final RStudioFrame frame_;
  
   public static final Type<Handler> TYPE = new Type<Handler>();
}
