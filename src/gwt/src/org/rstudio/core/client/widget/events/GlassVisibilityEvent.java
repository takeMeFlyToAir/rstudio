/*
 * GlassVisibilityEvent.java
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
package org.rstudio.core.client.widget.events;

import com.google.gwt.event.shared.GwtEvent;

public class GlassVisibilityEvent extends GwtEvent<GlassVisibilityHandler>
{
   public static final Type<GlassVisibilityHandler> TYPE =
         new Type<GlassVisibilityHandler>();

   public GlassVisibilityEvent(boolean show)
   {
      show_ = show;
   }

   public boolean isShow()
   {
      return show_;
   }

   @Override
   public Type<GlassVisibilityHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(GlassVisibilityHandler handler)
   {
      handler.onGlass(this);
   }

   private final boolean show_;
}
