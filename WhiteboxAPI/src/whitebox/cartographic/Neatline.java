/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.cartographic;

import java.awt.Color;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class Neatline implements CartographicElement, Comparable<CartographicElement> {
    private String cartoElementType = "NeatLine";
    
    int upperLeftX = -32768;
    int upperLeftY = -32768;
    int height = -1; // in points
    int width = -1; // in points
    boolean visible = true;
    boolean borderVisible = true;
    boolean backgroundVisible = true;
    boolean selected = false;
    boolean doubleLine = true;
    int doubleLineGap = 2;
    float innerLineWidth = 0.75f;
    float outerLineWidth = 1.5f;
    Color borderColour = Color.BLACK;
    Color backgroundColour = Color.WHITE;
    int number = -1;
    String name = "NeatLine";
    private int selectedOffsetX;
    private int selectedOffsetY;
    
    public Neatline(String name) {
        this.name = name;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
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
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
        
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }

    public Color getBackgroundColour() {
        return backgroundColour;
    }

    public void setBackgroundColour(Color backgroundColour) {
        this.backgroundColour = backgroundColour;
    }
    
    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color borderColour) {
        this.borderColour = borderColour;
    }

    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public boolean isBackgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
    }

    public boolean isDoubleLine() {
        return doubleLine;
    }

    public void setDoubleLine(boolean doubleLine) {
        this.doubleLine = doubleLine;
    }

    public int getDoubleLineGap() {
        return doubleLineGap;
    }

    public void setDoubleLineGap(int doubleLineGap) {
        this.doubleLineGap = doubleLineGap;
    }

    public float getInnerLineWidth() {
        return innerLineWidth;
    }

    public void setInnerLineWidth(float innerLineWidth) {
        this.innerLineWidth = innerLineWidth;
    }

    public float getOuterLineWidth() {
        return outerLineWidth;
    }

    public void setOuterLineThickness(float outerLineWidth) {
        this.outerLineWidth = outerLineWidth;
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
        return CartographicElementType.NEATLINE;
    }
}
