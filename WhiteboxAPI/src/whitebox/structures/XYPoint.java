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
public class XYPoint implements Comparable<XYPoint> {

    public double x;
    public double y;
    
    public XYPoint() {
        
    }
    
    public XYPoint(double X, double Y) {
        x = X;
        y = Y;
    }

    @Override
    public int compareTo(XYPoint cell) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.x < cell.x) {
            return BEFORE;
        } else if (this.x > cell.x) {
            return AFTER;
        }

        if (this.y < cell.y) {
            return BEFORE;
        } else if (this.y > cell.y) {
            return AFTER;
        }

        return EQUAL;
    }

}
