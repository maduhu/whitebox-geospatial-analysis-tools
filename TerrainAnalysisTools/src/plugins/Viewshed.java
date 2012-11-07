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
import java.util.Random;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Viewshed implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "Viewshed";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
    	return "Viewshed";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
    	return "Identifies the viewshed for a point or set of points.";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
    	String[] ret = { "TerrainAnalysis" };
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
        
        String inputHeader;
        String inputViewingStation;
        String outputHeader;
        int row, col, rows, cols;
        int progress = 0;
        double z, noData, outputNoData;
        double stationHeight;
        double[] data;
        double vertCount = 1;
        double horizCount;
        double t1, t2, tva;
        int stationRow = 220;
        int stationCol = 28;
        double x, y, dist, dZ, viewAngleValue;
        double va;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        inputHeader = args[0];
        inputViewingStation = args[1];
        outputHeader = args[2];
        stationHeight = Double.parseDouble(args[3]);
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            rows = DEM.getNumberRows();
            cols = DEM.getNumberColumns();
            noData = DEM.getNoDataValue();
            outputNoData = -32768;
            double stationX = DEM.getXCoordinateFromColumn(stationCol);
            double stationY = DEM.getYCoordinateFromRow(stationRow);
            double stationZ = DEM.getValue(stationRow, stationCol) + stationHeight;
            
            
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    inputHeader, WhiteboxRaster.DataType.INTEGER, 0);
            output.setNoDataValue(outputNoData);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            
            // create a temporary raster to hold the view angle
            WhiteboxRaster viewAngle = new WhiteboxRaster(outputHeader.replace(".dep", "_temp1.dep"), "rw", 
                    inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            viewAngle.isTemporaryFile = true;
            
            // create a temporary raster to hold the max view angle
            WhiteboxRaster maxViewAngle = new WhiteboxRaster(outputHeader.replace(".dep", "_temp2.dep"), "rw", 
                    inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            maxViewAngle.isTemporaryFile = true;
            
            Random generator = new Random();
            for (int a = 0; a < 500; a++) {
                stationRow = generator.nextInt(rows - 1);
                stationCol = generator.nextInt(cols - 1);
                
                stationX = DEM.getXCoordinateFromColumn(stationCol);
                stationY = DEM.getYCoordinateFromRow(stationRow);
                stationZ = DEM.getValue(stationRow, stationCol) + stationHeight;
            
            
            
            for (row = 0; row < rows; row++) {
                data = DEM.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    z = data[col];
                    if (z != noData) {
                        x = DEM.getXCoordinateFromColumn(col);
                        y = DEM.getYCoordinateFromRow(row);
                        dZ = z - stationZ;
                        dist = Math.sqrt((x - stationX) * (x - stationX) + (y - stationY) * (y - stationY));
                        if (dist != 0.0) {
                            viewAngleValue = dZ / dist * 1000;
                            viewAngle.setValue(row, col, viewAngleValue);
                        }
                    } else {
                        viewAngle.setValue(row, col, outputNoData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress(progress);
            }
            
            // perform the simple scan lines.
            for (row = stationRow - 1; row <= stationRow + 1; row++) {
                for (col = stationCol - 1; col <= stationCol + 1; col++) {
                    maxViewAngle.setValue(row, col, viewAngle.getValue(row, col));
                }
            }
            
            double maxVA = viewAngle.getValue(stationRow - 1, stationCol);
            for (row = stationRow - 2; row >= 0; row--) {
                z = viewAngle.getValue(row, stationCol);
                if (z > maxVA) {
                    maxVA = z;
                }
                maxViewAngle.setValue(row, stationCol, maxVA);
            }
            
            maxVA = viewAngle.getValue(stationRow + 1, stationCol);
            for (row = stationRow + 2; row < rows; row++) {
                z = viewAngle.getValue(row, stationCol);
                if (z > maxVA) {
                    maxVA = z;
                }
                maxViewAngle.setValue(row, stationCol, maxVA);
            }
            
            maxVA = viewAngle.getValue(stationRow, stationCol + 1);
            for (col = stationCol + 2; col < cols - 1; col++) {
                z = viewAngle.getValue(stationRow, col);
                if (z > maxVA) {
                    maxVA = z;
                }
                maxViewAngle.setValue(stationRow, col, maxVA);
            }
            
            maxVA = viewAngle.getValue(stationRow, stationCol - 1);
            for (col = stationCol - 2; col >= 0; col--) {
                z = viewAngle.getValue(stationRow, col);
                if (z > maxVA) {
                    maxVA = z;
                }
                maxViewAngle.setValue(stationRow, col, maxVA);
            }
            
            //solve the first triangular facet
            for (row = stationRow - 2; row >= 0; row--) {
                vertCount++;
                horizCount = 0;
                for (col = stationCol + 1; col <= stationCol + vertCount; col++) {
                    if (col <= cols) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row + 1, col - 1);
                            t2 = maxViewAngle.getValue(row + 1, col);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row + 1, col - 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }
            
            //solve the second triangular facet
            vertCount = 1;
            for (row = stationRow - 2; row >= 0; row--) {
                vertCount++;
                horizCount = 0;
                for (col = stationCol - 1; col >= stationCol - vertCount; col--) {
                    if (col >= 0) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row + 1, col + 1);
                            t2 = maxViewAngle.getValue(row + 1, col);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row + 1, col + 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }
            
            // solve the third triangular facet
            vertCount = 1;
            for (row = stationRow + 2; row < rows; row++) {
                vertCount++;
                horizCount = 0;
                for (col = stationCol - 1; col >= stationCol - vertCount; col--) {
                    if (col >= 0) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row - 1, col + 1);
                            t2 = maxViewAngle.getValue(row - 1, col);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row - 1, col + 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }
            
            // solve the fourth triangular facet
            vertCount = 1;
            for (row = stationRow + 2; row < rows; row++) {
                vertCount++;
                horizCount = 0;
                for (col = stationCol + 1; col <= stationCol + vertCount; col++) {
                    if (col < cols) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row - 1, col - 1);
                            t2 = maxViewAngle.getValue(row - 1, col);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row - 1, col - 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }
            
            // solve the fifth triangular facet
            vertCount = 1;
            for (col = stationCol + 2; col < cols; col++) {
                vertCount++;
                horizCount = 0;
                for (row = stationRow - 1; row >= stationRow - vertCount; row--) {
                    if (row >= 0) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row + 1, col - 1);
                            t2 = maxViewAngle.getValue(row, col - 1);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row + 1, col - 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the sixth triangular facet
            vertCount = 1;
            for (col = stationCol + 2; col < cols; col++) {
                vertCount++;
                horizCount = 0;
                for (row = stationRow + 1; row <= stationRow + vertCount; row++) {
                    if (row < rows) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row - 1, col - 1);
                            t2 = maxViewAngle.getValue(row, col - 1);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row - 1, col - 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }

            // solve the seventh triangular facet
            vertCount = 1;
            for (col = stationCol - 2; col >= 0; col--) {
                vertCount++;
                horizCount = 0;
                for (row = stationRow + 1; row <= stationRow + vertCount; row++) {
                    if (row < rows) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row - 1, col + 1);
                            t2 = maxViewAngle.getValue(row, col + 1);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row - 1, col + 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }
            
            // solve the eigth triangular facet
            vertCount = 1;
            for (col = stationCol - 2; col >= 0; col--) {
                vertCount++;
                horizCount = 0;
                for (row = stationRow - 1; row >= stationRow - vertCount; row--) {
                    if (row < rows) {
                        va = viewAngle.getValue(row, col);
                        horizCount++;
                        if (horizCount != vertCount) {
                            t1 = maxViewAngle.getValue(row + 1, col + 1);
                            t2 = maxViewAngle.getValue(row, col + 1);
                            tva = t2 + horizCount / vertCount * (t1 - t2);
                        } else {
                            tva = maxViewAngle.getValue(row + 1, col + 1);
                        }
                        if (tva > va) {
                            maxViewAngle.setValue(row, col, tva);
                        } else {
                            maxViewAngle.setValue(row, col, va);
                        }
                    } else {
                        break;
                    }
                }
            }
            //output.flush();
            viewAngle.flush();
            maxViewAngle.flush();
            
            double[] dataVA;
            for (row = 0; row < rows; row++) {
                dataVA = viewAngle.getRowValues(row);
                data = maxViewAngle.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] <= dataVA[col] && dataVA[col] != outputNoData) {
                        output.setValue(row, col, output.getValue(row, col) + 1);
                    } else if (dataVA[col] == outputNoData) {
                        output.setValue(row, col, outputNoData);
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
                        
            viewAngle.close();
            maxViewAngle.close();
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
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
    
    // this is only used for testing the tool
    public static void main(String[] args) {
        Viewshed vs = new Viewshed();
        args = new String[4];
        args[0] = "/Users/johnlindsay/Documents/Data/Vermont DEM/Vermont DEM.dep";
        args[1] = "/Users/johnlindsay/Documents/Data/Vermont DEM/tmp2.dep";
        args[2] = "/Users/johnlindsay/Documents/Data/Vermont DEM/temp2.dep";
        args[3] = "2";
        
        vs.setArgs(args);
        vs.run();
        
    }
}