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

import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.util.concurrent.Future
import java.util.concurrent.*
import java.util.Date
import java.util.ArrayList

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.WhiteboxRaster
import whitebox.geospatialfiles.WhiteboxRasterInfo
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType
import whitebox.geospatialfiles.WhiteboxRasterBase.DataScale
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.ui.plugin_dialog.ScriptDialog
import whitebox.utilities.StringUtilities
import groovy.transform.CompileStatic

// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
def name = "ConditionalEvaluation"
def descriptiveName = "Conditional Evaluation"
def description = "Performs a conditional evaluaton (if-then-else) operation on a raster."
def toolboxes = ["MathTools", "GISTools"]

public class ConditionalEvaluation implements ActionListener {
	private WhiteboxPluginHost pluginHost
	private ScriptDialog sd
	private String descriptiveName
	private String name
	
	public ConditionalEvaluation(WhiteboxPluginHost pluginHost, 
		String[] args, def name, def descriptiveName) {
		this.pluginHost = pluginHost
		this.name = name
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
			sd.setHelpFile(name)
		
			// Specifying the source file allows the 'view code' 
			// button on the tool dialog to be displayed.
			def pathSep = File.separator
			def scriptFile = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "Scripts" + pathSep + name + ".groovy"
			sd.setSourceFile(scriptFile)
			
			// add some components to the dialog
			sd.addDialogFile("Input raster file", "Input Raster:", "open", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogDataInput("<html>Conditional statement.\nThe <i>conditional statement</i> can be any valid Groovy statement, e.g. value > 35.0.</html>", "Conditional Statement e.g. value > 35.0:", "", false, false)
            sd.addDialogFile("Value  where TRUE (input raster or constant value)", "Value Where TRUE (Raster File Or Constant Value):", "open", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogFile("Value  where FALSE (input raster or constant value). If this parameter is not specified, NoData will be used.", "Value Where FALSE (Raster File Or Constant Value):", "open", "Raster Files (*.dep), DEP", true, true)
			sd.addDialogFile("Output raster file", "Output Raster File:", "save", "Raster Files (*.dep), DEP", true, false)
            
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
			double value, trueValue = 0, falseValue = 0
			boolean ret
			int progress, oldProgress
			String trueValueFile = ""
			String falseValueFile = ""
			boolean trueValueConstant = true
			boolean falseValueConstant = true
			
			if (args.length < 4) {
				pluginHost.showFeedback("Incorrect number of arguments given to tool.")
				return
			}
			
			// read the input parameters

			// the input file
			String inputFile = args[0]
			WhiteboxRaster image = new WhiteboxRaster(inputFile, "r")
			double nodata = image.getNoDataValue()
			int rows = image.getNumberRows()
			int cols = image.getNumberColumns()

			// the conditional statement
			String conStatement = args[1]
			boolean hasConStatement = !(conStatement.isEmpty() | conStatement.toLowerCase().equals("not specified"))

			// the TRUE value
			def file = new File(args[2])
			trueValueConstant = !(file).exists()
			
			if (trueValueConstant) {
				trueValue = Double.parseDouble(file.getName().replace(".dep", ""))
			} else {
				trueValueFile = args[2]
			}
			
			// the FALSE value
			if (args[3] != null && !args[3].isEmpty() && !(args[3].toLowerCase().equals("not specified"))) {
				file = new File(args[3])
				falseValueConstant = !(file).exists()
				if (falseValueConstant) {
					falseValue = Double.parseDouble(file.getName().replace(".dep", ""))
				} else {
					falseValueFile = args[3]
				}
			}

			// the output file
			String outputFile = args[4]
			
			
			if (args[3] == null || args[3].isEmpty() || args[3].toLowerCase().equals("not specified")) {
				falseValue = nodata
			}

			WhiteboxRaster trueValueRaster
			if (!trueValueConstant) {
				trueValueRaster = new WhiteboxRaster(trueValueFile, "r")
				if (trueValueRaster.getNumberRows() != rows || trueValueRaster.getNumberColumns() != cols) {
					pluginHost.showFeedback("The input images must all have the same dimensions \n(i.e. number of rows and columns and spatial extent)")
					return
				}
			}

			WhiteboxRaster falseValueRaster
			if (!falseValueConstant) {
				falseValueRaster = new WhiteboxRaster(falseValueFile, "r")
				if (falseValueRaster.getNumberRows() != rows || falseValueRaster.getNumberColumns() != cols) {
					pluginHost.showFeedback("The input images must all have the same dimensions \n(i.e. number of rows and columns and spatial extent)")
					return
				}
			}

			WhiteboxRaster output = new WhiteboxRaster(outputFile, "rw", 
  		  	     inputFile, DataType.FLOAT, nodata)
  		  	output.setNoDataValue(nodata)
			if (trueValueConstant && falseValueConstant) {
				output.setPreferredPalette("qual.plt")
				output.setDataScale(DataScale.CATEGORICAL)
			} else if (!trueValueConstant) {
				output.setPreferredPalette(trueValueRaster.getPreferredPalette())
				output.setDataScale(trueValueRaster.getDataScale())
			} else {
				output.setPreferredPalette(falseValueRaster.getPreferredPalette())
				output.setDataScale(falseValueRaster.getDataScale())
			}

			ScriptEngineManager mgr = new ScriptEngineManager();
   			ScriptEngine engine = mgr.getEngineByName("groovy");
   			String expression = conStatement.replace("\"Value\"", "value").replace("\'Value\'", "value").replace("\"value\"", "value").replace("\'value\'", "value").replace("VALUE", "value").replace("NoData", "nodata").replace("NODATA", "nodata").replace("Null", "nodata").replace("NULL", "nodata").replace("null", "nodata").replace(";", "");
   			boolean expressionEvaluatesNoData = expression.contains("nodata")
   			String myScript = "for (int i = 0; i < $cols; i++) { double value = inData[i]; if (value != nodata || expressionEvaluatesNoData) { if ($expression) { outData[i] = trueData[i]; } else { outData[i] = falseData[i]; } } else { outData[i] = nodata; } }"
   			//String myScript = "for i in xrange(0, $cols):\n\tvalue = inData[i]\n\tif (value != nodata) | expressionEvaluatesNoData:\n\t\tif $expression:\n\t\t\toutData[i] = trueData[i]\n\t\telse:\n\t\t\toutData[i] = falseData[i]\n\telse:\n\t\toutData[i] = nodata\n"
   			//pluginHost.showFeedback(myScript)
   			CompiledScript generate_data = ((Compilable) engine).compile(myScript);
   			double oldValue = nodata
   			boolean oldReturn
			Bindings bindings = engine.createBindings();

			double[] outData = new double[cols];
			double[] trueData = new double[cols];
			double[] falseData = new double[cols];
			if (trueValueConstant) {
				Arrays.fill(trueData, trueValue)
				bindings.put("trueData", trueData);
			}
			if (falseValueConstant) {
				Arrays.fill(falseData, falseValue)
				bindings.put("falseData", falseData);
			}
			bindings.put("outData", outData);
			bindings.put("nodata", nodata);
			bindings.put("expressionEvaluatesNoData", expressionEvaluatesNoData);
			
			for (int row = 0; row < rows; row++) {
				double[] inData = image.getRowValues(row);
				if (!trueValueConstant) {
					trueData = trueValueRaster.getRowValues(row)
					bindings.put("trueData", trueData);
				}
				if (!falseValueConstant) {
					falseData = falseValueRaster.getRowValues(row)
					bindings.put("falseData", falseData);
				}
				bindings.put("inData", inData);
				generate_data.eval(bindings)
				
				output.setRowValues(row, outData)
  				progress = (int)(100f * row / rows)
				if (progress != oldProgress) {
					pluginHost.updateProgress(progress)
					oldProgress = progress
					// check to see if the user has requested a cancellation
					if (pluginHost.isRequestForOperationCancelSet()) {
						pluginHost.showFeedback("Operation cancelled")
						return
					}
				}
				
			}
			
			image.close()
			
			output.addMetadataEntry("Created by the "
	                    + descriptiveName + " tool.")
	        output.addMetadataEntry("Created on " + new Date())
			output.close()
	
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

	class PixelValue {
		double value
		
		public PixelValue(double value) {
			this.value = value
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
	def f = new ConditionalEvaluation(pluginHost, args, name, descriptiveName)
}
