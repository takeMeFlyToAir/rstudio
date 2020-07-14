/*
 * WindowStateChangeEvent.java
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
package org.rstudio.core.client.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.layout.WindowState;

public class WindowStateChangeEvent extends GwtEvent<WindowStateChangeHandler>
{
   public static final Type<WindowStateChangeHandler> TYPE =
         new Type<WindowStateChangeHandler>();

   public WindowStateChangeEvent(WindowState newState)
   {
      newState_ = newState;
      skipFocusChange_ = false;
   }

   public WindowStateChangeEvent(WindowState newState,
                                 boolean keepFocus)
   {
      newState_ = newState;
      skipFocusChange_ = keepFocus;
   }
   
   public WindowState getNewState()
   {
      return newState_;
   }
   
   public boolean skipFocusChange()
   {
      return skipFocusChange_;
   }

   @Override
   public Type<WindowStateChangeHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(WindowStateChangeHandler handler)
   {
      handler.onWindowStateChange(this);
   }

   private final WindowState newState_;
   private final boolean skipFocusChange_;
}
