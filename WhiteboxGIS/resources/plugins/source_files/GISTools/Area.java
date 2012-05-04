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
import java.text.DecimalFormat;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Area implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    public String getName() {
        return "Area";
    }

    public String getDescriptiveName() {
    	return "Area";
    }

    public String getToolDescription() {
    	return "Calculates the area of polygons or classes within a raster image.";
    }

    public String[] getToolbox() {
    	String[] ret = { "GISTools" };
    	return ret;
    }
    
    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }
    
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }

    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.print("Progress: " + progress + "%");
        }
    }
    
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
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
        int row, col;
        float progress = 0;
        double z;
        int x, y;
        int i;
        double noData; 
        int numClasses;
        int minClass, maxClass;
        double[] classArea;
        boolean blnImageOutput = false;
        boolean blnTextOutput = false;
        boolean blnOutputUnitsGridCells = false;
        double gridRes;
        double gridArea;
        boolean zeroAsBackground = false;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
                if (!outputHeader.toLowerCase().contains("not specified")) {
                    blnImageOutput = true;
                }
            } else if (i == 2) {
                blnTextOutput = Boolean.parseBoolean(args[i]);
            } else if (i == 3) {
                if (args[i].toLowerCase().contains("cells")) {
                    blnOutputUnitsGridCells = true;
                } else {
                     blnOutputUnitsGridCells = false;
                }
            } else if (i == 4) {
                zeroAsBackground = Boolean.parseBoolean(args[i]);
            }
        }

        // check to see that the inputHeader are not null.
        if ((inputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        if (!blnImageOutput && !blnTextOutput) {
            showFeedback("You must select either an image or text output or both.");
            return;
        }


        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");
            int rows = image.getNumberRows();
            int cols = image.getNumberColumns();
            noData = image.getNoDataValue();
            gridRes = image.getCellSizeX();
            gridArea = gridRes * gridRes;

            minClass = (int) image.getMinimumValue();
            maxClass = (int) image.getMaximumValue();
            numClasses = maxClass - minClass + 1;

            classArea = new double[numClasses];

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = image.getValue(row, col);
                    if (z != noData) {
                        i = (int) z - minClass;
                        classArea[i]++;
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }

            if (!blnOutputUnitsGridCells) { //convert the areas to map units
                for (i = 0; i < numClasses; i++) {
                    classArea[i] = classArea[i] * gridArea;
                }
            }

            if (blnImageOutput) {
                WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, -1);
                if (!zeroAsBackground) {
                    output.setPreferredPalette("spectrum.pal");

                    for (row = 0; row < rows; row++) {
                        for (col = 0; col < cols; col++) {
                            z = image.getValue(row, col);
                            if (z != noData) {
                                i = (int) z - minClass;
                                output.setValue(row, col, classArea[i]);
                            } else {
                                output.setValue(row, col, noData);
                            }
                        }
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (float) (100f * row / (rows - 1));
                        updateProgress((int) progress);
                    }
                } else {
                    output.setPreferredPalette("spectrum_black_background.pal");

                    for (row = 0; row < rows; row++) {
                        for (col = 0; col < cols; col++) {
                            z = image.getValue(row, col);
                            if (z != noData) {
                                if (z != 0) {
                                    i = (int) z - minClass;
                                    output.setValue(row, col, classArea[i]);
                                } else {
                                    output.setValue(row, col, 0);
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
                        updateProgress((int) progress);
                    }
                }

                output.addMetadataEntry("Created by the "
                        + getDescriptiveName() + " tool.");
                output.addMetadataEntry("Created on " + new Date());
                output.close();
                
                // returning a header file string displays the image.
                returnData(outputHeader);
            }

            if (blnTextOutput) {
                if (zeroAsBackground) { classArea[0 - minClass] = 0; }
                StringBuilder sb = new StringBuilder();
                sb.append("Area Analysis\n");
                DecimalFormat df;
                if (!blnOutputUnitsGridCells) {
                    df = new DecimalFormat("###,###,###.000");
                } else {
                    df = new DecimalFormat("###,###,###");
                }
                for (i = 0; i < numClasses; i++) {
                    if (classArea[i] > 0) {
                        sb.append(minClass + i);
                        sb.append("\t");
                        sb.append(df.format(classArea[i]));
                        sb.append("\n");
                    }
                }

                returnData(sb.toString());
            }

            image.close();

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
