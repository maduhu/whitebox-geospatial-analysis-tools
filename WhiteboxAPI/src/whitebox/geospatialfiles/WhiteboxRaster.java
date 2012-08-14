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
 

import java.util.Arrays;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

/**
 * The whiteboxRaster is used to manipulate Whitebox GAT raster files (.dep and .tas).
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class WhiteboxRaster extends WhiteboxRasterBase {


    // ************************
    // Fields
    // ************************
    private double[] grid;
    private int blockSize = 0;
    private long blockStartingCell = 0;
    private long blockEndingCell = -1;
    private double initialValue;
    private boolean isDirty = false;

    /**
     * Set to false if the header and data files (.dep and .tas) should be deleted
     * when the object is closed.
     */
    public boolean isTemporaryFile = false;
    
    /**
     * Set to true when the getValue function should reflect beyond the edges.
     */
    public boolean isReflectedAtEdges = false;
    

    // ************************
    // Constructors
    // ************************


    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess(FileAccess);
        readHeaderFile();
        setBlockData();
        
    }

    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     * @param BufferSize Determines the how much data can be stored in memory.
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess, double BufferSize)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess(FileAccess);
        setBufferSize(BufferSize);
        readHeaderFile();
        setBlockData();
    }

    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     * @param BaseRasterHeader The name of a WhiteboxRaster header file to base this new object on.
     * @param DataType The data type of the new WhiteboxRaster. Can be 'double', 'float', 'integer', or 'byte'
     * @param InitialValue Double indicating the value used to initialize the grid. It is recommended to use the noDataValue.
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, DataType dataType, double InitialValue)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        File f1 = new File(this.headerFile);
        f1.delete();
        f1 = new File(this.dataFile);
        f1.delete();
        f1 = new File(this.statsFile);
        f1.delete();
        initialValue = InitialValue;
        setFileAccess(FileAccess);
        setPropertiesUsingAnotherRaster(BaseRasterHeader, dataType);
        setBlockData();

    }

    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     * @param BaseRasterHeader The name of a WhiteboxRaster header file to base this new object on.
     * @param DataType The data type of the new WhiteboxRaster. Can be 'double', 'float', 'integer', or 'byte'
     * @param InitialValue Double indicating the value used to initialize the grid. It is recommended to use the noDataValue.
     * @param BufferSize Determines how much data can be stored in memory.
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, DataType dataType, double InitialValue, double BufferSize)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        File f1 = new File(this.headerFile);
        f1.delete();
        f1 = new File(this.dataFile);
        f1.delete();
        f1 = new File(this.statsFile);
        f1.delete();
        initialValue = InitialValue;
        setFileAccess(FileAccess);
        setBufferSize(BufferSize);
        setPropertiesUsingAnotherRaster(BaseRasterHeader, dataType);
        setBlockData();

    }
    
    
    public WhiteboxRaster(String HeaderFile, double north, double south, double east, double west, int rows, int cols, DataScale dataScale, DataType dataType, double initialValue, double noData)
    {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        File f1 = new File(this.headerFile);
        f1.delete();
        f1 = new File(this.dataFile);
        f1.delete();
        f1 = new File(this.statsFile);
        f1.delete();
        
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.numberRows = rows;
        this.numberColumns = cols;
        this.dataScale = dataScale;
        setDataType(dataType);
        this.noDataValue = noData;
        writeHeaderFile();
        
        this.initialValue = initialValue;
        setFileAccess("rw");
        setBlockData();
        //createNewDataFile();
        
    }


    // ***********************************
    // Property getter and setter methods.
    // ***********************************

    private long bufferSize = 100 * 1048576; //Runtime.getRuntime().totalMemory(); //in bytes
    /**
     * Retrieves the maximum memory usage for this Whitebox grid in megabytes.
     * @return Maximum memory.
     */
    public double getBufferSize() {
        return bufferSize / 1048576;
    }
    /**
     * Sets maximum memory usage for this Whitebox grid in megabytes.
     * @param BufferSize maximum memory usage.
     */
    private void setBufferSize(double BufferSize) {
        bufferSize = (long) (BufferSize * 1048576);
    }

    /**
     * Retrieves the block size contained in memory.
     * @return Long containing block size
     */
    public long getBlockSize() {
        return blockSize;
    }

    private long numberOfDataFileReads = 0;
    /**
     * The number of times that the data file (.tas) has been read by this object.
     * @return long stating the number of reads.
     */
    public long getNumberOfDataFileReads() {
        return numberOfDataFileReads;
    }

    private long numberOfDataFileWrites = 0;
    /**
     * The number of times that the data file (.tas) has been written by this object.
     * @return long stating the number of reads.
     */
    public long getNumberOfDataFileWrites() {
        return numberOfDataFileWrites;
    }

