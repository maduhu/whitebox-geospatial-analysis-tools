/*
 * Copyright (C) 2013 johnlindsay
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitebox.ui.carto_properties;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import whitebox.cartographic.Neatline;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class NeatlinePropertyGrid extends JPanel implements PropertyChangeListener {
    
    private Neatline neatline;
    private int rightMargin = 20;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private BooleanProperty neatlineVisible;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty width;
    private NumericProperty height;
    private BooleanProperty doubleLine;
    private NumericProperty doubleLineGap;
    private NumericProperty innerLineWidth;
    private NumericProperty outerLineThickness;
    
    public NeatlinePropertyGrid() {
        createUI();
    }
    
    public NeatlinePropertyGrid(Neatline neatline, WhiteboxPluginHost host) {
        this.neatline = neatline;
        this.host = host;
        createUI();
    }

    public Neatline getNeatline() {
        return neatline;
    }

    public void setNeatline(Neatline neatline) {
        this.neatline = neatline;
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public WhiteboxPluginHost getHost() {
        return host;
    }

    public void setHost(WhiteboxPluginHost host) {
        this.host = host;
    }
    
    public final void createUI() {
        try {
            
            this.setBackground(Color.WHITE);
            
            Box mainBox = Box.createVerticalBox();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            int preferredWidth = 470;
            this.add(mainBox);
            //this.setPreferredSize(new Dimension(preferredWidth, 500));
            
            
            neatlineVisible = new BooleanProperty("Is the title visible?", 
                    neatline.isVisible());
            neatlineVisible.setLeftMargin(leftMargin);
            neatlineVisible.setRightMargin(rightMargin);
            neatlineVisible.setBackColour(backColour);
            neatlineVisible.setPreferredWidth(preferredWidth);
            neatlineVisible.addPropertyChangeListener("value", this);
            neatlineVisible.revalidate();
            mainBox.add(neatlineVisible);

            height = new NumericProperty("Height", 
                    String.valueOf(neatline.getHeight()));
            height.setLeftMargin(leftMargin);
            height.setRightMargin(rightMargin);
            height.setBackColour(Color.WHITE);
            height.setTextboxWidth(5);
            height.setParseIntegersOnly(true);
            height.setMinValue(1);
            height.setMaxValue(250);
            height.addPropertyChangeListener("value", this);
            height.setPreferredWidth(preferredWidth);
            height.revalidate();
            mainBox.add(height);
            
            width = new NumericProperty("Width", 
                    String.valueOf(neatline.getWidth()));
            width.setLeftMargin(leftMargin);
            width.setRightMargin(rightMargin);
            width.setBackColour(backColour);
            width.setTextboxWidth(5);
            width.setParseIntegersOnly(true);
            width.setMinValue(1);
            width.setMaxValue(250);
            width.addPropertyChangeListener("value", this);
            width.setPreferredWidth(preferredWidth);
            width.revalidate();
            mainBox.add(width);
            
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    neatline.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    neatline.getBackgroundColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    neatline.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            borderColour = new ColourProperty("Border colour", 
                    neatline.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(backColour);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            doubleLine = new BooleanProperty("Use a double-line?", 
                    neatline.isDoubleLine());
            doubleLine.setLeftMargin(leftMargin);
            doubleLine.setRightMargin(rightMargin);
            doubleLine.setBackColour(Color.WHITE);
            doubleLine.setPreferredWidth(preferredWidth);
            doubleLine.addPropertyChangeListener("value", this);
            doubleLine.revalidate();
            mainBox.add(doubleLine);
            
            doubleLineGap = new NumericProperty("Double-line gap", 
                    String.valueOf(neatline.getDoubleLineGap()));
            doubleLineGap.setLeftMargin(leftMargin);
            doubleLineGap.setRightMargin(rightMargin);
            doubleLineGap.setBackColour(backColour);
            doubleLineGap.setTextboxWidth(5);
            doubleLineGap.setParseIntegersOnly(true);
            doubleLineGap.setMinValue(1);
            doubleLineGap.setMaxValue(250);
            doubleLineGap.addPropertyChangeListener("value", this);
            doubleLineGap.setPreferredWidth(preferredWidth);
            doubleLineGap.revalidate();
            mainBox.add(doubleLineGap);
            
            innerLineWidth = new NumericProperty("Inner-line width", 
                    String.valueOf(neatline.getInnerLineWidth()));
            innerLineWidth.setLeftMargin(leftMargin);
            innerLineWidth.setRightMargin(rightMargin);
            innerLineWidth.setBackColour(Color.WHITE);
            innerLineWidth.setTextboxWidth(5);
            innerLineWidth.setParseIntegersOnly(false);
            innerLineWidth.setMinValue(1);
            innerLineWidth.setMaxValue(250);
            innerLineWidth.addPropertyChangeListener("value", this);
            innerLineWidth.setPreferredWidth(preferredWidth);
            innerLineWidth.revalidate();
            mainBox.add(innerLineWidth);
            
            outerLineThickness = new NumericProperty("Outer-line width", 
                    String.valueOf(neatline.getOuterLineWidth()));
            outerLineThickness.setLeftMargin(leftMargin);
            outerLineThickness.setRightMargin(rightMargin);
            outerLineThickness.setBackColour(backColour);
            outerLineThickness.setTextboxWidth(5);
            outerLineThickness.setParseIntegersOnly(false);
            outerLineThickness.setMinValue(1);
            outerLineThickness.setMaxValue(250);
            outerLineThickness.addPropertyChangeListener("value", this);
            outerLineThickness.setPreferredWidth(preferredWidth);
            outerLineThickness.revalidate();
            mainBox.add(outerLineThickness);
            
            super.revalidate();
        } catch (Exception e) {
            //host.showFeedback(e.getMessage());
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        Boolean didSomething = false;
        if (!evt.getPropertyName().equals("value")) {
            return;
        }
        if (source == neatlineVisible) {
            neatline.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            neatline.setBackgroundColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            neatline.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            neatline.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            neatline.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == doubleLine) {
            neatline.setDoubleLine((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == doubleLineGap) {
            neatline.setDoubleLineGap(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == height) {
            neatline.setHeight(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == width) {
            neatline.setWidth(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == innerLineWidth) {
            neatline.setInnerLineWidth(Float.parseFloat((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == outerLineThickness) {
            neatline.setOuterLineThickness(Float.parseFloat((String) evt.getNewValue()));
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
