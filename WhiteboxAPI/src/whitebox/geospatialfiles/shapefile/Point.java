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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Point implements Geometry {
    private double x;
    private double y;
    
    //constructors
    public Point(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            x = buf.getDouble(0);
            y = buf.getDouble(8);
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    // properties
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    
    @Override
    public int getLength() {
        return 16;
    }
    
    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        buf.putDouble(x);
        buf.putDouble(y);
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.POINT;
    }

    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.isPointInBox(x, y)) {
            return true;
        } else {
            return false;
        }
    }
}
