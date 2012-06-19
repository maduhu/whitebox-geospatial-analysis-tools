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

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PolygonM implements Geometry {
    //private double[] box = new double[4];
    private BoundingBox bb;
    private int numParts;
    private int numPoints;
    private int[] parts;
    private double[][] points;
    private double mMin = 0;
    private double mMax = 0;
    private double[] mArray;
    private boolean[] isHole;
    private boolean[] isConvex;
    private double maxExtent;
    private boolean mIncluded = false;
    
    public PolygonM(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            
            bb = new BoundingBox(buf.getDouble(0), buf.getDouble(8), 
                    buf.getDouble(16), buf.getDouble(24));
            maxExtent = bb.getMaxExtent();
            numParts = buf.getInt(32);
            numPoints = buf.getInt(36);
            parts = new int[numParts];
            for (int i = 0; i < numParts; i++) {
                parts[i] = buf.getInt(40 + i * 4);
            }
            int pos = 40 + numParts * 4;
            points = new double[numPoints][2];
            for (int i = 0; i < numPoints; i++) {
                points[i][0] = buf.getDouble(pos + i * 16); // x value
                points[i][1] = buf.getDouble(pos + i * 16 + 8); // y value
            }
            
            pos += numPoints * 16;
            // m data is optional.
            if (pos < buf.capacity()) {
                mMin = buf.getDouble(pos);
                mMax = buf.getDouble(pos + 8);

                mArray = new double[numPoints];
                pos += 16;
                for (int i = 0; i < numPoints; i++) {
                    mArray[i] = buf.getDouble(pos + i * 8); // m value
                }
                mIncluded = true;
            }
            
            isHole = new boolean[numParts];
            isConvex = new boolean[numParts];
            checkForHoles();
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    
    /**
     * This is the constructor that does not include the optional measure data. Note
     * that the vertices for polygon holes must be entered in a counter-clockwise
     * order as per the ShapeFile specifications.
     * @param parts an int array that indicates the zero-base starting byte for
     * each part.
     * @param points a double[][] array containing the point data. The first
     * dimension of the array is the total number of points in the polygon.
     */
    public PolygonM (int[] parts, double[][] points) {
        numParts = parts.length;
        numPoints = points.length;
        this.parts = (int[])parts.clone();
        this.points = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            this.points[i][0] = points[i][0];
            this.points[i][1] = points[i][1];
        }
        
        double minX = Float.POSITIVE_INFINITY;
        double minY = Float.POSITIVE_INFINITY;
        double maxX = Float.NEGATIVE_INFINITY;
        double maxY = Float.NEGATIVE_INFINITY;
        
        for (int i = 0; i < numPoints; i++) {
            if (points[i][0] < minX) { minX = points[i][0]; }
            if (points[i][0] > maxX) { maxX = points[i][0]; }
            if (points[i][1] < minY) { minY = points[i][1]; }
            if (points[i][1] > maxY) { maxY = points[i][1]; }
        }
        mIncluded = false;
        bb = new BoundingBox(minX, minY, maxX, maxY);
        maxExtent = bb.getMaxExtent();
        isHole = new boolean[numParts];
        isConvex = new boolean[numParts];
        checkForHoles();
    }
    
    /**
     * This is the constructor that does include the optional measure data. Note
     * that the vertices for polygon holes must be entered in a counter-clockwise
     * order as per the ShapeFile specifications.
     * @param parts an int array that indicates the zero-base starting byte for
     * each part.
     * @param points a double[][] array containing the point data. The first
     * dimension of the array is the total number of points in the polygon.
     */
    public PolygonM (int[] parts, double[][] points, double[] mArray) {
        numParts = parts.length;
        numPoints = points.length;
        this.parts = (int[])parts.clone();
        this.points = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            this.points[i][0] = points[i][0];
            this.points[i][1] = points[i][1];
        }
        this.mArray = (double[])mArray.clone();
        
        double minX = Float.POSITIVE_INFINITY;
        double minY = Float.POSITIVE_INFINITY;
        double maxX = Float.NEGATIVE_INFINITY;
        double maxY = Float.NEGATIVE_INFINITY;
        mMin = Float.POSITIVE_INFINITY;
        mMax = Float.NEGATIVE_INFINITY;
        
        for (int i = 0; i < numPoints; i++) {
            if (points[i][0] < minX) { minX = points[i][0]; }
            if (points[i][0] > maxX) { maxX = points[i][0]; }
            if (points[i][1] < minY) { minY = points[i][1]; }
            if (points[i][1] > maxY) { maxY = points[i][1]; }
            if (mArray[i] < mMin) { mMin = mArray[i]; }
            if (mArray[i] > mMax) { mMax = mArray[i]; }
        }
        mIncluded = true;
        bb = new BoundingBox(minX, minY, maxX, maxY);
        maxExtent = bb.getMaxExtent();
        isHole = new boolean[numParts];
        isConvex = new boolean[numParts];
        checkForHoles();
    }
    
    // properties
    public BoundingBox getBox() {
        return bb;
    }
    
    public double getXMin() {
        return bb.getMinX();
    }
    
    public double getYMin() {
        return bb.getMinY();
    }
    
    public double getXMax() {
        return bb.getMaxX();
    }
    
    public double getYMax() {
        return bb.getMaxY();
    }
    
    public int getNumPoints() {
        return numPoints;
    }

    public double[][] getPoints() {
        return points;
    }

    public int getNumParts() {
        return numParts;
    }

    public int[] getParts() {
        return parts;
    }

    public double[] getmArray() {
        return mArray;
    }

    public double getmMax() {
        return mMax;
    }

    public double getmMin() {
        return mMin;
    }
    
    public boolean isMDataIncluded() {
        return mIncluded;
    }
    
    // methods
    private void checkForHoles() {
        // Note: holes are polygons that have verticies in counter-clockwise order
        
        // This approach is based on the method described by Paul Bourke, March 1998
        // http://paulbourke.net/geometry/clockwise/index.html
        
        int stPoint, endPoint, numPointsInPart;
        double x0, y0, x1, y1, x2, y2;
        int n1 = 0, n2 = 0, n3 = 0;
        for (int i = 0; i < numParts; i++) {
            stPoint = parts[i];
            if (i < numParts - 1) {
                // remember, the last point in each part is the same as the first...it's not a legitamate point.
                endPoint = parts[i + 1] - 2;
            } else {
                endPoint = numPoints - 2;
            }
            numPointsInPart = endPoint - stPoint + 1;
            if (numPointsInPart < 3) { return; } // something's wrong! 
            // first see if it is a convex or concave polygon
            // calculate the cros product for each adjacent edge.
            double[] crossproducts = new double[numPointsInPart];
            for (int j = 0; j < numPointsInPart; j++) {
                n2 = stPoint + j;
                if (j == 0) {
                    n1 = stPoint + numPointsInPart - 1;
                    n3 = stPoint + j + 1;
                } else if (j == numPointsInPart - 1) {
                    n1 = stPoint + j - 1;
                    n3 = stPoint ;
                } else {
                    n1 = stPoint + j - 1;
                    n3 = stPoint + j + 1;
                }
                x0 = points[n1][0];
                y0 = points[n1][1];
                x1 = points[n2][0];
                y1 = points[n2][1];
                x2 = points[n3][0];
                y2 = points[n3][1];
                crossproducts[j] = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
            }
            boolean testSign;
            if (crossproducts[0] >= 0) { 
                testSign = true; // positive
            } else { 
                testSign = false; // negative
            }
            isConvex[i] = true;
            for (int j = 1; j < numPointsInPart; j++) {
                if (crossproducts[j] >= 0 && !testSign) { 
                    isConvex[i] = false;
                    break;
                } else if (crossproducts[j] < 0 && testSign) { 
                    isConvex[i] = false;
                    break; 
                }
            }
            
            // now see if it is clockwise or counter-clockwise
            if (isConvex[i]) {
                if (testSign) { // positive means counter-clockwise
                    isHole[i] = true;
                } else {
                    isHole[i] = false;
                }
            } else {
                // calculate the polygon area. If it is positive is is in clockwise order, else counter-clockwise.
                double area = 0;
                for (int j = 0; j < numPointsInPart; j++) {
                    n1 = stPoint + j;
                    if (j < numPointsInPart - 1) {
                        n2 = stPoint + j + 1;
                    } else {
                        n2 = stPoint;
                    }
                    x1 = points[n1][0];
                    y1 = points[n1][1];
                    x2 = points[n2][0];
                    y2 = points[n2][1];
                
                    area += (x1 * y2) - (x2 * y1);
                } 
                // if this were the true area, we'd half it, but we're only interested in the sign.
                if (area < 0) { // a positive area indicates counter-clockwise order
                    isHole[i] = false;
                } else {
                    isHole[i] = true;
                }
            }
        }
    }
    
    public boolean[] getPartHoleData() {
        return isHole;
    }
    
    public boolean isPartAHole(int partNum) {
        if (partNum < 0) { return false; }
        if (partNum >= numParts) { return false; }
        return isHole[partNum];
    }
    
    public int getNumberOfHoles() {
        int ret = 0;
        for (int i = 0; i < numParts; i++) {
            if (isHole[i]) {
                ret++;
            }
        }
        return ret;
    }
    
    public boolean isPartConvex(int partNum) {
        if (partNum < 0) { return false; }
        if (partNum >= numParts) { return false; }
        return isConvex[partNum];
    }
    
    @Override
    public int getLength() {
        if (mIncluded) {
            return 32 + 8 + numParts * 4 + numPoints * 16 + 16 + numPoints * 8;
        } else {
            return 32 + 8 + numParts * 4 + numPoints * 16;
        }
    }
    
    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(getLength());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        // put the bounding box data in.
        buf.putDouble(bb.getMinX());
        buf.putDouble(bb.getMinY());
        buf.putDouble(bb.getMaxX());
        buf.putDouble(bb.getMaxY());
        // put the numParts and numPoints in.
        buf.putInt(numParts);
        buf.putInt(numPoints);
        // put the part data in.
        for (int i = 0; i < numParts; i++) {
            buf.putInt(parts[i]);
        }
        // put the point data in.
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(points[i][0]);
            buf.putDouble(points[i][1]);
        }
        // put the min and max M values in
        if (mIncluded) {
            buf.putDouble(mMin);
            buf.putDouble(mMax);
            // put the m values in.
            for (int i = 0; i < numPoints; i++) {
                buf.putDouble(mArray[i]);
            }
        }
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.POLYGONM;
    }
    
    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.doesIntersect(bb) && maxExtent > minSize) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public com.vividsolutions.jts.geom.Geometry[] getJTSGeometries() {
        GeometryFactory factory = new GeometryFactory();
        int part;
        int j, i, a;
        int startingPointInPart, endingPointInPart;
        int numPointsInPart;
        int numHoles = this.getNumberOfHoles();
        int numNonHoles = numParts - numHoles; // this is the number of shells
        CoordinateArraySequence coordArray;
        ArrayList<com.vividsolutions.jts.geom.Polygon> polyList = new ArrayList<com.vividsolutions.jts.geom.Polygon>();
        
        // read the polygon shells into an array of Geometry
        //com.vividsolutions.jts.geom.Geometry[] shells = new com.vividsolutions.jts.geom.Geometry[numNonHoles];
        LinearRing[] shells = new LinearRing[numNonHoles];
        int shellNum;
        shellNum = 0;
        for (part = 0; part < numParts; part++) {
            if (!isHole[part]) {
                startingPointInPart = parts[part];

                if (part < numParts - 1) {
                    endingPointInPart = parts[part + 1];
                } else {
                    endingPointInPart = numPoints;
                }
                numPointsInPart = endingPointInPart - startingPointInPart;

                coordArray = new CoordinateArraySequence(numPointsInPart);
                j = 0;
                for (i = startingPointInPart; i < endingPointInPart; i++) {
                    coordArray.setOrdinate(j, 0, points[i][0]);
                    coordArray.setOrdinate(j, 1, points[i][1]);
                    coordArray.setOrdinate(j, 2, mArray[i]);
                    j++;
                }
                shells[shellNum] = factory.createLinearRing(coordArray);
                shellNum++;
            }
        }

        for (a = 0; a < numNonHoles; a++) {
            com.vividsolutions.jts.geom.Polygon p = factory.createPolygon(shells[a], new LinearRing[0]);
            // how many holes do each of the shells have?
            ArrayList<LinearRing> holesLR = new ArrayList<LinearRing>();
            for (part = 0; part < numParts; part++) {
                if (isHole[part]) {
                    startingPointInPart = parts[part];

                    if (part < numParts - 1) {
                        endingPointInPart = parts[part + 1];
                    } else {
                        endingPointInPart = numPoints;
                    }
                    numPointsInPart = endingPointInPart - startingPointInPart;

                    coordArray = new CoordinateArraySequence(numPointsInPart);
                    j = 0;
                    for (i = startingPointInPart; i < endingPointInPart; i++) {
                        coordArray.setOrdinate(j, 0, points[i][0]);
                        coordArray.setOrdinate(j, 1, points[i][1]);
                        coordArray.setOrdinate(j, 2, mArray[i]);
                        j++;
                    }
                    com.vividsolutions.jts.geom.Geometry hole = factory.createLineString(coordArray);
                    if (p.contains(hole)) {
                        holesLR.add(factory.createLinearRing(coordArray));
                        break;
                    }
                }
            }
            LinearRing[] holes = new LinearRing[0];
            if (holesLR.size() > 0) {
                holes = new LinearRing[holesLR.size()];
                for (int b = 0; b < holesLR.size(); b++) {
                    holes[b] = holesLR.get(b);
                }
            }
            holesLR.clear();
            p = factory.createPolygon(shells[a], holes);
            polyList.add(p);
        }
        
        com.vividsolutions.jts.geom.Polygon[] polyArray = new com.vividsolutions.jts.geom.Polygon[polyList.size()];
        for (a = 0; a < polyList.size(); a++) {
            polyArray[a] = polyList.get(a);
        }
        
        return polyArray;
    }
}
