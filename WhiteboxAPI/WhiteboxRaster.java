/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
 
import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Date;

/**
 * The whiteboxRaster is used to manipulate Whitebox GAT raster files (.dep and .tas).
 * @author John Lindsay
 */
public class WhiteboxRaster {


    // ************************
    // Fields
    // ************************
    private double[][] grid;
    private Date[] lastUsed;
    private boolean[] rowIsDirty;
    private int blockSize = 0;
    private int numRowsInMemory = 0;
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
    public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, String DataType, double InitialValue)
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
        setPropertiesUsingAnotherRaster(BaseRasterHeader, DataType);
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
    public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, String DataType, double InitialValue, double BufferSize)
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
        setPropertiesUsingAnotherRaster(BaseRasterHeader, DataType);
        setBlockData();

    }


    // ***********************************
    // Property getter and setter methods.
    // ***********************************

    private String headerFile;
    /**
     * Gets the header file (*.dep) name for this Whitebox raster grid.
     * Notice that the header file name is set during object creation.
     * @return A string containing the reference to the header file.
     */
    public String getHeaderFile() {
        return headerFile;
    }

    private String dataFile;
    /**
     * Gets the data file (*.tas) name for this Whitebox raster grid.
     * Notice that the data file name is set during object creation.
     * @return A string containing the reference to the data file.
     */
    public String getDataFile() {
        return dataFile;
    }

    /**
     * Used to determine the size of the data file (.tas).
     * @return long containing the size of the data file in bytes.
     */
    public long getDataFileSize() {
        File file = new File(dataFile);
        return file.length();
    }
    
    private String statsFile;
    /**
     * Gets the statistical distribution file (*.wstat) name for this Whitebox raster
     * grid. Notice that the stats file name is set during object creation.
     * @return A string containing the reference to the stats file.
     */
    public String getStatsFile() {
        return statsFile;
    }
    
    private final double largeValue = (double)Float.MAX_VALUE;
    private final double smallValue = (double)-Float.MAX_VALUE;
    
    private double minimumValue  = largeValue;
    /**
     * Retrieves the minimum value in the Whitebox grid.
     * @return The minimum value.
     */
    public double getMinimumValue() {
        return this.minimumValue;
    }
    /**
     * Sets the minimum value in the Whitebox grid.
     * @param MinimumValue The minimum value.
     */
    public void setMinimumValue(double MinimumValue) {
        this.minimumValue = MinimumValue;
    }

    private double maximumValue = smallValue;
    /**
     * Retrieves the maximum value in the Whitebox grid.
     * @return The maximum value.
     */
    public double getMaximumValue() {
        return maximumValue;
    }
    /**
     * Sets the maximum value in the Whitebox grid.
     * @param MaximumValue The minimum value.
     */
    public void setMaximumValue(double MaximumValue) {
        maximumValue = MaximumValue;
    }

    private double north;
    /**
     * Retrieves the coordinate of the northern edge.
     * @return The coordinate of the northern edge.
     */
    public double getNorth() {
        return north;
    }
    /**
     * Sets the coordinate of the northern edge.
     * @param North The coordinate of the northern edge.
     */
    public void setNorth(float North) {
        north = North;
    }

    private double south;
    /**
     * Retrieves the coordinate of the southern edge.
     * @return The coordinate of the southern edge.
     */
    public double getSouth() {
        return south;
    }
    /**
     * Sets the coordinate of the southern edge.
     * @param South The coordinate of the southern edge.
     */
    public void setSouth(float South) {
        south = South;
    }

    private double west;
    /**
     * Retrieves the coordinate of the western edge.
     * @return The coordinate of the western edge.
     */
    public double getWest() {
        return west;
    }
    /**
     * Sets the coordinate of the western edge.
     * @param West The coordinate of the western edge.
     */
    public void setWest(float West) {
        west = West;
    }

    private double east;
    /**
     * Retrieves the coordinate of the eastern edge.
     * @return The coordinate of the eastern edge.
     */
    public double getEast() {
        return east;
    }
    /**
     * Sets the coordinate of the eastern edge.
     * @param East The coordinate of the eastern edge.
     */
    public void setEast(float East) {
        east = East;
    }

    private int numberColumns;
    /**
     * Retrieves the number of columns in the grid.
     * @return The number of columns.
     */
    public int getNumberColumns() {
        return numberColumns;
    }

    private int numberRows;
    /**
     * Retrieves the number of rows in the grid.
     * @return The number of rows.
     */
    public int getNumberRows() {
        return numberRows;
    }

    private int cellSizeInBytes = 8;
    private String dataType = "double";
    /**
     * Retrieves the data type for this Whitebox grid. Data type may be <b><i>float</i></b> (decimal
     * numbers), <i><b>integer</i></b> (whole numbers from -32,768 to 32,767), or <i><b>byte</i></b> 
     * (whole number from 0 to 255).
     * @return Data type.
     */
    public String getDataType() {
        return dataType;
    }
    /**
     * Sets the data type for this Whitebox grid. Data type may be <b><i>float</i></b> (decimal
     * numbers), <i><b>integer</i></b> (whole numbers from -32,768 to 32,767), or <i><b>byte</i></b> (whole
     * number from 0 to 255).
     * @param DataType The specified data type.
     */
    public void setDataType(String DataType) {
        if (DataType.toLowerCase().equals("float")) {
            dataType = "float";
            cellSizeInBytes = 4;
        } else if (DataType.toLowerCase().contains("int")) {
            dataType = "integer";
            cellSizeInBytes = 2;
        } else if (DataType.toLowerCase().equals("byte")) {
            dataType = "byte";
            cellSizeInBytes = 1;
        } else if (DataType.toLowerCase().equals("double")) {
            dataType = "double";
            cellSizeInBytes = 8;
        } else {
            dataType = "float";
            cellSizeInBytes = 4;
        }
    }

    public static final int DATA_SCALE_CONTINUOUS = 0;
    public static final int DATA_SCALE_CATEGORICAL = 1;
    public static final int DATA_SCALE_BOOLEAN = 2;
    public static final int DATA_SCALE_RGB = 3;
    
    private int dataScale = DATA_SCALE_CONTINUOUS;
    /**
     * Retrieves the data scale for this Whitebox grid. Data scale may be <b><i>DATA_SCALE_CONTINUOUS</i></b> (0), 
     * <i><b>DATA_SCALE_CATEGORICAL</i></b> (1), <i><b>DATA_SCALE_BOOLEAN</i></b> (2), or <i><b>DATA_SCALE_RGB</i></b> (3).
     * @return int Data scale.
     */
    public int getDataScale() {
        return dataScale;
    }
    /**
     * Sets the data scale for this Whitebox grid. Data scale may be <b><i>DATA_SCALE_CONTINUOUS</i></b> (0), 
     * <i><b>DATA_SCALE_CATEGORICAL</i></b> (1), <i><b>DATA_SCALE_BOOLEAN</i></b> (2), or <i><b>DATA_SCALE_RGB</i></b> (3).
     * @param DataScale The specified data type.
     */
    public void setDataScale(int DataScale) {
        if (DataScale < 0) { DataScale = 0; }
        if (DataScale > 3) { DataScale = 3; }
        dataScale = DataScale;
    }

    private String zUnits = "not specified";
    /**
     * Retrieves the Z units for this Whitebox grid.
     * @return Z Units.
     */
    public String getZUnits() {
        return zUnits;
    }
    /**
     * Sets units of the attribute data, i.e. the z-values in the raster image.
     * @param ZUnits The specified data type.
     */
    public void setZUnits(String ZUnits) {
        zUnits = ZUnits.toLowerCase();
    }

    private String xyUnits = "not specified";
    /**
     * Retrieves the XY units for this Whitebox grid.
     * @return XY Units.
     */
    public String getXYUnits() {
        return xyUnits;
    }
    /**
     * Sets units of the attribute data, i.e. the xy-values in the raster image.
     * @param XYUnits The specified data type.
     */
    public void setXYUnits(String XYUnits) {
        xyUnits = XYUnits.toLowerCase();
    }

    private String projection = "not specified";
    /**
     * Retrieves the projection for this Whitebox grid.
     * @return Projection.
     */
    public String getProjection() {
        return projection;
    }
    /**
     * Sets projection the raster image.
     * @param Projection The specified projection.
     */
    public void setProjection(String Projection) {
        projection = Projection;
    }

    private double displayMinimum = largeValue;
    /**
     * Retrieves the display minimum for this Whitebox grid.
     * @return Display minimum.
     */
    public double getDisplayMinimum() {
        return displayMinimum;
    }
    /**
     * Sets display minimum.
     * @param DisplayMinimum The specified projection.
     */
    public void setDisplayMinimum(double DisplayMinimum) {
        displayMinimum = DisplayMinimum;
    }

    private double displayMaximum = smallValue;
    /**
     * Retrieves the display maximum for this Whitebox grid.
     * @return Display maximum.
     */
    public double getDisplayMaximum() {
        return displayMaximum;
    }
    /**
     * Sets display maximum.
     * @param DisplayMaximum The specified projection.
     */
    public void setDisplayMaximum(double DisplayMaximum) {
        displayMaximum = DisplayMaximum;
    }

    private String preferredPalette = "not specified";
    /**
     * Retrieves the preferred palette for this Whitebox grid.
     * @return Preferred palette.
     */
    public String getPreferredPalette() {
        return preferredPalette;
    }
    /**
     * Sets the preferred palette used to display the raster image.
     * @param PreferredPalette The default palette used to display the image, 
     * e.g. <b><i>earthtones.pal</b></i> or <b><i>spectrum.pal</b></i>.
     */
    public void setPreferredPalette(String PreferredPalette) {
        PreferredPalette.replace(".plt", ".pal");
        preferredPalette = PreferredPalette;
    }

    private long bufferSize = Runtime.getRuntime().totalMemory(); //20 * 1048576; //in bytes
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

    private double noDataValue = -32768d;
    /**
     * Retrieves the numeric value used to specify a grid cell containing no data or void.
     * @return float NoData value.
     */
    public double getNoDataValue() {
        return noDataValue;
    }
    /**
     * Sets the NoData value used in this raster image.
     * @param value A float specifying the value used. Default value is -32768.
     */
    public void setNoDataValue(double value) {
        noDataValue = value;
    }

    private double cellSizeX = 0;
    /**
     * The grid resolution in the X direction.
     * @return float containing the x-direction grid resolution
     */
    public double getCellSizeX() {
        if (cellSizeX == 0) {
            cellSizeX = Math.abs(this.east - this.west) / this.numberColumns;
        }
        return cellSizeX;
    }

    private double cellSizeY = 0;
    /**
     * The grid resolution in the Y direction.
     * @return float containing the y-direction grid resolution
     */
    public double getCellSizeY() {
        if (cellSizeY == 0) {
            cellSizeY = Math.abs(this.east - this.west) / this.numberColumns;
        }
        return cellSizeY;
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

    private ByteOrder byteOrder = java.nio.ByteOrder.nativeOrder(); // "LITTLE_ENDIAN";
    /**
     * Gets the file byte order (either LITTLE_ENDIAN or BIG_ENDIAN).
     * @return
     */
    public String getByteOrder() {
        return byteOrder.toString();
    }
    /**
     * Sets the file byte order (either LITTLE_ENDIAN or BIG_ENDIAN).
     * @param value
     */
    public void setByteOrder(String value) {
        if ((value.toLowerCase().contains("little")) || (value.toLowerCase().contains("lsb"))
                || (value.toLowerCase().contains("least")) || (value.toLowerCase().contains("intel"))) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }
    }

    private boolean saveChanges = true;
    /**
     * Used to determine the file access mode set during object construction.
     * @return "rw" (read/write) or "r" (read-only).
     */
    public String getFileAccess() {
        if (saveChanges) {
            return "rw";
        } else {
            return "r";
        }
    }

    private void setFileAccess(String value) {
        if (value.toLowerCase().contains("w")) {
            saveChanges = true;
        } else {
            saveChanges = false;
        }
    }

    private ArrayList<String> metadata = new ArrayList<String>();
    /**
     * Adds a metadata entry to the header file.
     * @param value String containing the metadata entry.
     */
    public void addMetadataEntry(String value) {
        metadata.add(value.replaceAll(";", ":"));
    }

    /**
     * Retrieves an ArrayList containing all of the metadata entries for this raster.
     * @return ArrayList of metadata strings.
     */
    public ArrayList<String> getMetadata() {
        return metadata;
    }

    /**
     * Used to delete a metadata entry
     * @param i The entry number in the metadata arraylist to delete.
     */
    public void deleteMetadataEntry(int i) {
        if (i < metadata.size()) {
            metadata.remove(i);
        }
    }
    
    private double stdDeviation = noDataValue;
    public double getStandardDeviation() {
        if (stdDeviation == noDataValue) {
            readStatsFile();
        }
        return stdDeviation;
    }
    
    private double mode = noDataValue;
    public double getMode() {
        if (mode == noDataValue) {
            readStatsFile();
        }
        return mode;
    }
    
    private double mean = noDataValue;
    public double getMean() {
        if (mean == noDataValue) {
            readStatsFile();
        }
        return mean;
    }
    
    private double median = noDataValue;
    public double getMedian() {
        if (median == noDataValue) {
            readStatsFile();
        }
        return median;
    }
    
    private long[] histo = null;
    public long[] getHisto() {
        if (mean == noDataValue) {
            readStatsFile();
        }
        return histo;
    }
    
    private double binWidth = noDataValue;
    public double getHistoBinWidth() {
        if (binWidth == noDataValue) {
            readStatsFile();
        }
        return binWidth;
    }
    
    private long numValidCells = (long)noDataValue;
    public long getnumValidCells() {
        if (numValidCells == (long)noDataValue) {
            readStatsFile();
        }
        return numValidCells;
    }
    



    //********************************************
    // Available methods.
    // *******************************************
    
    /**
     * This method should be used when you need to access an entire row of data
     * at a time. It has less overhead that the getValue method and can be used
     * to efficiently scan through a raster image row by row. It will read the 
     * specified row from disk and will not store it internally within the
     * WhiteboxRaster. As such, this method is appropriate when each of the cells
     * in the raster need to be accessed sequentially one time only. This is the
     * case, for example, when the raster is displayed as an image.
     * @param row An int stating the zero-based row to be returned.
     * @return An array of doubles containing the values store in the specified row.
     */
    public double[] getRowValues(int row) {
        if (row < 0 || row >= numberRows) { return null; }
        
        double[] retVals = new double[numberColumns];
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (!file.exists()) {
                createNewDataFile();
            }
            
            // what is the starting and ending cell?
            long startingCell = row * numberColumns;
            long endingCell = startingCell + numberColumns - 1;

            int writeLengthInCells = (int)(endingCell - startingCell + 1);
            buf = ByteBuffer.allocate((int) (writeLengthInCells * cellSizeInBytes));

            rIn = new RandomAccessFile(dataFile, "r");
            
            FileChannel inChannel = rIn.getChannel();
            
            inChannel.position(startingCell * cellSizeInBytes);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(byteOrder);

            
            if (dataType.equals("double")) {
                buf.rewind();
                DoubleBuffer db = buf.asDoubleBuffer();
                retVals = new double[writeLengthInCells];
                db.get(retVals);
                db = null;
                buf = null;
            } else if (dataType.equals("float")) {
                buf.rewind();
                FloatBuffer fb = buf.asFloatBuffer();
                float[] fa = new float[writeLengthInCells];
                fb.get(fa);
                fb = null;
                buf = null;
                retVals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    retVals[j] = fa[j];
                }
                fa = null;
            } else if (dataType.equals("integer")) {
                buf.rewind();
                ShortBuffer ib = buf.asShortBuffer();
                short[] ia = new short[writeLengthInCells];
                ib.get(ia);
                ib = null;
                buf = null;
                retVals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    retVals[j] = ia[j];
                }
                ia = null;
            } else if (dataType.equals("byte")) {
                buf.rewind();
                byte[] ba = new byte[writeLengthInCells];
                buf.get(ba);
                buf = null;
                retVals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    retVals[j] = ba[j];
                }
                ba = null;
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try{ rIn.close(); } catch (Exception e){}
            }
            numberOfDataFileReads++;
            return retVals.clone();
        }
        
        
    }

    private boolean readRow(int row) {
        double[] vals = new double[numberColumns];
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (!file.exists()) {
                createNewDataFile();
            }
            
            // what is the starting and ending cell?
            long startingCell = row * numberColumns;
            long endingCell = startingCell + numberColumns - 1;

            int writeLengthInCells = (int)(endingCell - startingCell + 1);
            buf = ByteBuffer.allocate((int) (writeLengthInCells * cellSizeInBytes));

            rIn = new RandomAccessFile(dataFile, "r");
            
            FileChannel inChannel = rIn.getChannel();
            
            inChannel.position(startingCell * cellSizeInBytes);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(byteOrder);
            
            if (dataType.equals("double")) {
                buf.rewind();
                DoubleBuffer db = buf.asDoubleBuffer();
                vals = new double[writeLengthInCells];
                db.get(vals);
                db = null;
                buf = null;
            } else if (dataType.equals("float")) {
                buf.rewind();
                FloatBuffer fb = buf.asFloatBuffer();
                float[] fa = new float[writeLengthInCells];
                fb.get(fa);
                fb = null;
                buf = null;
                vals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    vals[j] = fa[j];
                }
                fa = null;
            } else if (dataType.equals("integer")) {
                buf.rewind();
                ShortBuffer ib = buf.asShortBuffer();
                short[] ia = new short[writeLengthInCells];
                ib.get(ia);
                ib = null;
                buf = null;
                vals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    vals[j] = ia[j];
                }
                ia = null;
            } else if (dataType.equals("byte")) {
                buf.rewind();
                byte[] ba = new byte[writeLengthInCells];
                buf.get(ba);
                buf = null;
                vals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    vals[j] = ba[j];
                }
                ba = null;
            }

            if (numRowsInMemory >= blockSize) {
                Date minLastUsedTime = lastUsed[0];
                int whichRow = 0;
                for (int i = 1; i < numberRows; i++) {
                    if (lastUsed[i].before(minLastUsedTime)) {
                        minLastUsedTime = lastUsed[i];
                        whichRow = i;
                    }
                }
                if (rowIsDirty[whichRow] && saveChanges) {
                    writeRow(whichRow);
                }
                grid[whichRow] = null;
                numRowsInMemory--;
            }
            grid[row] = vals;
            numRowsInMemory++;
            
            return true;
        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
            return false;
        } finally {
            if (rIn != null) {
                try{ rIn.close(); } catch (Exception e){}
            }
            numberOfDataFileReads++;
        }
        
    }
    
    private boolean writeRow(int row) {
        if (!saveChanges) { return false; }
        
        // update the minimum and maximum values
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < numberColumns; i++) {
            if (grid[row][i] < min && grid[row][i] != noDataValue) { min = grid[row][i]; }
            if (grid[row][i] > max && grid[row][i] != noDataValue) { max = grid[row][i]; }
        }
        if (max > maximumValue) { maximumValue = max; }
        if (min < minimumValue) { minimumValue = min; }
        
        RandomAccessFile rOut = null;
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
            FileChannel outChannel = rOut.getChannel();
            outChannel.position(startingCell * cellSizeInBytes);
            int writeLengthInCells = (int) (endingCell - startingCell + 1);

            if (dataType.equals("double")) {
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                DoubleBuffer db = buf.asDoubleBuffer();
                db.put(grid[row]);
                db = null;
                outChannel.write(buf);
            } else if (dataType.equals("float")) {
                float[] fa = new float[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    fa[j] = (float)grid[row][j];
                }
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                FloatBuffer fb = buf.asFloatBuffer();
                fb.put(fa);
                fb = null;
                fa = null;
                outChannel.write(buf);
            } else if (dataType.equals("integer")) {
                short[] ia = new short[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ia[j] = (short)grid[row][j];
                }
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                ShortBuffer ib = buf.asShortBuffer();
                ib.put(ia);
                ib = null;
                ia = null;
                outChannel.write(buf);
            } else if (dataType.equals("byte")) {
                byte[] ba = new byte[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ba[j] = (byte)grid[row][j];
                }
                buf = ByteBuffer.wrap(ba);
                ba = null;
                outChannel.write(buf);
            }
            outChannel.close();

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            buf = null;
            if (rOut != null) {
                try{ rOut.close(); } catch (Exception e){}
            }
            rowIsDirty[row] = false;
            numberOfDataFileWrites++;
            return true;
        }
    }
    
    private void writeAllRows() {
        for (int i = 0; i < numberRows; i++) {
            if (saveChanges && rowIsDirty[i]) {
                writeRow(i);
            }
        }
    }
    
    /**
     * Retrieves the value contained at a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @return The value contained in the raster grid at the specified grid cell.
     */
    public double getValue(int row, int column){
        if (column >= 0 && column < numberColumns && row >= 0 && row < numberRows) {
            
            if (grid[row] == null) {
                readRow(row);
            }
        
            return grid[row][column];
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
     * @param row the zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to place in the grid cell.
     */
    public void setValue(int row, int column, double value){
        if (saveChanges && column >= 0 && column < this.numberColumns && 
                row >= 0 && row < this.numberRows) {
            if (grid[row] == null) {
                readRow(row);
            }
        
            grid[row][column] = value;
            rowIsDirty[row] = true;
            
            isDirty = true;
        }
    }


    /**
     * Reads the contents of the header file and fills the properties of the 
     * Whitebox grid.
     */
    private void readHeaderFile() {
        DataInputStream in = null;
        BufferedReader br = null;
        boolean byteOrderRead = false;
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(this.headerFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            if (this.headerFile != null) {
                String line;
                String[] str;
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    str = line.split("\t");
                    if (str[0].toLowerCase().contains("min:") && (str[0].toLowerCase().contains("display") == false)) {
                        this.minimumValue = Float.parseFloat(str[1]);
                    } else if (str[0].toLowerCase().contains("max:") && (str[0].toLowerCase().contains("display") == false)) {
                        this.maximumValue = Float.parseFloat(str[1]);
                    } else if (str[0].toLowerCase().contains("north")) {
                        this.north = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("south")) {
                        this.south = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("west")) {
                        this.west = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("east")) {
                        this.east = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("cols")) {
                        this.numberColumns =  Integer.parseInt(str[1]);
                    } else if (str[0].toLowerCase().contains("rows")) {
                        this.numberRows =  Integer.parseInt(str[1]);
                    } else if (str[0].toLowerCase().contains("data type")) {
                        this.setDataType(str[1]);
                    } else if (str[0].toLowerCase().contains("data scale")) {
                        if (str[1].toLowerCase().contains("continuous")) {
                            this.setDataScale(DATA_SCALE_CONTINUOUS);
                        } else if (str[1].toLowerCase().contains("categorical")) {
                            this.setDataScale(DATA_SCALE_CATEGORICAL);
                        } else if (str[1].toLowerCase().contains("bool")) {
                            this.setDataScale(DATA_SCALE_BOOLEAN);
                        } else if (str[1].toLowerCase().contains("rgb")) {
                            this.setDataScale(DATA_SCALE_RGB);
                        }
                    } else if (str[0].toLowerCase().contains("z units")) {
                        this.setZUnits(str[1]);
                    } else if (str[0].toLowerCase().contains("xy units")) {
                        this.setXYUnits(str[1]);
                    } else if (str[0].toLowerCase().contains("projection")) {
                        this.projection = str[1];
                    } else if (str[0].toLowerCase().contains("display min")) {
                        this.displayMinimum = Float.parseFloat(str[1]);
                    } else if (str[0].toLowerCase().contains("display max")) {
                        this.displayMaximum = Float.parseFloat(str[1]);
                    } else if (str[0].toLowerCase().contains("preferred palette")) {
                        this.preferredPalette = str[1].replace(".plt", ".pal");
                    } else if (str[0].toLowerCase().contains("byte order")) {
                        this.setByteOrder(str[1]);
                        byteOrderRead = true;
                    } else if (str[0].toLowerCase().contains("nodata")) {
                        this.noDataValue = Double.parseDouble(str[1]);
                        System.out.println(str[1]);
                    } else if (str[0].toLowerCase().contains("metadata entry")) {
                        if (str.length > 1) { this.addMetadataEntry(str[1]); }
                    }
                }
                System.out.println("hello");
                //Close the input stream
                in.close();
                br.close();

                if (byteOrderRead == false) {
                    this.byteOrder = ByteOrder.LITTLE_ENDIAN;
                }

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

    /**
     * Writes the whiteboxRaster header file (.dep) to disc.
     */
    public void writeHeaderFile() {
        String str1 = null;
        File file = new File(this.headerFile);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            if (this.displayMaximum == smallValue) {
                this.displayMaximum = this.maximumValue;
            }
            if (this.displayMinimum == largeValue) {
                this.displayMinimum = this.minimumValue;
            }
            
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            str1 = "Min:\t" + Double.toString(this.minimumValue);
            out.println(str1);
            str1 = "Max:\t" + Double.toString(this.maximumValue);
            out.println(str1);
            str1 = "North:\t" + Double.toString(this.north);
            out.println(str1);
            str1 = "South:\t" + Double.toString(this.south);
            out.println(str1);
            str1 = "East:\t" + Double.toString(this.east);
            out.println(str1);
            str1 = "West:\t" + Double.toString(this.west);
            out.println(str1);
            str1 = "Cols:\t" + Integer.toString(this.numberColumns);
            out.println(str1);
            str1 = "Rows:\t" + Integer.toString(this.numberRows);
            out.println(str1);
            str1 = "Data Type:\t" + this.dataType;
            out.println(str1);
            str1 = "Z Units:\t" + this.zUnits;
            out.println(str1);
            str1 = "XY Units:\t" + this.xyUnits;
            out.println(str1);
            str1 = "Projection:\t" + this.projection;
            out.println(str1);
            if (this.dataScale == DATA_SCALE_CONTINUOUS) {
                str1 = "Data Scale:\tcontinuous";
            } else if (this.dataScale == DATA_SCALE_CATEGORICAL) {
                str1 = "Data Scale:\tcategorical";
            } else if (this.dataScale == DATA_SCALE_BOOLEAN) {
                str1 = "Data Scale:\tboolean";
            } else if (this.dataScale == DATA_SCALE_RGB) {
                str1 = "Data Scale:\trgb";
            }
            out.println(str1);
            str1 = "Display Min:\t" + Double.toString(this.displayMinimum);
            out.println(str1);
            str1 = "Display Max:\t" + Double.toString(this.displayMaximum);
            out.println(str1);
            str1 = "Preferred Palette:\t" + this.preferredPalette.replace(".plt", ".pal");
            out.println(str1);
            str1 = "NoData:\t" + Double.toString(this.noDataValue);
            out.println(str1);
            str1 = "Byte Order:\t" + this.byteOrder;
            out.println(str1);

            // Write the metadata entries to the file
            if (metadata.size() > 0) {
                for (int i = 0; i < metadata.size(); i++) {
                    str1 = "Metadata Entry:\t" + metadata.get(i).replaceAll(":", ";");
                    out.println(str1);
                }
            }


        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }

        }
    }

    private void setBlockData() {
        try {
            blockSize = (int) (Math.round(Math.floor(bufferSize / 8))) / 3;
            if (blockSize > (numberRows * numberColumns)) {
                blockSize = numberRows * numberColumns;
                bufferSize = blockSize * 8;
            }
            blockSize = (int) blockSize / numberColumns;

            grid = new double[numberRows][];
            lastUsed = new Date[numberRows];
            Arrays.fill(lastUsed, new Date());
            rowIsDirty = new boolean[numberRows];
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createNewDataFile() {

        RandomAccessFile rOut = null;
        ByteBuffer buf = null;
        try {
            long numberCells = numberRows * numberColumns;
            int writeLength = 2000000;
            long numCellsWritten = 0;

            rOut = new RandomAccessFile(dataFile, "rw");

            FileChannel outChannel = rOut.getChannel();
            outChannel.position(0);

            if (dataType.equals("double")) {
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                    }
                    double[] da = new double[writeLength];
                    Arrays.fill(da, initialValue);
                    buf = ByteBuffer.allocate(cellSizeInBytes * writeLength);
                    buf.order(byteOrder);
                    DoubleBuffer db = buf.asDoubleBuffer();
                    db.put(da);
                    outChannel.write(buf);
                    db = null;
                    da = null;
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
            } else if (dataType.equals("float")) {
                
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                    }
                    float[] fa = new float[writeLength];
                    Arrays.fill(fa, (float)initialValue);
                    buf = ByteBuffer.allocate(cellSizeInBytes * writeLength);
                    buf.order(byteOrder);
                    FloatBuffer fb = buf.asFloatBuffer();
                    fb.put(fa);
                    outChannel.write(buf);
                    fb = null;
                    fa = null;
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
            } else if (dataType.equals("integer")) {
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                    }
                    short[] ia = new short[(int)writeLength];
                    Arrays.fill(ia, (short)initialValue);
                    buf = ByteBuffer.allocate(cellSizeInBytes * writeLength);
                    buf.order(byteOrder);
                    ShortBuffer ib = buf.asShortBuffer();
                    ib.put(ia);
                    outChannel.write(buf);
                    ib = null;
                    ia = null;
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
            } else if (dataType.equals("byte")) {
                do {
                    if ((numCellsWritten + writeLength) > numberCells) {
                        writeLength = (int)(numberCells - numCellsWritten);
                    }
                    byte[] ba = new byte[writeLength];
                    Arrays.fill(ba, (byte)initialValue);
                    buf = ByteBuffer.wrap(ba);
                    outChannel.write(buf);
                    ba = null;
                    numCellsWritten += writeLength;
                } while (numCellsWritten < numberCells);
            }
            outChannel.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            buf = null;
            if (rOut != null) {
                try{ rOut.close(); } catch (Exception e){}
            }
        }

    }

    /*
    private void readDataBlock() {
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (file.exists() == false) {
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
            
            FileChannel inChannel = rIn.getChannel();
            
            inChannel.position(blockStartingCell * cellSizeInBytes);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(byteOrder);

            
            if (dataType.equals("double")) {
                buf.rewind();
                DoubleBuffer db = buf.asDoubleBuffer();
                grid = new double[writeLengthInCells];
                db.get(grid);
                db = null;
                buf = null;
            } else if (dataType.equals("float")) {
                buf.rewind();
                FloatBuffer fb = buf.asFloatBuffer();
                float[] fa = new float[writeLengthInCells];
                fb.get(fa);
                fb = null;
                buf = null;
                grid = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    grid[j] = fa[j];
                }
                fa = null;
            } else if (dataType.equals("integer")) {
                buf.rewind();
                ShortBuffer ib = buf.asShortBuffer();
                short[] ia = new short[writeLengthInCells];
                ib.get(ia);
                ib = null;
                buf = null;
                grid = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    grid[j] = ia[j];
                }
                ia = null;
            } else if (dataType.equals("byte")) {
                buf.rewind();
                byte[] ba = new byte[writeLengthInCells];
                buf.get(ba);
                buf = null;
                grid = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    grid[j] = ba[j];
                }
                ba = null;
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try{ rIn.close(); } catch (Exception e){}
            }
            numberOfDataFileReads++;
        }
    }
     * 
     */

    /**
     * Dumps the data block currently in memory to the data file.
     */
    /*private void writeDataBlock() {
        if (saveChanges == false) { return; }
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
        try {

            // See if the data file exists.
            File file = new File(dataFile);
            if (!file.exists()) {
                createNewDataFile();
            }

            rOut = new RandomAccessFile(dataFile, "rw");
            FileChannel outChannel = rOut.getChannel();
            outChannel.position(blockStartingCell * cellSizeInBytes);
            int writeLengthInCells = (int) (blockEndingCell - blockStartingCell + 1);

            if (dataType.equals("double")) {
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                DoubleBuffer db = buf.asDoubleBuffer();
                db.put(grid);
                db = null;
                outChannel.write(buf);
            } else if (dataType.equals("float")) {
                float[] fa = new float[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    fa[j] = (float)grid[j];
                }
                buf = ByteBuffer.allocate(cellSizeInBytes * writeLengthInCells);
                buf.order(byteOrder);
                FloatBuffer fb = buf.asFloatBuffer();
                fb.put(fa);
                fb = null;
                fa = null;
                outChannel.write(buf);
            } else if (dataType.equals("integer")) {
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
            } else if (dataType.equals("byte")) {
                byte[] ba = new byte[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    ba[j] = (byte)grid[j];
                }
                buf = ByteBuffer.wrap(ba);
                ba = null;
                outChannel.write(buf);
            }
            outChannel.close();

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            buf = null;
            if (rOut != null) {
                try{ rOut.close(); } catch (Exception e){}
            }
            isDirty = false;
            numberOfDataFileWrites++;
        }

    }*/
    

    private void setPropertiesUsingAnotherRaster(String BaseRasterHeader, String DataType) {

        // Set the data type.
        if (DataType.toLowerCase().equals("float")) {
            dataType = "float";
            cellSizeInBytes = 4;
        } else if (DataType.toLowerCase().equals("integer")) {
            dataType = "integer";
            cellSizeInBytes = 2;
        } else if (DataType.toLowerCase().equals("byte")) {
            dataType = "byte";
            cellSizeInBytes = 1;
        } else if (DataType.toLowerCase().equals("double")) {
            dataType = "double";
            cellSizeInBytes = 8;
        } else {
            dataType = "float";
            cellSizeInBytes = 4;
        }

        // Set the properties of this WhiteboxRaster to those of the base raster.
        DataInputStream in = null;
        BufferedReader br = null;
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(BaseRasterHeader);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            if (BaseRasterHeader != null) {
                String line;
                String[] str;
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    str = line.split("\t");
                    if (str[0].toLowerCase().contains("north")) {
                        this.north = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("south")) {
                        this.south = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("west")) {
                        this.west = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("east")) {
                        this.east = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("cols")) {
                        this.numberColumns =  Integer.parseInt(str[1]);
                    } else if (str[0].toLowerCase().contains("rows")) {
                        this.numberRows =  Integer.parseInt(str[1]);
                    } else if (str[0].toLowerCase().contains("data scale")) {
                        if (str[1].toLowerCase().contains("continuous")) {
                            this.setDataScale(DATA_SCALE_CONTINUOUS);
                        } else if (str[1].toLowerCase().contains("categorical")) {
                            this.setDataScale(DATA_SCALE_CATEGORICAL);
                        } else if (str[1].toLowerCase().contains("bool")) {
                            this.setDataScale(DATA_SCALE_BOOLEAN);
                        } else if (str[1].toLowerCase().contains("rgb")) {
                            this.setDataScale(DATA_SCALE_RGB);
                        }
                    } else if (str[0].toLowerCase().contains("xy units")) {
                        this.xyUnits = str[1];
                    } else if (str[0].toLowerCase().contains("projection")) {
                        this.projection = str[1];
                    } else if (str[0].toLowerCase().contains("nodata value")) {
                        this.noDataValue = Float.parseFloat(str[1]);
                    }
                }

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

        // Save the header file.
        this.writeHeaderFile();
    }
    
    /**
     * Used to find the minimum and maximum values in the raster. NoDataValues are ignored.
     * Minimum and maximum values are stored in the minimumValue and maximumValue fields.
     */
    public void findMinAndMaxVals() {
        double[] data;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double z;
        for (int row = 0; row < numberRows; row++) {
            data = getRowValues(row);
            for (int col = 0; col < numberColumns; col++) {
                z = data[col];
                if (z != noDataValue) {
                    if (z < min) { min = z; }
                    if (z > max) { max = z; }
                }
            }
        }
        maximumValue = max;
        minimumValue = min;
    }
    
    private double[] cumulativeHisto = null;
    public double getPercentileValue(double percentile) {
        if (mean == noDataValue) {
            readStatsFile();
        }
        percentile = percentile / 100;
        double retVal = 0;
        double x1, x2;
        double y1, y2;
        
        if (cumulativeHisto == null) {
            cumulativeHisto = new double[histo.length];
            
            cumulativeHisto[0] = histo[0];
            for (int i = 1; i < histo.length; i++) {
                cumulativeHisto[i] = histo[i] + cumulativeHisto[i - 1];
            }
            for (int i = 0; i < histo.length; i++) {
                cumulativeHisto[i] = cumulativeHisto[i] / numValidCells;
            }
        }
        for (int i = 0; i < histo.length; i++) {
            if (cumulativeHisto[i] >= percentile) { // find the first bin with a value greater than percentile.
                if (i > 0) {
                    x1 = minimumValue + (i - 1) * binWidth;
                    x2 = minimumValue + i * binWidth;
                    y1 = cumulativeHisto[i - 1];
                    y2 = cumulativeHisto[i];
                    
                } else {
                    x1 = minimumValue + (i - 1) * binWidth;
                    x2 = minimumValue + i * binWidth;
                    y1 = 0;
                    y2 = cumulativeHisto[i];
                    
                }
                retVal = x1 + (percentile - y1) / (y2 - y1) * binWidth;
                break;
            }
        }
        return retVal;
    }
    
    public void readStatsFile() {
        File file = new File(statsFile);
        if (!file.exists()) { 
            createStatsFile();
            return;
        }
        
        DataInputStream in = null;
        BufferedReader br = null;
        boolean statsFlag = false;
        boolean histoFlag = false;
        int i = 0;
        long histoVal = 0;
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(statsFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));

            if (statsFile != null) {
                String line;
                String[] str;
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    str = line.split("\t");
                    if (str[0].toLowerCase().contains("start_stats")) {
                        statsFlag = true;
                    }
                    if (str[0].toLowerCase().contains("end_stats")) {
                        statsFlag = false;
                    }
                    if (str[0].toLowerCase().contains("start_histo")) {
                        histoFlag = true;
                    }
                    if (str[0].toLowerCase().contains("end_histo")) {
                        histoFlag = false;
                    }
                    if (statsFlag) {
                        if (str[0].toLowerCase().contains("mean")) {
                            this.mean = Double.parseDouble(str[1]);
                        } else if (str[0].toLowerCase().contains("median")) {
                            this.median = Double.parseDouble(str[1]);
                        } else if (str[0].toLowerCase().contains("mode")) {
                            this.mode = Double.parseDouble(str[1]);
                        } else if (str[0].toLowerCase().contains("std_dev")) {
                            this.stdDeviation = Double.parseDouble(str[1]);
                        } else if (str[0].toLowerCase().contains("num_valid_cells")) {
                            this.numValidCells = Long.parseLong(str[1]);
                        }
                    } else if (histoFlag) {
                        if (str[0].toLowerCase().contains("bin_width")) {
                            this.binWidth = Double.parseDouble(str[1]);
                        } else if (str[0].toLowerCase().contains("num_bins")) {
                            histo = new long[Integer.parseInt(str[1])];
                            i = 0;
                        } else if (str[0].toLowerCase().contains("start_histo") == false) {
                            histo[i] = Long.parseLong(str[0]);
                            i++;
                        }
                    }
                    
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
    
    /**
     * Creates a .wst file to store information about the statistical distribution
     * of the raster, including the min, max, mean, mode, stdev, and the histogram. These
     * data are used for clipping the tails of the distribution for enhanced visualization.
     */
    public void createStatsFile() {
        File file = new File(statsFile);
        if (file.exists()) { 
            file.delete(); 
        }
        
        mean = 0;
        mode = 0;
        long n = 0;
        double[] data;
        double imageTotalDeviation = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double z;
        double[] rowMedians = new double[numberRows];
        
        binWidth = 0;
        int binNum = 0;
        int numberOfBins = 0;
        
            
        if (dataScale != DATA_SCALE_RGB) {
            
            // calculate the mean, min and max.
            for (int row = 0; row < numberRows; row++) {
                data = getRowValues(row);
                for (int col = 0; col < numberColumns; col++) {
                    z = data[col];
                    if (z != noDataValue) {
                        mean += z;
                        n++;
                        if (z < min) {
                            min = z;
                        }
                        if (z > max) {
                            max = z;
                        }
                    }
                }
            }
            
            maximumValue = max;
            minimumValue = min;
            mean = mean / n;
            numValidCells = n;
            
            if (dataType.equals("integer")) {
                numberOfBins = (int)(max - min + 1);
                binWidth = 1;
            } else if (dataType.equals("float") || dataType.equals("double")) {
                if ((max - min) < 1024) {
                    numberOfBins = 1024;
                } else if ((max - min) < 2048) {
                    numberOfBins = 2048;
                } else if ((max - min) < 4096) {
                    numberOfBins = 4096;
                } else {
                    numberOfBins = 8196;
                }
                binWidth = (max - min) / (numberOfBins - 1);
            }
            
            histo = new long[numberOfBins];
            
            // figure out how many bins should be in the histogram

            for (int row = 0; row < numberRows; row++) {
                data = getRowValues(row);
                for (int col = 0; col < numberColumns; col++) {
                    z = data[col];
                    if (z != noDataValue) {
                        imageTotalDeviation += (z - mean) * (z - mean);
                        binNum = (int)(Math.floor((z - min) / binWidth));
                        histo[binNum]++;
                    }
                }
            }
            
            stdDeviation = Math.sqrt(imageTotalDeviation / (n - 1));

            long highestVal = 0;
            int highestBin = 0;
            for (int i = 0; i < histo.length; i++) {
                if (histo[i] > highestVal) { 
                    highestVal = histo[i]; 
                    highestBin = i;
                }
            }

            mode = highestBin * binWidth;
            median = getPercentileValue(50.0d);
            

            String str = null;
            FileWriter fw = null;
            BufferedWriter bw = null;
            PrintWriter out = null;
            try {
                fw = new FileWriter(file, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                str = "START_STATS:";
                out.println(str);
                str = "MIN: \t" + Double.toString(this.minimumValue);
                out.println(str);
                str = "MAX: \t" + Double.toString(this.maximumValue);
                out.println(str);
                str = "MEAN: \t" + Double.toString(mean);
                out.println(str);
                str = "MEDIAN: \t" + Double.toString(median);
                out.println(str);
                str = "MODE: \t" + Double.toString(mode);
                out.println(str);
                str = "STD_DEV: \t" + Double.toString(stdDeviation);
                out.println(str);
                str = "NUM_VALID_CELLS: \t" + Long.toString(n);
                out.println(str);
                str = "END_STATS";
                out.println(str);
                
                str = "START_HISTO";
                out.println(str);
                str = "BIN_WIDTH: \t" + binWidth;
                out.println(str);
                str = "NUM_BINS: \t" + numberOfBins;
                out.println(str);
                for (int i = 0; i < histo.length; i++) {
                    str = String.valueOf(histo[i]);
                    out.println(str);
                }
                str = "END_HISTO";
                out.println(str);
                
            } catch (java.io.IOException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (Exception e) { //Catch exception if any
                System.err.println("Error: " + e.getMessage());
            } finally {
                if (out != null || bw != null) {
                    out.flush();
                    out.close();
                }

            }

        } else {
            numberOfBins = 256;
        }
    }
    
    /**
     * Used to perform closing functionality when a whiteboxRaster is no longer needed.
     */
    public void close() {
        if (this.isTemporaryFile) {
            File f1 = new File(this.headerFile);
            f1.delete();
            f1 = new File(this.dataFile);
            f1.delete();
        } else {
            if (saveChanges) {
                if (isDirty) { writeAllRows(); }
                findMinAndMaxVals();
                writeHeaderFile();
            }
        }
        grid = null;
    }

}
