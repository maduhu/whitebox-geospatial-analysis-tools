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
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class Legend implements CartographicElement, Comparable<CartographicElement> {
    boolean visible = false;
    boolean selected = false;
    int number = -1;
    String label = "Legend";
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
    String name = "legend";

    public Legend() {
        labelFont = new Font("SanSerif", Font.PLAIN, 12);
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
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
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
    
    @Override
    public int compareTo(CartographicElement other) {
        final int BEFORE = 1;
        final int EQUAL = 0;
        final int AFTER = -1;
        
        // compare them based on their element (overlay) numbers
        if (this.number < other.getElementNumber()) {
            return BEFORE;
        } else if (this.number > other.getElementNumber()) {
            return AFTER;
        }

        return EQUAL;
    }
}
