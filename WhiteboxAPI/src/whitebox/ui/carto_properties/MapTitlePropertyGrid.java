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

import whitebox.ui.StringProperty;
import whitebox.ui.NumericProperty;
import whitebox.ui.ColourProperty;
import whitebox.ui.FontProperty;
import whitebox.ui.BooleanProperty;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import javax.swing.*;
import whitebox.cartographic.MapTitle;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class MapTitlePropertyGrid extends JPanel implements PropertyChangeListener {
    
    private MapTitle mapTitle;
    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private StringProperty titleString;
    private ColourProperty outlineColourBox;
    private BooleanProperty titleVisible;
    private ColourProperty fontColourBox;
    private BooleanProperty outlineVisible;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty marginSize;
    private FontProperty fontProperty;
    private ResourceBundle bundle;
    
    public MapTitlePropertyGrid() {
        createUI();
    }
    
    public MapTitlePropertyGrid(MapTitle mapTitle, WhiteboxPluginHost host) {
        this.mapTitle = mapTitle;
        this.host = host;
        bundle = host.getGuiLabelsBundle();
        createUI();
    }

    public MapTitle getMapTitle() {
        return mapTitle;
    }

    public void setMapTitle(MapTitle mapTitle) {
        this.mapTitle = mapTitle;
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
    
    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }
    
    public final void createUI() {
        try {
            
            if (bundle == null) { return; }
            this.setBackground(Color.WHITE);
            
            Box mainBox = Box.createVerticalBox();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            int preferredWidth = 470;
            this.add(mainBox);
            
            Font labelFont = mapTitle.getLabelFont();
            
            titleString = new StringProperty(bundle.getString("LabelText"), 
                    mapTitle.getLabel());
            titleString.setLeftMargin(leftMargin);
            titleString.setRightMargin(rightMargin);
            titleString.setBackColour(Color.WHITE);
            titleString.setTextboxWidth(10);
            titleString.setPreferredWidth(preferredWidth);
            titleString.addPropertyChangeListener("value", this);
            titleString.revalidate();
            mainBox.add(titleString);
            
            titleVisible = new BooleanProperty(bundle.getString("IsElementVisible"), 
                    mapTitle.isVisible());
            titleVisible.setLeftMargin(leftMargin);
            titleVisible.setRightMargin(rightMargin);
            titleVisible.setBackColour(backColour);
            titleVisible.setPreferredWidth(preferredWidth);
            titleVisible.addPropertyChangeListener("value", this);
            titleVisible.revalidate();
            mainBox.add(titleVisible);

            fontProperty = new FontProperty(bundle.getString("Font"), labelFont);
            fontProperty.setLeftMargin(leftMargin);
            fontProperty.setRightMargin(rightMargin);
            fontProperty.setBackColour(Color.WHITE);
            fontProperty.setTextboxWidth(15);
            fontProperty.setPreferredWidth(preferredWidth);
            fontProperty.addPropertyChangeListener("value", this);
            fontProperty.revalidate();
            mainBox.add(fontProperty);
      
            fontColourBox = new ColourProperty(bundle.getString("FontColor"), 
                    mapTitle.getFontColour());
            fontColourBox.setLeftMargin(leftMargin);
            fontColourBox.setRightMargin(rightMargin);
            fontColourBox.setBackColour(backColour);
            fontColourBox.setPreferredWidth(preferredWidth);
            fontColourBox.revalidate();
            fontColourBox.addPropertyChangeListener("value", this);
            mainBox.add(fontColourBox);
            
            outlineVisible = new BooleanProperty(bundle.getString("IsOutlineVisible"), 
                    mapTitle.isOutlineVisible());
            outlineVisible.setLeftMargin(leftMargin);
            outlineVisible.setRightMargin(rightMargin);
            outlineVisible.setBackColour(Color.WHITE);
            outlineVisible.setPreferredWidth(preferredWidth);
            outlineVisible.revalidate();
            outlineVisible.addPropertyChangeListener("value", this);
            mainBox.add(outlineVisible);
            
            outlineColourBox = new ColourProperty(bundle.getString("OutlineColor"), 
                    mapTitle.getOutlineColour());
            outlineColourBox.setLeftMargin(leftMargin);
            outlineColourBox.setRightMargin(rightMargin);
            outlineColourBox.setBackColour(backColour);
            outlineColourBox.setPreferredWidth(preferredWidth);
            outlineColourBox.revalidate();
            outlineColourBox.addPropertyChangeListener("value", this);
            mainBox.add(outlineColourBox);
            
            backgroundVisible = new BooleanProperty(bundle.getString("IsBackgroundVisible"), 
                    mapTitle.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty(bundle.getString("BackgroundColor"), 
                    mapTitle.getBackColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty(bundle.getString("IsBorderVisible"), 
                    mapTitle.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            borderColour = new ColourProperty(bundle.getString("BorderColor"), 
                    mapTitle.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(backColour);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            marginSize = new NumericProperty(bundle.getString("MarginSize2"), 
                    String.valueOf(mapTitle.getMargin()));
            marginSize.setLeftMargin(leftMargin);
            marginSize.setRightMargin(rightMargin);
            marginSize.setBackColour(Color.WHITE);
            marginSize.setTextboxWidth(5);
            marginSize.setParseIntegersOnly(true);
            marginSize.addPropertyChangeListener("value", this);
            marginSize.setPreferredWidth(preferredWidth);
            marginSize.revalidate();
            mainBox.add(marginSize);
            
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
            mapTitle.setLabel((String) evt.getNewValue());
            didSomething = true;
        } else if (source == outlineColourBox) {
            mapTitle.setOutlineColour(outlineColourBox.getValue());
            didSomething = true;
        } else if (source == titleVisible) {
            mapTitle.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == fontColourBox) {
            mapTitle.setFontColour(fontColourBox.getValue());
            didSomething = true;
        } else if (source == outlineVisible) {
            mapTitle.setOutlineVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            mapTitle.setBackColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            mapTitle.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            mapTitle.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            mapTitle.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == marginSize) {
            mapTitle.setMargin(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == fontProperty) {
            mapTitle.setLabelFont((Font)evt.getNewValue());
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
