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

import java.nio.ByteOrder;
import java.io.*;
import java.util.Date;
import whitebox.geospatialfiles.GeoTiff;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.WhiteboxRasterBase;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
//import whitebox.utilities.BitOps;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ImportGeoTiff implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "ImportGeoTiff";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Import GeoTIFF (.tif)";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Imports a GeoTIFF.";
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
                
                ByteOrder byteOrder = gt.getByteOrder();
                
                WhiteboxRasterBase.DataScale myDataScale = WhiteboxRasterBase.DataScale.CONTINUOUS;
                if (gt.getPhotometricInterpretation() == 2) {
                    myDataScale = WhiteboxRasterBase.DataScale.RGB;
                }
                WhiteboxRaster wbr = new WhiteboxRaster(whiteboxHeaderFile, gt.getNorth(), gt.getSouth(), gt.getEast(),
                        gt.getWest(), nRows, nCols, myDataScale,
                        WhiteboxRasterBase.DataType.FLOAT, gt.getNoData(), gt.getNoData());
                
                wbr.setByteOrder(byteOrder.toString());

                double[] data = null;
                for (int row = 0; row < nRows; row++) {
                    data = gt.getRowData(row);
                    if (!(data != null)) {
                        showFeedback("The GeoTIFF reader cannot read 48-bit and 64-bit RGB and aRGB image, "
                                + "nor compressed data files, and files with tile offsets. We're working on it...");
                        return;
                    }
                    for (int col = 0; col < nCols; col++) {
                        wbr.setValue(row, col, data[col]);
                    }
                    progress = (int)(100f * row / (nRows - 1));
                    updateProgress(progress);
                }
                
                //wbr.flush();
                wbr.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
                wbr.addMetadataEntry("Created on " + new Date());
                String[] metaData = gt.showInfo();
                for (int a = 0; a < metaData.length; a++) {
                    wbr.addMetadataEntry(metaData[a]);
                }
                //wbr.writeHeaderFile();
                wbr.close();

                gt.close();

            }

            showFeedback("Operation complete");
            
        } catch (IOException e) {
            myHost.logException("Error in ImportGeoTiff.run", e);
            showFeedback(e.toString());
        } catch (Exception e) {
            myHost.logException("Error in ImportGeoTiff.run", e);
            showFeedback(e.toString());
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
