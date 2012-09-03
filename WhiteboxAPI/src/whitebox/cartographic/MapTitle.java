/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.cartographic;

import java.awt.Color;
import java.awt.Font;

/**
 *
 * @author johnlindsay
 */
public class MapTitle {
    boolean visible = false;
    String label = "";
    boolean borderVisible = false;
    boolean backgroundVisible = false;
    int upperLeftX = -9999;
    int upperLeftY = -9999;
    int height = -1; // in points
    int width = -1; // in points
    int margin = 5;
    Color backColour = Color.WHITE;
    Color borderColour = Color.BLACK;
    Color fontColour = Color.BLACK;
    Font labelFont;
    Font[] availableFonts;
    int fontHeight = 0;

    public MapTitle(String label) {
        this.label = label;
        //GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        //Font[] availableFonts = e.getAllFonts();
        labelFont = new Font("SanSerif", Font.BOLD, 20);
        //setWidthAndHeight();
    }
    
    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public boolean isBackgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
    }

    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public Color getFontColour() {
        return fontColour;
    }

    public void setFontColour(Color fontColour) {
        this.fontColour = fontColour;
    }

    public Font getLabelFont() {
        return labelFont;
    }

    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
        width = -1;
        height = -1;
    }
    
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
        width = -1;
        height = -1;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color outlineColour) {
        this.borderColour = outlineColour;
    }

    public int getUpperLeftX() {
        return upperLeftX;
    }

    public void setUpperLeftX(int upperLeftX) {
        if (upperLeftX >= 0) {
            this.upperLeftX = upperLeftX;
        }
    }

    public int getUpperLeftY() {
        return upperLeftY;
    }

    public void setUpperLeftY(int upperLeftY) {
        if (upperLeftY >= 0) {
            this.upperLeftY = upperLeftY;
        }
    }
    
    public int getLowerRightX() {
        return upperLeftX + width;
    }

    public int getLowerRightY() {
        return upperLeftY + height;
    }
    
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        width = -1;
        height = -1;
        upperLeftX = -9999;
        upperLeftY = -9999;
    }

    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        width = -1;
        height = -1;
    }

    public int getFontHeight() {
        return fontHeight;
    }
}
