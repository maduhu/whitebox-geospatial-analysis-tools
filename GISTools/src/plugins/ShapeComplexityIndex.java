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
public class ShapeComplexityIndex implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    public String getName() {
        return "ShapeComplexityIndex";
    }

    public String getDescriptiveName() {
    	return "Shape Complexity Index";
    }

    public String getToolDescription() {
    	return "Assigns each patch in a raster image a simple index value based "
                + "on the patch's shape complexity.";
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
        int z;
        int previousZ;

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

            //minValue = (int)(image.getMinimumValue());
            maxValue = (int)(image.getMaximumValue());
            range = maxValue; // - minValue;

            double[] data;
            double[] totalColumns = new double[range + 1];
            double[] totalRows = new double[range + 1];
            double[] totalSW_NE = new double[range + 1];
            double[] totalSE_NW = new double[range + 1];
            double[] spanColumns = new double[range + 1];
            double[] spanRows = new double[range + 1];
            double[] spanSW_NE = new double[range + 1];
            double[] spanSE_NW = new double[range + 1];
            boolean[] counter;
            double[] shapeComplexity = new double[range + 1];
            
            updateProgress("Loop 1 of 5:", 0);
            for (row = 0; row < numRows; row++) {
                previousZ = (int) noData;
                counter = new boolean[range + 1];
                data = image.getRowValues(row);
                for (col = 0; col < numCols; col++) {
                    if (data[col] > 0 && data[col] != previousZ) {
                        totalRows[(int) data[col]]++;
                        if (counter[(int) data[col]] == false) {
                            spanRows[(int) data[col]]++;
                            counter[(int) data[col]] = true;
                        }
                    }
                    previousZ = (int) data[col];
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (numRows - 1));
                updateProgress("Loop 1 of 5:", (int) progress);
            }

            updateProgress("Loop 2 of 5:", 0);
            for (col = 0; col < numCols; col++) {
                previousZ = (int) noData;
                counter = new boolean[range + 1];
                data = image.getRowValues(row);
                for (row = 0; row < numRows; row++) {
                    z = (int)image.getValue(row, col);
                    if (z > 0 && data[col] != previousZ) {
                        totalColumns[z]++;
                        if (counter[z] == false) {
                            spanColumns[z]++;
                            counter[z] = true;
                        }
                    }
                    previousZ = z;
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (numRows - 1));
                updateProgress("Loop 2 of 5:", (int) progress);
            }

            int j, k;
            int numCells = numCols * numRows;

            updateProgress("Loop 3 of 5:", 0);
            row = 0;
            col = 0;
            k = 0;
            do {
                i = col;
                j = row;
                previousZ = (int) noData;
                counter = new boolean[range + 1];
                do {
                    k++;
                    z = (int) (image.getValue(j, i));
                    if (z > 0 && z != previousZ) {
                        totalSW_NE[z]++;
                        if (counter[z] == false) {
                            spanSW_NE[z]++;
                            counter[z] = true;
                        }
                    }
                    previousZ = z;
                    i++;
                    j--;
                } while (i < numCols && j >= 0);

                if (row == (numRows - 1)) {
                    col++;
                } else {
                    row++;
                }


                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (k * 100f / numCells);
                updateProgress("Loop 3 of 5:", (int) progress);
            } while (col < numCols);
               		
            updateProgress("Loop 4 of 5:", 0);
            row = 0;
            col = numCols - 1;
            k = 0;
            do {
                i = col;
                j = row;
                previousZ = (int) noData;
                counter = new boolean[range + 1];
                do {
                    k++;
                    z = (int) (image.getValue(j, i));
                    if (z > 0 && z != previousZ) {
                        totalSE_NW[z]++;
                        if (counter[z] == false) {
                            spanSE_NW[z]++;
                            counter[z] = true;
                        }
                    }
                    previousZ = z;
                    i--;
                    j--;
                } while (i >= 0 && j >= 0);

                if (row == (numRows - 1)) {
                    col--;
                } else {
                    row++;
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (k * 100f / numCells);
                updateProgress("Loop 4 of 5:", (int) progress);

            } while (col >= 0);
              

            for (a = 0; a <= range; a++) {
                if (totalColumns[a] > 0) {
                    shapeComplexity[a] = (totalColumns[a] / spanColumns[a] + totalRows[a] / spanRows[a]
                            + totalSW_NE[a] / spanSW_NE[a] + totalSE_NW[a] / spanSE_NW[a]) / 4;
                }
            }

            updateProgress("Loop 5 of 5:", 0);
            for (row = 0; row < numRows; row++) {
                data = image.getRowValues(row);
                for (col = 0; col < numCols; col++) {
                    if (data[col] > 0) {
                        output.setValue(row, col, shapeComplexity[(int) data[col]]);

                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (numRows - 1));
                updateProgress("Loop 5 of 5:", (int) progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            image.close();
            output.close();

            if (blnTextOutput) {
                DecimalFormat df;
                df = new DecimalFormat("0.0000");

                String retstr = "Shape Complexity Index\nPatch ID\tComplexity";

                for (a = 0; a <= range; a++) {
                    if (shapeComplexity[a] > 0) {
                        retstr = retstr + "\n" + a + "\t" + df.format(shapeComplexity[a]);
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