//    private double stdDeviation = noDataValue;
//    public double getStandardDeviation() {
//        if (stdDeviation == noDataValue) {
//            readStatsFile();
//        }
//        return stdDeviation;
//    }
//    
//    private double mode = noDataValue;
//    public double getMode() {
//        if (mode == noDataValue) {
//            readStatsFile();
//        }
//        return mode;
//    }
//    
//    private double mean = noDataValue;
//    public double getMean() {
//        if (mean == noDataValue) {
//            readStatsFile();
//        }
//        return mean;
//    }
//    
//    private double median = noDataValue;
//    public double getMedian() {
//        if (median == noDataValue) {
//            readStatsFile();
//        }
//        return median;
//    }
//    
//    private long[] histo = null;
//    public long[] getHisto() {
//        if (mean == noDataValue) {
//            readStatsFile();
//        }
//        return histo;
//    }
//    
//    private double binWidth = noDataValue;
//    public double getHistoBinWidth() {
//        if (binWidth == noDataValue) {
//            readStatsFile();
//        }
//        return binWidth;
//    }
//    
//    private long numValidCells = (long)noDataValue;
//    public long getnumValidCells() {
//        if (numValidCells == (long)noDataValue) {
//            readStatsFile();
//        }
//        return numValidCells;
//    }
    



    //********************************************
    // Available methods.
    // *******************************************
    
