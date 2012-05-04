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
import java.util.Date;
import java.io.IOException;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.geospatialfiles.GeoTiff;
//import whitebox.utilities.BitOps;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ImportGeoTiff implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "ImportGeoTiff";
    }

    @Override
    public String getDescriptiveName() {
        return "Import GeoTIFF (.tif)";
    }

    @Override
    public String getToolDescription() {
        return "Imports a GeoTIFF.";
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
        String whiteboxHeaderFile = null;
        String whiteboxDataFile = null;
        WhiteboxRaster output = null;
        int i = 0;
        String[] imageFiles;
        int numImages = 0;
        double noData = -32768;
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
                //int progress = (int) (100f * i / (numImages - 1));
                updateProgress("Loop " + (i + 1) + " of " + numImages + ":", 0);
                GeoTiff gt = new GeoTiff(imageFiles[i]);
                gt.read();
                //gt.showInfo(System.out);
                
                int nRows = gt.getNumberRows();
                int nCols = gt.getNumberColumns();

                int dot = imageFiles[i].lastIndexOf(".");
                String tiffExtension = imageFiles[i].substring(dot + 1); // either .tif or .tiff
                whiteboxHeaderFile = imageFiles[i].replace(tiffExtension, "dep");
                whiteboxDataFile = imageFiles[i].replace(tiffExtension, "tas");

                // see if they exist, and if so, delete them.
                (new File(whiteboxHeaderFile)).delete();
                (new File(whiteboxDataFile)).delete();

                // create the whitebox header file.
                fw = new FileWriter(whiteboxHeaderFile, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
                out.println(str1);
                str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
                out.println(str1);
                str1 = "North:\t" + Double.toString(gt.getNorth());
                out.println(str1);
                str1 = "South:\t" + Double.toString(gt.getSouth());
                out.println(str1);
                str1 = "East:\t" + Double.toString(gt.getEast());
                out.println(str1);
                str1 = "West:\t" + Double.toString(gt.getWest());
                out.println(str1);
                str1 = "Cols:\t" + Integer.toString(nCols);
                out.println(str1);
                str1 = "Rows:\t" + Integer.toString(nRows);
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
                str1 = "Preferred Palette:\t" + "grey.pal";
                out.println(str1);
                str1 = "NoData:\t" + gt.getNoData(); //-32768";
                out.println(str1);
                if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
                } else {
                    str1 = "Byte Order:\t" + "BIG_ENDIAN";
                }
                out.println(str1);

                // Create the whitebox raster object.
                WhiteboxRaster wbr = new WhiteboxRaster(whiteboxHeaderFile, "rw");

                double[] data = null;
                for (int row = 0; row < nRows; row++) {
                    data = gt.getRowData(row);
                    if (data == null) {
                        showFeedback("The GeoTIFF reader cannot read this type of file.");
                        return;
                    }
                    for (int col = 0; col < nCols; col++) {
                        wbr.setValue(row, col, data[col]);
                    }
                    progress = (int)(100f * row / (nRows - 1));
                    updateProgress(progress);
                }
                
                wbr.flush();
                //wbr.findMinAndMaxVals();
                wbr.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
                wbr.addMetadataEntry("Created on " + new Date());
                String[] metaData = gt.showInfo();
                for (int a = 0; a < metaData.length; a++) {
                    wbr.addMetadataEntry(metaData[a]);
                }
                wbr.writeHeaderFile();
                wbr.close();

                gt.close();

                // returning a header file string displays the image.
                //returnData(whiteboxHeaderFile);
            }


        } catch (IOException e) {
            showFeedback(e.getMessage());
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
