/*
 * DialogBuilderBase.java
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
package org.rstudio.studio.client.common.dialog;

import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperation;

import java.util.ArrayList;

public abstract class DialogBuilderBase implements DialogBuilder
{
   protected static class ButtonSpec
   {
      public String label;
      public String elementId;
      public Operation operation;
      public ProgressOperation progressOperation;
   }

   public DialogBuilderBase(int type, String caption)
   {
      this.type = type;
      this.caption = caption;
   }

   @Override
   public DialogBuilder addButton(String label, String elementId)
   {
      return addButton(label, elementId, (Operation)null);
   }

   @Override
   public DialogBuilder addButton(String label, String elementId, Operation operation)
   {
      ButtonSpec button = new ButtonSpec();
      button.label = label;
      button.elementId = elementId;
      button.operation = operation;
      buttons_.add(button);

      return this;
   }

   @Override
   public DialogBuilder addButton(String label, String elementId, ProgressOperation operation)
   {
      ButtonSpec button = new ButtonSpec();
      button.label = label;
      button.elementId = elementId;
      button.progressOperation = operation;
      buttons_.add(button);

      return this;
   }

   @Override
   public DialogBuilder setDefaultButton(int index)
   {
      defaultButton_ = index;
      return this;
   }
   
   public abstract void showModal();

   protected final int type;
   protected final String caption;
   protected ArrayList<ButtonSpec> buttons_ = new ArrayList<>();
   protected int defaultButton_ = 0;
}
