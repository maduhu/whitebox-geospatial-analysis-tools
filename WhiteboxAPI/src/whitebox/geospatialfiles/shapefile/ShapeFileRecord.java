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
    private Object data;
    private boolean pointType;

    /**
     * Constructor.
     */
    public ShapeFileRecord() {
    }

    /**
     * Constructor.
     * @param recordNumber
     * @param contentLength
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
                this.data = new Point(rawData);
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

    public Object getData() {
        return data;
    }

    public boolean isPointType() {
        return pointType;
    }
    
}
