/*
 * ProvidesBusy.java
 *
 * Copyright (C) 2009-14 by RStudio, PBC
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
package org.rstudio.core.client.widget.model;

import org.rstudio.studio.client.workbench.events.BusyHandler;

public interface ProvidesBusy
{
   // Note that this doesn't return HandlerRegistration to avoid complications
   // with busy sources that are async (i.e. tab shims) 
   public void addBusyHandler(BusyHandler handler);
}
