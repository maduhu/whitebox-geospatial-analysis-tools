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
package whitebox.cartographic.properties;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import whitebox.cartographic.MapTextArea;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class MapTextAreaPropertyGrid extends JPanel implements PropertyChangeListener {
    
    private MapTextArea mapTextArea;
    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private StringProperty titleString;
    private BooleanProperty titleVisible;
    private ColourProperty fontColourBox;
    private BooleanProperty fontBold;
    private BooleanProperty fontItalics;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty marginSize;
    private NumericProperty fontSizeBox;
    private NumericProperty interlineSpacing;
    
    public MapTextAreaPropertyGrid() {
        createUI();
    }
    
    public MapTextAreaPropertyGrid(MapTextArea mapTextArea, WhiteboxPluginHost host) {
        this.mapTextArea = mapTextArea;
        this.host = host;
        createUI();
    }

    public MapTextArea getMapTextArea() {
        return mapTextArea;
    }

    public void setMapTitle(MapTextArea mapTextArea) {
        this.mapTextArea = mapTextArea;
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
            
            Font labelFont = mapTextArea.getLabelFont();
            
            titleString = new StringProperty("Label text", 
                    mapTextArea.getLabel());
            titleString.setLeftMargin(leftMargin);
            titleString.setRightMargin(rightMargin);
            titleString.setBackColour(Color.WHITE);
            titleString.setTextboxWidth(10);
            titleString.setPreferredWidth(preferredWidth);
            titleString.addPropertyChangeListener("value", this);
            titleString.revalidate();
            mainBox.add(titleString);
            
            titleVisible = new BooleanProperty("Is the title visible?", 
                    mapTextArea.isVisible());
            titleVisible.setLeftMargin(leftMargin);
            titleVisible.setRightMargin(rightMargin);
            titleVisible.setBackColour(backColour);
            titleVisible.setPreferredWidth(preferredWidth);
            titleVisible.addPropertyChangeListener("value", this);
            titleVisible.revalidate();
            mainBox.add(titleVisible);

            fontSizeBox = new NumericProperty("Font size", 
                    String.valueOf(labelFont.getSize()));
            fontSizeBox.setLeftMargin(leftMargin);
            fontSizeBox.setRightMargin(rightMargin);
            fontSizeBox.setBackColour(Color.WHITE);
            fontSizeBox.setTextboxWidth(3);
            fontSizeBox.setPreferredWidth(preferredWidth);
            fontSizeBox.setParseIntegersOnly(true);
            fontSizeBox.setMinValue(1);
            fontSizeBox.setMaxValue(200);
            fontSizeBox.addPropertyChangeListener("value", this);
            fontSizeBox.revalidate();
            mainBox.add(fontSizeBox);
            
            int fontBoldInt = labelFont.getStyle() & Font.BOLD;
            fontBold = new BooleanProperty("Use bold font?", 
                    (fontBoldInt > 0));
            fontBold.setLeftMargin(leftMargin);
            fontBold.setRightMargin(rightMargin);
            fontBold.setBackColour(backColour);
            fontBold.setPreferredWidth(preferredWidth);
            fontBold.addPropertyChangeListener("value", this);
            fontBold.revalidate();
            mainBox.add(fontBold);
            
            int fontItalicInt = labelFont.getStyle() & Font.ITALIC;
            fontItalics = new BooleanProperty("Use italicized font?", 
                    (fontItalicInt > 0));
            fontItalics.setLeftMargin(leftMargin);
            fontItalics.setRightMargin(rightMargin);
            fontItalics.setBackColour(Color.WHITE);
            fontItalics.setPreferredWidth(preferredWidth);
            fontItalics.addPropertyChangeListener("value", this);
            fontItalics.revalidate();
            mainBox.add(fontItalics);
      
            fontColourBox = new ColourProperty("Font colour", 
                    mapTextArea.getFontColour());
            fontColourBox.setLeftMargin(leftMargin);
            fontColourBox.setRightMargin(rightMargin);
            fontColourBox.setBackColour(backColour);
            fontColourBox.setPreferredWidth(preferredWidth);
            fontColourBox.revalidate();
            fontColourBox.addPropertyChangeListener("value", this);
            mainBox.add(fontColourBox);
            
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    mapTextArea.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    mapTextArea.getBackColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    mapTextArea.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            borderColour = new ColourProperty("Border colour", 
                    mapTextArea.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(backColour);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            marginSize = new NumericProperty("Margin size (points)", 
                    String.valueOf(mapTextArea.getMargin()));
            marginSize.setLeftMargin(leftMargin);
            marginSize.setRightMargin(rightMargin);
            marginSize.setBackColour(Color.WHITE);
            marginSize.setTextboxWidth(5);
            marginSize.setParseIntegersOnly(true);
            marginSize.addPropertyChangeListener("value", this);
            marginSize.setPreferredWidth(preferredWidth);
            marginSize.revalidate();
            mainBox.add(marginSize);
            
            
            interlineSpacing = new NumericProperty("Interline spacing:", 
                    String.valueOf(mapTextArea.getInterlineSpace()));
            interlineSpacing.setLeftMargin(leftMargin);
            interlineSpacing.setRightMargin(rightMargin);
            interlineSpacing.setBackColour(backColour);
            interlineSpacing.setTextboxWidth(3);
            interlineSpacing.setPreferredWidth(preferredWidth);
            interlineSpacing.setParseIntegersOnly(false);
            interlineSpacing.setMinValue(0);
            interlineSpacing.setMaxValue(10);
            interlineSpacing.addPropertyChangeListener("value", this);
            interlineSpacing.revalidate();
            mainBox.add(interlineSpacing);
            
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
            mapTextArea.setLabel((String) evt.getNewValue());
            didSomething = true;
        } else if (source == titleVisible) {
            mapTextArea.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == fontColourBox) {
            mapTextArea.setFontColour(fontColourBox.getValue());
            didSomething = true;
        } else if (source == fontBold) {
            Boolean fontBolded = (Boolean) evt.getNewValue();

            Font labelFont = mapTextArea.getLabelFont();
            int fontSize = (Integer) (labelFont.getSize());
            int style = 0;
            if (fontBolded) {
                style += Font.BOLD;
            }
            if ((labelFont.getStyle() & Font.ITALIC) > 0) {
                style += Font.ITALIC;
            }
            Font newFont = new Font(labelFont.getName(), style, fontSize);
            if (!labelFont.equals(newFont)) {
                mapTextArea.setLabelFont(newFont);
            }

            didSomething = true;
        } else if (source == fontItalics) {
            Boolean fontItalicized = (Boolean) evt.getNewValue();

            Font labelFont = mapTextArea.getLabelFont();
            int fontSize = (Integer) (labelFont.getSize());
            int style = 0;
            if ((labelFont.getStyle() & Font.BOLD) > 0) {
                style += Font.BOLD;
            }
            if (fontItalicized) {
                style += Font.ITALIC;
            }
            Font newFont = new Font(labelFont.getName(), style, fontSize);
            if (!labelFont.equals(newFont)) {
                mapTextArea.setLabelFont(newFont);
            }

            didSomething = true;
        } else if (source == fontSizeBox) {
            int fontSize = Integer.parseInt(String.valueOf(evt.getNewValue()));
            Font labelFont = mapTextArea.getLabelFont();
            int style = labelFont.getStyle();
            Font newFont = new Font(labelFont.getName(), style, fontSize);
            if (!labelFont.equals(newFont)) {
                mapTextArea.setLabelFont(newFont);
            }

            didSomething = true;
        } else if (source == backgroundColourBox) {
            mapTextArea.setBackColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            mapTextArea.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            mapTextArea.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            mapTextArea.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == marginSize) {
            mapTextArea.setMargin(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == interlineSpacing) {
            mapTextArea.setInterlineSpace(Float.parseFloat((String) evt.getNewValue()));
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
