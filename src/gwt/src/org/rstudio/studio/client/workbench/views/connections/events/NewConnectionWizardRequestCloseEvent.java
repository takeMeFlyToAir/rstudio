/*
 * NewConnectionWizardRequestCloseEvent.java
 *
 * Copyright (C) 2009-18 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.connections.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class NewConnectionWizardRequestCloseEvent extends GwtEvent<NewConnectionWizardRequestCloseEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onNewConnectionWizardRequestClose(NewConnectionWizardRequestCloseEvent event);
   }
   
   public NewConnectionWizardRequestCloseEvent()
   {
   }
  

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNewConnectionWizardRequestClose(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
}
