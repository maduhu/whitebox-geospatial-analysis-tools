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
public class MultiPatch implements Geometry {
   //private double[] box = new double[4];
    private BoundingBox bb;
    private int numParts;
    private int numPoints;
    private int[] parts;
    private double[][] points;
    private double mMin;
    private double mMax;
    private double[] mArray;
    private double zMin;
    private double zMax;
    private double[] zArray;
    private PartType[] partTypes;
    private double maxExtent;
    
    public MultiPatch(byte[] rawData) {
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
            partTypes = new PartType[numParts];
            for (int i = 0; i < numParts; i++) {
                partTypes[i] = getPartTypeFromInt(buf.getInt(40 + i * 4));
            }
            
            pos += numParts * 4;
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
            mMin = buf.getDouble(pos);
            mMax = buf.getDouble(pos + 8);
            
            mArray = new double[numPoints];
            pos += 16;
            for (int i = 0; i < numPoints; i++) {
                mArray[i] = buf.getDouble(pos + i * 8); // m value
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

    public PartType[] getPartTypes() {
        return partTypes;
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
    
    // methods
    private PartType[] pt = new PartType[]{PartType.TRIANGLE_STRIP, PartType.TRIANGLE_FAN,
      PartType.OUTER_RING, PartType.INNER_RING, PartType.FIRST_RING, PartType.RING};
    private PartType getPartTypeFromInt(int i) {
        return pt[i];
    }
    
    private int getIntFromPartType(PartType pt) {
        switch (pt) {
            case TRIANGLE_STRIP:
                return 0;
            case TRIANGLE_FAN:
                return 1;
            case OUTER_RING:
                return 2;
            case INNER_RING:
                return 3;
            case FIRST_RING:
                return 4;
            case RING:
                return 5;
        }
        return 0; // it should never get here.
    }
    
    @Override
    public int getLength() {
        return 32 + 8 + numParts * 4 + numPoints * 16 + 16 + numPoints * 8
                + 16 + numPoints * 8;
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
        // put in the part types.
        for (int i = 0; i < numParts; i++) {
            buf.putInt(getIntFromPartType(partTypes[i]));
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
        // put the min and max M values in
        buf.putDouble(mMin);
        buf.putDouble(mMax);
        // put the m values in
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(mArray[i]);
        }
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.MULTIPATCH;
    }
    
    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.doesIntersect(bb) && maxExtent > minSize) {
            return true;
        } else {
            return false;
        }
    }
}
