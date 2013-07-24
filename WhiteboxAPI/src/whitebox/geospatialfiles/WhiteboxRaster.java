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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * The whiteboxRaster is used to manipulate Whitebox GAT raster files (.dep and
 * .tas).
 *
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
     * Set to false if the header and data files (.dep and .tas) should be
     * deleted when the object is closed.
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
     * Class constructor. Notice that the data file name will also be set based
     * on the specified header file name.
     *
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw"
     * (read/write).
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess) {
        // set the header file and data file.
        headerFile = HeaderFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess(FileAccess);
        readHeaderFile();
        setBlockData();
    }

    /**
     * Class constructor. Notice that the data file name will also be set based
     * on the specified header file name.
     *
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw"
     * (read/write).
     * @param BufferSize Determines the how much data can be stored in memory.
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess, double BufferSize) {
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
     * Class constructor. Notice that the data file name will also be set based
     * on the specified header file name.
     *
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw"
     * (read/write).
     * @param BaseRasterHeader The name of a WhiteboxRaster header file to base
     * this new object on.
     * @param dataType The data type of the new WhiteboxRaster. Can be 'double',
     * 'float', 'integer', or 'byte'
     * @param InitialValue Double indicating the value used to initialize the
     * grid. It is recommended to use the noDataValue.
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, DataType dataType, double InitialValue) {
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
     * Class constructor. Notice that the data file name will also be set based
     * on the specified header file name.
     *
     * @param HeaderFile The name of the WhiteboxRaster header file.
     * @param FileAccess Sets the file access. Either "r" (read-only) or "rw"
     * (read/write).
     * @param BaseRasterHeader The name of a WhiteboxRaster header file to base
     * this new object on.
     * @param DataType The data type of the new WhiteboxRaster. Can be 'double',
     * 'float', 'integer', or 'byte'
     * @param InitialValue Double indicating the value used to initialize the
     * grid. It is recommended to use the noDataValue.
     * @param BufferSize Determines how much data can be stored in memory.
     */
    public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, DataType dataType, double InitialValue, double BufferSize) {
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

    public WhiteboxRaster(String HeaderFile, double north, double south, double east, double west, int rows, int cols, DataScale dataScale, DataType dataType, double initialValue, double noData) {
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
    private long bufferSize = Runtime.getRuntime().maxMemory() / 5; //100 * 1048576; //in bytes

    /**
     * Retrieves the maximum memory usage for this Whitebox grid in megabytes.
     *
     * @return Maximum memory.
     */
    public double getBufferSize() {
        return bufferSize / 1048576;
    }

    /**
     * Sets maximum memory usage for this Whitebox grid in megabytes.
     *
     * @param BufferSize maximum memory usage.
     */
    private void setBufferSize(double BufferSize) {
        bufferSize = (long) (BufferSize * 1048576);
    }

    /**
     * Retrieves the block size contained in memory.
     *
     * @return Long containing block size
     */
    public long getBlockSize() {
        return blockSize;
    }
    private long numberOfDataFileReads = 0;

    /**
     * The number of times that the data file (.tas) has been read by this
     * object.
     *
     * @return long stating the number of reads.
     */
    public long getNumberOfDataFileReads() {
        return numberOfDataFileReads;
    }
    private long numberOfDataFileWrites = 0;

    /**
     * The number of times that the data file (.tas) has been written by this
     * object.
     *
     * @return long stating the number of reads.
     */
    public long getNumberOfDataFileWrites() {
        return numberOfDataFileWrites;
    }

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
     * This method should be used when you need to set an entire row of data at
     * a time. It has less overhead that the setValue method (which works on a
     * pixel-by-pixel basis) and can be used to efficiently scan through a
     * raster image row by row.
     *
     * @param row An int stating the zero-based row to be returned.
     * @param vals An array of doubles containing the values store in the
     * specified row.
     */
    public void setRowValues(int row, double[] vals) {
        if (!saveChanges) {
            return;
        }
        if (vals.length != numberColumns) {
            return;
        }

        // update the minimum and maximum values
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < numberColumns; i++) {
            if (vals[i] < min && vals[i] != noDataValue) {
                min = vals[i];
            }
            if (vals[i] > max && vals[i] != noDataValue) {
                max = vals[i];
            }
        }
        if (max > maximumValue) {
            maximumValue = max;
        }
        if (min < minimumValue) {
            minimumValue = min;
        }

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
                    fa[j] = (float) vals[j];
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
                    ia[j] = (short) vals[j];
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
                    ba[j] = (byte) vals[j];
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
                try {
                    rOut.close();
                } catch (Exception e) {
                }
            }
            if (outChannel != null) {
                try {
                    outChannel.force(false);
                    outChannel.close();
                } catch (Exception e) {
                }
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
     *
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @return The value contained in the raster grid at the specified grid
     * cell.
     */
    public double getValue(int row, int column) {
        //try {
        if (column >= 0 && column < numberColumns && row >= 0 && row < numberRows) {

            if (blockEndingCell < 0) {
                readDataBlock();
            }

            // what is the cell number?
            long cellNum = (long) (row) * numberColumns + column;

            // check to see if it is within the current block
            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (saveChanges && isDirty) {
                    writeDataBlock();
                }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) { // reading downward
                    if (currentReadDirection == -1) {
                        currentReadDirection = 0;
                    }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - halfBlockSize * switchRatio); //10 * numberColumns);
                } else { // reading upward
                    if (currentReadDirection == -1) {
                        currentReadDirection = 1;
                    }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - (blockSize - (switchRatio * halfBlockSize))); //+ (blockSize / 2) * ((double)upReadDirection / downReadDirection)); // + 10 * numberColumns - blockSize);
                }
                previousRow = row;
                //blockStartingCell = (int)(cellNum - blockSize / 2);
                if (blockStartingCell < 0) {
                    blockStartingCell = 0;
                }

                readDataBlock();
            }

            return grid[(int) (cellNum - blockStartingCell)];
        } else {
            if (!isReflectedAtEdges) {
                return noDataValue;
            }

            // if you get to this point, it is reflected at the edges
            if (row < 0) {
                row = -row - 1;
            }
            if (row >= numberRows) {
                row = numberRows - (row - numberRows) - 1;
            }
            if (column < 0) {
                column = -column - 1;
            }
            if (column >= numberColumns) {
                column = numberColumns - (column - numberColumns) - 1;
            }
            if (column >= 0 && column < numberColumns && row >= 0 && row < numberRows) {
                return getValue(row, column);
            } else {
                // it was too off grid to be reflected.
                return noDataValue;
            }
        }
