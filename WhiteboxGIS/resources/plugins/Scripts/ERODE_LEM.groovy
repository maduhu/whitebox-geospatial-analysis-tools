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
import java.util.concurrent.Future
import java.util.concurrent.*
import java.util.Date
import java.util.ArrayList
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.WhiteboxRaster
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType
import whitebox.ui.plugin_dialog.ScriptDialog
import whitebox.utilities.StringUtilities
import groovy.transform.CompileStatic

// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
//def name = "ERODE_LEM"
//def descriptiveName = "ERODE Landscape Evolution Model (LEM)"
//def description = "Simulates long-term landscape evolution using the ERODE model."
//def toolboxes = ["TerrainAnalysis"]

public class ERODE_LEM implements ActionListener {
	private WhiteboxPluginHost pluginHost
	private ScriptDialog sd;
	private String descriptiveName
	
	public ERODE_LEM(WhiteboxPluginHost pluginHost, 
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
			def helpFile = "AverageOverlay"
			sd.setHelpFile(helpFile)
		
			// Specifying the source file allows the 'view code' 
			// button on the tool dialog to be displayed.
			def pathSep = File.separator
			def scriptFile = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "Scripts" + pathSep + "AverageOverlay.groovy"
			sd.setSourceFile(scriptFile)
			
			// add some components to the dialog
			sd.addDialogFile("Specify the name of the digital elevation model raster file.", "Input Digital Elevation Model (DEM):", "open", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogFile("Output file", "Output Raster File:", "save", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogDataInput("Number of time steps", "Number of time steps:", "", true, false)
			sd.addDialogDataInput("Time step duration (years)", "Time step duration (years):", "1", true, false)
			sd.addDialogDataInput("Uplift rate (mm / yr)", "Uplift rate (mm / yr):", "", true, false)
			sd.addDialogDataInput("Erodability factor (mm / yr)", "Erodability factor (mm / yr):", "", true, false)
			sd.addDialogDataInput("Average annual rainfall (mm / yr)", "Average annual rainfall (mm / yr):", "", true, false)
			sd.addDialogDataInput("Slope exponent", "Slope exponent:", "", true, false)
			sd.addDialogDataInput("Discharge exponent", "Discharge exponent:", "", true, false)
			sd.addDialogDataInput("Area exponent", "Area exponent:", "", true, false)

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
	  	
		if (args.length != 10) {
			pluginHost.showFeedback("Incorrect number of arguments given to tool.")
			return
		}

		def wd = pluginHost.getWorkingDirectory() 
		double z
		int progress
		int oldProgress = -1
  		
		// read the input parameters
		String demFile = args[0]
		String outputFile = args[1]
		int numTimeSteps = Integer.parseInt(args[2])
		double timeStepDuration = Double.parseDouble(args[3])
		double U = Double.parseDouble(args[4])
		double K = Double.parseDouble(args[5])
		double R = Double.parseDouble(args[6])
		double n = Double.parseDouble(args[7])
		double m = Double.parseDouble(args[8])
		double p = Double.parseDouble(args[9])

		WhiteboxRaster dem = new WhiteboxRaster(demFile, "r")
		int numRows = dem.getNumberRows()
		int numColumns = dem.getNumberColumns()
		double nodata = dem.getNoDataValue()
		
		WhiteboxRaster output = new WhiteboxRaster(outputFile, "rw", 
  		  	  demFile, DataType.FLOAT, nodata)

		for (int row in 0..(numRows - 1)) {
  			for (int col in 0..(numColumns - 1)) {
  				z = dem.getValue(row, col)
  				output.setValue(row, col, z)
  			}
  			progress = (int)(100f * row / numRows)
			if (progress > oldProgress) {
				pluginHost.updateProgress(progress)
				oldProgress = progress
			}
			// check to see if the user has requested a cancellation
			if (pluginHost.isRequestForOperationCancelSet()) {
				pluginHost.showFeedback("Operation cancelled")
				return
			}
  		}

  		dem.close()

		for (int timeStep = 0; timeStep < numTimeSteps; timeStep++) {
			// calculate slope
			def slopeFile = wd + "slope.dep" 
			def zFactor = "1.0" 
			args = [outputFile, slopeFile, zFactor] 
			pluginHost.runPlugin("Slope", args, false)

			def filledDEMFile = wd + "filledDEM.dep" 
			def flatIncrement = "0.001" 
			args = [outputFile, filledDEMFile, flatIncrement]
			pluginHost.runPlugin("FillDepressions", args, false)
		}

		output.addMetadataEntry("Created by the "
                    + descriptiveName + " tool.")
        output.addMetadataEntry("Created on " + new Date())
		output.close()

		// display the output image
		pluginHost.returnData(outputFile)

		// reset the progress bar
		pluginHost.updateProgress(0)
	  } catch (OutOfMemoryError oe) {
            pluginHost.showFeedback("An out-of-memory error has occurred during operation.")
      } catch (Exception e) {
            pluginHost.showFeedback("An error has occurred during operation. See log file for details.")
            pluginHost.logException("Error in " + descriptiveName, e)
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
	def f = new ERODE_LEM(pluginHost, args, descriptiveName)
}
