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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;
import java.io.File;
import java.util.ArrayList;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.PointsList;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.ShapefilePoint;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.DBFReader;
import whitebox.geospatialfiles.shapefile.attributes.DBFWriter;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.utilities.Topology;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class VoronoiDiagram implements WhiteboxPlugin {

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
        return "VoronoiDiagram";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Voronoi Diagram (Thiessen Polygon)";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Creates a Voronoi diagram, or Thiessen Polygon, for a series of vector points.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"DistanceTools"};
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
        String inputFile;
        String outputFile;
        int progress;
        int i, j, n, FID;
        int oneHundredthTotal;
        int numRecs;
        int parentRecNum;
        ShapeType shapeType;
        GeometryFactory factory = new GeometryFactory();
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputFile = args[0];
        outputFile = args[1];

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFile == null) || (outputFile == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            // set up the input shapefile.
            ShapeFile input = new ShapeFile(inputFile);
            shapeType = input.getShapeType();
            if (shapeType.getBaseType() != ShapeType.POINT
                    && shapeType.getBaseType() != ShapeType.MULTIPOINT) {
                showFeedback("The input shapefile must have a POINT shape type.");
                return;
            }

            // create the clip envelope
            double minX = input.getxMin();
            double maxX = input.getxMax();
            double minY = input.getyMin();
            double maxY = input.getyMax();
            

            numRecs = input.getNumberOfRecords();

            oneHundredthTotal = numRecs / 100;

            // set up the output files of the shapefile and the dbf
            ShapeFile output = new ShapeFile(outputFile, ShapeType.POLYGON);

            DBFReader reader = new DBFReader(input.getDatabaseFile());
            int numFields = reader.getFieldCount();

            DBFField[] fields = reader.getAllFields();
            String DBFName = output.getDatabaseFile();
            DBFWriter writer = new DBFWriter(new File(DBFName));

            writer.setFields(fields);

            ArrayList<com.vividsolutions.jts.geom.Geometry> pointList = new ArrayList<com.vividsolutions.jts.geom.Geometry>();
            com.vividsolutions.jts.geom.Geometry[] recJTS = null;
            n = 0;
            progress = 0;
            for (ShapeFileRecord record : input.records) {
                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                    recJTS = record.getGeometry().getJTSGeometries();
                    for (int a = 0; a < recJTS.length; a++) {
                        recJTS[a].setUserData(record.getRecordNumber());
                        //com.vividsolutions.jts.geom.Point p1 = (com.vividsolutions.jts.geom.Point)recJTS[a];
                        pointList.add((com.vividsolutions.jts.geom.Point)recJTS[a]);
                        com.vividsolutions.jts.geom.Coordinate c = recJTS[a].getCoordinate();
                    }
                }
                n++;
                if (n >= oneHundredthTotal) {
                    n = 0;
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress++;
                    updateProgress("Reading shapefile data:", progress);
                }
            }

            double NSRange = maxY - minY;
            double EWRange = maxX - minX;
            double NSOffset = NSRange * 0.05;
            double EWOffset = EWRange * 0.05;

            Envelope env = new Envelope(minX - EWOffset, maxX + EWOffset,
                    minY - NSOffset, maxY + NSOffset);


            updateProgress("Creating Voronoi diagram (progress will not be updated):", 0);
            VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
            com.vividsolutions.jts.geom.Geometry geom = factory.buildGeometry(pointList);
            vdb.setSites(geom);
            vdb.setClipEnvelope(env);
            com.vividsolutions.jts.geom.Geometry vd = vdb.getDiagram(factory);


            int numPoints = pointList.size();
            PreparedGeometry[] tests = new PreparedGeometry[numPoints];
            int[] userData = new int[numPoints];
            for (int a = 0; a < numPoints; a++) {
                tests[a] = PreparedGeometryFactory.prepare(geom.getGeometryN(a));
                userData[a] = Integer.parseInt(geom.getGeometryN(a).getUserData().toString());
            }

            Object[][] attributeTableRecords = new Object[reader.getRecordCount()][numFields];
            for (int a = 0; a < reader.getRecordCount(); a++) {
                Object[] rec = reader.nextRecord();
                for (int b = 0; b < numFields - 1; b++) {
                    attributeTableRecords[a][b] = rec[b];
                }
            }

            progress = 0;
            updateProgress("Creating new shapefile:", -1);
            for (int a = 0; a < vd.getNumGeometries(); a++) {
                parentRecNum = 0;
                com.vividsolutions.jts.geom.Geometry g = vd.getGeometryN(a);
                if (g instanceof com.vividsolutions.jts.geom.Polygon) {
                    com.vividsolutions.jts.geom.Polygon p = (com.vividsolutions.jts.geom.Polygon) g;
                    ArrayList<ShapefilePoint> pnts = new ArrayList<ShapefilePoint>();
                    int[] parts = new int[p.getNumInteriorRing() + 1];

                    Coordinate[] buffCoords = p.getExteriorRing().getCoordinates();
                    if (!Topology.isLineClosed(buffCoords)) {
                        System.out.println("Exterior ring not closed.");
                    }
                    if (Topology.isClockwisePolygon(buffCoords)) {
                        for (i = 0; i < buffCoords.length; i++) {
                            pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
                        }
                    } else {
                        for (i = buffCoords.length - 1; i >= 0; i--) {
                            pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
                        }
                    }

                    for (int b = 0; b < p.getNumInteriorRing(); b++) {
                        parts[b + 1] = pnts.size();
                        buffCoords = p.getInteriorRingN(b).getCoordinates();
                        if (!Topology.isLineClosed(buffCoords)) {
                            System.out.println("Interior ring not closed.");
                        }
                        if (Topology.isClockwisePolygon(buffCoords)) {
                            for (i = buffCoords.length - 1; i >= 0; i--) {
                                pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
                            }
                        } else {
                            for (i = 0; i < buffCoords.length; i++) {
                                pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
                            }
                        }
                    }

                    PointsList pl = new PointsList(pnts);

                    whitebox.geospatialfiles.shapefile.Polygon wbPoly = new whitebox.geospatialfiles.shapefile.Polygon(parts, pl.getPointsArray());
                    output.addRecord(wbPoly);

                    // which point is contained within this polygon?
                    for (int m = 0; m < tests.length; m++) {
                        if ((tests[m].within(p))) {
                            parentRecNum = userData[m];
                            break;
                        }
                    }
                    Object[] rowData = attributeTableRecords[parentRecNum - 1];
                    writer.addRecord(rowData);
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    n++;
                    progress = (int) (n * 100.0 / vd.getNumGeometries());
                    updateProgress("Creating new shapefile:", progress);
                }
            }

            output.write();
            writer.write();


            // returning a header file string displays the image.
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

//    // This method is only used during testing.
//    public static void main(String[] args) {
//        args = new String[2];
//        args[0] = "/Users/johnlindsay/Downloads/canvec_023i07_shp/023i07_5_0_HD_1460009_0.shp";
//        args[1] = "/Users/johnlindsay/Downloads/canvec_023i07_shp/tmp3.shp";
//
//        VoronoiDiagram vd = new VoronoiDiagram();
//        vd.setArgs(args);
//        vd.run();
//    }
}