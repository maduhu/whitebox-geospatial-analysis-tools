/*
 * Copyright (C) 2012 johnlindsay
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import whitebox.cartographic.MapScale;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class ScalePropertyGrid extends JPanel implements PropertyChangeListener  {
    
    private MapScale mapScale;
    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private StringProperty scaleUnits;
    private ColourProperty outlineColourBox;
    private BooleanProperty scaleVisible;
    private BooleanProperty scaleRepFracVisible;
    private ColourProperty borderColourBox;
//    private BooleanProperty outlineVisible;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
//    private ColourProperty borderColour;
    private NumericProperty marginSize;
    private NumericProperty scaleWidth;
    private NumericProperty scaleHeight;
//    private NumericProperty barLength;
    
    public ScalePropertyGrid() {
        createUI();
    }
    
    public ScalePropertyGrid(MapScale mapScale, WhiteboxPluginHost host) {
        this.mapScale = mapScale;
        this.host = host;
        createUI();
    }

    public MapScale getMapScale() {
        return mapScale;
    }

    public void setMapScale(MapScale mapScale) {
        this.mapScale = mapScale;
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
            //scroll.setMaximumSize(new Dimension(1000, preferredWidth));
            //this.add(scroll);
            this.add(mainBox);
            
            //this.setPreferredSize(new Dimension(this.getParent().getPreferredSize().width, 500));
            
            scaleVisible = new BooleanProperty("Is the scale visible?", 
                    mapScale.isVisible());
            scaleVisible.setLeftMargin(leftMargin);
            scaleVisible.setRightMargin(rightMargin);
            scaleVisible.setBackColour(backColour);
            scaleVisible.setPreferredWidth(preferredWidth);
            scaleVisible.addPropertyChangeListener("value", this);
            scaleVisible.revalidate();
            mainBox.add(scaleVisible);
            
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    mapScale.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    mapScale.getBackColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    mapScale.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);

            borderColourBox = new ColourProperty("Border colour", 
                    mapScale.getFontColour());
            borderColourBox.setLeftMargin(leftMargin);
            borderColourBox.setRightMargin(rightMargin);
            borderColourBox.setBackColour(backColour);
            borderColourBox.setPreferredWidth(preferredWidth);
            borderColourBox.revalidate();
            borderColourBox.addPropertyChangeListener("value", this);
            mainBox.add(borderColourBox);
            
            
            // scale units
            scaleUnits = new StringProperty("Scale Units:", mapScale.getUnits());
            scaleUnits.setLeftMargin(leftMargin);
            scaleUnits.setRightMargin(rightMargin);
            scaleUnits.setBackColour(Color.WHITE);
            scaleUnits.setPreferredWidth(preferredWidth);
            scaleUnits.revalidate();
            scaleUnits.addPropertyChangeListener("value", this);
            mainBox.add(scaleUnits);
            
            // scale width
            scaleWidth = new NumericProperty("Scale Width:", String.valueOf(mapScale.getWidth()));
            scaleWidth.setLeftMargin(leftMargin);
            scaleWidth.setRightMargin(rightMargin);
            scaleWidth.setBackColour(backColour);
            scaleWidth.setPreferredWidth(preferredWidth);
            scaleWidth.setParseIntegersOnly(true);
            scaleWidth.setTextboxWidth(5);
            scaleWidth.revalidate();
            scaleWidth.addPropertyChangeListener("value", this);
            mainBox.add(scaleWidth);
            
            // scale height
            scaleHeight = new NumericProperty("Scale Height:", String.valueOf(mapScale.getHeight()));
            scaleHeight.setLeftMargin(leftMargin);
            scaleHeight.setRightMargin(rightMargin);
            scaleHeight.setBackColour(Color.WHITE);
            scaleHeight.setPreferredWidth(preferredWidth);
            scaleHeight.setParseIntegersOnly(true);
            scaleHeight.setTextboxWidth(5);
            scaleHeight.revalidate();
            scaleHeight.addPropertyChangeListener("value", this);
            mainBox.add(scaleHeight);
            
            // scale margin
            marginSize = new NumericProperty("Margin size (points)", 
                    String.valueOf(mapScale.getMargin()));
            marginSize.setLeftMargin(leftMargin);
            marginSize.setRightMargin(rightMargin);
            marginSize.setBackColour(backColour);
            marginSize.setTextboxWidth(5);
            marginSize.setParseIntegersOnly(true);
            marginSize.addPropertyChangeListener("value", this);
            marginSize.setPreferredWidth(preferredWidth);
            marginSize.revalidate();
            mainBox.add(marginSize);
            
//            // bar length
//            barLength = new NumericProperty("Bar Length:", String.valueOf(mapScale.getBarLength()));
//            barLength.setLeftMargin(leftMargin);
//            barLength.setRightMargin(rightMargin);
//            barLength.setBackColour(Color.WHITE);
//            barLength.setPreferredWidth(preferredWidth);
//            barLength.setTextboxWidth(5);
//            barLength.revalidate();
//            barLength.addPropertyChangeListener("value", this);
//            mainBox.add(barLength);
            
            // scale representative fraction
            scaleRepFracVisible = new BooleanProperty("Show Representative Fraction?", 
                    mapScale.isBorderVisible());
            scaleRepFracVisible.setLeftMargin(leftMargin);
            scaleRepFracVisible.setRightMargin(rightMargin);
            scaleRepFracVisible.setBackColour(Color.WHITE);
            scaleRepFracVisible.setPreferredWidth(preferredWidth);
            scaleRepFracVisible.revalidate();
            scaleRepFracVisible.addPropertyChangeListener("value", this);
            mainBox.add(scaleRepFracVisible);
            
            outlineColourBox = new ColourProperty("Outline colour", 
                    mapScale.getOutlineColour());
            outlineColourBox.setLeftMargin(leftMargin);
            outlineColourBox.setRightMargin(rightMargin);
            outlineColourBox.setBackColour(backColour);
            outlineColourBox.setPreferredWidth(preferredWidth);
            outlineColourBox.revalidate();
            outlineColourBox.addPropertyChangeListener("value", this);
            mainBox.add(outlineColourBox);
            
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
        if (source == outlineColourBox) {
            mapScale.setOutlineColour(outlineColourBox.getValue());
            didSomething = true;
        } else if (source == scaleWidth) {
            mapScale.setWidth(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == scaleHeight) {
            mapScale.setHeight(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == scaleVisible) {
            mapScale.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
//        } else if (source == barLength) {
//            mapScale.setBarLength(Double.parseDouble((String) evt.getNewValue()));
//            didSomething = true;
        } else if (source == scaleRepFracVisible) {
            mapScale.setRepresentativeFractionVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColourBox) {
            mapScale.setBorderColour(borderColourBox.getValue());
            didSomething = true;
        } else if (source == scaleUnits) {
            mapScale.setUnits(evt.getNewValue().toString());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            mapScale.setBackColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            mapScale.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            mapScale.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == marginSize) {
            mapScale.setMargin(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
