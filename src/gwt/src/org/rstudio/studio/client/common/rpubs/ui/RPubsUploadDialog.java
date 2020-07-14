/*
 * RPubsUploadDialog.java
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

package org.rstudio.studio.client.common.rpubs.ui;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressImage;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.RPubsUploader;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.rsconnect.model.StaticHtmlGenerator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RPubsUploadDialog extends ModalDialogBase
{
   public RPubsUploadDialog(String contextId,
                            String title, 
                            String rmdFile,
                            String htmlFile, 
                            String uploadId,
                            boolean isPublished)
   {
      super(Roles.getDialogRole());
      RStudioGinjector.INSTANCE.injectMembers(this);
      setText("Publish to RPubs");
      title_ = title;
      htmlFile_ = htmlFile;
      rmdFile_ = rmdFile;
      isPublished_ = isPublished;
      contextId_ = contextId;
      uploadId_ = uploadId == null ? "" : uploadId;
      uploader_ = new RPubsUploader(server_, globalDisplay_, eventBus_, contextId_);
      uploader_.setOnUploadComplete(arg -> closeDialog());
   }

   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   EventBus eventBus,
                   RPubsServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      Styles styles = RESOURCES.styles();
      
      SimplePanel mainPanel = new SimplePanel();
      mainPanel.addStyleName(styles.mainWidget());
      
      VerticalPanel verticalPanel = new VerticalPanel();
  
      HorizontalPanel headerPanel = new HorizontalPanel();
      headerPanel.addStyleName(styles.headerPanel());
      headerPanel.add(new DecorativeImage(new ImageResource2x(RESOURCES.publishLarge2x())));
      
      Label headerLabel = new Label("Publish to RPubs");
      headerLabel.addStyleName(styles.headerLabel());
      headerPanel.add(headerLabel);
      headerPanel.setCellVerticalAlignment(headerLabel,
                                           HasVerticalAlignment.ALIGN_MIDDLE);
      
      verticalPanel.add(headerPanel);

      String msg;
      if (!isPublished_ && uploadId_.isEmpty())
      {
         msg = "RPubs is a free service from RStudio for sharing " +
                       "documents on the web. Click Publish to get " +
                       "started.";
      }
      else
      {
         msg = "This document has already been published on RPubs. You can " +
               "choose to either update the existing RPubs document, or " +
               "create a new one.";
      }
      Label descLabel = new Label(msg);
      descLabel.addStyleName(styles.descLabel());
      verticalPanel.add(descLabel);
      setARIADescribedBy(descLabel.getElement());

      HTML warningLabel =  new HTML(
        "<strong>IMPORTANT: All documents published to RPubs are " +
        "publicly visible.</strong> You should " +
        "only publish documents that you wish to share publicly.");
      warningLabel.addStyleName(styles.warningLabel());
      verticalPanel.add(warningLabel);
        
      ThemedButton cancelButton = createCancelButton(new Operation() {
         @Override
         public void execute()
         {
            // if an upload is in progress then terminate it
            if (uploader_.isUploadInProgress())
            {
               uploader_.terminateUpload();
            }
         }
         
      });

      continueButton_ = new ThemedButton("Publish", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {   
            performUpload(false);
         }
      });

      updateButton_ = new ThemedButton("Update Existing", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            performUpload(true);
         }
      });

      createButton_ = new ThemedButton("Create New", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            performUpload(false);
         }
      });

      if (!isPublished_ && uploadId_.isEmpty())
      {
         addOkButton(continueButton_);
         addCancelButton(cancelButton);
      }
      else
      {
         addOkButton(updateButton_);
         addButton(createButton_, ElementIds.CREATE_BUTTON);
         addCancelButton(cancelButton);
      }
     
      mainPanel.setWidget(verticalPanel);
      return mainPanel;
   }

   protected void onUnload()
   {
      super.onUnload();
   }
   
   private String getTitleText()
   {
      return title_;
   }

   private String getCommentText()
   {
      return "";
   }

   private void performUpload(final boolean modify)
   {
      showProgressPanel();

      // synthesize html generator
      StaticHtmlGenerator htmlGenerator = new StaticHtmlGenerator() {
         @Override
         public void generateStaticHtml(String title,
                                       String comment,
                                       CommandWithArg<String> onCompleted)
         {
            onCompleted.execute(htmlFile_);
         }
      };

      // generate html and initiate the upload
      final String title = getTitleText();
      htmlGenerator.generateStaticHtml(
          title, getCommentText(), new CommandWithArg<String>() {
         @Override
         public void execute(String htmlFile)
         {
            if (modify)
               uploader_.performUpload(title, rmdFile_, htmlFile, 
                     uploadId_, modify);
            else
               uploader_.performUpload(title, rmdFile_, htmlFile, "", false);
         }
      });
   }
   
   private void showProgressPanel()
   {
      // disable continue button
      continueButton_.setVisible(false);
      updateButton_.setVisible(false);
      createButton_.setVisible(false);
      enableOkButton(false);
      
      // add progress
      HorizontalPanel progressPanel = new HorizontalPanel();
      ProgressImage progressImage =  new ProgressImage(CoreResources.INSTANCE.progress_gray());
      progressImage.addStyleName(RESOURCES.styles().progressImage());
      progressImage.show(true);
      progressPanel.add(progressImage);
      progressPanel.add(new Label(RPubsUploader.PROGRESS_MESSAGE));
      addLeftWidget(progressPanel);
   }
   
   interface Styles extends CssResource
   {
      String mainWidget();
      String headerPanel();
      String headerLabel();
      String descLabel();
      String progressImage();
      String warningLabel();
   }
  
   interface Resources extends ClientBundle
   {
      @Source("RPubsUploadDialog.css")
      Styles styles();
      
      @Source("publishLarge_2x.png")
      ImageResource publishLarge2x();
   }

   private final boolean isPublished_;

   static Resources RESOURCES = GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }

   private ThemedButton continueButton_;
   private ThemedButton updateButton_;
   private ThemedButton createButton_;

   private final String title_;
   private final String htmlFile_;
   private final String rmdFile_;
   private final String contextId_;
   private final String uploadId_;

   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private RPubsServerOperations server_;
   private RPubsUploader uploader_;
}
