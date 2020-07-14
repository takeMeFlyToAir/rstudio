/*
 * FindInFilesEvent.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.output.find.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class FindInFilesEvent extends GwtEvent<FindInFilesEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onFindInFiles(FindInFilesEvent event);
   }

   public FindInFilesEvent(String searchPattern)
   {
      searchPattern_ = searchPattern;
   }

   public String getSearchPattern()
   {
      return searchPattern_;
   }

   public boolean isReplace()
   {
      return replace_;
   }

   public String getReplacePattern()
   {
      return replacePattern_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onFindInFiles(this);
   }

   private final String searchPattern_;
   private String replacePattern_;
   private boolean replace_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
