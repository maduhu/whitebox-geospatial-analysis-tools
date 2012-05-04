/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.structures;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class DimensionBox implements Comparable<DimensionBox>  {
    
    public DimensionBox() {
        
    }
    
    public DimensionBox(Double top, Double right, Double bottom, Double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }
    
    private double top = 0;
    public double getTop() {
        return top;
    }
    
    public void setTop(double value) {
        top = value;
    }
    
    private double right = 0;
    public double getRight() {
        return right;
    }
    
    public void setRight(double value) {
        right = value;
    }
    
    
    private double bottom = 0;
    public double getBottom() {
        return bottom;
    }
    
    public void setBottom(double value) {
        bottom = value;
    }
    
    
    private double left = 0;
    public double getLeft() {
        return left;
    }
    
    public void setLeft(double value) {
        left = value;
    }
    
    @Override
    public DimensionBox clone() {
        DimensionBox db = new DimensionBox(top, right, bottom, left);
        return db;
    }
    
    @Override
    public int compareTo(DimensionBox other) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.top < other.top) {
            return BEFORE;
        } else if (this.top > other.top) {
            return AFTER;
        }

        if (this.right < other.right) {
            return BEFORE;
        } else if (this.right > other.right) {
            return AFTER;
        }

        if (this.bottom < other.bottom) {
            return BEFORE;
        } else if (this.bottom > other.bottom) {
            return AFTER;
        }
        
        if (this.left < other.left) {
            return BEFORE;
        } else if (this.left > other.left) {
            return AFTER;
        }
        return EQUAL;
    }
    
    /**
     * Define equality of state.
     */
    @Override
    public boolean equals(Object aThat) {
        if (this == aThat) {
            return true;
        }
        if (!(aThat instanceof DimensionBox)) {
            return false;
        }

        DimensionBox that = (DimensionBox) aThat;
        return (this.top == that.top)
                && (this.right == that.right)
                && (this.bottom == that.bottom)
                && (this.left == that.left);
    }

    /**
     * A class that overrides equals must also override hashCode.
     */
    @Override
    public int hashCode() {
        int result = HashCodeUtil.SEED;
        result = HashCodeUtil.hash(result, top);
        result = HashCodeUtil.hash(result, right);
        result = HashCodeUtil.hash(result, bottom);
        result = HashCodeUtil.hash(result, left);
        return result;
    }
}

