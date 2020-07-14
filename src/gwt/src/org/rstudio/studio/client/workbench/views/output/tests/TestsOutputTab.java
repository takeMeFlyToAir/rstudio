
/*
 * TestsOutputTab.java
 *
 * Copyright (C) 2009-18 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.output.tests;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildErrorsEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;

public class TestsOutputTab 
   extends DelayLoadWorkbenchTab<TestsOutputPresenter> 
   implements ProvidesBusy
{
   public abstract static class Shim extends
                DelayLoadTabShim<TestsOutputPresenter, TestsOutputTab>
      implements BuildStartedEvent.Handler,
                 BuildOutputEvent.Handler,
                 BuildCompletedEvent.Handler,
                 BuildErrorsEvent.Handler,
                 RestartStatusEvent.Handler,
                 ProvidesBusy
   {
      abstract void initialize();
      abstract void confirmClose(Command onConfirmed);
   }

   @Inject
   public TestsOutputTab(Shim shim,
                         EventBus events,
                         final Session session)
   {
      super("Tests", shim);
      shim_ = shim;

      events.addHandler(BuildStartedEvent.TYPE, shim);
      events.addHandler(BuildOutputEvent.TYPE, shim);
      events.addHandler(BuildCompletedEvent.TYPE, shim);
      events.addHandler(BuildErrorsEvent.TYPE, shim);
      events.addHandler(RestartStatusEvent.TYPE, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }
   
   @Override
   public void confirmClose(Command onConfirmed)
   {
      shim_.confirmClose(onConfirmed);
   }
   
   @Override
   public void addBusyHandler(BusyHandler handler)
   {
      shim_.addBusyHandler(handler);
   }
   
   private Shim shim_;
}
