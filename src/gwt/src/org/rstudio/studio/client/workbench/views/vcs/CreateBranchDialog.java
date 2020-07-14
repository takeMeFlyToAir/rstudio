/*
 * CreateBranchDialog.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.Functional;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.Functional.Predicate;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.common.vcs.RemotesInfo;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CreateBranchDialog extends ModalDialog<CreateBranchDialog.Input>
{
   public static class Input
   {
      public Input(String branch, String remote, boolean push)
      {
         branch_ = branch;
         remote_ = remote;
         push_ = push;
      }
      
      public final String getBranch()
      {
         return branch_;
      }
      
      public final String getRemote()
      {
         return remote_;
      }
      
      public final boolean getPush()
      {
         return push_;
      }
      
      private final String branch_;
      private final String remote_;
      private final boolean push_;
   }

   @Override
   protected Input collectInput()
   {
      String branch = tbBranch_.getValue().trim();
      String remote = sbRemote_.getValue().trim();
      boolean push = cbPush_.isVisible() ? cbPush_.getValue() : false;
      return new Input(branch, remote, push);
   }
   
   public CreateBranchDialog(final String caption,
                             final JsArray<RemotesInfo> remotesInfo,
                             final OperationWithInput<CreateBranchDialog.Input> onCreateBranch,
                             final OperationWithInput<AddRemoteDialog.Input> onAddRemote)
   {
      super(caption, Roles.getDialogRole(), onCreateBranch);
      
      setOkButtonCaption("Create");
      enableOkButton(false);
      
      container_ = new VerticalPanel();
      
      tbBranch_ = textBox();
      Roles.getTextboxRole().setAriaRequiredProperty(tbBranch_.getElement(), true);
      tbBranch_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  String text = tbBranch_.getValue();
                  enableOkButton(!StringUtil.isNullOrEmpty(text));
               }
            });
         }
      });
      
      sbRemote_ = new SelectWidget("Remote:");
      sbRemote_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            boolean isNone = sbRemote_.getValue() == REMOTE_NONE;
            cbPush_.setVisible(!isNone);
         }
      });
      
      btnAddRemote_ = new SmallButton("Add Remote...");
      btnAddRemote_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            // Try to use the remote URL associated with the currently
            // selected remote name if available.
            String remoteUrl = null;
            if (remotesInfo_ != null)
            {
               final String currentRemote = sbRemote_.getValue();
               RemotesInfo info = Functional.find(remotesInfo_, new Predicate<RemotesInfo>()
               {
                  @Override
                  public boolean test(RemotesInfo info)
                  {
                     return info.getRemote() == currentRemote;
                  }
               });

               if (info != null)
                  remoteUrl = info.getUrl();
            }
            
            AddRemoteDialog dialog = new AddRemoteDialog(
                  "Add Remote",
                  remoteUrl,
                  onAddRemote);
            
            dialog.showModal();
         }
      });
      
      cbPush_ = new CheckBox("Sync branch with remote");
      cbPush_.setVisible(sbRemote_.getValue() != REMOTE_NONE);
      cbPush_.setValue(true);
      
      setRemotes(remotesInfo);
      
      LayoutGrid ctrBranch = new LayoutGrid(1, 2);
      ctrBranch.setWidth("100%");
      FormLabel branchLabel = new FormLabel("Branch Name:", tbBranch_);
      ctrBranch.setWidget(0, 0, branchLabel);
      ctrBranch.setWidget(0, 1, tbBranch_);
      
      HorizontalPanel ctrRemote = new HorizontalPanel();
      ctrRemote.setWidth("100%");
      ctrRemote.add(sbRemote_);
      sbRemote_.getElement().getStyle().setFloat(Style.Float.LEFT);
      ctrRemote.add(btnAddRemote_);
      btnAddRemote_.getElement().getStyle().setFloat(Style.Float.RIGHT);
      btnAddRemote_.getElement().getStyle().setMarginTop(2, Unit.PX);
      btnAddRemote_.getElement().getStyle().setMarginRight(3, Unit.PX);
      
      VerticalPanel panel = new VerticalPanel();
      panel.add(ctrBranch);
      panel.add(new VerticalSpacer("6px"));
      panel.add(ctrRemote);
      
      container_.add(panel);
      container_.add(cbPush_);
   }
   
   public void setRemotes(JsArray<RemotesInfo> remotesInfo)
   {
      setRemotes(null, remotesInfo);
   }
   
   public void setRemotes(String activeRemote,
                          JsArray<RemotesInfo> remotesInfo)
   {
      remotesInfo_ = remotesInfo;
      
      List<String> remotes = new ArrayList<String>();
      for (RemotesInfo info : JsUtil.asIterable(remotesInfo))
      {
         if (!remotes.contains(info.getRemote()))
         {
            String remote = info.getRemote();
            remotes.add(info.getRemote());
            if (activeRemote == null && info.isActive())
               activeRemote = remote;
         }
      }
      
      String[] choices = new String[remotes.size() + 1];
      for (int i = 0; i < remotes.size(); i++)
         choices[i] = remotes.get(i);
      choices[remotes.size()] = REMOTE_NONE;
      
      // if we haven't set an active remote, try defaulting to the one called
      // 'origin' (if it exists)
      if (activeRemote == null)
      {
         for (int i = 0; i < choices.length; i++)
         {
            if (REMOTE_ORIGIN == choices[i])
            {
               activeRemote = REMOTE_ORIGIN;
               break;
            }
         }
      }
      
      // if we still haven't found anything, just default to the first entry
      // (note that because we always add the (none) remote there will always
      // be an entry available here)
      if (activeRemote == null)
         activeRemote = choices[0];
      
      sbRemote_.setChoices(choices);
      sbRemote_.setValue(activeRemote);
      
      cbPush_.setVisible(choices.length > 1);
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   @Override
   public void focusFirstControl()
   {
      tbBranch_.setFocus(true);
      tbBranch_.selectAll();
   }
   
   private TextBox textBox()
   {
      TextBox textBox = new TextBox();
      textBox.getElement().getStyle().setProperty("minWidth", "200px");
      DomUtils.disableSpellcheck(textBox);
      return textBox;
   }
   
   private JsArray<RemotesInfo> remotesInfo_;
   
   private final VerticalPanel container_;
   private final TextBox tbBranch_;
   private final SelectWidget sbRemote_;
   private final SmallButton btnAddRemote_;
   private final CheckBox cbPush_;
   
   private static final String REMOTE_NONE = "(None)";
   private static final String REMOTE_ORIGIN = "origin";
}
