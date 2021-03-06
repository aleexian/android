/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.gradle.run;

import com.android.ddmlib.IDevice;
import com.android.resources.Density;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.devices.Abi;
import com.android.tools.idea.fd.InstantRunBuilder;
import com.android.tools.idea.fd.InstantRunContext;
import com.android.tools.idea.fd.RunAsValidityService;
import com.android.tools.idea.gradle.GradleModel;
import com.android.tools.idea.gradle.GradleSyncState;
import com.android.tools.idea.gradle.compiler.AndroidGradleBuildConfiguration;
import com.android.tools.idea.gradle.facet.AndroidGradleFacet;
import com.android.tools.idea.gradle.invoker.GradleInvoker;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.project.GradleSyncListener;
import com.android.tools.idea.gradle.util.AndroidGradleSettings;
import com.android.tools.idea.gradle.util.BuildMode;
import com.android.tools.idea.run.AndroidDevice;
import com.android.tools.idea.run.AndroidRunConfigContext;
import com.android.tools.idea.run.AndroidRunConfigurationBase;
import com.android.tools.idea.run.DeviceFutures;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.ThreeState;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.android.builder.model.AndroidProject.*;
import static com.android.tools.idea.apk.ApkProjects.isApkProject;
import static com.android.tools.idea.gradle.util.Projects.*;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;

/**
 * Provides the "Gradle-aware Make" task for Run Configurations, which
 * <ul>
 * <li>is only available in Android Studio</li>
 * <li>delegates to the regular "Make" if the project is not an Android Gradle project</li>
 * <li>otherwise, invokes Gradle directly, to build the project</li>
 * </ul>
 */
public class MakeBeforeRunTaskProvider extends BeforeRunTaskProvider<MakeBeforeRunTask> {
  @NotNull public static final Key<MakeBeforeRunTask> ID = Key.create("Android.Gradle.BeforeRunTask");

  private static final Logger LOG = Logger.getInstance(MakeBeforeRunTask.class);
  public static final String TASK_NAME = "Gradle-aware Make";

  @NotNull private final Project myProject;

