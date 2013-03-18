/*
 * Copyright (C) 2011-2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.*;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MinimumBoundingBox implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;

    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "MinimumBoundingBox";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Minimum Bounding Box";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Identfies the minimum bounding box around vector polygons or lines.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"VectorTools"};
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
            System.out.println("Progress: " + progress + "%");
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
            System.out.println("Progress: " + progress + "%");
        }
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
        String inputFile;
        double x, y;
        int progress;
        int oldProgress;
        int i, n;
        double[][] vertices = null;
        int numPolys = 0;
        ShapeType shapeType, outputShapeType = ShapeType.POLYLINE;
        int[] parts = {0};
        double psi = 0;

        double DegreeToRad = Math.PI / 180;
        double[] axes = new double[2];
        double[][] axesEndPoints = new double[4][2];
        double newXAxis = 0;
        double newYAxis = 0;
        double longAxis;
        double shortAxis;
        final double rightAngle = Math.toRadians(90);
        double midX, midY;
        double[] newBoundingBox = new double[4];
        double slope;
        double boxCentreX, boxCentreY;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputFile = args[0];
        String outputFile = args[1];
        if (args[2].toLowerCase().contains("true")) {
            outputShapeType = ShapeType.POLYGON;
        } else {
            outputShapeType = ShapeType.POLYLINE;
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFile == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            // set up the input shapefile.
            ShapeFile input = new ShapeFile(inputFile);
            shapeType = input.getShapeType();
            numPolys = input.getNumberOfRecords();

            // make sure that the shapetype is a flavour of polygon or polyline.
            if (shapeType.getBaseType() != ShapeType.POLYGON && shapeType.getBaseType() != ShapeType.POLYLINE) {
                showFeedback("This tool only works with shapefiles of a polygon or line base shape type.");
                return;
            }
            
            DBFField fields[] = new DBFField[4];

            fields[0] = new DBFField();
            fields[0].setName("PARENT_ID");
            fields[0].setDataType(DBFField.FIELD_TYPE_N);
            fields[0].setFieldLength(10);
            fields[0].setDecimalCount(0);
            
            fields[1] = new DBFField();
            fields[1].setName("SHRT_AXIS");
            fields[1].setDataType(DBFField.FIELD_TYPE_N);
            fields[1].setFieldLength(10);
            fields[1].setDecimalCount(3);
            
            fields[2] = new DBFField();
            fields[2].setName("LNG_AXIS");
            fields[2].setDataType(DBFField.FIELD_TYPE_N);
            fields[2].setFieldLength(10);
            fields[2].setDecimalCount(3);
            
            fields[3] = new DBFField();
            fields[3].setName("ELONGATION");
            fields[3].setDataType(DBFField.FIELD_TYPE_N);
            fields[3].setFieldLength(10);
            fields[3].setDecimalCount(3);
            
            ShapeFile output = new ShapeFile(outputFile, outputShapeType, fields);
            
            // find the minimum bounding box of each shape and locate points at the top and bottom centres
            int recordNum;
            oldProgress = -1;
            for (ShapeFileRecord record : input.records) {
                recordNum = record.getRecordNumber();
                vertices = record.getGeometry().getPoints();
                int numVertices = vertices.length;
                double east = Double.NEGATIVE_INFINITY;
                double west = Double.POSITIVE_INFINITY;
                double north = Double.NEGATIVE_INFINITY;
                double south = Double.POSITIVE_INFINITY;

                for (i = 0; i < numVertices; i++) {
                    if (vertices[i][0] > east) {
                        east = vertices[i][0];
                    }
                    if (vertices[i][0] < west) {
                        west = vertices[i][0];
                    }
                    if (vertices[i][1] > north) {
                        north = vertices[i][1];
                    }
                    if (vertices[i][1] < south) {
                        south = vertices[i][1];
                    }

                }

                midX = west + (east - west) / 2.0;
                midY = south + (north - south) / 2.0;


                double[][] verticesRotated = new double[numVertices][2];
                axes[0] = 9999999;
                axes[1] = 9999999;
                slope = 0;
                boxCentreX = 0;
                boxCentreY = 0;
                // Rotate the edge cells in 0.5 degree increments.
                for (int m = 0; m <= 180; m++) {
                    psi = -m * 0.5 * DegreeToRad; // rotation in clockwise direction
                    // Rotate each edge cell in the array by m degrees.
                    for (n = 0; n < numVertices; n++) {
                        x = vertices[n][0] - midX;
                        y = vertices[n][1] - midY;
                        verticesRotated[n][0] = (x * Math.cos(psi)) - (y * Math.sin(psi));
                        verticesRotated[n][1] = (x * Math.sin(psi)) + (y * Math.cos(psi));
                    }
                    // calculate the minimum bounding box in this coordinate 
                    // system and see if it is less
                    newBoundingBox[0] = Double.MAX_VALUE; // west
                    newBoundingBox[1] = Double.MIN_VALUE; // east
                    newBoundingBox[2] = Double.MAX_VALUE; // north
                    newBoundingBox[3] = Double.MIN_VALUE; // south
                    for (n = 0; n < numVertices; n++) {
                        x = verticesRotated[n][0];
                        y = verticesRotated[n][1];
                        if (x < newBoundingBox[0]) {
                            newBoundingBox[0] = x;
                        }
                        if (x > newBoundingBox[1]) {
                            newBoundingBox[1] = x;
                        }
                        if (y < newBoundingBox[2]) {
                            newBoundingBox[2] = y;
                        }
                        if (y > newBoundingBox[3]) {
                            newBoundingBox[3] = y;
                        }
                    }
                    newXAxis = newBoundingBox[1] - newBoundingBox[0];
                    newYAxis = newBoundingBox[3] - newBoundingBox[2];
                    
                    if ((newXAxis * newYAxis) < (axes[0] * axes[1])) { // minimize the area of the bounding box.
                        axes[0] = newXAxis;
                        axes[1] = newYAxis;

                        if (axes[0] > axes[1]) {
                            slope = -psi;
                        } else {
                            slope = -(rightAngle + psi);
                        }
                        x = newBoundingBox[0] + newXAxis / 2;
                        y = newBoundingBox[2] + newYAxis / 2;
                        boxCentreX = midX + (x * Math.cos(-psi)) - (y * Math.sin(-psi));
                        boxCentreY = midY + (x * Math.sin(-psi)) + (y * Math.cos(-psi));
                    }
                }
                longAxis = Math.max(axes[0], axes[1]);
                shortAxis = Math.min(axes[0], axes[1]);
                
                axesEndPoints[0][0] = boxCentreX + longAxis / 2.0 * Math.cos(slope);
                axesEndPoints[0][1] = boxCentreY + longAxis / 2.0 * Math.sin(slope);
                axesEndPoints[1][0] = boxCentreX - longAxis / 2.0 * Math.cos(slope);
                axesEndPoints[1][1] = boxCentreY - longAxis / 2.0 * Math.sin(slope);
                
                axesEndPoints[2][0] = boxCentreX + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                axesEndPoints[2][1] = boxCentreY + shortAxis / 2.0 * Math.sin(rightAngle + slope);
                axesEndPoints[3][0] = boxCentreX - shortAxis / 2.0 * Math.cos(rightAngle + slope);
                axesEndPoints[3][1] = boxCentreY - shortAxis / 2.0 * Math.sin(rightAngle + slope);
                
                Object[] rowData = new Object[4];
                rowData[0] = new Double(recordNum);
                rowData[1] = new Double(shortAxis);
                rowData[2] = new Double(longAxis);
                rowData[3] = new Double(1 - shortAxis / longAxis);
                
                double[][] points = new double[5][2];
                points[0][0] = axesEndPoints[0][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                points[0][1] = axesEndPoints[0][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);
                
                points[1][0] = axesEndPoints[0][0] - shortAxis / 2.0 * Math.cos(rightAngle + slope);
                points[1][1] = axesEndPoints[0][1] - shortAxis / 2.0 * Math.sin(rightAngle + slope);

                points[2][0] = axesEndPoints[1][0] - shortAxis / 2.0 * Math.cos(rightAngle + slope);
                points[2][1] = axesEndPoints[1][1] - shortAxis / 2.0 * Math.sin(rightAngle + slope);
                
                points[3][0] = axesEndPoints[1][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                points[3][1] = axesEndPoints[1][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);
                
                points[4][0] = axesEndPoints[0][0] + shortAxis / 2.0 * Math.cos(rightAngle + slope);
                points[4][1] = axesEndPoints[0][1] + shortAxis / 2.0 * Math.sin(rightAngle + slope);
                
                Geometry poly;
                if (outputShapeType == ShapeType.POLYLINE) {
                    poly = new PolyLine(parts, points);
                } else {
                    poly = new Polygon(parts, points);
                }
                output.addRecord(poly, rowData);
                
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) ((recordNum * 100.0) / numPolys);
                if (progress > oldProgress) {
                    updateProgress(progress);
                }
                oldProgress = progress;
                
            }

            output.write();

            // returning a header file string displays the image.
            updateProgress("Displaying vector: ", 0);
            returnData(outputFile);


        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }

    }

    //This method is only used during testing.
    public static void main(String[] args) {
        args = new String[3];
        args[0] = "/Users/johnlindsay/Documents/Research/Contracts/NRCan 2012/Data/MediumLakes/medium lakes2.shp";
        args[1] = "/Users/johnlindsay/Documents/Research/Contracts/NRCan 2012/Data/MediumLakes/tmp3.shp";
        args[2] = "POLYLINE";
        
        MinimumBoundingBox mbb = new MinimumBoundingBox();
        mbb.setArgs(args);
        mbb.run();
    }
}