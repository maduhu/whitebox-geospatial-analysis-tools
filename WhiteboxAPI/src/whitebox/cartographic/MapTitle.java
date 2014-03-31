/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.cartographic;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapTitle implements CartographicElement, Comparable<CartographicElement>, java.io.Serializable {
    private String cartoElementType = "MapTitle";
    
    boolean visible = true;
    boolean selected = false;
    String name = "MapTitle";
    int number = -1;
    String label = "";
    boolean borderVisible = false;
    boolean backgroundVisible = false;
    boolean outlineVisible = false;
    int upperLeftX = -32768;
    int upperLeftY = -32768;
    int height = -1; // in points
    int width = -1; // in points
    int margin = 5;
    Color backColour = Color.WHITE;
    Color borderColour = Color.BLACK;
    Color fontColour = Color.BLACK;
    Color outlineColour = Color.BLACK;
    Font labelFont = new Font("SanSerif", Font.BOLD, 20);
//    Font[] availableFonts;
    int fontHeight = 0;
    int maxFontSize = 300;
    float lineWidth = 0.75f;
    private int selectedOffsetX;
    private int selectedOffsetY;
    private double rotation = 0;

    public MapTitle() {}
    
    public MapTitle(String name) {
        this.name = name;
        measureFontSizes();
    }
    
    public MapTitle(String label, String name) {
        this.label = label;
        //GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        //Font[] availableFonts = e.getAllFonts();
        this.name = name;
        measureFontSizes();
    }
    
    static int[] fontHeights = new int[300];
    static int[] fontWidths = new int[300];
    private void measureFontSizes() {
        fontHeights = new int[maxFontSize];
        fontWidths = new int[maxFontSize];
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = bi.getGraphics();
        int style = labelFont.getStyle();
        for (int i = 1; i < fontHeights.length; i++) {
            Font font = new Font(labelFont.getName(), style, i);
            FontMetrics metrics = g.getFontMetrics(font);
            fontWidths[i] = metrics.stringWidth(label);
            fontHeights[i] = metrics.getHeight();
        }
        g.dispose();
        bi = null;
    }
    
    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }
    
    public Color getOutlineColour() {
        return outlineColour;
    }

    public void setOutlineColour(Color outlineColour) {
        this.outlineColour = outlineColour;
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
    
    public boolean isOutlineVisible() {
        return outlineVisible;
    }

    public void setOutlineVisible(boolean outlineVisible) {
        this.outlineVisible = outlineVisible;
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
        measureFontSizes();
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

    @Override
    public int getUpperLeftX() {
        return upperLeftX;
    }

    @Override
    public void setUpperLeftX(int upperLeftX) {
        this.upperLeftX = upperLeftX;
    }

    @Override
    public int getUpperLeftY() {
        return upperLeftY;
    }

    @Override
    public void setUpperLeftY(int upperLeftY) {
        this.upperLeftY = upperLeftY;
    }
    
    @Override
    public int getLowerRightX() {
        return upperLeftX + width;
    }

    @Override
    public int getLowerRightY() {
        return upperLeftY + height;
    }
    
    @Override
    public int getSelectedOffsetX() {
        return selectedOffsetX;
    }

    @Override
    public void setSelectedOffsetX(int selectedOffsetX) {
        this.selectedOffsetX = selectedOffsetX;
    }

    @Override
    public int getSelectedOffsetY() {
        return selectedOffsetY;
    }

    @Override
    public void setSelectedOffsetY(int selectedOffsetY) {
        this.selectedOffsetY = selectedOffsetY;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        width = -1;
        height = -1;
        upperLeftX = -32768;
        upperLeftY = -32768;
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
        measureFontSizes();
    }

    public int getFontHeight() {
        return fontHeight;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public int getElementNumber() {
        return number;
    }

    @Override
    public void setElementNumber(int number) {
        this.number = number;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public int getMaxFontSize() {
        return maxFontSize;
    }

    public void setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    
    
    
    @Override
    public int compareTo(CartographicElement other) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        
        // compare them based on their element (overlay) numbers
        if (this.number < other.getElementNumber()) {
            return BEFORE;
        } else if (this.number > other.getElementNumber()) {
            return AFTER;
        }

        return EQUAL;
    }

    @Override
    public void resize(int x, int y, int resizeMode) {
        int minSize = fontHeights[1];
        int deltaX;
        int deltaY;
        int actualFontHeight = height - 2 * margin;
        int actualFontWidth = width - 2 * margin;
        int minDiff = Integer.MAX_VALUE;
        int whichSize = - 1;
        int j;
        switch (resizeMode) {
            case 0: // off the north edge
                deltaY = y - upperLeftY;
                actualFontHeight -= deltaY;
                if (actualFontHeight < minSize) { 
                    actualFontHeight = minSize;
                } 
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontHeights[i] - actualFontHeight) * (fontHeights[i] - actualFontHeight);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                upperLeftY += deltaY;
                break;
            case 1: // off the south edge
                deltaY = y - (upperLeftY + height);
                actualFontHeight += deltaY;
                if (actualFontHeight < minSize) { 
                    actualFontHeight = minSize;
                } 
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontHeights[i] - actualFontHeight) * (fontHeights[i] - actualFontHeight);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                break;
            case 2: // off the east edge
                deltaX = x - (upperLeftX + width);
                actualFontWidth += deltaX;
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontWidths[i] - actualFontWidth) * (fontWidths[i] - actualFontWidth);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                break;
            case 3: // off the west edge
                deltaX = x - upperLeftX;
                actualFontWidth -= deltaX;
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontWidths[i] - actualFontWidth) * (fontWidths[i] - actualFontWidth);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                upperLeftX += deltaX;
                break;
            case 4: // off the northeast edge
                deltaY = y - upperLeftY;
                actualFontHeight -= deltaY;
                if (actualFontHeight < minSize) { 
                    actualFontHeight = minSize;
                } 
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontHeights[i] - actualFontHeight) * (fontHeights[i] - actualFontHeight);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                upperLeftY += deltaY;
                break;
            case 5: // off the northwest edge
                deltaY = y - upperLeftY;
                actualFontHeight -= deltaY;
                if (actualFontHeight < minSize) { 
                    actualFontHeight = minSize;
                } 
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontHeights[i] - actualFontHeight) * (fontHeights[i] - actualFontHeight);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                upperLeftY += deltaY;
                break;
            case 6: // off the southeast edge
                deltaY = y - (upperLeftY + height);
                actualFontHeight += deltaY;
                if (actualFontHeight < minSize) { 
                    actualFontHeight = minSize;
                } 
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontHeights[i] - actualFontHeight) * (fontHeights[i] - actualFontHeight);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                break;
            case 7: // off the southwest edge
                deltaY = y - (upperLeftY + height);
                actualFontHeight += deltaY;
                if (actualFontHeight < minSize) { 
                    actualFontHeight = minSize;
                } 
                for (int i = 1; i < maxFontSize; i++) {
                    j = (fontHeights[i] - actualFontHeight) * (fontHeights[i] - actualFontHeight);
                    if (j < minDiff) {
                        minDiff = j;
                        whichSize = i;
                    }
                }
                this.setLabelFont(new Font(labelFont.getFamily(), labelFont.getStyle(), whichSize));
                break;
                
        }
    }
    
    @Override
    public CartographicElementType getCartographicElementType() {
        return CartographicElementType.MAP_TITLE;
    }
}
