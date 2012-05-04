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
public class ElevAbovePit implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "ElevAbovePit";
    }

    @Override
    public String getDescriptiveName() {
        return "Elevation Above Pit";
    }

    @Override
    public String getToolDescription() {
        return "Calculate the elevation of each grid cell above the nearest "
                + "downstream pit cell or grid edge cell.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"RelativeLandscapePosition"};
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
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
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

        String inputHeader = null;
        String outputHeader = null;
        String DEMHeader = null;
        int row, col, x, y;
        int progress = 0;
        double z;
        int i, c;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        boolean flag = false;
        double flowDir = 0;
        double pitElev = 0;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                DEMHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster pntr = new WhiteboxRaster(inputHeader, "r");
            int rows = pntr.getNumberRows();
            int cols = pntr.getNumberColumns();
            double noData = pntr.getNoDataValue();

            WhiteboxRaster DEM = new WhiteboxRaster(DEMHeader, "r");
            if (DEM.getNumberRows() != rows || DEM.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw",
                    inputHeader, WhiteboxRaster.DataType.FLOAT, -999);
            output.setPreferredPalette(DEM.getPreferredPalette());
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    flowDir = pntr.getValue(row, col);
                    if (output.getValue(row, col) == -999 && flowDir != noData) {
                        // first travel down the flowpath accumulating the flow length.
                        flag = false;
                        x = col;
                        y = row;
                        do {
                            // find it's downslope neighbour
                            flowDir = pntr.getValue(y, x);
                            if (flowDir > 0) {
                                // what's the flow direction as an int?
                                c = (int) (Math.log(flowDir) / LnOf2);
                                //move x and y accordingly
                                x += dX[c];
                                y += dY[c];
                                if (output.getValue(y, x) != -999) {
                                    // you've hit a cell that already has
                                    // a value assigned to it.
                                    pitElev = DEM.getValue(y, x) - output.getValue(y, x);
                                    flag = true;
                                }
                            } else {  // you've hit the edge or a pit cell.
                                pitElev = DEM.getValue(y, x);
                                flag = true;
                            }
                        } while (!flag);

                        // travel down the flowpath a second time, this time
                        // assigning the flowpath length in reverse to the output.
                        flag = false;
                        x = col;
                        y = row;
                        do {
                            z = DEM.getValue(y, x) - pitElev;
                            output.setValue(y, x, z);
                            // find it's downslope neighbour
                            flowDir = pntr.getValue(y, x);
                            if (flowDir > 0) {
                                c = (int) (Math.log(flowDir) / LnOf2);
                                x += dX[c];
                                y += dY[c];
                                z = output.getValue(y, x);
                                if (z != -999) {
                                    // you've hit a cell that already has
                                    // a flowlength assigned to it. Stop.
                                    flag = true;
                                }
                            } else { // you've hit the edge or a pit cell.
                                output.setValue(y, x, 0);
                                flag = true;
                            }
                        } while (!flag);
                    } else if (flowDir == noData) {
                        output.setValue(row, col, noData);
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
            DEM.close();
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