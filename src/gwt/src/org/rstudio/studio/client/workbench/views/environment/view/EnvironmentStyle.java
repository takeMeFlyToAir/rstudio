/*
 * EnvironmentStyle.java
 *
 * Copyright (C) 2009-17 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.resources.client.CssResource;

interface EnvironmentStyle extends CssResource
{
   int headerRowHeight();
   String expandCol();
   String nameCol();
   String resizeCol();
   String valueCol();
   String clickableCol();
   String detailRow();
   String categoryHeaderRow();
   String categoryHeaderText();
   String emptyEnvironmentPanel();
   String emptyEnvironmentMessage();
   String unclickableIcon();
   String unevaluatedPromise();
   String objectGrid();
   String widthSettingRow();
   String decoratedValueCol();
   String environmentPanel();
   String environmentPane();
   String fillHeight();
}

