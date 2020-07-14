/*
 * ActivateHelpEvent.java
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
package org.rstudio.studio.client.workbench.views.help.events;

import com.google.gwt.event.shared.GwtEvent;

public class ActivateHelpEvent extends GwtEvent<ActivateHelpHandler>
{
   public static final GwtEvent.Type<ActivateHelpHandler> TYPE =
      new GwtEvent.Type<ActivateHelpHandler>();
   
   public ActivateHelpEvent()
   {
   }
   
   @Override
   protected void dispatch(ActivateHelpHandler handler)
   {
      handler.onActivateHelp(this);
   }

   @Override
   public GwtEvent.Type<ActivateHelpHandler> getAssociatedType()
   {
      return TYPE;
   }
}
