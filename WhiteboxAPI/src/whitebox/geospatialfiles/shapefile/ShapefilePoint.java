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
package whitebox.geospatialfiles.shapefile;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ShapefilePoint implements Comparable<ShapefilePoint> {
    public double x = 0.0;
    public double y = 0.0;
    public double z = 0.0;
    public double m = 0.0;

    public ShapefilePoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public ShapefilePoint(double x, double y, double m) {
        this.x = x;
        this.y = y;
        this.m = m;
    }
    
    public ShapefilePoint(double x, double y, double z, double m) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.m = m;
    }
    
    @Override
    public int compareTo(ShapefilePoint t) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        
        if (this.x > t.x) {
            return AFTER;
        } else if (this.x < t.x) {
            return BEFORE;
        } else {
            if (this.y > t.y) {
                return BEFORE;
            } else if (this.y < t.y) {
                return AFTER;
            } else {
                return EQUAL;
            }
        }
    }
}
