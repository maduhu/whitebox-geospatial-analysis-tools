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

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
/**
 * The abstract base class serving the WhiteboxRaster and WhiteboxRasterInfo 
 * subclasses.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public abstract class WhiteboxRasterBase {
    // ***********************************
    // Property getter and setter methods.
    // ***********************************

    protected String headerFile;
    protected String shortHeaderName = null;
    /**
     * Gets the header file (*.dep) name for this Whitebox raster grid.
     * Notice that the header file name is set during object creation.
     * @return A string containing the reference to the header file.
     */
    public String getHeaderFile() {
        return headerFile;
    }
    
    
    /**
     * Returns the file name with no directory path for the header file (*.dep).
     * @return A string containing the reference to the header file.
     */
    public String getShortHeaderFile() {
        if (shortHeaderName == null) {
            File file = new File(headerFile);
            shortHeaderName = file.getName();
            shortHeaderName = shortHeaderName.replace(".dep", "");
        }
        return shortHeaderName;
    }

    protected String dataFile;
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
    
    protected String statsFile;
    /**
     * Gets the statistical distribution file (*.wstat) name for this Whitebox raster
     * grid. Notice that the stats file name is set during object creation.
     * @return A string containing the reference to the stats file.
     */
    public String getStatsFile() {
        return statsFile;
    }
    
    protected final double largeValue = Double.POSITIVE_INFINITY;
    protected final double smallValue = Double.NEGATIVE_INFINITY;
    
    protected double minimumValue  = largeValue;
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

    protected double maximumValue = smallValue;
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
    
    protected double north;
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

    protected double south;
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

    protected double west;
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

    protected double east;
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
    
    protected int numberColumns;
    /**
     * Retrieves the number of columns in the grid.
     * @return The number of columns.
     */
    public int getNumberColumns() {
        return numberColumns;
    }

    protected int numberRows;
    /**
     * Retrieves the number of rows in the grid.
     * @return The number of rows.
     */
    public int getNumberRows() {
        return numberRows;
    }
    
    public enum DataScale {
        CONTINUOUS, CATEGORICAL, BOOLEAN, RGB;
    }
    
    protected DataScale dataScale = DataScale.CONTINUOUS;
    /**
     * Retrieves the data scale for this Whitebox grid. Data scale may be <b><i>CONTINUOUS</i></b>, 
     * <i><b>CATEGORICAL</i></b>, <i><b>BOOLEAN</i></b>, or <i><b>RGB</i></b>.
     * @return DataScale Data scale.
     */
    public DataScale getDataScale() {
        return dataScale;
    }
    /**
     * Sets the data scale for this Whitebox grid. Data scale may be <b><i>CONTINUOUS</i></b>, 
     * <i><b>CATEGORICAL</i></b>, <i><b>BOOLEAN</i></b>, or <i><b>RGB</i></b>.
     * @param DataScale The specified data type.
     */
    public void setDataScale(DataScale dataScale) {
        this.dataScale = dataScale;
    }

    public enum DataType {
        DOUBLE, FLOAT, INTEGER, BYTE;
    }
    
    protected int cellSizeInBytes = 8;
    protected DataType dataType = DataType.DOUBLE;
    /**
     * Retrieves the data type for this Whitebox grid. Data type may be <b><i>DataType.DOUBLE</i></b>, <b><i>DataType.FLOAT</i></b> (decimal
     * numbers), <i><b>DataType.INTEGER</i></b> (whole numbers from -32,768 to 32,767), or <i><b>DataType.BYTE</i></b> 
     * (whole number from 0 to 255).
     * @return Data type.
     */
    public DataType getDataType() {
        return dataType;
    }
    /**
     * Sets the data type for this Whitebox grid. Data type may be <b><i>DataType.DOUBLE</i></b>, <b><i>DataType.FLOAT</i></b> (decimal
     * numbers), <i><b>DataType.INTEGER</i></b> (whole numbers from -32,768 to 32,767), or <i><b>DataType.BYTE</i></b> (whole
     * number from 0 to 255).
     * @param DataType The specified data type.
     */
    public void setDataType(DataType dataType) {
        switch (dataType) {
            case DOUBLE:
                this.dataType = DataType.DOUBLE;
                cellSizeInBytes = 8;
                break;
            case FLOAT:
                this.dataType = DataType.FLOAT;
                cellSizeInBytes = 4;
                break;
            case INTEGER:
                this.dataType = DataType.INTEGER;
                cellSizeInBytes = 2;
                break;
            case BYTE:
                this.dataType = DataType.BYTE;
                cellSizeInBytes = 1;
                break;
        }
    }
    
    protected String zUnits = "not specified";
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

    protected String xyUnits = "not specified";
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

    protected String projection = "not specified";
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

    protected double displayMinimum = largeValue;
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

    protected double displayMaximum = smallValue;
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

    protected String preferredPalette = "grey.pal";
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
        if (PreferredPalette.lastIndexOf(File.separator) > -1) {
            String[] str = PreferredPalette.split(File.separator);
            PreferredPalette = str[str.length - 1];
        }
        preferredPalette = PreferredPalette;
    }

    protected double noDataValue = -32768d;
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

    protected double cellSizeX = 0;
    /**
     * The grid resolution in the X direction.
     * @return float containing the x-direction grid resolution
     */
    public double getCellSizeX() {
        if (cellSizeX == 0) {
            cellSizeX = (this.east - this.west) / this.numberColumns;
        }
        return cellSizeX;
    }

    protected double cellSizeY = 0;
    /**
     * The grid resolution in the Y direction.
     * @return float containing the y-direction grid resolution
     */
    public double getCellSizeY() {
        if (cellSizeY == 0) {
            cellSizeY = (this.north - this.south) / this.numberRows;
        }
        return cellSizeY;
    }

    protected ByteOrder byteOrder = java.nio.ByteOrder.nativeOrder(); // "LITTLE_ENDIAN";
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

    protected boolean saveChanges = true;
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

    protected void setFileAccess(String value) {
        if (value.toLowerCase().contains("w")) {
            saveChanges = true;
        } else {
            saveChanges = false;
        }
    }
    
    protected ArrayList<String> metadata = new ArrayList<String>();
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
    
    protected double stdDeviation = noDataValue;
    public double getStandardDeviation() {
        if (stdDeviation == noDataValue) {
            readStatsFile();
        }
        return stdDeviation;
    }
    
    protected double mode = noDataValue;
    public double getMode() {
        if (mode == noDataValue) {
            readStatsFile();
        }
        return mode;
    }
    
    protected double mean = noDataValue;
    public double getMean() {
        if (mean == noDataValue) {
            readStatsFile();
        }
        return mean;
    }
    
    protected double median = noDataValue;
    public double getMedian() {
        if (median == noDataValue) {
            readStatsFile();
        }
        return median;
    }
    
    protected long[] histo = null;
    public long[] getHisto() {
        if (mean == noDataValue) {
            readStatsFile();
        }
        return histo;
    }
    
    protected double binWidth = noDataValue;
    public double getHistoBinWidth() {
        if (binWidth == noDataValue) {
            readStatsFile();
        }
        return binWidth;
    }
    
    protected long numValidCells = (long)noDataValue;
    public long getNumValidCells() {
        if (numValidCells == (long)noDataValue) {
            readStatsFile();
        }
        return numValidCells;
    }
    
    protected boolean containsFractionalData = false;
    protected boolean containsFractionalDataChecked = false;
    /*
     * This property can be used to assess whether the data contained in this
     * Whitebox raster possesses fractional parts, i.e. if there are any
     * pixels with values that are decimals. If the data type is integer or byte,
     * this method will always return false. If the data type is either float or
     * decimal, then each pixel value will be assessed to identify the existence of
     * a fractional part. If a cell is found with a fractional part, the method will
     * return a value of true. This function is necessary because it is common
     * for integer type data to be stored as decimal type data.
     */
    public boolean doesDataContainFractionalParts() {
        if (!containsFractionalDataChecked) {
            checkContainsFractionalData();
        }
        return containsFractionalData;
    }
    
    //********************************************
    // Available methods.
    // *******************************************

    // checks to see if the data contains decimal values.
    private void checkContainsFractionalData() {
        if (dataType == DataType.INTEGER || dataType == DataType.BYTE) {
            containsFractionalDataChecked = true;
            containsFractionalData = false;
            return;
        }
        double[] data = null;
        double z;
        containsFractionalDataChecked = true;
        containsFractionalData = false;
        for (int row = 0; row < numberRows; row++) {
            data = getRowValues(row);
            for (int col = 0; col < numberColumns; col++) {
                if (data[col] != noDataValue) {
                    z = Math.floor(data[col]);
                    if ((data[col] - z) > 0.001) { // you have to deal with rounding issues
                        containsFractionalData = true;
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Reads the contents of the header file and fills the properties of the Whitebox grid.
     */
    protected void readHeaderFile() {
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
                    if (str[0].toLowerCase().contains("min:") && (!str[0].toLowerCase().contains("display"))) {
                        this.minimumValue = Float.parseFloat(str[1]);
                    } else if (str[0].toLowerCase().contains("max:") && (!str[0].toLowerCase().contains("display"))) {
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
                        //this.setDataType(str[1]);
                        if (str[1].toLowerCase().contains("double")) {
                            this.setDataType(DataType.DOUBLE);
                        } else if (str[1].toLowerCase().contains("float")) {
                            this.setDataType(DataType.FLOAT);
                        } else if (str[1].toLowerCase().contains("integer")) {
                            this.setDataType(DataType.INTEGER);
                        } else if (str[1].toLowerCase().contains("byte")) {
                            this.setDataType(DataType.BYTE);
                        }
                    } else if (str[0].toLowerCase().contains("data scale")) {
                        if (str[1].toLowerCase().contains("continuous")) {
                            this.setDataScale(DataScale.CONTINUOUS); //DATA_SCALE_CONTINUOUS);
                        } else if (str[1].toLowerCase().contains("categorical")) {
                            this.setDataScale(DataScale.CATEGORICAL); //DATA_SCALE_CATEGORICAL);
                        } else if (str[1].toLowerCase().contains("bool")) {
                            this.setDataScale(DataScale.BOOLEAN); //DATA_SCALE_BOOLEAN);
                        } else if (str[1].toLowerCase().contains("rgb")) {
                            this.setDataScale(DataScale.RGB); //DATA_SCALE_RGB);
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
                        this.noDataValue = Float.parseFloat(str[1]);
                    } else if (str[0].toLowerCase().contains("metadata entry")) {
                        if (str.length > 1) { this.addMetadataEntry(str[1]); }
                    }
                }
                if (this.displayMinimum == Float.POSITIVE_INFINITY) {
                    this.displayMinimum = this.minimumValue;
                }
                if (this.displayMaximum == Float.NEGATIVE_INFINITY) {
                    this.displayMaximum = this.maximumValue;
                }
                //Close the input stream
                in.close();
                br.close();

                if (!byteOrderRead) {
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
            if (this.displayMaximum < this.displayMinimum ||
                    this.displayMaximum == this.displayMinimum) {
                if (this.maximumValue < this.minimumValue) { findMinAndMaxVals(); }
                this.displayMinimum = this.minimumValue;
                this.displayMaximum = this.maximumValue;
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
            switch (this.dataScale) {
                case CONTINUOUS:
                    str1 = "Data Scale:\tcontinuous";
                    break;
                
                case CATEGORICAL:
                    str1 = "Data Scale:\tcategorical";
                    break;
                
                case BOOLEAN:
                    str1 = "Data Scale:\tboolean";
                    break;
                
                case RGB:
                    str1 = "Data Scale:\trgb";
                    break;
                    
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
    
    protected void setPropertiesUsingAnotherRaster(String BaseRasterHeader, DataType dataType) {
        setDataType(dataType);

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
                            this.setDataScale(DataScale.CONTINUOUS);
                            //this.setDataScale(DATA_SCALE_CONTINUOUS);
                        } else if (str[1].toLowerCase().contains("categorical")) {
                            this.setDataScale(DataScale.CATEGORICAL);
                            //this.setDataScale(DATA_SCALE_CATEGORICAL);
                        } else if (str[1].toLowerCase().contains("bool")) {
                            this.setDataScale(DataScale.BOOLEAN);
                            //this.setDataScale(DATA_SCALE_BOOLEAN);
                        } else if (str[1].toLowerCase().contains("rgb")) {
                            this.setDataScale(DataScale.RGB);
                            //this.setDataScale(DATA_SCALE_RGB);
                        }
                    } else if (str[0].toLowerCase().contains("xy units")) {
                        this.xyUnits = str[1];
                    } else if (str[0].toLowerCase().contains("projection")) {
                        this.projection = str[1];
                    } else if (str[0].toLowerCase().contains("nodata")) {
                        this.noDataValue = Double.parseDouble(str[1]);
                    } else if (str[0].toLowerCase().contains("palette")) {
                        this.preferredPalette = str[1];
                    }
                }

            }
        } catch (java.io.IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            System.out.println("Error: " + e.getMessage());
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
     * This method should be used when you need to access an entire row of data
     * at a time. It has less overhead that the getValue method and can be used
     * to efficiently scan through a raster image row by row.
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
                return null;
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

            
            if (dataType == DataType.DOUBLE) { //.equals("double")) {
                buf.rewind();
                DoubleBuffer db = buf.asDoubleBuffer();
                retVals = new double[writeLengthInCells];
                db.get(retVals);
                db = null;
                buf = null;
            } else if (dataType == DataType.FLOAT) { //.equals("float")) {
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
            } else if (dataType == DataType.INTEGER) { //.equals("integer")) {
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
            } else if (dataType == DataType.BYTE) { //.equals("byte")) {
                buf.rewind();
                //byte[] ba = new byte[writeLengthInCells];
                //buf.get(ba);
                //buf = null;
                retVals = new double[writeLengthInCells];
                for (int j = 0; j < writeLengthInCells; j++) {
                    retVals[j] = whitebox.utilities.Unsigned.getUnsignedByte(buf, j); //ba[j]);
                }
                //ba = null;
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try{ rIn.close(); } catch (Exception e){}
            }
            return retVals.clone();
        }
        
        
    }
    
    /**
     * This is a lightweight method of setting individual pixel values. It writes
     * values directly to the file without the use of a buffer. As such it is only
     * useful for setting small numbers of pixels. The setValue method of the 
     * WhiteboxRaster class offers a buffered means of setting individual pixel
     * values and is far better suited to setting larger numbers of pixels. This
     * method should only be used for existing files.
     * @param row Pixel zero-based row number.
     * @param column Pixel zero-based column number.
     * @param value Pixel value to set.
     */
    public void setPixelValue(int row, int column, double value) {
        // update the minimum and maximum values
        if (value < minimumValue && value != noDataValue) { 
            minimumValue = value; 
            displayMinimum =  value;
            writeHeaderFile();
        }
        if (value > maximumValue && value != noDataValue) { 
            maximumValue = value; 
            displayMaximum = value;
            writeHeaderFile();
        }
        
        RandomAccessFile rOut = null;
        ByteBuffer buf = null;
        FileChannel outChannel = null;
        try {

            rOut = new RandomAccessFile(dataFile, "rw");
            outChannel = rOut.getChannel();
            int cellNum = row * numberColumns + column;
            outChannel.position(cellNum * cellSizeInBytes);
            int writeLengthInCells = 1;
            
            if (dataType == DataType.DOUBLE) { 
                buf = ByteBuffer.allocate(cellSizeInBytes);
                buf.order(byteOrder);
                DoubleBuffer db = buf.asDoubleBuffer();
                db.put(value);
                db = null;
                outChannel.write(buf);
            } else if (dataType == DataType.FLOAT) {
                float fa = (float)value;
                buf = ByteBuffer.allocateDirect(cellSizeInBytes);
                buf.order(byteOrder);
                FloatBuffer fb = buf.asFloatBuffer();
                fb.put(fa);
                fb = null;
                outChannel.write(buf);
            } else if (dataType == DataType.INTEGER) {
                short ia = (short)value;
                buf = ByteBuffer.allocate(cellSizeInBytes);
                buf.order(byteOrder);
                ShortBuffer ib = buf.asShortBuffer();
                ib.put(ia);
                ib = null;
                outChannel.write(buf);
            } else if (dataType == DataType.BYTE) { 
                byte[] ba = { (byte)value };
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
        }
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
    
    protected double[] cumulativeHisto = null;
    public double getPercentileValue(double percentile) {
        if (mean == noDataValue || mean == -32768d) {
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
        
            
        if (dataScale != DataScale.RGB) { //DATA_SCALE_RGB) {
            
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
            
            if (dataType == DataType.INTEGER) { //.equals("integer")) {
                numberOfBins = (int)(max - min + 1);
                binWidth = 1;
            } else if (dataType == DataType.FLOAT || dataType == DataType.DOUBLE) { //.equals("float") || dataType.equals("double")) {
                if ((max - min) < 512) {
                    numberOfBins = 512;
                } else if ((max - min) < 1024) {
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
    
    private double halfCellSizeX = -1;
    private double EWRange = -1;
    public int getColumnFromXCoordinate(double x) {
        if (halfCellSizeX < 0 || EWRange < 0) {
            getCellSizeX();
            halfCellSizeX = cellSizeX / 2.0;
            EWRange = east - west - cellSizeX;
        }
        return (int)(Math.round((numberColumns - 1) * (x - west - halfCellSizeX) / EWRange));
    }
    
    
    private double halfCellSizeY = -1;
    private double NSRange = -1;
    public int getRowFromYCoordinate(double y) {
        if (halfCellSizeY < 0 || NSRange < 0) {
            getCellSizeY();
            halfCellSizeY = cellSizeY / 2.0;
            NSRange = north - south - cellSizeY;
        }
        return (int)(Math.round((numberRows - 1) * (north - halfCellSizeY - y) / NSRange));
    }
    
    private double[] xCoordsByColumn;
    public double getXCoordinateFromColumn(int column) {
        if (halfCellSizeX < 0 || EWRange < 0) {
            getCellSizeX();
            halfCellSizeX = cellSizeX / 2.0;
            EWRange = east - west - cellSizeX;
        }
        if (xCoordsByColumn == null) {
            xCoordsByColumn = new double[numberColumns];
            for (int i = 0; i < numberColumns; i++) {
                xCoordsByColumn[i] = west + halfCellSizeX + i * cellSizeX;
            }
        }
        if (column >= 0 && column < numberColumns) {
            return xCoordsByColumn[column];
        } else {
            return west + halfCellSizeX + column * cellSizeX;
        }
    }
    
    private double[] yCoordsByRow;
    public double getYCoordinateFromRow(int row) {
        if (halfCellSizeY < 0 || NSRange < 0) {
            getCellSizeY();
            halfCellSizeY = cellSizeY / 2.0;
            NSRange = north - south - cellSizeY;
        }
        if (yCoordsByRow == null) {
            yCoordsByRow = new double[numberRows];
            for (int i = 0; i < numberRows; i++) {
                yCoordsByRow[i] = north - halfCellSizeY - i * cellSizeY;
            }
        }
        if (row >= 0 && row < numberRows) {
            return yCoordsByRow[row];
        } else {
            return north - halfCellSizeY - row * cellSizeY;
        }
    }
    
    public abstract void close();
}
