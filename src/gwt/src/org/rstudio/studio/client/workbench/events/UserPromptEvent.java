/*
 * UserPromptEvent.java
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
import org.rstudio.studio.client.workbench.model.UserPrompt;

public class UserPromptEvent extends GwtEvent<UserPromptHandler>
{
   public static final GwtEvent.Type<UserPromptHandler> TYPE =
      new GwtEvent.Type<UserPromptHandler>();
   
   public UserPromptEvent(UserPrompt userPrompt)
   {
      userPrompt_ = userPrompt;
   }
   
   public UserPrompt getUserPrompt()
   {
      return userPrompt_;
   }
   
   @Override
   protected void dispatch(UserPromptHandler handler)
   {
      handler.onUserPrompt(this);
   }

   @Override
   public GwtEvent.Type<UserPromptHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final UserPrompt userPrompt_;
}
