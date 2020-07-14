/*
 * EnsureHeightEvent.java
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

public class EnsureHeightEvent extends GwtEvent<EnsureHeightHandler>
{
   public static final Type<EnsureHeightHandler> TYPE
         = new Type<EnsureHeightHandler>();

   public static final int MAXIMIZED = -1;
   public static final int NORMAL = -2;
   
   public EnsureHeightEvent(int height)
   {
      height_ = height;
   }
   
   public int getHeight()
   {
      return height_;
   }

   @Override
   public Type<EnsureHeightHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EnsureHeightHandler handler)
   {
      handler.onEnsureHeight(this);
   }
   
   private final int height_;
}
