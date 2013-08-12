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
import whitebox.utilities.ByteSwapper;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ShapeFileRecord {

    private int recordNumber;
    private int contentLength;
    private ShapeType shapeType; // notice that the shape type is not officially part of the
    // record header, but that it is the starting part of each shapeType and as such
    // is effectively a component of the header.
    private Geometry data;
    private boolean pointType;

    /**
     * Constructors.
     */
    public ShapeFileRecord() {
    }

    /**
     * Constructor.
     * @param recordNumber
     * @param contentLength content length in 16-bit words.
     * @param shapeType
     * @param data 
     */
    public ShapeFileRecord(int recordNumber, int contentLength, ShapeType shapeType, byte[] rawData) {
        this.recordNumber = recordNumber;
        this.contentLength = contentLength;
        this.shapeType = shapeType;
        this.pointType = false;
        switch (shapeType) {
            case POINT:
                this.data = new whitebox.geospatialfiles.shapefile.Point(rawData);
                this.pointType = true;
                break;
            case MULTIPOINT:
                this.data = new MultiPoint(rawData);
                this.pointType = true;
                break;
            case POLYLINE:
                this.data = new PolyLine(rawData);
                break;
            case POINTZ:
                this.data = new PointZ(rawData);
                this.pointType = true;
                break;
            case POINTM:
                this.data = new PointM(rawData);
                this.pointType = true;
                break;
            case MULTIPOINTM:
                this.data = new MultiPointM(rawData);
                this.pointType = true;
                break;
            case POLYLINEM:
                this.data = new PolyLineM(rawData);
                break;
            case MULTIPOINTZ:
                this.data = new MultiPointZ(rawData);
                this.pointType = true;
                break;
            case POLYLINEZ:
                this.data = new PolyLineZ(rawData);
                break;
            case POLYGON:
                this.data = new Polygon(rawData);
                break;
            case POLYGONM:
                this.data = new PolygonM(rawData);
                break;
            case POLYGONZ:
                this.data = new PolygonZ(rawData);
                break;
            case MULTIPATCH:
                this.data = new MultiPatch(rawData);
                break;
            case NULLSHAPE:
                this.data = null;
                break;
            default:
                System.err.println("Shape type not recognized.");
                break;
        }
    }
    
    public ShapeFileRecord(int recordNumber, int contentLength, ShapeType shapeType, Geometry geom) {
        this.recordNumber = recordNumber;
        this.contentLength = contentLength;
        this.shapeType = shapeType;
        this.data = geom;
        switch (shapeType) {
            case POINT:
            case MULTIPOINT:
            case POINTZ:
            case POINTM:
            case MULTIPOINTM:
            case MULTIPOINTZ:
                this.pointType = true;
                break;
            case POLYLINE:
            case POLYLINEZ:
            case POLYLINEM:
            case POLYGON:
            case POLYGONM:
            case POLYGONZ:
            case MULTIPATCH:
            case NULLSHAPE:
                this.pointType = false;
                break;
        }
    }

    // properties
    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(int recordNumber) {
        this.recordNumber = recordNumber;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    public Geometry getGeometry() {
        return data;
    }

    public boolean isPointType() {
        return pointType;
    }
    
    // methods
    
    /**
     * This is used by the ShapeFile class to get convert the current data 
     * contained in this object into a bytebuffer than can then be written to
     * disc.
     * @return A ByteBuffer representation of this object. 
     */
    public byte[] toBytes() {
        ByteBuffer geometryByteBuffer = data.toByteBuffer();
        geometryByteBuffer.rewind();
        int size = 12 + geometryByteBuffer.capacity();
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        buf.putInt(ByteSwapper.swap(recordNumber));
        buf.putInt(ByteSwapper.swap(contentLength));
        buf.putInt(getIntFromShapeType(shapeType));
        byte[] bytes = geometryByteBuffer.array();
        for (int i = 0; i < bytes.length; i++) {
            buf.put(bytes[i]); //i + 12, 
        }
        //buf.put(bytes);
        return buf.array();
    }
    
    public int getLength() {
        return 12 + data.getLength(); // 12 is the size of the recordNumber, 
                                      // contentLength, and shapeType.
    }
    
    private int getIntFromShapeType(ShapeType st) {
        switch (st) {
            case NULLSHAPE:
                return 0;
            case POINT:
                return 1;
            case POLYLINE:
                return 3;
            case POLYGON:
                return 5;
            case MULTIPOINT:
                return 8;
            case POINTZ:
                return 11;
            case POLYLINEZ:
                return 13;
            case POLYGONZ:
                return 15;
            case MULTIPOINTZ:
                return 18;
            case POINTM:
                return 21;
            case POLYLINEM:
                return 23;
            case POLYGONM:
                return 25;
            case MULTIPOINTM:
                return 28;
            case MULTIPATCH:
                return 31;
        }
        return -1; // it should never reach here.
    }
    
//    public int[] getPartDataFromRecord() {
//        
//    }
//    
//    public double[][] getXYGeometryFromRecord() {
//        if (data == null) { return null; }
//        double[][] ret;
//        ShapeType shapeType = this.getShapeType();
//        switch (shapeType) {
//            case POLYLINE:
//                whitebox.geospatialfiles.shapefile.PolyLine recPolyLine =
//                        (whitebox.geospatialfiles.shapefile.PolyLine) (data);
//                ret = recPolyLine.getPoints();
//                partData = recPolyLine.getParts();
//                break;
//            case POLYLINEZ:
//                PolyLineZ recPolyLineZ = (PolyLineZ) (data);
//                ret = recPolyLineZ.getPoints();
//                partData = recPolyLineZ.getParts();
//                break;
//            case POLYLINEM:
//                PolyLineM recPolyLineM = (PolyLineM) (data);
//                ret = recPolyLineM.getPoints();
//                partData = recPolyLineM.getParts();
//                break;
//            case POLYGON:
//                Polygon recPolygon = (Polygon) (data);
//                ret = recPolygon.getPoints();
//                partData = recPolygon.getParts();
//                break;
//            case POLYGONZ:
//                PolygonZ recPolygonZ = (PolygonZ) (data);
//                ret = recPolygonZ.getPoints();
//                partData = recPolygonZ.getParts();
//                break;
//            case POLYGONM:
//                PolygonM recPolygonM = (PolygonM) (data);
//                ret = recPolygonM.getPoints();
//                partData = recPolygonM.getParts();
//                break;
//            default:
//                ret = new double[1][2];
//                ret[1][0] = -1;
//                ret[1][1] = -1;
//                break;
//        }
//
//        return ret;
//    }
}
