/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class FetchAnalysis implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;

    @Override
    public String getName() {
        return "FetchAnalysis";
    }

    @Override
    public String getDescriptiveName() {
    	return "Fetch Analysis";
    }

    @Override
    public String getToolDescription() {
    	return "Performs an analysis of fetch or upwind distance to an obstacle.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "WindRelatedTAs" };
    	return ret;
    }

    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private int previousProgress = 0;
    private String previousProgressLabel = "";
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }
    
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;
        
        String inputHeader = null;
        String outputHeader = null;
        int i;
        int progress;
        int row, col;
        double z = 0;
        double gridRes = 0;
        double currentVal = 0;
        double maxValDist = 0;
        double maxDist = 0;
        double lineSlope = 0;
        double azimuth = 0;
        double deltaX = 0;
        double deltaY = 0;
        double x = 0;
        int x1 = 0;
        int x2 = 0;
        double y = 0;
        int y1 = 0;
        int y2 = 0;
        double z1 = 0;
        double z2 = 0;
        double dist = 0;
        double oldDist = 0;
        double yIntercept = 0;
        int xStep = 0;
        int yStep = 0;
        double noData = 0;
        boolean flag = false;
        double heightIncrement = 0;
        double currentMaxVal = 0;

        maxDist = Double.MAX_VALUE;
                    
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                azimuth = Double.parseDouble(args[i]);
                if (azimuth > 360 || azimuth < 0) {
                    azimuth = 0.1;
                }
                if (azimuth == 0) { azimuth = 0.1; }
                if (azimuth == 180) { azimuth = 179.9; }
                if (azimuth == 360) { azimuth = 359.9; }
                if (azimuth < 180) { 
                    lineSlope = Math.tan(Math.toRadians(90 - azimuth));
                } else {
                    lineSlope = Math.tan(Math.toRadians(270 - azimuth));
                }
                
            } else if (i == 3) {
                heightIncrement = Double.parseDouble(args[i]);
            }
        }
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            noData = DEM.getNoDataValue();
            gridRes = (DEM.getCellSizeX() + DEM.getCellSizeY()) / 2;
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("grey.pal");

            if (azimuth > 0 && azimuth <= 90) {
                xStep = 1;
                yStep = 1;
            } else if (azimuth <= 180) {
                xStep = 1;
                yStep = -1;
            } else if (azimuth <= 270) {
                xStep = -1;
                yStep = -1;
            } else {
                xStep = -1;
                yStep = 1;
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    currentVal = DEM.getValue(row, col);
                    if (currentVal != noData) {
                        //calculate the y intercept of the line equation
                        yIntercept = -row - lineSlope * col;

                        //find all of the vertical intersections
                        currentMaxVal = 0;
                        maxValDist = 0;
                        x = col;
                        
                        flag = true;
                        do {
                            x = x + xStep;
                            if (x < 0 || x >= cols) {
                                flag = false;
                                break;
                            }

                            //calculate the Y value
                            y = (lineSlope * x + yIntercept) * -1;
                            if (y < 0 || y >= rows) {
                                flag = false;
                                break;
                            }

                            //calculate the distance
                            deltaX = (x - col) * gridRes;
                            deltaY = (y - row) * gridRes;

                            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                            if (dist > maxDist) {
                                flag = false;
                                break;
                            }

                            //estimate z
                            y1 = (int)(y);
                            y2 = y1 + yStep * -1;
                            z1 = DEM.getValue(y1, (int)x);
                            z2 = DEM.getValue(y2, (int)x);
                            z = z1 + (y - y1) * (z2 - z1);
                            if (z >= currentVal + dist * heightIncrement) {
                                maxValDist = dist;
                                flag = false;
                            }
                        } while (flag);
                        
                        oldDist = dist;
                        
                        //find all of the horizontal intersections
                        y = -row;
                        flag = true;
                        do {
                            y = y + yStep;
                            if (-y < 0 || -y >= rows) {
                                flag = false;
                                break;
                            }

                            //calculate the X value
                            x = (y - yIntercept) / lineSlope;
                            if (x < 0 || x >= cols) {
                                flag = false;
                                break;
                            }

                            //calculate the distance
                            deltaX = (x - col) * gridRes;
                            deltaY = (-y - row) * gridRes;
                            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                            if (dist > maxDist) {
                                flag = false;
                                break;
                            }

                            //estimate z
                            x1 = (int)x;
                            x2 = x1 + xStep;
                            if (x2 < 0 || x2 >= cols) {
                                flag = false;
                                break;
                            }

                            z1 = DEM.getValue((int)-y, x1);
                            z2 = DEM.getValue((int)y, x2);
                            z = z1 + (x - x1) * (z2 - z1);
                            if (z >= currentVal + dist * heightIncrement) {
                                if (dist < maxValDist || maxValDist == 0) {
                                    maxValDist = dist; 
                                }
                                flag = false;
                            }
                        } while (flag);
                        
                        if (maxValDist == 0) {
                            //find the larger of dist and olddist
                            if (dist > oldDist) {
                                maxValDist = -dist;
                            } else {
                                maxValDist = -oldDist;
                            }
                        }
                        output.setValue(row, col, maxValDist);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int)(100f * row / (rows - 1));
                updateProgress(progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            DEM.close();
            output.close();
            
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
