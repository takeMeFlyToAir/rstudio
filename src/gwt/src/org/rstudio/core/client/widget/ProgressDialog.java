/*
 * ProgressDialog.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.DialogRole;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomMetrics;

public abstract class ProgressDialog extends ModalDialogBase
{
   interface Resources extends ClientBundle
   {
      ImageResource progress();

      @Source("ProgressDialog.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String progressDialog();
      String labelCell();
      String progressCell();
      String buttonCell();
      String displayWidget();
   }

   interface Binder extends UiBinder<Widget, ProgressDialog>
   {}

   public static void ensureStylesInjected()
   {
      resources_.styles().ensureInjected();
   }

   public ProgressDialog(String title, DialogRole role)
   {
      this(title, role, null);
   }

   public ProgressDialog(String title, DialogRole role, Object param)
   {
      super(role);
      addStyleName(resources_.styles().progressDialog());

      operationStarted_ = false;

      setText(title);

      display_ = createDisplayWidget(param);
      display_.addStyleName(resources_.styles().displayWidget());
      Style style = display_.getElement().getStyle();
      double skewFactor = (12 + BrowseCap.getFontSkew()) / 12.0;
      int width = Math.min((int)(skewFactor * 660),
                            Window.getClientWidth() - 100);
      style.setWidth(width, Unit.PX);
      
      progressAnim_ = new Image(resources_.progress().getSafeUri());
      stopButton_ = new ThemedButton("Stop");
      centralWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);

      ElementIds.assignElementId(label_, ElementIds.PROGRESS_TITLE_LABEL);
      Roles.getProgressbarRole().set(progressAnim_.getElement());
      Roles.getProgressbarRole().setAriaLabelledbyProperty(progressAnim_.getElement(),
            Id.of(label_.getElement()));
      setLabel(title);
   } 
   
   @Override
   protected Widget createMainWidget()
   {
      return centralWidget_;
   }
   
   protected abstract Widget createDisplayWidget(Object param);

   @Override
   protected void onUnload()
   {
      super.onUnload();
      unregisterHandlers();
   }

   @Override
   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN
          && KeyboardShortcut.getModifierValue(event.getNativeEvent()) == KeyboardShortcut.NONE)
      {
         if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            stopButton_.click();
            event.cancel();
            return;
         }
         else if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
         {   
            if (handleEnterKey())
            {
               event.cancel();
               return;
            }
         }
      }

      super.onPreviewNativeEvent(event);
   }
   
   protected ThemedButton stopButton()
   {
      return stopButton_;
   }
   
   protected void addHandlerRegistration(HandlerRegistration reg)
   {
      registrations_.add(reg);
   }
   
   protected void unregisterHandlers()
   {
      registrations_.removeHandler();
   }
   
   protected void setLabel(String text)
   {
      Size labelSize = DomMetrics.measureHTML(text);
      labelCell_.getStyle().setWidth(labelSize.width + 10, Unit.PX);
      label_.setText(text);
      labelText_ = text;
   }
   
   protected void showProgress()
   {
      operationStarted_ = true;
      progressAnim_.getElement().getStyle().setDisplay(Style.Display.INITIAL);
   }
   
   protected void hideProgress()
   {
      if (operationStarted_)
      {
         operationStarted_ = false;
         announceCompletion(StringUtil.isNullOrEmpty(labelText_) ?
            "Operation completed" : labelText_ + " completed");
      }
      progressAnim_.getElement().getStyle().setDisplay(Style.Display.NONE);
   }

   protected boolean handleEnterKey()
   {
      return false;
   }

   /**
    * Invoked when action has completed with a message suitable for announcement via
    * screen readers
    */
   protected abstract void announceCompletion(String message);
   
   private HandlerRegistrations registrations_ = new HandlerRegistrations();
  
   @UiField(provided = true)
   Widget display_;
   
   @UiField(provided = true)
   Image progressAnim_;
   @UiField
   Label label_;
   @UiField
   TableCellElement labelCell_;
   @UiField(provided = true)
   ThemedButton stopButton_;
   private Widget centralWidget_;
   private boolean operationStarted_;
   private String labelText_;

   private static final Resources resources_ = GWT.create(Resources.class);
}
