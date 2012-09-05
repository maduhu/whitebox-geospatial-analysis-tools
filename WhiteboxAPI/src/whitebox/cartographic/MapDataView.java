/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.cartographic;

import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapDataView implements CartographicElement, Comparable<CartographicElement> {
    boolean visible = false;
    boolean selected = false;
    int number = -1;
    String name = "map data view";
    
    public MapDataView(String name) {
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
