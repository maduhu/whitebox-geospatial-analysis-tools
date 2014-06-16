/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import whitebox.geospatialfiles.LASReader;
import whitebox.geospatialfiles.LASReader.PointRecColours;
import whitebox.geospatialfiles.LASReader.PointRecord;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.*;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.structures.KdTree;

/**
 *
 * @author Dr. John Lindsay
 */
public class LiDAR_segmentation implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    KdTree<Integer> pointsTree;
    LidarData[] data;
    double threshold;
    double searchDist;
    long numClassifiedPoints = 0;

    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "LiDAR_segmentation";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "LiDAR Segmentation";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Segments LiDAR points contained in a LAS file into groups.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"LidarTools"};
        return ret;
    }

    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the
     * class that the plugin will send all feedback messages, progress updates,
     * and return objects.
     *
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    /**
     * Used to communicate feedback pop-up messages between a plugin tool and
     * the main Whitebox user-interface.
     *
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
     * Used to communicate a return object from a plugin tool to the main
     * Whitebox user-interface.
     *
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
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + String.valueOf(progress) + "%");
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        } else {
            System.out.println("Progress: " + String.valueOf(progress) + "%");
        }
        previousProgress = progress;
    }

    /**
     * Sets the arguments (parameters) used by the plugin.
     *
     * @param args
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     *
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
     *
     * @return a boolean describing whether or not the plugin is actively being
     * used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;

        double x, y;
        double z = 0;
        int a, intensity;
        int oldProgress, progress;
        int numPoints = 0;
        PointRecord point;
        double[] entry;

        // get the arguments
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];
        threshold = Double.parseDouble(args[2]);
        // convert the threshold from degrees to tan slope
        threshold = Math.tan(Math.toRadians(threshold));
        searchDist = Double.parseDouble(args[3]);

        // check to see that the input and output are not null.
        if ((inputFile.length() <= 0 || outputFile.length() <= 0)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            LASReader las = new LASReader(inputFile);
            numPoints = (int) las.getNumPointRecords();
            data = new LidarData[numPoints];

            // Read the valid points into the k-dimensional tree.
            pointsTree = new KdTree.SqrEuclid<>(2, numPoints);
            oldProgress = -1;
            for (a = 0; a < numPoints; a++) {
                point = las.getPointRecord(a);
                if (!point.isPointWithheld()) {
                    x = point.getX();
                    y = point.getY();
                    z = point.getZ();
                    intensity = point.getIntensity();

                    entry = new double[]{x, y};
                    pointsTree.addPoint(entry, a);
                    data[a] = new LidarData(x, y, z, intensity, a);
                }
                progress = (int) (100f * (a + 1) / numPoints);
                if (progress != oldProgress) {
                    oldProgress = progress;
                    updateProgress("Reading point data:", progress);
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                }
            }

            int currentClass = 0;
            do {
                // find the first unclassified point
                int startingPoint = -1;
                for (a = 0; a < numPoints; a++) {
                    if (data[a].classValue == -1) {
                        startingPoint = a;
                        currentClass++;
                        break;
                    }
                }
                if (startingPoint == -1) {
                    break;
                }
                List<Integer> seeds = new ArrayList<>();
                seeds.add(startingPoint);
                boolean flag = false;
            
                do {
                    flag = false;
                    for (Integer s : seeds) {
                        data[s].setClassValue(currentClass);
                        scanNeighbours(s);
                    }
                    seeds.clear();
                    if (seedPoints.size() > 0) {
                        flag = true;
                        seedPoints.stream().forEach((s) -> {
                            seeds.add(s);
                        });
                        seedPoints.clear();
                    }
                    progress = (int) (100f * numClassifiedPoints / numPoints);
                    if (progress != oldProgress) {
                        oldProgress = progress;
                        updateProgress(progress);
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                    }
                } while (flag);
            } while (numClassifiedPoints < numPoints);
            DBFField fields[] = new DBFField[3];

            fields[0] = new DBFField();
            fields[0].setName("Z");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setFieldLength(10);
            fields[0].setDecimalCount(3);

            fields[1] = new DBFField();
            fields[1].setName("I");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setFieldLength(8);
            fields[1].setDecimalCount(0);

            fields[2] = new DBFField();
            fields[2].setName("CLASS");
            fields[2].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[2].setFieldLength(8);
            fields[2].setDecimalCount(0);

            File outFile = new File(outputFile);
            if (outFile.exists()) { outFile.delete(); }
            ShapeFile output = new ShapeFile(outputFile, ShapeType.POINT, fields);

            for (a = 0; a < numPoints; a++) {
                if (data[a].classValue > -1) {
                    whitebox.geospatialfiles.shapefile.Point wbGeometry = new whitebox.geospatialfiles.shapefile.Point(data[a].x, data[a].y);

                    Object[] rowData = new Object[3];
                    rowData[0] = data[a].z;
                    rowData[1] = (double) data[a].intensity;
                    rowData[2] = (double) data[a].classValue;

                    output.addRecord(wbGeometry, rowData);
                }
                
                progress = (int) (100f * (a + 1) / numPoints);
                if (progress != oldProgress) {
                    oldProgress = progress;
                    updateProgress("Outputting point data:", progress);
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                }
            }

            output.write();
            
            System.out.println("Done!");

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

    private long depth = 0;
    private long maxDepth = 1000;
    private List<Integer> seedPoints = new ArrayList<>();

    private void scanNeighbours(int refPointNum) {
        depth++;
        if (depth > maxDepth) {
            seedPoints.add(refPointNum);
            depth--;
            return;
        }

        int classValue = data[refPointNum].classValue;

        double[] entry = new double[]{data[refPointNum].x, data[refPointNum].y};
        List<KdTree.Entry<Integer>> results = pointsTree.neighborsWithinRange(entry, searchDist);
        for (int i = 0; i < results.size(); i++) {
            int pointNum = results.get(i).value;
            if (pointNum != data[refPointNum].pointNum) {
                if (data[pointNum].classValue == -1) {
                    double dist = Math.sqrt(results.get(i).distance);
                    if (Math.abs(data[pointNum].z - data[refPointNum].z) / dist <= threshold) {
                        data[pointNum].setClassValue(classValue);
                        scanNeighbours(pointNum);
                    }
                }
            }
        }       
        depth--;
    }

    //this is only used for debugging the tool
    public static void main(String[] args) {
        LiDAR_segmentation seg = new LiDAR_segmentation();
        args = new String[4];
        args[0] = "/Users/johnlindsay/Documents/Data/Rashaads Sites/CVC_all.las";
        args[1] = "/Users/johnlindsay/Documents/Data/Rashaads Sites/CVC ground points.shp";
        args[2] = "15.0"; // degree slope
        args[3] = "0.25"; // meter search window
        
//        args[0] = "/Users/johnlindsay/Documents/Data/LAS files/423_4695.las";
//        args[1] = "/Users/johnlindsay/Documents/Data/LAS files/423_4695 ground points2.shp";
//        args[2] = "20.0"; // degree slope
//        args[3] = "2.0"; // meter search window
        
        seg.setArgs(args);
        seg.run();

    }

    class LidarData {

        double x, y, z;
        int pointNum;
        int classValue = -1;
        int intensity;

        public LidarData(double x, double y, double z, int intensity, int pointNum) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.intensity = intensity;
            this.pointNum = pointNum;
        }
        
        public void setClassValue(int value) { 
            if (classValue != value) {
                this.classValue = value;
                numClassifiedPoints++;
            }
        }
    }
}
