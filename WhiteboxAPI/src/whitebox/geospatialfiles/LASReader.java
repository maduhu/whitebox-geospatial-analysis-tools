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

import java.io.File;
import java.nio.ByteOrder;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.ArrayList;
import whitebox.utilities.Unsigned;
import whitebox.utilities.BitOps;
import whitebox.structures.BoundingBox;

/**
 * This class is used to provide reading and writing capabilities with LAS LiDAR
 * files.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class LASReader {
    
    // Fields
    private String fileName = "";
    private short versionMajor = 1;
    private short versionMinor = 3;
    private int fileSourceID = 0;
    private byte GPSTimeType = 0;
    private byte waveDataPacketsInternal = 0;
    private byte waveDataPacketsExternal = 0;
    private byte retNumsSynthGenerated = 0;
    private long projectID1 = 0;
    private int projectID2 = 0;
    private int projectID3 = 0;
    private short[] projectID4 = new short[8];
    private String systemIdentifer = "";
    private String generatingSoftware = "";
    private int fileCreationDay = 0;
    private int fileCreationYear = 0;
    private int headerSize = 0;
    private long offsetToPointData = 0;
    private long numVLR = 0;
    private short pointDataFormatID = 0;
    private int pointDataRecLength = 0;
    private long numPointRecords = 0;
    private long[] numPointsByReturn = new long[5];
    private double xScale, yScale, zScale;
    private double xOffset, yOffset, zOffset;
    private double maxX, minX, maxY, minY, maxZ, minZ;
    private ArrayList<VariableLengthRecord> vlrArray = new ArrayList<>();
    private int bufferSize = 1000;
    private int startingPoint = -1;
    private int endingPoint = -1;
    private PointRecord[] pointRecs;
    private PointRecColours[] pointColours;
    //private PointWavePacket[] pointWavePagetData;
    
    // Constructors
    public LASReader() {
        
    }
    
    public LASReader(String fileName) {
        setFileName(fileName);
    }

    // Properties
    public String getFileName() {
        return fileName;
    }

    public final void setFileName(String fileName) {
        this.fileName = fileName;
        readHeaderData();
        readVariableLengthRecords();
    }

    public int getFileSourceID() {
        return fileSourceID;
    }
    
    public short getVersionMajor() {
        return versionMajor;
    }

    public short getVersionMinor() {
        return versionMinor;
    }

    public byte getGPSTimeType() {
        return GPSTimeType;
    }

    public byte getWaveDataPacketsInternal() {
        return waveDataPacketsInternal;
    }

    public byte getWaveDataPacketsExternal() {
        return waveDataPacketsExternal;
    }

    public byte getRetNumsSynthGenerated() {
        return retNumsSynthGenerated;
    }

    public long getProjectID1() {
        return projectID1;
    }

    public int getProjectID2() {
        return projectID2;
    }

    public int getProjectID3() {
        return projectID3;
    }
    
    public short[] getProjectID4() {
        return projectID4;
    }
    
    public String getSystemIdentifer() {
        return systemIdentifer;
    }

    public String getGeneratingSoftware() {
        return generatingSoftware;
    }
    
    public int getFileCreationDay() {
        return fileCreationDay;
    }

    public int getFileCreationYear() {
        return fileCreationYear;
    }
    
    public int getHeaderSize() {
        return headerSize;
    }
    
    public long getOffsetToPointData() {
        return offsetToPointData;
    }
    
    public long getNumVLR() {
        return numVLR;
    }
    
    public short getPointDataFormatID() {
        return pointDataFormatID;
    }
    
    public int getPointDataRecLength() {
        return pointDataRecLength;
    }
    
    public long getNumPointRecords() {
        return numPointRecords;
    }
    
    public long[] getNumPointsByReturn() {
        return numPointsByReturn;
    }
    
    public double getXScale() {
        return xScale;
    }
    
    public double getYScale() {
        return yScale;
    }
    
    public double getZScale() {
        return zScale;
    }
    
    public double getXOffset() {
        return xOffset;
    }
    
    public double getYOffset() {
        return yOffset;
    }
    
    public double getZOffset() {
        return zOffset;
    }
    
    public double getMaxX() {
        return maxX;
    }
    
    public double getMinX() {
        return minX;
    }
    
    public double getMaxY() {
        return maxY;
    }
    
    public double getMinY() {
        return minY;
    }
    
    public double getMaxZ() {
        return maxZ;
    }
    
    public double getMinZ() {
        return minZ;
    }
    
    // Methods
    public ArrayList<PointRecord> getPointRecordsInBoundingBox(BoundingBox bb) {
        double minXbb = bb.getMinX();
        double minYbb = bb.getMinY();
        double maxXbb = bb.getMaxX();
        double maxYbb = bb.getMaxY();
        double x, y;
        
        ArrayList<PointRecord> ret = new ArrayList<>();
        PointRecord rec;
        for (int i = 0; i < numPointRecords; i++) {
            rec = getPointRecord(i);
            x = rec.getX();
            y = rec.getY();
            if (maxYbb < y || maxXbb < x || minYbb > y || minXbb > x) {
                // do nothing
            } else {
                ret.add(rec);
            }
        }
        return ret;
    }
    
    public PointRecord getPointRecord(int i) {
        try {
            if (i < 0 || i > numPointRecords) {
                return null;
            }

            if (i < startingPoint || i > endingPoint) {
                startingPoint = i;
                endingPoint = i + bufferSize - 1;
                //read the points in
                readPointRecords();
            }


            int n = i - startingPoint;

            return pointRecs[n];
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }
    
    public PointRecColours getPointRecordColours(int i) {
        try {
            if (i < 0 || i > numPointRecords) {
                return null;
            }

            if (i < startingPoint || i > endingPoint) {
                startingPoint = i;
                endingPoint = i + bufferSize - 1;
                //read the points in
                readPointRecColours();
            }


            int n = i - startingPoint;

            return pointColours[n];
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }
    
    private void readPointRecords() {
        int pos = (int)(offsetToPointData + startingPoint * pointDataRecLength);
        int pos2 = 0;
        byte returnNumberByte = 0;
        byte classificationByte = 0;
        byte b = 0;
        pointRecs = new PointRecord[bufferSize];
        int x, y, z;
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        
        try {

            buf = ByteBuffer.allocate(bufferSize * pointDataRecLength);

            rIn = new RandomAccessFile(fileName, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(pos);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            
            for (int i = 0; i < bufferSize; i++) {
                pointRecs[i] = new PointRecord();
                x = buf.getInt(pos2);
                pointRecs[i].setX((x * xScale) + xOffset);
                y = buf.getInt(pos2 + 4);
                pointRecs[i].setY((y * yScale) + yOffset);
                z = buf.getInt(pos2 + 8);
                pointRecs[i].setZ((z * zScale) + zOffset);
                pointRecs[i].setIntensity(Unsigned.getUnsignedShort(buf, pos2 + 12));
                
                // get the record number byte
                returnNumberByte = buf.get(pos2 + 14);
                b = 0;
                for (int a = 0; a < 3; a++) {
                    if (BitOps.checkBit(returnNumberByte, a)) {
                        b = BitOps.setBit(b, (byte)a);
                    }
                }
                pointRecs[i].setReturnNumber(b);
                
                b = 0;
                for (int a = 0; a < 3; a++) {
                    if (BitOps.checkBit(returnNumberByte, a + 3)) {
                        b = BitOps.setBit(b, (byte)a);
                    }
                }
                pointRecs[i].setNumberOfReturns(b);
                
                pointRecs[i].setScanDirectionFlag(BitOps.checkBit(returnNumberByte, 6));
                pointRecs[i].setEdgeOfFlightLine(BitOps.checkBit(returnNumberByte, 7));
                
                // get the classification data
                classificationByte = buf.get(pos2 + 15);
                b = 0;
                for (int a = 0; a < 5; a++) {
                    if (BitOps.checkBit(classificationByte, a)) {
                        b = BitOps.setBit(b, (byte)a);
                    }
                }
                pointRecs[i].setClassification(b);
                pointRecs[i].setSynthetic(BitOps.checkBit(returnNumberByte, 5));
                pointRecs[i].setKeyPoint(BitOps.checkBit(returnNumberByte, 6));
                pointRecs[i].setPointWithheld(BitOps.checkBit(returnNumberByte, 7));
                pointRecs[i].setScanAngle(buf.get(pos2 + 16));
                pointRecs[i].setUserData(Unsigned.getUnsignedByte(buf, pos2 + 17));
                pointRecs[i].setPointSourceID(Unsigned.getUnsignedShort(buf, pos2 + 18));
                
                if (pointDataFormatID == 1 || pointDataFormatID == 3 || 
                        pointDataFormatID == 4 || pointDataFormatID == 5) {
                    pointRecs[i].setGPSTime(buf.getDouble(pos2 + 20));   
                }
                
                pos2 += pointDataRecLength;
            }
            
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    private void readPointRecColours() {
        if (pointDataFormatID == 2 || pointDataFormatID == 3 || pointDataFormatID == 5) {
            int pos = (int) (offsetToPointData + startingPoint * pointDataRecLength);
            int pos2 = 0;
            int offsetToColourData = 0;
            if (pointDataFormatID == 2) {
                offsetToColourData = 20;
            } else if (pointDataFormatID == 3 || pointDataFormatID == 5) {
                offsetToColourData = 28;
            }
            byte b = 0;
            pointColours = new PointRecColours[bufferSize];
            int x, y, z;
            RandomAccessFile rIn = null;
            ByteBuffer buf = null;

            try {

                buf = ByteBuffer.allocate(bufferSize * pointDataRecLength);

                rIn = new RandomAccessFile(fileName, "r");

                FileChannel inChannel = rIn.getChannel();

                inChannel.position(pos);
                inChannel.read(buf);

                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.rewind();

                for (int i = 0; i < bufferSize; i++) {
                    pointColours[i] = new PointRecColours();

                    pointColours[i].setRed(Unsigned.getUnsignedShort(buf, pos2 + offsetToColourData));
                    pointColours[i].setGreen(Unsigned.getUnsignedShort(buf, pos2 + offsetToColourData + 2));
                    pointColours[i].setBlue(Unsigned.getUnsignedShort(buf, pos2 + offsetToColourData + 4));

                    pos2 += pointDataRecLength;
                }

            } catch (Exception e) {
                System.err.println(e);
            } finally {
                if (rIn != null) {
                    try {
                        rIn.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
    
    public void setBufferSize(int i) {
        bufferSize = i;
        startingPoint = -1;
        endingPoint = -1;
    
        // reinitialize the point record arrays.
        pointRecs = new PointRecord[0];
        pointColours = new PointRecColours[0];
    }
    
    private boolean readHeaderData() {
        int pos;
        
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        
        try {

            // See if the data file exists.
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }

            buf = ByteBuffer.allocate(300);

            rIn = new RandomAccessFile(fileName, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();

            // the first four bytes should be the file signature "LASF"
            short[] sig = new short[4];
            sig[0] = Unsigned.getUnsignedByte(buf);
            sig[1] = Unsigned.getUnsignedByte(buf);
            sig[2] = Unsigned.getUnsignedByte(buf);
            sig[3] = Unsigned.getUnsignedByte(buf);
            short[] testSig = new short[]{76, 65, 83, 70};
            if (!Arrays.equals(sig, testSig)) {
                return false;
            }

            fileSourceID = Unsigned.getUnsignedShort(buf);

            int globalEncoding = Unsigned.getUnsignedShort(buf, 6);
            
            if (BitOps.checkBit(globalEncoding, 0)) {
                GPSTimeType = 1;
            }
            
            if (BitOps.checkBit(globalEncoding, 1)) {
                waveDataPacketsInternal = 1;
            }
            
            if (BitOps.checkBit(globalEncoding, 2)) {
                waveDataPacketsExternal = 1;
            }
            
            if (BitOps.checkBit(globalEncoding, 3)) {
                retNumsSynthGenerated = 1;
            }
            
            projectID1 = Unsigned.getUnsignedInt(buf, 8);
            projectID2 = Unsigned.getUnsignedShort(buf, 12);
            projectID3 = Unsigned.getUnsignedShort(buf, 14);
            pos = 16;
            for (int a = 0; a < 8; a++) {
                projectID4[a] = Unsigned.getUnsignedByte(buf, pos);
                pos += 1;
            }
            
            short[] tmp1 = new short[32];
            pos = 26;
            for (int a = 0; a < tmp1.length; a++) {
                tmp1[a] = Unsigned.getUnsignedByte(buf, pos);
                pos += 1;
            }
            
            systemIdentifer = convertShortArrayToAscii(tmp1);
            
            tmp1 = new short[32];
            pos = 58;
            for (int a = 0; a < tmp1.length; a++) {
                tmp1[a] = Unsigned.getUnsignedByte(buf, pos);
                pos += 1;
            }
            
            generatingSoftware = convertShortArrayToAscii(tmp1);
            
            versionMajor = Unsigned.getUnsignedByte(buf, 24);
            versionMinor = Unsigned.getUnsignedByte(buf, 25);

            fileCreationDay = Unsigned.getUnsignedShort(buf, 90);
            fileCreationYear = Unsigned.getUnsignedShort(buf, 92);
            headerSize = Unsigned.getUnsignedShort(buf, 94);
            offsetToPointData = Unsigned.getUnsignedInt(buf, 96);
            numVLR = Unsigned.getUnsignedInt(buf, 100);
            pointDataFormatID = Unsigned.getUnsignedByte(buf, 104);
            pointDataRecLength = Unsigned.getUnsignedShort(buf, 105);
            numPointRecords = Unsigned.getUnsignedInt(buf, 107);
            pos = 111;
            for (int a = 0; a < 5; a++) {
                numPointsByReturn[a] = Unsigned.getUnsignedInt(buf, pos);
                pos += 4;
            }

            xScale = buf.getDouble(131);
            yScale = buf.getDouble(139);
            zScale = buf.getDouble(147);

            xOffset = buf.getDouble(155);
            yOffset = buf.getDouble(163);
            zOffset = buf.getDouble(171);

            maxX = buf.getDouble(179);
            minX = buf.getDouble(187);

            maxY = buf.getDouble(195);
            minY = buf.getDouble(203);

            maxZ = buf.getDouble(211);
            minZ = buf.getDouble(219);

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
    
    private boolean readVariableLengthRecords() {
        int pos = headerSize;
        short[] tmp1;
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        
        try {

            // See if the data file exists.
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }

            buf = ByteBuffer.allocate((int)offsetToPointData);

            rIn = new RandomAccessFile(fileName, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            
            for (int a = 0; a < numVLR; a++) {
                VariableLengthRecord vlr = new VariableLengthRecord();
                /* Nothing is done with this reserved byt at the moment, but it 
                 * may be useful in future versions of the specification */
                int reservedByte = Unsigned.getUnsignedShort(buf, pos);
                
                // UserID--16 byte ASCII field
                tmp1 = new short[16];
                int m = 2; // starts at pos + 2
                for (int j = 0; j < tmp1.length; j++) {
                    tmp1[j] = Unsigned.getUnsignedByte(buf, pos + m);
                    m++;
                }
                
                String userID = convertShortArrayToAscii(tmp1);
                vlr.setUserID(userID);
                
                // RecordID--starts at pos + 18
                //int i = Unsigned.getUnsignedShort(buf, pos + 18);
                vlr.setRecordID(Unsigned.getUnsignedShort(buf, pos + 18));
                
                // RecordLengthAfterHeader--starts at pos + 20
                //int k = Unsigned.getUnsignedShort(buf, pos + 20);
                vlr.setRecordLengthAfterHeader(Unsigned.getUnsignedShort(buf, pos + 20));
                
                // Description--32 byte ASCII field starting at pos + 22
                tmp1 = new short[32];
                m = 22;
                for (int j = 0; j < tmp1.length; j++) {
                    tmp1[j] = Unsigned.getUnsignedByte(buf, pos + m);
                    m++;
                }
                String description = convertShortArrayToAscii(tmp1);
                vlr.setDescription(description);
                
                // Read the raw data contained in the vlr
                byte[] rawData = new byte[vlr.getRecordLengthAfterHeader()];
                buf.position(pos + 54);
                buf.get(rawData);
                vlr.setRawData(rawData);
                
                vlrArray.add(vlr);
                
                pos += 54 + vlr.getRecordLengthAfterHeader();
            }
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
    
    private String convertShortArrayToAscii(short[] array) {
        String str = "";
        char[] charArray = new char[array.length];
        
        for (int a = 0; a < array.length; a++) {
            switch (array[a]) {
                case 0:
                    charArray[a] = " ".charAt(0);
                    break;
                    
                case 32:
                    charArray[a] = " ".charAt(0);
                    break;
                case 33:
                    charArray[a] = "!".charAt(0);
                    break;
                case 34:
                    charArray[a] = "\"".charAt(0);
                    break;
                case 35:
                    charArray[a] = "#".charAt(0);
                    break;
                case 36:
                    charArray[a] = "$".charAt(0);
                    break;
                case 37:
                    charArray[a] = "%".charAt(0);
                    break;
                case 38:
                    charArray[a] = "&".charAt(0);
                    break;
                case 39:
                    charArray[a] = "\'".charAt(0);
                    break;
                case 40:
                    charArray[a] = "(".charAt(0);
                    break;
                case 41:
                    charArray[a] = ")".charAt(0);
                    break;
                case 42:
                    charArray[a] = "*".charAt(0);
                    break;
                case 43:
                    charArray[a] = "+".charAt(0);
                    break;
                case 44:
                    charArray[a] = ",".charAt(0);
                    break;
                case 45:
                    charArray[a] = "-".charAt(0);
                    break;
                case 46:
                    charArray[a] = ".".charAt(0);
                    break;
                case 47:
                    charArray[a] = "/".charAt(0);
                    break;
                case 48:
                    charArray[a] = "0".charAt(0);
                    break;
                case 49:
                    charArray[a] = "1".charAt(0);
                    break;
                case 50:
                    charArray[a] = "2".charAt(0);
                    break;
                case 51:
                    charArray[a] = "3".charAt(0);
                    break;
                case 52:
                    charArray[a] = "4".charAt(0);
                    break;
                case 53:
                    charArray[a] = "5".charAt(0);
                    break;
                case 54:
                    charArray[a] = "6".charAt(0);
                    break;
                case 55:
                    charArray[a] = "7".charAt(0);
                    break;
                case 56:
                    charArray[a] = "8".charAt(0);
                    break;
                case 57:
                    charArray[a] = "9".charAt(0);
                    break;
                case 58:
                    charArray[a] = ":".charAt(0);
                    break;
                case 59:
                    charArray[a] = ";".charAt(0);
                    break;
                case 60:
                    charArray[a] = "<".charAt(0);
                    break;
                case 61:
                    charArray[a] = "=".charAt(0);
                    break;
                case 62:
                    charArray[a] = ">".charAt(0);
                    break;
                case 63:
                    charArray[a] = "?".charAt(0);
                    break;
                case 64:
                    charArray[a] = "@".charAt(0);
                    break;
                case 65:
                    charArray[a] = "A".charAt(0);
                    break;
                case 66:
                    charArray[a] = "B".charAt(0);
                    break;
                case 67:
                    charArray[a] = "C".charAt(0);
                    break;
                case 68:
                    charArray[a] = "D".charAt(0);
                    break;
                case 69:
                    charArray[a] = "E".charAt(0);
                    break;
                case 70:
                    charArray[a] = "F".charAt(0);
                    break;
                case 71:
                    charArray[a] = "G".charAt(0);
                    break;
                case 72:
                    charArray[a] = "H".charAt(0);
                    break;
                case 73:
                    charArray[a] = "I".charAt(0);
                    break;
                case 74:
                    charArray[a] = "J".charAt(0);
                    break;
                case 75:
                    charArray[a] = "K".charAt(0);
                    break;
                case 76:
                    charArray[a] = "L".charAt(0);
                    break;
                case 77:
                    charArray[a] = "M".charAt(0);
                    break;
                case 78:
                    charArray[a] = "N".charAt(0);
                    break;
                case 79:
                    charArray[a] = "O".charAt(0);
                    break;
                case 80:
                    charArray[a] = "P".charAt(0);
                    break;
                case 81:
                    charArray[a] = "Q".charAt(0);
                    break;
                case 82:
                    charArray[a] = "R".charAt(0);
                    break;
                case 83:
                    charArray[a] = "S".charAt(0);
                    break;
                case 84:
                    charArray[a] = "T".charAt(0);
                    break;
                case 85:
                    charArray[a] = "U".charAt(0);
                    break;
                case 86:
                    charArray[a] = "V".charAt(0);
                    break;
                case 87:
                    charArray[a] = "W".charAt(0);
                    break;
                case 88:
                    charArray[a] = "X".charAt(0);
                    break;
                case 89:
                    charArray[a] = "Y".charAt(0);
                    break;
                case 90:
                    charArray[a] = "Z".charAt(0);
                    break;
                case 91:
                    charArray[a] = "[".charAt(0);
                    break;
                case 92:
                    charArray[a] = "\\".charAt(0);
                    break;
                case 93:
                    charArray[a] = "]".charAt(0);
                    break;
                case 94:
                    charArray[a] = "^".charAt(0);
                    break;
                case 95:
                    charArray[a] = "_".charAt(0);
                    break;
                case 96:
                    charArray[a] = "`".charAt(0);
                    break;
                case 97:
                    charArray[a] = "a".charAt(0);
                    break;
                case 98:
                    charArray[a] = "b".charAt(0);
                    break;
                case 99:
                    charArray[a] = "c".charAt(0);
                    break;
                case 100:
                    charArray[a] = "d".charAt(0);
                    break;
                case 101:
                    charArray[a] = "e".charAt(0);
                    break;
                case 102:
                    charArray[a] = "f".charAt(0);
                    break;
                case 103:
                    charArray[a] = "g".charAt(0);
                    break;
                case 104:
                    charArray[a] = "h".charAt(0);
                    break;
                case 105:
                    charArray[a] = "i".charAt(0);
                    break;
                case 106:
                    charArray[a] = "j".charAt(0);
                    break;
                case 107:
                    charArray[a] = "k".charAt(0);
                    break;
                case 108:
                    charArray[a] = "l".charAt(0);
                    break;
                case 109:
                    charArray[a] = "m".charAt(0);
                    break;
                case 110:
                    charArray[a] = "n".charAt(0);
                    break;
                case 111:
                    charArray[a] = "o".charAt(0);
                    break;
                case 112:
                    charArray[a] = "p".charAt(0);
                    break;
                case 113:
                    charArray[a] = "q".charAt(0);
                    break;
                case 114:
                    charArray[a] = "r".charAt(0);
                    break;
                case 115:
                    charArray[a] = "s".charAt(0);
                    break;
                case 116:
                    charArray[a] = "t".charAt(0);
                    break;
                case 117:
                    charArray[a] = "u".charAt(0);
                    break;
                case 118:
                    charArray[a] = "v".charAt(0);
                    break;
                case 119:
                    charArray[a] = "w".charAt(0);
                    break;
                case 120:
                    charArray[a] = "x".charAt(0);
                    break;
                case 121:
                    charArray[a] = "y".charAt(0);
                    break;
                case 122:
                    charArray[a] = "z".charAt(0);
                    break;
                case 123:
                    charArray[a] = "{".charAt(0);
                    break;
                case 124:
                    charArray[a] = "|".charAt(0);
                    break;
                case 125:
                    charArray[a] = "}".charAt(0);
                    break;
                case 126:
                    charArray[a] = "~".charAt(0);
                    break;
                
                default:
                    charArray[a] = " ".charAt(0);
                    break;
                
            }
        }
        str = String.valueOf(charArray);
        return str;
    }
    
    
    // related classes
    private class VariableLengthRecord {
        // fields
        String userID;
        int recordID;
        int recordLengthAfterHeader;
        String description;
        
        byte[] rawData;
        
        // property getters and setters
        String getUserID() {
            return userID;
        }
        
        void setUserID(String userID) {
            this.userID = userID;
        }
        
        int getRecordID() {
            return recordID;
        }
        
        void setRecordID(int recordID) {
            this.recordID = recordID;
        }
        
        int getRecordLengthAfterHeader() {
            return recordLengthAfterHeader;
        }
        
        void setRecordLengthAfterHeader(int recordLengthAfterHeader) {
            this.recordLengthAfterHeader = recordLengthAfterHeader;
        }
        
        String getDescription() {
            return description;
        }
        
        void setDescription(String description) {
            this.description = description;
        }
        
        byte[] getRawData() {
            return rawData;
        }
        
        void setRawData(byte[] rawData) {
            this.rawData = rawData;
        }
    }
    
    public class PointRecord {
        private double x; //8 bytes
        private double y; //8 bytes
        private double z; //8 bytes
        private int intensity; //4 bytes
        private byte classification; //2 bytes
        private byte returnNumber; //1 byte
        private byte numberOfReturns; //1 byte
        private boolean scanDirectionFlag; //1 byte
        private boolean edgeOfFlightLine; //1 byte
        private boolean synthetic; //1 byte
        private boolean keyPoint; //1 byte
        private boolean pointWithheld; //1 byte
        private byte scanAngle; //1 byte
        private short userData; //2 bytes
        private int pointSourceID; //4 bytes
        private double GPSTime = -1; //8 bytes
        
        public double getX() {
            return x;
        }
        
        public void setX(double x) {
            this.x = x;
        }
        
        public double getY() {
            return y;
        }
        
        public void setY(double y) {
            this.y = y;
        }
        
        public double getZ() {
            return z;
        }
        
        public void setZ(double z) {
            this.z = z;
        }
        
        public int getIntensity() {
            return intensity;
        }
        
        public void setIntensity(int i) {
            this.intensity = i;
        }
        
        public byte getClassification() {
            return classification;
        }
        
        public void setClassification(byte i) {
            this.classification = i;
        }
        
        public byte getReturnNumber() {
            return returnNumber;
        }
        
        public void setReturnNumber(byte n) {
            this.returnNumber = n;
        }
        
        public byte getNumberOfReturns() {
            return numberOfReturns;
        }
        
        public void setNumberOfReturns(byte n) {
            numberOfReturns = n;
        }
        
        public boolean getScanDirectionFlag() {
            return scanDirectionFlag;
        }
        
        public void setScanDirectionFlag(boolean val) {
            scanDirectionFlag = val;
        }
        
        public boolean isEdgeOfFlightLine() {
            return edgeOfFlightLine;
        }
        
        public void setEdgeOfFlightLine(boolean val) {
            edgeOfFlightLine = val;
        }
        
        public boolean isSynthetic() {
            return synthetic;
        }
        
        public void setSynthetic(boolean val) {
            synthetic = val;
        }
        
        public boolean isKeyPoint() {
            return keyPoint;
        }
        
        public void setKeyPoint(boolean val) {
            keyPoint = val;
        }
        
        public boolean isPointWithheld() {
            return pointWithheld;
        }
        
        public void setPointWithheld(boolean val) {
            pointWithheld = val;
        }
        
        public byte getScanAngle() {
            return scanAngle;
        }
        
        public void setScanAngle(byte a) {
            scanAngle = a;
        }
        
        public short getUserData() {
            return userData;
        }
        
        public void setUserData(short d) {
            userData = d;
        }
        
        public int getPointSourceID() {
            return pointSourceID;
        }
        
        public void setPointSourceID(int id) {
            pointSourceID = id;
        }
        
        public double getGPSTime() {
            return GPSTime;
        }
        
        public void setGPSTime(double t) {
            GPSTime = t;
        }
    }
    
    public class PointRecColours {
        private int red = -1; //2 bytes
        private int green = -1; //2 bytes
        private int blue = -1; //2 bytes
        
        public int getRed() {
            return red;
        }
        
        public void setRed(int r) {
            red = r;
        }
        
        public int getGreen() {
            return green;
        }
        
        public void setGreen(int g) {
            green = g;
        }
        
        public int getBlue() {
            return blue;
        }
        
        public void setBlue(int b) {
            blue = b;
        }
    }
    