  public MakeBeforeRunTaskProvider(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public Key<MakeBeforeRunTask> getId() {
    return ID;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AndroidIcons.Android;
  }

  @Nullable
  @Override
  public Icon getTaskIcon(MakeBeforeRunTask task) {
    return AndroidIcons.Android;
  }

  @Override
  public String getName() {
    return TASK_NAME;
  }

  @Override
  public String getDescription(MakeBeforeRunTask task) {
    String goal = task.getGoal();
    return isEmpty(goal) ? TASK_NAME : "gradle " + goal;
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Nullable
  @Override
  public MakeBeforeRunTask createTask(RunConfiguration runConfiguration) {
    // "Gradle-aware Make" is only available in Android Studio.
    if (configurationTypeIsSupported(runConfiguration)) {
      MakeBeforeRunTask task = new MakeBeforeRunTask();
      if (runConfiguration instanceof AndroidRunConfigurationBase) {
        // For Android configurations, we want to replace the default make, so this new task needs to be enabled.
        // In AndroidRunConfigurationType#configureBeforeTaskDefaults we disable the default make, which is
        // enabled by default. For other configurations we leave it disabled, so we don't end up with two different
        // make steps executed by default. If the task is added to the run configuration manually, it will be
        // enabled by the UI layer later.
        task.setEnabled(true);
      }
      return task;
    }
    else {
      return null;
    }
  }

  private static boolean configurationTypeIsSupported(@NotNull RunConfiguration runConfiguration) {
    if (isApkProject(runConfiguration.getProject())) {
      return false;
    }
    return runConfiguration instanceof AndroidRunConfigurationBase || isUnitTestConfiguration(runConfiguration);
  }

  private static boolean isUnitTestConfiguration(@NotNull RunConfiguration runConfiguration) {
    return runConfiguration instanceof JUnitConfiguration ||
           // Avoid direct dependency on the TestNG plugin:
           runConfiguration.getClass().getSimpleName().equals("TestNGConfiguration");
  }

  @Override
  public boolean configureTask(RunConfiguration runConfiguration, MakeBeforeRunTask task) {
    GradleEditTaskDialog dialog = new GradleEditTaskDialog(myProject);
    dialog.setGoal(task.getGoal());
    dialog.setAvailableGoals(createAvailableTasks());
    if (!dialog.showAndGet()) {
      // since we allow tasks without any arguments (assumed to be equivalent to assembling the app),
      // we need a way to specify that a task is not valid. This is because of the current restriction
      // of this API, where the return value from configureTask is ignored.
      task.setInvalid();
      return false;
    }

    task.setGoal(dialog.getGoal());
    return true;
  }

  private List<String> createAvailableTasks() {
    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    List<String> gradleTasks = Lists.newArrayList();
    for (Module module : moduleManager.getModules()) {
      AndroidGradleFacet facet = AndroidGradleFacet.getInstance(module);
      if (facet == null) {
        continue;
      }

      GradleModel gradleModel = facet.getGradleModel();
      if (gradleModel == null) {
        continue;
      }

      gradleTasks.addAll(gradleModel.getTaskNames());
    }

    return gradleTasks;
  }

  @Override
  public boolean canExecuteTask(RunConfiguration configuration, MakeBeforeRunTask task) {
    return task.isValid();
  }

  @Override
  public boolean executeTask(DataContext context, RunConfiguration configuration, ExecutionEnvironment env, MakeBeforeRunTask task) {
    if (!requiresAndroidModel(myProject) || !isDirectGradleInvocationEnabled(myProject)) {
      CompileStepBeforeRun regularMake = new CompileStepBeforeRun(myProject);
      return regularMake.executeTask(context, configuration, env, new CompileStepBeforeRun.MakeBeforeRunTask());
    }

    AtomicReference<String> errorMsgRef = new AtomicReference<>();

    if (AndroidGradleBuildConfiguration.getInstance(myProject).SYNC_PROJECT_BEFORE_BUILD) {
      // If the model needs a sync, we need to sync "synchronously" before running.
      // See: https://code.google.com/p/android/issues/detail?id=70718
      GradleSyncState syncState = GradleSyncState.getInstance(myProject);
      if (syncState.isSyncNeeded() != ThreeState.NO) {
        GradleProjectImporter.getInstance().syncProjectSynchronously(myProject, false, new GradleSyncListener.Adapter() {
          @Override
          public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
            errorMsgRef.set(errorMessage);
          }
        });
      }
    }

    String errorMsg = errorMsgRef.get();
    if (errorMsg != null) {
      // Sync failed. There is no point on continuing, because most likely the model is either not there, or has stale information,
      // including the path of the APK.
      LOG.info("Unable to launch '" + TASK_NAME + "' task. Project sync failed with message: " + errorMsg);
      return false;
    }

    if (myProject.isDisposed()) {
      return false;
    }

    // Some configurations (e.g. native attach) don't require a build while running the configuration
    if (configuration instanceof RunConfigurationBase && ((RunConfigurationBase)configuration).excludeCompileBeforeLaunchOption()) {
      return true;
    }

    // Note: this before run task provider may be invoked from a context such as Java unit tests, in which case it doesn't have
    // the android run config context
    AndroidRunConfigContext runConfigContext = env.getCopyableUserData(AndroidRunConfigContext.KEY);
    DeviceFutures deviceFutures = runConfigContext == null ? null : runConfigContext.getTargetDevices();
    List<AndroidDevice> targetDevices = deviceFutures == null ? Collections.emptyList() : deviceFutures.getDevices();
    List<String> cmdLineArgs = getCommonArguments(configuration, targetDevices);

    BeforeRunBuilder builder =
      createBuilder(env, getModules(myProject, context, configuration), configuration, runConfigContext, task.getGoal());

    try {
      boolean success = builder.build(GradleTaskRunner.newRunner(myProject), cmdLineArgs);
      LOG.info("Gradle invocation complete, success = " + success);
      return success;
    }
    catch (InvocationTargetException e) {
      LOG.info("Unexpected error while launching gradle before run tasks", e);
      return false;
    }
    catch (InterruptedException e) {
      LOG.info("Interrupted while launching gradle before run tasks");
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Returns the list of arguments to Gradle that are common to both instant and non-instant builds.
   */
  @NotNull
  private static List<String> getCommonArguments(@NotNull RunConfiguration configuration, @NotNull List<AndroidDevice> targetDevices) {
    List<String> cmdLineArgs = Lists.newArrayList();
    cmdLineArgs.addAll(getDeviceSpecificArguments(targetDevices));
    cmdLineArgs.addAll(getProfilingOptions(configuration));
    return cmdLineArgs;
  }

  @NotNull
  public static List<String> getDeviceSpecificArguments(@NotNull List<AndroidDevice> devices) {
    if (devices.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> properties = new ArrayList<>(2);

    // Find the minimum value of the build API level and pass it to Gradle as a property
    AndroidVersion minVersion = devices.get(0).getVersion();
    for (int i = 1; i < devices.size(); i++) {
      AndroidDevice androidDevice = devices.get(i);
      if (androidDevice.getVersion().compareTo(minVersion) < 0) {
        minVersion = androidDevice.getVersion();
      }
    }
    properties.add(AndroidGradleSettings.createProjectProperty(PROPERTY_BUILD_API, Integer.toString(minVersion.getFeatureLevel())));

    // If we are building for only one device, pass the density and the ABI
    if (devices.size() == 1) {
      AndroidDevice device = devices.get(0);
      Density density = Density.getEnum(device.getDensity());
      if (density != null) {
        properties.add(AndroidGradleSettings.createProjectProperty(PROPERTY_BUILD_DENSITY, density.getResourceValue()));
      }

      // Note: the abis are returned in their preferred order which should be maintained while passing it on to Gradle.
      List<String> abis = device.getAbis()
        .stream()
        .map(Abi::toString)
        .collect(Collectors.toList());
      if (!abis.isEmpty()) {
        properties.add(AndroidGradleSettings.createProjectProperty(PROPERTY_BUILD_ABI, Joiner.on(',').join(abis)));
      }
    }

    return properties;
  }

  @NotNull
  public static List<String> getProfilingOptions(@NotNull RunConfiguration configuration) {
    return Collections.emptyList();
  }

  private static BeforeRunBuilder createBuilder(@NotNull ExecutionEnvironment env,
                                                @NotNull Module[] modules,
                                                @NotNull RunConfiguration configuration,
                                                @Nullable AndroidRunConfigContext runConfigContext,
                                                @Nullable String userGoal) {
    if (modules.length == 0) {
      throw new IllegalStateException("Unable to determine list of modules to build");
    }

    if (!isEmpty(userGoal)) {
      return new DefaultGradleBuilder(Collections.singletonList(userGoal), null);
    }

    GradleModuleTasksProvider gradleTasksProvider = new GradleModuleTasksProvider(modules);

    GradleInvoker.TestCompileType testCompileType = GradleInvoker.getTestCompileType(configuration.getType().getId());
    if (testCompileType == GradleInvoker.TestCompileType.JAVA_TESTS) {
      BuildMode buildMode = BuildMode.COMPILE_JAVA;
      return new DefaultGradleBuilder(gradleTasksProvider.getUnitTestTasks(buildMode), buildMode);
    }

    InstantRunContext irContext = env.getCopyableUserData(InstantRunContext.KEY);
    DeviceFutures deviceFutures = runConfigContext == null ? null : runConfigContext.getTargetDevices();
    if (deviceFutures == null || irContext == null) {
      return new DefaultGradleBuilder(gradleTasksProvider.getTasksFor(BuildMode.ASSEMBLE, testCompileType), BuildMode.ASSEMBLE);
    }

    List<AndroidDevice> targetDevices = deviceFutures.getDevices();
    assert targetDevices.size() == 1 : "instant run context available, but deploying to > 1 device";
    return new InstantRunBuilder(getLaunchedDevice(targetDevices.get(0)), irContext, runConfigContext, gradleTasksProvider,
                                 RunAsValidityService.getInstance());
  }

  @NotNull
  private static Module[] getModules(@NotNull Project project, @Nullable DataContext context, @Nullable RunConfiguration configuration) {
    if (configuration instanceof ModuleRunProfile) {
      // ModuleBasedConfiguration includes Android and JUnit run configurations, including "JUnit: Rerun Failed Tests",
      // which is AbstractRerunFailedTestsAction.MyRunProfile.
      return ((ModuleRunProfile)configuration).getModules();
    }
    else {
      return getModulesToBuildFromSelection(project, context);
    }
  }

  @Nullable
  public static IDevice getLaunchedDevice(@NotNull AndroidDevice device) {
    if (!device.getLaunchedDevice().isDone()) {
      // If we don't have access to the device (this happens if the AVD is still launching)
      return null;
    }

    try {
      return device.getLaunchedDevice().get(1, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
    catch (ExecutionException | TimeoutException e) {
      return null;
    }
  }
}
