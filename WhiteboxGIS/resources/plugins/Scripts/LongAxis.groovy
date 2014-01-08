/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
 
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.io.File
import java.util.Date
import java.util.ArrayList
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.geospatialfiles.shapefile.attributes.DBFField
import whitebox.ui.plugin_dialog.ScriptDialog
import whitebox.utilities.FileUtilities;
import groovy.transform.CompileStatic

// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
def name = "LongAxis"
def descriptiveName = "Long Axis"
def description = "Maps the long axes of a group of polygons"
def toolboxes = ["VectorTools"]

public class LongAxis implements ActionListener {
    private WhiteboxPluginHost pluginHost
    private ScriptDialog sd;
    private String descriptiveName
	
    public LongAxis(WhiteboxPluginHost pluginHost, 
        String[] args, def descriptiveName) {
        this.pluginHost = pluginHost
        this.descriptiveName = descriptiveName
			
        if (args.length > 0) {
            execute(args)
        } else {
            // Create a dialog for this tool to collect user-specified
            // tool parameters.
            sd = new ScriptDialog(pluginHost, descriptiveName, this)	
		
            // Specifying the help file will display the html help
            // file in the help pane. This file should be be located 
            // in the help directory and have the same name as the 
            // class, with an html extension.
            def helpFile = "LongAxis"
            sd.setHelpFile(helpFile)
		
            // Specifying the source file allows the 'view code' 
            // button on the tool dialog to be displayed.
            def pathSep = File.separator
            def scriptFile = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "Scripts" + pathSep + "LongAxis.groovy"
            sd.setSourceFile(scriptFile)
			
            // add some components to the dialog
            sd.addDialogFile("Input polygon file", "Input Vector Polygon File:", "open", "Vector Files (*.shp), SHP", true, false)
            sd.addDialogFile("Output file", "Output Vector File:", "saveAs", "Vector Files (*.shp), SHP", true, false)
            
            // resize the dialog to the standard size and display it
            sd.setSize(800, 400)
            sd.visible = true
        }
    }

