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

import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ImportSurferAsciiGrid implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ImportSurferAsciiGrid";
    }

    @Override
    public String getDescriptiveName() {
        return "Import Surfer ASCII Grid";
    }

    @Override
    public String getToolDescription() {
        return "Imports a Surfer ASCII grid file (.grd).";
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
        String surferFile = null;
        String whiteboxHeaderFile = null;
        int i = 0;
        int row, col, rows, cols;
        String[] imageFiles;
        int numImages = 0;
        int progress = 0;
        double cellsize = 0;
        double north = 0;
        double east = 0;
        double west = 0;
        double south = 0;
        double arcNoData = -9999;
        double whiteboxNoData = -32768d;
        double z = 0;
        String delimiter = " ";

        String str1 = null;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;

        DataInputStream in = null;
        BufferedReader br = null;

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
                progress = (int) (100f * i / (numImages - 1));
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", progress);

                surferFile = imageFiles[i];
                // check to see if the file exists.
                if (!((new File(surferFile)).exists())) {
                    showFeedback("Surfer raster file does not exist.");
                    break;
                }
                if (surferFile.lastIndexOf(".") >= 0) { // there is an extension
                    String extension = surferFile.substring(surferFile.lastIndexOf("."));
                    whiteboxHeaderFile = surferFile.replace(extension, ".dep");
                } else {
                    whiteboxHeaderFile = surferFile + ".dep";
                }
                
                (new File(whiteboxHeaderFile)).delete();
                (new File(whiteboxHeaderFile.replace(".dep", ".tas"))).delete();

                FileInputStream fstream = new FileInputStream(surferFile);
                rows = 0;
                cols = 0;

                // Get the object of DataInputStream
                in = new DataInputStream(fstream);

                br = new BufferedReader(new InputStreamReader(in));

                if (surferFile != null) {
                    String line;
                    String[] str;
                    //Read File Line By Line, getting the header data.
                    i = 1;
                    while ((line = br.readLine()) != null && i < 5) {
                        str = line.split(delimiter);
                        if (str.length <= 1 && i > 1) {
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
                        if (i == 1) {
                            if (!(str[0].toLowerCase().equals("dsaa"))) {
                                showFeedback("Warning: Surfer ASCII GRD ID value "
                                        + "was not located. This file may be "
                                        + "improperly structured.");
                            }
                        } else if (i == 2) {
                            cols = Integer.parseInt(str[0]);
                            rows = Integer.parseInt(str[1]);
                        } else if (i == 3) {
                            west = Double.parseDouble(str[0]);
                            east = Double.parseDouble(str[1]);
                        } else if (i == 4) {
                            south = Double.parseDouble(str[0]);
                            north = Double.parseDouble(str[1]);
                        } else {
                            break;
                        }
                        
                        i++;
                    }

                    // create the whitebox header file.
                    fw = new FileWriter(whiteboxHeaderFile, false);
                    bw = new BufferedWriter(fw);
                    out = new PrintWriter(bw, true);

                    str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
                    out.println(str1);
                    str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
                    out.println(str1);
                    str1 = "North:\t" + Double.toString(north);
                    out.println(str1);
                    str1 = "South:\t" + Double.toString(south);
                    out.println(str1);
                    str1 = "East:\t" + Double.toString(east);
                    out.println(str1);
                    str1 = "West:\t" + Double.toString(west);
                    out.println(str1);
                    str1 = "Cols:\t" + Integer.toString(cols);
                    out.println(str1);
                    str1 = "Rows:\t" + Integer.toString(rows);
                    out.println(str1);
                    str1 = "Data Type:\t" + "float";
                    out.println(str1);
                    str1 = "Z Units:\t" + "not specified";
                    out.println(str1);
                    str1 = "XY Units:\t" + "not specified";
                    out.println(str1);
                    str1 = "Projection:\t" + "not specified";
                    out.println(str1);
                    str1 = "Data Scale:\tcontinuous";
                    out.println(str1);
                    str1 = "Preferred Palette:\t" + "spectrum.pal";
                    out.println(str1);
                    str1 = "NoData:\t-32768";
                    out.println(str1);
                    if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                        str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
                    } else {
                        str1 = "Byte Order:\t" + "BIG_ENDIAN";
                    }
                    out.println(str1);

                    // Create the whitebox raster object.
                    WhiteboxRaster wbr = new WhiteboxRaster(whiteboxHeaderFile, "rw");

                    // Read File Line By Line, this time ingesting the data block
                    // and outputing it to the whitebox raster object.
                    delimiter = " ";
                    row = rows - 1;
                    col = 0;
                    int a = 0;
                    while ((line = br.readLine()) != null) {
                        if (a > 4) {
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

                            // read the data
                            for (i = 0; i < str.length; i++) {
                                z = Double.parseDouble(str[i]);
                                wbr.setValue(row, col, z);
                                
                                col++;
                                if (col == cols) {
                                    col = 0;
                                    row--;
                                }
                            }
                        }
                        a++;
                    }

                    //Close the input stream
                    in.close();
                    br.close();

                    wbr.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
                    wbr.addMetadataEntry("Created on " + new Date());
                    //wbr.findMinAndMaxVals();
                    //wbr.writeHeaderFile();
                    wbr.close();

                    returnData(whiteboxHeaderFile);
                }
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
