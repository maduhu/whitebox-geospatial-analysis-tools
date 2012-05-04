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
public class FindMainStem implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "FindMainStem";
    }

    @Override
    public String getDescriptiveName() {
    	return "Find Main Stem";
    }

    @Override
    public String getToolDescription() {
    	return "Finds the main stem of each stream network draining to an outlet.";
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
        String accumHeader = null;
        String outputHeader = null;
        int row, col, x, y, x2, y2, nx, ny;
        int progress = 0;
        int i, c;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        boolean flag = false;
        double flowDir = 0;
        double nFlowDir = 0;
        boolean isOutlet = false;
        double maxFlowAccum = 0;
                
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
                accumHeader = args[i];
            } else if (i == 3) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((streamsHeader == null) || (pointerHeader == null) || 
                 (accumHeader == null) ||(outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster streams = new WhiteboxRaster(streamsHeader, "r");
            int rows = streams.getNumberRows();
            int cols = streams.getNumberColumns();
            double streamsNoData = streams.getNoDataValue();
            
            WhiteboxRaster pntr = new WhiteboxRaster(pointerHeader, "r");
            if (pntr.getNumberRows() != rows || pntr.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            //double pntrNoData = pntr.getNoDataValue();
            
            
            WhiteboxRaster accum = new WhiteboxRaster(accumHeader, "r");
            if (accum.getNumberRows() != rows || accum.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            //double accumNoData = accum.getNoDataValue();
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    streamsHeader, WhiteboxRaster.DataType.INTEGER, 0);
            output.setPreferredPalette("qual.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);
            
            
            updateProgress("Finding network links:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (streams.getValue(row, col) != 0 &&
                            streams.getValue(row, col) != streamsNoData) {
                        isOutlet = false;
                        flowDir = pntr.getValue(row, col);
                        if (flowDir == 0) { 
                            isOutlet = true; 
                        } else {
                            c = (int)(Math.log(flowDir) / LnOf2);
                            if (streams.getValue(row + dY[c], col + dX[c]) == 0 
                                    || streams.getValue(row + dY[c], col + dX[c]) == streamsNoData) {
                                isOutlet = true;
                            }
                        }
                        
                        if (isOutlet) {
                            x = col;
                            y = row;
                            flag = true;
                            do {
                                output.setValue(y, x, 1.0);
                                
                                // find the upslope neighbouring stream cell
                                // with the highest flow accumulation value
                                maxFlowAccum = 0;
                                nx = 0;
                                ny = 0;
                                for (c = 0; c < 8; c++) {
                                    x2 = x + dX[c];
                                    y2 = y + dY[c];
                                    nFlowDir = pntr.getValue(y2, x2);
                                    if (streams.getValue(y2, x2) > 0 
                                            && nFlowDir == inflowingVals[c]) {
                                        if (accum.getValue(y2, x2) > maxFlowAccum) {
                                            nx = x2;
                                            ny = y2;
                                            maxFlowAccum = accum.getValue(y2, x2);
                                        }
                                        
                                    }
                                }
                                
                                if (maxFlowAccum > 0) {
                                    x = nx;
                                    y = ny;
                                } else {
                                    flag = false;
                                }
                            } while (flag);
                        }
                    } else if (streams.getValue(row, col) == streamsNoData) {
                        output.setValue(row, col, streamsNoData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress(progress);
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