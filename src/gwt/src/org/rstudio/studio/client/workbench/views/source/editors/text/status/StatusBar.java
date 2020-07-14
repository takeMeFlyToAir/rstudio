/*
 * StatusBar.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event.NativePreviewEvent;

public interface StatusBar
{
   public static interface HideMessageHandler
   {
      // return 'true' to indicate message should be hidden
      public boolean onNativePreviewEvent(NativePreviewEvent preview);
   }
   
   public static final int SCOPE_FUNCTION   = 1;
   public static final int SCOPE_CHUNK      = 2;
   public static final int SCOPE_SECTION    = 3;
   public static final int SCOPE_SLIDE      = 4;
   public static final int SCOPE_CLASS      = 5;
   public static final int SCOPE_NAMESPACE  = 6;
   public static final int SCOPE_LAMBDA     = 7;
   public static final int SCOPE_ANON       = 8;
   public static final int SCOPE_TOP_LEVEL  = 9;
   
   StatusBarElement getPosition();
   StatusBarElement getScope();
   StatusBarElement getLanguage();
   void setScopeVisible(boolean visible);
   void setScopeType(int type);
   
   void showMessage(String message);
   void showMessage(String message, int timeMs);
   void showMessage(String message, HideMessageHandler handler);
   void hideMessage();
   
   void showNotebookProgress(String label);
   void updateNotebookProgress(int percent);
   void hideNotebookProgress(boolean immediately);
   HandlerRegistration addProgressClickHandler(ClickHandler handler);
   HandlerRegistration addProgressCancelHandler(Command onCanceled);
}
