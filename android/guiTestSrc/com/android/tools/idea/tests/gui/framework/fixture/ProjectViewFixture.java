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
package com.android.tools.idea.tests.gui.framework.fixture;

import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.Wait;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.ExternalLibrariesNode;
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElement;
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElementNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.content.BaseLabel;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.ui.tree.TreeUtil;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProjectViewFixture extends ToolWindowFixture {
  ProjectViewFixture(@NotNull Project project, @NotNull Robot robot) {
    super("Project", project, robot);
  }

  @NotNull
  public PaneFixture selectProjectPane() {
    activate();
    final ProjectView projectView = ProjectView.getInstance(myProject);

    if (!"ProjectView".equals(projectView.getCurrentViewId())) {
      changePane("Project");
    }

    return new PaneFixture(projectView.getCurrentProjectViewPane());
  }

  @NotNull
  public PaneFixture selectAndroidPane() {
    activate();
    final ProjectView projectView = ProjectView.getInstance(myProject);

    if (!"AndroidView".equals(projectView.getCurrentViewId())) {
      changePane("Android");
    }

    return new PaneFixture(projectView.getCurrentProjectViewPane());
  }

  /**
   * Given a list of relative paths, finds if they all belong to the Project.
   * @param paths The list of relative paths with / used as separators
   */
  public void assertFilesExist(@NotNull String... paths) {
    VirtualFile baseDir = myProject.getBaseDir();
    for (String path : paths) {
      VirtualFile file = baseDir.findFileByRelativePath(path);
      assertTrue("File doesn't exist: " + path, file != null && file.exists());
    }
  }

  private void changePane(@NotNull String paneName) {
    Component projectDropDown = GuiTests.waitUntilFound(myRobot, new GenericTypeMatcher<BaseLabel>(BaseLabel.class) {
      @Override
      protected boolean isMatching(@NotNull BaseLabel component) {
        return "Project:".equals(component.getText());
      }
    });

    myRobot.click(projectDropDown.getParent());
    GuiTests.clickPopupMenuItem(paneName, projectDropDown, myRobot);
  }

  public static class PaneFixture {
    @NotNull private final AbstractProjectViewPane myPane;

    PaneFixture(@NotNull AbstractProjectViewPane pane) {
      myPane = pane;
    }

    @NotNull
    public PaneFixture expand() {
      GuiActionRunner.execute(new GuiTask() {
        @Override
        protected void executeInEDT() throws Throwable {
          TreeUtil.expandAll(myPane.getTree());
        }
      });
      return this;
    }

    @NotNull
    private AbstractTreeStructure getTreeStructure() {
      final AtomicReference<AbstractTreeStructure> treeStructureRef = new AtomicReference<>();
      Wait.minutes(2).expecting("AbstractTreeStructure to be built")
        .until(() -> {
          AbstractTreeStructure treeStructure = GuiActionRunner.execute(new GuiQuery<AbstractTreeStructure>() {
            @Override
            protected AbstractTreeStructure executeInEDT() throws Throwable {
              try {
                return myPane.getTreeBuilder().getTreeStructure();
              }
              catch (NullPointerException e) {
                // expected;
              }
              return null;
            }
          });
          treeStructureRef.set(treeStructure);
          return treeStructure != null;
        });

      return treeStructureRef.get();
    }

    @NotNull
    public NodeFixture findExternalLibrariesNode() {
      final AbstractTreeStructure treeStructure = getTreeStructure();

      ExternalLibrariesNode node = GuiActionRunner.execute(new GuiQuery<ExternalLibrariesNode>() {
        @Nullable
        @Override
        protected ExternalLibrariesNode executeInEDT() throws Throwable {
          Object[] childElements = treeStructure.getChildElements(treeStructure.getRootElement());
          for (Object child : childElements) {
            if (child instanceof ExternalLibrariesNode) {
              return (ExternalLibrariesNode)child;
            }
          }
          return null;
        }
      });
      if (node != null) {
        return new NodeFixture(node, treeStructure);
      }
      throw new AssertionError("Unable to find 'External Libraries' node");
    }

    public void selectByPath(@NotNull final String... paths) {
      final AbstractTreeStructure treeStructure = getTreeStructure();

      final PsiDirectoryNode node = GuiActionRunner.execute(new GuiQuery<PsiDirectoryNode>() {
        @Nullable
        @Override
        protected PsiDirectoryNode executeInEDT() throws Throwable {
          Object root = treeStructure.getRootElement();
          final List<Object> treePath = Lists.newArrayList(root);

          for (String path : paths) {
            Object[] childElements = treeStructure.getChildElements(root);
            Object newRoot = null;
            for (Object child : childElements) {
              if (child instanceof PsiDirectoryNode) {
                PsiDirectory dir = ((PsiDirectoryNode)child).getValue();
                if (dir != null && path.equals(dir.getName())) {
                  newRoot = child;
                  treePath.add(newRoot);
                  break;
                }
              }
            }
            if (newRoot != null) {
              root = newRoot;
            }
            else {
              return null;
            }
          }
          if (root == treeStructure.getRootElement()) {
            return null;
          }

          myPane.expand(treePath.toArray(), true);
          myPane.select(root, ((PsiDirectoryNode)root).getVirtualFile(), true);
          return (PsiDirectoryNode)root;
        }
      });

      assertNotNull(node);

      Wait.minutes(2).expecting("node to be selected").until(
        () -> node.equals(GuiActionRunner.execute(
          new GuiQuery<Object>() {
            @Override
            protected Object executeInEDT() throws Throwable {
              DefaultMutableTreeNode selectedNode = myPane.getSelectedNode();
              if (selectedNode != null) {
                return selectedNode.getUserObject();
              }
              return null;
            }
          })));
    }
  }

  public static class NodeFixture {
    @NotNull private final ProjectViewNode<?> myNode;
    @NotNull private final AbstractTreeStructure myTreeStructure;

    NodeFixture(@NotNull ProjectViewNode<?> node, @NotNull AbstractTreeStructure treeStructure) {
      myNode = node;
      myTreeStructure = treeStructure;
    }

    @NotNull
    public List<NodeFixture> getChildren() {
      final List<NodeFixture> children = Lists.newArrayList();
      GuiActionRunner.execute(new GuiTask() {
        @Override
        protected void executeInEDT() throws Throwable {
          for (Object child : myTreeStructure.getChildElements(myNode)) {
            if (child instanceof ProjectViewNode) {
              children.add(new NodeFixture((ProjectViewNode<?>)child, myTreeStructure));
            }
          }
        }
      });
      return children;
    }

    public boolean isJdk() {
      if (myNode instanceof NamedLibraryElementNode) {
        NamedLibraryElement value = ((NamedLibraryElementNode)myNode).getValue();
        assertNotNull(value);
        LibraryOrSdkOrderEntry orderEntry = value.getOrderEntry();
        if (orderEntry instanceof JdkOrderEntry) {
          Sdk sdk = ((JdkOrderEntry)orderEntry).getJdk();
          return sdk.getSdkType() instanceof JavaSdk;
        }
      }
      return false;
    }

    @NotNull
    public NodeFixture requireDirectory(@NotNull String name) {
      assertThat(myNode).isInstanceOf(PsiDirectoryNode.class);
      VirtualFile file = myNode.getVirtualFile();
      assertNotNull(file);
      assertThat(file.getName()).isEqualTo(name);
      return this;
    }

    @Override
    public String toString() {
      return Strings.nullToEmpty(myNode.getName());
    }
  }
}