    // The CompileStatic annotation can be used to significantly
    // improve the performance of a Groovy script to nearly 
    // that of native Java code.
    @CompileStatic
    private void execute(String[] args) {
        try {
            int i, progress, oldProgress, numPoints, numParts, n
            int part, startingPointInPart, endingPointInPart
            double x, y, midX, midY;
            double[] axes = new double[2];
            ShapefileRecordData recordData;
	  		double[][] geometry
	  		int[] partData
	  		int incrementVal = 2
	  		double slope;
        	double boxCentreX, boxCentreY;
        	double psi = 0;
			double DegreeToRad = Math.PI / 180.0
			double RadToDegree = 180.0 / Math.PI
			double[] newBoundingBox = new double[4];
			double newXAxis = 0;
        	double newYAxis = 0;
        	double longAxis;
       		double shortAxis;
       		final double rightAngle = Math.toRadians(90);
       		ShapeType outputShapeType = ShapeType.POLYLINE;
       		int[] parts = [0];
			
            if (args.length != 2) {
                pluginHost.showFeedback("Incorrect number of arguments given to tool.")
                return
            }
            // read the input parameters
            String inputFile = args[0]
            String outputFile = args[1]
            
            def input = new ShapeFile(inputFile)

            // make sure that input is of a POLYGON base shapetype
            ShapeType shapeType = input.getShapeType()
            if (shapeType.getBaseType() != ShapeType.POLYGON) {
                pluginHost.showFeedback("Input shapefile must be of a POLYLINE base shapetype.")
                return
            }

			int numFeatures = input.getNumberOfRecords()
			
            DBFField[] fields = new DBFField[3];

            fields[0] = new DBFField();
            fields[0].setName("PARENT_ID");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setFieldLength(10);
            fields[0].setDecimalCount(0);

            fields[1] = new DBFField();
            fields[1].setName("LENGTH");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setFieldLength(10);
            fields[1].setDecimalCount(3);

            fields[2] = new DBFField();
            fields[2].setName("ORIENT");
            fields[2].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[2].setFieldLength(10);
            fields[2].setDecimalCount(3);

            ShapeFile output = new ShapeFile(outputFile, outputShapeType, fields);
			
            int featureNum = 0;
            oldProgress = -1;
            for (ShapeFileRecord record : input.records) {
            	featureNum++;
                int recordNum = record.getRecordNumber();
                double[][] vertices = record.getGeometry().getPoints();
                int numVertices = vertices.length;
                double east = Double.NEGATIVE_INFINITY;
                double west = Double.POSITIVE_INFINITY;
                double north = Double.NEGATIVE_INFINITY;
                double south = Double.POSITIVE_INFINITY;
                double[][] axesEndPoints = new double[2][2];

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

                Object[] rowData = new Object[3];
                rowData[0] = new Double(recordNum);
                rowData[1] = new Double(longAxis);
                double slopeDeg = (Math.atan(slope) * RadToDegree);
                if (slopeDeg < 0) {
                    slopeDeg = 90 + -1 * slopeDeg;
                } else {
                    slopeDeg = 90 - slopeDeg;
                }
                rowData[2] = new Double(slopeDeg);
                
                Geometry poly;
                poly = new PolyLine(parts, axesEndPoints);
                output.addRecord(poly, rowData);

                progress = (int)(100f * featureNum / (numFeatures - 1))
            	if (progress != oldProgress) {
					pluginHost.updateProgress(progress)
            		oldProgress = progress
            	}
            	// check to see if the user has requested a cancellation
				if (pluginHost.isRequestForOperationCancelSet()) {
					pluginHost.showFeedback("Operation cancelled")
					return
				}
            }
            
            output.write();
            
            // display the output image
            pluginHost.returnData(outputFile)
        } catch (OutOfMemoryError oe) {
            pluginHost.showFeedback("An out-of-memory error has occurred during operation.")
	    } catch (Exception e) {
	        pluginHost.showFeedback("An error has occurred during operation. See log file for details.")
	        pluginHost.logException("Error in " + descriptiveName, e)
        } finally {
        	// reset the progress bar
        	pluginHost.updateProgress(0)
        }
    }

    
	@CompileStatic
    private ShapefileRecordData getXYFromShapefileRecord(ShapeFileRecord record) {
        int[] partData;
        double[][] points;
        ShapeType shapeType = record.getShapeType();
        switch (shapeType) {
            case ShapeType.POLYLINE:
                whitebox.geospatialfiles.shapefile.PolyLine recPolyLine =
                        (whitebox.geospatialfiles.shapefile.PolyLine) (record.getGeometry());
                points = recPolyLine.getPoints();
                partData = recPolyLine.getParts();
                break;
            case ShapeType.POLYLINEZ:
                PolyLineZ recPolyLineZ = (PolyLineZ) (record.getGeometry());
                points = recPolyLineZ.getPoints();
                partData = recPolyLineZ.getParts();
                break;
            case ShapeType.POLYLINEM:
                PolyLineM recPolyLineM = (PolyLineM) (record.getGeometry());
                points = recPolyLineM.getPoints();
                partData = recPolyLineM.getParts();
                break;
            default: // should never hit this.
                points = new double[1][2];
                points[1][0] = -1;
                points[1][1] = -1;
                break;
        }
        ShapefileRecordData ret = new ShapefileRecordData(points, partData)
        return ret;
    }

	@CompileStatic
    class ShapefileRecordData {
    	private final double[][] points
    	private final int[] parts
    	ShapefileRecordData(double[][] points, int[] parts) {
    		this.points = points
    		this.parts = parts
    	}

		double[][] getPoints() {
			return points
		}

		int[] getParts() {
			return parts
		}
    	
    }

    @Override
    public void actionPerformed(ActionEvent event) {
    	if (event.getActionCommand().equals("ok")) {
            final def args = sd.collectParameters()
            sd.dispose()
            final Runnable r = new Runnable() {
            	@Override
            	public void run() {
                    execute(args)
            	}
            }
            final Thread t = new Thread(r)
            t.start()
    	}
    }
}

if (args == null) {
    pluginHost.showFeedback("Plugin arguments not set.")
} else {
    def f = new LongAxis(pluginHost, args, descriptiveName)
}
