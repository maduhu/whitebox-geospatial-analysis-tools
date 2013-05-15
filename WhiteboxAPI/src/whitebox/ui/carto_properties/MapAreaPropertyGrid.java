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
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import whitebox.cartographic.MapArea;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class MapAreaPropertyGrid extends JPanel implements PropertyChangeListener  {
    
    private MapArea mapArea;
    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private BooleanProperty neatlineVisible;
    private BooleanProperty mapAreaVisible;
    private BooleanProperty referenceMarksVisible;
    private NumericProperty referenceMarkSize;
    private ColourProperty borderColourBox;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private NumericProperty mapAreaWidth;
    private NumericProperty mapAreaHeight;
    private StringProperty xyUnits;
    private FontProperty fontProperty;
    private BooleanProperty fitToData;
    
    public MapAreaPropertyGrid() {
        createUI();
    }
    
    public MapAreaPropertyGrid(MapArea mapArea, WhiteboxPluginHost host) {
        this.mapArea = mapArea;
        this.host = host;
        createUI();
    }

    public MapArea getMapArea(){
        return mapArea;
    }

    public void setMapArea(MapArea mapArea) {
        this.mapArea = mapArea;
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
            
            //JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            int preferredWidth = 470;
            this.add(mainBox);
            
            mapAreaVisible = new BooleanProperty("Is the map area visible?", 
                    mapArea.isVisible());
            mapAreaVisible.setLeftMargin(leftMargin);
            mapAreaVisible.setRightMargin(rightMargin);
            mapAreaVisible.setBackColour(backColour);
            mapAreaVisible.setPreferredWidth(preferredWidth);
            mapAreaVisible.addPropertyChangeListener("value", this);
            mapAreaVisible.revalidate();
            mainBox.add(mapAreaVisible);
            
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    mapArea.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    mapArea.getBackgroundColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    mapArea.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);

            borderColourBox = new ColourProperty("Border colour", 
                    mapArea.getBorderColour());
            borderColourBox.setLeftMargin(leftMargin);
            borderColourBox.setRightMargin(rightMargin);
            borderColourBox.setBackColour(backColour);
            borderColourBox.setPreferredWidth(preferredWidth);
            borderColourBox.revalidate();
            borderColourBox.addPropertyChangeListener("value", this);
            mainBox.add(borderColourBox);
            
            referenceMarksVisible = new BooleanProperty("Are reference marks visible?", mapArea.isReferenceMarksVisible());
            referenceMarksVisible.setLeftMargin(leftMargin);
            referenceMarksVisible.setRightMargin(rightMargin);
            referenceMarksVisible.setBackColour(Color.WHITE);
            referenceMarksVisible.setPreferredWidth(preferredWidth);
            referenceMarksVisible.revalidate();
            referenceMarksVisible.addPropertyChangeListener("value", this);
            mainBox.add(referenceMarksVisible);
            
            referenceMarkSize = new NumericProperty("Reference mark size", String.valueOf(mapArea.getReferenceMarksSize()));
            referenceMarkSize.setLeftMargin(leftMargin);
            referenceMarkSize.setRightMargin(rightMargin);
            referenceMarkSize.setBackColour(backColour);
            referenceMarkSize.setPreferredWidth(preferredWidth);
            referenceMarkSize.setParseIntegersOnly(true);
            referenceMarkSize.setTextboxWidth(10);
            referenceMarkSize.revalidate();
            referenceMarkSize.addPropertyChangeListener("value", this);
            mainBox.add(referenceMarkSize);
            
            neatlineVisible = new BooleanProperty("Is the neatline visible?", mapArea.isNeatlineVisible());
            neatlineVisible.setLeftMargin(leftMargin);
            neatlineVisible.setRightMargin(rightMargin);
            neatlineVisible.setBackColour(Color.WHITE);
            neatlineVisible.setPreferredWidth(preferredWidth);
            neatlineVisible.revalidate();
            neatlineVisible.addPropertyChangeListener("value", this);
            mainBox.add(neatlineVisible);
            
            mapAreaWidth = new NumericProperty("Map area width", String.valueOf(mapArea.getWidth()));
            mapAreaWidth.setLeftMargin(leftMargin);
            mapAreaWidth.setRightMargin(rightMargin);
            mapAreaWidth.setBackColour(backColour);
            mapAreaWidth.setPreferredWidth(preferredWidth);
            mapAreaWidth.setParseIntegersOnly(true);
            mapAreaWidth.setTextboxWidth(10);
            mapAreaWidth.revalidate();
            mapAreaWidth.addPropertyChangeListener("value", this);
            mainBox.add(mapAreaWidth);
            
            // scale height
            mapAreaHeight = new NumericProperty("Map area height", String.valueOf(mapArea.getHeight()));
            mapAreaHeight.setLeftMargin(leftMargin);
            mapAreaHeight.setRightMargin(rightMargin);
            mapAreaHeight.setBackColour(Color.WHITE);
            mapAreaHeight.setPreferredWidth(preferredWidth);
            mapAreaHeight.setParseIntegersOnly(true);
            mapAreaHeight.setTextboxWidth(10);
            mapAreaHeight.revalidate();
            mapAreaHeight.addPropertyChangeListener("value", this);
            mainBox.add(mapAreaHeight);
            
            // xy units
            xyUnits = new StringProperty("X-Y units", mapArea.getXYUnits());
            xyUnits.setLeftMargin(leftMargin);
            xyUnits.setRightMargin(rightMargin);
            xyUnits.setBackColour(backColour);
            xyUnits.setPreferredWidth(preferredWidth);
            xyUnits.setTextboxWidth(10);
            xyUnits.revalidate();
            xyUnits.addPropertyChangeListener("value", this);
            mainBox.add(xyUnits);
            
            fontProperty = new FontProperty("Font", mapArea.getLabelFont());
            fontProperty.setLeftMargin(leftMargin);
            fontProperty.setRightMargin(rightMargin);
            fontProperty.setBackColour(Color.WHITE);
            fontProperty.setTextboxWidth(15);
            fontProperty.setPreferredWidth(preferredWidth);
            fontProperty.addPropertyChangeListener("value", this);
            fontProperty.revalidate();
            mainBox.add(fontProperty);
            
            fitToData = new BooleanProperty("Fit to data?", 
                    mapArea.isFitToData());
            fitToData.setLeftMargin(leftMargin);
            fitToData.setRightMargin(rightMargin);
            fitToData.setBackColour(backColour);
            fitToData.setPreferredWidth(preferredWidth);
            fitToData.revalidate();
            fitToData.addPropertyChangeListener("value", this);
            mainBox.add(fitToData);
            
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
        if (source == mapAreaWidth) {
            mapArea.setWidth(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == mapAreaHeight) {
            mapArea.setHeight(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == mapAreaVisible) {
            mapArea.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColourBox) {
            mapArea.setBorderColour(borderColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            mapArea.setBackgroundColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            mapArea.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            mapArea.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == neatlineVisible) {
            mapArea.setNeatlineVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == referenceMarksVisible) {
            mapArea.setReferenceMarksVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == xyUnits) {
            mapArea.setXYUnits(evt.getNewValue().toString());
            didSomething = true;
        } else if (source == fontProperty) {
            mapArea.setLabelFont((Font)evt.getNewValue());
            didSomething = true;
        } else if (source == fitToData) {
            if ((Boolean)evt.getNewValue()) {
                mapArea.setFitToData();
                didSomething = true;
            }
        } else if (source == referenceMarkSize) {
            mapArea.setReferenceMarksSize(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
