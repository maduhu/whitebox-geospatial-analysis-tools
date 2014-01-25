/*
 * Copyright (C) 2014 Dr. Ehsan Roshani and Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import java.text.DecimalFormat
import java.io.File
import java.util.Date
import java.util.ArrayList
import java.util.PriorityQueue
import java.util.Arrays
import java.util.Collections
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.ui.plugin_dialog.*
import whitebox.utilities.FileUtilities;
import com.vividsolutions.jts.geom.*
import whitebox.geospatialfiles.WhiteboxRaster
import whitebox.geospatialfiles.WhiteboxRasterBase
import whitebox.geospatialfiles.VectorLayerInfo
import whitebox.geospatialfiles.shapefile.attributes.*
import whitebox.geospatialfiles.shapefile.ShapeFileRecord
import whitebox.utilities.Topology
import groovy.transform.CompileStatic
import whitebox.stats.Kriging
import whitebox.stats.Kriging.*
import whitebox.stats.KrigingPoint


// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
def name = "KrigingInterpolation"
def descriptiveName = "Kriging Interpolation"
def description = "Performs an Kriging interpolation"
def toolboxes = ["Interpolation", "StatisticalTools"]

public class KrigingInterpolation implements ActionListener {
    private WhiteboxPluginHost pluginHost
    private ScriptDialog sd;
    private String descriptiveName
    private String basisFunctionType = ""
    private DialogDataInput txtSill
    private DialogDataInput txtRange
    private DialogDataInput txtNugget
    private Kriging k

	public KrigingInterpolation(WhiteboxPluginHost pluginHost, 
        String[] args, def name, def descriptiveName) {
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
            sd.setHelpFile(name)
		
            // Specifying the source file allows the 'view code' 
            // button on the tool dialog to be displayed.
            def pathSep = File.separator
            def scriptFile = pluginHost.getResourcesDirectory() + "plugins" + pathSep + "Scripts" + pathSep + name + ".groovy"
            sd.setSourceFile(scriptFile)
			
            // add some components to the dialog
        	DialogFieldSelector dfs = sd.addDialogFieldSelector("Input file and Value field.", "Input Value Field:", false)															//0
			DialogFile dfOutput = sd.addDialogFile("Output file", "Output Raster File:", "saveAs", "Whitebox Raster Files (*.dep), DEP", true, false)								//1
			
			DialogCheckBox chxError = sd.addDialogCheckBox("Saves the Kriging Variance Error Map", "Save Variance Error Raster", false)												//2
			DialogFile dfError = sd.addDialogFile("Error Output file", "Output Raster File:", "saveAs", "Whitebox Raster Files (*.dep), DEP", true, false)							//3
			dfError.visible = false
			
			DialogDataInput txtCellSize = sd.addDialogDataInput("Output raster cell size.", "Cell Size:", "", true, false)														//4

			def btn = sd.addDialogButton("Calculate Semivariogram", "Center")
			
			DialogComboBox cboxSVType = sd.addDialogComboBox("Enter Semivariogram Type", "Semivariogram Type:", ["Gaussian Model", "Exponential Model", "Spherical Model"], 0)	//5
			DialogDataInput txtNlage = sd.addDialogDataInput("Number of Lags.", "Number of Lags:", "12", true, false)																//6
			DialogDataInput txtLagSize = sd.addDialogDataInput("Lag Size.", "Lag Size:", "10", true, false)																			//7
			DialogDataInput txtNNeighbor = sd.addDialogDataInput("Enter number of neighbors.", "Number of Neighbors:", "5", true, false)											//8
			
			DialogCheckBox chxNugget = sd.addDialogCheckBox("Apply Nugget in the theoritical semivariogram calculation ", "Apply Nugget:", false)									//9
			txtNugget = sd.addDialogDataInput("Nugget.", "Nugget:", "", true, false)																						//10
			txtSill = sd.addDialogDataInput("Sill.", "Sill:", "", true, false)																							//11
			txtRange = sd.addDialogDataInput("Range.", "Range:", "", true, false)																							//12
			DialogCheckBox chxCurve = sd.addDialogCheckBox("Show semivariogram curve", "Show Semivariogram Curve:", true)															//13
			DialogCheckBox chxMap = sd.addDialogCheckBox("Show semivariogram map", "Show Semivariogram Map:", true)																//14

			DialogCheckBox chxAnIsotropic = sd.addDialogCheckBox("Is the data Anisotropic", "Use Anisotropic Model:", false)														//15
			DialogDataInput txtAngle = sd.addDialogDataInput("Angle (rad).", "Angle (Rad):", "0", true, false)																		//16
			DialogDataInput txtTolerance = sd.addDialogDataInput("Tolerance.", "Tolerance (Rad):", "1.5707", true, false)															//17
			DialogDataInput txtBandWidth = sd.addDialogDataInput("Band Width.", "Band Width:", "5", true, false)																	//18
			txtAngle.visible = false
			txtTolerance.visible = false
			txtBandWidth.visible = false

			DialogDataInput txtNorth = sd.addDialogDataInput("North.", "North:", "", true, false)																			//19
			DialogDataInput txtSouth = sd.addDialogDataInput("South.", "South:", "", true, false)																			//20
			DialogDataInput txtEast = sd.addDialogDataInput("East.", "East:", "", true, false)																			//21
			DialogDataInput txtWest = sd.addDialogDataInput("West.", "West:", "", true, false)																			//22

			
			btn.addActionListener(new ActionListener() {
 	            public void actionPerformed(ActionEvent e)
	            {
				    k = new Kriging()

					def progressListener = { evt -> if (evt.getPropertyName().equals("progress")) { 
		            		int progress = (int)evt.getNewValue()
		            		pluginHost.updateProgress("Interpolating Data", progress)
		            	} 
		            } as PropertyChangeListener
		            k.addPropertyChangeListener(progressListener)
				    
					if (Boolean.parseBoolean(chxNugget.getValue())) {
						k.ConsiderNugget = true;
					} else {
						k.ConsiderNugget = false;
					}

					String[] inputData = dfs.getValue().split(";")
					
					if ((dfs.getValue()).length()>2) {
						k.Points  =  k.ReadPointFile(inputData[0],inputData[1]);
					}
			        
			        k.LagSize =  (txtLagSize.getValue()).toDouble()
			        if (Boolean.parseBoolean(chxAnIsotropic.getValue())) {
						k.Anisotropic = true;
						k.Angle = Double.parseDouble(txtAngle.getValue())
						k.BandWidth = Double.parseDouble(txtBandWidth.getValue())
						k.Tolerance = Double.parseDouble(txtTolerance.getValue())
					} else {
						k.Anisotropic = false;
					}
			
			        int numLags = Integer.parseInt(txtNlage.getValue())
			        boolean anisotropic = Boolean.parseBoolean(chxAnIsotropic.getValue())

			        
			        
			        Kriging.Variogram var 
			        if ((cboxSVType.getValue()).toString() == "Gaussian Model") {
						var = k.Semivariogram(Kriging.SemivariogramType.Gaussian, 1d, numLags , anisotropic, true);
					} else if ((cboxSVType.getValue()).toString() == "Exponential Model") {
						
						var = k.Semivariogram(Kriging.SemivariogramType.Exponential, 1d, numLags , anisotropic, true);
					} else {
						var = k.Semivariogram(Kriging.SemivariogramType.Spherical, 1d, numLags , anisotropic, true);
					}
			        
					txtSill.setValue((var.Sill).toString())
					txtRange.setValue((var.Range).toString())
					txtNugget.setValue((var.Nugget).toString())
					if (Boolean.parseBoolean(chxCurve.getValue()))
					{
			        	k.DrawSemivariogram(k.bins, var);
					}
					
			        if(Boolean.parseBoolean(chxMap.getValue()))
					{        		
						k.calcBinSurface(k.SVType,  1,k.NumberOfLags, Boolean.parseBoolean(chxAnIsotropic.getValue()));
						k.DrawSemivariogramSurface(k.LagSize*(k.NumberOfLags),  Boolean.parseBoolean(chxAnIsotropic.getValue()));
					}			
					
	            }
        	});      
            

            

			//Listener for chxError            
            def lstrError = { evt -> if (evt.getPropertyName().equals("value")) { 
            		String value = chxError.getValue()
            		if (!value.isEmpty()&& value != null) { 
            			if ( chxError.getValue() == "true") {
            				dfError.visible = true
		            	} else {
		            		dfError.visible = false
		            	}
            		}
            	} 
            } as PropertyChangeListener
            chxError.addPropertyChangeListener(lstrError)


			//Listener for chxError            
            def lstrAnIso = { evt -> if (evt.getPropertyName().equals("value")) { 
            		String value = chxAnIsotropic.getValue()
            		if (!value.isEmpty()&& value != null) { 
            			if ( chxAnIsotropic.getValue() == "true") {
							txtAngle.visible = true
							txtTolerance.visible = true
							txtBandWidth.visible = true
		            	} else {
							txtAngle.visible = false
							txtTolerance.visible = false
							txtBandWidth.visible = false
		            	}
            		}
            	} 
            } as PropertyChangeListener
            chxAnIsotropic.addPropertyChangeListener(lstrAnIso)


            
            //Listener for dfs, It updates the lag size and boundaries 
            def lsnFeild = { evt -> if (evt.getPropertyName().equals("value")) { 
            		
            		String value = dfs.getValue()
            		if (value != null&& !value.isEmpty()) { 
            			value = value.trim()
            			String[] strArray = dfs.getValue().split(";")
            			String fileName = strArray[0]
            			File file = new File(fileName)
            			ShapeFile shapefile = new ShapeFile(fileName)
						txtNorth.setValue((shapefile.getyMax()).toString())
						txtSouth.setValue((shapefile.getyMin()).toString())
						txtEast.setValue((shapefile.getxMax()).toString())
						txtWest.setValue((shapefile.getxMin()).toString())

						double difY = shapefile.getyMax()-shapefile.getyMin()
						double difX = shapefile.getxMax()-shapefile.getxMin()
						double maxL
						if(difY>= difX){
							maxL = difY
						}
						else{
							maxL = difX
						}
						txtLagSize.setValue((maxL/(txtNlage.getValue()).toDouble()).toString())
            		}
            	} 
            } as PropertyChangeListener
            dfs.addPropertyChangeListener(lsnFeild)
            
            // resize the dialog to the standard size and display it
            //sd.setSize(800, 400)
            sd.visible = true
        }
    }

    @CompileStatic
    private void execute(String[] args) {
		try {
			pluginHost.updateProgress("Interpolating Data...", 0)

	        boolean anisotropic = Boolean.parseBoolean(args[15])
	        
	        Kriging.Variogram var 
			if (args[5] == "Gaussian Model") {
				var = k.Semivariogram(Kriging.SemivariogramType.Gaussian, Double.parseDouble(txtRange.getValue()), Double.parseDouble(txtSill.getValue()),Double.parseDouble(txtNugget.getValue()), anisotropic)
			} else if(args[5] == "Exponential Model") {
				var = k.Semivariogram(Kriging.SemivariogramType.Exponential, Double.parseDouble(txtRange.getValue()), Double.parseDouble(txtSill.getValue()),Double.parseDouble(txtNugget.getValue()), anisotropic)
			} else {
				var = k.Semivariogram(Kriging.SemivariogramType.Spherical, Double.parseDouble(txtRange.getValue()), Double.parseDouble(txtSill.getValue()),Double.parseDouble(txtNugget.getValue()), anisotropic)
			}

	        k.resolution = Double.parseDouble(args[4]);
	        k.bMinX = Double.parseDouble(args[22]) + k.resolution / 2.0;
	        k.bMaxX = Double.parseDouble(args[21]) - k.resolution / 2.0;
	        k.bMinY = Double.parseDouble(args[20]) + k.resolution / 2.0;
	        k.bMaxY = Double.parseDouble(args[19]) - k.resolution / 2.0;
	
	        List<KrigingPoint> outPnts = k.calcInterpolationPoints();
	        
	        outPnts = k.InterpolatePoints(var, outPnts, Integer.parseInt(args[8]));
	        k.buildRaster(args[1], outPnts,false);
	        if (Boolean.parseBoolean(args[2])) {
				k.buildRaster(args[3], outPnts,true);
			}
			
			if (Boolean.parseBoolean(args[13])) {
	        	k.DrawSemivariogram(k.bins, var);
			}
			
	        if(Boolean.parseBoolean(args[14])) {        		
				k.calcBinSurface(k.SVType,  1,k.NumberOfLags, Boolean.parseBoolean(args[15]));
				k.DrawSemivariogramSurface(k.LagSize*(k.NumberOfLags),  Boolean.parseBoolean(args[15]));
			}			
			pluginHost.returnData(args[1])
			if (Boolean.parseBoolean(args[2])) {
				pluginHost.returnData(args[3])
			}
			
		} catch (Exception e) {
			pluginHost.showFeedback("An error has occurred during operation. See log file for details.")
	        pluginHost.logException("Error in " + descriptiveName, e)
		} finally {
			pluginHost.updateProgress("Progress", 0)
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
    def f = new KrigingInterpolation(pluginHost, args, name, descriptiveName)
}
