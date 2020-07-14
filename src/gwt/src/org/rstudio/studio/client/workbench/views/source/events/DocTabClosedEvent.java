/*
 * DocTabClosedEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class DocTabClosedEvent 
             extends CrossWindowEvent<DocTabClosedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onDocTabClosed(DocTabClosedEvent event);
   }

   public static final GwtEvent.Type<DocTabClosedEvent.Handler> TYPE =
      new GwtEvent.Type<DocTabClosedEvent.Handler>();
   
   public DocTabClosedEvent()
   {
   }
   
   public DocTabClosedEvent(String docId)
   {
      docId_ = docId;
   }
   
   public String getDocId()
   {
      return docId_;
   }
   
   @Override
   protected void dispatch(DocTabClosedEvent.Handler handler)
   {
      handler.onDocTabClosed(this);
   }

   @Override
   public GwtEvent.Type<DocTabClosedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String docId_;
}
