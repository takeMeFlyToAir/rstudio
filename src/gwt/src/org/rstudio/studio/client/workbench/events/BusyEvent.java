/*
 * BusyEvent.java
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
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.GwtEvent;

public class BusyEvent extends GwtEvent<BusyHandler>
{
   public static final GwtEvent.Type<BusyHandler> TYPE =
      new GwtEvent.Type<BusyHandler>();
   
   public BusyEvent(boolean isBusy)
   {
      isBusy_ = isBusy;
   }
   
   public boolean isBusy()
   {
      return isBusy_;
   }
   
   @Override
   protected void dispatch(BusyHandler handler)
   {
      handler.onBusy(this);
   }

   @Override
   public GwtEvent.Type<BusyHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final boolean isBusy_;
}
