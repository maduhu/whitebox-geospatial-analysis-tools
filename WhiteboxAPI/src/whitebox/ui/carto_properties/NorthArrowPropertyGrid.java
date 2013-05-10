package whitebox.ui.carto_properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import whitebox.cartographic.NorthArrow;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class NorthArrowPropertyGrid extends JPanel implements PropertyChangeListener {
    
    private NorthArrow northArrow;
    private int rightMargin = 20;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private ColourProperty outlineColourBox;
    private BooleanProperty northArrowVisible;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty marginSize;
    private NumericProperty markerSize;
    
    public NorthArrowPropertyGrid() {
        createUI();
    }
    
    public NorthArrowPropertyGrid(NorthArrow northArrow, WhiteboxPluginHost host) {
        this.northArrow = northArrow;
        this.host = host;
        createUI();
    }

    public NorthArrow getNorthArrow() {
        return northArrow;
    }

    public void setNorthArrow(NorthArrow northArrow) {
        this.northArrow = northArrow;
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
            int preferredWidth = 270;
            //scroll.setMaximumSize(new Dimension(1000, preferredWidth));
            //this.add(scroll);
            this.add(mainBox);
            this.setPreferredSize(new Dimension(preferredWidth, 500));
            
            
            northArrowVisible = new BooleanProperty("Is the title visible?", 
                    northArrow.isVisible());
            northArrowVisible.setLeftMargin(leftMargin);
            northArrowVisible.setRightMargin(rightMargin);
            northArrowVisible.setBackColour(backColour);
            northArrowVisible.setPreferredWidth(preferredWidth);
            northArrowVisible.addPropertyChangeListener("value", this);
            northArrowVisible.revalidate();
            mainBox.add(northArrowVisible);

            markerSize = new NumericProperty("Marker size (points)", 
                    String.valueOf(northArrow.getMarkerSize()));
            markerSize.setLeftMargin(leftMargin);
            markerSize.setRightMargin(rightMargin);
            markerSize.setBackColour(Color.WHITE);
            markerSize.setTextboxWidth(5);
            markerSize.setParseIntegersOnly(true);
            markerSize.setMinValue(1);
            markerSize.setMaxValue(250);
            markerSize.addPropertyChangeListener("value", this);
            markerSize.setPreferredWidth(preferredWidth);
            markerSize.revalidate();
            mainBox.add(markerSize);
            
            outlineColourBox = new ColourProperty("Outline colour", 
                    northArrow.getOutlineColour());
            outlineColourBox.setLeftMargin(leftMargin);
            outlineColourBox.setRightMargin(rightMargin);
            outlineColourBox.setBackColour(backColour);
            outlineColourBox.setPreferredWidth(preferredWidth);
            outlineColourBox.revalidate();
            outlineColourBox.addPropertyChangeListener("value", this);
            mainBox.add(outlineColourBox);
            
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    northArrow.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(Color.WHITE);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    northArrow.getBackColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(backColour);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    northArrow.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(Color.WHITE);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            borderColour = new ColourProperty("Border colour", 
                    northArrow.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(backColour);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            marginSize = new NumericProperty("Margin size (points)", 
                    String.valueOf(northArrow.getMargin()));
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
        if (source == outlineColourBox) {
            northArrow.setOutlineColour(outlineColourBox.getValue());
            didSomething = true;
        } else if (source == markerSize) {
            northArrow.setMarkerSize(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == northArrowVisible) {
            northArrow.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            northArrow.setBackColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            northArrow.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            northArrow.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            northArrow.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == marginSize) {
            northArrow.setMargin(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
