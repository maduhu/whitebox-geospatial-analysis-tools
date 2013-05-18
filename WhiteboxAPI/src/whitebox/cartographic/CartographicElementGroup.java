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
package whitebox.cartographic;

import java.util.ArrayList;
import java.util.List;
import whitebox.interfaces.CartographicElement;

/**
 * This is a container for other cartographic elements that allows for grouping,
 * such that the elements can be treated as a single object for drawing.
 * @author johnlindsay
 */
public class CartographicElementGroup implements CartographicElement, Comparable<CartographicElement> {
    private String cartoElementType = "CartographicElementGroup";
    
    private int upperLeftX = -32768;
    private int upperLeftY = -32768;
    private int height = -1; // in points
    private int width = -1; // in points
    private boolean visible = true;
    private boolean selected = false;
    private int number = -1;
    private String name = "Neatline";
    private int selectedOffsetX;
    private int selectedOffsetY;
    private List<CartographicElement> elementList = new ArrayList<>();
    
    public CartographicElementGroup() {
        // no-arg constructor
    }
    
    public CartographicElementGroup(String name) {
        this.name = name;
    }
    
    public CartographicElementGroup(String name, List<CartographicElement> elementList) {
        this.name = name;
        this.elementList = elementList;
        setBox();
    }

    public List<CartographicElement> getElementList() {
        return elementList;
    }

    public void setElementList(List<CartographicElement> elementList) {
        this.elementList = elementList;
        setBox();
    }
    
    private void setBox() {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (CartographicElement ce : elementList) {
            if (ce.getUpperLeftX() < minX) { minX = ce.getUpperLeftX(); }
            if (ce.getUpperLeftY() < minY) { minY = ce.getUpperLeftY(); }
            if (ce.getLowerRightX() > maxX) { maxX = ce.getLowerRightX(); }
            if (ce.getLowerRightY() > maxY) { maxY = ce.getLowerRightY(); }
        }
        upperLeftX = minX;
        upperLeftY = minY;
        width = maxX - minX;
        height = maxY - minY;
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
        int delta = upperLeftX - this.upperLeftX;
        int value;
        this.upperLeftX = upperLeftX;
        for (CartographicElement ce : elementList) {
            value = ce.getUpperLeftX();
            ce.setUpperLeftX(value + delta);
        }
    }

    @Override
    public int getUpperLeftY() {
        return upperLeftY;
    }

    @Override
    public void setUpperLeftY(int upperLeftY) {
        int delta = upperLeftY - this.upperLeftY;
        int value;
        this.upperLeftY = upperLeftY;
        for (CartographicElement ce : elementList) {
            value = ce.getUpperLeftY();
            ce.setUpperLeftY(value + delta);
        }
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
    
    public int getHeight() {
        return height;
    }
    
    // methods
    
    public void addElement(CartographicElement ce) {
        elementList.add(ce);
        setBox();
    }
    
    public void removeElement(int i) {
        if (i < elementList.size()) {
            elementList.remove(i);
        }
        setBox();
    }
    
    @Override
    public void resize(int x, int y, int resizeMode) {
        // can't resize a cartographic element group.
//        int minSize = 50;
//        int deltaX, deltaY;
//        switch (resizeMode) {
//            case 0: // off the north edge
//                deltaY = y - upperLeftY;
//                if (height - deltaY >= minSize) {
//                    upperLeftY = y;
//                    height -= deltaY;
//                }
//                break;
//            case 1: // off the south edge
//                deltaY = y - (upperLeftY + height);
//                if (height + deltaY >= minSize) {
//                    height += deltaY;
//                }
//                break;
//            case 2: // off the east edge
//                deltaX = x - (upperLeftX + width);
//                if (width + deltaX >= minSize) {
//                    width += deltaX;
//                }
//                break;
//            case 3: // off the west edge
//                deltaX = x - upperLeftX;
//                if (width - deltaX >= minSize) {
//                    upperLeftX = x;
//                    width -= deltaX;
//                }
//                break;
//            case 4: // off the northeast edge
//                deltaY = y - upperLeftY;
//                if (height - deltaY >= minSize) {
//                    upperLeftY = y;
//                    height -= deltaY;
//                }
//                deltaX = x - (upperLeftX + width);
//                if (width + deltaX >= minSize) {
//                    width += deltaX;
//                }
//                break;
//            case 5: // off the northwest edge
//                deltaY = y - upperLeftY;
//                if (height - deltaY >= minSize) {
//                    upperLeftY = y;
//                    height -= deltaY;
//                }
//                deltaX = x - upperLeftX;
//                if (width - deltaX >= minSize) {
//                    upperLeftX = x;
//                    width -= deltaX;
//                }
//                break;
//            case 6: // off the southeast edge
//                deltaY = y - (upperLeftY + height);
//                if (height + deltaY >= minSize) {
//                    height += deltaY;
//                }
//                deltaX = x - (upperLeftX + width);
//                if (width + deltaX >= minSize) {
//                    width += deltaX;
//                }
//                break;
//            case 7: // off the southwest edge
//                deltaY = y - (upperLeftY + height);
//                if (height + deltaY >= minSize) {
//                    height += deltaY;
//                }
//                deltaX = x - upperLeftX;
//                if (width - deltaX >= minSize) {
//                    upperLeftX = x;
//                    width -= deltaX;
//                }
//                break;
//        }
    }
    
    @Override
    public CartographicElement.CartographicElementType getCartographicElementType() {
        return CartographicElement.CartographicElementType.CARTOGRAPHIC_ELEMENT_GROUP;
    }
}
