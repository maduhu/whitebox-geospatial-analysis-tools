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
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Date
import java.util.ArrayList
import java.util.Queue
import java.util.LinkedList
import java.util.ArrayDeque
import java.text.DecimalFormat
import java.util.stream.IntStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import whitebox.interfaces.WhiteboxPluginHost
import whitebox.geospatialfiles.WhiteboxRaster
import whitebox.geospatialfiles.WhiteboxRasterInfo
import whitebox.geospatialfiles.WhiteboxRasterBase.DataType
import whitebox.geospatialfiles.WhiteboxRasterBase.DataScale
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.ui.plugin_dialog.*
import whitebox.utilities.StringUtilities
import groovy.transform.CompileStatic
import groovy.time.TimeDuration
import groovy.time.TimeCategory

// The following four variables are required for this 
// script to be integrated into the tool tree panel. 
// Comment them out if you want to remove the script.
def name = "ImpoundmentIndex"
def descriptiveName = "Impoundment Index"
def description = "Calculates the impoundment size resulting from damming a DEM."
def toolboxes = ["HydroTools", "FlowpathTAs", "WetlandTools"]

public class ImpoundmentIndex implements ActionListener {
	private WhiteboxPluginHost pluginHost
	private ScriptDialog sd;
	private String descriptiveName

	private AtomicInteger numSolved = new AtomicInteger(0)
	private AtomicInteger curProgress = new AtomicInteger(-1)
	
