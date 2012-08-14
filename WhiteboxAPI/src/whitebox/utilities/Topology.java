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
package whitebox.utilities;

import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Topology {

    public static boolean isLineClosed(Coordinate[] coords) {
        int lastIndex = coords.length - 1;
        return (coords[0].x == coords[lastIndex].x) & 
                (coords[0].y == coords[lastIndex].y);
    }
    
    public static boolean isClockwisePolygon(Coordinate[] coords) {
        // Note: holes are polygons that have verticies in counter-clockwise order

        // This approach is based on the method described by Paul Bourke, March 1998
        // http://paulbourke.net/geometry/clockwise/index.html

        double x0, y0, x1, y1, x2, y2;
        int n1 = 0, n2 = 0, n3 = 0;
        int numPoints = coords.length;
        
        // are the first and last points the same? If so, remove the last point.
        if ((coords[0].x == coords[numPoints - 1].x) && (coords[0].y == coords[numPoints - 1].y)) {
            Coordinate[] coords2 = new Coordinate[numPoints - 1];
            System.arraycopy(coords, 0, coords2, 0, numPoints - 1);
            coords = new Coordinate[numPoints - 1];
            System.arraycopy(coords2, 0, coords, 0, numPoints - 1);
            numPoints--;
        }
        
        if (numPoints < 3) {
            throw new IllegalArgumentException("Degenerate polygon with less than three points.");
        }

        // first see if it is a convex or concave polygon
        // calculate the cross product for each adjacent edge.
        double[] crossproducts = new double[numPoints];

        for (int j = 0; j < numPoints; j++) {
            n2 = j;
            if (j == 0) {
                n1 = numPoints - 1;
                n3 = j + 1;
            } else if (j == numPoints - 1) {
                n1 = j - 1;
                n3 = 0;
            } else {
                n1 = j - 1;
                n3 = j + 1;
            }
            x0 = coords[n1].x;
            y0 = coords[n1].y;
            x1 = coords[n2].x;
            y1 = coords[n2].y;
            x2 = coords[n3].x;
            y2 = coords[n3].y;
            crossproducts[j] = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
        }
        boolean testSign;
        if (crossproducts[0] >= 0) {
            testSign = true; // positive
        } else {
            testSign = false; // negative
        }
        boolean isConvex = true;
        for (int j = 1; j < numPoints; j++) {
            if (crossproducts[j] >= 0 && !testSign) {
                isConvex = false;
                break;
            } else if (crossproducts[j] < 0 && testSign) {
                isConvex = false;
                break;
            }
        }

        // now see if it is clockwise or counter-clockwise
        if (isConvex) {
            if (testSign) { // positive means counter-clockwise
                return false;
            } else {
                return true;
            }
        } else {
            // calculate the polygon area. If it is positive is is in clockwise order, else counter-clockwise.
            double area = 0;
            for (int j = 0; j < numPoints; j++) {
                n1 = j;
                if (j < numPoints - 1) {
                    n2 = j + 1;
                } else {
                    n2 = 0;
                }
                x1 = coords[n1].x;
                y1 = coords[n1].y;
                x2 = coords[n2].x;
                y2 = coords[n2].y;

                area += (x1 * y2) - (x2 * y1);
            }
            // if this were the true area, we'd half it, but we're only interested in the sign.
            if (area < 0) { // a positive area indicates counter-clockwise order
                return true;
            } else {
                return false;
            }
        }
    }
    
    
    public static boolean isPolygonConvex(Coordinate[] coords) {
        // Note: holes are polygons that have verticies in counter-clockwise order

        // This approach is based on the method described by Paul Bourke, March 1998
        // http://paulbourke.net/geometry/clockwise/index.html

        double x0, y0, x1, y1, x2, y2;
        int n1 = 0, n2 = 0, n3 = 0;
        int numPoints = coords.length;
        
        // are the first and last points the same? If so, remove the last point.
        if ((coords[0].x == coords[numPoints - 1].x) && (coords[0].y == coords[numPoints - 1].y)) {
            Coordinate[] coords2 = new Coordinate[numPoints - 1];
            System.arraycopy(coords, 0, coords2, 0, numPoints - 1);
            coords = new Coordinate[numPoints - 1];
            System.arraycopy(coords2, 0, coords, 0, numPoints - 1);
            numPoints--;
        }
        
        if (numPoints < 3) {
            throw new IllegalArgumentException("Degenerate polygon with less than three points.");
        }

        // first see if it is a convex or concave polygon
        // calculate the cross product for each adjacent edge.
        double[] crossproducts = new double[numPoints];

        for (int j = 0; j < numPoints; j++) {
            n2 = j;
            if (j == 0) {
                n1 = numPoints - 1;
                n3 = j + 1;
            } else if (j == numPoints - 1) {
                n1 = j - 1;
                n3 = 0;
            } else {
                n1 = j - 1;
                n3 = j + 1;
            }
            x0 = coords[n1].x;
            y0 = coords[n1].y;
            x1 = coords[n2].x;
            y1 = coords[n2].y;
            x2 = coords[n3].x;
            y2 = coords[n3].y;
            crossproducts[j] = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
        }
        boolean testSign;
        if (crossproducts[0] >= 0) {
            testSign = true; // positive
        } else {
            testSign = false; // negative
        }
        boolean isConvex = true;
        for (int j = 1; j < numPoints; j++) {
            if (crossproducts[j] >= 0 && !testSign) {
                isConvex = false;
                break;
            } else if (crossproducts[j] < 0 && testSign) {
                isConvex = false;
                break;
            }
        }

        return isConvex;
        
    }
}
