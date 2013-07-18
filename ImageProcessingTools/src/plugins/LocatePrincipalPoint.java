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

import java.util.ArrayList;
import java.util.Date;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.*;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINTZ;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTZ;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.structures.XYPoint;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 *
 * @author johnlindsay
 */
public class LocatePrincipalPoint implements WhiteboxPlugin {

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
        return "LocatePrincipalPoint";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Locate Principal Point";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Locates the principal point in an aerial photograph from fiducial marks.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"ImageEnhancement"};
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

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        String fiducialHeader = args[0];
        String outputHeader = args[1];

        // check to see that the inputHeader and outputHeader are not null.
        if (fiducialHeader.isEmpty() || outputHeader.isEmpty()) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            int progress = 0;
            ArrayList<XYPoint> fiducialMarks = new ArrayList<>();
            
            ShapeFile fiducials = new ShapeFile(fiducialHeader);

            if (fiducials.getShapeType().getBaseType() != ShapeType.POINT
                    || fiducials.getShapeType().getBaseType() != ShapeType.MULTIPOINT) {
                showFeedback("The input shapefile must be of a 'POINT' or 'MULTIPOINT' data type.");
                return;
            }

            DBFField[] fields = new DBFField[1];
            fields[0].setName("FID");
            fields[0].setDecimalCount(0);
            fields[0].setFieldLength(5);
            ShapeFile output = new ShapeFile(outputHeader, ShapeType.POINT, fields);
            
            // read in the fiducial marks
            progress = 0;
            for (ShapeFileRecord record : fiducials.records) {
                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                    double[][] vertices;
                    ShapeType shapeType = record.getShapeType();
                    switch (shapeType) {
                    case POINT:
                        whitebox.geospatialfiles.shapefile.Point recPoint =
                            (whitebox.geospatialfiles.shapefile.Point) (record.getGeometry());
                        vertices = recPoint.getPoints();
                        fiducialMarks.add(new XYPoint(vertices[0][0], vertices[0][1]));
                        break;
                    case POINTZ:
                        PointZ recPointZ = (PointZ) (record.getGeometry());
                        vertices = recPointZ.getPoints();
                        fiducialMarks.add(new XYPoint(vertices[0][0], vertices[0][1]));
                        break;
                    case POINTM:
                        PointM recPointM = (PointM) (record.getGeometry());
                        vertices = recPointM.getPoints();
                        fiducialMarks.add(new XYPoint(vertices[0][0], vertices[0][1]));
                        break;
                    case MULTIPOINT:
                        MultiPoint recMultiPoint = (MultiPoint) (record.getGeometry());
                        vertices = recMultiPoint.getPoints();
                        for (int j = 0; j < vertices.length; j++) {
                            fiducialMarks.add(new XYPoint(vertices[j][0], vertices[j][1]));
                        }
                        break;
                    case MULTIPOINTZ:
                        MultiPointZ recMultiPointZ = (MultiPointZ) (record.getGeometry());
                        vertices = recMultiPointZ.getPoints();
                        for (int j = 0; j < vertices.length; j++) {
                            fiducialMarks.add(new XYPoint(vertices[j][0], vertices[j][1]));
                        }
                        break;
                     case MULTIPOINTM:
                        MultiPointM recMultiPointM = (MultiPointM) (record.getGeometry());
                        vertices = recMultiPointM.getPoints();
                        for (int j = 0; j < vertices.length; j++) {
                            fiducialMarks.add(new XYPoint(vertices[j][0], vertices[j][1]));
                        }
                        break;
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress++;
                updateProgress(progress);
            }
            
            int numMarks = fiducialMarks.size();
            
            if (numMarks == 8) {
                
            } else if (numMarks == 4) {
                // are the fiducials arranged by the diagonal corners or the centres of sides?
                
            } else {
                showFeedback("There should be either 4 or 8 fiducial marks. \nThere is something wrong with the input file. \nThe operation will be terminated.");
                return;
            }
            
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
