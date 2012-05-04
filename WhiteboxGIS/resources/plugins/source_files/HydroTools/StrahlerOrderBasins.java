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
public class StrahlerOrderBasins implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "StrahlerOrderBasins";
    }

    @Override
    public String getDescriptiveName() {
    	return "Strahler-Order Basins";
    }

    @Override
    public String getToolDescription() {
    	return "Identifies Strahler-order basins from an input stream network.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "WatershedTools" };
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
        float progress = 0;
        double z;
        int i, c;
        int d, x2, y2;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        boolean flag = false;
        double flowDir = 0;
                
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
            
            WhiteboxRaster pntr = new WhiteboxRaster(pointerHeader, "r");
            
            if (pntr.getNumberRows() != rows || pntr.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    streamsHeader, WhiteboxRaster.DataType.INTEGER, 0);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            
            byte numNeighbouringStreamCells = 0;
            double currentValue = 0;
            double currentOrder = 0;
            double maxStreamOrder = noData;
            updateProgress("Loop 1 of 2:", 0);
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
                            x = col;
                            y = row;
                            currentOrder = 1;
                            output.setValue(y, x, currentOrder);
                            flag = true;
                            do {
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
                                    x += dX[c];
                                    y += dY[c];

                                    if (streams.getValue(y, x) <= 0) { //it's not a stream cell
                                        flag = false;
                                    } else {
                                        currentValue = output.getValue(y, x);
                                        if (currentValue > currentOrder) {
                                            flag = false; //run into a larger stream, end the downstream search
                                            break;
                                        }
                                        if (currentValue == currentOrder) {
                                            numNeighbouringStreamCells = 0;
                                            for (d = 0; d < 8; d++) {
                                                x2 = x + dX[d];
                                                y2 = y + dY[d];
                                                if (streams.getValue(y2, x2) > 0 &&
                                                        pntr.getValue(y2, x2) == inflowingVals[d] &&
                                                        output.getValue(y2, x2) == currentOrder) {
                                                    numNeighbouringStreamCells++;
                                                }
                                            }
                                            if (numNeighbouringStreamCells >= 2) {
                                                currentOrder++;
                                                if (currentOrder > maxStreamOrder) {
                                                    maxStreamOrder = currentOrder;
                                                }
                                            } else {
                                                flag = false;
                                                break;
                                            }
                                        }
                                        if (currentValue < currentOrder) {
                                            output.setValue(y, x, currentOrder);
                                        }
                                    }

                                } else {
                                    if (streams.getValue(y, x) > 0) { //it is a valid stream cell and probably just has no downslope neighbour (e.g. at the edge of the grid)
                                        output.setValue(y, x, output.getValue(y, x) + 1);
                                    }
                                    flag = false;
                                }
                            } while (flag);
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 2:", (int) progress);
            }
            
            updateProgress("Loop 2 of 2:", 0);
            double streamsID = 0;
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (output.getValue(row, col) == noData && pntr.getValue(row, col) != noData) {
                        flag = false;
                        x = col;
                        y = row;
                        do {
                            //find it's downslope neighbour
                            flowDir = pntr.getValue(y, x);
                            if (flowDir > 0) {
                                //move x and y accordingly
                                c = (int)(Math.log(flowDir) / LnOf2);
                                x += dX[c];
                                y += dY[c];
                                //if the new cell already has a value in the output, 
                                //use that as the StreamsID
                                z = output.getValue(y, x);
                                if (z != noData) {
                                    streamsID = z;
                                    flag = true;
                                }
                            } else {
                                streamsID = noData;
                                flag = true;
                            }
                        } while (!flag);

                        flag = false;
                        x = col;
                        y = row;
                        output.setValue(y, x, streamsID);
                        do {
                            //find it's downslope neighbour
                            flowDir = pntr.getValue(y, x);
                            if (flowDir > 0) {
                                c = (int)(Math.log(flowDir) / LnOf2);
                                x += dX[c];
                                y += dY[c];
                                z = output.getValue(y, x);
                                if (z != noData) {
                                    flag = true;
                                }
                            } else {
                                flag = true;
                            }
                            output.setValue(y, x, streamsID);
                        } while (!flag);
                    } else if (pntr.getValue(row, col) == noData) {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Loop 2 of 2:", (int) progress);
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