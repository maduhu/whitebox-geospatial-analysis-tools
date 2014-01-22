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
package whitebox.geospatialfiles;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.*;
import static whitebox.geospatialfiles.shapefile.ShapeType.*;
import whitebox.structures.BoundingBox;
import whitebox.utilities.ByteSwapper;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.utilities.StringUtilities;
import whitebox.structures.KdTree;
import whitebox.structures.KdTree.Entry;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ShapeFile {

    private String fileName;
    private String shortFileName;
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
    private String projection = "";
    private String zUnits = "";
    private String xyUnits = "";
    private String spheroid = "";
    private String xShift = "";
    private String yShift = "";
    private double[] parameters;
    public boolean databaseFileExists;
    public ArrayList<ShapeFileRecord> records = new ArrayList<>();
    private boolean pointType;
    private AttributeTable attributeTable = null;

    // Constructors
    public ShapeFile() {
    }

    public ShapeFile(String fileName) throws IOException {
        setFileName(fileName);
        this.indexFile = StringUtilities.replaceLast(fileName, ".shp", ".shx");
        setProjectionFile(StringUtilities.replaceLast(fileName, ".shp", ".prj"));
        setDatabaseFile(StringUtilities.replaceLast(fileName, ".shp", ".dbf"));
//        int extensionIndex = fileName.lastIndexOf(".");
//        this.indexFile = fileName.substring(0, extensionIndex) + ".shx";
//        setProjectionFile(fileName.substring(0, extensionIndex) + ".prj");
//        setDatabaseFile(fileName.substring(0, extensionIndex) + ".dbf");
        // see if the databaseFile exists.
        databaseFileExists = (new File(databaseFile)).exists();

        if (databaseFileExists) {
            this.attributeTable = new AttributeTable(databaseFile);
        }
    }

    public ShapeFile(String fileName, ShapeType st) {
        this.fileName = fileName;
        int extensionIndex = fileName.lastIndexOf(".");
        this.indexFile = fileName.substring(0, extensionIndex) + ".shx";
        this.projectionFile = fileName.substring(0, extensionIndex) + ".prj";
        this.databaseFile = fileName.substring(0, extensionIndex) + ".dbf";
        this.fileCode = 9994;
        this.version = 1000;
        this.shapeType = st;
        this.xMin = Float.POSITIVE_INFINITY;
        this.yMin = Float.POSITIVE_INFINITY;
        this.xMax = Float.NEGATIVE_INFINITY;
        this.yMax = Float.NEGATIVE_INFINITY;
        this.zMin = Float.POSITIVE_INFINITY;
        this.zMax = Float.NEGATIVE_INFINITY;
        this.mMin = Float.POSITIVE_INFINITY;
        this.mMax = Float.NEGATIVE_INFINITY;
        this.numRecs = 0;
        deleteFiles();
    }

    public ShapeFile(String fileName, ShapeType st, DBFField[] fields) {
        this.fileName = fileName;
        int extensionIndex = fileName.lastIndexOf(".");
        this.indexFile = fileName.substring(0, extensionIndex) + ".shx";
        this.projectionFile = fileName.substring(0, extensionIndex) + ".prj";
        this.databaseFile = fileName.substring(0, extensionIndex) + ".dbf";
        this.fileCode = 9994;
        this.version = 1000;
        this.shapeType = st;
        this.xMin = Float.POSITIVE_INFINITY;
        this.yMin = Float.POSITIVE_INFINITY;
        this.xMax = Float.NEGATIVE_INFINITY;
        this.yMax = Float.NEGATIVE_INFINITY;
        this.zMin = Float.POSITIVE_INFINITY;
        this.zMax = Float.NEGATIVE_INFINITY;
        this.mMin = Float.POSITIVE_INFINITY;
        this.mMax = Float.NEGATIVE_INFINITY;
        this.numRecs = 0;
        deleteFiles();
        try {
            this.attributeTable = new AttributeTable(databaseFile, fields, false);
        } catch (Exception e) {
            // do nothing
        }
    }

    // Properties
    public String getFileName() {
        return fileName;
    }

    public String getShortName() {
        if (shortFileName == null) {
            File file = new File(fileName);
            shortFileName = file.getName();
            shortFileName = shortFileName.replace(".shp", "");
        }
        return shortFileName;
    }

    public final void setFileName(String fileName) {
        this.fileName = fileName;
        // See if the data file exists.
        File file = new File(fileName);
        if (file.exists()) { // it's an existing file
            readHeaderData();
            readRecords();
        } else { // it's a new file
        }
    }

    public String getProjectionFile() {
        return projectionFile;
    }

    public final void setProjectionFile(String projectionFile) {
        this.projectionFile = projectionFile;
        readProjectionFile();
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    public final void setDatabaseFile(String databaseFile) {
        this.databaseFile = databaseFile;
    }
    
    public String getIndexFile() {
        return indexFile;
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

    public int getFileCode() {
        return fileCode;
    }

    public void setFileCode(int fileCode) {
        this.fileCode = fileCode;
    }

    public int getFileLength() {
        return fileLength;
    }

    public void setFileLength(int fileLength) {
        this.fileLength = fileLength;
    }

    public double getmMax() {
        return mMax;
    }

    public void setmMax(double mMax) {
        this.mMax = mMax;
    }

    public double getmMin() {
        return mMin;
    }

    public void setmMin(double mMin) {
        this.mMin = mMin;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public double getxMax() {
        return xMax;
    }

    public void setxMax(double xMax) {
        this.xMax = xMax;
    }

    public double getxMin() {
        return xMin;
    }

    public void setxMin(double xMin) {
        this.xMin = xMin;
    }

    public double getyMax() {
        return yMax;
    }

    public void setyMax(double yMax) {
        this.yMax = yMax;
    }

    public double getyMin() {
        return yMin;
    }

    public void setyMin(double yMin) {
        this.yMin = yMin;
    }

    public double getzMax() {
        return zMax;
    }

    public void setzMax(double zMax) {
        this.zMax = zMax;
    }

    public double getzMin() {
        return zMin;
    }

    public void setzMin(double zMin) {
        this.zMin = zMin;
    }

    public int getNumberOfRecords() {
        return numRecs;
    }

    public String getXYUnits() {
        return xyUnits;
    }

    public void setXYUnits(String xyUnits) {
        this.xyUnits = xyUnits;
    }

    public boolean isPointType() {
        return pointType;
    }

    // Methods
    public final boolean deleteFiles() {
        try {
            File file = new File(fileName);
            file.delete();
            file = new File(databaseFile);
            file.delete();
            file = new File(indexFile);
            file.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
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

    public boolean write() throws IOException {
        ByteBuffer buf;

        try {
            OutputStream output = null;
            try {
                // what is the size of the file?
                int size = 100; // initialized to the size of the file header
                for (ShapeFileRecord sfr : records) {
                    size += sfr.getLength();
                }
                fileLength = size / 2; // in 16-bit words

                buf = ByteBuffer.allocate(size);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.rewind();

                // place the file header data into the buffer then output it
                buf.putInt(0, ByteSwapper.swap(fileCode));
                buf.putInt(24, ByteSwapper.swap(fileLength));
                buf.putInt(28, version);
                buf.putInt(32, getIntFromShapeType(shapeType));
                buf.putDouble(36, xMin);
                buf.putDouble(44, yMin);
                buf.putDouble(52, xMax);
                buf.putDouble(60, yMax);
                buf.putDouble(68, zMin);
                buf.putDouble(76, zMax);
                buf.putDouble(84, mMin);
                buf.putDouble(92, mMax);

                int pos = 100;
                byte[] bytes;
                int[] indexData = new int[numRecs];
                int a = 0;
                for (ShapeFileRecord sfr : records) {
                    bytes = sfr.toBytes();
                    for (int i = 0; i < bytes.length; i++) {
                        buf.put(i + pos, bytes[i]); //i + pos, 
                    }
                    indexData[a] = pos / 2;
                    a++;
                    pos += sfr.getLength();
                }

                output = new BufferedOutputStream(new FileOutputStream(fileName));
                output.write(buf.array());
                output.close();

                // now save the index file
                size = 100 + 8 * numRecs;
                buf = ByteBuffer.allocate(size);
                buf.order(ByteOrder.BIG_ENDIAN);
                buf.rewind();

                buf.putInt(0, fileCode);
                buf.putInt(24, size / 2);
                buf.putInt(28, ByteSwapper.swap(version));
                buf.putInt(32, ByteSwapper.swap(getIntFromShapeType(shapeType)));
                buf.putDouble(36, ByteSwapper.swap(xMin));
                buf.putDouble(44, ByteSwapper.swap(yMin));
                buf.putDouble(52, ByteSwapper.swap(xMax));
                buf.putDouble(60, ByteSwapper.swap(yMax));
                buf.putDouble(68, ByteSwapper.swap(zMin));
                buf.putDouble(76, ByteSwapper.swap(zMax));
                buf.putDouble(84, ByteSwapper.swap(mMin));
                buf.putDouble(92, ByteSwapper.swap(mMax));

                pos = 100;
                a = 0;
                for (ShapeFileRecord sfr : records) {
                    buf.putInt(pos, indexData[a]);
                    a++;
                    buf.putInt(pos + 4, sfr.getContentLength());
                    pos += 8;
                }
                output = new BufferedOutputStream(new FileOutputStream(indexFile));
                output.write(buf.array());

                if (this.attributeTable != null) {
                    this.attributeTable.write();

                }
            } finally {
                output.close();
            }
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public boolean addRecord(Geometry recordGeometry) {
        if (recordGeometry.getShapeType() == shapeType) {
            numRecs++;
            int contentLength = (4 + recordGeometry.getLength()) / 2;
            ShapeFileRecord sfr = new ShapeFileRecord(numRecs, contentLength,
                    shapeType, recordGeometry);
            records.add(sfr);

            // update the min and max coordinates
            double recXMin = 0, recYMin = 0, recXMax = 0, recYMax = 0,
                    recZMin = 0, recZMax = 0, recMMin = 0, recMMax = 0;

            switch (shapeType) {
                case POINT:
                    Point recPoint = (Point) recordGeometry;
                    recXMin = recPoint.getX();
                    recYMin = recPoint.getY();
                    recXMax = recXMin;
                    recYMax = recYMin;
                    break;
                case MULTIPOINT:
                    MultiPoint recMultiPoint = (MultiPoint) recordGeometry;
                    recXMin = recMultiPoint.getXMin();
                    recYMin = recMultiPoint.getYMin();
                    recXMax = recMultiPoint.getXMax();
                    recYMax = recMultiPoint.getYMax();
                    break;
                case POLYLINE:
                    PolyLine recPolyLine = (PolyLine) recordGeometry;
                    recXMin = recPolyLine.getXMin();
                    recYMin = recPolyLine.getYMin();
                    recXMax = recPolyLine.getXMax();
                    recYMax = recPolyLine.getYMax();
                    break;
                case POINTZ:
                    PointZ recPointZ = (PointZ) recordGeometry;
                    recXMin = recPointZ.getX();
                    recYMin = recPointZ.getY();
                    recXMax = recXMin;
                    recYMax = recYMin;
                    recZMin = recPointZ.getZ();
                    recZMax = recPointZ.getZ();
                    recMMin = recPointZ.getM();
                    recMMax = recPointZ.getM();
                    break;
                case POINTM:
                    PointM recPointM = (PointM) recordGeometry;
                    recXMin = recPointM.getX();
                    recYMin = recPointM.getY();
                    recXMax = recXMin;
                    recYMax = recYMin;
                    recMMin = recPointM.getM();
                    recMMax = recPointM.getM();
                    break;
                case MULTIPOINTM:
                    MultiPointM recMultiPointM = (MultiPointM) recordGeometry;
                    recXMin = recMultiPointM.getXMin();
                    recYMin = recMultiPointM.getYMin();
                    recXMax = recMultiPointM.getXMax();
                    recYMax = recMultiPointM.getYMax();
                    recMMin = recMultiPointM.getmMin();
                    recMMax = recMultiPointM.getmMax();
                    break;
                case POLYLINEM:
                    PolyLineM recPolyLineM = (PolyLineM) recordGeometry;
                    recXMin = recPolyLineM.getXMin();
                    recYMin = recPolyLineM.getYMin();
                    recXMax = recPolyLineM.getXMax();
                    recYMax = recPolyLineM.getYMax();
                    recMMin = recPolyLineM.getmMin();
                    recMMax = recPolyLineM.getmMax();
                    break;
                case MULTIPOINTZ:
                    MultiPointZ recMultiPointZ = (MultiPointZ) recordGeometry;
                    recXMin = recMultiPointZ.getXMin();
                    recYMin = recMultiPointZ.getYMin();
                    recXMax = recMultiPointZ.getXMax();
                    recYMax = recMultiPointZ.getYMax();
                    recZMin = recMultiPointZ.getzMin();
                    recZMax = recMultiPointZ.getzMin();
                    recMMin = recMultiPointZ.getmMin();
                    recMMax = recMultiPointZ.getmMax();
                    break;
                case POLYLINEZ:
                    PolyLineZ recPolyLineZ = (PolyLineZ) recordGeometry;
                    recXMin = recPolyLineZ.getXMin();
                    recYMin = recPolyLineZ.getYMin();
                    recXMax = recPolyLineZ.getXMax();
                    recYMax = recPolyLineZ.getYMax();
                    recZMin = recPolyLineZ.getzMin();
                    recZMax = recPolyLineZ.getzMax();
                    recMMin = recPolyLineZ.getmMin();
                    recMMax = recPolyLineZ.getmMax();
                    break;
                case POLYGON:
                    Polygon recPolygon = (Polygon) recordGeometry;
                    recXMin = recPolygon.getXMin();
                    recYMin = recPolygon.getYMin();
                    recXMax = recPolygon.getXMax();
                    recYMax = recPolygon.getYMax();
                    break;
                case POLYGONM:
                    PolygonM recPolygonM = (PolygonM) recordGeometry;
                    recXMin = recPolygonM.getXMin();
                    recYMin = recPolygonM.getYMin();
                    recXMax = recPolygonM.getXMax();
                    recYMax = recPolygonM.getYMax();
                    recMMin = recPolygonM.getmMin();
                    recMMax = recPolygonM.getmMax();
                    break;
                case POLYGONZ:
                    PolygonZ recPolygonZ = (PolygonZ) recordGeometry;
                    recXMin = recPolygonZ.getXMin();
                    recYMin = recPolygonZ.getYMin();
                    recXMax = recPolygonZ.getXMax();
                    recYMax = recPolygonZ.getYMax();
                    recZMin = recPolygonZ.getzMin();
                    recZMax = recPolygonZ.getzMax();
                    recMMin = recPolygonZ.getmMin();
                    recMMax = recPolygonZ.getmMax();
                    break;
                case MULTIPATCH:
                    MultiPatch recMultiPatch = (MultiPatch) recordGeometry;
                    recXMin = recMultiPatch.getXMin();
                    recYMin = recMultiPatch.getYMin();
                    recXMax = recMultiPatch.getXMax();
                    recYMax = recMultiPatch.getYMax();
                    recZMin = recMultiPatch.getzMin();
                    recZMax = recMultiPatch.getzMax();
                    recMMin = recMultiPatch.getmMin();
                    recMMax = recMultiPatch.getmMax();
                    break;
            }

            if (recXMin < xMin) {
                xMin = recXMin;
            }
            if (recYMin < yMin) {
                yMin = recYMin;
            }
            if (recXMax > xMax) {
                xMax = recXMax;
            }
            if (recYMax > yMax) {
                yMax = recYMax;
            }
            if (recZMin < zMin) {
                zMin = recZMin;
            }
            if (recZMax > zMax) {
                zMax = recZMax;
            }
            if (recMMin < mMin) {
                mMin = recMMin;
            }
            if (recMMax > mMax) {
                mMax = recMMax;
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean addRecord(Geometry recordGeometry, Object[] rowData) {
        if (recordGeometry.getShapeType() == shapeType) {
            numRecs++;
            int contentLength = (4 + recordGeometry.getLength()) / 2;
            ShapeFileRecord sfr = new ShapeFileRecord(numRecs, contentLength,
                    shapeType, recordGeometry);
            records.add(sfr);

            // update the min and max coordinates
            double recXMin = 0, recYMin = 0, recXMax = 0, recYMax = 0,
                    recZMin = 0, recZMax = 0, recMMin = 0, recMMax = 0;

            switch (shapeType) {
                case POINT:
                    Point recPoint = (Point) recordGeometry;
                    recXMin = recPoint.getX();
                    recYMin = recPoint.getY();
                    recXMax = recXMin;
                    recYMax = recYMin;
                    break;
                case MULTIPOINT:
                    MultiPoint recMultiPoint = (MultiPoint) recordGeometry;
                    recXMin = recMultiPoint.getXMin();
                    recYMin = recMultiPoint.getYMin();
                    recXMax = recMultiPoint.getXMax();
                    recYMax = recMultiPoint.getYMax();
                    break;
                case POLYLINE:
                    PolyLine recPolyLine = (PolyLine) recordGeometry;
                    recXMin = recPolyLine.getXMin();
                    recYMin = recPolyLine.getYMin();
                    recXMax = recPolyLine.getXMax();
                    recYMax = recPolyLine.getYMax();
                    break;
                case POINTZ:
                    PointZ recPointZ = (PointZ) recordGeometry;
                    recXMin = recPointZ.getX();
                    recYMin = recPointZ.getY();
                    recXMax = recXMin;
                    recYMax = recYMin;
                    recZMin = recPointZ.getZ();
                    recZMax = recPointZ.getZ();
                    recMMin = recPointZ.getM();
                    recMMax = recPointZ.getM();
                    break;
                case POINTM:
                    PointM recPointM = (PointM) recordGeometry;
                    recXMin = recPointM.getX();
                    recYMin = recPointM.getY();
                    recXMax = recXMin;
                    recYMax = recYMin;
                    recMMin = recPointM.getM();
                    recMMax = recPointM.getM();
                    break;
                case MULTIPOINTM:
                    MultiPointM recMultiPointM = (MultiPointM) recordGeometry;
                    recXMin = recMultiPointM.getXMin();
                    recYMin = recMultiPointM.getYMin();
                    recXMax = recMultiPointM.getXMax();
                    recYMax = recMultiPointM.getYMax();
                    recMMin = recMultiPointM.getmMin();
                    recMMax = recMultiPointM.getmMax();
                    break;
                case POLYLINEM:
                    PolyLineM recPolyLineM = (PolyLineM) recordGeometry;
                    recXMin = recPolyLineM.getXMin();
                    recYMin = recPolyLineM.getYMin();
                    recXMax = recPolyLineM.getXMax();
                    recYMax = recPolyLineM.getYMax();
                    recMMin = recPolyLineM.getmMin();
                    recMMax = recPolyLineM.getmMax();
                    break;
                case MULTIPOINTZ:
                    MultiPointZ recMultiPointZ = (MultiPointZ) recordGeometry;
                    recXMin = recMultiPointZ.getXMin();
                    recYMin = recMultiPointZ.getYMin();
                    recXMax = recMultiPointZ.getXMax();
                    recYMax = recMultiPointZ.getYMax();
                    recZMin = recMultiPointZ.getzMin();
                    recZMax = recMultiPointZ.getzMin();
                    recMMin = recMultiPointZ.getmMin();
                    recMMax = recMultiPointZ.getmMax();
                    break;
                case POLYLINEZ:
                    PolyLineZ recPolyLineZ = (PolyLineZ) recordGeometry;
                    recXMin = recPolyLineZ.getXMin();
                    recYMin = recPolyLineZ.getYMin();
                    recXMax = recPolyLineZ.getXMax();
                    recYMax = recPolyLineZ.getYMax();
                    recZMin = recPolyLineZ.getzMin();
                    recZMax = recPolyLineZ.getzMax();
                    recMMin = recPolyLineZ.getmMin();
                    recMMax = recPolyLineZ.getmMax();
                    break;
                case POLYGON:
                    Polygon recPolygon = (Polygon) recordGeometry;
                    recXMin = recPolygon.getXMin();
                    recYMin = recPolygon.getYMin();
                    recXMax = recPolygon.getXMax();
                    recYMax = recPolygon.getYMax();
                    break;
                case POLYGONM:
                    PolygonM recPolygonM = (PolygonM) recordGeometry;
                    recXMin = recPolygonM.getXMin();
                    recYMin = recPolygonM.getYMin();
                    recXMax = recPolygonM.getXMax();
                    recYMax = recPolygonM.getYMax();
                    recMMin = recPolygonM.getmMin();
                    recMMax = recPolygonM.getmMax();
                    break;
                case POLYGONZ:
                    PolygonZ recPolygonZ = (PolygonZ) recordGeometry;
                    recXMin = recPolygonZ.getXMin();
                    recYMin = recPolygonZ.getYMin();
                    recXMax = recPolygonZ.getXMax();
                    recYMax = recPolygonZ.getYMax();
                    recZMin = recPolygonZ.getzMin();
                    recZMax = recPolygonZ.getzMax();
                    recMMin = recPolygonZ.getmMin();
                    recMMax = recPolygonZ.getmMax();
                    break;
                case MULTIPATCH:
                    MultiPatch recMultiPatch = (MultiPatch) recordGeometry;
                    recXMin = recMultiPatch.getXMin();
                    recYMin = recMultiPatch.getYMin();
                    recXMax = recMultiPatch.getXMax();
                    recYMax = recMultiPatch.getYMax();
                    recZMin = recMultiPatch.getzMin();
                    recZMax = recMultiPatch.getzMax();
                    recMMin = recMultiPatch.getmMin();
                    recMMax = recMultiPatch.getmMax();
                    break;
            }

            if (recXMin < xMin) {
                xMin = recXMin;
            }
            if (recYMin < yMin) {
                yMin = recYMin;
            }
            if (recXMax > xMax) {
                xMax = recXMax;
            }
            if (recYMax > yMax) {
                yMax = recYMax;
            }
            if (recZMin < zMin) {
                zMin = recZMin;
            }
            if (recZMax > zMax) {
                zMax = recZMax;
            }
            if (recMMin < mMin) {
                mMin = recMMin;
            }
            if (recMMax > mMax) {
                mMax = recMMax;
            }

            try {
                this.attributeTable.addRecord(rowData);
            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean addRecords(ArrayList<Geometry> recordsGeometry) {
        boolean allRightShapeType = true;
        for (Geometry rec : recordsGeometry) {
            if (rec.getShapeType() != shapeType) {
                allRightShapeType = false;
            }
        }
        if (allRightShapeType) {
            double recXMin = 0, recYMin = 0, recXMax = 0, recYMax = 0,
                    recZMin = 0, recZMax = 0, recMMin = 0, recMMax = 0;

            for (Geometry rec : recordsGeometry) {
                numRecs++;
                int contentLength = (4 + rec.getLength()) / 2;
                ShapeFileRecord sfr = new ShapeFileRecord(numRecs, contentLength,
                        shapeType, rec);
                records.add(sfr);


                // update the min and max coordinates

                switch (shapeType) {
                    case POINT:
                        Point recPoint = (Point) rec;
                        recXMin = recPoint.getX();
                        recYMin = recPoint.getY();
                        recXMax = recXMin;
                        recYMax = recYMin;
                        break;
                    case MULTIPOINT:
                        MultiPoint recMultiPoint = (MultiPoint) rec;
                        recXMin = recMultiPoint.getXMin();
                        recYMin = recMultiPoint.getYMin();
                        recXMax = recMultiPoint.getXMax();
                        recYMax = recMultiPoint.getYMax();
                        break;
                    case POLYLINE:
                        PolyLine recPolyLine = (PolyLine) rec;
                        recXMin = recPolyLine.getXMin();
                        recYMin = recPolyLine.getYMin();
                        recXMax = recPolyLine.getXMax();
                        recYMax = recPolyLine.getYMax();
                        break;
                    case POINTZ:
                        PointZ recPointZ = (PointZ) rec;
                        recXMin = recPointZ.getX();
                        recYMin = recPointZ.getY();
                        recXMax = recXMin;
                        recYMax = recYMin;
                        recZMin = recPointZ.getZ();
                        recZMax = recPointZ.getZ();
                        recMMin = recPointZ.getM();
                        recMMax = recPointZ.getM();
                        break;
                    case POINTM:
                        PointM recPointM = (PointM) rec;
                        recXMin = recPointM.getX();
                        recYMin = recPointM.getY();
                        recXMax = recXMin;
                        recYMax = recYMin;
                        recMMin = recPointM.getM();
                        recMMax = recPointM.getM();
                        break;
                    case MULTIPOINTM:
                        MultiPointM recMultiPointM = (MultiPointM) rec;
                        recXMin = recMultiPointM.getXMin();
                        recYMin = recMultiPointM.getYMin();
                        recXMax = recMultiPointM.getXMax();
                        recYMax = recMultiPointM.getYMax();
                        recMMin = recMultiPointM.getmMin();
                        recMMax = recMultiPointM.getmMax();
                        break;
                    case POLYLINEM:
                        PolyLineM recPolyLineM = (PolyLineM) rec;
                        recXMin = recPolyLineM.getXMin();
                        recYMin = recPolyLineM.getYMin();
                        recXMax = recPolyLineM.getXMax();
                        recYMax = recPolyLineM.getYMax();
                        recMMin = recPolyLineM.getmMin();
                        recMMax = recPolyLineM.getmMax();
                        break;
                    case MULTIPOINTZ:
                        MultiPointZ recMultiPointZ = (MultiPointZ) rec;
                        recXMin = recMultiPointZ.getXMin();
                        recYMin = recMultiPointZ.getYMin();
                        recXMax = recMultiPointZ.getXMax();
                        recYMax = recMultiPointZ.getYMax();
                        recZMin = recMultiPointZ.getzMin();
                        recZMax = recMultiPointZ.getzMin();
                        recMMin = recMultiPointZ.getmMin();
                        recMMax = recMultiPointZ.getmMax();
                        break;
                    case POLYLINEZ:
                        PolyLineZ recPolyLineZ = (PolyLineZ) rec;
                        recXMin = recPolyLineZ.getXMin();
                        recYMin = recPolyLineZ.getYMin();
                        recXMax = recPolyLineZ.getXMax();
                        recYMax = recPolyLineZ.getYMax();
                        recZMin = recPolyLineZ.getzMin();
                        recZMax = recPolyLineZ.getzMax();
                        recMMin = recPolyLineZ.getmMin();
                        recMMax = recPolyLineZ.getmMax();
                        break;
                    case POLYGON:
                        Polygon recPolygon = (Polygon) rec;
                        recXMin = recPolygon.getXMin();
                        recYMin = recPolygon.getYMin();
                        recXMax = recPolygon.getXMax();
                        recYMax = recPolygon.getYMax();
                        break;
                    case POLYGONM:
                        PolygonM recPolygonM = (PolygonM) rec;
                        recXMin = recPolygonM.getXMin();
                        recYMin = recPolygonM.getYMin();
                        recXMax = recPolygonM.getXMax();
                        recYMax = recPolygonM.getYMax();
                        recMMin = recPolygonM.getmMin();
                        recMMax = recPolygonM.getmMax();
                        break;
                    case POLYGONZ:
                        PolygonZ recPolygonZ = (PolygonZ) rec;
                        recXMin = recPolygonZ.getXMin();
                        recYMin = recPolygonZ.getYMin();
                        recXMax = recPolygonZ.getXMax();
                        recYMax = recPolygonZ.getYMax();
                        recZMin = recPolygonZ.getzMin();
                        recZMax = recPolygonZ.getzMax();
                        recMMin = recPolygonZ.getmMin();
                        recMMax = recPolygonZ.getmMax();
                        break;
                    case MULTIPATCH:
                        MultiPatch recMultiPatch = (MultiPatch) rec;
                        recXMin = recMultiPatch.getXMin();
                        recYMin = recMultiPatch.getYMin();
                        recXMax = recMultiPatch.getXMax();
                        recYMax = recMultiPatch.getYMax();
                        recZMin = recMultiPatch.getzMin();
                        recZMax = recMultiPatch.getzMax();
                        recMMin = recMultiPatch.getmMin();
                        recMMax = recMultiPatch.getmMax();
                        break;
                }

                if (recXMin < xMin) {
                    xMin = recXMin;
                }
                if (recYMin < yMin) {
                    yMin = recYMin;
                }
                if (recXMax > xMax) {
                    xMax = recXMax;
                }
                if (recYMax > yMax) {
                    yMax = recYMax;
                }
                if (recZMin < zMin) {
                    zMin = recZMin;
                }
                if (recZMax > zMax) {
                    zMax = recZMax;
                }
                if (recMMin < mMin) {
                    mMin = recMMin;
                }
                if (recMMax > mMax) {
                    mMax = recMMax;
                }

            }
            return true;
        } else {
            return false;
        }
    }

    public boolean addRecords(ArrayList<Geometry> recordsGeometry, ArrayList<Object[]> attributeData) {
        boolean allRightShapeType = true;
        for (Geometry rec : recordsGeometry) {
            if (rec.getShapeType() != shapeType) {
                allRightShapeType = false;
            }
        }
        if (allRightShapeType) {
            if (recordsGeometry.size() != attributeData.size()) {
                // there must be an attribute array for each record.
                return false;
            }

            double recXMin = 0, recYMin = 0, recXMax = 0, recYMax = 0,
                    recZMin = 0, recZMax = 0, recMMin = 0, recMMax = 0;

            for (Geometry rec : recordsGeometry) {
                numRecs++;
                int contentLength = (4 + rec.getLength()) / 2;
                ShapeFileRecord sfr = new ShapeFileRecord(numRecs, contentLength,
                        shapeType, rec);
                records.add(sfr);


                // update the min and max coordinates

                switch (shapeType) {
                    case POINT:
                        Point recPoint = (Point) rec;
                        recXMin = recPoint.getX();
                        recYMin = recPoint.getY();
                        recXMax = recXMin;
                        recYMax = recYMin;
                        break;
                    case MULTIPOINT:
                        MultiPoint recMultiPoint = (MultiPoint) rec;
                        recXMin = recMultiPoint.getXMin();
                        recYMin = recMultiPoint.getYMin();
                        recXMax = recMultiPoint.getXMax();
                        recYMax = recMultiPoint.getYMax();
                        break;
                    case POLYLINE:
                        PolyLine recPolyLine = (PolyLine) rec;
                        recXMin = recPolyLine.getXMin();
                        recYMin = recPolyLine.getYMin();
                        recXMax = recPolyLine.getXMax();
                        recYMax = recPolyLine.getYMax();
                        break;
                    case POINTZ:
                        PointZ recPointZ = (PointZ) rec;
                        recXMin = recPointZ.getX();
                        recYMin = recPointZ.getY();
                        recXMax = recXMin;
                        recYMax = recYMin;
                        recZMin = recPointZ.getZ();
                        recZMax = recPointZ.getZ();
                        recMMin = recPointZ.getM();
                        recMMax = recPointZ.getM();
                        break;
                    case POINTM:
                        PointM recPointM = (PointM) rec;
                        recXMin = recPointM.getX();
                        recYMin = recPointM.getY();
                        recXMax = recXMin;
                        recYMax = recYMin;
                        recMMin = recPointM.getM();
                        recMMax = recPointM.getM();
                        break;
                    case MULTIPOINTM:
                        MultiPointM recMultiPointM = (MultiPointM) rec;
                        recXMin = recMultiPointM.getXMin();
                        recYMin = recMultiPointM.getYMin();
                        recXMax = recMultiPointM.getXMax();
                        recYMax = recMultiPointM.getYMax();
                        recMMin = recMultiPointM.getmMin();
                        recMMax = recMultiPointM.getmMax();
                        break;
                    case POLYLINEM:
                        PolyLineM recPolyLineM = (PolyLineM) rec;
                        recXMin = recPolyLineM.getXMin();
                        recYMin = recPolyLineM.getYMin();
                        recXMax = recPolyLineM.getXMax();
                        recYMax = recPolyLineM.getYMax();
                        recMMin = recPolyLineM.getmMin();
                        recMMax = recPolyLineM.getmMax();
                        break;
                    case MULTIPOINTZ:
                        MultiPointZ recMultiPointZ = (MultiPointZ) rec;
                        recXMin = recMultiPointZ.getXMin();
                        recYMin = recMultiPointZ.getYMin();
                        recXMax = recMultiPointZ.getXMax();
                        recYMax = recMultiPointZ.getYMax();
                        recZMin = recMultiPointZ.getzMin();
                        recZMax = recMultiPointZ.getzMin();
                        recMMin = recMultiPointZ.getmMin();
                        recMMax = recMultiPointZ.getmMax();
                        break;
                    case POLYLINEZ:
                        PolyLineZ recPolyLineZ = (PolyLineZ) rec;
                        recXMin = recPolyLineZ.getXMin();
                        recYMin = recPolyLineZ.getYMin();
                        recXMax = recPolyLineZ.getXMax();
                        recYMax = recPolyLineZ.getYMax();
                        recZMin = recPolyLineZ.getzMin();
                        recZMax = recPolyLineZ.getzMax();
                        recMMin = recPolyLineZ.getmMin();
                        recMMax = recPolyLineZ.getmMax();
                        break;
                    case POLYGON:
                        Polygon recPolygon = (Polygon) rec;
                        recXMin = recPolygon.getXMin();
                        recYMin = recPolygon.getYMin();
                        recXMax = recPolygon.getXMax();
                        recYMax = recPolygon.getYMax();
                        break;
                    case POLYGONM:
                        PolygonM recPolygonM = (PolygonM) rec;
                        recXMin = recPolygonM.getXMin();
                        recYMin = recPolygonM.getYMin();
                        recXMax = recPolygonM.getXMax();
                        recYMax = recPolygonM.getYMax();
                        recMMin = recPolygonM.getmMin();
                        recMMax = recPolygonM.getmMax();
                        break;
                    case POLYGONZ:
                        PolygonZ recPolygonZ = (PolygonZ) rec;
                        recXMin = recPolygonZ.getXMin();
                        recYMin = recPolygonZ.getYMin();
                        recXMax = recPolygonZ.getXMax();
                        recYMax = recPolygonZ.getYMax();
                        recZMin = recPolygonZ.getzMin();
                        recZMax = recPolygonZ.getzMax();
                        recMMin = recPolygonZ.getmMin();
                        recMMax = recPolygonZ.getmMax();
                        break;
                    case MULTIPATCH:
                        MultiPatch recMultiPatch = (MultiPatch) rec;
                        recXMin = recMultiPatch.getXMin();
                        recYMin = recMultiPatch.getYMin();
                        recXMax = recMultiPatch.getXMax();
                        recYMax = recMultiPatch.getYMax();
                        recZMin = recMultiPatch.getzMin();
                        recZMax = recMultiPatch.getzMax();
                        recMMin = recMultiPatch.getmMin();
                        recMMax = recMultiPatch.getmMax();
                        break;
                }

                if (recXMin < xMin) {
                    xMin = recXMin;
                }
                if (recYMin < yMin) {
                    yMin = recYMin;
                }
                if (recXMax > xMax) {
                    xMax = recXMax;
                }
                if (recYMax > yMax) {
                    yMax = recYMax;
                }
                if (recZMin < zMin) {
                    zMin = recZMin;
                }
                if (recZMax > zMax) {
                    zMax = recZMax;
                }
                if (recMMin < mMin) {
                    mMin = recMMin;
                }
                if (recMMax > mMax) {
                    mMax = recMMax;
                }

            }

            try {
                for (Object[] rowData : attributeData) {
                    this.attributeTable.addRecord(rowData);
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Used to retrieve a particular record.
     * @param recordNumber The zero-based record number.
     * @return A ShapeFileRecord corresponding with the record number.
     */
    public ShapeFileRecord getRecord(int recordNumber) {
        return records.get(recordNumber);
    }

    public void deleteRecord(int recordNumber) {
        try {
            String tempFile = StringUtilities.replaceLast(fileName, ".shp", "_temp.shp");
            DBFField fields[] = this.attributeTable.getAllFields();
            ShapeFile tempShape = new ShapeFile(tempFile, this.shapeType, fields);
            int i = 0;
            for (ShapeFileRecord record : this.records) {
                if (record.getRecordNumber() != recordNumber) {
                    tempShape.addRecord(record.getGeometry(), attributeTable.getRecord(i));
                }
                i++;
            }
            tempShape.write();
            
            Path source = Paths.get(tempFile);
            Path target = Paths.get(fileName);
            Files.move(source, target, REPLACE_EXISTING);

            source = Paths.get(tempShape.getDatabaseFile());
            target = Paths.get(this.databaseFile);
            Files.move(source, target, REPLACE_EXISTING);
            
            source = Paths.get(tempShape.getIndexFile());
            target = Paths.get(this.indexFile);
            Files.move(source, target, REPLACE_EXISTING);

            // reload the data files.
            setFileName(fileName);
            this.indexFile = StringUtilities.replaceLast(fileName, ".shp", ".shx");
            setProjectionFile(StringUtilities.replaceLast(fileName, ".shp", ".prj"));
            setDatabaseFile(StringUtilities.replaceLast(fileName, ".shp", ".dbf"));
            // see if the databaseFile exists.
            databaseFileExists = (new File(databaseFile)).exists();

            if (databaseFileExists) {
                this.attributeTable = new AttributeTable(databaseFile);
            }

        } catch (Exception e) {
            System.out.println("ShapeFile.deleteRecord Error: " + e.getMessage());
        }
    }

    private boolean readRecords() {
        int pos;
        int recordNumber, contentLength;
        ShapeType recShapeType;

        RandomAccessFile rIn = null;
        ByteBuffer buf;

        try {
            
            records.clear();

            // See if the data file exists.
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }

            buf = ByteBuffer.allocate(fileLength * 2);

            rIn = new RandomAccessFile(fileName, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();

            // read the records into an arraylist of ShapeFileRecords
            pos = 100;
            buf.rewind();
            byte[] data;
            int i = 0;
            int contentLenInBytes;
            while (pos < fileLength * 2) {
                recordNumber = ByteSwapper.swap(buf.getInt(pos));
                contentLength = ByteSwapper.swap(buf.getInt(pos + 4));
                contentLenInBytes = contentLength * 2 - 4; // the minus four is to exclude the recShapeType
                recShapeType = getShapeTypeFromInt(buf.getInt(pos + 8));
                data = new byte[contentLenInBytes];
                buf.position(pos + 12);
                buf.get(data, 0, contentLenInBytes);
                records.add(new ShapeFileRecord(recordNumber, contentLength, recShapeType, data));
                //records[i] = new ShapeFileRecord(recordNumber, contentLength, recShapeType, data);
                pos += 8 + contentLength * 2;
                i++;
            }

            numRecs = records.size();

            try {
                rIn.close();
            } catch (Exception e) {
            }

            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }// finally {
        //if (rIn != null) {
//                try {
//                    rIn.close();
//                } catch (Exception e) {
//                }
        //}
        //}
    }

    /**
     * Returns an ArrayList of ShapeFileRecords that are within an area
     * described by a BoundingBox, which usually corresponds with a mapped area.
     *
     * @param box The bounding box describing the extent of the area into which
     * the data will be mapped.
     * @param minSize The minimum size of feature to be mapped, in map units.
     * This is based on the size of a single pixel, in map units, which is
     * affected by the scale of the map.
     * @return An array list of ShapeFileRecords that intersect with the map
     * area bounding box and that are larger than the minimum size..
     */
    public ArrayList<ShapeFileRecord> getRecordsInBoundingBox(BoundingBox box, double minSize) {
        ArrayList<ShapeFileRecord> recs = new ArrayList<>();
        // first see if the bounding box for the entire shapefile fits within box
        BoundingBox myBox = new BoundingBox(xMin, yMin, xMax, yMax);
        if (box.contains(myBox)) {
            // just return all of the records
            for (ShapeFileRecord sfr : records) {
                recs.add(sfr);
            }
            return recs;
        }
        if (myBox.overlaps(box)) {
            for (ShapeFileRecord sfr : records) {
                Geometry sfrGeom = sfr.getGeometry();
                if (sfrGeom != null) {
                    if (sfrGeom.isMappable(box, minSize)) {
                        recs.add(sfr);
                    }
                }
            }
            return recs;
        } else {
            // it doesn't overlap with box at all and null is returned.
            return null;
        }
    }

    public ArrayList<ShapeFileRecord> clipRecordsToBoundingBox(BoundingBox box, double minSize) {
        ArrayList<ShapeFileRecord> recs = new ArrayList<>();
        // first see if the bounding box for the entire shapefile fits within box
        BoundingBox myBox = new BoundingBox(xMin, yMin, xMax, yMax);
        if (box.contains(myBox)) {
            // just return all of the records
            for (ShapeFileRecord sfr : records) {
                recs.add(sfr);
            }
            return recs;
        }
        if (myBox.overlaps(box)) {
            for (ShapeFileRecord sfr : records) {
                Geometry sfrGeometry = sfr.getGeometry();
                if (sfrGeometry.isMappable(box, minSize)) {
                    // does it need to be clipped?
                    if (!sfrGeometry.needsClipping(box)) {
                        recs.add(sfr);
                    } else {
                    }
                }
            }
            return recs;
        } else {
            // it doesn't overlap with box at all and null is returned.
            return null;
        }
    }

    /**
     * Gets the AttributeTable object associated with this ShapeFile. If no
     * database exists for this ShapeFile null will be returned.
     *
     * @return AttributeTable object or null
     */
    public AttributeTable getAttributeTable() {
        return this.attributeTable;
    }

    public String[] getAttributeTableFields() {
//        try {

        if (databaseFileExists) {
//                DBFReader reader = new DBFReader(databaseFile);
            try {
                AttributeTable reader = new AttributeTable(databaseFile);

                //DBFReader reader = new DBFReader( inputStream); 

                // get the field count if you want for some reasons like the following
                //
                int numberOfFields = reader.getFieldCount();
                String[] ret = new String[numberOfFields];
                for (int i = 0; i < numberOfFields; i++) {

                    DBFField field = reader.getField(i);

                    ret[i] = field.getName();
                }
                return ret;
            } catch (Exception e) {
                return null;
            }

        } else {
            // return an empty string array
            return new String[1];
        }

//            // Now, lets us start reading the rows
//            //
//            Object[] rowObjects;
//
//            while ((rowObjects = reader.nextRecord()) != null) {
//
//                for (int i = 0; i < rowObjects.length; i++) {
//
//                    System.out.println(rowObjects[i]);
//                }
//            }


//        } catch (DBFException dbfe) {
//            System.out.println(dbfe);
//            return null;
//        }
    }

//    private boolean readDatabaseFile() {
//        try {
////            DBFReader reader = new DBFReader(databaseFile);
////            
////            //DBFReader reader = new DBFReader( inputStream); 
////
////              // get the field count if you want for some reasons like the following
////            //
////            int numberOfFields = reader.getFieldCount();
////
////            // use this count to fetch all field information
////            // if required
////            //
////            for (int i = 0; i < numberOfFields; i++) {
////
////                DBFField field = reader.getField(i);
////
////                // do something with it if you want
////                // refer the JavaDoc API reference for more details
////                //
////                System.out.println(field.getName());
////            }
////
////            // Now, lets us start reading the rows
////            //
////            Object[] rowObjects;
////
////            while ((rowObjects = reader.nextRecord()) != null) {
////
////                for (int i = 0; i < rowObjects.length; i++) {
////
////                    System.out.println(rowObjects[i]);
////                }
////            }
//
//            return true;
//        } catch (DBFException dbfe) {
//            System.out.println(dbfe);
//            return false;
//        } 
//    }
    private void readProjectionFile() {

        // this method still needs work to enable parsing of a WKT file.
        boolean wktFormat = false;
        DataInputStream in = null;
        BufferedReader br = null;
        try {
            // see if the projection file exists
            File projFile = new File(projectionFile);
            if (!projFile.exists()) {
                //System.out.println("The projection file could not be located.");
                return;
            }
            // Open the file
            FileInputStream fstream = new FileInputStream(projectionFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            if (projectionFile != null) {
                String line;
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    // is it in WKT format?
                    if (line.toLowerCase().endsWith("unit[\"meter\",1.0]]")) {
                        xyUnits = "metres";
                    }
                    if (line.toLowerCase().contains("unit")) {
                        int j = line.toLowerCase().indexOf("unit");
                        int k = line.toLowerCase().indexOf("[", j);
                        //System.out.println(line);
                    }
                    if (line.contains("[") || line.contains("(")) {
                        // it is in WKT format
                        wktFormat = true;
                        // first make sure that it contains square and not round brackets
                        line = line.replace("(", "[");
                        line = line.replace(")", "]");


//                        String[] str = line.split(",");
//                        for (int i = 0; i < str.length; i++) {
//                            System.out.println(str[i]);
//                        }
                    } else {
                        // it is in the older format for .prj files.
                    }
                    //str = line.split("\t");
                    //System.out.println(line);
                }
                //Close the input stream
                in.close();
                br.close();

            }
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                if (in != null || br != null) {
                    in.close();
                    br.close();
                }
            } catch (java.io.IOException ex) {
            }

        }

    }
    
    public boolean areGeometriesEqual(ShapeFile other) {
        if (other.getNumberOfRecords() != this.getNumberOfRecords()) {
            return false;
        }
        
        if (other.getShapeType() != this.getShapeType()) {
            return false;
        }
        
        int numRecords = this.getNumberOfRecords();
        double[][] thisPoints;
        double[][] otherPoints;
        for (int a = 0; a < numRecords; a++) {
            thisPoints = this.getRecord(a).getGeometry().getPoints();
            otherPoints = other.getRecord(a).getGeometry().getPoints();
            if (thisPoints.length != otherPoints.length) {
                return false;
            } else {
                for (int b = 0; b < thisPoints.length; b++) {
                    if (thisPoints[b][0] != otherPoints[b][0] ||
                            thisPoints[b][1] != otherPoints[b][1]) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    public void refreshAttributeTable() {
        try {
            this.attributeTable = new AttributeTable(databaseFile);
        } catch (Exception e) {
        }
    }
    
//    public KdTree<Integer> getKdTree() {
//        KdTree<Integer> kdTree = new KdTree<>();
//        
//    }
    
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        
//        // reading shapefiles test
//        
////        //String fileName = "/Users/johnlindsay/Documents/Data/Marvin-UofG-20111005-Order2133/SWOOP 2010/DEM - Masspoints and Breaklines - 400km_ZIP_UTM17_50cm_XXbands_0bits/20km174000471002010MAPCON/20km17400047100_masspoints.shp";
////        //String fileName = "/Users/johnlindsay/Documents/Data/ShapeFiles/NTDB_roads_rmow.shp";
////        String fileName = "/Users/johnlindsay/Documents/Data/ShapeFiles/Water_Body_rmow.shp";
////        
////        ShapeFile shp = new ShapeFile(fileName);
////        
////        System.out.println("ShapeFile info");
////        System.out.println("Number of records: " + shp.getNumberOfRecords());
////        ShapeFileRecord rec = shp.getRecord(0);
////        System.out.println("Record number: " + rec.getRecordNumber());
////        System.out.println("Shape Type: " + rec.getShapeType() + ", Length: " + rec.getContentLength());
////        System.out.println(rec.getGeometry().getClass());    
////        if (rec.getShapeType() == ShapeType.POINTZ) {
////            PointZ pt = (PointZ)(rec.getGeometry());
////            System.out.println(rec.getGeometry().getClass());
////            System.out.println(pt.getZ());
////        } else if (rec.getShapeType() == ShapeType.POLYLINE) {
////            PolyLine pl = (PolyLine)(rec.getGeometry());
////            System.out.println("First point x: " + pl.getPoints()[0][0] + " First point y: " + pl.getPoints()[0][1]);
////        }
//        
//        // writing shapefiles test
//        String fileName = "/Users/johnlindsay/Documents/Data/tmp2.shp";
//        //ShapeFile shp = new ShapeFile(fileName, ShapeType.POINT);
//        ShapeFile shp = new ShapeFile(fileName, ShapeType.POLYLINE);
//        shp.deleteFiles();
////        ArrayList<Geometry> pnts = new ArrayList<Geometry>(); 
////        Random generator = new Random();
////        double x, y;
////        
////        for (int i = 0; i < 500; i++) {
////            x = generator.nextInt(1000);
////            y = generator.nextInt(600);
////            pnts.add(new Point(x, y));
////        }
////        shp.addRecords(pnts);
//        
//        ArrayList<Geometry> lines = new ArrayList<Geometry>();
//        int[] parts = {0, 6};
//        //double[][] points = new double[11][2];
//        PointsList pl = new PointsList();
//        // pentagon
//        pl.addPoint(50, 100);
//        pl.addPoint(100, 75);
//        pl.addPoint(75, 0);
//        pl.addPoint(25, 0);
//        pl.addPoint(0, 75);
//        pl.addPoint(50, 100);
//        //square
//        pl.addPoint(25, 75);
//        pl.addPoint(25, 25);
//        pl.addPoint(75, 25);
//        pl.addPoint(75, 75);
//        pl.addPoint(25, 75);
//        
//        lines.add(new PolyLine(parts, pl.getPointsArray()));
//
//        shp.addRecords(lines);
//        try {
//            shp.write();
//        } catch (IOException ioe) {
//            
//        }
//        // now read it in
//        //ShapeFile shp2 = new ShapeFile(fileName);
//    }
}
