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
import java.io.*;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class XYZTextToRaster implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    @Override
    public String getName() {
        return "XYZTextToRaster";
    }

    @Override
    public String getDescriptiveName() {
        return "Convert X,Y,Z text file to a raster";
    }

    @Override
    public String getToolDescription() {
        return "Converts the points in an ASCII text file into a raster grid.";
    }

    @Override
    public String[] getToolbox() {
        String[] ret = {"ConversionTools", "RasterCreation", "IOTools"};
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
        String[] pointFiles;
        String outputHeader = null;
        String baseFileHeader = null;
        int row, col;
        double x, y, z;
        double east, west, north, south;
        int i, j;
        int progress = 0;
        String delimiter = " ";
        boolean firstLineHeader = false;
        String str1 = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        double noData = -32768;
        WhiteboxRaster.DataType dataType = WhiteboxRaster.DataType.FLOAT;
            
        // get the arguments
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        inputFilesString = args[0];
        firstLineHeader = Boolean.parseBoolean(args[1]);
        outputHeader = args[2];
        baseFileHeader = args[3];
        if (args[4].toLowerCase().contains("double")) {
            dataType = WhiteboxRaster.DataType.DOUBLE;
        } else if (args[4].toLowerCase().contains("float")) {
            dataType = WhiteboxRaster.DataType.FLOAT;
        } else if (args[4].toLowerCase().contains("int")) {
            dataType = WhiteboxRaster.DataType.INTEGER;
        }
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString.length() <= 0) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            pointFiles = inputFilesString.split(";");
            int numPointFiles = pointFiles.length;
            WhiteboxRaster baseImage = new WhiteboxRaster(baseFileHeader, "r");
            double resolutionX = baseImage.getCellSizeX();
            double resolutionY = baseImage.getCellSizeY();
            //int rows = baseImage.getNumberRows();
            //int cols = baseImage.getNumberColumns();
            noData = baseImage.getNoDataValue();
            east = baseImage.getEast();
            west = baseImage.getWest();
            north = baseImage.getNorth();
            south = baseImage.getSouth();
            
                    
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    baseFileHeader, dataType, noData);
           
            for (i = 0; i < numPointFiles; i++) {
                DataInputStream in = null;
                BufferedReader br = null;
                try {
                    // Open the file that is the first command line parameter
                    FileInputStream fstream = new FileInputStream(pointFiles[i]);
                    // Get the object of DataInputStream
                    in = new DataInputStream(fstream);

                    br = new BufferedReader(new InputStreamReader(in));

                    String line;
                    String[] str;
                    j = 1;
                    //Read File Line By Line
                    while ((line = br.readLine()) != null) {
                        str = line.split(delimiter);
                        if (str.length <= 1) {
                            delimiter = "\t";
                            str = line.split(delimiter);
                            if (str.length <= 1) {
                                delimiter = " ";
                                str = line.split(delimiter);
                                if (str.length <= 1) {
                                    delimiter = ",";
                                    str = line.split(delimiter);
                                }
                            }
                        }
                        if ((j > 1 || !firstLineHeader) && (str.length >= 3)) {
                            x = Double.parseDouble(str[0]);
                            y = Double.parseDouble(str[1]);
                            z = Double.parseDouble(str[2]);
                            
                            row = (int)(Math.floor((y - south) / resolutionY));
                            col = (int)(Math.floor((x - west) / resolutionX));
                            
                            output.setValue(row, col, z);
                        }
                        j++;
                    }
                    //Close the input stream
                    in.close();
                    br.close();

                } catch (java.io.IOException e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    try {
                        if (in != null || br != null) {
                            in.close();
                            br.close();
                        }
                    } catch (java.io.IOException ex) {
                    }

                }
            }
            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

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