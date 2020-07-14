/*
 * BrowseUrlEvent.java
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
import org.rstudio.studio.client.workbench.model.BrowseUrlInfo;

public class BrowseUrlEvent extends GwtEvent<BrowseUrlHandler>
{
   public static final GwtEvent.Type<BrowseUrlHandler> TYPE =
      new GwtEvent.Type<BrowseUrlHandler>();
   
   public BrowseUrlEvent(BrowseUrlInfo urlInfo)
   {
      urlInfo_ = urlInfo;
   }
   
   public BrowseUrlInfo getUrlInfo()
   {
      return urlInfo_;
   }
   
   @Override
   protected void dispatch(BrowseUrlHandler handler)
   {
      handler.onBrowseUrl(this);
   }

   @Override
   public GwtEvent.Type<BrowseUrlHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final BrowseUrlInfo urlInfo_;
}
