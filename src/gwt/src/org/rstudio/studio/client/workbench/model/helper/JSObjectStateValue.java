/*
 * JSObjectStateValue.java
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
package org.rstudio.studio.client.workbench.model.helper;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.workbench.model.ClientInitState;
import org.rstudio.studio.client.workbench.model.ClientState;

public abstract class JSObjectStateValue extends ClientStateValue<JsObject>

{
   public JSObjectStateValue(String group,
                             String name,
                             int persist,
                             ClientInitState state,
                             boolean delayedInit)
   {
      super(group,
            name,
            persist,
            state,
            delayedInit);
   }

   @Override
   protected final JsObject doGet(JsObject group, String name)
   {
      return group.getObject(name);
   }

   @Override
   protected final void doSet(ClientState state,
                        String group,
                        String name,
                        JsObject value,
                        int persist)
   {
      state.putObject(group, name, value, persist);
   }
}