	public ImpoundmentIndex(WhiteboxPluginHost pluginHost, 
		String[] args, String name, String descriptiveName) {
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
			sd.addDialogFile("Input DEM raster", "Input DEM Raster:", "open", "Raster Files (*.dep), DEP", true, false)
            sd.addDialogFile("Output impoundment index raster file", "Output Impoundment Size Raster File:", "save", "Raster Files (*.dep), DEP", true, false)
			sd.addDialogComboBox("What measure of impoundment size should be output?", "Output impoundment", ["area", "volume"], 0)
			sd.addDialogDataInput("Max. dam height (in z-units)", "Max. dam height (in z-units)", "", true, false)
            sd.addDialogDataInput("Max. dam length (in grid cells)", "Max. dam length (in grid cells)", "", true, false)
            
			DialogCheckBox outDEM = sd.addDialogCheckBox("Output dammed DEM?", "Output dammed DEM?", false)
			DialogDataInput lowerThreshold = sd.addDialogDataInput("Lower threshold", "Lower threshold", "", true, false)
            DialogDataInput upperThreshold = sd.addDialogDataInput("Upper threshold", "Upper threshold", "", true, false)
			lowerThreshold.visible = false
			upperThreshold.visible = false
							
            //Listener for chxError            
            def lstr = { evt -> if (evt.getPropertyName().equals("value")) 
            	{ 
            		String value = outDEM.getValue()
            		if (!value.isEmpty() && value != null) {
            			if (outDEM.getValue() == "true") {
            				lowerThreshold.visible = true
							upperThreshold.visible = true
		            	} else {
		            		lowerThreshold.visible = false
							upperThreshold.visible = false
		            	}
            		}
            	} 
            } as PropertyChangeListener
            outDEM.addPropertyChangeListener(lstr)

			
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

			//Date start = new Date()
			
			int progress, oldProgress, i, row, col, rN, cN, dir, perpDir, n

			/*
			 * 6  7  0
			 * 5  X  1
			 * 4  3  2
			 */
	  		int[] dX = [ 1, 1, 1, 0, -1, -1, -1, 0 ]
			int[] dY = [ -1, 0, 1, 1, 1, 0, -1, -1 ]

			int[] perpendicular = [ 2, 3, 4, 5, 6, 7, 0, 1 ]
			
			
			int[] inflowingVals = [ 4, 5, 6, 7, 0, 1, 2, 3 ]
        	double z, zN, height, outVal
        	DecimalFormat df = new DecimalFormat("###.#")
			
			if (args.length < 4) {
				pluginHost.showFeedback("Incorrect number of arguments given to tool.")
				return
			}
			// read the input parameters
			String inputFile = args[0]
			String outputFile = args[1]
			int mode = 1 // 0 = output volume; 1 = output area
			if (args[2].toLowerCase().contains("vol")) {
				mode = 0
			}
			double maxDamHeight = Double.parseDouble(args[3])
			int damLength = Integer.parseInt(args[4])
			damLength = (int)(Math.floor(damLength / 2))
			boolean createDEM = false
			if (args[5].toLowerCase().contains("t")) {
				createDEM = true
			}
			
			
			// read the input image
			WhiteboxRaster dem = new WhiteboxRaster(inputFile, "r")
			//dem.setForceAllDataInMemory(true)
			double nodata = dem.getNoDataValue()
			int rows = dem.getNumberRows()
			int rowsLessOne = rows - 1
			int cols = dem.getNumberColumns()
			int numPixels = rows * cols
			double gridResX = dem.getCellSizeX();
            double gridResY = dem.getCellSizeY();
            if (dem.getXYUnits().toLowerCase().contains("deg")) {
            	// estimate the cell size 
                double p1 = 111412.84;		// longitude calculation term 1
                double p2 = -93.5;			// longitude calculation term 2
                double p3 = 0.118;			// longitude calculation term 3
                double lat = Math.toRadians((dem.getNorth() - dem.getSouth()) / 2.0);
                double longlen = (p1 * Math.cos(lat)) + (p2 * Math.cos(3 * lat)) + (p3 * Math.cos(5 * lat));

                gridResX = gridResX * longlen;
                gridResY = gridResY * longlen;
            }
            double diagGridRes = Math.sqrt(gridResX * gridResX + gridResY * gridResY);
            double[] gridLengths = [diagGridRes, gridResX, diagGridRes, gridResY, diagGridRes, gridResX, diagGridRes, gridResY]
			double cellArea = gridResX * gridResY
			
            // calculate the flow direction
            byte[][] flowDir = new byte[rows][cols]
        	double[][] allData = new double[rows + 2][cols]
			for (row = -1; row <= rows; row++) {
				 allData[row + 1] = dem.getRowValues(row)
			}
			IntStream.range(0, rows).parallel().forEach{ 
				double[][] data = new double[3][cols]
				data[0] = allData[it] //dem.getRowValues(it - 1)
				data[1] = allData[it + 1] //dem.getRowValues(it)
				data[2] = allData[it + 2] //dem.getRowValues(it + 1)
				calcFlowDirection(it, data, flowDir, gridLengths, nodata) 
			};
			allData = new double[0][0]

			// count the number of inflowing neighbours
			byte[][] numInflow = new byte[rows][cols]
			numSolved.set(-1)
			curProgress.set(0)
			IntStream.range(0, rows).parallel().forEach{ countInflowingCells(it, flowDir, numInflow) };


			// initialize the output image
			pluginHost.updateProgress("Creating output file:", 0)
			WhiteboxRaster output = new WhiteboxRaster(outputFile, "rw", 
  		  	     inputFile, DataType.FLOAT, 0.0)
			//output.setPreferredPalette("blueyellow.pal");
            output.setPreferredPalette("spectrum_white_background.pal");
            output.setDataScale(DataScale.CONTINUOUS);
            output.setZUnits("upslope grid cells");
            output.setNonlinearity(0.2)


			if (createDEM) {
				int damCount = 0
				
				// initialize the output DEM
				String outputDEMfile = outputFile.replace(".dep", "_DEM.dep")
	            WhiteboxRaster outputDEM = new WhiteboxRaster(outputDEMfile, "rw", 
	  		  	     inputFile, DataType.FLOAT, nodata)
				outputDEM.setPreferredPalette(dem.getPreferredPalette());

	            double lowerThreshold = Double.parseDouble(args[6])
	            double upperThreshold = Double.parseDouble(args[7])
	            
				double[][] damHeight = new double[rows][cols]
				UpslopeValues[][] upslopeVals = new UpslopeValues[rows][cols]
				double zLeft, zRight
				oldProgress = -1
				for (row = 0; row < rows; row++) {
					for (col = 0; col < cols; col++) {
						z = dem.getValue(row, col);
						if (z != nodata) {
							outputDEM.setValue(row, col, z)
							
							upslopeVals[row][col] = new UpslopeValues(z, maxDamHeight)
							// what's the flow direction?
							dir = flowDir[row][col]
	
							// what's the perpendicular flow direction?
							perpDir = perpendicular[dir]
	
							// find the elevation at the end of the right traverse
							rN = row
							cN = col
							zRight = z
							for (i = 0; i < damLength; i++) {
								rN += dY[perpDir]
								cN += dX[perpDir]
								zN = dem.getValue(rN, cN)
								if (zN != nodata) {
									zRight = zN
								} else {
									break
								}
							}
	
							// find the elevation at the end of the left traverse
							rN = row
							cN = col
							zLeft = z
							for (i = 0; i < damLength; i++) {
								rN -= dY[perpDir]
								cN -= dX[perpDir]
								zN = dem.getValue(rN, cN)
								if (zN != nodata) {
									zLeft = zN
								} else {
									break
								}
							}
	
							// What is the difference in height between
							// the minimum of zRight and zLeft and z
							height = Math.min(zRight, zLeft) - z
							if (height < 0) { height = 0 }
							if (height > maxDamHeight) { height = maxDamHeight }
							damHeight[row][col] = height
							//output.setValue(row, col, height)
						} else {
							output.setValue(row, col, nodata)
						}
					}
					progress = (int)(100f * row / rowsLessOne)
					if (progress != oldProgress) {
						pluginHost.updateProgress("Calculating Index (1 of 2):", progress)
						oldProgress = progress
	
						// check to see if the user has requested a cancellation
						if (pluginHost.isRequestForOperationCancelSet()) {
							pluginHost.showFeedback("Operation cancelled")
							return
						}
					}
				}
	
				// close the DEM, it won't be needed any further.
				dem.close()
	
				// Now perform the accumulation operation.
				int r, c
				boolean flag, createdDam
				double targetHeight
				ArrayList<Double> vals
				for (row = 0; row < rows; row++) {
					for (col = 0; col < cols; col++) {
						if (numInflow[row][col] == 0) {
							r = row
							c = col
							numInflow[r][c] = -1
							flag = true
							while (flag) {
								height = damHeight[r][c]
								vals = upslopeVals[r][c].getValues()
								if (mode == 0) {
									outVal = upslopeVals[r][c].volumeBelow(height, cellArea)
								} else { // area
									outVal = upslopeVals[r][c].numLessThan(height) * cellArea
								}
								upslopeVals[r][c].values = null
								upslopeVals[r][c] = null
								output.setValue(r, c, outVal)
								
								dir = flowDir[r][c]

								createdDam = false
								
								if (outVal >= lowerThreshold && outVal <= upperThreshold) {
									// create the dam in the output DEM
									createdDam = true
									damCount++
									
									z = outputDEM.getValue(r, c)
									targetHeight = z + height
									
									// what's the perpendicular flow direction?
									perpDir = perpendicular[dir]
			
									// find the elevation at the end of the right traverse
									rN = r
									cN = c
									for (i = 0; i < damLength; i++) {
										rN += dY[perpDir]
										cN += dX[perpDir]
										zN = outputDEM.getValue(rN, cN)
										if (zN != nodata) {
											if (zN < targetHeight) {
												outputDEM.setValue(rN, cN, targetHeight)
											}
										} else {
											break
										}
									}
			
									// find the elevation at the end of the left traverse
									rN = r
									cN = c
									for (i = 0; i < damLength; i++) {
										rN -= dY[perpDir]
										cN -= dX[perpDir]
										zN = outputDEM.getValue(rN, cN)
										if (zN != nodata) {
											if (zN < targetHeight) {
												outputDEM.setValue(rN, cN, targetHeight)
											}
										} else {
											break
										}
									}

									outputDEM.setValue(r, c, targetHeight)
								}
								
								if (dir >= 0) {
									rN = r + dY[dir]
									cN = c + dX[dir]
									if (flowDir[rN][cN] >= 0) {
										if (!createdDam) {
											upslopeVals[rN][cN].addValues(vals)
										}
										numInflow[rN][cN] -= 1
										if (numInflow[rN][cN] > 0) {
											flag = false
										} else {
											numInflow[rN][cN] = -1
											r = rN
											c = cN
										}
									} else if (flowDir[rN][cN] == -1) {
										flag = false
									}
								} else {
									flag = false
								}
							}
						}
						if (flowDir[row][col] == -2) {
							output.setValue(row, col, nodata)
						}
					}
					progress = (int)(100f * row / rowsLessOne)
					if (progress != oldProgress) {
						pluginHost.updateProgress("Calculating Index (2 of 2):", progress)
						oldProgress = progress
	
						// check to see if the user has requested a cancellation
						if (pluginHost.isRequestForOperationCancelSet()) {
							pluginHost.showFeedback("Operation cancelled")
							return
						}
					}
				}

				pluginHost.showFeedback("$damCount dams were created in the output DEM.")
				
				outputDEM.addMetadataEntry("Created by the "
	                    + descriptiveName + " tool.")
	        	outputDEM.addMetadataEntry("Created on " + new Date())
				outputDEM.close()
				pluginHost.returnData(outputDEMfile)
				
			} else {
				double[][] damHeight = new double[rows][cols]
				UpslopeValues[][] upslopeVals = new UpslopeValues[rows][cols]
				double zLeft, zRight
				oldProgress = -1
				for (row = 0; row < rows; row++) {
					for (col = 0; col < cols; col++) {
						z = dem.getValue(row, col);
						if (z != nodata) {
							upslopeVals[row][col] = new UpslopeValues(z, maxDamHeight)
							// what's the flow direction?
							dir = flowDir[row][col]
	
							// what's the perpendicular flow direction?
							perpDir = perpendicular[dir]
	
							// find the elevation at the end of the right traverse
							rN = row
							cN = col
							zRight = z
							for (i = 0; i < damLength; i++) {
								rN += dY[perpDir]
								cN += dX[perpDir]
								zN = dem.getValue(rN, cN)
								if (zN != nodata) {
									zRight = zN
								} else {
									break
								}
							}
	
							// find the elevation at the end of the left traverse
							rN = row
							cN = col
							zLeft = z
							for (i = 0; i < damLength; i++) {
								rN -= dY[perpDir]
								cN -= dX[perpDir]
								zN = dem.getValue(rN, cN)
								if (zN != nodata) {
									zLeft = zN
								} else {
									break
								}
							}
	
							// What is the difference in height between
							// the minimum of zRight and zLeft and z
							height = Math.min(zRight, zLeft) - z
							if (height < 0) { height = 0 }
							if (height > maxDamHeight) { height = maxDamHeight }
							damHeight[row][col] = height
							//output.setValue(row, col, height)
						} else {
							output.setValue(row, col, nodata)
						}
					}
					progress = (int)(100f * row / rowsLessOne)
					if (progress != oldProgress) {
						pluginHost.updateProgress("Calculating Index (1 of 2):", progress)
						oldProgress = progress
	
						// check to see if the user has requested a cancellation
						if (pluginHost.isRequestForOperationCancelSet()) {
							pluginHost.showFeedback("Operation cancelled")
							return
						}
					}
				}
	
				// close the DEM, it won't be needed any further.
				dem.close()
	
				// Now perform the accumulation operation.
				int r, c
				boolean flag
				ArrayList<Double> vals
				for (row = 0; row < rows; row++) {
					for (col = 0; col < cols; col++) {
						if (numInflow[row][col] == 0) {
							r = row
							c = col
							numInflow[r][c] = -1
							flag = true
							while (flag) {
								height = damHeight[r][c]
								vals = upslopeVals[r][c].getValues()
								if (mode == 0) {
									outVal = upslopeVals[r][c].volumeBelow(height, cellArea)
								} else { // area
									outVal = upslopeVals[r][c].numLessThan(height) * cellArea
								}
								upslopeVals[r][c].values = null
								upslopeVals[r][c] = null
								output.setValue(r, c, outVal)
								
								dir = flowDir[r][c]
								if (dir >= 0) {
									rN = r + dY[dir]
									cN = c + dX[dir]
									if (flowDir[rN][cN] >= 0) {
										upslopeVals[rN][cN].addValues(vals)
										numInflow[rN][cN] -= 1
										if (numInflow[rN][cN] > 0) {
											flag = false
										} else {
											numInflow[rN][cN] = -1
											r = rN
											c = cN
										}
									} else if (flowDir[rN][cN] == -1) {
										flag = false
									}
								} else {
									flag = false
								}
							}
						}
						if (flowDir[row][col] == -2) {
							output.setValue(row, col, nodata)
						}
					}
					progress = (int)(100f * row / rowsLessOne)
					if (progress != oldProgress) {
						pluginHost.updateProgress("Calculating Index (2 of 2):", progress)
						oldProgress = progress
	
						// check to see if the user has requested a cancellation
						if (pluginHost.isRequestForOperationCancelSet()) {
							pluginHost.showFeedback("Operation cancelled")
							return
						}
					}
				}
			}

			output.addMetadataEntry("Created by the "
	                    + descriptiveName + " tool.")
	        output.addMetadataEntry("Created on " + new Date())
			output.close()
			
//			int r, c
//			boolean flag
//			oldProgress = -1
//			for (row = 0; row < rows; row++) {
//				for (col = 0; col < cols; col++) {
//					z = dem.getValue(row ,col);
//					if (z != nodata) {
//						rN = row
//						cN = col
//						flag = true
//						while (flag) {
//							dir = flowDir[rN][cN]
//							if (dir >= 0) {
//								rN += dY[dir]
//								cN += dX[dir]
//								zN = dem.getValue(rN, cN)
//								if ((z - zN) <= maxDamHeight) {
//									output.incrementValue(rN, cN, 1.0)
//								} else {
//									flag = false
//								}
//							} else {
//								flag = false
//							}
//						}
//					} else {
//						output.setValue(row, col, nodata)
//					}
//				}
//				progress = (int)(100f * row / rowsLessOne)
//				if (progress != oldProgress) {
//					pluginHost.updateProgress("Calculating Index:", progress)
//					oldProgress = progress
//
//					// check to see if the user has requested a cancellation
//					if (pluginHost.isRequestForOperationCancelSet()) {
//						pluginHost.showFeedback("Operation cancelled")
//						return
//					}
//				}
//			}

//			Date stop = new Date()
//			TimeDuration td = TimeCategory.minus(stop, start)
//			pluginHost.showFeedback("Elapsed time: $td")

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

	private void calcFlowDirection(int row, double[][] data, byte[][] flowDir, double[] gridLengths, double nodata) {
		// check to see if the user has requested a cancellation
		if (pluginHost.isRequestForOperationCancelSet()) {
			return
		}
		
		int[] dX = [ 1, 1, 1, 0, -1, -1, -1, 0 ]
		int[] dY = [ -1, 0, 1, 1, 1, 0, -1, -1 ]
		int cols = flowDir[0].length
        int rows = flowDir.length
        int dir, cN
		double z, zN, maxSlope, slope
		for (int col = 0; col < cols; col++) {
			z = data[1][col]
			if (z != nodata) {
				dir = -1
				maxSlope = -99999999
				for (int i = 0; i < 8; i++) {
					cN = col + dX[i]
					if (cN >= 0 && cN < cols) {
						zN = data[1 + dY[i]][cN]
						if (zN != nodata) {
							slope = (z - zN) / gridLengths[i]
							if (slope > maxSlope && slope > 0) {
								maxSlope = slope
								dir = i
							}
						}
					}
				}
				flowDir[row][col] = (byte)dir
			} else {
				flowDir[row][col] = (byte)-2
			}
			
		}

		int solved = numSolved.incrementAndGet()
		int progress = (int) (100f * solved / (rows - 1))
		if (progress > curProgress.get()) {
			curProgress.incrementAndGet()
			pluginHost.updateProgress("Calculating Flow Directions:", progress)
		}
	}

	private void countInflowingCells(int row, byte[][] flowDir, byte[][] numInflow) {
		// check to see if the user has requested a cancellation
		if (pluginHost.isRequestForOperationCancelSet()) {
			return
		}
		
		int[] dX = [ 1, 1, 1, 0, -1, -1, -1, 0 ]
		int[] dY = [ -1, 0, 1, 1, 1, 0, -1, -1 ]
		int[] inflowingVals = [ 4, 5, 6, 7, 0, 1, 2, 3 ]
        int cols = flowDir[0].length
        int rows = flowDir.length
        int cN, rN, n
		for (int col = 0; col < cols; col++) {
			if (flowDir[row][col] >= 0) {
				n = 0
				for (int i = 0; i < 8; i++) {
					rN = row + dY[i]
					cN = col + dX[i]
					if (rN >= 0 && cN >= 0 && rN < rows && cN < cols) {
						if (flowDir[rN][cN] == inflowingVals[i]) {
							n++
						}
					}
				}
				numInflow[row][col] = (byte)n
			} else {
				//output.setValue(row, col, nodata)
				numInflow[row][col] = (byte)-1
			}
		}

		int solved = numSolved.incrementAndGet()
		int progress = (int) (100f * solved / (rows - 1))
		if (progress > curProgress.get()) {
			curProgress.incrementAndGet()
			pluginHost.updateProgress("Counting Inflowing Neighbours:", progress)
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

	@CompileStatic
    class UpslopeValues {
		ArrayList<Double> values
		double z
    	double maxDamHeight
    	
    	UpslopeValues(double z, double maxHeight) {
    		this.z = z
    		this.maxDamHeight = maxHeight
    	}

    	ArrayList<Double> getValues() {
    		if (this.values == null) {
    			values = new ArrayList<Double>()
    			values.add(z)
    		}
    		return values
    	}

    	void addValues(ArrayList<Double> otherValues) {
    		if (this.values == null) {
    			values = new ArrayList<Double>()
    			values.add(z)
    		}
    		double cutoff = z + maxDamHeight
    		// any value greater than cutoff is no longer
    		// a candidate since all downslope cells will 
    		// be lower than z and no cells higher than 
    		// the maxDamHeight value can be included.
    		for (Double zN : otherValues) {
    			if (zN <= cutoff) {
    				values.add(zN)
    			}
    		}
    	}

    	int numLessThan(double height) {
    		if (this.values == null) {
    			values = new ArrayList<Double>()
    			values.add(z)
    		}
    		int numLess = 0
    		if (height > 0) {
    			double cutoff = z + height
	    		for (double z2 : values) {
	    			if (z2 <= cutoff) { 
	    				numLess++
	    			}
	    		}
    		}
    		return numLess
    	}

    	double volumeBelow(double height, double areaMultiplier) {
    		if (this.values == null) {
    			values = new ArrayList<Double>()
    			values.add(z)
    		}
    		double volume = 0
    		if (height > 0) {
    			double cutoff = z + height
	    		for (double z2 : values) {
	    			if (z2 <= cutoff) { 
	    				volume += (cutoff - z2)
	    			}
	    		}
    		}
    		return volume * areaMultiplier
    	}
    }
}

if (args == null) {
	pluginHost.showFeedback("Plugin arguments not set.")
} else {
	def di = new ImpoundmentIndex(pluginHost, args, name, descriptiveName)
}
