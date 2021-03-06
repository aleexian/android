/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.run.tasks;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.editors.gfxtrace.GfxTraceEditor;
import com.android.tools.idea.editors.gfxtrace.GfxTracer;
import com.android.tools.idea.editors.gfxtrace.actions.GfxTraceCaptureAction;
import com.android.tools.idea.editors.gfxtrace.forms.TraceDialog;
import com.android.tools.idea.profiling.capture.CaptureService;
import com.android.tools.idea.run.AndroidRunConfigurationBase;
import com.android.tools.idea.run.ConsolePrinter;
import com.android.tools.idea.run.LaunchOptions;
import com.android.tools.idea.run.util.LaunchStatus;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GapidTraceTask implements LaunchTask {
  private static final NotificationGroup DEBUG_NOTIFICATION_GROUP =
    NotificationGroup.toolWindowGroup(GfxTraceEditor.NOTIFICATION_GROUP, ToolWindowId.DEBUG);
  private static final String NOTIFICATION_DEBUG_TITLE = "Cannot trace while debugging";
  private static final String NOTIFICATION_DEBUG_MESSAGE = "Cannot capture a graphics trace while debugging the application. " +
                                                           "Tracing has been disabled for this run.";


  private final AndroidRunConfigurationBase myConfiguration;
  private final String myApplicationId;

  public GapidTraceTask(@NotNull AndroidRunConfigurationBase configuration, @NotNull String applicationId) {
    myConfiguration = configuration;
    myApplicationId = applicationId;
  }

  public static boolean checkIfOkToTrace(@Nullable Project project, @NotNull LaunchOptions launchOptions) {
    if (launchOptions.isDebug()) {
      DEBUG_NOTIFICATION_GROUP
        .createNotification(NOTIFICATION_DEBUG_TITLE, NOTIFICATION_DEBUG_MESSAGE, NotificationType.WARNING, null).notify(project);
      return false;
    }
    return true;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Starts listening for a GPU trace";
  }

  @Override
  public int getDuration() {
    return LaunchTaskDurations.ASYNC_TASK;
  }

  @Override
  public boolean perform(final @NotNull IDevice device, @NotNull LaunchStatus launchStatus, @NotNull ConsolePrinter printer) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        final TraceDialog dialog = new TraceDialog();
        dialog.setListener(new TraceDialog.Listener() {
          private GfxTracer myTracer = null;

          @Override
          public void onStartTrace(@NotNull String name) {
            GfxTracer.Options options = GfxTracer.Options.fromRunConfiguration(myConfiguration);
            options.myTraceName = name;
            myTracer = GfxTracer.listen(
              myConfiguration.getProject(), device, myApplicationId, options, GfxTraceCaptureAction.bindListener(dialog));
          }

          @Override
          public void onStopTrace() {
            myTracer.stop();
          }

          @Override
          public void onCancelTrace() {
          }
        });

        dialog.setLocationRelativeTo(JOptionPane.getRootFrame());
        dialog.setDefaultName(CaptureService.getInstance(myConfiguration.getProject()).getSuggestedName(device.getClient(myApplicationId)));
        dialog.setVisible(true);
      }
    });
    return true;
  }
}
