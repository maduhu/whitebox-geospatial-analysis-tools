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
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import whitebox.structures.BoundingBox;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PolyLineZ implements Geometry {
    private BoundingBox bb = new BoundingBox();
    private int numParts;
    private int numPoints;
    private int[] parts;
    private double[][] points;
    private double zMin;
    private double zMax;
    private double[] zArray;
    private double mMin;
    private double mMax;
    private double[] mArray;
    private double maxExtent;
    private boolean mIncluded = false;
    
    //constructors
    
    /**
     * This constructor is used when the PolyLineZ is being created from data
     * that is read directly from a file.
     * @param rawData A byte array containing all of the raw data needed to create
     * the PolyLineZ, starting with the bounding box, i.e. leaving out the 
     * ShapeType data.
     */
    public PolyLineZ(byte[] rawData) {
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
            zMin = buf.getDouble(pos);
            zMax = buf.getDouble(pos + 8);
            
            zArray = new double[numPoints];
            pos += 16;
            for (int i = 0; i < numPoints; i++) {
                zArray[i] = buf.getDouble(pos + i * 8); // z value
            }
            
            pos += numPoints * 8;
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
            
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    /**
     * This is the constructor that does not include the optional measure data.
     * @param parts an int array that indicates the zero-base starting byte for
     * each part.
     * @param points a double[][] array containing the point data. The first
     * dimension of the array is the total number of points in the polyline.
     * @param zArray a double[] array containing the z data.
     */
    public PolyLineZ (int[] parts, double[][] points, double[] zArray) {
        numParts = parts.length;
        numPoints = points.length;
        this.parts = (int[])parts.clone();
        this.points = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            this.points[i][0] = points[i][0];
            this.points[i][1] = points[i][1];
        }
        this.zArray = (double[])zArray.clone();
        
        double minX = Float.POSITIVE_INFINITY;
        double minY = Float.POSITIVE_INFINITY;
        double maxX = Float.NEGATIVE_INFINITY;
        double maxY = Float.NEGATIVE_INFINITY;
        zMin = Float.POSITIVE_INFINITY;
        zMax = Float.NEGATIVE_INFINITY;
        
        for (int i = 0; i < numPoints; i++) {
            if (points[i][0] < minX) { minX = points[i][0]; }
            if (points[i][0] > maxX) { maxX = points[i][0]; }
            if (points[i][1] < minY) { minY = points[i][1]; }
            if (points[i][1] > maxY) { maxY = points[i][1]; }
            if (zArray[i] < zMin) { zMin = zArray[i]; }
            if (zArray[i] > zMax) { zMax = zArray[i]; }
        }
        mIncluded = false;
        bb = new BoundingBox(minX, minY, maxX, maxY);
        maxExtent = bb.getMaxExtent();
    }
    
    /**
     * This is the constructor that does include the optional measure data.
     * @param parts an int array that indicates the zero-base starting byte for
     * each part.
     * @param points a double[][] array containing the point data. The first
     * dimension of the array is the total number of points in the polyline.
     * @param zArray a double[] array containing the z data for each point.
     * @param mArray a double[] array containing the measure data for each point.
     */
    public PolyLineZ (int[] parts, double[][] points, double[] zArray, double[] mArray) {
        numParts = parts.length;
        numPoints = points.length;
        this.parts = (int[])parts.clone();
        this.points = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            this.points[i][0] = points[i][0];
            this.points[i][1] = points[i][1];
        }
        this.zArray = (double[])zArray.clone();
        this.mArray = (double[])mArray.clone();
        
        double minX = Float.POSITIVE_INFINITY;
        double minY = Float.POSITIVE_INFINITY;
        double maxX = Float.NEGATIVE_INFINITY;
        double maxY = Float.NEGATIVE_INFINITY;
        zMin = Float.POSITIVE_INFINITY;
        zMax = Float.NEGATIVE_INFINITY;
        mMin = Float.POSITIVE_INFINITY;
        mMax = Float.NEGATIVE_INFINITY;
        
        for (int i = 0; i < numPoints; i++) {
            if (points[i][0] < minX) { minX = points[i][0]; }
            if (points[i][0] > maxX) { maxX = points[i][0]; }
            if (points[i][1] < minY) { minY = points[i][1]; }
            if (points[i][1] > maxY) { maxY = points[i][1]; }
            if (mArray[i] < zMin) { zMin = mArray[i]; }
            if (mArray[i] > zMax) { zMax = mArray[i]; }
            if (mArray[i] < mMin) { mMin = mArray[i]; }
            if (mArray[i] > mMax) { mMax = mArray[i]; }
        }
        mIncluded = true;
        bb = new BoundingBox(minX, minY, maxX, maxY);
        maxExtent = bb.getMaxExtent();
    }
    
    // properties
    @Override
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

    @Override
    public double[][] getPoints() {
        return points;
    }

    public int getNumParts() {
        return numParts;
    }

    @Override
    public int[] getParts() {
        return parts;
    }

    public double[] getzArray() {
        return zArray;
    }

    public double getzMax() {
        return zMax;
    }

    public double getzMin() {
        return zMin;
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
    
    @Override
    public int getLength() {
        if (mIncluded) {
            return 32 + 8 + numParts * 4 + numPoints * 16 + 16 + numPoints * 8
                + 16 + numPoints * 8;
        } else {
            return 32 + 8 + numParts * 4 + numPoints * 16 + 16 + numPoints * 8;
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
        // put the min and max z values in
        buf.putDouble(zMin);
        buf.putDouble(zMax);
        // put the z values in
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(zArray[i]);
        }
        if (mIncluded) {
            // put the min and max M values in
            buf.putDouble(mMin);
            buf.putDouble(mMax);
            // put the m values in
            for (int i = 0; i < numPoints; i++) {
                buf.putDouble(mArray[i]);
            }
        }
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.POLYLINEZ;
    }
    
    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.overlaps(bb) && maxExtent > minSize) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean needsClipping(BoundingBox box) {
        if ((!bb.entirelyContainedWithin(box)) && (bb.overlaps(box))) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public com.vividsolutions.jts.geom.Geometry[] getJTSGeometries() {
        GeometryFactory factory = new GeometryFactory();
        int part;
        int j, i;
        int startingPointInPart, endingPointInPart;
        int numPointsInPart;
        CoordinateArraySequence coordArray;
        com.vividsolutions.jts.geom.LineString[] polyArray = new com.vividsolutions.jts.geom.LineString[numParts];
        
        for (part = 0; part < numParts; part++) {
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
                coordArray.setOrdinate(j, 2, zArray[i]);
                j++;
            }
            polyArray[part] = factory.createLineString(coordArray);
        }
        
        
        return polyArray;
    }
}
