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

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ExportGRASSAsciiGrid implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ExportGRASSAsciiGrid";
    }

    @Override
    public String getDescriptiveName() {
        return "Export GRASS ASCII Grid";
    }

    @Override
    public String getToolDescription() {
        return "Exports a GRASS ASCII grid file (.txt).";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"IOTools"};
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

        String inputFilesString = null;
        String grassFile = null;
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
                // check to see if the file exists.
                if (!((new File(whiteboxHeaderFile)).exists())) {
                    showFeedback("Whitebox raster file does not exist.");
                    break;
                }
                WhiteboxRaster wbr = new WhiteboxRaster(whiteboxHeaderFile, "r");
                rows = wbr.getNumberRows();
                cols = wbr.getNumberColumns();
                noData = wbr.getNoDataValue();
                
                // grass file name.
                grassFile = whiteboxHeaderFile.replace(".dep", ".txt");
                
                // see if it exists, and if so, delete it.
                (new File(grassFile)).delete();
                
                // deal with the header data first
                fw = new FileWriter(grassFile, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);
                str1 = "north: " + String.valueOf(wbr.getNorth());
                out.println(str1);    
                str1 = "south: " + String.valueOf(wbr.getSouth());
                out.println(str1);    
                str1 = "east: " + String.valueOf(wbr.getEast());
                out.println(str1);    
                str1 = "west: " + String.valueOf(wbr.getWest());
                out.println(str1);    
                str1 = "rows: " + String.valueOf(wbr.getNumberRows());
                out.println(str1);    
                str1 = "cols: " + String.valueOf(wbr.getNumberColumns());
                out.println(str1);    
                
                // copy the data file.
                double[] data = null;
                String line = "";
                if (wbr.getDataType().equals("double") || wbr.getDataType().equals("float")) {
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
            return;
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
