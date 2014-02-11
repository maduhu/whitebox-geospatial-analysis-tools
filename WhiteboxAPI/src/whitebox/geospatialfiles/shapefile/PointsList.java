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

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PointsList {

    private ArrayList<ShapefilePoint> myList = new ArrayList<>();
    boolean isClosedForAdding = false;

    public PointsList() {

    }

    public PointsList(ArrayList<ShapefilePoint> list) {
        for (ShapefilePoint sfp : list) {
            myList.add(sfp);
        }
    }

    public void addPoint(double x, double y) {
        if (isClosedForAdding) {
            return;
        }
        ShapefilePoint sfp = new ShapefilePoint(x, y);
        myList.add(sfp);
    }

    public void addMPoint(double x, double y, double m) {
        if (isClosedForAdding) {
            return;
        }
        ShapefilePoint sfp = new ShapefilePoint(x, y);
        sfp.m = m;
        myList.add(sfp);
    }

    public void addMPoint(double x, double y) {
        if (isClosedForAdding) {
            return;
        }
        ShapefilePoint sfp = new ShapefilePoint(x, y);
        myList.add(sfp);
    }

    public void addZPoint(double x, double y, double z, double m) {
        if (isClosedForAdding) {
            return;
        }
        ShapefilePoint sfp = new ShapefilePoint(x, y);
        sfp.z = z;
        sfp.m = m;
        myList.add(sfp);
    }

    public void addZPoint(double x, double y, double z) {
        if (isClosedForAdding) {
            return;
        }
        ShapefilePoint sfp = new ShapefilePoint(x, y);
        sfp.z = z;
        myList.add(sfp);
    }

    public void clear() {
        myList.clear();
    }

    public void removePoint(int i) {
        myList.remove(i);
    }

    public ShapefilePoint getPoint(int i) {
        return myList.get(i);
    }

    public double[][] getPointsArray() {
        double[][] ret = new double[myList.size()][2];
        int i = 0;
        for (ShapefilePoint sfp : myList) {
            ret[i][0] = sfp.x;
            ret[i][1] = sfp.y;
            i++;
        }
        return ret;
    }

    public double[] getZArray() {
        double[] ret = new double[myList.size()];
        int i = 0;
        for (ShapefilePoint sfp : myList) {
            ret[i] = sfp.z;
            i++;
        }
        return ret;
    }

    public double[] getMArray() {
        double[] ret = new double[myList.size()];
        int i = 0;
        for (ShapefilePoint sfp : myList) {
            ret[i] = sfp.m;
            i++;
        }
        return ret;
    }

    public void closePolygon() {
        ShapefilePoint firstPoint = myList.get(0);
        ShapefilePoint sfp = new ShapefilePoint(firstPoint.x, firstPoint.y);
        myList.add(sfp);
        isClosedForAdding = true;
    }

    public boolean isClockwiseOrder() {
        // Note: holes are polygons that have verticies in counter-clockwise order

        // This approach is based on the method described by Paul Bourke, March 1998
        // http://paulbourke.net/geometry/clockwise/index.html
        //int stPoint, endPoint;
        double x0, y0, x1, y1, x2, y2;
        int n1 = 0, n2 = 0, n3 = 0;
        int numPointsInList = myList.size();

        if (numPointsInList < 2) {
            return false;
        } // something's wrong! 
        // first see if it is a convex or concave polygon
        // calculate the cross product for each adjacent edge.
        double[] crossproducts = new double[numPointsInList];
        for (int j = 0; j < numPointsInList; j++) {
            n2 = j;
            if (j == 0) {
                n1 = numPointsInList - 1;
                n3 = j + 1;
            } else if (j == numPointsInList - 1) {
                n1 = j - 1;
                n3 = 0;
            } else {
                n1 = j - 1;
                n3 = j + 1;
            }
            x0 = myList.get(n1).x;
            y0 = myList.get(n1).y; //points[n1][1];
            x1 = myList.get(n2).x; //points[n2][0];
            y1 = myList.get(n2).y; //points[n2][1];
            x2 = myList.get(n3).x; //points[n3][0];
            y2 = myList.get(n3).y; //points[n3][1];
            crossproducts[j] = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
        }
        boolean testSign;
        testSign = crossproducts[0] >= 0;
        boolean isConvex = true;
        for (int j = 1; j < numPointsInList; j++) {
            if (crossproducts[j] >= 0 && !testSign) {
                isConvex = false;
                break;
            } else if (crossproducts[j] < 0 && testSign) {
                isConvex = false;
                break;
            }
        }

        // now see if it is clockwise or counter-clockwise
        boolean isHole;
        if (isConvex) {
            if (testSign) { // positive means counter-clockwise
                isHole = true;
            } else {
                isHole = false;
            }
        } else {
            // calculate the polygon area. If it is positive is is in clockwise order, else counter-clockwise.
            double area2 = 0;
            for (int j = 0; j < numPointsInList; j++) {
                n1 = j;
                if (j < numPointsInList - 1) {
                    n2 = j + 1;
                } else {
                    n2 = 0;
                }
                x1 = myList.get(n1).x; //points[n1][0];
                y1 = myList.get(n1).y; //points[n1][1];
                x2 = myList.get(n2).x; //points[n2][0];
                y2 = myList.get(n2).y; //points[n2][1];

                area2 += (x1 * y2) - (x2 * y1);
            }
            area2 = area2 / 2.0;

            if (area2 < 0) { // a positive area indicates counter-clockwise order
                isHole = false;
            } else {
                isHole = true;
            }
        }
        return isHole;
    }

    public int size() {
        return myList.size();
    }

    public void reverseOrder() {
        Collections.reverse(myList);
    }

    public void removeDuplicates() {
        int numElements = myList.size();
        for (int i = numElements - 1; i > 0; i--) {
            ShapefilePoint p1 = myList.get(i);
            ShapefilePoint p2 = myList.get(i - 1);
            if (p1.equals(p2)) {
                myList.remove(i);
            }
        }
    }

    @Override
    public PointsList clone() throws CloneNotSupportedException {
//        super.clone();
        ArrayList<ShapefilePoint> newList = new ArrayList<>();
        for (ShapefilePoint p : myList) {
            newList.add(p);
        }
        return new PointsList(newList);
    }

    public void appendList(PointsList other) {
        if (other == null) {
            return;
        }
        for (int i = 0; i < other.size(); i++) {
            ShapefilePoint p = other.getPoint(i);
            myList.add(p);
        }
    }
}
