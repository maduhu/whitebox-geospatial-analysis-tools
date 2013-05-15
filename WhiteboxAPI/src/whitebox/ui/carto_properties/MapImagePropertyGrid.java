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
import whitebox.cartographic.MapImage;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class MapImagePropertyGrid extends JPanel implements PropertyChangeListener {
    
    private MapImage mapImage;
    private int rightMargin = 20;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private BooleanProperty elementVisible;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty lineWidth;
    private NumericProperty upperLeftX;
    private NumericProperty upperLeftY;
    private NumericProperty width;
    private NumericProperty height;
    private BooleanProperty aspectRatio;
    
    public MapImagePropertyGrid() {
        createUI();
    }
    
    public MapImagePropertyGrid(MapImage mapImage, WhiteboxPluginHost host) {
        this.mapImage = mapImage;
        this.host = host;
        createUI();
    }

    public MapImage getMapImage() {
        return mapImage;
    }

    public void setMapImage(MapImage mapImage) {
        this.mapImage = mapImage;
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
            //JScrollPane scroll = new JScrollPane(mainBox);
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            int preferredWidth = 470;
            this.add(mainBox);
//            this.setPreferredSize(new Dimension(preferredWidth, 500));
            
            
            elementVisible = new BooleanProperty("Is the image visible?", 
                    mapImage.isVisible());
            elementVisible.setLeftMargin(leftMargin);
            elementVisible.setRightMargin(rightMargin);
            elementVisible.setBackColour(backColour);
            elementVisible.setPreferredWidth(preferredWidth);
            elementVisible.addPropertyChangeListener("value", this);
            elementVisible.revalidate();
            mainBox.add(elementVisible);

            borderVisible = new BooleanProperty("Is the border visible?", 
                    mapImage.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            lineWidth = new NumericProperty("Line width", 
                    String.valueOf(mapImage.getLineWidth()));
            lineWidth.setLeftMargin(leftMargin);
            lineWidth.setRightMargin(rightMargin);
            lineWidth.setBackColour(backColour);
            lineWidth.setTextboxWidth(10);
            lineWidth.setParseIntegersOnly(false);
            lineWidth.addPropertyChangeListener("value", this);
            lineWidth.setPreferredWidth(preferredWidth);
            lineWidth.revalidate();
            mainBox.add(lineWidth);
            
            borderColour = new ColourProperty("Border colour", 
                    mapImage.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(Color.WHITE);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            upperLeftX = new NumericProperty("Upper-left x", 
                    String.valueOf(mapImage.getUpperLeftX()));
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
                    String.valueOf(mapImage.getUpperLeftY()));
            upperLeftY.setLeftMargin(leftMargin);
            upperLeftY.setRightMargin(rightMargin);
            upperLeftY.setBackColour(Color.WHITE);
            upperLeftY.setTextboxWidth(10);
            upperLeftY.setParseIntegersOnly(true);
            upperLeftY.addPropertyChangeListener("value", this);
            upperLeftY.setPreferredWidth(preferredWidth);
            upperLeftY.revalidate();
            mainBox.add(upperLeftY);
            
            width = new NumericProperty("Width", 
                    String.valueOf(mapImage.getWidth()));
            width.setLeftMargin(leftMargin);
            width.setRightMargin(rightMargin);
            width.setBackColour(backColour);
            width.setTextboxWidth(10);
            width.setParseIntegersOnly(true);
            width.addPropertyChangeListener("value", this);
            width.setPreferredWidth(preferredWidth);
            width.revalidate();
            mainBox.add(width);
            
            height = new NumericProperty("Height", 
                    String.valueOf(mapImage.getHeight()));
            height.setLeftMargin(leftMargin);
            height.setRightMargin(rightMargin);
            height.setBackColour(Color.WHITE);
            height.setTextboxWidth(10);
            height.setParseIntegersOnly(true);
            height.addPropertyChangeListener("value", this);
            height.setPreferredWidth(preferredWidth);
            height.revalidate();
            mainBox.add(height);
            
            aspectRatio = new BooleanProperty("Maintain the aspect ratio?", 
                    mapImage.isMaintainAspectRatio());
            aspectRatio.setLeftMargin(leftMargin);
            aspectRatio.setRightMargin(rightMargin);
            aspectRatio.setBackColour(backColour);
            aspectRatio.setPreferredWidth(preferredWidth);
            aspectRatio.addPropertyChangeListener("value", this);
            aspectRatio.revalidate();
            mainBox.add(aspectRatio);
            
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
        if (source == elementVisible) {
            mapImage.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            mapImage.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            mapImage.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == width) {
            mapImage.setWidth(Integer.parseInt((String)evt.getNewValue()));
            didSomething = true;
        } else if (source == height) {
            mapImage.setHeight(Integer.parseInt((String)evt.getNewValue()));
            didSomething = true;
        } else if (source == upperLeftX) {
            mapImage.setUpperLeftX(Integer.parseInt((String)evt.getNewValue()));
            didSomething = true;
        } else if (source == upperLeftY) {
            mapImage.setUpperLeftY(Integer.parseInt((String)evt.getNewValue()));
            didSomething = true;
        } else if (source == lineWidth) {
            mapImage.setLineWidth(Float.parseFloat((String)evt.getNewValue()));
            didSomething = true;
        } else if (source == aspectRatio) {
            mapImage.setMaintainAspectRatio((Boolean)evt.getNewValue());
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
