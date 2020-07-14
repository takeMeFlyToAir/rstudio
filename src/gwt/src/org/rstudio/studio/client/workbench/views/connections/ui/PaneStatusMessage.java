/*
 * PaneStatusMessage.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.widget.HorizontalCenterPanel;

import com.google.gwt.user.client.ui.Label;

class PaneStatusMessage extends HorizontalCenterPanel
{
   public PaneStatusMessage(String message, int verticalOffset)
   {
      super(createStatusLabel(message), verticalOffset);
      setSize("100%", "100%");
   }

   
   private static Label createStatusLabel(String message)
   {
      Label label = new Label(message);
      label.getElement().getStyle().setColor("#888");
      return label;
   }
}
