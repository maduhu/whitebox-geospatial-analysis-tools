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
public class ExposureTowardsWindFlux implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ExposureTowardsWindFlux";
    }

    @Override
    public String getDescriptiveName() {
        return "Exposure Towards A Wind Flux";
    }

    @Override
    public String getToolDescription() {
        return "Calculates the exposure towards a wind flux.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"WindRelatedTAs"};
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

    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    private boolean amIActive = false;

    public boolean isActive() {
        return amIActive;
    }

    public void run() {
        amIActive = true;

        String slopeHeader = null;
        String aspectHeader = null;
        String outputHeader = null;
        String horizonAngleHeader = null;
        double z;
        int progress;
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int row, col;
        double azimuth = 0;
        boolean blnSlope = false;
        double relativeAspect = 0;
        double slopeVal = 0;
        double aspectVal = 0;
        double HAval = 0;
        double gridRes = 0;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                slopeHeader = args[i];
            } else if (i == 1) {
                aspectHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            } else if (i == 2) {
                azimuth = Math.toRadians(Double.parseDouble(args[i]) - 90);
            } else if (i == 3) {
                if (args[i].toLowerCase().contains("slope")) {
                    blnSlope = true;
                } else {
                    blnSlope = false;
                }
            } else if (i == 4) {
                if (blnSlope) {
                    if (args[i].toLowerCase().contains("not specified")) {
                        showFeedback("The horizon angle raster must be specified");
                        break;
                    }
                    horizonAngleHeader = args[i];
                }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((slopeHeader == null) || aspectHeader == null || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster slope = new WhiteboxRaster(slopeHeader, "r");
            int rows = slope.getNumberRows();
            int cols = slope.getNumberColumns();
            gridRes = (slope.getCellSizeX() + slope.getCellSizeY()) / 2;
            double slopeNoData = slope.getNoDataValue();

            WhiteboxRaster aspect = new WhiteboxRaster(aspectHeader, "r");
            if (aspect.getNumberRows() != rows || aspect.getNumberColumns() != cols) {
                showFeedback("the input images must have the same dimensions (i.e. rows and columns).");
                return;
            }
            double aspectNoData = aspect.getNoDataValue();

            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", slopeHeader, 
                    WhiteboxRaster.DataType.FLOAT, slopeNoData);
            output.setPreferredPalette("grey.pal");

            double[] slopeData;
            double[] aspectData;

            if (blnSlope) {
                WhiteboxRaster horizonAngle = new WhiteboxRaster(horizonAngleHeader, "r");
                if (horizonAngle.getNumberRows() != rows || horizonAngle.getNumberColumns() != cols) {
                    showFeedback("the input images must have the same dimensions (i.e. rows and columns).");
                    return;
                }
                double HANoData = horizonAngle.getNoDataValue();
                double[] HAdata;
                for (row = 0; row < rows; row++) {
                    slopeData = slope.getRowValues(row);
                    aspectData = aspect.getRowValues(row);
                    HAdata = horizonAngle.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        relativeAspect = azimuth - aspectData[col];
                        if (relativeAspect > 180) {
                            relativeAspect = 360 - relativeAspect;
                            if (slopeData[col] != slopeNoData && aspectData[col] != aspectNoData
                                    && HAdata[col] != HANoData) {
                                slopeVal = Math.toRadians(slopeData[col]);
                                aspectVal = Math.toRadians(aspectData[col]);
                                HAval = Math.toRadians(HAdata[col]);
                                relativeAspect = Math.toRadians(relativeAspect);
                                output.setValue(row, col, Math.cos(slopeVal)
                                        * Math.sin(HAval) + Math.sin(slopeVal)
                                        * Math.cos(HAval) * Math.cos(relativeAspect));
                            } else {
                                output.setValue(row, col, slopeNoData);
                            }
                        }
                    }

                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (rows - 1));
                    updateProgress(progress);
                }
                horizonAngle.close();
            } else {
                HAval = 0;
                for (row = 0; row < rows; row++) {
                    slopeData = slope.getRowValues(row);
                    aspectData = aspect.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        relativeAspect = azimuth - aspectData[col];
                        if (relativeAspect > 180) {
                            relativeAspect = 360 - relativeAspect;
                        }
                        if (slopeData[col] != slopeNoData && aspectData[col] != aspectNoData) {
                            slopeVal = Math.toRadians(slopeData[col]);
                            aspectVal = Math.toRadians(aspectData[col]);
                            relativeAspect = Math.toRadians(relativeAspect);
                            output.setValue(row, col, Math.cos(slopeVal) * 
                                    Math.sin(HAval) + Math.sin(slopeVal) * 
                                    Math.cos(HAval) * Math.cos(relativeAspect));
                        } else {
                            output.setValue(row, col, slopeNoData);
                        }
                    }

                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (int) (100f * row / (rows - 1));
                    updateProgress(progress);
                }
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            slope.close();
            aspect.close();
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
