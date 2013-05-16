/*
 *  * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import whitebox.interfaces.MapLayer;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class RasterLayerInfo implements MapLayer {

    private WhiteboxRasterInfo source = null;
    private String paletteDirectory = null;
    private double noDataValue = 0;
    private int overlayNumber = 0;
    private int rows, cols;
    public boolean increasesEastward = false;
    public boolean increasesNorthward = false;
    private boolean dirty = true;
    private String[] defaultPalettes;
    private double[] data = null;
    private boolean visibleInLegend = true;

    /* Constructors*/
    public RasterLayerInfo() {
        
    }
    
    public RasterLayerInfo(String headerFile, String paletteDirectory, String[] defaultPalettes, int alpha, int overlayNum) {
        // check to see that the file exists.
        File file = new File(headerFile);
        if (!file.exists()) {
            System.out.println("File not found.");
        }
        this.headerFile = headerFile;
        this.layerTitle = file.getName().replace(".dep", "");
        source = new WhiteboxRasterInfo(headerFile);
        this.paletteDirectory = paletteDirectory;
        this.defaultPalettes = defaultPalettes.clone();
        if (!source.getPreferredPalette().toLowerCase().equals("not specified")) {
            this.paletteFile = paletteDirectory + source.getPreferredPalette();
            // see if the palette exists. if not, give it one that does.
            file = new File(this.paletteFile);
            if (!file.exists()) {
                if (source.getDataScale() == WhiteboxRaster.DataScale.CONTINUOUS) {
                    this.paletteFile = paletteDirectory + defaultPalettes[0];
                } else if (source.getDataScale() == WhiteboxRaster.DataScale.CATEGORICAL
                        || source.getDataScale() == WhiteboxRaster.DataScale.BOOLEAN) {
                    this.paletteFile = paletteDirectory + defaultPalettes[1];
                } else {
                    this.paletteFile = paletteDirectory + defaultPalettes[2];
                }
            }
        } else {
            if (source.getDataScale() == WhiteboxRaster.DataScale.CONTINUOUS) {
                this.paletteFile = paletteDirectory + defaultPalettes[0];
            } else if (source.getDataScale() == WhiteboxRaster.DataScale.CATEGORICAL
                    || source.getDataScale() == WhiteboxRaster.DataScale.BOOLEAN) {
                this.paletteFile = paletteDirectory + defaultPalettes[1];
            } else {
                this.paletteFile = paletteDirectory + defaultPalettes[2];
            }
        }
        this.imageWidth = source.getNumberColumns();
        this.imageHeight = source.getNumberRows();
        this.alpha = alpha;
        this.noDataValue = source.getNoDataValue();
        this.overlayNumber = overlayNum;
        this.rows = source.getNumberRows();
        this.cols = source.getNumberColumns();
        this.setDataScale(source.getDataScale());
        minVal = source.getDisplayMinimum();
        maxVal = source.getDisplayMaximum();

        if (source.getEast() > source.getWest()) {
            increasesEastward = true;
        } else {
            increasesEastward = false;
        }

        if (source.getNorth() > source.getSouth()) {
            increasesNorthward = true;
        } else {
            increasesNorthward = false;
        }

        currentExtent = new BoundingBox(source.getWest(), source.getSouth(), 
                source.getEast(), source.getNorth());

        fullExtent = currentExtent.clone();
    }

    public RasterLayerInfo(String headerFile, String paletteFile, int alpha, int overlayNum) {
        // check to see that the file exists.
        File file = new File(headerFile);
        if (!file.exists()) {
            System.out.println("File not found.");
        }
        this.headerFile = headerFile;
        this.layerTitle = file.getName().replace(".dep", "");
        source = new WhiteboxRasterInfo(headerFile);
        // see if the paletteDirectory has been set.
        if((null != paletteDirectory) && (paletteDirectory.length() == 0)) {
            // do nothing
	} else {
            int a = paletteFile.lastIndexOf(File.separator);
            paletteDirectory = paletteFile.substring(0, a + 1);
	}
//        if (paletteDirectory.isEmpty()) {
//            int a = paletteFile.lastIndexOf(File.separator);
//            paletteDirectory = paletteFile.substring(a);
//        }
        // see if the palette file exists
        if (!(new File(paletteFile).exists())) {
            paletteFile = paletteDirectory + "spectrum.pal";
        }
        this.paletteFile = paletteFile;
        this.imageWidth = source.getNumberColumns();
        this.imageHeight = source.getNumberRows();
        this.alpha = alpha;
        this.noDataValue = source.getNoDataValue();
        this.overlayNumber = overlayNum;
        this.rows = source.getNumberRows();
        this.cols = source.getNumberColumns();
        this.setDataScale(source.getDataScale());
        minVal = source.getDisplayMinimum();
        maxVal = source.getDisplayMaximum();

        if (source.getEast() > source.getWest()) {
            increasesEastward = true;
        } else {
            increasesEastward = false;
        }

        if (source.getNorth() > source.getSouth()) {
            increasesNorthward = true;
        } else {
            increasesNorthward = false;
        }

        currentExtent = new BoundingBox(source.getWest(), source.getSouth(),
                source.getEast(), source.getNorth());

        fullExtent = currentExtent.clone();
    }
    /* Property getters and setters */
    private String headerFile = "";

    public String getHeaderFile() {
        return headerFile;
    }

    public int getRowFromYCoordinate(double y) {
        return source.getRowFromYCoordinate(y);
    }
    
    public int getColFromXCoordinate(double x) {
        return source.getRowFromYCoordinate(x);
    }
    
    public double getXCoordinateFromColumn(int col) {
        return source.getXCoordinateFromColumn(col);
    }
    
    public double getYCoordinateFromRow(int row) {
        return source.getYCoordinateFromRow(row);
    }
    
    public double getCellSizeX() {
        return source.getCellSizeX();
    }

    public double getCellSizeY() {
        return source.getCellSizeY();
    }
    
    private int alpha = 255;
    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int value) {
        if (value < 0) {
            value = 0;
        }
        if (value > 255) {
            value = 255;
        }
        alpha = value;
    }
    private double gamma = 1.0;

    public double getNonlinearity() {
        return gamma;
    }

    public void setNonlinearity(double value) {
        gamma = value;
    }
    private String paletteFile = "";

    public String getPaletteFile() {
        return paletteFile;
    }

    public void setPaletteFile(String fileName) {
        if (!paletteFile.equals(fileName)) {
            paletteFile = fileName;
            paletteData = null;
            // update the header file
            source.setPreferredPalette(fileName.replace(paletteDirectory, ""));
            source.writeHeaderFile();
        }
    }
    private int[] paletteData = null;
    private int numPaletteEntries = 0;

    public int[] getPaletteData() {
        return paletteData;
    }

    public int getNumPaletteEntries() {
        if (numPaletteEntries <= 0) {
            readPalette();
        }
        return numPaletteEntries;
    }
    private int[] pixelData = null;

    public int[] getPixelData() {
        if (pixelData == null || pixelData.length == 0 || dirty) {
            createPixelData();
        }
        return pixelData;
    }
    private int imageWidth = 0;

    public int getImageWidth() {
        return imageWidth;
    }
    private int imageHeight = 0;

    public int getImageHeight() {
        return imageHeight;
    }

    public int getNumberColumns() {
        return cols;
    }

    public int getNumberRows() {
        return rows;
    }

    public double getNoDataValue() {
        return source.getNoDataValue();
    }
    private String XYUnits = "metres";

    public String getXYUnits() {
        return source.getXYUnits();
    }
    private String layerTitle = "";

    @Override
    public String getLayerTitle() {
        return layerTitle;
    }

    @Override
    public void setLayerTitle(String title) {
        layerTitle = title;
    }

    @Override
    public int getOverlayNumber() {
        return overlayNumber;
    }

    @Override
    public void setOverlayNumber(int num) {
        overlayNumber = num;
    }
    private double minVal = 0;

    public double getDisplayMinVal() {
        return minVal;
    }

    public void setDisplayMinVal(double value) {
        minVal = value;
        source.setDisplayMinimum(value);
        source.writeHeaderFile();
    }

    public double getMinVal() {
        return source.getMinimumValue();
    }
    private double maxVal = 0;

    public double getDisplayMaxVal() {
        return maxVal;
    }

    public void setDisplayMaxVal(double value) {
        maxVal = value;
        source.setDisplayMaximum(value);
        source.writeHeaderFile();
    }

    public double getMaxVal() {
        return source.getMaximumValue();
    }
    private WhiteboxRaster.DataScale dataScale = WhiteboxRaster.DataScale.CONTINUOUS;

    /**
     * Retrieves the data scale for this Whitebox grid. Data scale may be <b><i>DATA_SCALE_CONTINUOUS</i></b> (0), 
     * <i><b>DATA_SCALE_CATEGORICAL</i></b> (1), <i><b>DATA_SCALE_BOOLEAN</i></b> (2), or <i><b>DATA_SCALE_RGB</i></b> (3).
     * @return int Data scale.
     */
    public WhiteboxRaster.DataScale getDataScale() {
        return dataScale;
    }

    /**
     * Sets the data scale for this Whitebox grid. Data scale may be <b><i>DATA_SCALE_CONTINUOUS</i></b> (0), 
     * <i><b>DATA_SCALE_CATEGORICAL</i></b> (1), <i><b>DATA_SCALE_BOOLEAN</i></b> (2), or <i><b>DATA_SCALE_RGB</i></b> (3).
     * @param DataScale The specified data type.
     */
    public final void setDataScale(WhiteboxRaster.DataScale DataScale) {
        dataScale = DataScale;

        source.setDataScale(dataScale);
        source.writeHeaderFile();
    }

    public long getDataFileSize() {
        return source.getDataFileSize();
    }
    private boolean paletteReversed = false;

    public boolean isPaletteReversed() {
        return paletteReversed;
    }

    public void setPaletteReversed(boolean value) {
        if (value != paletteReversed) {
            paletteReversed = value;
            dirty = true;
            if (paletteData == null) {
                readPalette();
            }
            int[] copyPalette = new int[numPaletteEntries];
            for (int i = 0; i < numPaletteEntries; i++) {
                copyPalette[i] = paletteData[numPaletteEntries - i - 1];
            }
            paletteData = copyPalette.clone();
        }
    }
    public BoundingBox fullExtent = null;

    @Override
    public BoundingBox getFullExtent() {
        return fullExtent.clone();
    }
    BoundingBox currentExtent = null;

    @Override
    public BoundingBox getCurrentExtent() {
        return currentExtent.clone();
    }

    @Override
    public void setCurrentExtent(BoundingBox bb) {
        if (!bb.equals(currentExtent)) {
            currentExtent = bb.clone();
            dirty = true;
        }
    }
    private int resolutionFactor = 1;

    public void setResolutionFactor(int value) {
        if (value < 1) {
            value = 1;
        }
        if (value != resolutionFactor) {
            resolutionFactor = value;
            dirty = true;
        }
    }

    public double getDataValue(int row, int col) {
        int r = (int) (((double) row - startRow) / (endRow - startRow) * (imageHeight - 1) + 0.5);
        int c = (int) (((double) col - startCol) / (endCol - startCol) * (imageWidth - 1) + 0.5);
        int cellNum = (int) (r * imageWidth + c);
        if (cellNum > data.length) {
            return Double.NaN;
        }
        return data[cellNum];
    }

    /**
     * Accesses WhiteboxRasterBase's setPixelValue method. This is a lightweight 
     * method of setting individual pixel values. It writes values directly to 
     * the file without the use of a buffer. As such it is only useful for 
     * setting small numbers of pixels. The setValue method of the WhiteboxRaster 
     * class offers a buffered means of setting individual pixel values and is 
     * far better suited to setting larger numbers of pixels. This method should 
     * only be used for existing files.
     * @param row Pixel zero-based row number.
     * @param column Pixel zero-based column number.
     * @param value Pixel value to set. 
     */
    public void setDataValue(int row, int column, double value) {
        source.setPixelValue(row, column, value);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public WhiteboxRasterInfo getWhiteboxRasterInfo() {
        return source;
    }
    
    
    int startRow;
    int endRow;
    int startCol;
    int endCol;

    public void createPixelData() {
        try {

            if ((paletteData == null) && (dataScale != WhiteboxRaster.DataScale.RGB)) {
                readPalette();
            }

            startRow = (int) (Math.abs(fullExtent.getMaxY() - currentExtent.getMaxY()) / source.getCellSizeY());
            endRow = (int) (rows - (Math.abs(fullExtent.getMinY() - currentExtent.getMinY()) / source.getCellSizeY())) - 1;
            startCol = (int) (Math.abs(fullExtent.getMinX() - currentExtent.getMinX()) / source.getCellSizeX());
            endCol = (int) (cols - (Math.abs(fullExtent.getMaxX() - currentExtent.getMaxX()) / source.getCellSizeX())) - 1;
            int row, col;
            double range = maxVal - minVal;
            double value = 0;
            int entryNum = 0;

            // check the numCells
            imageHeight = 0;
            imageWidth = 0;
            for (row = startRow; row <= endRow; row += resolutionFactor) {
                imageHeight++;
            }
            for (col = startCol; col <= endCol; col += resolutionFactor) {
                imageWidth++;
            }
            
            int numCells = imageHeight * imageWidth;

            WhiteboxRasterInfo sourceData = new WhiteboxRasterInfo(source.getHeaderFile());

            int backgroundColour = 0; // transparent black
            pixelData = new int[numCells];
            data = new double[numCells];

            int numPaletteEntriesLessOne = numPaletteEntries - 1;

            //long startTime = System.currentTimeMillis();
        
            double[] rawData;
            int i = 0;
            if (dataScale == WhiteboxRaster.DataScale.CONTINUOUS) {
//                CreatePixelsContinuous cpc = new CreatePixelsContinuous(sourceData, startRow, endRow,
//                        startCol, endCol, resolutionFactor, minVal, maxVal, gamma, paletteData, backgroundColour);
//                cpc.createPixels();
//                pixelData = cpc.getPixels();
                for (row = startRow; row <= endRow; row += resolutionFactor) {
                    rawData = sourceData.getRowValues(row);
                    for (col = startCol; col <= endCol; col += resolutionFactor) {
                        value = rawData[col]; //sourceData.getValue(row, col);
                        if (value != noDataValue) {
                            entryNum = (int) (Math.pow(((value - minVal) / range), gamma) * numPaletteEntriesLessOne);
                            if (entryNum < 0) {
                                entryNum = 0;
                            }
                            if (entryNum > numPaletteEntriesLessOne) {
                                entryNum = numPaletteEntriesLessOne;
                            }
                            pixelData[i] = paletteData[entryNum];
                        } else {
                            pixelData[i] = backgroundColour;
                        }
                        data[i] = value;
                        i++;
                    }
                }

            } else if (dataScale == WhiteboxRaster.DataScale.CATEGORICAL) {
                for (row = startRow; row <= endRow; row += resolutionFactor) {
                    rawData = sourceData.getRowValues(row);
                    for (col = startCol; col <= endCol; col += resolutionFactor) {
                        value = rawData[col]; //sourceData.getValue(row, col);
                        if (value != noDataValue) {
                            entryNum = (int) (value - minVal) % numPaletteEntries;
                            if (entryNum < 0) {
                                entryNum = 0;
                            }
                            if (entryNum > numPaletteEntriesLessOne) {
                                entryNum = numPaletteEntriesLessOne;
                            }
                            pixelData[i] = paletteData[entryNum];
                        } else {
                            pixelData[i] = backgroundColour;
                        }
                        data[i] = value;
                        i++;
                    }
                }
            } else if (dataScale == WhiteboxRaster.DataScale.BOOLEAN) {
                for (row = startRow; row <= endRow; row += resolutionFactor) {
                    rawData = sourceData.getRowValues(row);
                    for (col = startCol; col <= endCol; col += resolutionFactor) {
                        value = rawData[col]; //sourceData.getValue(row, col);
                        if (value != noDataValue) {
                            if (value > 0) {
                                entryNum = numPaletteEntriesLessOne;
                            } else {
                                entryNum = 0;
                            }
                            pixelData[i] = paletteData[entryNum];
                        } else {
                            pixelData[i] = backgroundColour;
                        }
                        data[i] = value;
                        i++;
                    }
                }
            } else if (dataScale == WhiteboxRaster.DataScale.RGB) {
                int r, g, b, a, val;
                for (row = startRow; row <= endRow; row += resolutionFactor) {
                    rawData = sourceData.getRowValues(row);
                    for (col = startCol; col <= endCol; col += resolutionFactor) {
                        value = rawData[col]; //sourceData.getValue(row, col);
                        if (value != noDataValue) {
                            val = (int) value;
                            a = (val >> 24) & 0xFF;
                            a = (int) (a * alpha / 255d);
                            b = (val >> 16) & 0xFF;
                            g = (val >> 8) & 0xFF;
                            r = val & 0xFF;
                            pixelData[i] = (a << 24) | (r << 16) | (g << 8) | b;
                        } else {
                            pixelData[i] = backgroundColour;
                        }
                        data[i] = value;
                        i++;
                    }
                }
            }

            //long endTime = System.currentTimeMillis();

            //System.out.println("CreatePixels took " + (endTime - startTime) + " milliseconds.");

            sourceData.close();
            sourceData = null;

            dirty = false;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void readPalette() {
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        int i;
        try {
            // see if the file exists, if not, set it to the default palette.
            File file = new File(paletteFile);
            if (!file.exists()) {
                if (source.getDataScale() == WhiteboxRaster.DataScale.CONTINUOUS) {
                    this.paletteFile = paletteDirectory + defaultPalettes[0];
                } else if (source.getDataScale() == WhiteboxRaster.DataScale.CATEGORICAL
                        || source.getDataScale() == WhiteboxRaster.DataScale.BOOLEAN) {
                    this.paletteFile = paletteDirectory + defaultPalettes[1];
                } else {
                    this.paletteFile = paletteDirectory + defaultPalettes[3];
                }
            }


            numPaletteEntries = (int) (file.length() / 4);

            buf = ByteBuffer.allocate(numPaletteEntries * 4);

            rIn = new RandomAccessFile(paletteFile, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // Read the data.
            buf.rewind();
            IntBuffer ib = buf.asIntBuffer();
            paletteData = new int[numPaletteEntries];
            ib.get(paletteData);
            ib = null;

            // Update the palette for the alpha value.
            if (alpha < 255) {
                int r, g, b, val;
                for (i = 0; i < numPaletteEntries; i++) {
                    val = paletteData[i];
                    r = (val >> 16) & 0xFF;
                    g = (val >> 8) & 0xFF;
                    b = val & 0xFF;
                    paletteData[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
                }
            }

            if (paletteReversed) {
                int[] copyPalette = new int[numPaletteEntries];
                for (i = 0; i < numPaletteEntries; i++) {
                    copyPalette[i] = paletteData[numPaletteEntries - i - 1];
                }
                paletteData = copyPalette.clone();
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }

    }

    public double clipLowerTail(double percent) {
        return source.getPercentileValue(percent);
    }

    public double clipUpperTail(double percent) {
        return source.getPercentileValue(100 - percent);
    }

    public void update() {
        readPalette();
        createPixelData();
    }

    public void clipLayerToExtent(BoundingBox extent, String outputFileName) {
        String str1 = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;

        try {
            // Do the two extents overlap?
            boolean cond1, cond2, cond3, cond4;
            if (fullExtent.getMinY() < fullExtent.getMaxY()) { // y-axis increases upwards
                cond1 = (extent.getMaxY() < fullExtent.getMinY());
                cond2 = (extent.getMinY() > fullExtent.getMaxY());
            } else { // y-axis increases downwards
                cond1 = (extent.getMaxY() > fullExtent.getMinY());
                cond2 = (extent.getMinY() < fullExtent.getMaxY());
            }

            if (fullExtent.getMinX() < fullExtent.getMaxX()) { // x-axis increases to right
                cond3 = (extent.getMinX() > fullExtent.getMaxX());
                cond4 = (extent.getMaxX() < fullExtent.getMinX());
            } else { // x-axis increases to left
                cond3 = (extent.getMinX() < fullExtent.getMaxX());
                cond4 = (extent.getMaxX() > fullExtent.getMinX());
            }

            if (!cond1 && !cond2 && !cond3 && !cond4) {

                // Find the box of intersection between extent and fullExtent.
                // And, what is the range of rows and columns in the source image to read?
                double top, bottom, left, right;
                int fromRow, toRow, fromCol, toCol;

                if (fullExtent.getMinY() < fullExtent.getMaxY()) {
                    top = Math.min(extent.getMaxY(), fullExtent.getMaxY());
                    bottom = Math.max(extent.getMinY(), fullExtent.getMinY());
                } else {
                    top = Math.max(extent.getMaxY(), fullExtent.getMaxY());
                    bottom = Math.min(extent.getMinY(), fullExtent.getMinY());
                }
                if (fullExtent.getMinX() < fullExtent.getMaxX()) {
                    left = Math.max(extent.getMinX(), fullExtent.getMinX());
                    right = Math.min(extent.getMaxX(), fullExtent.getMaxX());
                } else {
                    left = Math.min(extent.getMinX(), fullExtent.getMinX());
                    right = Math.max(extent.getMaxX(), fullExtent.getMaxX());
                }

                fromRow = (int) ((fullExtent.getMaxY() - top) / (fullExtent.getMaxY() - fullExtent.getMinY()) * (rows - 0.5));
                toRow = (int) ((fullExtent.getMaxY() - bottom) / (fullExtent.getMaxY() - fullExtent.getMinY()) * (rows - 0.5));
                fromCol = (int) ((left - fullExtent.getMinX()) / (fullExtent.getMaxX() - fullExtent.getMinX()) * (cols - 0.5));
                toCol = (int) ((right - fullExtent.getMinX()) / (fullExtent.getMaxX() - fullExtent.getMinX()) * (cols - 0.5));

                if (fromRow > toRow) {
                    int i = fromRow;
                    fromRow = toRow;
                    toRow = i;
                }
                if (fromCol > toCol) {
                    int i = fromCol;
                    fromCol = toCol;
                    toCol = i;
                }
                
                // recalculate top, bottom, left and right to align with the row/col coordinates
                double gridResY = source.getCellSizeY();
                double gridResX = source.getCellSizeX();
                top = fullExtent.getMaxY() - fromRow * gridResY;
                bottom = fullExtent.getMaxY() - (toRow + 1) * gridResY;
                left = fullExtent.getMinX() + fromCol * gridResX;
                right = fullExtent.getMinX() + (toCol + 1) * gridResX;

                int nRows = toRow - fromRow + 1;
                int nCols = toCol - fromCol + 1;

                // see if the outputFileName already exists, and if so delete it
                if (new File(outputFileName).exists()) {
                    new File(outputFileName).delete();
                    new File(outputFileName.replace(".dep", ".tas")).delete();
                }
                
                String dataType = "float";
                switch (source.getDataType()) {
                    case DOUBLE:
                        dataType = "double";
                        break;
                    case FLOAT:
                        dataType = "float";
                        break;
                    case INTEGER:
                        dataType = "integer";
                        break;
                    case BYTE:
                        dataType = "byte";
                        break;
                }
                
                String dataScale = "continuous";
                switch (source.getDataScale()) {
                    case CONTINUOUS:
                        dataScale = "continuous";
                        break;

                    case CATEGORICAL:
                        dataScale = "categorical";
                        break;

                    case BOOLEAN:
                        dataScale = "boolean";
                        break;

                    case RGB:
                        dataScale = "rgb";
                        break;

                }
                // write the header file for the new raster
                fw = new FileWriter(outputFileName, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
                out.println(str1);
                str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
                out.println(str1);
                str1 = "North:\t" + Double.toString(top);
                out.println(str1);
                str1 = "South:\t" + Double.toString(bottom);
                out.println(str1);
                str1 = "East:\t" + Double.toString(right);
                out.println(str1);
                str1 = "West:\t" + Double.toString(left);
                out.println(str1);
                str1 = "Cols:\t" + Integer.toString(nCols);
                out.println(str1);
                str1 = "Rows:\t" + Integer.toString(nRows);
                out.println(str1);
                str1 = "Data Type:\t" + dataType;
                out.println(str1);
                str1 = "Z Units:\t" + source.getZUnits();
                out.println(str1);
                str1 = "XY Units:\t" + source.getXYUnits();
                out.println(str1);
                str1 = "Projection:\t" + source.getProjection();
                out.println(str1);
                str1 = "Data Scale:\t" + dataScale;
                out.println(str1);
                str1 = "Preferred Palette:\t" + source.getPreferredPalette();
                out.println(str1);
                str1 = "NoData:\t" + source.getNoDataValue();
                out.println(str1);
                if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
                } else {
                    str1 = "Byte Order:\t" + "BIG_ENDIAN";
                }
                out.println(str1);

                // Create the whitebox raster object.
                WhiteboxRaster wbr = new WhiteboxRaster(outputFileName, "rw");
                double[] rowData = null;
                int row, col;
                for (row = fromRow; row <= toRow; row++) {
                    if (row >= 0 && row < rows) {
                        rowData = source.getRowValues(row);
                        for (col = fromCol; col <= toCol; col++) {
                            wbr.setValue(row - fromRow, col - fromCol, rowData[col]);
                        }
                    }
                }
                
                wbr.findMinAndMaxVals();
                wbr.close();


                // 

            } else {
                // the two extents don't overlap.
            }
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        } catch (Exception e) {
            
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }

        }

    }

    @Override
    public MapLayer.MapLayerType getLayerType() {
        return MapLayer.MapLayerType.RASTER;
    }
    private boolean visible = true;

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean value) {
        visible = value;
    }

    @Override
    public boolean isVisibleInLegend() {
        return visibleInLegend;
    }

    @Override
    public void setVisibleInLegend(boolean value) {
        this.visibleInLegend = value;
    }
}