//    public class PointRecord {
//        private double x; //8 bytes
//        private double y; //8 bytes
//        private double z; //8 bytes
//        private int intensity; //4 bytes
//        private byte returnNumber; //1 byte
//        private byte numberOfReturns; //1 byte
//        private byte scanDirectionFlag; //1 byte
//        private byte edgeOfFlightLine; //1 byte
//        private byte classification; //1 bytes
//        private short scanAngleRank; //2 bytes
//        private byte userData; //1 bytes
//        private int pointSourceID; //4 bytes
//        private double GPSTime; //8 bytes
//        private int red; //2 bytes
//        private int green; //2 bytes
//        private int blue; //2 bytes
//        private byte pointWithheld; //1 byte
//        private byte isSynthetic; //1 byte
//        private byte isKeyPoint; //1 byte
//        
//        public double getX() {
//            return x;
//        }
//        
//        public void setX(double x) {
//            this.x = x;
//        }
//    }
    
//    // This is used for debugging.
//    public static void main(String[] args) {
//        int i = 0;
//        String lasFile = "/Users/johnlindsay/Documents/Data/u_5565073175.las";
//        if (!(new File(lasFile)).exists()) {
//            System.out.println("File not found");
//        } else {
//            LASReader las = new LASReader(lasFile);
//            for (int a = 0; a < las.getNumPointRecords(); a++) {
//                //PointRecord pr = las.getPointRecord(a);
//                //System.out.println(pr.getZ() + " " + pr.getIntensity() + " " + pr.getClassification() + " " + pr.getReturnNumber() + " " + pr.getNumberOfReturns());
//                i++;
//            }
//        }
//        System.out.println(i);
//    }
}
