/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author johnlindsay
 */
public class FilterKNearestMean implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "FilterKNearestMean";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
    	return "K-Nearest Mean Filter";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
    	return "Performs a k-nearest mean filter on an image.";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
    	String[] ret = { "Filters" };
    	return ret;
    }
    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the class
     * that the plugin will send all feedback messages, progress updates, and return objects.
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */  
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }
    /**
     * Used to communicate feedback pop-up messages between a plugin tool and the main Whitebox user-interface.
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }
    /**
     * Used to communicate a return object from a plugin tool to the main Whitebox user-interface.
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private int previousProgress = 0;
    private String previousProgressLabel = "";
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }
    /**
     * Sets the arguments (parameters) used by the plugin.
     * @param args 
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     * @return a boolean describing whether or not the plugin is actively being used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;
        
        String inputHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        double z;
        float progress = 0;
        int a;
        int filterSizeX = 3;
        int filterSizeY = 3;
        double n;
        double sum;
        double centreVal;
        int dX[];
        int dY[];
        int midPointX;
        int midPointY;
        int numPixelsInFilter;
        int k = 0;
        int kMinusOne = 0;
        int p = 0;
        double aLargeValue = Float.MAX_VALUE;
        double currentNearestVal;
        boolean filterRounded = false;
        double[] filterShape;
        boolean reflectAtBorders = false;
    
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
                filterSizeX = Integer.parseInt(args[i]);
            } else if (i == 3) {
                filterSizeY = Integer.parseInt(args[i]);
            } else if (i == 4) {
                filterRounded = Boolean.parseBoolean(args[i]);
            } else if (i == 5) {
                k = Integer.parseInt(args[i]);
            } else if (i == 6) {
                reflectAtBorders = Boolean.parseBoolean(args[i]);
            }
        }
        
        if (k < 3 || k > filterSizeX * filterSizeY) {
            showFeedback("The k-value of the k-nearest mean filter is set inappropriately.");
        }
        kMinusOne--;
        double[] nearestVals = new double[k];
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }
        
        try {
            WhiteboxRaster inputFile = new WhiteboxRaster(inputHeader, "r");
            inputFile.isReflectedAtEdges = reflectAtBorders;

            int rows = inputFile.getNumberRows();
            int cols = inputFile.getNumberColumns();
            double noData = inputFile.getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette(inputFile.getPreferredPalette());
            
            //the filter dimensions must be odd numbers such that there is a middle pixel
            if (Math.floor(filterSizeX / 2d) == (filterSizeX / 2d)) {
                showFeedback("Filter dimensions must be odd numbers. The specified filter x-dimension" + 
                        " has been modified.");
                
                filterSizeX++;
            }
            if (Math.floor(filterSizeY / 2d) == (filterSizeY / 2d)) {
                showFeedback("Filter dimensions must be odd numbers. The specified filter y-dimension" + 
                        " has been modified.");
                filterSizeY++;
            }

            numPixelsInFilter = filterSizeX * filterSizeY;
            
            double[][] absDiffVals = new double[2][numPixelsInFilter];
            
            dX = new int[numPixelsInFilter];
            dY = new int[numPixelsInFilter];
            filterShape = new double[numPixelsInFilter];

            //fill the filter DX and DY values
            midPointX = (int)Math.floor(filterSizeX / 2);
            midPointY = (int)Math.floor(filterSizeY / 2);
            if (!filterRounded) {
                a = 0;
                for (row = 0; row < filterSizeY; row++) {
                    for (col = 0; col < filterSizeX; col++) {
                        dX[a] = col - midPointX;
                        dY[a] = row - midPointY;
                        filterShape[a] = 1;
                        a++;
                     }
                }
            } else {
                //see which pixels in the filter lie within the largest ellipse 
                //that fits in the filter box 
                double aSqr = midPointX * midPointX;
                double bSqr = midPointY * midPointY;
                a = 0;
                for (row = 0; row < filterSizeY; row++) {
                    for (col = 0; col < filterSizeX; col++) {
                        dX[a] = col - midPointX;
                        dY[a] = row - midPointY;
                        z = (dX[a] * dX[a]) / aSqr + (dY[a] * dY[a]) / bSqr;
                        if (z > 1) {
                            filterShape[a] = 0;
                        } else {
                            filterShape[a] = 1;
                        }
                        a++;
                    }
                }
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    centreVal = inputFile.getValue(row, col);
                    if (centreVal != noData) {
                        for (a = 0; a < numPixelsInFilter; a++) {
                            x = col + dX[a];
                            y = row + dY[a];
                            z = inputFile.getValue(y, x);
                            if (z != noData && filterShape[a] == 1) {
                                absDiffVals[0][a] = (z - centreVal) * (z - centreVal);
                                absDiffVals[1][a] = z;
                            } else {
                                absDiffVals[0][a] = noData;
                            }
                        }
                        
                        n = 0;
                        sum = 0;
                        for (int m = 0; m < k; m++) {
                            p = -1;
                            currentNearestVal = aLargeValue;
                            for (a = 0; a < numPixelsInFilter; a++) {
                                if (absDiffVals[0][a] != noData && absDiffVals[0][a] < currentNearestVal) {
                                    p = a;
                                    currentNearestVal = absDiffVals[0][a];
                                }
                            }
                            if (p >= 0) {
                                n++;
                                sum += absDiffVals[1][p];
                                absDiffVals[0][p] = noData;
                            }
                        }

                        if (n == k) {
                            outputFile.setValue(row,col, sum / n);
                        } else {
                            outputFile.setValue(row,col, centreVal);
                        }       
                    } else {
                        outputFile.setValue(row, col, noData);
                    }

                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress((int) progress);
            }

            outputFile.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());
            
            inputFile.close();
            outputFile.close();

            // returning a header file string displays the image.
            returnData(outputHeader);

        } catch (OutOfMemoryError oe) {
            myHost.showFeedback("An out-of-memory error has occurred during operation.");
        } catch (Exception e) {
            myHost.showFeedback("An error has occurred during operation. See log file for details.");
            myHost.logException("Error in " + getDescriptiveName(), e);
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}