//        } catch (Exception e) {
//            if (communicator != null) {
//                communicator.logException("WhiteboxRaster error", e);
//            }
//            return noDataValue;
//        }
    }

    /**
     * Sets the value of a specified cell in the raster grid.
     *
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to place in the grid cell.
     */
    public void setValue(int row, int column, double value) {
//        try {
        if (saveChanges && column >= 0 && column < this.numberColumns
                && row >= 0 && row < this.numberRows) {
            // what is the cell number?
            long cellNum = (long) (row) * numberColumns + column;

            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (isDirty) {
                    writeDataBlock();
                }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) {
                    if (currentReadDirection == -1) {
                        currentReadDirection = 0;
                    }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - halfBlockSize * switchRatio);
                } else {
                    if (currentReadDirection == -1) {
                        currentReadDirection = 1;
                    }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - (blockSize - (switchRatio * halfBlockSize)));
                }
                previousRow = row;
                if (blockStartingCell < 0) {
                    blockStartingCell = 0;
                }
                readDataBlock();
            }

            grid[(int) (cellNum - blockStartingCell)] = value;
            isDirty = true;
        }
//        } catch (Exception e) {
//            if (communicator != null) {
//                communicator.logException("WhiteboxRaster error", e);
//            }
//        }
    }

    /**
     * Increments the value of a specified cell in the raster grid.
     *
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to increment the grid cell by.
     */
    public void incrementValue(int row, int column, double value) {
        if (saveChanges && column >= 0 && column < this.numberColumns
                && row >= 0 && row < this.numberRows) {
            // what is the cell number?
            long cellNum = (long) (row) * numberColumns + column;

            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (isDirty) {
                    writeDataBlock();
                }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) {
                    if (currentReadDirection == -1) {
                        currentReadDirection = 0;
                    }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - halfBlockSize * switchRatio);
                } else {
                    if (currentReadDirection == -1) {
                        currentReadDirection = 1;
                    }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - (blockSize - (switchRatio * halfBlockSize)));
                }
                previousRow = row;
                if (blockStartingCell < 0) {
                    blockStartingCell = 0;
                }
                readDataBlock();
            }

            grid[(int) (cellNum - blockStartingCell)] += value;
            isDirty = true;
        }
    }

    /**
     * Decrements the value of a specified cell in the raster grid.
     *
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to decrement the grid cell by.
     */
    public void decrementValue(int row, int column, double value) {
        if (saveChanges && column >= 0 && column < this.numberColumns
                && row >= 0 && row < this.numberRows) {
            // what is the cell number?
            long cellNum = (long) (row) * numberColumns + column;

            if ((cellNum > blockEndingCell) || (cellNum < blockStartingCell)) {
                if (isDirty) {
                    writeDataBlock();
                }
                numReads++;
                // Figure out a new blockstartingcell
                if (previousRow < row) {
                    if (currentReadDirection == -1) {
                        currentReadDirection = 0;
                    }
                    if (currentReadDirection != 0) {
                        currentReadDirection = 0;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - halfBlockSize * switchRatio);
                } else {
                    if (currentReadDirection == -1) {
                        currentReadDirection = 1;
                    }
                    if (currentReadDirection != 1) {
                        currentReadDirection = 1;
                        numSwitchReadDirections++;
                        switchRatio = (double) numSwitchReadDirections / numReads;
                    }
                    blockStartingCell = (long) (cellNum - (blockSize - (switchRatio * halfBlockSize)));
                }
                previousRow = row;
                if (blockStartingCell < 0) {
                    blockStartingCell = 0;
                }
                readDataBlock();
            }

            grid[(int) (cellNum - blockStartingCell)] -= value;
            isDirty = true;
        }
    }
    
    private boolean forceAllDataInMemory = false;
    public void setForceAllDataInMemory(boolean value) {
        forceAllDataInMemory = value;
        setBlockData();
    }
    
    public boolean isForceAllDataInMemory() {
        return forceAllDataInMemory;
    }

    private void setBlockData() {
        try {
            // see if the data can be comfortably contained in memory, keeping in
            // mind that it is always stored as doubles.
            //System.gc();
            long availableMemory = Runtime.getRuntime().freeMemory();
            long gridMemoryRequirements = (long) numberRows * (long) numberColumns * 8L;
            if ((availableMemory / 3) > gridMemoryRequirements || forceAllDataInMemory) {
                // store the entire grid in memory.
                blockSize = numberRows * numberColumns;
                bufferSize = gridMemoryRequirements;
            } else if (((double) gridMemoryRequirements / (availableMemory / 3)) > 2) {
                // the data doesn't come close to fitting in the available memory.
                bufferSize = 100 * 1048576;
                blockSize = (int) (Math.round(Math.floor(bufferSize / 8))) / 3;
            } else {
                blockSize = (numberRows * numberColumns) / 2;
                bufferSize = ((numberRows * numberColumns) / 2) * 8;
            }

            halfBlockSize = blockSize / 2;
            blockStartingCell = 0;
            //readDataBlock();
        } catch (Exception e) {
            if (communicator != null) {
                communicator.logException("WhiteboxRaster error", e);
            } else {
                System.out.println(e.toString());
            }
        }
    }

    public void reinitialize(double initialValue) {
        this.initialValue = initialValue;

        // See if the data file exists.
        File file = new File(dataFile);
        file.delete();
        createNewDataFile();

    }

    public void createNewDataFile() {
        RandomAccessFile rOut = null;
        ByteBuffer buf = null;
        FileChannel outChannel = null;
        try {
            long numberCells = (long) ((long) (numberRows) * numberColumns);
            int writeLength = 2000000;
            if (writeLength > numberCells) {
                writeLength = (int) numberCells;
            }
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
                        writeLength = (int) (numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                        db = buf.asDoubleBuffer();
                    }
                    da = new double[writeLength];
                    if (initialValue != 0) {
                        Arrays.fill(da, initialValue);
                    }
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
                        writeLength = (int) (numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                        fb = buf.asFloatBuffer();
                    }
                    fa = new float[writeLength];
                    if (initialValue != 0) {
                        Arrays.fill(fa, (float) initialValue);
                    }
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
                        writeLength = (int) (numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                        ib = buf.asShortBuffer();
                    }
                    ia = new short[(int) writeLength];
                    if (initialValue != 0) {
                        Arrays.fill(ia, (short) initialValue);
                    }
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
                        writeLength = (int) (numberCells - numCellsWritten);
                        buf = ByteBuffer.allocateDirect(cellSizeInBytes * writeLength);
                        buf.order(byteOrder);
                    }
                    ba = new byte[writeLength];
                    if (initialValue != 0) {
                        Arrays.fill(ba, (byte) initialValue);
                    }
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
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            buf = null;
            if (rOut != null) {
                try {
                    rOut.close();
                } catch (Exception e) {
                }
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
            long endCell = (long) (blockStartingCell) + blockSize;
            if (endCell > ((long) (numberRows) * numberColumns - 1)) {
                endCell = (long) (numberRows) * numberColumns - 1;
            }

            blockEndingCell = endCell;

            int readLengthInCells = (int) (blockEndingCell - blockStartingCell + 1);
            buf = ByteBuffer.allocate((int) (readLengthInCells * cellSizeInBytes));

            rIn = new RandomAccessFile(dataFile, "r");

            inChannel = rIn.getChannel();

            inChannel.position(blockStartingCell * cellSizeInBytes);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(byteOrder);

            grid = new double[readLengthInCells];

            if (dataType == DataType.DOUBLE) {
                buf.rewind();
                DoubleBuffer db = buf.asDoubleBuffer();
                db.get(grid);
            } else if (dataType == DataType.FLOAT) {
                buf.rewind();
                FloatBuffer fb = buf.asFloatBuffer();
                float[] fa = new float[readLengthInCells];
                fb.get(fa);
                for (int j = 0; j < readLengthInCells; j++) {
                    grid[j] = fa[j];
                }
            } else if (dataType == DataType.INTEGER) {
                buf.rewind();
                ShortBuffer ib = buf.asShortBuffer();
                short[] ia = new short[readLengthInCells];
                ib.get(ia);
                for (int j = 0; j < readLengthInCells; j++) {
                    grid[j] = ia[j];
                }
            } else if (dataType == DataType.BYTE) {
                buf.rewind();
                for (int j = 0; j < readLengthInCells; j++) {
                    grid[j] = whitebox.utilities.Unsigned.getUnsignedByte(buf, j);
                }
            }

        } catch (Exception e) {
            if (communicator != null) {
                communicator.logException("WhiteboxRaster error", e);
            } else {
                System.out.println(e.toString());
            }
        } catch (Throwable t) {
            if (communicator != null) {
                communicator.logThrowable("WhiteboxRaster error", t);
            } else {
                System.err.println(t.getMessage());
            }
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
            if (inChannel != null) {
                try {
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
        try {
            if (!saveChanges) {
                return;
            }
            // update the minimum and maximum values
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int i = 0; i < grid.length; i++) {
                if (grid[i] < min && grid[i] != noDataValue) {
                    min = grid[i];
                }
                if (grid[i] > max && grid[i] != noDataValue) {
                    max = grid[i];
                }
            }
            if (max > maximumValue) {
                maximumValue = max;
            }
            if (min < minimumValue) {
                minimumValue = min;
            }

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
                        fa[j] = (float) grid[j];
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
                        ia[j] = (short) grid[j];
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
                        ba[j] = (byte) grid[j];
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
                    try {
                        rOut.close();
                    } catch (Exception e) {
                    }
                }
                if (outChannel != null) {
                    try {
                        outChannel.force(false);
                        outChannel.close();
                    } catch (Exception e) {
                    }
                }
                isDirty = false;
                numberOfDataFileWrites++;
            }
        } catch (Exception e) {
            if (communicator != null) {
                communicator.logException("WhiteboxRaster error", e);
            }
        }
    }

    /**
     * Used to perform closing functionality when a whiteboxRaster is no longer
     * needed.
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
                if (isDirty) {
                    writeDataBlock();
                }
                findMinAndMaxVals();
                writeHeaderFile();
            }
        }
        grid = null;
    }
}
