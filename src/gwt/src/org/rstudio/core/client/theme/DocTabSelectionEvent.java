/*
 * DocTabSelectionEvent.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.core.client.theme;

import com.google.gwt.event.logical.shared.SelectionEvent;

public class DocTabSelectionEvent extends SelectionEvent<Integer>
{
   public DocTabSelectionEvent(int tab, boolean focus)
   {
      super(tab);
      focus_ = focus;
   }
   
   public boolean getFocus()
   {
      return focus_;
   }
   
   private final boolean focus_;
}
