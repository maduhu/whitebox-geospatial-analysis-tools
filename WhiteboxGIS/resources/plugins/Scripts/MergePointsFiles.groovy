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
import java.lang.Math.*
import java.util.concurrent.Future
import java.util.concurrent.*
import java.util.Date
import java.util.ArrayList
import java.util.Map
import java.util.Random
import jopensurf.*;
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.geospatialfiles.shapefile.attributes.*
import whitebox.ui.plugin_dialog.ScriptDialog
import whitebox.utilities.StringUtilities
import whitebox.structures.XYPoint
import groovy.transform.CompileStatic

// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
def name = "MergePointsFiles"
def descriptiveName = "Merge Points Files"
def description = "Combines multiple shapefiles of points"
def toolboxes = ["VectorTools"]

public class EstimateHeightsFromParallax implements ActionListener {
	private WhiteboxPluginHost pluginHost
	private ScriptDialog sd;
	private String descriptiveName
	
	public EstimateHeightsFromParallax(WhiteboxPluginHost pluginHost, 
		String[] args, def descriptiveName) {
		this.pluginHost = pluginHost
		this.descriptiveName = descriptiveName
			
		if (args.length > 0) {
			final Runnable r = new Runnable() {
            	@Override
            	public void run() {
                	execute(args)
            	}
        	}
        	final Thread t = new Thread(r)
        	t.start()
		} else {
			// Create a dialog for this tool to collect user-specified
			// tool parameters.
			sd = new ScriptDialog(pluginHost, descriptiveName, this)	
		
			// Specifying the help file will display the html help
			// file in the help pane. This file should be be located 
			// in the help directory and have the same name as the 
			// class, with an html extension.
			def helpFile = "EstimateHeightsFromParallax"
			sd.setHelpFile(helpFile)
		
			// Specifying the source file allows the 'view code' 
			// button on the tool dialog to be displayed.
			def pathSep = File.separator
			def scriptFile = pluginHost.getResourcesDirectory() + 
			 "plugins" + pathSep + "Scripts" + pathSep + 
			   "EstimateHeightsFromParallax.groovy"
			sd.setSourceFile(scriptFile)
			
			// add some components to the dialog
			sd.addDialogMultiFile("Select input vector files", "Input Vector Point Files:", "Vector Files (*.shp), SHP")
			sd.addDialogFile("Output file", "Output Vector File:", "close", "Vector Files (*.shp), SHP", true, false)
			
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
	  	
			if (args.length != 2) {
				pluginHost.showFeedback("Incorrect number of arguments given to tool.")
				return
			}

			int progress, oldProgress
			double x, y
			whitebox.geospatialfiles.shapefile.Point wbGeometry
			
			String inputFileString = args[0]
			String outputFile = args[1]

			String[] inputFiles = inputFileString.split(";")

			DBFField[] fields = new DBFField[1];

            fields[0] = new DBFField();
            fields[0].setName("FID");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setFieldLength(10);
            fields[0].setDecimalCount(0);
            
            ShapeFile output = new ShapeFile(outputFile, ShapeType.POINT, fields);

			int fileNum = 0
			for (String inputFile : inputFiles) {
				if (!inputFile.isEmpty()) {
					ShapeFile input = new ShapeFile(inputFile)
					if (input.getShapeType().getBaseType() != ShapeType.POINT) {
						pluginHost.showFeedback("ERROR: One of the input files is not of a POINT ShapeType.")
						return
					}
	            	double[][] point
	            	for (ShapeFileRecord record : input.records) {
	            		
						point = record.getGeometry().getPoints()
						for (int p = 0; p < point.length; p++) {
							x = point[p][0]
							y = point[p][1]

							wbGeometry = new whitebox.geospatialfiles.shapefile.Point(x, y);                  
                			Object[] rowData = new Object[1]
                			rowData[0] = new Double(1)
                			output.addRecord(wbGeometry, rowData);
						}
					}
				}
				fileNum++
				progress = (int)(100f * fileNum / inputFiles.length)
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
			
	  	} catch (Exception e) {
			pluginHost.showFeedback("An error has occurred during operation. See log file for details.")
			pluginHost.logException("Error in " + descriptiveName, e)
			return
	  	} finally {
	  		// reset the progress bar
			pluginHost.updateProgress("Progress:", 0)
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
	def f = new EstimateHeightsFromParallax(pluginHost, args, descriptiveName)
}
