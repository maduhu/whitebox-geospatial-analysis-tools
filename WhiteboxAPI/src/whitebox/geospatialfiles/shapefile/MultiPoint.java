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
public class MultiPoint implements Geometry {
   //private double[] box = new double[4];
    private BoundingBox bb;
    private int numPoints;
    private double[][] points;
    private double maxExtent;
    
    //constructors
    public MultiPoint(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            bb = new BoundingBox(buf.getDouble(0), buf.getDouble(8), 
                    buf.getDouble(16), buf.getDouble(24));
            maxExtent = bb.getMaxExtent();
            numPoints = buf.getInt(32);
            points = new double[numPoints][2];
            for (int i = 0; i < numPoints; i++) {
                points[i][0] = buf.getDouble(36 + i * 16); // x value
                points[i][1] = buf.getDouble(36 + i * 16 + 8); // y value
            }
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    /**
     * This is the constructor that is used when creating a new multipoint.
     * @param points a double[][] array containing the point data. The first
     * dimension of the array is the total number of points in the multipoint.
     */
    public MultiPoint (double[][] points) {
        numPoints = points.length;
        this.points = new double[numPoints][2];
        double minX = Float.POSITIVE_INFINITY;
        double minY = Float.POSITIVE_INFINITY;
        double maxX = Float.NEGATIVE_INFINITY;
        double maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numPoints; i++) {
            this.points[i][0] = points[i][0];
            this.points[i][1] = points[i][1];
            
            if (points[i][0] < minX) { minX = points[i][0]; }
            if (points[i][0] > maxX) { maxX = points[i][0]; }
            if (points[i][1] < minY) { minY = points[i][1]; }
            if (points[i][1] > maxY) { maxY = points[i][1]; }
        }
        
        bb = new BoundingBox(minX, minY, maxX, maxY);
        maxExtent = bb.getMaxExtent();
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

    @Override
    public double[][] getPoints() {
        return points;
    }
    
    @Override
    public int[] getParts() {
        return new int[0];
    }
    
    @Override
    public int getLength() {
        return 32 + 4 + numPoints * 16;
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
        // put the numPoints in.
        buf.putInt(numPoints);
        // put the point data in.
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(points[i][0]);
            buf.putDouble(points[i][1]);
        }
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.MULTIPOINT;
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
        int a;
        CoordinateArraySequence coordArray;
        com.vividsolutions.jts.geom.Point[] pointArray = new com.vividsolutions.jts.geom.Point[numPoints];
        
        for (a = 0; a < numPoints; a++) {
            coordArray = new CoordinateArraySequence(1);
            coordArray.setOrdinate(0, 0, points[a][0]);
            coordArray.setOrdinate(0, 1, points[a][1]);
            pointArray[a] = factory.createPoint(coordArray);
        }
        
        return pointArray;
    }
}
