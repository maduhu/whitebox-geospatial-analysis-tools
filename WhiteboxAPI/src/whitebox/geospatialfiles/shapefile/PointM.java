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
public class PointM implements Geometry {
    private double x;
    private double y;
    private double m;

    //constructors
    public PointM(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            x = buf.getDouble(0);
            y = buf.getDouble(8);
            m = buf.getDouble(16);
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public PointM(double x, double y, double m) {
        this.x = x;
        this.y = y;
        this.m = m;
    }
    
    // properties

    public double getM() {
        return m;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    
    @Override
    public double[][] getPoints() {
        double[][] points = new double[1][2];
        points[0][0] = x;
        points[0][1] = y;
        return points;
    }
    
    @Override
    public int getLength() {
        return 24;
    }
    
    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        buf.putDouble(x);
        buf.putDouble(y);
        buf.putDouble(m);
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.POINTM;
    }
    
    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.isPointInBox(x, y)) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean needsClipping(BoundingBox box) {
        if (box.isPointInBox(x, y)) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public com.vividsolutions.jts.geom.Geometry[] getJTSGeometries() {
        GeometryFactory factory = new GeometryFactory();
        CoordinateArraySequence coordArray = new CoordinateArraySequence(1);
        coordArray.setOrdinate(0, 0, x);
        coordArray.setOrdinate(0, 1, y);
        coordArray.setOrdinate(0, 2, m);
        com.vividsolutions.jts.geom.Point[] retArray = new com.vividsolutions.jts.geom.Point[1];
        retArray[0] = factory.createPoint(coordArray);
        return retArray;
    }
}
