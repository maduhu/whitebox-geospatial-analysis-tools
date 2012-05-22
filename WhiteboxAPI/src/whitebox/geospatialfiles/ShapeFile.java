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
import java.util.ArrayList;
import whitebox.geospatialfiles.shapefile.DBF.DBFException;
import whitebox.geospatialfiles.shapefile.DBF.DBFField;
import whitebox.geospatialfiles.shapefile.DBF.DBFReader;
import whitebox.geospatialfiles.shapefile.*;
import whitebox.structures.BoundingBox;
import whitebox.utilities.ByteSwapper;

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
    //public ShapeFileRecord[] records;
    public ArrayList<ShapeFileRecord> records = new ArrayList<ShapeFileRecord>();
    private boolean pointType;
    
    // Constructors
    public ShapeFile(String fileName) {
        setFileName(fileName);
        int extensionIndex = fileName.lastIndexOf(".");
        this.indexFile = fileName.substring(0, extensionIndex) + ".shx";
        setProjectionFile(fileName.substring(0, extensionIndex) + ".prj");
        setDatabaseFile(fileName.substring(0, extensionIndex) + ".dbf");
        // see if the databaseFile exists.
        databaseFileExists = (new File(databaseFile)).exists();
        
    }
    
    public ShapeFile(String fileName, ShapeFileRecord[] recs) {
        numRecs = recs.length;
        //records = new ShapeFileRecord[numRecs];
        //System.arraycopy(recs, 0, records, 0, numRecs);
        this.fileName = fileName;
        int extensionIndex = fileName.lastIndexOf(".");
        this.indexFile = fileName.substring(0, extensionIndex) + ".shx";
        this.projectionFile = fileName.substring(0, extensionIndex) + ".prj";
        this.databaseFile = fileName.substring(0, extensionIndex) + ".dbf";
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
    private boolean readHeaderData() {
        RandomAccessFile rIn = null;
        ByteBuffer buf;
        
        try {
            // See if the data file exists.
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }

            buf = ByteBuffer.allocate(109);

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
     
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    private boolean writeHeaderData() {
        return true;
    }
    
    public boolean addRecord(ShapeFileRecord rec) {
        if (rec.getShapeType() == shapeType) {
            records.add(rec);
            numRecs++;
            return true;
        } else {
            return false;
        }
    }
    
    public boolean addRecords(ArrayList<ShapeFileRecord> recs) {
        boolean allRightShapeType = true;
        for (ShapeFileRecord rec : recs) {
            if (rec.getShapeType() != shapeType) {
                allRightShapeType = false;
            }
        }
        if (allRightShapeType) {
            for (ShapeFileRecord rec : recs) {
                records.add(rec);
            }
            numRecs = records.size();
            return true;
        } else {
            return false;
        }
    }
    
    public ShapeFileRecord getRecord(int recordNumber) {
        return records.get(recordNumber);
    }
    
    private boolean readRecords() {
        int pos = 100;
        int recordNumber, contentLength;
        ShapeType recShapeType;
        
        RandomAccessFile rIn = null;
        ByteBuffer buf;
        
        try {

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
            
            // first count the number of records.
            numRecs = 0;
            do {
                contentLength = ByteSwapper.swap(buf.getInt(pos + 4)); // is in 16-bit words.
                numRecs++;
                pos += 8 + contentLength * 2;
            } while (pos < fileLength * 2);
            
            // now read them into an array of ShapeFileRecords
            //records = new ShapeFileRecord[numRecs];
            pos = 100;
            buf.rewind();
            byte[] data;
            int i = 0;
            int contentLenInBytes;
            do {
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
            } while (pos < fileLength * 2);
     
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
//    public ArrayList<ShapeFileRecord> getRecordsInBoundingBox(BoundingBox box) {
//        ArrayList<ShapeFileRecord> recs = new ArrayList<ShapeFileRecord>();
//        
//        if (shapeType == ShapeType.POINT) {
//            for (int r = 0; r < records.length; r++) {
//                whitebox.geospatialfiles.shapefile.Point rec = 
//                        (whitebox.geospatialfiles.shapefile.Point) (records[r].getData());
//                if (box.isPointInBox(rec.getX(), rec.getY())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POINTZ) {
//            for (int r = 0; r < records.length; r++) {
//                PointZ rec = (PointZ) (records[r].getData());
//                if (box.isPointInBox(rec.getX(), rec.getY())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POINTM) {
//            for (int r = 0; r < records.length; r++) {
//                PointM rec = (PointM) (records[r].getData());
//                if (box.isPointInBox(rec.getX(), rec.getY())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.MULTIPOINT) {
//            double[][] recPoints;
//            for (int r = 0; r < records.length; r++) {
//                MultiPoint rec = (MultiPoint) (records[r].getData());
//                recPoints = rec.getPoints();
//                for (int p = 0; p < recPoints.length; p++) {
//                    if (box.isPointInBox(recPoints[p][0], recPoints[p][1])) {
//                        recs.add(records[r]);
//                    }
//                }
//            }
//        } else if (shapeType == ShapeType.MULTIPOINTZ) {
//            double[][] recPoints;
//            for (int r = 0; r < records.length; r++) {
//                MultiPointZ rec = (MultiPointZ) (records[r].getData());
//                recPoints = rec.getPoints();
//                for (int p = 0; p < recPoints.length; p++) {
//                    if (box.isPointInBox(recPoints[p][0], recPoints[p][1])) {
//                        recs.add(records[r]);
//                    }
//                }
//            }
//        } else if (shapeType == ShapeType.MULTIPOINTM) {
//            double[][] recPoints;
//            for (int r = 0; r < records.length; r++) {
//                MultiPointM rec = (MultiPointM) (records[r].getData());
//                recPoints = rec.getPoints();
//                for (int p = 0; p < recPoints.length; p++) {
//                    if (box.isPointInBox(recPoints[p][0], recPoints[p][1])) {
//                        recs.add(records[r]);
//                    }
//                }
//            }
//        } else if (shapeType == ShapeType.POLYLINE) {
//            for (int r = 0; r < records.length; r++) {
//                PolyLine rec = (PolyLine) (records[r].getData());
//                if (box.doesIntersect(rec.getBox())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POLYLINEZ) {
//            for (int r = 0; r < records.length; r++) {
//                PolyLineZ rec = (PolyLineZ) (records[r].getData());
//                if (box.doesIntersect(rec.getBox())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POLYLINEM) {
//            for (int r = 0; r < records.length; r++) {
//                PolyLineM rec = (PolyLineM) (records[r].getData());
//                if (box.doesIntersect(rec.getBox())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POLYGON) {
//            for (int r = 0; r < records.length; r++) {
//                Polygon rec = (Polygon) (records[r].getData());
//                if (box.doesIntersect(rec.getBox())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POLYGONZ) {
//            for (int r = 0; r < records.length; r++) {
//                PolygonZ rec = (PolygonZ) (records[r].getData());
//                if (box.doesIntersect(rec.getBox())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else if (shapeType == ShapeType.POLYGONM) {
//            for (int r = 0; r < records.length; r++) {
//                PolygonM rec = (PolygonM) (records[r].getData());
//                if (box.doesIntersect(rec.getBox())) {
//                    recs.add(records[r]);
//                }
//            }
//        } else {
//            return null;
//        }
//        
//        return recs; //(ShapeFileRecord[]) recs.toArray(new ShapeFileRecord[recs.size()]);
//    }
     
    
    public ArrayList<ShapeFileRecord> getRecordsInBoundingBox(BoundingBox box, double minSize) {
        ArrayList<ShapeFileRecord> recs = new ArrayList<ShapeFileRecord>();
        
        if (shapeType == ShapeType.POINT) {
            //for (int r = 0; r < records.size(); r++) {
            for (ShapeFileRecord sfr : records) {
                whitebox.geospatialfiles.shapefile.Point rec = 
                        (whitebox.geospatialfiles.shapefile.Point) (sfr.getData());
                if (box.isPointInBox(rec.getX(), rec.getY())) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POINTZ) {
            for (ShapeFileRecord sfr : records) {
                PointZ rec = (PointZ) (sfr.getData());
                if (box.isPointInBox(rec.getX(), rec.getY())) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POINTM) {
            for (ShapeFileRecord sfr : records) {
                PointM rec = (PointM) (sfr.getData());
                if (box.isPointInBox(rec.getX(), rec.getY())) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.MULTIPOINT) {
            double[][] recPoints;
            for (ShapeFileRecord sfr : records) {
                MultiPoint rec = (MultiPoint) (sfr.getData());
                recPoints = rec.getPoints();
                for (int p = 0; p < recPoints.length; p++) {
                    if (box.isPointInBox(recPoints[p][0], recPoints[p][1])) {
                        recs.add(sfr);
                    }
                }
            }
        } else if (shapeType == ShapeType.MULTIPOINTZ) {
            double[][] recPoints;
            for (ShapeFileRecord sfr : records) {
                MultiPointZ rec = (MultiPointZ) (sfr.getData());
                recPoints = rec.getPoints();
                for (int p = 0; p < recPoints.length; p++) {
                    if (box.isPointInBox(recPoints[p][0], recPoints[p][1])) {
                        recs.add(sfr);
                    }
                }
            }
        } else if (shapeType == ShapeType.MULTIPOINTM) {
            double[][] recPoints;
            for (ShapeFileRecord sfr : records) {
                MultiPointM rec = (MultiPointM) (sfr.getData());
                recPoints = rec.getPoints();
                for (int p = 0; p < recPoints.length; p++) {
                    if (box.isPointInBox(recPoints[p][0], recPoints[p][1])) {
                        recs.add(sfr);
                    }
                }
            }
        } else if (shapeType == ShapeType.POLYLINE) {
            for (ShapeFileRecord sfr : records) {
                PolyLine rec = (PolyLine) (sfr.getData());
                if (box.doesIntersect(rec.getBox()) && rec.getBox().getMaxExtent() > minSize) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POLYLINEZ) {
            for (ShapeFileRecord sfr : records) {
                PolyLineZ rec = (PolyLineZ) (sfr.getData());
                if (box.doesIntersect(rec.getBox()) && rec.getBox().getMaxExtent() > minSize) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POLYLINEM) {
            for (ShapeFileRecord sfr : records) {
                PolyLineM rec = (PolyLineM) (sfr.getData());
                if (box.doesIntersect(rec.getBox()) && rec.getBox().getMaxExtent() > minSize) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POLYGON) {
            for (ShapeFileRecord sfr : records) {
                Polygon rec = (Polygon) (sfr.getData());
                if (box.doesIntersect(rec.getBox()) && rec.getBox().getMaxExtent() > minSize) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POLYGONZ) {
            for (ShapeFileRecord sfr : records) {
                PolygonZ rec = (PolygonZ) (sfr.getData());
                if (box.doesIntersect(rec.getBox()) && rec.getBox().getMaxExtent() > minSize) {
                    recs.add(sfr);
                }
            }
        } else if (shapeType == ShapeType.POLYGONM) {
            for (ShapeFileRecord sfr : records) {
                PolygonM rec = (PolygonM) (sfr.getData());
                if (box.doesIntersect(rec.getBox()) && rec.getBox().getMaxExtent() > minSize) {
                    recs.add(sfr);
                }
            }
        } else {
            return null;
        }
        
        return recs; //(ShapeFileRecord[]) recs.toArray(new ShapeFileRecord[recs.size()]);
    }
    
    public String[] getAttributeTableFields() {
        try {
            
            if (databaseFileExists){ 
                DBFReader reader = new DBFReader(databaseFile);

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

            
        } catch (DBFException dbfe) {
            System.out.println(dbfe);
            return null;
        } 
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
        
        DataInputStream in = null;
        BufferedReader br = null;
        try {
            // see if the projection file exists
            File projFile = new File(projectionFile);
            if (!projFile.exists()) {
                System.out.println("The projection file could not be located.");
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
                    if (line.contains("[") || line.contains("(")) {
                        // it is in WKT format
                        
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
                if (in != null || br!= null) {
                    in.close();
                    br.close();
                }
            } catch (java.io.IOException ex) {
            }

        }

    }
    
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        //String fileName = "/Users/johnlindsay/Documents/Data/Marvin-UofG-20111005-Order2133/SWOOP 2010/DEM - Masspoints and Breaklines - 400km_ZIP_UTM17_50cm_XXbands_0bits/20km174000471002010MAPCON/20km17400047100_masspoints.shp";
//        //String fileName = "/Users/johnlindsay/Documents/Data/ShapeFiles/NTDB_roads_rmow.shp";
//        String fileName = "/Users/johnlindsay/Documents/Data/ShapeFiles/Water_Body_rmow.shp";
//        
//        ShapeFile shp = new ShapeFile(fileName);
//        
//        System.out.println("ShapeFile info");
//        System.out.println("Number of records: " + shp.getNumberOfRecords());
//        ShapeFileRecord rec = shp.getRecord(0);
//        System.out.println("Record number: " + rec.getRecordNumber());
//        System.out.println("Shape Type: " + rec.getShapeType() + ", Length: " + rec.getContentLength());
//        System.out.println(rec.getData().getClass());    
//        if (rec.getShapeType() == ShapeType.POINTZ) {
//            PointZ pt = (PointZ)(rec.getData());
//            System.out.println(rec.getData().getClass());
//            System.out.println(pt.getZ());
//        } else if (rec.getShapeType() == ShapeType.POLYLINE) {
//            PolyLine pl = (PolyLine)(rec.getData());
//            System.out.println("First point x: " + pl.getPoints()[0][0] + " First point y: " + pl.getPoints()[0][1]);
//        }
//    }
}