//    /**
//     * This method should be used when you need to access an entire row of data
//     * at a time. It has less overhead that the getValue method and can be used
//     * to efficiently scan through a raster image row by row. It will read the 
//     * specified row from disk and will not store it internally within the
//     * WhiteboxRaster. As such, this method is appropriate when each of the cells
//     * in the raster need to be accessed sequentially one time only. This is the
//     * case, for example, when the raster is displayed as an image.
//     * @param row An int stating the zero-based row to be returned.
//     * @return An array of doubles containing the values store in the specified row.
//     */
//
//    public double[] getRowValues(int row) {
//        if (row < 0 || row >= numberRows) { return null; }
//        
//        double[] retVals = new double[numberColumns];
//        RandomAccessFile rIn = null;
//        ByteBuffer buf = null;
//        FileChannel inChannel = null;
//        try {
//
//            // See if the data file exists.
//            File file = new File(dataFile);
//            if (!file.exists()) {
//                createNewDataFile();
//            }
//            
//            // what is the starting and ending cell?
//            long startingCell = row * numberColumns;
//            long endingCell = startingCell + numberColumns - 1;
//            int readLengthInCells = (int)(endingCell - startingCell + 1);
//            
//            buf = ByteBuffer.allocateDirect((int) (readLengthInCells * cellSizeInBytes));
//            rIn = new RandomAccessFile(dataFile, "r");
//            
//            inChannel = rIn.getChannel();
//            inChannel.position(startingCell * cellSizeInBytes);
//            inChannel.read(buf);
//
//            // Check the byte order.
//            buf.order(byteOrder);
//
//            
//            if (dataType == DataType.DOUBLE) { //.equals("double")) {
//                buf.rewind();
//                DoubleBuffer db = buf.asDoubleBuffer();
//                retVals = new double[readLengthInCells];
//                db.get(retVals);
//                db = null;
//                buf = null;
//            } else if (dataType == DataType.FLOAT) { //.equals("float")) {
//                buf.rewind();
//                FloatBuffer fb = buf.asFloatBuffer();
//                float[] fa = new float[readLengthInCells];
//                fb.get(fa);
//                fb = null;
//                buf = null;
//                retVals = new double[readLengthInCells];
//                for (int j = 0; j < readLengthInCells; j++) {
//                    retVals[j] = fa[j];
//                }
//                fa = null;
//            } else if (dataType == DataType.INTEGER) { //.equals("integer")) {
//                buf.rewind();
//                ShortBuffer ib = buf.asShortBuffer();
//                short[] ia = new short[readLengthInCells];
//                ib.get(ia);
//                ib = null;
//                buf = null;
//                retVals = new double[readLengthInCells];
//                for (int j = 0; j < readLengthInCells; j++) {
//                    retVals[j] = ia[j];
//                }
//                ia = null;
//            } else if (dataType == DataType.BYTE) { //.equals("byte")) {
//                buf.rewind();
//                byte[] ba = new byte[readLengthInCells];
//                buf.get(ba);
//                buf = null;
//                retVals = new double[readLengthInCells];
//                for (int j = 0; j < readLengthInCells; j++) {
//                    retVals[j] = ba[j];
//                }
//                ba = null;
//            }
//
//        } catch (Exception e) {
//            System.err.println("Caught exception: " + e.toString());
//            System.err.println(e.getStackTrace());
//        } finally {
//            if (rIn != null) {
//                try { rIn.close(); } catch (Exception e) {}
//            }
//            if (inChannel != null) {
//                try { inChannel.close(); } catch (Exception e) {};
//            }
//            numberOfDataFileReads++;
//            return retVals.clone();
//        }
//        
//        
//    }
    
    /**
     * This method should be used when you need to set an entire row of data
     * at a time. It has less overhead that the setValue method (which works
     * on a pixel-by-pixel basis) and can be used to efficiently scan through 
     * a raster image row by row.
     * @param row An int stating the zero-based row to be returned.
     * @param vals An array of doubles containing the values store in the specified row.
     */
    public void setRowValues(int row, double[] vals) {
        if (!saveChanges) { return; }
        if (vals.length != numberColumns) { return; } 
        
        // update the minimum and maximum values
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < numberColumns; i++) {
            if (vals[i] < min && vals[i] != noDataValue) { min = vals[i]; }
            if (vals[i] > max && vals[i] != noDataValue) { max = vals[i]; }
        }
        if (max > maximumValue) { maximumValue = max; }
        if (min < minimumValue) { minimumValue = min; }
        
        RandomAccessFile rOut = null;
        FileChannel outChannel = null;
        ByteBuffer buf = null;
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (!file.exists()) {
                createNewDataFile();
            }
            long startingCell = row * numberColumns;
            long endingCell = startingCell + numberColumns - 1;
            
            rOut = new RandomAccessFile(dataFile, "rw");
            outChannel = rOut.getChannel();
            outChannel.position(startingCell * cellSizeInBytes);
            int writeLengthInCells = (int) (endingCell - startingCell + 1);

            if (dataType == DataType.DOUBLE) { //.equals("double")) {
                buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                DoubleBuffer db = buf.asDoubleBuffer();
                db.put(grid[row]);
                db = null;
                outChannel.write(buf);
            } else if (dataType == DataType.FLOAT) { //.equals("float")) {
                float[] fa = new float[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    fa[j] = (float)vals[j];
                }
                buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                FloatBuffer fb = buf.asFloatBuffer();
                fb.put(fa);
                fb = null;
                fa = null;
                outChannel.write(buf);
            } else if (dataType == DataType.INTEGER) { //.equals("integer")) {
                short[] ia = new short[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ia[j] = (short)vals[j];
                }
                buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                ShortBuffer ib = buf.asShortBuffer();
                ib.put(ia);
                ib = null;
                ia = null;
                outChannel.write(buf);
            } else if (dataType == DataType.BYTE) { //.equals("byte")) {
                byte[] ba = new byte[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ba[j] = (byte)vals[j];
                }
                buf = ByteBuffer.wrap(ba);
                ba = null;
                outChannel.write(buf);
            }
            //outChannel.close();

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            buf = null;
            if (rOut != null) {
                try { rOut.close(); } catch (Exception e){}
            }
            if (outChannel != null) {
                try { 
                    outChannel.force(false);
                    outChannel.close(); 
                } catch (Exception e) {}
            }
            numberOfDataFileWrites++;
        }
    }


    private int previousRow = 0;
    private int currentReadDirection = -1;
    private int numSwitchReadDirections = 0;
    private int numReads = 0;
    private double switchRatio = 0;
    private int halfBlockSize = 0;
    //private int readDirectionTendency = 0;
    /**
     * Retrieves the value contained at a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @return The value contained in the raster grid at the specified grid cell.
     */
    public double getValue(int row, int column){
        if (column >= 0 && column < numberColumns && row >= 0 && row < numberRows) {
            
            if (blockEndingCell < 0) { readDataBlock(); }
            
            
            // what is the cell number?
            int cellNum = row * numberColumns + column;

            // check to see if it is within the current block
            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (saveChanges && isDirty) { writeDataBlock(); }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) { // reading downward
                    if (currentReadDirection == -1) { currentReadDirection = 0; }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - halfBlockSize * switchRatio); //10 * numberColumns);
                } else { // reading upward
                    if (currentReadDirection == -1) { currentReadDirection = 1; }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - (blockSize - (switchRatio * halfBlockSize))); //+ (blockSize / 2) * ((double)upReadDirection / downReadDirection)); // + 10 * numberColumns - blockSize);
                }
                previousRow = row;
                //blockStartingCell = (int)(cellNum - blockSize / 2);
                if (blockStartingCell < 0) { blockStartingCell = 0; }

                 readDataBlock();
            }
           
        
            return grid[(int)(cellNum - blockStartingCell)];
        } else {
            if (!isReflectedAtEdges) { return noDataValue; }
            
            // if you get to this point, it is reflected at the edges
            if (row < 0) { row = -row; }
            if (row >= numberRows) { row = numberRows - (row - numberRows); }
            if (column < 0) { column = -column; }
            if (column >= numberColumns) { column = numberColumns - (column - numberColumns); }
            if (column >= 0 && column < numberColumns && row >= 0 && row < numberRows) {
                return getValue(row, column);
            } else {
                // it was too off grid to be reflected.
                return noDataValue;
            }
        }
    }

    /**
     * Sets the value of a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to place in the grid cell.
     */
    public void setValue(int row, int column, double value){
        if (saveChanges && column >= 0 && column < this.numberColumns 
                && row >= 0 && row < this.numberRows) {
            // what is the cell number?
            int cellNum = row * numberColumns + column;

            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (isDirty) { writeDataBlock(); }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) {
                    if (currentReadDirection == -1) { currentReadDirection = 0; }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - halfBlockSize * switchRatio); 
                } else {
                    if (currentReadDirection == -1) { currentReadDirection = 1; }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - (blockSize - (switchRatio * halfBlockSize))); 
                }
                previousRow = row;
                if (blockStartingCell < 0) { blockStartingCell = 0; }
                readDataBlock();
            }

            grid[(int)(cellNum - blockStartingCell)] = value;
            isDirty = true;
        }
    }
    
    /**
     * Increments the value of a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to increment the grid cell by.
     */
    public void incrementValue(int row, int column, double value){
        if (saveChanges && column >= 0 && column < this.numberColumns 
                && row >= 0 && row < this.numberRows) {
            // what is the cell number?
            int cellNum = row * numberColumns + column;

            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (isDirty) { writeDataBlock(); }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) {
                    if (currentReadDirection == -1) { currentReadDirection = 0; }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - halfBlockSize * switchRatio); 
                } else {
                    if (currentReadDirection == -1) { currentReadDirection = 1; }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - (blockSize - (switchRatio * halfBlockSize))); 
                }
                previousRow = row;
                if (blockStartingCell < 0) { blockStartingCell = 0; }
                readDataBlock();
            }

            grid[(int)(cellNum - blockStartingCell)] += value;
            isDirty = true;
        }
    }

    /**
     * Decrements the value of a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to decrement the grid cell by.
     */
    public void decrementValue(int row, int column, double value){
        if (saveChanges && column >= 0 && column < this.numberColumns 
                && row >= 0 && row < this.numberRows) {
            // what is the cell number?
            int cellNum = row * numberColumns + column;

            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (isDirty) { writeDataBlock(); }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) {
                    if (currentReadDirection == -1) { currentReadDirection = 0; }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - halfBlockSize * switchRatio); 
                } else {
                    if (currentReadDirection == -1) { currentReadDirection = 1; }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double)numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (int)(cellNum - (blockSize - (switchRatio * halfBlockSize))); 
                }
                previousRow = row;
                if (blockStartingCell < 0) { blockStartingCell = 0; }
                readDataBlock();
            }

            grid[(int)(cellNum - blockStartingCell)] -= value;
            isDirty = true;
        }
    }
    
    private void setBlockData() {
        try {
            // see if the data can be comfortably contained in memory, keeping in
            // mind that it is always stored as doubles.
            System.gc();
            long availableMemory = Runtime.getRuntime().freeMemory();
            long gridMemoryRequirements = (long)numberRows * (long)numberColumns * 8L;
            if ((availableMemory / 3) > gridMemoryRequirements) {
                // store the entire grid in memory.
                blockSize = numberRows * numberColumns;
                bufferSize = gridMemoryRequirements;
            } else if (((double)gridMemoryRequirements / (availableMemory / 3)) > 2) {
                // the data doesn't come close to fitting in the available memory.
                bufferSize = 100 * 1048576;
                blockSize = (int) (Math.round(Math.floor(bufferSize / 8))) / 3;
            } else {
                blockSize = (numberRows * numberColumns) / 2;
                bufferSize = ((numberRows * numberColumns) / 2) * 8;
            }
            
            /*
            blockSize = (int) (Math.round(Math.floor(bufferSize / 8))) / 3;
            if (blockSize > (numberRows * numberColumns)) {
                blockSize = numberRows * numberColumns;
                halfBlockSize = blockSize / 2;
                bufferSize = blockSize * 8;
            }
             * 
             */
            
            halfBlockSize = blockSize / 2;
            blockStartingCell = 0;
            //readDataBlock();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void createNewDataFile() {
        RandomAccessFile rOut = null;
        ByteBuffer buf = null;
        FileChannel outChannel = null;
        try {
            long numberCells = numberRows * numberColumns;
            int writeLength = 2000000;
            if (writeLength > numberCells) { writeLength = (int)numberCells; }
            long numCellsWritten = 0;
            
            rOut = new RandomAccessFile(dataFile, "rws");

            outChannel = rOut.getChannel();
            outChannel.position(0);

            buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
            buf.order(byteOrder);
                
            if (dataType == DataType.DOUBLE) { //.equals("double")) {
                double[] da;
                DoubleBuffer db = buf.asDoubleBuffer();
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                        db = buf.asDoubleBuffer();
                    }
                    da = new double[writeLength];
                    if (initialValue != 0) { Arrays.fill(da, initialValue); }
                    //buf = ByteBuffer.allocate(cellSizeInBytes * writeLength);
                    //buf.order(byteOrder);
                    buf.clear();
                    db.clear();
                    db.put(da);
                    outChannel.write(buf);
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
                db = null;
                da = null;
                    
            } else if (dataType == DataType.FLOAT) { //.equals("float")) {
                //buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                //buf.order(byteOrder);
                float[] fa;
                FloatBuffer fb = buf.asFloatBuffer();
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                        fb = buf.asFloatBuffer();
                    }
                    fa = new float[writeLength];
                    if (initialValue != 0) { Arrays.fill(fa, (float)initialValue); }
                    //buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                    //buf.order(byteOrder);
                    buf.clear();
                    fb.clear();
                    fb.put(fa);
                    outChannel.write(buf);
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
                fb = null;
                fa = null;
                    
            } else if (dataType == DataType.INTEGER) { //.equals("integer")) {
                short[] ia;
                ShortBuffer ib = buf.asShortBuffer();
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                        ib = buf.asShortBuffer();
                    }
                    ia = new short[(int)writeLength];
                    if (initialValue != 0) { Arrays.fill(ia, (short)initialValue); }
                    //buf = ByteBuffer.allocate(cellSizeInBytes * writeLength);
                    //buf.order(byteOrder);
                    buf.clear();
                    ib.clear();
                    ib.put(ia);
                    outChannel.write(buf);
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
                ib = null;
                ia = null;
                    
            } else if (dataType == DataType.BYTE) { //.equals("byte")) {
                byte[] ba;
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                    }
                    ba = new byte[writeLength];
                    if (initialValue != 0) { Arrays.fill(ba, (byte)initialValue); }
                    buf = ByteBuffer.wrap(ba);
                    buf.flip();
                    outChannel.write(buf);
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
                ba = null;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (outChannel != null) {
                try { 
                    outChannel.force(false);
                    outChannel.close(); 
                } catch (Exception e) {};
            }
            buf = null;
            if (rOut != null) {
                try { rOut.close(); } catch (Exception e){}
            }
        }

    }

    private void readDataBlock() {
        RandomAccessFile rIn = null;
        FileChannel inChannel = null;
        ByteBuffer buf = null;
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (!file.exists()) {
                createNewDataFile();
            }

            // What is the ending cell?
            long endCell = blockStartingCell + blockSize;
            if (endCell > (numberRows * numberColumns - 1)) {
                endCell = numberRows * numberColumns - 1;
            }


            blockEndingCell = endCell;

            int writeLengthInCells = (int)(blockEndingCell - blockStartingCell + 1);
            buf = ByteBuffer.allocate((int) (writeLengthInCells * cellSizeInBytes));

            rIn = new RandomAccessFile(dataFile, "r");
            
            inChannel = rIn.getChannel();
            
            inChannel.position(blockStartingCell * cellSizeInBytes);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(byteOrder);

            
            if (dataType == DataType.DOUBLE) { //.equals("double")) {
                buf.rewind();
                DoubleBuffer db = buf.asDoubleBuffer();
                grid = new double[writeLengthInCells];
                db.get(grid);
            } else if (dataType == DataType.FLOAT) { //.equals("float")) {
                buf.rewind();
                FloatBuffer fb = buf.asFloatBuffer();
                float[] fa = new float[writeLengthInCells];
                fb.get(fa);
                //fb = null;
                //buf = null;
                grid = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    grid[j] = fa[j];
                }
                //fa = null;
            } else if (dataType == DataType.INTEGER) { //.equals("integer")) {
                buf.rewind();
                ShortBuffer ib = buf.asShortBuffer();
                short[] ia = new short[writeLengthInCells];
                ib.get(ia);
                grid = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    grid[j] = ia[j];
                }
            } else if (dataType == DataType.BYTE) { //.equals("byte")) {
                buf.rewind();
                byte[] ba = new byte[writeLengthInCells];
                buf.get(ba);
                grid = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    grid[j] = ba[j];
                }
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } catch (Throwable t) {
            System.err.println(t.getMessage());
        } finally {
            if (rIn != null) {
                try { rIn.close(); } catch (Exception e){}
            }
            if (inChannel != null) {
                try {
                    //inChannel.force(false);
                    inChannel.close();
                } catch (Exception e) {
                    
                }
            }
            numberOfDataFileReads++;
        }
    }
    
    /**
     * Used to dump any data contained in memory to disk.
     */
    public void flush() {
        writeDataBlock();
    }

    /**
     * Dumps the data block currently in memory to the data file.
     */
    private void writeDataBlock() {
        if (!saveChanges) { return; }
        // update the minimum and maximum values
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] < min && grid[i] != noDataValue) { min = grid[i]; }
            if (grid[i] > max && grid[i] != noDataValue) { max = grid[i]; }
        }
        if (max > maximumValue) { maximumValue = max; }
        if (min < minimumValue) { minimumValue = min; }
        
        RandomAccessFile rOut = null;
        ByteBuffer buf = null;
        FileChannel outChannel = null;
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (!file.exists()) {
                createNewDataFile();
            }
            
            rOut = new RandomAccessFile(dataFile, "rw");
            outChannel = rOut.getChannel();
            outChannel.position(blockStartingCell * cellSizeInBytes);
            int writeLengthInCells = (int) (blockEndingCell - blockStartingCell + 1);
            
            /*long startPos = blockStartingCell * cellSizeInBytes;
            FileChannel fc = new RandomAccessFile(dataFile, "rw").getChannel();
            FloatBuffer fb = fc.map(FileChannel.MapMode.READ_WRITE, startPos, 
                    writeLengthInCells).asFloatBuffer();
            for(int j = 0; j < writeLengthInCells; j++) {
                fb.put((float)grid[j]);
            }
            fc.force(true);
            fc.close();*/
            
            if (dataType == DataType.DOUBLE) { //.equals("double")) {
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                DoubleBuffer db = buf.asDoubleBuffer();
                db.put(grid);
                db = null;
                outChannel.write(buf);
            } else if (dataType == DataType.FLOAT) { //.equals("float")) {
                float[] fa = new float[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    fa[j] = (float)grid[j];
                }
                buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                FloatBuffer fb = buf.asFloatBuffer();
                fb.put(fa);
                fb = null;
                fa = null;
                outChannel.write(buf);
            } else if (dataType == DataType.INTEGER) { //.equals("integer")) {
                short[] ia = new short[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ia[j] = (short)grid[j];
                }
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                ShortBuffer ib = buf.asShortBuffer();
                ib.put(ia);
                ib = null;
                ia = null;
                outChannel.write(buf);
            } else if (dataType == DataType.BYTE) { //.equals("byte")) {
                byte[] ba = new byte[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ba[j] = (byte)grid[j];
                }
                buf = ByteBuffer.wrap(ba);
                ba = null;
                outChannel.write(buf);
            }
            
        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            buf = null;
            if (rOut != null) {
                try { rOut.close(); } catch (Exception e){}
            }
            if (outChannel != null) {
                try { 
                    outChannel.force(false);
                    outChannel.close(); 
                } catch (Exception e){}
            }
            isDirty = false;
            numberOfDataFileWrites++;
        }

    }
