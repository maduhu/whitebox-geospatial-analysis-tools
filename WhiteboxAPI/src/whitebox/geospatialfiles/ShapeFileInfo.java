/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

package whitebox.geospatialfiles;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.utilities.ByteSwapper;

/**
 * This is a light-weight class used to simply retrieve information about a shapefile
 * e.g. it's shape type (point, line, polygon) or dimension (z, m). It cannot be
 * used to read individual records. For that, use the ShapeFile class.
 * @author johnlindsay
 */
public class ShapeFileInfo {
    
    private String fileName;
    private String indexFile;
    private String databaseFile;
    private String projectionFile;
    private int fileCode;
    private int fileLength;
    private int version;
    private ShapeType shapeType;
    private double xMin;
    private double yMin;
    private double xMax;
    private double yMax;
    private double zMin;
    private double zMax;
    private double mMin;
    private double mMax;
    private int numRecs;
    private boolean pointType;
    
    public ShapeFileInfo(String fileName) {
        this.fileName = fileName;
        readHeaderData();
    }
    
    private boolean readHeaderData() {
        RandomAccessFile rIn = null;
        ByteBuffer buf;

        try {
            // See if the data file exists.
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }

            buf = ByteBuffer.allocate(100);

            rIn = new RandomAccessFile(fileName, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            fileCode = ByteSwapper.swap(buf.getInt(0));
            fileLength = ByteSwapper.swap(buf.getInt(24)); // in 16-bit words.
            version = buf.getShort(28);
            shapeType = getShapeTypeFromInt(buf.getShort(32));

            if (shapeType == ShapeType.POINT) {
                this.pointType = true;
            } else if (shapeType == ShapeType.MULTIPOINT) {
                this.pointType = true;
            } else if (shapeType == ShapeType.POINTZ) {
                this.pointType = true;
            } else if (shapeType == ShapeType.POINTM) {
                this.pointType = true;
            } else if (shapeType == ShapeType.MULTIPOINTM) {
                this.pointType = true;
            } else if (shapeType == ShapeType.MULTIPOINTZ) {
                this.pointType = true;
            } else {
                pointType = false;
            }

            xMin = buf.getDouble(36);
            yMin = buf.getDouble(44);
            xMax = buf.getDouble(52);
            yMax = buf.getDouble(60);
            zMin = buf.getDouble(68);
            zMax = buf.getDouble(76);
            mMin = buf.getDouble(84);
            mMax = buf.getDouble(92);

        } catch (Exception e) {
            return false;
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
            return true;
        }
    }
    
    private static ShapeType[] st = new ShapeType[]{ShapeType.NULLSHAPE, ShapeType.POINT,
        ShapeType.UNUSED1, ShapeType.POLYLINE, ShapeType.UNUSED2, ShapeType.POLYGON,
        ShapeType.UNUSED3, ShapeType.UNUSED4, ShapeType.MULTIPOINT,
        ShapeType.UNUSED5, ShapeType.UNUSED6, ShapeType.POINTZ, ShapeType.UNUSED7,
        ShapeType.POLYLINEZ, ShapeType.UNUSED8, ShapeType.POLYGONZ, ShapeType.UNUSED9,
        ShapeType.UNUSED10, ShapeType.MULTIPOINTZ, ShapeType.UNUSED11,
        ShapeType.UNUSED12, ShapeType.POINTM, ShapeType.UNUSED13, ShapeType.POLYLINEM,
        ShapeType.UNUSED14, ShapeType.POLYGONM, ShapeType.UNUSED15, ShapeType.UNUSED16,
        ShapeType.MULTIPOINTM, ShapeType.UNUSED17, ShapeType.UNUSED18, ShapeType.MULTIPATCH};

    private ShapeType getShapeTypeFromInt(int i) {
        return st[i];
    }
    
    public int getFileCode() {
        return fileCode;
    }

    public int getFileLength() {
        return fileLength;
    }

    public double getmMax() {
        return mMax;
    }

    public double getmMin() {
        return mMin;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public int getVersion() {
        return version;
    }

    public double getxMax() {
        return xMax;
    }

    public double getxMin() {
        return xMin;
    }

    public double getyMax() {
        return yMax;
    }

    public double getyMin() {
        return yMin;
    }

    public double getzMax() {
        return zMax;
    }

    public double getzMin() {
        return zMin;
    }

    public int getNumberOfRecords() {
        return numRecs;
    }

    public boolean isPointType() {
        return pointType;
    }
}
