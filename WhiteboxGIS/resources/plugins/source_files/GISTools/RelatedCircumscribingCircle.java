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
public class RelatedCircumscribingCircle implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    public String getName() {
        return "RelatedCircumscribingCircle";
    }

    public String getDescriptiveName() {
    	return "Related Circumscribing Circle";
    }

    public String getToolDescription() {
    	return "Measures the ratio between "
                + "the area of a patch and the smallest circumscribing circle.";
    }

    public String[] getToolbox() {
    	String[] ret = { "PatchShapeTools" };
    	return ret;
    }

    
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
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
        int col;
        int row;
        int numCols;
        int numRows;
        int a, i;
        float progress;
        int minValue, maxValue, range;
        boolean blnTextOutput = false;
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
            } else if (i == 2) {
                blnTextOutput = Boolean.parseBoolean(args[i]);
            } else if (i == 3) {
                zeroAsBackground = Boolean.parseBoolean(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");

            numRows = image.getNumberRows();
            numCols = image.getNumberColumns();
            double noData = image.getNoDataValue();
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);

            minValue = (int)(image.getMinimumValue());
            maxValue = (int)(image.getMaximumValue());
            range = maxValue - minValue;

            double[] data;
            // find the axis-aligned minimum bounding box.
            updateProgress("Loop 1 of 2:", 0);
            double[][] boundingBox = new double[6][range + 1];
            for (a = 0; a <= range; a++) {
                boundingBox[0][a] = Integer.MAX_VALUE; // west
                boundingBox[1][a] = Integer.MIN_VALUE; // east
                boundingBox[2][a] = Integer.MAX_VALUE; // north
                boundingBox[3][a] = Integer.MIN_VALUE; // south
            }
      
            for (row = 0; row < numRows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < numCols; col++) {
                    if (data[col] != noData) {
                        a = (int) (data[col] - minValue);
                        if (col < boundingBox[0][a]) {
                            boundingBox[0][a] = col;
                        }
                        if (col > boundingBox[1][a]) {
                            boundingBox[1][a] = col;
                        }
                        if (row < boundingBox[2][a]) {
                            boundingBox[2][a] = row;
                        }
                        if (row > boundingBox[3][a]) {
                            boundingBox[3][a] = row;
                        }
                        boundingBox[5][a]++;

                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (numRows - 1));
                updateProgress("Loop 1 of 2:", (int) progress);
            }

            updateProgress("Loop 2 of 2:", 0);
            double radius;
            for (a = 0; a <= range; a++) {
                if ((boundingBox[1][a] - boundingBox[0][a] + 1)
                        > (boundingBox[3][a] - boundingBox[2][a] + 1)) {
                    radius = (boundingBox[1][a] - boundingBox[0][a] + 1) / 2;
                } else {
                    radius = (boundingBox[3][a] - boundingBox[2][a] + 1) / 2;
                }
                boundingBox[4][a] = Math.PI * radius * radius;
            }

            if (zeroAsBackground) {
                boundingBox[0 - minValue][4] = 0d;
                // sum the column numbers and row numbers of each patch cell 
                // along with the total number of cells.
                for (row = 0; row < numRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < numCols; col++) {
                        if (data[col] > 0) {
                            a = (int) (data[col] - minValue);
                            output.setValue(row, col, boundingBox[5][a] / boundingBox[4][a]);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (numRows - 1));
                    updateProgress("Loop 2 of 2:", (int) progress);
                }
            } else {

                // sum the column numbers and row numbers of each patch cell 
                // along with the total number of cells.
                for (row = 0; row < numRows; row++) {
                    data = image.getRowValues(row);
                    for (col = 0; col < numCols; col++) {
                        if (data[col] != noData) {
                            a = (int) (data[col] - minValue);
                            output.setValue(row, col, boundingBox[5][a] / boundingBox[4][a]);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (numRows - 1));
                    updateProgress("Loop 2 of 2:", (int) progress);
                }
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            image.close();
            output.close();

            if (blnTextOutput) {
                DecimalFormat df;
                df = new DecimalFormat("0.0000");

                String retstr = "Related Circumscribing Circle\nPatch ID\tValue";

                for (a = 0; a <= range; a++) {
                    if (boundingBox[4][a] > 0) {
                        retstr = retstr + "\n" + (a + minValue) + "\t" + 
                                df.format(boundingBox[5][a] / boundingBox[4][a]);
                    }
                }

                returnData(retstr);
            }

            // returning a header file string displays the image.
            returnData(outputHeader);

            
        } catch (Exception e) {
            showFeedback(e.getMessage());
            showFeedback(e.getCause().toString());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}
