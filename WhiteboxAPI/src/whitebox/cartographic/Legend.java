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
import java.util.ArrayList;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.MapLayer;

/**
 *
 * @author johnlindsay
 */
public class Legend implements CartographicElement, Comparable<CartographicElement> {
    private boolean visible = true;
    private boolean selected = false;
    private int number = -1;
    private String label = "Legend";
    private boolean borderVisible = false;
    private boolean backgroundVisible = true;
    private int upperLeftX = -32768; // initialize with a large negative value
    private int upperLeftY = -32768;
    private int height = -32768; // in points
    private int width = -32768; // in points
    private int margin = 5;
    private Color backColour = Color.WHITE;
    private Color borderColour = Color.BLACK;
    private Color fontColour = Color.BLACK;
    private Font labelFont;
    //private Font[] availableFonts;
    private int fontHeight = 0;
    private String name = "legend";
    private int selectedOffsetX;
    private int selectedOffsetY;
    private ArrayList<MapArea> mapAreas = new ArrayList<>();
    private float borderWidth = 0.75f;
    private float lineWidth = 0.75f;
    
    public Legend(String name) {
        labelFont = new Font("SanSerif", Font.PLAIN, 10);
        this.name = name;
    }
    
    public Color getBackgroundColour() {
        return backColour;
    }

    public void setBackgroundColour(Color backColour) {
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
//        width = -1;
//        height = -1;
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
//        width = -1;
//        height = -1;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color outlineColour) {
        this.borderColour = outlineColour;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
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
//        width = -1;
//        height = -1;
//        upperLeftX = -9999;
//        upperLeftY = -9999;
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
//        width = -1;
//        height = -1;
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
    
    public void addMapArea(MapArea mapArea) {
        mapAreas.add(mapArea);
    }
    
    public void removeMapArea(int index) {
        mapAreas.remove(index);
    }
    
    public void removeMapArea(MapArea mapArea) {
        mapAreas.remove(mapArea);
    }
    
    public MapArea getMapArea(int index) {
        if (mapAreas.size() > index) {
            return mapAreas.get(index);
        } else {
            return null;
        }
    }
    
    public ArrayList<MapArea> getMapAreasList() {
        return mapAreas;
    }
    
    public int getNumberOfMapAreas() {
        return mapAreas.size();
    }
    
    public void clearMapAreas() {
        mapAreas.clear();
    }
    
    public int getNumberOfLegendEntries() {
        int n = 0;
        for (MapArea ma : mapAreas) {
            for (int i = 0; i < ma.getNumLayers(); i++) {
                MapLayer layer = ma.getLayer(i);
                if (layer.isVisible()) {
                    n++;
                }
            }
        }
        return n;
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
        int minSize = 50;
        int deltaX, deltaY;
        switch (resizeMode) {
            case 0: // off the north edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSize) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                break;
            case 1: // off the south edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSize) {
                    height += deltaY;
                }
                break;
            case 2: // off the east edge
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSize) {
                    width += deltaX;
                }
                break;
            case 3: // off the west edge
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSize) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
            case 4: // off the northeast edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSize) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSize) {
                    width += deltaX;
                }
                break;
            case 5: // off the northwest edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSize) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSize) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
            case 6: // off the southeast edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSize) {
                    height += deltaY;
                }
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSize) {
                    width += deltaX;
                }
                break;
            case 7: // off the southwest edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSize) {
                    height += deltaY;
                }
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSize) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
        }
    }

    @Override
    public CartographicElementType getCartographicElementType() {
        return CartographicElementType.LEGEND;
    }
}
