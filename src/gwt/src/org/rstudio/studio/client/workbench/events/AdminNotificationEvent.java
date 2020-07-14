/*
 * AdminNotificationEvent.java
 *
 * Copyright (C) 2017 by RStudio, PBC
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
import org.rstudio.studio.client.workbench.model.AdminNotification;

public class AdminNotificationEvent extends GwtEvent<AdminNotificationHandler>
{
   public static final GwtEvent.Type<AdminNotificationHandler> TYPE =
      new GwtEvent.Type<AdminNotificationHandler>();
   
   public AdminNotificationEvent(AdminNotification notification)
   {
      adminNotification_ = notification;
   }
   
   public AdminNotification getAdminNotification()
   {
      return adminNotification_;
   }
   
   @Override
   protected void dispatch(AdminNotificationHandler handler)
   {
      handler.onAdminNotification(this);
   }

   @Override
   public GwtEvent.Type<AdminNotificationHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final AdminNotification adminNotification_;
}
