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
public class CostPathway implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "CostPathway";
    }

    @Override
    public String getDescriptiveName() {
    	return "Cost Pathway";
    }

    @Override
    public String getToolDescription() {
    	return "Performs cost-distance pathway analysis using a series of "
                + "destination grid cells.";
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
                
        String destHeader = null;
        String outputHeader = null;
        String backLinkHeader = null;
        WhiteboxRaster destImage;
        WhiteboxRaster output;
        WhiteboxRaster backLink;
        int cols, rows;
        double z, flowDir;
        float progress = 0;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double lnOf2 = 0.693147180559945;
        double gridRes;
        int col, row, a;
        int c;
        int x, y, i;
        boolean flag = false;
        
    
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                destHeader = args[i];
            } else if (i == 1) {
                backLinkHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((destHeader == null) || (backLinkHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        try {
            destImage = new WhiteboxRaster(destHeader, "r");
            rows = destImage.getNumberRows();
            cols = destImage.getNumberColumns();
            double noData = destImage.getNoDataValue();
            gridRes = (destImage.getCellSizeX() + destImage.getCellSizeY()) / 2;

            backLink = new WhiteboxRaster(backLinkHeader, "r");
            if (backLink.getNumberColumns() != cols || 
                    backLink.getNumberRows() != rows) {
                showFeedback("Input images must have the same dimensions");
                return;
            }

            output = new WhiteboxRaster(outputHeader, "rw", backLinkHeader, 
                    WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("qual.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);
            
            double[] data;
            for (row = 0; row < rows; row++) {
                data = destImage.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] > 0) {
                        x = col;
                        y = row;
                        flag = true;
                        do {
                            z = output.getValue(y, x);
                            if (z == noData) {
                                output.setValue(y, x, 1);
                            } else {
                                output.setValue(y, x, z + 1);
                            }
                            flowDir = backLink.getValue(y, x);
                            if (flowDir > 0) {
                                //move x and y accordingly
                                c = (int)(Math.log(flowDir) / lnOf2);
                                x += dX[c];
                                y += dY[c];
                            } else {
                                flag = false; //stop the flowpath traverse
                            }
                        } while (flag);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float)(100f * row / (rows - 1));
                updateProgress((int) progress);
            }
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            destImage.close();
            backLink.close();
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