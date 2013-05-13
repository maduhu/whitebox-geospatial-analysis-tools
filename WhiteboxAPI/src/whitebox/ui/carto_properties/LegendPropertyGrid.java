/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import whitebox.cartographic.Legend;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class LegendPropertyGrid extends JPanel implements PropertyChangeListener {
    
    private Legend legend;
    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private StringProperty titleString;
    private BooleanProperty legendVisible;
    private ColourProperty fontColourBox;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty marginSize;
    private FontProperty fontProperty;
    private NumericProperty borderWidth;
    private NumericProperty height;
    private NumericProperty width;
    private NumericProperty lineWidth;
    private NumericProperty upperLeftX;
    private NumericProperty upperLeftY;
    
    public LegendPropertyGrid() {
        createUI();
    }
    
    public LegendPropertyGrid(Legend legend, WhiteboxPluginHost host) {
        this.legend = legend;
        this.host = host;
        createUI();
    }

    public Legend getLegend() {
        return legend;
    }

    public void setMapTitle(Legend legend) {
        this.legend = legend;
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
            
            Font labelFont = legend.getLabelFont();
            
            titleString = new StringProperty("Label text", 
                    legend.getLabel());
            titleString.setLeftMargin(leftMargin);
            titleString.setRightMargin(rightMargin);
            titleString.setBackColour(Color.WHITE);
            titleString.setTextboxWidth(10);
            titleString.setPreferredWidth(preferredWidth);
            titleString.addPropertyChangeListener("value", this);
            titleString.revalidate();
            mainBox.add(titleString);
            
            height = new NumericProperty("Height", 
                    String.valueOf(legend.getHeight()));
            height.setLeftMargin(leftMargin);
            height.setRightMargin(rightMargin);
            height.setBackColour(backColour);
            height.setTextboxWidth(10);
            height.setParseIntegersOnly(true);
            height.addPropertyChangeListener("value", this);
            height.setPreferredWidth(preferredWidth);
            height.revalidate();
            mainBox.add(height);
            
            width = new NumericProperty("Width", 
                    String.valueOf(legend.getWidth()));
            width.setLeftMargin(leftMargin);
            width.setRightMargin(rightMargin);
            width.setBackColour(Color.WHITE);
            width.setTextboxWidth(10);
            width.setParseIntegersOnly(true);
            width.addPropertyChangeListener("value", this);
            width.setPreferredWidth(preferredWidth);
            width.revalidate();
            mainBox.add(width);
            
            upperLeftX = new NumericProperty("Upper-left x", 
                    String.valueOf(legend.getUpperLeftX()));
            upperLeftX.setLeftMargin(leftMargin);
            upperLeftX.setRightMargin(rightMargin);
            upperLeftX.setBackColour(backColour);
            upperLeftX.setTextboxWidth(10);
            upperLeftX.setParseIntegersOnly(true);
            upperLeftX.addPropertyChangeListener("value", this);
            upperLeftX.setPreferredWidth(preferredWidth);
            upperLeftX.revalidate();
            mainBox.add(upperLeftX);
            
            upperLeftY = new NumericProperty("Upper-left y", 
                    String.valueOf(legend.getUpperLeftY()));
            upperLeftY.setLeftMargin(leftMargin);
            upperLeftY.setRightMargin(rightMargin);
            upperLeftY.setBackColour(Color.WHITE);
            upperLeftY.setTextboxWidth(10);
            upperLeftY.setParseIntegersOnly(true);
            upperLeftY.addPropertyChangeListener("value", this);
            upperLeftY.setPreferredWidth(preferredWidth);
            upperLeftY.revalidate();
            mainBox.add(upperLeftY);
            
            legendVisible = new BooleanProperty("Is the title visible?", 
                    legend.isVisible());
            legendVisible.setLeftMargin(leftMargin);
            legendVisible.setRightMargin(rightMargin);
            legendVisible.setBackColour(backColour);
            legendVisible.setPreferredWidth(preferredWidth);
            legendVisible.addPropertyChangeListener("value", this);
            legendVisible.revalidate();
            mainBox.add(legendVisible);

            fontProperty = new FontProperty("Font:", labelFont);
            fontProperty.setLeftMargin(leftMargin);
            fontProperty.setRightMargin(rightMargin);
            fontProperty.setBackColour(Color.WHITE);
            fontProperty.setTextboxWidth(15);
            fontProperty.setPreferredWidth(preferredWidth);
            fontProperty.addPropertyChangeListener("value", this);
            fontProperty.revalidate();
            mainBox.add(fontProperty);
      
            fontColourBox = new ColourProperty("Font colour", 
                    legend.getFontColour());
            fontColourBox.setLeftMargin(leftMargin);
            fontColourBox.setRightMargin(rightMargin);
            fontColourBox.setBackColour(backColour);
            fontColourBox.setPreferredWidth(preferredWidth);
            fontColourBox.revalidate();
            fontColourBox.addPropertyChangeListener("value", this);
            mainBox.add(fontColourBox);
                        
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    legend.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    legend.getBackgroundColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    legend.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            borderColour = new ColourProperty("Border colour", 
                    legend.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(backColour);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            borderWidth = new NumericProperty("Border width", 
                    String.valueOf(legend.getBorderWidth()));
            borderWidth.setLeftMargin(leftMargin);
            borderWidth.setRightMargin(rightMargin);
            borderWidth.setBackColour(Color.WHITE);
            borderWidth.setTextboxWidth(10);
            borderWidth.setParseIntegersOnly(false);
            borderWidth.setMinValue(0);
            borderWidth.setMaxValue(250);
            borderWidth.addPropertyChangeListener("value", this);
            borderWidth.setPreferredWidth(preferredWidth);
            borderWidth.revalidate();
            mainBox.add(borderWidth);
            
            marginSize = new NumericProperty("Margin size (points)", 
                    String.valueOf(legend.getMargin()));
            marginSize.setLeftMargin(leftMargin);
            marginSize.setRightMargin(rightMargin);
            marginSize.setBackColour(backColour);
            marginSize.setTextboxWidth(10);
            marginSize.setParseIntegersOnly(true);
            marginSize.addPropertyChangeListener("value", this);
            marginSize.setPreferredWidth(preferredWidth);
            marginSize.revalidate();
            mainBox.add(marginSize);
            
            lineWidth = new NumericProperty("Line width", 
                    String.valueOf(legend.getLineWidth()));
            lineWidth.setLeftMargin(leftMargin);
            lineWidth.setRightMargin(rightMargin);
            lineWidth.setBackColour(Color.WHITE);
            lineWidth.setTextboxWidth(10);
            lineWidth.setParseIntegersOnly(false);
            lineWidth.setMinValue(0);
            lineWidth.setMaxValue(250);
            lineWidth.addPropertyChangeListener("value", this);
            lineWidth.setPreferredWidth(preferredWidth);
            lineWidth.revalidate();
            mainBox.add(lineWidth);
            
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
        if (source == titleString) {
            legend.setLabel((String) evt.getNewValue());
            didSomething = true;
        } else if (source == legendVisible) {
            legend.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == fontColourBox) {
            legend.setFontColour(fontColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            legend.setBackgroundColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            legend.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            legend.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            legend.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == marginSize) {
            legend.setMargin(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == fontProperty) {
            legend.setLabelFont((Font)evt.getNewValue());
            didSomething = true;
        } else if (source == height) {
            legend.setHeight(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == width) {
            legend.setWidth(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == upperLeftX) {
            legend.setUpperLeftX(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == upperLeftY) {
            legend.setUpperLeftY(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == lineWidth) {
            legend.setLineWidth(Float.parseFloat((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == borderWidth) {
            legend.setBorderWidth(Float.parseFloat((String) evt.getNewValue()));
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
