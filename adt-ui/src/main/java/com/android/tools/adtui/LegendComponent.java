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

package com.android.tools.adtui;

import com.android.tools.adtui.common.AdtUiUtils;
import com.android.tools.adtui.model.LegendRenderData;
import com.android.tools.adtui.model.ReportingSeries;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A label component that updates its value based on the reporting series passed to it.
 */
public class LegendComponent extends AnimatedComponent {

  public enum Orientation {
    HORIZONTAL,
    VERTICAL,
  }

  private static final int LINE_THICKNESS = 4;

  private static final int ICON_WIDTH = 16;

  private static final int ICON_PADDING = 2;

  private static final int LABEL_PADDING = 8;

  private int mFrequencyMillis;

  private List<JLabel> mLabelsToDraw;

  private long mLastUpdate;

  private List<LegendRenderData> mLegendRenderData;

  private Orientation mOrientation;

  /**
   * Legend component that renders a label, and icon for each series in a chart.
   *
   * @param orientation      Determines if we want the labels to be stacked horizontally or vertically
   * @param frequencyMillis  How frequently the labels get updated
   */
  public LegendComponent(Orientation orientation, int frequencyMillis) {
    mFrequencyMillis = frequencyMillis;
    mOrientation = orientation;
    mLastUpdate = 0;
  }

  /**
   * Clears existing LegendRenderData and adds new ones.
   */
  public void setLegendData(List<LegendRenderData> data) {
    mLegendRenderData = new ArrayList<>(data);
    mLabelsToDraw = new ArrayList<>(mLegendRenderData.size());
    for (LegendRenderData initialData : mLegendRenderData) {
      JBLabel label = new JBLabel(initialData.getLabel());
      label.setFont(AdtUiUtils.DEFAULT_FONT);
      mLabelsToDraw.add(label);
    }
  }

  @Override
  protected void updateData() {
    long now = System.currentTimeMillis();
    if (now - mLastUpdate > mFrequencyMillis) {
      mLastUpdate = now;
      for (int i = 0; i < mLegendRenderData.size(); ++i) {
        LegendRenderData data = mLegendRenderData.get(i);
        ReportingSeries series = data.getSeries();
        JLabel label = mLabelsToDraw.get(i);
        Dimension preferredSize = label.getPreferredSize();
        label.setBounds(0, 0, preferredSize.width, preferredSize.height);
        if (series != null) {
          ReportingSeries.ReportingData report = series.getLatestReportingData();
          if (report != null) {
            label.setText(String.format("%s: %s", series.getLabel(), report.formattedYData));
          }
        }
      }

      //As we adjust the size of the label we need to adjust the size of ourself
      //to tell our parent to give us enough room to draw.
      Dimension newSize = getLegendPreferredSize();
      if (newSize != getPreferredSize()) {
        setPreferredSize(newSize);
        revalidate();
      }
    }
  }

  @Override
  protected void draw(Graphics2D g2d) {
    for (int i = 0; i < mLegendRenderData.size(); ++i) {
      LegendRenderData data = mLegendRenderData.get(i);
      JLabel label = mLabelsToDraw.get(i);
      Dimension preferredSize = label.getPreferredSize();
      int xOffset = 0;
      //Draw the icon, and apply a translation offset for the label to be drawn.
      if (data.getIcon() == LegendRenderData.IconType.BOX) {
        g2d.setColor(data.getColor());
        g2d.fillRect(xOffset, 0, ICON_WIDTH, ICON_WIDTH);
        xOffset = ICON_WIDTH + ICON_PADDING;
      }
      else if (data.getIcon() == LegendRenderData.IconType.LINE) {
        g2d.setColor(data.getColor());
        Stroke defaultStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(LINE_THICKNESS));
        g2d.drawLine(xOffset, preferredSize.height / 2, xOffset + ICON_WIDTH, preferredSize.height / 2);
        g2d.setStroke(defaultStroke);
        xOffset = ICON_WIDTH + ICON_PADDING;
      }
      g2d.translate(xOffset, 0);
      label.paint(g2d);

      //Translate the draw position for the next set of labels.
      if (mOrientation == Orientation.HORIZONTAL) {
        g2d.translate(preferredSize.width + LABEL_PADDING, 0);
      }
      else if (mOrientation == Orientation.VERTICAL) {
        g2d.translate(-xOffset, 0);
        g2d.translate(0, preferredSize.height + LABEL_PADDING);
      }
    }
  }

  private Dimension getLegendPreferredSize() {
    int totalWidth = 0;
    int totalHeight = 0;
    int iconPaddedSize = ICON_WIDTH + ICON_PADDING + LABEL_PADDING;
    //Calculate total size of all icons + labels.
    for (JLabel label : mLabelsToDraw) {
      Dimension size = label.getPreferredSize();
      if (mOrientation == Orientation.HORIZONTAL) {
        totalWidth += iconPaddedSize + size.width;
        if (totalHeight < size.height) {
          totalHeight = size.height;
        }
      }
      else if (mOrientation == Orientation.VERTICAL) {
        totalHeight += iconPaddedSize;
        if (totalWidth < size.width + iconPaddedSize) {
          totalWidth = size.width + iconPaddedSize;
        }
      }
    }
    return new Dimension(totalWidth, totalHeight);
  }
}