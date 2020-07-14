/*
 * ToolbarPopupMenu.java
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

import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.BaseMenuBar;
import org.rstudio.core.client.command.CommandEvent;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

public class ToolbarPopupMenu extends ThemedPopupPanel
                              implements CommandHandler
{
   // Extensibility point for dynamically constructed popup menus. The default
   // implementation returns itself, but extensions can do some work to build
   // the menu and return the built menu. Callers can use this in combination
   // with getDynamicPopupMenu() when an up-to-date instance of the object is
   // required.
   public interface DynamicPopupMenuCallback
   {
      void onPopupMenu(ToolbarPopupMenu menu);
   }

   public ToolbarPopupMenu()
   {
      super(true);
      menuBar_ = createMenuBar();
      Widget mainWidget = createMainWidget();
      setWidget(mainWidget);
      events_ = RStudioGinjector.INSTANCE.getEventBus();
      commandHandler_ = new HandlerRegistrations();
   }
   
   public ToolbarPopupMenu(ToolbarPopupMenu parent)
   {
      this();
      parent_ = parent;
   }

   protected ToolbarMenuBar createMenuBar()
   {
      return new ToolbarMenuBar(true);
   }

   protected Widget createMainWidget()
   {
      return menuBar_;
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
      commandHandler_.add(events_.addHandler(CommandEvent.TYPE, this));
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      menuBar_.selectItem(null);
      commandHandler_.removeHandler();
   }
   
   public void selectFirst()
   {
      menuBar_.selectFirst();
   }

   public void selectItem(MenuItem menuItem)
   {
      menuBar_.selectItem(menuItem);
   }

   public void addItem(MenuItem menuItem)
   {
      ScheduledCommand command = menuItem.getScheduledCommand();
      if (command == null && menuItem instanceof AppMenuItem)
      {
         AppMenuItem appMenuItem = (AppMenuItem) menuItem;
         command = appMenuItem.getScheduledCommand(true);
      }
      
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
      
      menuBar_.addItem(menuItem);
   }
   
   public void addItem(SafeHtml html, MenuBar popup)
   {
      menuBar_.addItem(html, popup);
   }
   
   public void addItem(MenuItem menuItem, final ToolbarPopupMenu popup)
   {
      menuBar_.addItem(SafeHtmlUtils.fromTrustedString(menuItem.getHTML()), popup.menuBar_);
   }
   
   public void addItem(AppCommand command, ToolbarPopupMenu popup)
   {
      if (command.isEnabled())
         addItem(command.createMenuItem(false), popup);
   }
   
   public void setAutoOpen(boolean autoOpen)
   {
      menuBar_.setAutoOpen(autoOpen);
   }
   
   public void insertItem(MenuItem menuItem, int beforeIndex)
   {
     ScheduledCommand command = menuItem.getScheduledCommand();
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.insertItem(menuItem, beforeIndex);
   }
   
   public void removeItem(MenuItem menuItem)
   {
      menuBar_.removeItem(menuItem);
   }
   
   public boolean containsItem(MenuItem menuItem)
   {
      return menuBar_.getItemIndex(menuItem) >= 0;
   }
   
   public void clearItems()
   {
      menuBar_.clearItems();
   }
   
   public void addSeparator()
   {
      menuBar_.addSeparator();
   }
   
   public void addSeparator(MenuItemSeparator separator)
   {
      menuBar_.addSeparator(separator);
   }
   
   public void addSeparator(String label)
   {
      menuBar_.addSeparator(new LabelledMenuSeparator(label));
   }
   
   public void addSeparator(int minPx)
   {
      menuBar_.addSeparator(new MinWidthMenuSeparator(minPx));
   }
   
   public int getItemCount()
   {
      return menuBar_.getItemCount();
   }

   public List<MenuItem> getMenuItems() { return menuBar_.getMenuItems(); }

   public void focus()
   {
      menuBar_.focus();
   }
   
   public void setAutoHideRedundantSeparators(boolean value)
   {
      menuBar_.setAutoHideRedundantSeparators(value);
   }

   public void getDynamicPopupMenu(DynamicPopupMenuCallback callback)
   {
      callback.onPopupMenu(this);
   }

   private class ToolbarPopupMenuCommand implements ScheduledCommand
   {
      public ToolbarPopupMenuCommand(ScheduledCommand coreCommand)
      {
         coreCommand_ = coreCommand;
      }
      public void execute()
      {
         Scheduler.get().scheduleFinally(coreCommand_);
         hide();
         if (parent_ != null) parent_.hide();
      }
   
      private ScheduledCommand coreCommand_;
   }
   
   protected class ToolbarMenuBar extends BaseMenuBar
   {
      public ToolbarMenuBar(boolean vertical)
      {
         super(vertical);
      }
      
      @Override
      protected void onUnload()
      {
         nativePreviewReg_.removeHandler();
         super.onUnload();
      }

      @Override
      protected void onLoad()
      {
         super.onLoad();
         
         nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
         {
            public void onPreviewNativeEvent(NativePreviewEvent e)
            {
               if (e.getTypeInt() == Event.ONKEYDOWN)
               {
                  switch (e.getNativeEvent().getKeyCode())
                  {
                     case KeyCodes.KEY_ESCAPE:
                        e.cancel();
                        hide();
                        break;
                     case KeyCodes.KEY_DOWN:
                        e.cancel();
                        moveSelectionDown();
                        break;
                     case KeyCodes.KEY_UP:
                        e.cancel();
                        moveSelectionUp();
                        break;
                     case KeyCodes.KEY_PAGEDOWN:
                        e.cancel();
                        moveSelectionFwd(5);
                        break;
                     case KeyCodes.KEY_PAGEUP:
                        e.cancel();
                        moveSelectionBwd(5);
                        break;
                     case KeyCodes.KEY_HOME:
                        e.cancel();
                        selectFirst();
                        break;
                     case KeyCodes.KEY_END:
                        e.cancel();
                        selectLast();
                        break;
                     case KeyCodes.KEY_ENTER:
                     case KeyCodes.KEY_SPACE:
                        final MenuItem menuItem = getSelectedItem();
                        if (menuItem != null)
                        {
                           e.cancel();
                           NativeEvent evt = Document.get().createClickEvent(
                                 0,
                                 0,
                                 0,
                                 0,
                                 0,
                                 false,
                                 false,
                                 false,
                                 false);
                           menuItem.getElement().dispatchEvent(evt);
                        }
                        break;
                  }
               }
            }
         });
      }

      public int getItemCount()
      {
         return getItems().size();
      }

      public List<MenuItem> getMenuItems() { return getItems(); }
      
      public int getSelectedIndex()
      {
         MenuItem selectedMenuItem = getSelectedItem();
         List<MenuItem> menuItems = getItems();
         for (int i = 0; i<menuItems.size(); i++)
         {
            if (menuItems.get(i) == selectedMenuItem)
               return i;
         }
         return -1;
      }
      
      private void moveSelectionFwd(int numElements)
      {
         selectItem(getSelectedIndex() + numElements);
      }
      
      private void moveSelectionBwd(int numElements)
      {
         selectItem(getSelectedIndex() - numElements);
      }
      
      private void selectFirst()
      {
         selectItem(0);
      }
      
      private void selectLast()
      {
         selectItem(getItemCount());
      }

      private HandlerRegistration nativePreviewReg_;
   }
   
   public Element getMenuTableElement()
   {
      Element menuEl = getWidget().getElement();
      Node tableNode = DomUtils.findNode(menuEl, true, true, new NodePredicate()
      {
         @Override
         public boolean test(Node node)
         {
            if (!(node instanceof Element))
               return false;

            Element el = (Element) node;
            return el.hasTagName("table");
         }
      });
      
      if (tableNode == null)
         return null;
      
      return tableNode.cast();
      
   }

   @Override
   public void onCommand(AppCommand command)
   {
      if (command.getExecutedFromShortcut())
      {
         if (menuBar_.isVisible())
            menuBar_.setVisible(false);
      }
   }
   
   protected ToolbarMenuBar menuBar_;
   private ToolbarPopupMenu parent_;
   private EventBus events_;
   private HandlerRegistrations commandHandler_;
}
