/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.framework;

import com.android.SdkConstants;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.android.tools.idea.gradle.util.LocalProperties;
import com.android.tools.idea.sdk.IdeSdks;
import com.android.tools.idea.templates.AndroidGradleTestCase;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.android.tools.idea.tests.gui.framework.fixture.WelcomeFrameFixture;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.WaitTimedOutError;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.AssumptionViolatedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.TruthJUnit.assume;
import static org.fest.reflect.core.Reflection.*;
import static org.junit.Assert.assertNotNull;

public class GuiTestRule implements TestRule {

  /** Hack to solve focus issue when running with no window manager */
  private static final boolean HAS_EXTERNAL_WINDOW_MANAGER = Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH);
  private final RobotTestRule myRobotTestRule = new RobotTestRule();
  private final LeakCheck myLeakCheck = new LeakCheck();
  private final RuleChain myRuleChain = RuleChain.emptyRuleChain()
    .around(new BlockReloading())
    .around(myRobotTestRule)
    .around(myLeakCheck)
    .around(new IdeHandling())
    .around(new TestPerformance())
    .around(new ScreenshotOnFailure())
    .around(new Timeout(5, TimeUnit.MINUTES));
  private final PropertyChangeListener myGlobalFocusListener = e -> {
    Object oldValue = e.getOldValue();
    if ("permanentFocusOwner".equals(e.getPropertyName()) && oldValue instanceof Component && e.getNewValue() == null) {
      Window parentWindow = oldValue instanceof Window ? (Window)oldValue : SwingUtilities.getWindowAncestor((Component)oldValue);
      if (parentWindow instanceof Dialog) {
        Container parent = parentWindow.getParent();
        if (parent != null && parent.isVisible()) {
          System.out.println("Focus Listener: Request focus!");
          IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
            IdeFocusManager.getGlobalInstance().requestFocus(parent, true);
          });
        }
      }
    }
  };
  private File myProjectPath;
  private IdeFrameFixture myIdeFrameFixture;

  public GuiTestRule withLeakCheck() {
    myLeakCheck.setEnabled(true);
    return this;
  }

  @NotNull
  @Override
  public Statement apply(final Statement base, final Description description) {
    return myRuleChain.apply(base, description);
  }

  private void assumeOnlyWelcomeFrameShowing() {
    try {
      WelcomeFrameFixture.find(robot());
    } catch (WaitTimedOutError e) {
      throw new AssumptionViolatedException("didn't find welcome frame", e);
    }
    assume().that(GuiTests.windowsShowing()).named("windows showing").hasSize(1);
  }

  private void setUp() {
    GuiTests.setUpDefaultProjectCreationLocationPath();
    GuiTests.setIdeSettings();
    GuiTests.setUpSdks();

    if (!HAS_EXTERNAL_WINDOW_MANAGER) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(myGlobalFocusListener);
    }
  }

  private void tearDown(@NotNull List<Throwable> errors) {
    try {
      waitForBackgroundTasks();
    }
    catch (Throwable e) {
      errors.add(e);
    }
    errors.addAll(checkForModalDialogs());
    if (myProjectPath != null) {
      if (ideFrame().target().isShowing()) {
        ideFrame().closeProject();
      }
      FileUtilRt.delete(myProjectPath);
      GuiTests.refreshFiles();
    }
    if (!HAS_EXTERNAL_WINDOW_MANAGER) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(myGlobalFocusListener);
    }
    errors.addAll(GuiTests.fatalErrorsFromIde());

    fixMemLeaks();
  }

  private List<AssertionError> checkForModalDialogs() {
    List<AssertionError> errors = new ArrayList<>();
    // We close all modal dialogs left over, because they block the AWT thread and could trigger a deadlock in the next test.
    Dialog modalDialog;
    while ((modalDialog = getActiveModalDialog()) != null) {
      robot().close(modalDialog);
      errors.add(new AssertionError(
        String.format("Modal dialog showing: %s with title '%s'", modalDialog.getClass().getName(), modalDialog.getTitle())));
    }
    return errors;
  }

  private void fixMemLeaks() {
    myIdeFrameFixture = null;

    // Work-around for https://youtrack.jetbrains.com/issue/IDEA-153492
    Class<?> keyboardManagerType = type("javax.swing.KeyboardManager").load();
    Object manager = method("getCurrentManager").withReturnType(Object.class).in(keyboardManagerType).invoke();
    assertNotNull(manager);

    Map componentKeyStrokeMap = field("componentKeyStrokeMap").ofType(Hashtable.class).in(manager).get();
    assertNotNull(componentKeyStrokeMap);
    componentKeyStrokeMap.clear();

    Map containerMap = field("containerMap").ofType(Hashtable.class).in(manager).get();
    assertNotNull(containerMap);
    containerMap.clear();
  }

  public IdeFrameFixture importSimpleApplication() throws IOException {
    return importProjectAndWaitForProjectSyncToFinish("SimpleApplication");
  }

  public IdeFrameFixture importMultiModule() throws IOException {
    return importProjectAndWaitForProjectSyncToFinish("MultiModule");
  }

  public IdeFrameFixture importProjectAndWaitForProjectSyncToFinish(@NotNull String projectDirName) throws IOException {
    return importProjectAndWaitForProjectSyncToFinish(projectDirName, null);
  }

  public IdeFrameFixture importProjectAndWaitForProjectSyncToFinish(@NotNull String projectDirName, @Nullable String gradleVersion)
    throws IOException {
    return importProject(projectDirName, gradleVersion).waitForGradleProjectSyncToFinish();
  }

  public IdeFrameFixture importProject(@NotNull String projectDirName) throws IOException {
    return importProject(projectDirName, null);
  }

  private IdeFrameFixture importProject(@NotNull String projectDirName, String gradleVersion) throws IOException {
    setUpProject(projectDirName, gradleVersion);
    final VirtualFile toSelect = checkNotNull(VfsUtil.findFileByIoFile(myProjectPath, true));
    GuiActionRunner.execute(new GuiTask() {
      @Override
      protected void executeInEDT() throws Throwable {
        GradleProjectImporter.getInstance().importProject(toSelect);
      }
    });
    return ideFrame();
  }

  /**
   * Sets up a project before using it in a UI test:
   * <ul>
   * <li>Makes a copy of the project in testData/guiTests/newProjects (deletes any existing copy of the project first.) This copy is
   * the one the test will use.</li>
   * <li>Creates a Gradle wrapper for the test project.</li>
   * <li>Updates the version of the Android Gradle plug-in used by the project, if applicable</li>
   * <li>Creates a local.properties file pointing to the Android SDK path specified by the system property (or environment variable)
   * 'ADT_TEST_SDK_PATH'</li>
   * <li>Copies over missing files to the .idea directory (if the project will be opened, instead of imported.)</li>
   * <li>Deletes .idea directory, .iml files and build directories, if the project will be imported.</li>
   * <p/>
   * </ul>
   *
   * @param projectDirName             the name of the project's root directory. Tests are located in testData/guiTests.
   * @param gradleVersion              the Gradle version to use in the wrapper. If {@code null} is passed, this method will use the latest supported
   *                                   version of Gradle.
   * @throws IOException if an unexpected I/O error occurs.
   */
  private void setUpProject(@NotNull String projectDirName,
                            @Nullable String gradleVersion) throws IOException {
    copyProjectBeforeOpening(projectDirName);

    File gradlePropertiesFilePath = new File(myProjectPath, SdkConstants.FN_GRADLE_PROPERTIES);
    if (gradlePropertiesFilePath.isFile()) {
      FileUtilRt.delete(gradlePropertiesFilePath);
    }

    if (gradleVersion == null) {
      createGradleWrapper(myProjectPath, SdkConstants.GRADLE_LATEST_VERSION);
    }
    else {
      createGradleWrapper(myProjectPath, gradleVersion);
    }

    updateGradleVersions(myProjectPath);
    updateLocalProperties(myProjectPath);
    cleanUpProjectForImport(myProjectPath);
  }

  public void copyProjectBeforeOpening(@NotNull String projectDirName) throws IOException {
    File masterProjectPath = getMasterProjectDirPath(projectDirName);

    setProjectPath(getTestProjectDirPath(projectDirName));
    if (myProjectPath.isDirectory()) {
      FileUtilRt.delete(myProjectPath);
    }
    FileUtil.copyDir(masterProjectPath, myProjectPath);
    System.out.println(String.format("Copied project '%1$s' to path '%2$s'", projectDirName, myProjectPath.getPath()));
  }

  protected boolean createGradleWrapper(@NotNull File projectDirPath, @NotNull String gradleVersion) throws IOException {
    return GradleUtil.createGradleWrapper(projectDirPath, gradleVersion);
  }

  protected void updateLocalProperties(File projectPath) throws IOException {
    LocalProperties localProperties = new LocalProperties(projectPath);
    localProperties.setAndroidSdkPath(checkNotNull(IdeSdks.getAndroidSdkPath()));
    localProperties.save();
  }

  protected void updateGradleVersions(@NotNull File projectPath) throws IOException {
    AndroidGradleTestCase.updateGradleVersions(projectPath);
  }

  @NotNull
  protected File getMasterProjectDirPath(@NotNull String projectDirName) {
    return new File(GuiTests.getTestProjectsRootDirPath(), projectDirName);
  }

  @NotNull
  protected File getTestProjectDirPath(@NotNull String projectDirName) {
    return new File(GuiTests.getProjectCreationDirPath(), projectDirName);
  }

  public void cleanUpProjectForImport(@NotNull File projectPath) {
    File dotIdeaFolderPath = new File(projectPath, Project.DIRECTORY_STORE_FOLDER);
    if (dotIdeaFolderPath.isDirectory()) {
      File modulesXmlFilePath = new File(dotIdeaFolderPath, "modules.xml");
      if (modulesXmlFilePath.isFile()) {
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
          Document document = saxBuilder.build(modulesXmlFilePath);
          XPath xpath = XPath.newInstance("//*[@fileurl]");
          //noinspection unchecked
          List<Element> modules = xpath.selectNodes(document);
          int urlPrefixSize = "file://$PROJECT_DIR$/".length();
          for (Element module : modules) {
            String fileUrl = module.getAttributeValue("fileurl");
            if (!StringUtil.isEmpty(fileUrl)) {
              String relativePath = FileUtil.toSystemDependentName(fileUrl.substring(urlPrefixSize));
              File imlFilePath = new File(projectPath, relativePath);
              if (imlFilePath.isFile()) {
                FileUtilRt.delete(imlFilePath);
              }
              // It is likely that each module has a "build" folder. Delete it as well.
              File buildFilePath = new File(imlFilePath.getParentFile(), "build");
              if (buildFilePath.isDirectory()) {
                FileUtilRt.delete(buildFilePath);
              }
            }
          }
        }
        catch (Throwable ignored) {
          // if something goes wrong, just ignore. Most likely it won't affect project import in any way.
        }
      }
      FileUtilRt.delete(dotIdeaFolderPath);
    }
  }

  public void waitForBackgroundTasks() {
    GuiTests.waitForBackgroundTasks(robot());
  }

  public Robot robot() {
    return myRobotTestRule.getRobot();
  }

  @NotNull
  public File getProjectPath() {
    checkState(myProjectPath != null, "No project path set. Was a project imported?");
    return myProjectPath;
  }

  public void setProjectPath(@NotNull File projectPath) {
    myProjectPath = projectPath;
    myIdeFrameFixture = null;
  }

  @NotNull
  public IdeFrameFixture ideFrame() {
    if (myIdeFrameFixture == null || myIdeFrameFixture.isClosed()) {
      // This call to find() creates a new IdeFrameFixture object every time. Each of these Objects creates a new gradleProjectEventListener
      // and registers it with GradleSyncState. This keeps adding more and more listeners, and the new recent listeners are only updated
      // with gradle State when that State changes. This means the listeners may have outdated info.
      myIdeFrameFixture = IdeFrameFixture.find(robot(), getProjectPath(), null);
      myIdeFrameFixture.requestFocusIfLost();
    }
    return myIdeFrameFixture;
  }

  // Note: this works with a cooperating window manager that returns focus properly. It does not work on bare Xvfb.
  private static Dialog getActiveModalDialog() {
    Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (activeWindow instanceof Dialog) {
      Dialog dialog = (Dialog)activeWindow;
      if (dialog.getModalityType() == Dialog.ModalityType.APPLICATION_MODAL) {
        return dialog;
      }
    }
    return null;
  }

  private class IdeHandling implements TestRule {
    @NotNull
    @Override
    public Statement apply(final Statement base, final Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          System.out.println("Starting " + description.getDisplayName());
          assume().that(GuiTests.fatalErrorsFromIde()).named("IDE errors").isEmpty();
          assumeOnlyWelcomeFrameShowing();
          setUp();
          List<Throwable> errors = new ArrayList<>();
          try {
            base.evaluate();
          }
          catch (MultipleFailureException e) {
            errors.addAll(e.getFailures());
          }
          catch (Throwable e) {
            errors.add(e);
          }
          finally {
            tearDown(errors);
          }
          MultipleFailureException.assertEmpty(errors);
        }
      };
    }
  }
}
