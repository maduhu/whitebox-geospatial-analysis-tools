/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
public class BoundingBox implements Comparable<BoundingBox>, java.io.Serializable {
    
    public BoundingBox() {
        this.maxY = Float.NEGATIVE_INFINITY;
        this.maxX = Float.NEGATIVE_INFINITY;
        this.minY = Float.POSITIVE_INFINITY;
        this.minX = Float.POSITIVE_INFINITY;
    }
    
    public BoundingBox(Double minX, Double minY, Double maxX, Double maxY) {
            this.maxY = maxY;
            this.minY = minY;
            this.maxX = maxX;
            this.minX = minX;
    }
    
    private double maxY = -1;
    public double getMaxY() {
        return maxY;
    }
    
    public void setMaxY(double value) {
        maxY = value;
    }
    
    private double maxX = -1;
    public double getMaxX() {
        return maxX;
    }
    
    public void setMaxX(double value) {
        maxX = value;
    }
    
    private double minY = 0;
    public double getMinY() {
        return minY;
    }
    
    public void setMinY(double value) {
        minY = value;
    }
    
    
    private double minX = 0;
    public double getMinX() {
        return minX;
    }
    
    public void setMinX(double value) {
        minX = value;
    }
    
    public boolean isNull() {
        return maxX < minX;
    }
    
    public double getWidth() {
        return maxX - minX;
    }
    
    public double getHeight() {
        return maxY - minY;
    }
    
    public double getMinExtent() {
        if (isNull()) {
            return 0.0;
        }
        double w = getWidth();
        double h = getHeight();
        if (w < h) {
            return w;
        }
        return h;
    }
    
    public double getMaxExtent() {
        if (isNull()) {
            return 0.0;
        }
        double w = getWidth();
        double h = getHeight();
        if (w > h) {
            return w;
        }
        return h;
    }
    
    public boolean near(BoundingBox other, double distance) {
        if (overlaps(other)) { return true; }
        if (intersectsAnEdgeOf(other)) { return true; }
        if (within(other)) { return true; }
        if (entirelyContains(other)) { return true; }
        if (Math.abs(other.minY - maxY) <= distance) { return true; } // just south of
        if (Math.abs(other.maxY - minY) <= distance) { return true; } // just north of
        if (Math.abs(other.minX - maxX) <= distance) { return true; } // just west of
        if (Math.abs(other.maxX - minX) <= distance) { return true; } // just east of
        
        return false;
    }
    
    public boolean overlaps(BoundingBox other) {
        if (isNull()) { return false; }
        if (this.maxY < other.getMinY()
                || this.maxX < other.getMinX()
                || this.minY > other.getMaxY()
                || this.minX > other.getMaxX()) {
            return false;
        } else {
            return true;
        }
    }
    
    public boolean intersectsAnEdgeOf(BoundingBox other) {
        if (isNull()) { return false; }
        double x, y;
        boolean oneInsideFound = false;
        boolean oneOutsideFound = false;
        // at least one of the coordinates has to be within and at least one of them has to be outside
        for (int a = 0; a < 4; a++) {
            switch (a) {
                case 0:
                    x = minX;
                    y = maxY;
                    break;
                case 1:
                    x = minX;
                    y = minY;
                    break;
                case 2:
                    x = maxX;
                    y = maxY;
                    break;
                default: // 3
                    x = maxX;
                    y = minY;
                    break;
            }
            if (!oneInsideFound) {
                if (y <= other.getMaxY() && y >= other.getMinY()
                        && x <= other.getMaxX() && x >= other.getMinX()) {
                    oneInsideFound = true;
                }
            }
            if (!oneOutsideFound) {
                if (!(y <= other.getMaxY() && y >= other.getMinY())
                        || !(x <= other.getMaxX() && x >= other.getMinX())) {
                    oneOutsideFound = true;
                }
            }
            if (oneInsideFound && oneOutsideFound) {
                return true;
            }
        }
        return false;
    }
    
    public boolean entirelyContainedWithin(BoundingBox other) {
        if (isNull()) { return false; }
        if (this.maxY < other.getMaxY()
                && this.maxX < other.getMaxX()
                && this.minY > other.getMinY()
                && this.minX > other.getMinX()) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean within(BoundingBox other) {
        if (isNull()) { return false; }
        if (this.maxY <= other.getMaxY()
                && this.maxX <= other.getMaxX()
                && this.minY >= other.getMinY()
                && this.minX >= other.getMinX()) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean entirelyContains(BoundingBox other) {
        if (isNull()) { return false; }
        if (other.getMaxY() < this.maxY
                && other.getMaxX() < this.maxX
                && other.getMinY() > this.minY
                && other.getMinX() > this.minX) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean contains(BoundingBox other) {
        if (isNull()) { return false; }
        if (other.getMaxY() <= this.maxY
                && other.getMaxX() <= this.maxX
                && other.getMinY() >= this.minY
                && other.getMinX() >= this.minX) {
            return true;
        } else {
            return false;
        }
    }
    
    public BoundingBox intersect(BoundingBox other) {
        BoundingBox bb = new BoundingBox();
        if (!isNull()) {
            // some performance tests have shown this to be better than Math.min 
            // and Math.max
            bb.setMaxY((this.maxY <= other.getMaxY()) ? this.maxY : other.getMaxY()); 
            bb.setMaxX((this.maxX <= other.getMaxX()) ? this.maxX : other.getMaxX()); 
            bb.setMinY((this.minY >= other.getMinY()) ? this.minY : other.getMinY());
            bb.setMinX((this.minX >= other.getMinX()) ? this.minX : other.getMinX());
        }
        return bb;
    }
    
    public boolean isPointInBox(double x, double y) {
        if (isNull()) { return false; }
        if (this.maxY < y || this.maxX < x || this.minY > y || this.minX > x) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public BoundingBox clone() {
        BoundingBox db = new BoundingBox(minX, minY, maxX, maxY);
        return db;
    }
    
    @Override
    public int compareTo(BoundingBox other) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.maxY < other.maxY) {
            return BEFORE;
        } else if (this.maxY > other.maxY) {
            return AFTER;
        }

        if (this.maxX < other.maxX) {
            return BEFORE;
        } else if (this.maxX > other.maxX) {
            return AFTER;
        }

        if (this.minY < other.minY) {
            return BEFORE;
        } else if (this.minY > other.minY) {
            return AFTER;
        }
        
        if (this.minX < other.minX) {
            return BEFORE;
        } else if (this.minX > other.minX) {
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
        if (!(aThat instanceof BoundingBox)) {
            return false;
        }

        BoundingBox that = (BoundingBox) aThat;
        return (this.maxY == that.maxY)
                && (this.maxX == that.maxX)
                && (this.minY == that.minY)
                && (this.minX == that.minX);
    }

    /**
     * A class that overrides equals must also override hashCode.
     */
    @Override
    public int hashCode() {
        int result = HashCodeUtil.SEED;
        result = HashCodeUtil.hash(result, maxY);
        result = HashCodeUtil.hash(result, maxX);
        result = HashCodeUtil.hash(result, minY);
        result = HashCodeUtil.hash(result, minX);
        return result;
    }
}

