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
package whiteboxgis;

import java.util.ArrayList;
import java.io.*;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.structures.BoundingBox;
import whitebox.interfaces.MapLayer;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MultispectralLayerInfo implements MapLayer { 
    private WhiteboxRasterInfo source = null;
    private double noDataValue = 0;
    private int overlayNumber = 0;
    private int rows, cols;
    public boolean increasesEastward = false;
    public boolean increasesNorthward = false;
    public boolean isDirty = false;
    private ArrayList<String> headerFiles = new ArrayList<String>();
    
    public MultispectralLayerInfo(String[] headerFiles, String layerTitle, int alpha, int overlayNum) {
        this.layerTitle = layerTitle;
        
        for (int i = 0; i < headerFiles.length; i++) {
            String headerFile = headerFiles[i];
            // check to see that the file exists.
            File file = new File(headerFile);
            if (!file.exists()) {
                System.out.println("File not found.");
            }
            
            this.headerFiles.add(headerFile);
            source = new WhiteboxRasterInfo(headerFile);
        
        }
        
        this.imageWidth = source.getNumberColumns();
        this.imageHeight = source.getNumberRows();
        this.alpha = alpha;
        this.noDataValue = source.getNoDataValue();
        this.overlayNumber = overlayNum;
        this.rows = source.getNumberRows();
        this.cols = source.getNumberColumns();
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
    
    private String redHeaderFile = "";
    public String getRedHeaderFile() {
        return redHeaderFile;
    }
    
    private String greenHeaderFile = "";
    public String getGreenHeaderFile() {
        return greenHeaderFile;
    }
    
    private String blueHeaderFile = "";
    public String getBlueHeaderFile() {
        return blueHeaderFile;
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
        if (value < 0) { value = 0; }
        if (value > 255) { value = 255; }
        alpha = value;
    }
    
    private double gamma = 1.0;
    public double getNonlinearity() {
        return gamma;
    }
    
    public void setNonlinearity(double value) {
        gamma = value;
    }
    
    private int[] pixelData = null;
    public int[] getPixelData() {
        if (pixelData == null || pixelData.length == 0 || isDirty) {
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
    
    public long getDataFileSize() {
        return source.getDataFileSize();
    }
    
    BoundingBox fullExtent = null;
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
    public void setCurrentExtent(BoundingBox db) {
        if (db.getMaxY() != currentExtent.getMaxY() || 
                db.getMaxX() != currentExtent.getMaxX() ||
                db.getMinY() != currentExtent.getMinY() ||
                db.getMinX() != currentExtent.getMinX()) {
            currentExtent = db.clone();
            isDirty = true;
        }
    }
    
    private int resolutionFactor = 1;
    public void setResolutionFactor(int value) {
        if (value < 1) { value = 1; }
        if (value != resolutionFactor) {
            resolutionFactor = value;
            isDirty = true;
        }
    }
    
    /*
    public double getDataValue(int row, int col) {
        int r = (int)(((double)row - startRow) / (endRow - startRow) * (imageHeight - 1));
        int c = (int)(((double)col - startCol) / (endCol - startCol) * (imageWidth - 1));
        int cellNum = (int)(r * imageWidth + c);
        if (cellNum > data.length) { return Double.NaN; }
        return data[cellNum];
    }*/
    
    int startRow;
    int endRow;
    int startCol;
    int endCol;
    public void createPixelData() {
        try {
            startRow = (int)(Math.abs(fullExtent.getMaxY() - currentExtent.getMaxY()) / source.getCellSizeY());
            endRow = (int)(rows - (Math.abs(fullExtent.getMinY() - currentExtent.getMinY()) / source.getCellSizeY())) - 1;
            startCol = (int)(Math.abs(fullExtent.getMinX() - currentExtent.getMinX()) / source.getCellSizeX());
            endCol = (int)(cols - (Math.abs(fullExtent.getMaxX() - currentExtent.getMaxX()) / source.getCellSizeX())) - 1;
            int row, col;
            
            double redVal, greenVal, blueVal;
            int r, g, b;
            int entryNum = 0;
            
            // check the numCells
            imageHeight = 0;
            imageWidth = 0;
            for (row = startRow; row < endRow; row += resolutionFactor) {
                imageHeight++;
            }
            for (col = startCol; col < endCol; col += resolutionFactor) {
                imageWidth++;
            }
            int numCells = imageHeight * imageWidth;
            
            WhiteboxRaster redSourceData = new WhiteboxRaster(redHeaderFile, "r");
            WhiteboxRaster greenSourceData = new WhiteboxRaster(greenHeaderFile, "r");
            WhiteboxRaster blueSourceData = new WhiteboxRaster(blueHeaderFile, "r");
            
            double redRange, greenRange, blueRange;
            double redMin, greenMin, blueMin;
            redMin = redSourceData.getDisplayMinimum();
            greenMin = greenSourceData.getDisplayMinimum();
            blueMin = blueSourceData.getDisplayMinimum();
            redRange = redSourceData.getDisplayMaximum() - redSourceData.getDisplayMinimum();
            greenRange = greenSourceData.getDisplayMaximum() - greenSourceData.getDisplayMinimum();
            blueRange = blueSourceData.getDisplayMaximum() - blueSourceData.getDisplayMinimum();
            
            int backgroundColour = 0; // transparent black
            pixelData = new int[numCells];
            //redData = new double[numCells];
            //greenData = new double[numCells];
            //blueData = new double[numCells];
            
            double[] redRawData;
            double[] greenRawData;
            double[] blueRawData;
            int i = 0;
            for (row = startRow; row < endRow; row += resolutionFactor) {
                redRawData = redSourceData.getRowValues(row);
                greenRawData = greenSourceData.getRowValues(row);
                blueRawData = blueSourceData.getRowValues(row);
                for (col = startCol; col < endCol; col += resolutionFactor) {
                    redVal = redRawData[col];
                    greenVal = greenRawData[col];
                    blueVal = blueRawData[col];
                    if ((redVal != noDataValue) && (greenVal != noDataValue) && (blueVal != noDataValue)) {
                        r = (int)((redVal - redMin) / redRange * 255);
                        g = (int)((greenVal - greenMin) / greenRange * 255);
                        b = (int)((blueVal - blueMin) / blueRange * 255);
                        
                        pixelData[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
                    } else {
                        pixelData[i] = backgroundColour;
                    }
                    //data[i] = value;
                    i++;
                }
            }

            //sourceData.close();
            //sourceData = null;
            isDirty = false;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private void getRedImage() {
        
    }
   
    public void update() {
        createPixelData();
    }

    @Override
    public MapLayerType getLayerType() {
        return MapLayerType.MULTISPECTRAL;
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
}
