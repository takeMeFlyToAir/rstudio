/*
 * GitReviewPanel.java
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
package org.rstudio.studio.client.workbench.views.vcs.git.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.inject.Inject;

import com.google.inject.Provider;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.vcs.CheckoutBranchToolbarButton;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.ChunkOrLine;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.dialog.SharedStyles;
import org.rstudio.studio.client.workbench.views.vcs.dialog.SizeWarningWidget;
import org.rstudio.studio.client.workbench.views.vcs.git.dialog.GitReviewPresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.git.GitChangelistTablePresenter;

import java.util.ArrayList;

public class GitReviewPanel extends ResizeComposite implements Display
{
   interface Resources extends ClientBundle
   {
      @Source("GitReviewPanel.css")
      Styles styles();

      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      @Source("../../dialog/images/toolbarTile.png")
      ImageResource toolbarTile();

      @Source("../../dialog/images/stageAllFiles_2x.png")
      ImageResource stageAllFiles2x();

      @Source("../../dialog/images/discard_2x.png")
      ImageResource discard2x();

      @Source("../../dialog/images/ignore_2x.png")
      ImageResource ignore2x();

      @Source("../../dialog/images/stage_2x.png")
      ImageResource stage2x();

      @Source("../../dialog/images/splitterTileV.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource splitterTileV();

      @Source("../../dialog/images/splitterTileH.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource splitterTileH();

      @Source("../../dialog/images/blankFileIcon_2x.png")
      ImageResource blankFileIcon2x();
   }

   interface Styles extends SharedStyles
   {
      String diffToolbar();

      String stagedLabel();
      String staged();

      String unstaged();

      String diffViewOptions();

      String commitMessage();
      String commitButton();

      String splitPanelCommit();
      String ignoreWhitespace();
   }

   @SuppressWarnings("unused")
   private static class ClickCommand implements HasClickHandlers, Command
   {
      @Override
      public void execute()
      {
         ClickEvent.fireNativeEvent(
               Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false),
               this);
      }

      @Override
      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return handlerManager_.addHandler(ClickEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlerManager_.fireEvent(event);
      }

      private final HandlerManager handlerManager_ = new HandlerManager(this);
   }

   private static class ListBoxAdapter implements HasValue<Integer>
   {
      private ListBoxAdapter(ListBox listBox)
      {
         listBox_ = listBox;
         listBox_.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent event)
            {
               ValueChangeEvent.fire(ListBoxAdapter.this, getValue());
            }
         });
      }

      @Override
      public Integer getValue()
      {
         return Integer.parseInt(
               listBox_.getValue(listBox_.getSelectedIndex()));
      }

      @Override
      public void setValue(Integer value)
      {
         setValue(value, true);
      }

      @Override
      public void setValue(Integer value, boolean fireEvents)
      {
         String valueStr = value.toString();
         for (int i = 0; i < listBox_.getItemCount(); i++)
         {
            if (listBox_.getValue(i) == valueStr)
            {
               listBox_.setSelectedIndex(i);
               break;
            }
         }
      }

      @Override
      public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler)
      {
         return handlers_.addHandler(ValueChangeEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlers_.fireEvent(event);
      }

      private final ListBox listBox_;
      private final HandlerManager handlers_ = new HandlerManager(this);
   }


   interface Binder extends UiBinder<Widget, GitReviewPanel>
   {
   }

   @Inject
   public GitReviewPanel(GitChangelistTablePresenter changelist,
                         LineTableView diffPane,
                         final Commands commands,
                         FileTypeRegistry fileTypeRegistry,
                         CheckoutBranchToolbarButton branchToolbarButton,
                         AriaLiveService ariaLive,
                         Provider<UserPrefs> pPrefs)
   {
      fileTypeRegistry_ = fileTypeRegistry;
      ariaLive_ = ariaLive;
      pPrefs_ = pPrefs;
      splitPanel_ = new SplitLayoutPanel(4);
      splitPanelCommit_ = new SplitLayoutPanel(4);

      commitButton_ = new ThemedButton("Commit");
      commitButton_.addStyleName(RES.styles().commitButton());

      changelist_ = changelist.getView();
      lines_ = diffPane;
      lines_.getElement().setTabIndex(-1);

      overrideSizeWarning_ = new SizeWarningWidget("diff");

      topToolbar_ = new Toolbar("Git Review");
      diffToolbar_ = new Toolbar("Git Diff");

      changelist.setSelectFirstItemByDefault(true);

      Widget widget = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      initWidget(widget);

      topToolbar_.addStyleName(RES.styles().toolbar());
      topToolbar_.getWrapper().addStyleName(RES.styles().toolbarInnerWrapper());

      switchViewButton_ = new LeftRightToggleButton("Changes", "History", true);
      topToolbar_.addLeftWidget(switchViewButton_);

      topToolbar_.addLeftWidget(branchToolbarButton);
      
      topToolbar_.addLeftSeparator();
      
      topToolbar_.addLeftWidget(new ToolbarButton(
            ToolbarButton.NoText,
            commands.vcsRefresh().getTooltip(),
            commands.vcsRefresh().getImageResource(),
            new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  changelist_.showProgress();
                  commands.vcsRefresh().execute();
               }
            }));
      
      topToolbar_.addLeftSeparator();

      stageFilesButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Stage",
            ToolbarButton.NoTitle,
            new ImageResource2x(RES.stage2x()),
            (ClickHandler) null));

      topToolbar_.addLeftSeparator();

      revertFilesButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Revert",
            ToolbarButton.NoTitle,
            commands.vcsRevert().getImageResource(),
            (ClickHandler) null));

      ignoreButton_ = topToolbar_.addLeftWidget(new ToolbarButton(
            "Ignore", ToolbarButton.NoTitle, new ImageResource2x(RES.ignore2x())));

      topToolbar_.addRightWidget(commands.vcsPull().createToolbarButton());

      topToolbar_.addRightSeparator();

      topToolbar_.addRightWidget(commands.vcsPush().createToolbarButton());
  
      diffToolbar_.addStyleName(RES.styles().toolbar());
      diffToolbar_.addStyleName(RES.styles().diffToolbar());
      diffToolbar_.getWrapper().addStyleName(RES.styles().diffToolbarInnerWrapper());

      toolbarWrapper_.setCellWidth(diffToolbar_, "100%");

      stageAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Stage All", ToolbarButton.NoTitle, new ImageResource2x(RES.stage2x())));
      diffToolbar_.addLeftSeparator();
      discardAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Discard All", ToolbarButton.NoTitle, new ImageResource2x(RES.discard2x())));

      unstageAllButton_ = diffToolbar_.addLeftWidget(new ToolbarButton(
            "Unstage All", ToolbarButton.NoTitle, new ImageResource2x(RES.discard2x())));
      unstageAllButton_.setVisible(false);

      lblCommit_.setFor(commitMessage_);
      lblContext_.setFor(contextLines_);
      
      // Hide frequently-updating character count from screen readers
      A11y.setARIAHidden(lblCharCount_);

      unstagedCheckBox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> e)
         {
            ValueChangeEvent.fire(stagedCheckBox_, stagedCheckBox_.getValue());
         }
      });

      stagedCheckBox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> e)
         {
            stageAllButton_.setVisible(!e.getValue());
            discardAllButton_.setVisible(!e.getValue());
            unstageAllButton_.setVisible(e.getValue());
            diffToolbar_.invalidateSeparators();
         }
      });
      
      DomUtils.disableSpellcheck(commitMessage_);

      listBoxAdapter_ = new ListBoxAdapter(contextLines_);

      FontSizer.applyNormalFontSize(commitMessage_);
      commitMessage_.addKeyUpHandler(e ->
      {
         // Update commit message whenever keys are pressed
         updateCharCount();
      });
      commitMessage_.addChangeHandler(e ->
      {
         // Update commit message whenever the text content changes; catches
         // e.g. changes on blur after a mouse paste
         updateCharCount();
      });

      new WidgetHandlerRegistration(this)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return Event.addNativePreviewHandler(new NativePreviewHandler()
            {
               @Override
               public void onPreviewNativeEvent(NativePreviewEvent event)
               {
                  NativeEvent nativeEvent = event.getNativeEvent();
                  int keyCode = nativeEvent.getKeyCode();
                  int modifier = KeyboardShortcut.getModifierValue(nativeEvent);
                  if (event.getTypeInt() == Event.ONKEYDOWN && isCtrlOrMeta(modifier))
                  {
                     switch (keyCode)
                     {
                        case KeyCodes.KEY_DOWN:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_, getLineScroll(diffScroll_), 0);
                           break;
                        case KeyCodes.KEY_UP:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_, -getLineScroll(diffScroll_), 0);
                           break;
                        case KeyCodes.KEY_PAGEDOWN:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_, getPageScroll(diffScroll_), 0);
                           break;
                        case KeyCodes.KEY_PAGEUP:
                           nativeEvent.preventDefault();
                           scrollBy(diffScroll_, -getPageScroll(diffScroll_), 0);
                           break;
                        case KeyCodes.KEY_ENTER:
                           if (DomUtils.getActiveElement() == commitMessage_.getElement())
                           {
                              nativeEvent.preventDefault();
                              commitButton_.click();
                           }
                           break;
                     }
                  }
               }
            });
         }
      };
   }
   
   private boolean isCtrlOrMeta(int modifier)
   {
      return BrowseCap.isMacintosh()
            ? (modifier == KeyboardShortcut.CTRL || modifier == KeyboardShortcut.META)
            : modifier == KeyboardShortcut.CTRL;
   }

   private void scrollBy(ScrollPanel scrollPanel, int vscroll, int hscroll)
   {
      if (vscroll != 0)
      {
         scrollPanel.setVerticalScrollPosition(
               Math.max(0, scrollPanel.getVerticalScrollPosition() + vscroll));
      }

      if (hscroll != 0)
      {
         scrollPanel.setHorizontalScrollPosition(
               Math.max(0, scrollPanel.getHorizontalScrollPosition() + hscroll));
      }
   }

   private int getLineScroll(ScrollPanel panel)
   {
      return 30;
   }

   private int getPageScroll(ScrollPanel panel)
   {
      // Return slightly less than the client height (so there's overlap between
      // one screen and the next) but never less than the line scroll height.
      return Math.max(
            getLineScroll(panel),
            panel.getElement().getClientHeight() - getLineScroll(panel));
   }

   @Override
   public HasClickHandlers getSwitchViewButton()
   {
      return switchViewButton_;
   }

   @Override
   public HasClickHandlers getStageFilesButton()
   {
      return stageFilesButton_;
   }

   @Override
   public HasClickHandlers getRevertFilesButton()
   {
      return revertFilesButton_;
   }
   
   @Override
   public void setFilesCommandsEnabled(boolean enabled)
   {
      stageFilesButton_.setEnabled(enabled);
      revertFilesButton_.setEnabled(enabled);
      ignoreButton_.setEnabled(enabled);
      
   }

   @Override
   public HasClickHandlers getIgnoreButton()
   {
      return ignoreButton_;
   }

   @Override
   public HasClickHandlers getStageAllButton()
   {
      return stageAllButton_;
   }

   @Override
   public HasClickHandlers getDiscardAllButton()
   {
      return discardAllButton_;
   }

   @Override
   public HasClickHandlers getUnstageAllButton()
   {
      return unstageAllButton_;
   }

   @Override
   public void setShowActions(boolean showActions)
   {
      diffToolbar_.setVisible(showActions);
      lines_.setShowActions(showActions);
   }

   @Override
   public void setData(ArrayList<ChunkOrLine> lines, PatchMode patchMode)
   {
      int vscroll = diffScroll_.getVerticalScrollPosition();
      int hscroll = diffScroll_.getHorizontalScrollPosition();

      getLineTableDisplay().setData(lines, patchMode);

      diffScroll_.setVerticalScrollPosition(vscroll);
      diffScroll_.setHorizontalScrollPosition(hscroll);
   }

   @Override
   public HasText getCommitMessage()
   {
      return new HasText()
      {
         @Override
         public void setText(String text)
         {
            commitMessage_.setText(text);
            updateCharCount();
         }
         
         @Override
         public String getText()
         {
            return commitMessage_.getText();
         }
      };
   }

   @Override
   public HasClickHandlers getCommitButton()
   {
      return commitButton_;
   }

   @Override
   public HasValue<Boolean> getCommitIsAmend()
   {
      return commitIsAmend_;
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      return changelist_.getSelectedPaths();
   }

   @Override
   public void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      changelist_.setSelectedStatusAndPaths(selectedPaths);
   }

   @Override
   public ArrayList<String> getSelectedDiscardablePaths()
   {
      return changelist_.getSelectedDiscardablePaths();
   }

   @Override
   public HasValue<Boolean> getStagedCheckBox()
   {
      return stagedCheckBox_;
   }

   @Override
   public HasValue<Boolean> getUnstagedCheckBox()
   {
      return unstagedCheckBox_;
   }
   
   @Override
   public HasValue<Boolean> getIgnoreWhitespaceCheckBox()
   {
      return ignoreWhitespaceCheckbox_;
   }

   @Override
   public LineTablePresenter.Display getLineTableDisplay()
   {
      return lines_;
   }

   @Override
   public ChangelistTable getChangelistTable()
   {
      return changelist_;
   }

   @Override
   public HasValue<Integer> getContextLines()
   {
      return listBoxAdapter_;
   }

   @Override
   public HasClickHandlers getOverrideSizeWarningButton()
   {
      return overrideSizeWarning_;
   }

   @Override
   public void showSizeWarning(long sizeInBytes)
   {
      overrideSizeWarning_.setSize(sizeInBytes);
      diffScroll_.setWidget(overrideSizeWarning_);
   }

   @Override
   public void hideSizeWarning()
   {
      diffScroll_.setWidget(lines_);
   }
   
   @Override
   public void showContextMenu(final int clientX, 
                               final int clientY,
                               Command openSelectedCommand)
   {
      final ToolbarPopupMenu menu = new ToolbarPopupMenu();
      
      MenuItem stageMenu = new MenuItem(
           AppCommand.formatMenuLabel(new ImageResource2x(RES.stage2x()), "Stage", ""),
           true,
           new Command() {
              @Override
              public void execute()
              {
                 stageFilesButton_.click();
              }
              
           });
      if (stageFilesButton_.isEnabled())
      {
         menu.addItem(stageMenu);
         menu.addSeparator();
      }
     
    
      
     MenuItem revertMenu = new MenuItem(
           AppCommand.formatMenuLabel(new ImageResource2x(RES.discard2x()), "Revert...", ""),
           true,
           new Command() {
              @Override
              public void execute()
              {
                 revertFilesButton_.click();
              }
              
           });
      if (revertFilesButton_.isEnabled())
         menu.addItem(revertMenu);
      
      MenuItem ignoreMenu = new MenuItem(
            AppCommand.formatMenuLabel(new ImageResource2x(RES.ignore2x()), "Ignore...", ""),
            true,
            new Command() {
               @Override
               public void execute()
               {
                  ignoreButton_.click();
               }
               
            });
       if (ignoreButton_.isEnabled())
          menu.addItem(ignoreMenu);
     
      menu.addSeparator();
      MenuItem openMenu = new MenuItem(
                           AppCommand.formatMenuLabel(null, "Open File", ""),
                           true, 
                           openSelectedCommand);
      menu.addItem(openMenu);
     
      menu.setPopupPositionAndShow(new PositionCallback() {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
             menu.setPopupPosition(clientX, clientY);     
         }
      });
     
   }

   @Override
   public void onShow()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            changelist_.focus();
         }
      });
   }

   /**
    * Update the character count for the commit message, or clear it if there
    * are no longer any characters in the commit message.
    */
   private void updateCharCount()
   {
      int length = commitMessage_.getText().length();
      String liveRegionMessage;
      if (length == 0)
      {
         lblCharCount_.setText("");
         liveRegionMessage = "";
      }
      else
      {
         lblCharCount_.setText(length + " characters");
         liveRegionMessage = length + " characters in message";
      }

      // Debounce an update to the accessible character count
      ariaLive_.announce(AriaLiveService.GIT_MESSAGE_LENGTH, liveRegionMessage,
            Timing.DEBOUNCE, Severity.STATUS);
   }

   @UiField(provided = true)
   SplitLayoutPanel splitPanel_;
   @UiField(provided = true)
   SplitLayoutPanel splitPanelCommit_;
   @UiField(provided = true)
   ChangelistTable changelist_;
   @UiField(provided = true)
   ThemedButton commitButton_;
   @UiField
   RadioButton stagedCheckBox_;
   @UiField
   RadioButton unstagedCheckBox_;
   @UiField(provided = true)
   LineTableView lines_;
   @UiField
   FormLabel lblContext_;
   @UiField
   ListBox contextLines_;
   @UiField(provided = true)
   Toolbar topToolbar_;
   @UiField(provided = true)
   Toolbar diffToolbar_;
   @UiField
   FormLabel lblCommit_;
   @UiField
   Label lblCharCount_;
   @UiField
   TextArea commitMessage_;
   @UiField
   CheckBox commitIsAmend_;
   @UiField
   ScrollPanel diffScroll_;
   @UiField
   FlowPanel diffViewOptions_;
   @UiField
   HorizontalPanel toolbarWrapper_;
   @UiField
   CheckBox ignoreWhitespaceCheckbox_;

   private ListBoxAdapter listBoxAdapter_;

   private ToolbarButton stageFilesButton_;
   private ToolbarButton revertFilesButton_;
   private ToolbarButton ignoreButton_;
   private ToolbarButton stageAllButton_;
   private ToolbarButton discardAllButton_;
   private ToolbarButton unstageAllButton_;
   @SuppressWarnings("unused")
   private final FileTypeRegistry fileTypeRegistry_;
   private final AriaLiveService ariaLive_;
   private final Provider<UserPrefs> pPrefs_;
   private LeftRightToggleButton switchViewButton_;

   private SizeWarningWidget overrideSizeWarning_;

   private static final Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
}
