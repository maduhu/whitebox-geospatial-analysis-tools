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
public class Hillshade implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "Hillshade";
    }

    @Override
    public String getDescriptiveName() {
    	return "Hillshade";
    }

    @Override
    public String getToolDescription() {
    	return "This tool calculates a hillshade grid from a digital elevation model (DEM).";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "SurfDerivatives" };
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
        
        String inputHeader = null;
        String outputHeader = null;
        
        final double radToDeg = 180 / Math.PI;
        final double degToRad = Math.PI / 180;
        double azimuth = 315 * degToRad;
        double altitude = 45 * degToRad;
        double zFactor = 1;
        double z;
        int progress;
        int[] Dy = {-1, 0, 1, 1, 1, 0, -1, -1};
        int[] Dx = {1, 1, 1, 0, -1, -1, -1, 0};
        double sinTheta;
        double cosTheta;
        double tanSlope;
        int row, col;
        double fx, fy, aspect;
        double gridRes, eightGridRes;
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        double[] N = new double[8];
        double term1, term2, term3;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                azimuth = (Double.parseDouble(args[i]) - 90) * degToRad;
            } else if (i == 3) {
                altitude = Double.parseDouble(args[i]) * degToRad;
            } else if (i == 4) {
                zFactor = Double.parseDouble(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            sinTheta = Math.sin(altitude);
            cosTheta = Math.cos(altitude);

            WhiteboxRaster inputFile = new WhiteboxRaster(inputHeader, "r");
            inputFile.isReflectedAtEdges = true;
            
            int rows = inputFile.getNumberRows();
            int cols = inputFile.getNumberColumns();
            gridRes = inputFile.getCellSizeX();
            eightGridRes = 8 * gridRes;
            double Rad180 = 180 * degToRad;
            double Rad90 = 90 * degToRad;


            double noData = inputFile.getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette("grey.pal");

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = inputFile.getValue(row, col);
                    if (z != noData) {
                        z = z * zFactor;
                        // get the neighbouring cell Z values
                        for (int c = 0; c < 8; c++) {
                            N[c] = inputFile.getValue(row + Dy[c], col + Dx[c]);
                            if (N[c] != noData) {
                                N[c] = N[c] * zFactor;
                            } else {
                                N[c] = z;
                            }
                        }
                        // calculate slope and aspect
                        fy = (N[6] - N[4] + 2 * (N[7] - N[3]) + N[0] - N[2]) / eightGridRes;
                        fx = (N[2] - N[4] + 2 * (N[1] - N[5]) + N[0] - N[6]) / eightGridRes;
                        if (fx != 0) {
                            tanSlope = Math.sqrt(fx * fx + fy * fy);
                            aspect = (180 - Math.atan(fy / fx) * radToDeg + 90 * (fx / Math.abs(fx))) * degToRad;
                            term1 = tanSlope / Math.sqrt(1 + tanSlope * tanSlope);
                            term2 = sinTheta / tanSlope;
                            term3 = cosTheta * Math.sin(azimuth - aspect);
                            z = term1 * (term2 - term3);
                        } else {
                            z = 0.5;
                        }
                        if (z > maxVal) {
                            maxVal = z;
                        }
                        if (z < minVal) {
                            minVal = z;
                        }
                        outputFile.setValue(row, col, z);
                    } else {
                        outputFile.setValue(row, col, noData);
                    }
                }

                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int)(100f * row / (rows - 1));
                updateProgress("Loop 1 of 2", progress);
            }

            outputFile.setMaximumValue(1.0);
            outputFile.setMinimumValue(0.0);
            double range = maxVal - minVal;
            double value;
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = outputFile.getValue(row, col);
                    if (z != noData) {
                        value = (z - minVal) / range;
                        outputFile.setValue(row, col, value);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int)(100f * row / (rows - 1));
                updateProgress("Loop 2 of 2", (int) progress);
            }
            
            outputFile.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());

            inputFile.close();
            outputFile.close();

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
