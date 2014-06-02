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
package whitebox.interfaces;

/**
 *
 * @author johnlindsay
 */
public interface CartographicElement extends Comparable<CartographicElement> {
    public boolean isVisible();
    
    public void setVisible(boolean visible);
    
    public boolean isSelected();
    
    public void setSelected(boolean selected);
    
    public int getElementNumber();
    
    public void setElementNumber(int number);
    
    public CartographicElementType getCartographicElementType();
    
    public String getName();
    
    public void setName(String name);
    
    @Override
    public int compareTo(CartographicElement other);
    
    public int getUpperLeftX();
    
    public int getUpperLeftY();
    
    public void setUpperLeftX(int x);
    
    public void setUpperLeftY(int y);
    
    public int getLowerRightX();
    
    public int getLowerRightY();
    
    public void resize(int x, int y, int resizeMode);
    
    public void setSelectedOffsetX(int x);
    
    public void setSelectedOffsetY(int y);
    
    public int getSelectedOffsetX();
    
    public int getSelectedOffsetY();
    
    /**
     * An enum of cartographic element types.
     */
    public enum CartographicElementType {
        LEGEND, MAP_AREA, MAP_SCALE, MAP_TITLE, NEATLINE, NORTH_ARROW, MAP_TEXT_AREA,
        MAP_IMAGE, CARTOGRAPHIC_ELEMENT_GROUP, MAP_LABEL
    }
}
