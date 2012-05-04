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
public class RemoveShortStreams implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "RemoveShortStreams";
    }

    @Override
    public String getDescriptiveName() {
    	return "Remove Short Streams";
    }

    @Override
    public String getToolDescription() {
    	return "Removes short first-order streams from a stream network.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "StreamAnalysis" };
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
        
        String streamsHeader = null;
        String pointerHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        int progress = 0;
        int i, c;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        boolean flag = false;
        double flowDir = 0;
        double minStreamLength = 0;
        boolean blnRemoveStream = false;
        int stopRow, stopCol;

                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                streamsHeader = args[i];
            } else if (i == 1) {
                pointerHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            } else if (i == 3) {
                minStreamLength = Double.parseDouble(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((streamsHeader == null) || (pointerHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster streams = new WhiteboxRaster(streamsHeader, "r");
            int rows = streams.getNumberRows();
            int cols = streams.getNumberColumns();
            double noData = streams.getNoDataValue();
            double gridResX = streams.getCellSizeX();
            double gridResY = streams.getCellSizeY();
            double diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            double[] gridLengths = new double[]{diagGridRes, gridResX, diagGridRes, gridResY, diagGridRes, gridResX, diagGridRes, gridResY};
            
            double maxLinkID = streams.getMaximumValue();
            
            //convert the MinStreamLength to grid cell units
            minStreamLength = minStreamLength / ((gridResX + gridResY) / 2);

            WhiteboxRaster pntr = new WhiteboxRaster(pointerHeader, "r");
            if (pntr.getNumberRows() != rows || pntr.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    streamsHeader, WhiteboxRaster.DataType.FLOAT, 0);
            output.setPreferredPalette("qual.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);
            
            // fill the output image with the streams
            updateProgress("Loop 1 of 2:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    output.setValue(row, col, streams.getValue(row, col));
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 2:", progress);
            }
            
            byte numNeighbouringStreamCells = 0;
            double linkLength = 0;
            int x2, y2;
            updateProgress("Loop 2 of 2:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (streams.getValue(row, col) > 0) {
                        // see if it is a headwater location
                        numNeighbouringStreamCells = 0;
                        for (c = 0; c < 8; c++) {
                            x = col + dX[c];
                            y = row + dY[c];
                            if (streams.getValue(y, x) > 0 && 
                                    pntr.getValue(y, x) == inflowingVals[c]) { 
                                numNeighbouringStreamCells++; 
                            }
                        }
                        if (numNeighbouringStreamCells == 0) {
                            //it's a headwater location so start a downstream flowpath
                            linkLength = 0;
                            blnRemoveStream = false;
                            x = col;
                            y = row;
                            stopRow = -1;
                            stopCol = -1;
                            flag = true;
                            do {
                                //see if it's the end of the link, i.e. a confluence
                                numNeighbouringStreamCells = 0;
                                for (c = 0; c < 8; c++) {
                                    x2 = x + dX[c];
                                    y2 = y + dY[c];
                                    if (streams.getValue(y2, x2) > 0
                                            && pntr.getValue(y2, x2) == inflowingVals[c]) {
                                        numNeighbouringStreamCells++;
                                    }
                                }
                                if (numNeighbouringStreamCells > 1) { //it's a confluence
                                    if (linkLength < minStreamLength) {
                                        stopRow = y;
                                        stopCol = x;
                                        blnRemoveStream = true;
                                    }
                                    flag = false;
                                    break;
                                }
                                
                                //find the downslope neighbour
                                flowDir = pntr.getValue(y, x);
                                if (flowDir > 0) {
                                    c = (int)(Math.log(flowDir) / LnOf2);
                                    if (c > 7) {
                                        showFeedback("An unexpected value has "
                                                + "been identified in the pointer "
                                                + "image. This tool requires a "
                                                + "pointer grid that has been "
                                                + "created using either the D8 "
                                                + "or Rho8 tools.");
                                        return;
                                    }
                                    linkLength += gridLengths[c];
                                    
                                    if (linkLength > minStreamLength) {
                                        flag = false;
                                    } else {
                                        x += dX[c];
                                        y += dY[c];
                                    
                                        if (streams.getValue(y, x) <= 0) { //it's not a stream cell
                                            flag = false;
                                        }
                                    }

                                } else {
                                    flag = false;
                                }
                            } while (flag);
                            
                            
                            if (blnRemoveStream) { //remove the stream
                                x = col;
                                y = row;
                                flag = true;
                                do {
                                    if (x == stopCol && y == stopRow) {
                                        flag = false;
                                        break;
                                    }
                                    output.setValue(y, x, noData);
                                    //find the downslope neighbour
                                    flowDir = pntr.getValue(y, x);
                                    if (flowDir > 0) {
                                        c = (int)(Math.log(flowDir) / LnOf2);
                                        x += dX[c];
                                        y += dY[c];
                                        if (streams.getValue(y, x) <= 0) { //it's not a stream cell
                                            flag = false;
                                        }
                                    } else {
                                        flag = false;
                                    }
                                } while (flag);
                            }
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 2:", progress);
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            pntr.close();
            streams.close();
            output.close();

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