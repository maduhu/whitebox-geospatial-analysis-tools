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
    
    //constructors
    public MultiPoint(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            bb = new BoundingBox(buf.getDouble(0), buf.getDouble(8), 
                    buf.getDouble(16), buf.getDouble(24));
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
        if (box.doesIntersect(bb)) {
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
