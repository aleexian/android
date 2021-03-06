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
package com.android.tools.idea.uibuilder.handlers.constraint;

import com.android.SdkConstants;
import com.android.tools.idea.uibuilder.model.AttributesTransaction;
import com.android.tools.sherpa.structure.WidgetCompanion;
import com.android.tools.idea.uibuilder.api.DragHandler;
import com.android.tools.idea.uibuilder.api.DragType;
import com.android.tools.idea.uibuilder.api.ViewEditor;
import com.android.tools.idea.uibuilder.graphics.NlGraphics;
import com.android.tools.idea.uibuilder.model.AndroidCoordinate;
import com.android.tools.idea.uibuilder.model.NlComponent;
import android.support.constraint.solver.widgets.ConstraintWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handles drag'n drop on a ConstraintLayout
 */
public class ConstraintDragHandler extends DragHandler {

  private ConstraintWidget myDragWidget;
  private NlComponent myComponent;

  public ConstraintDragHandler(@NotNull ViewEditor editor,
                               @NotNull ConstraintLayoutHandler constraintLayoutHandler,
                               @NotNull NlComponent layout,
                               @NotNull List<NlComponent> components, DragType type) {
    super(editor, constraintLayoutHandler, layout, components, type);
    if (components.size() == 1) {
      myComponent = components.get(0);
      ConstraintModel model = ConstraintModel.getConstraintModel(editor.getModel());
      if (model.getScene().getWidget(myComponent) == null) {
        myDragWidget = new ConstraintWidget();
        WidgetCompanion companion = WidgetCompanion.create(myDragWidget);
        companion.setWidgetModel(myComponent);
        companion.setWidgetTag(myComponent);
        myDragWidget.setCompanionWidget(companion);
      } else {
        // For now, only deal with drag handler on ConstraintLayout here if it's an external DND
        // (ConstraintInteraction will handle the rest)
        myComponent = null;
      }
    }
  }

  @Override
  public void start(@AndroidCoordinate int x, @AndroidCoordinate int y, int modifiers) {
    super.start(x, y, modifiers);
    ConstraintModel model = ConstraintModel.getConstraintModel(editor.getModel());
    if (myComponent != null && myDragWidget != null
        && model != null && model.getScene() != null && model.getScene().getRoot() != null) {
      model.getSelection().clear();
      model.getScene().getRoot().add(myDragWidget);
      model.getScene().addWidget(myDragWidget);
      int ax = model.pxToDp(x - this.layout.x - this.layout.getPadding().left - myComponent.w / 2);
      int ay = model.pxToDp(y - this.layout.y - this.layout.getPadding().top - myComponent.h / 2);
      myDragWidget.setDimension(model.pxToDp(myComponent.w), model.pxToDp(myComponent.h));
      myDragWidget.setX(ax);
      myDragWidget.setY(ay);
      model.setDragDropWidget(myDragWidget);
      model.getSelection().add(myDragWidget);
    }
  }

  @Nullable
  @Override
  public String update(@AndroidCoordinate int x, @AndroidCoordinate int y, int modifiers) {
    String result = super.update(x, y, modifiers);
    if (myComponent != null && myDragWidget != null) {
      ConstraintModel model = ConstraintModel.getConstraintModel(editor.getModel());
      int ax = x - this.layout.x - this.layout.getPadding().left - myComponent.w / 2;
      int ay = y - this.layout.y - this.layout.getPadding().top - myComponent.h / 2;
      model.dragComponent(ax, ay);
    }
    return result;
  }

  @Override
  public void cancel() {
    if (myDragWidget != null) {
      ConstraintModel model = ConstraintModel.getConstraintModel(editor.getModel());
      model.removeDragComponent();
    }
  }

  @Override
  public void commit(@AndroidCoordinate int x, @AndroidCoordinate int y, int modifiers) {
    if (myComponent != null) {
      myComponent = components.get(0);
      myComponent.x = x;
      myComponent.y = y;
      NlComponent root = myComponent.getRoot();
      root.ensureNamespace(SdkConstants.SHERPA_PREFIX, SdkConstants.AUTO_URI);

      ConstraintModel model = ConstraintModel.getConstraintModel(editor.getModel());
      if (model != null) {
        int ax = model.pxToDp(x - this.layout.x - this.layout.getPadding().left - myComponent.w / 2);
        int ay = model.pxToDp(y - this.layout.y - this.layout.getPadding().top - myComponent.h / 2);
        AttributesTransaction attributes = myComponent.startAttributeTransaction();
        ConstraintUtilities.setEditorPosition(null, attributes, ax, ay);
        attributes.commit();
      }
      if (myDragWidget != null) {
        model.commitDragComponent(myComponent);
      }
    }
  }

  @Override
  public void paint(@NotNull NlGraphics graphics) {
    // Do nothing for now
  }
}
