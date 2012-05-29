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

import java.io.*;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ExportArcAsciiGrid implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "ExportArcAsciiGrid";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Export ArcGIS ASCII Grid";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Exports an ArcGIS ASCII grid file (.txt).";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"IOTools"};
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
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
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

        String inputFilesString = null;
        String arcFile = null;
        String whiteboxHeaderFile = null;
        int i = 0;
        int row, col, rows, cols;
        String[] imageFiles;
        int numImages = 0;
        double noData = -32768;
        InputStream inStream = null;
        OutputStream outStream = null;
        int progress = 0;

        String str1 = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputFilesString = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        imageFiles = inputFilesString.split(";");
        numImages = imageFiles.length;

        try {

            for (i = 0; i < numImages; i++) {
                progress = (int)(100f * i / (numImages - 1));
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                
                whiteboxHeaderFile = imageFiles[i];
                if (!((new File(whiteboxHeaderFile)).exists())) {
                    showFeedback("Whitebox raster file does not exist.");
                    break;
                }
                WhiteboxRaster wbr = new WhiteboxRaster(whiteboxHeaderFile, "r");
                rows = wbr.getNumberRows();
                cols = wbr.getNumberColumns();
                noData = wbr.getNoDataValue();
                
                // arc file name.
                arcFile = whiteboxHeaderFile.replace(".dep", ".txt");
                
                // see if it exists, and if so, delete it.
                (new File(arcFile)).delete();
                
                // deal with the header data first
                fw = new FileWriter(arcFile, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);
                str1 = "NCOLS         " + String.valueOf(wbr.getNumberColumns());
                out.println(str1);    
                str1 = "NROWS         " + String.valueOf(wbr.getNumberRows());
                out.println(str1);    
                str1 = "XLLCORNER     " + String.valueOf(wbr.getWest());
                out.println(str1);
                str1 = "YLLCORNER     " + String.valueOf(wbr.getSouth());
                out.println(str1);
                str1 = "CELLSIZE      " + String.valueOf(
                        (wbr.getCellSizeX() + wbr.getCellSizeY()) / 2);
                out.println(str1);
                str1 = "NODATA_VALUE  -9999";
                out.println(str1);
                
                // copy the data file.
                double[] data;
                String line;
                if (wbr.getDataType() == WhiteboxRaster.DataType.FLOAT ||
                        wbr.getDataType() == WhiteboxRaster.DataType.DOUBLE) {
                    for (row = 0; row < rows; row++) {
                        data = wbr.getRowValues(row);
                        line = "";
                        str1 = "";
                        for (col = 0; col < cols; col++) {
                            if (col != 0) {
                                str1 = " ";
                            }
                            if (data[col] != noData) {
                                str1 += String.valueOf((float) data[col]);
                            } else {
                                str1 += "-9999";
                            }
                            line += str1;
                        }
                        out.println(line);
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (int) (100f * row / (rows - 1));
                        updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                    }
                } else {
                    for (row = 0; row < rows; row++) {
                        data = wbr.getRowValues(row);
                        line = "";
                        str1 = "";
                        for (col = 0; col < cols; col++) {
                            if (col != 0) {
                                str1 = " ";
                            }
                            if (data[col] != noData) {
                                str1 += String.valueOf((int)data[col]);
                            } else {
                                str1 += "-9999";
                            }
                            line += str1;
                        }
                        out.println(line);
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress = (int) (100f * row / (rows - 1));
                        updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);
                    }
                }
                
                wbr.close();
                
                // delete the temp file's header file (data file has already been renamed).
                (new File(whiteboxHeaderFile.replace(".dep", "_temp.dep"))).delete();
            }


        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }

            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}
