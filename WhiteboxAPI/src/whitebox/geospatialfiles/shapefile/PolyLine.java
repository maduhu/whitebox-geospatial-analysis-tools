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
public class PolyLine {
    //private double[] box = new double[4];
    private int numParts;
    private int numPoints;
    private int[] parts;
    private double[][] points;
    private BoundingBox bb;
    
    //constructors
    public PolyLine(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            
            //box[0] = buf.getDouble(0);
            //box[1] = buf.getDouble(8);
            //box[2] = buf.getDouble(16);
            //box[3] = buf.getDouble(24);
            bb = new BoundingBox(buf.getDouble(0), buf.getDouble(8), 
                    buf.getDouble(16), buf.getDouble(24));
            
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

    public int getNumParts() {
        return numParts;
    }

    public int[] getParts() {
        return parts;
    }
}
