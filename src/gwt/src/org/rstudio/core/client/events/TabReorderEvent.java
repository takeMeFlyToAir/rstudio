/*
 * TabReorderEvent.java
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
package org.rstudio.core.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class TabReorderEvent extends GwtEvent<TabReorderHandler>
{
   public static final Type<TabReorderHandler> TYPE = new Type<TabReorderHandler>();

   public TabReorderEvent(int oldPos, int newPos)
   {
      oldPos_ = oldPos;
      newPos_ = newPos;
   }

   public int getNewPos()
   {
      return newPos_;
   }

   public int getOldPos()
   {
      return oldPos_;
   }

   @Override
   public Type<TabReorderHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(TabReorderHandler handler)
   {
      handler.onTabReorder(this);
   }

   private int oldPos_;
   private int newPos_;
}
