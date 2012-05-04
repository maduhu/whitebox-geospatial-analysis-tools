/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package plugins;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class CostAccumulation implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "CostAccumulation";
    }

    @Override
    public String getDescriptiveName() {
    	return "Cost Accumulation";
    }

    @Override
    public String getToolDescription() {
    	return "Performs cost-distance accumulation on a cost surface and a "
                + "group of source cells.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "CostTools" };
    	return ret;
    }

    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private int previousProgress = 0;
    private String previousProgressLabel = "";
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }
    
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;
        
        String sourceHeader = null;
        String costHeader = null;
        String outputHeader = null;
        String backLinkHeader = null;
        WhiteboxRaster costSurface;
        WhiteboxRaster sourceImage;
        WhiteboxRaster output;
        WhiteboxRaster backLink;
        int cols, rows;
        double z, costVal, srcVal;
        float progress = 0;
        double largeVal = Float.MAX_VALUE - 10000000;
        int[] dX = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        int[] dY = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
        double diagDist = Math.sqrt(2);
        double[] dist = new double[]{1, diagDist, 1, diagDist, 1, diagDist, 1, diagDist};
        double gridRes;
        int col, row, a;
        int c;
        int x, y, i;
        int[] backLinkDir = new int[]{32, 64, 128, 1, 2, 4, 8, 16};
        double costAccumVal, cost1, cost2, newCostVal;
        boolean didSomething = false;
        int loopNum = 0;
        boolean blnAnisotropicForce = false;
        double anisotropicForceDirection = -999;
        double anisotropicForceStrength = -999;
        double[] azDir = new double[]{90, 135, 180, 225, 270, 315, 0, 45};

    
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                sourceHeader = args[i];
            } else if (i == 1) {
                costHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            } else if (i == 3) {
                backLinkHeader = args[i];
            } else if (i == 4) {
                blnAnisotropicForce = false;
                if (!args[i].toLowerCase().equals("not specified")) {
                    blnAnisotropicForce = true;
                    anisotropicForceDirection = Double.parseDouble(args[i]);
                    if (anisotropicForceDirection >= 360) {
                        anisotropicForceDirection = 0;
                    }
                    if (anisotropicForceDirection < 0) {
                        anisotropicForceDirection = 0;
                    }
                }
            } else if (i == 5) {
                blnAnisotropicForce = false;
                if (!args[i].toLowerCase().equals("not specified")) {
                    anisotropicForceStrength = Double.parseDouble(args[i]);
                    if (anisotropicForceStrength == 1 || anisotropicForceStrength == 0) {
                        blnAnisotropicForce = false;
                    } else {
                        blnAnisotropicForce = true;
                        if (anisotropicForceStrength > 100) {
                            anisotropicForceStrength = 100;
                        }
                        if (anisotropicForceStrength < -100) {
                            anisotropicForceStrength = -100;
                        }
                    }
                }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((sourceHeader == null) || (costHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        if (anisotropicForceDirection == -999 || anisotropicForceStrength == -999) {
            if (blnAnisotropicForce) {
                showFeedback("Both the Anisotropic Force Direction and "
                        + "Anisotropic Force Strength must be set to valid "
                        + "values to carry out this operation.");
                return;
            }
        }

        try {
            sourceImage = new WhiteboxRaster(sourceHeader, "r");
            rows = sourceImage.getNumberRows();
            cols = sourceImage.getNumberColumns();
            double noData = sourceImage.getNoDataValue();
            gridRes = (sourceImage.getCellSizeX() + sourceImage.getCellSizeY()) / 2;

            costSurface = new WhiteboxRaster(costHeader, "r");
            if (costSurface.getNumberColumns() != cols || 
                    costSurface.getNumberRows() != rows) {
                showFeedback("Input images must have the same dimensions");
                return;
            }

            output = new WhiteboxRaster(outputHeader, "rw", sourceHeader, WhiteboxRaster.DataType.FLOAT, largeVal);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
           
            backLink = new WhiteboxRaster(outputHeader, "rw", backLinkHeader, WhiteboxRaster.DataType.INTEGER, noData);
            backLink.setPreferredPalette("spectrum.pal");
            backLink.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
           
            updateProgress("Calculating Cost Accumulation Surface:", 0);
            double[] data;
            for (row = 0; row < rows; row++) {
                data = costSurface.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] !=noData) { 
                        srcVal = sourceImage.getValue(row, col);
                        if (srcVal > 0) {
                            output.setValue(row, col, 0);
                            backLink.setValue(row, col, 0);
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float)(100f * row / (rows - 1));
                updateProgress((int) progress);
            }

            if (!blnAnisotropicForce) {
                do {
                    didSomething = false;
                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (row = 0; row < rows; row++) {
                        for (col = 0; col < cols; col++) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 0; c <= 3; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + (cost1 + cost2) / 2 * dist[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress((int) progress);
                    }
                    
                    if (!didSomething) { break; }
                    
                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (row = rows - 1; row >= 0; row--) {
                        for (col = cols - 1; col >= 0; col--) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 4; c <= 7; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + (cost1 + cost2) / 2 * dist[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * (rows - 1 - row) / (rows - 1));
                        updateProgress((int) progress);
                    }
                    if (!didSomething) { break; }
                    
                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (col = cols - 1; col >= 0; col--) {
                        for (row = rows - 1; row >= 0; row--) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 3; c <= 6; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + (cost1 + cost2) / 2 * dist[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * (cols - 1 - col) / (cols - 1));
                        updateProgress((int) progress);
                    }
                    if (!didSomething) { break; }
                    
                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (row = 0; row < rows - 1; row++) {
                        for (col = cols - 1; col >= 0; col--) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 1; c <= 4; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + (cost1 + cost2) / 2 * dist[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress((int) progress);
                    }
                    if (!didSomething) { break; }
                    
                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (col = cols - 1; col >= 0; col--) {
                        for (row = 0; row < rows - 1; row++) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 2; c <= 5; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + (cost1 + cost2) / 2 * dist[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * (cols - 1 - col) / (cols - 1));
                        updateProgress((int) progress);
                    }

                } while (didSomething);
            } else {
                double dir = 0;
                //convert the azdir to force multipliers
                for (c = 0; c <= 7; c++) {
                    dir = Math.abs(azDir[c] - anisotropicForceDirection);
                    if (dir > 180) { dir = 360 - dir; }
                    azDir[c] = 1 + (180 - dir) / 180 * (anisotropicForceStrength - 1);
                }
                
                do {
                    didSomething = false;
                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (row = 0; row < rows; row++) {
                        for (col = 0; col < cols; col++) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 0; c <= 3; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + ((cost1 + cost2) / 2 * dist[c]) / azDir[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress((int) progress);
                    }
                    
                    if (!didSomething) { break; }
                    didSomething = false;

                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (row = rows - 1; row >= 0; row--) {
                        for (col = cols - 1; col >= 0; col--) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 4; c <= 7; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + ((cost1 + cost2) / 2 * dist[c]) / azDir[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * (rows - 1 - row) / (rows - 1));
                        updateProgress((int) progress);
                    }
                    if (!didSomething) { break; }
                    didSomething = false;

                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (col = cols - 1; col >= 0; col--) {
                        for (row = rows - 1; row >= 0; row--) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 3; c <= 6; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + ((cost1 + cost2) / 2 * dist[c]) / azDir[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * (cols - 1 - col) / (cols - 1));
                        updateProgress((int) progress);
                    }
                    if (!didSomething) { break; }
                    didSomething = false;

                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (row = 0; row < rows - 1; row++) {
                        for (col = cols - 1; col >= 0; col--) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 1; c <= 4; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + ((cost1 + cost2) / 2 * dist[c]) / azDir[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress((int) progress);
                    }
                    if (!didSomething) { break; }
                    didSomething = false;

                    loopNum++;
                    updateProgress("Loop Number " + loopNum + ":", 0);
                    for (col = cols - 1; col >= 0; col--) {
                        for (row = 0; row < rows - 1; row++) {
                            costAccumVal = output.getValue(row, col);
                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                cost1 = costSurface.getValue(row, col);
                                for (c = 2; c <= 5; c++) {
                                    x = col + dX[c];
                                    y = row + dY[c];
                                    cost2 = costSurface.getValue(y, x);
                                    newCostVal = costAccumVal + ((cost1 + cost2) / 2 * dist[c]) / azDir[c];
                                    if (newCostVal < output.getValue(y, x)) {
                                        output.setValue(y, x, newCostVal);
                                        backLink.setValue(y, x, backLinkDir[c]);
                                        didSomething = true;
                                    }
                                }
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * (cols - 1 - col) / (cols - 1));
                        updateProgress((int) progress);
                    }
                    
                } while (didSomething);
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            backLink.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            backLink.addMetadataEntry("Created on " + new Date());
            
            sourceImage.close();
            costSurface.close();
            output.close();
            backLink.close();
            
            // returning a header file string displays the image.
            returnData(outputHeader);

        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}