//
//    private void setPropertiesUsingAnotherRaster(String BaseRasterHeader, DataType dataType) {
//        setDataType(dataType);
//
//        // Set the properties of this WhiteboxRaster to those of the base raster.
//        DataInputStream in = null;
//        BufferedReader br = null;
//        try {
//            // Open the file that is the first command line parameter
//            FileInputStream fstream = new FileInputStream(BaseRasterHeader);
//            // Get the object of DataInputStream
//            in = new DataInputStream(fstream);
//
//            br = new BufferedReader(new InputStreamReader(in));
//
//            if (BaseRasterHeader != null) {
//                String line;
//                String[] str;
//                //Read File Line By Line
//                while ((line = br.readLine()) != null) {
//                    str = line.split("\t");
//                    if (str[0].toLowerCase().contains("north")) {
//                        this.north = Double.parseDouble(str[1]);
//                    } else if (str[0].toLowerCase().contains("south")) {
//                        this.south = Double.parseDouble(str[1]);
//                    } else if (str[0].toLowerCase().contains("west")) {
//                        this.west = Double.parseDouble(str[1]);
//                    } else if (str[0].toLowerCase().contains("east")) {
//                        this.east = Double.parseDouble(str[1]);
//                    } else if (str[0].toLowerCase().contains("cols")) {
//                        this.numberColumns =  Integer.parseInt(str[1]);
//                    } else if (str[0].toLowerCase().contains("rows")) {
//                        this.numberRows =  Integer.parseInt(str[1]);
//                    } else if (str[0].toLowerCase().contains("data scale")) {
//                        if (str[1].toLowerCase().contains("continuous")) {
//                            this.setDataScale(DataScale.CONTINUOUS);
//                            //this.setDataScale(DATA_SCALE_CONTINUOUS);
//                        } else if (str[1].toLowerCase().contains("categorical")) {
//                            this.setDataScale(DataScale.CATEGORICAL);
//                            //this.setDataScale(DATA_SCALE_CATEGORICAL);
//                        } else if (str[1].toLowerCase().contains("bool")) {
//                            this.setDataScale(DataScale.BOOLEAN);
//                            //this.setDataScale(DATA_SCALE_BOOLEAN);
//                        } else if (str[1].toLowerCase().contains("rgb")) {
//                            this.setDataScale(DataScale.RGB);
//                            //this.setDataScale(DATA_SCALE_RGB);
//                        }
//                    } else if (str[0].toLowerCase().contains("xy units")) {
//                        this.xyUnits = str[1];
//                    } else if (str[0].toLowerCase().contains("projection")) {
//                        this.projection = str[1];
//                    } else if (str[0].toLowerCase().contains("nodata")) {
//                        this.noDataValue = Double.parseDouble(str[1]);
//                    } else if (str[0].toLowerCase().contains("palette")) {
//                        this.preferredPalette = str[1];
//                    }
//                }
//
//            }
//        } catch (java.io.IOException e) {
//            System.out.println("Error: " + e.getMessage());
//        } catch (Exception e) { //Catch exception if any
//            System.out.println("Error: " + e.getMessage());
//        } finally {
//            try {
//                if (in != null || br!= null) {
//                    in.close();
//                    br.close();
//                }
//            } catch (java.io.IOException ex) {
//            }
//
//        }
//
//        // Save the header file.
//        this.writeHeaderFile();
//    }
//    
//    /**
//     * Used to find the minimum and maximum values in the raster. NoDataValues are ignored.
//     * Minimum and maximum values are stored in the minimumValue and maximumValue fields.
//     */
//    public void findMinAndMaxVals() {
//        double[] data;
//        double min = Double.MAX_VALUE;
//        double max = -Double.MAX_VALUE;
//        double z;
//        for (int row = 0; row < numberRows; row++) {
//            data = getRowValues(row);
//            for (int col = 0; col < numberColumns; col++) {
//                z = data[col];
//                if (z != noDataValue) {
//                    if (z < min) { min = z; }
//                    if (z > max) { max = z; }
//                }
//            }
//        }
//        maximumValue = max;
//        minimumValue = min;
//    }
//    
//    private double[] cumulativeHisto = null;
//    public double getPercentileValue(double percentile) {
//        if (mean == noDataValue || mean == -32768d) {
//            readStatsFile();
//        }
//        percentile = percentile / 100;
//        double retVal = 0;
//        double x1, x2;
//        double y1, y2;
//        
//        if (cumulativeHisto == null) {
//            cumulativeHisto = new double[histo.length];
//            
//            cumulativeHisto[0] = histo[0];
//            for (int i = 1; i < histo.length; i++) {
//                cumulativeHisto[i] = histo[i] + cumulativeHisto[i - 1];
//            }
//            for (int i = 0; i < histo.length; i++) {
//                cumulativeHisto[i] = cumulativeHisto[i] / numValidCells;
//            }
//        }
//        for (int i = 0; i < histo.length; i++) {
//            if (cumulativeHisto[i] >= percentile) { // find the first bin with a value greater than percentile.
//                if (i > 0) {
//                    x1 = minimumValue + (i - 1) * binWidth;
//                    x2 = minimumValue + i * binWidth;
//                    y1 = cumulativeHisto[i - 1];
//                    y2 = cumulativeHisto[i];
//                    
//                } else {
//                    x1 = minimumValue + (i - 1) * binWidth;
//                    x2 = minimumValue + i * binWidth;
//                    y1 = 0;
//                    y2 = cumulativeHisto[i];
//                    
//                }
//                retVal = x1 + (percentile - y1) / (y2 - y1) * binWidth;
//                break;
//            }
//        }
//        return retVal;
//    }
//    
//    public void readStatsFile() {
//        File file = new File(statsFile);
//        if (!file.exists()) { 
//            createStatsFile();
//            return;
//        }
//        
//        DataInputStream in = null;
//        BufferedReader br = null;
//        boolean statsFlag = false;
//        boolean histoFlag = false;
//        int i = 0;
//        long histoVal = 0;
//        try {
//            // Open the file that is the first command line parameter
//            FileInputStream fstream = new FileInputStream(statsFile);
//            // Get the object of DataInputStream
//            in = new DataInputStream(fstream);
//
//            br = new BufferedReader(new InputStreamReader(in));
//
//            if (statsFile != null) {
//                String line;
//                String[] str;
//                //Read File Line By Line
//                while ((line = br.readLine()) != null) {
//                    str = line.split("\t");
//                    if (str[0].toLowerCase().contains("start_stats")) {
//                        statsFlag = true;
//                    }
//                    if (str[0].toLowerCase().contains("end_stats")) {
//                        statsFlag = false;
//                    }
//                    if (str[0].toLowerCase().contains("start_histo")) {
//                        histoFlag = true;
//                    }
//                    if (str[0].toLowerCase().contains("end_histo")) {
//                        histoFlag = false;
//                    }
//                    if (statsFlag) {
//                        if (str[0].toLowerCase().contains("mean")) {
//                            this.mean = Double.parseDouble(str[1]);
//                        } else if (str[0].toLowerCase().contains("median")) {
//                            this.median = Double.parseDouble(str[1]);
//                        } else if (str[0].toLowerCase().contains("mode")) {
//                            this.mode = Double.parseDouble(str[1]);
//                        } else if (str[0].toLowerCase().contains("std_dev")) {
//                            this.stdDeviation = Double.parseDouble(str[1]);
//                        } else if (str[0].toLowerCase().contains("num_valid_cells")) {
//                            this.numValidCells = Long.parseLong(str[1]);
//                        }
//                    } else if (histoFlag) {
//                        if (str[0].toLowerCase().contains("bin_width")) {
//                            this.binWidth = Double.parseDouble(str[1]);
//                        } else if (str[0].toLowerCase().contains("num_bins")) {
//                            histo = new long[Integer.parseInt(str[1])];
//                            i = 0;
//                        } else if (!str[0].toLowerCase().contains("start_histo")) {
//                            histo[i] = Long.parseLong(str[0]);
//                            i++;
//                        }
//                    }
//                    
//                }
//                //Close the input stream
//                in.close();
//                br.close();
//
//            }
//        } catch (java.io.IOException e) {
//            System.err.println("Error: " + e.getMessage());
//        } catch (Exception e) { //Catch exception if any
//            System.err.println("Error: " + e.getMessage());
//        } finally {
//            try {
//                if (in != null || br!= null) {
//                    in.close();
//                    br.close();
//                }
//            } catch (java.io.IOException ex) {
//            }
//
//        }
//
//    }
//    
//    /**
//     * Creates a .wst file to store information about the statistical distribution
//     * of the raster, including the min, max, mean, mode, stdev, and the histogram. These
//     * data are used for clipping the tails of the distribution for enhanced visualization.
//     */
//    public void createStatsFile() {
//        File file = new File(statsFile);
//        if (file.exists()) { 
//            file.delete(); 
//        }
//        
//        mean = 0;
//        mode = 0;
//        long n = 0;
//        double[] data;
//        double imageTotalDeviation = 0;
//        double min = Double.MAX_VALUE;
//        double max = -Double.MAX_VALUE;
//        double z;
//        double[] rowMedians = new double[numberRows];
//        
//        binWidth = 0;
//        int binNum = 0;
//        int numberOfBins = 0;
//        
//            
//        if (dataScale != DataScale.RGB) { //DATA_SCALE_RGB) {
//            
//            // calculate the mean, min and max.
//            for (int row = 0; row < numberRows; row++) {
//                data = getRowValues(row);
//                for (int col = 0; col < numberColumns; col++) {
//                    z = data[col];
//                    if (z != noDataValue) {
//                        mean += z;
//                        n++;
//                        if (z < min) {
//                            min = z;
//                        }
//                        if (z > max) {
//                            max = z;
//                        }
//                    }
//                }
//            }
//            
//            maximumValue = max;
//            minimumValue = min;
//            mean = mean / n;
//            numValidCells = n;
//            
//            if (dataType == DataType.INTEGER) { //equals("integer")) {
//                numberOfBins = (int)(max - min + 1);
//                binWidth = 1;
//            } else if (dataType == DataType.FLOAT || dataType == DataType.DOUBLE) { //.equals("float") || dataType.equals("double")) {
//                if ((max - min) < 512) {
//                    numberOfBins = 512;
//                } else if ((max - min) < 1024) {
//                    numberOfBins = 1024;
//                }  else if ((max - min) < 2048) {
//                    numberOfBins = 2048;
//                } else if ((max - min) < 4096) {
//                    numberOfBins = 4096;
//                } else {
//                    numberOfBins = 8196;
//                }
//                binWidth = (max - min) / (numberOfBins - 1);
//            }
//            
//            histo = new long[numberOfBins];
//            
//            // figure out how many bins should be in the histogram
//
//            for (int row = 0; row < numberRows; row++) {
//                data = getRowValues(row);
//                for (int col = 0; col < numberColumns; col++) {
//                    z = data[col];
//                    if (z != noDataValue) {
//                        imageTotalDeviation += (z - mean) * (z - mean);
//                        binNum = (int)(Math.floor((z - min) / binWidth));
//                        histo[binNum]++;
//                    }
//                }
//            }
//            
//            stdDeviation = Math.sqrt(imageTotalDeviation / (n - 1));
//
//            long highestVal = 0;
//            int highestBin = 0;
//            for (int i = 0; i < histo.length; i++) {
//                if (histo[i] > highestVal) { 
//                    highestVal = histo[i]; 
//                    highestBin = i;
//                }
//            }
//
//            mode = highestBin * binWidth;
//            median = getPercentileValue(50.0d);
//            
//
//            String str = null;
//            FileWriter fw = null;
//            BufferedWriter bw = null;
//            PrintWriter out = null;
//            try {
//                fw = new FileWriter(file, false);
//                bw = new BufferedWriter(fw);
//                out = new PrintWriter(bw, true);
//
//                str = "START_STATS:";
//                out.println(str);
//                str = "MIN: \t" + Double.toString(this.minimumValue);
//                out.println(str);
//                str = "MAX: \t" + Double.toString(this.maximumValue);
//                out.println(str);
//                str = "MEAN: \t" + Double.toString(mean);
//                out.println(str);
//                str = "MEDIAN: \t" + Double.toString(median);
//                out.println(str);
//                str = "MODE: \t" + Double.toString(mode);
//                out.println(str);
//                str = "STD_DEV: \t" + Double.toString(stdDeviation);
//                out.println(str);
//                str = "NUM_VALID_CELLS: \t" + Long.toString(n);
//                out.println(str);
//                str = "END_STATS";
//                out.println(str);
//                
//                str = "START_HISTO";
//                out.println(str);
//                str = "BIN_WIDTH: \t" + binWidth;
//                out.println(str);
//                str = "NUM_BINS: \t" + numberOfBins;
//                out.println(str);
//                for (int i = 0; i < histo.length; i++) {
//                    str = String.valueOf(histo[i]);
//                    out.println(str);
//                }
//                str = "END_HISTO";
//                out.println(str);
//                
//            } catch (java.io.IOException e) {
//                System.err.println("Error: " + e.getMessage());
//            } catch (Exception e) { //Catch exception if any
//                System.err.println("Error: " + e.getMessage());
//            } finally {
//                if (out != null || bw != null) {
//                    out.flush();
//                    out.close();
//                }
//
//            }
//
//        } else {
//            numberOfBins = 256;
//        }
//    }
//    
    /**
     * Used to perform closing functionality when a whiteboxRaster is no longer needed.
     */
    @Override
    public void close() {
        if (this.isTemporaryFile) {
            File f1 = new File(this.headerFile);
            f1.delete();
            f1 = new File(this.dataFile);
            f1.delete();
        } else {
            if (saveChanges) {
                if (isDirty) { writeDataBlock(); }
                findMinAndMaxVals();
                writeHeaderFile();
            }
        }
        grid = null;
    }

}
