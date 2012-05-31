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

import java.io.File;
import java.util.Date;
import java.util.PriorityQueue;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.utilities.FileUtilities;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class BreachDepressions implements WhiteboxPlugin {

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
        return "BreachDepressions";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Breach Depressions";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Remove all depressions in a DEM by breaching.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"DEMPreprocessing"};
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

        String inputHeader;
        String outputHeader;
        int row, col, x, y;
        int progress;
        double z, zn, previousZ;
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        int[] dX5 = {2, 2, 2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2, -1, 0, 1};
        int[] dY5 = {-2, -1, 0, 1, 2, 2, 2, 2, 2, 1, 0, -1, -2, -2, -2, -2};
        int[] breachCell = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 0};
        int maxDist = 0;
        int subgridSize = 0;
        int smallNeighbourhoodmaxDist = 5;
        int smallNeighbourhoodsubgridSize = 11;
        int largeNeighbourhoodmaxDist = 0;
        int largeNeighbourhoodsubgridSize = 0;
        boolean isLowest;
        double gridRes;
        double aSmallValue;
        int a, r, c, i, j, k, n, cn, rn;
        int numNoFlowCells;
        double largeVal = Float.MAX_VALUE;
        int solvedCells;
        boolean cellHasBeenSolved;
        boolean foundSolution;
        boolean atLeastOneSourceCell;
        boolean flag;
        double zMin;
        int minCell;
        int b = 0;
        double[][] decrementValue;
        double costAccumVal;
        double cost1, cost2;
        double newcostVal;
        double diagdist = Math.sqrt(2);
        double[] dist = {1, diagdist, 1, diagdist, 1, diagdist, 1, diagdist};
        int[] backLinkDir = {4, 5, 6, 7, 0, 1, 2, 3};
        int numLarge = 0, numUnsolvedCells = 0;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        inputHeader = args[0];
        outputHeader = args[1];
        largeNeighbourhoodmaxDist = Integer.parseInt(args[2]);
        largeNeighbourhoodsubgridSize = 2 * largeNeighbourhoodmaxDist + 1;

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster DEM = new WhiteboxRaster(inputHeader, "r");
            int rows = DEM.getNumberRows();
            int cols = DEM.getNumberColumns();
            double noData = DEM.getNoDataValue();

            // copy the input file to the output file.
            FileUtilities.copyFile(new File(inputHeader), new File(outputHeader));
            FileUtilities.copyFile(new File(inputHeader.replace(".dep", ".tas")), 
                    new File(outputHeader.replace(".dep", ".tas")));
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw");
            
            // figure out what the value of ASmallNumber should be.
            z = Math.abs(DEM.getMaximumValue());
            if (z <= 9) {
                aSmallValue = 0.00001F;
            } else if (z <= 99) {
                aSmallValue = 0.0001F;
            } else if (z <= 999) {
                aSmallValue = 0.001F;
            } else if (z <= 9999) {
                aSmallValue = 0.001F;
            } else if (z <= 99999) {
                aSmallValue = 0.01F;
            } else {
                aSmallValue = 1F;
            }

            // calculate the additional decrement value needed to create a descending pathway of 
            // lower elevation values along a breach path.
            double[][] SNDecrementValue =
                    new double[smallNeighbourhoodsubgridSize][smallNeighbourhoodsubgridSize];
            for (r = 0; r < smallNeighbourhoodsubgridSize; r++) {
                for (c = 0; c < smallNeighbourhoodsubgridSize; c++) {
                    j = Math.abs(c - smallNeighbourhoodmaxDist);
                    k = Math.abs(r - smallNeighbourhoodmaxDist);
                    SNDecrementValue[r][c] = (j + k) * aSmallValue;
                }
            }

            double[][] LNDecrementValue =
                    new double[largeNeighbourhoodsubgridSize][largeNeighbourhoodsubgridSize];
            for (r = 0; r < largeNeighbourhoodsubgridSize; r++) {
                for (c = 0; c < largeNeighbourhoodsubgridSize; c++) {
                    j = Math.abs(c - largeNeighbourhoodmaxDist);
                    k = Math.abs(r - largeNeighbourhoodmaxDist);
                    LNDecrementValue[r][c] = (j + k) * aSmallValue;
                }
            }

            DEM.close();

            // find all the cells with no downslope neighbours and put them into the queue
            PriorityQueue<DepGridCell> pq = new PriorityQueue<DepGridCell>((2 * rows + 2 * cols) * 2);

            //List<GridCell> pq = new List<GridCell>(); // this will serve as a 'priority queue'
            //to hold the no-flow cells.

            updateProgress("Loop 1 of 2:", 1);
            for (row = 1; row < (rows - 1); row++) {
                for (col = 1; col < (cols - 1); col++) {
                    z = output.getValue(row, col);
                    if (z != noData) {
                        isLowest = true;
                        for (a = 0; a < 8; a++) {
                            cn = col + dX[a];
                            rn = row + dY[a];
                            zn = output.getValue(rn, cn);
                            if (zn < z && zn != noData) {
                                isLowest = false;
                                break;
                            }
                        }
                        if (isLowest) {
                            pq.add(new DepGridCell(row, col, z));
                        }
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (int) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 2:", progress);
            }
            numNoFlowCells = pq.size();
            int oneHundredthOfNumNoFlowCells = (int)(numNoFlowCells / 100);

            updateProgress("Loop 2 of 2:", 1);
            DepGridCell cell = new DepGridCell(-1, -1, largeVal);
            solvedCells = 0;
            do {
                cell = pq.poll();
                col = cell.col;
                row = cell.row;
                z = output.getValue(row, col);

                // see if it's still a no-flow cell. It can be modified to have a downslope neighbour
                // during the processing of other cells prior.
                isLowest = true;
                for (a = 0; a < 8; a++) {
                    cn = col + dX[a];
                    rn = row + dY[a];
                    zn = output.getValue(rn, cn);
                    if (zn < z && zn != noData) {
                        isLowest = false;
                        break;
                    }
                }
                if (isLowest) {
                    // scan the 2nd order neighbours to see if one is lower.
                    cellHasBeenSolved = false;
                    zMin = largeVal;
                    minCell = -1;
                    for (a = 0; a < 16; a++) {
                        cn = col + dX5[a];
                        rn = row + dY5[a];
                        zn = output.getValue(rn, cn);

                        if (zn < zMin && zn != noData) {
                            zMin = zn;
                            minCell = a;
                        }
                    }

                    if (zMin < (z - aSmallValue)) {
                        cn = col + dX[breachCell[minCell]];
                        rn = row + dY[breachCell[minCell]];
                        zn = output.getValue(rn, cn);
                        if (((z + zMin) / 2) < zn) {
                            output.setValue(rn, cn, (z - aSmallValue));
                            cellHasBeenSolved = true;
                            b++;
                        }
                    }

                    if (!cellHasBeenSolved) {
                        foundSolution = false;
                        maxDist = smallNeighbourhoodmaxDist;
                        subgridSize = smallNeighbourhoodsubgridSize;
                        do {

                            double[][] sourceCells = new double[subgridSize][subgridSize];
                            double[][] cost = new double[subgridSize][subgridSize];
                            double[][] accumulatedcost = new double[subgridSize][subgridSize];
                            int[][] backLink = new int[subgridSize][subgridSize];

                            decrementValue = new double[subgridSize][subgridSize];
                            if (maxDist == smallNeighbourhoodmaxDist) {
                                System.arraycopy(SNDecrementValue, 0, decrementValue, 0, SNDecrementValue.length);
                            } else {
                                System.arraycopy(LNDecrementValue, 0, decrementValue, 0, LNDecrementValue.length);
                            }

                            atLeastOneSourceCell = false;
                            for (r = -maxDist; r <= maxDist; r++) {
                                for (c = -maxDist; c <= maxDist; c++) {
                                    zn = output.getValue(row + r, col + c);
                                    j = c + maxDist;
                                    k = r + maxDist;
                                    if ((zn + decrementValue[k][j]) < z && zn != noData) {
                                        sourceCells[k][j] = 1;
                                        cost[k][j] = 0;
                                        accumulatedcost[k][j] = 0;
                                        atLeastOneSourceCell = true;
                                    } else if ((zn + decrementValue[k][j]) >= z) {
                                        sourceCells[k][j] = 0;
                                        cost[k][j] = (zn - z) + decrementValue[k][j];
                                        accumulatedcost[k][j] = largeVal;
                                    } else { // noData cell
                                        sourceCells[k][j] = noData;
                                        cost[k][j] = noData;
                                        accumulatedcost[k][j] = noData;
                                    }
                                    backLink[k][j] = (int) noData;
                                }
                            }

                            sourceCells[maxDist][maxDist] = 0;
                            cost[maxDist][maxDist] = 0;
                            accumulatedcost[maxDist][maxDist] = Float.MAX_VALUE;

                            // is there at least one source cell?

                            if (atLeastOneSourceCell) {
                                boolean didSomething;
                                do {
                                    didSomething = false;
                                    for (r = 0; r < subgridSize; r++) {
                                        for (c = 0; c < subgridSize; c++) {
                                            costAccumVal = accumulatedcost[r][c];
                                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                                cost1 = cost[r][c];
                                                for (a = 0; a <= 3; a++) {
                                                    cn = c + dX[a];
                                                    rn = r + dY[a];
                                                    if (cn >= 0 && cn < subgridSize && rn >= 0 && rn < subgridSize) {
                                                        cost2 = cost[rn][cn];
                                                        newcostVal = costAccumVal + (cost1 + cost2) / 2 * dist[a];
                                                        if (newcostVal < accumulatedcost[rn][cn] && cost2 != noData) {
                                                            accumulatedcost[rn][cn] = newcostVal;
                                                            backLink[rn][cn] = backLinkDir[a];
                                                            didSomething = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    didSomething = false;
                                    for (r = (subgridSize - 1); r >= 0; r--) {
                                        for (c = (subgridSize - 1); c >= 0; c--) {
                                            costAccumVal = accumulatedcost[r][c];
                                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                                cost1 = cost[r][c];
                                                for (a = 4; a <= 7; a++) {
                                                    cn = c + dX[a];
                                                    rn = r + dY[a];
                                                    if (cn >= 0 && cn < subgridSize && rn >= 0 && rn < subgridSize) {
                                                        cost2 = cost[rn][cn];
                                                        newcostVal = costAccumVal + (cost1 + cost2) / 2 * dist[a];
                                                        if (newcostVal < accumulatedcost[rn][cn] && cost2 != noData) {
                                                            accumulatedcost[rn][cn] = newcostVal;
                                                            backLink[rn][cn] = backLinkDir[a];
                                                            didSomething = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    didSomething = false;
                                    for (c = (subgridSize - 1); c >= 0; c--) {
                                        for (r = (subgridSize - 1); r >= 0; r--) {
                                            costAccumVal = accumulatedcost[r][c];
                                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                                cost1 = cost[r][c];
                                                for (a = 3; a <= 6; a++) {
                                                    cn = c + dX[a];
                                                    rn = r + dY[a];
                                                    if (cn >= 0 && cn < subgridSize && rn >= 0 && rn < subgridSize) {
                                                        cost2 = cost[rn][cn];
                                                        newcostVal = costAccumVal + (cost1 + cost2) / 2 * dist[a];
                                                        if (newcostVal < accumulatedcost[rn][cn] && cost2 != noData) {
                                                            accumulatedcost[rn][cn] = newcostVal;
                                                            backLink[rn][cn] = backLinkDir[a];
                                                            didSomething = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    didSomething = false;
                                    for (r = (subgridSize - 1); r >= 0; r--) {
                                        for (c = 0; c < subgridSize; c++) {
                                            costAccumVal = accumulatedcost[r][c];
                                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                                cost1 = cost[r][c];
                                                for (a = 1; a <= 4; a++) {
                                                    cn = c + dX[a];
                                                    rn = r + dY[a];
                                                    if (cn >= 0 && cn < subgridSize && rn >= 0 && rn < subgridSize) {
                                                        cost2 = cost[rn][cn];
                                                        newcostVal = costAccumVal + (cost1 + cost2) / 2 * dist[a];
                                                        if (newcostVal < accumulatedcost[rn][cn] && cost2 != noData) {
                                                            accumulatedcost[rn][cn] = newcostVal;
                                                            backLink[rn][cn] = backLinkDir[a];
                                                            didSomething = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    didSomething = false;
                                    for (c = (subgridSize - 1); c >= 0; c--) {
                                        for (r = 0; r < subgridSize; r++) {
                                            costAccumVal = accumulatedcost[r][c];
                                            if (costAccumVal < largeVal && costAccumVal != noData) {
                                                cost1 = cost[r][c];
                                                for (a = 2; a <= 5; a++) {
                                                    cn = c + dX[a];
                                                    rn = r + dY[a];
                                                    if (cn >= 0 && cn < subgridSize && rn >= 0 && rn < subgridSize) {
                                                        cost2 = cost[rn][cn];
                                                        newcostVal = costAccumVal + (cost1 + cost2) / 2 * dist[a];
                                                        if (newcostVal < accumulatedcost[rn][cn] && cost2 != noData) {
                                                            accumulatedcost[rn][cn] = newcostVal;
                                                            backLink[rn][cn] = backLinkDir[a];
                                                            didSomething = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } while (didSomething);

                                c = maxDist;
                                r = maxDist;
                                previousZ = z;
                                b = 0;
                                flag = true;
                                do {
                                    //Find which cell to go to from here
                                    b = backLink[r][c];
                                    if (b >= 0) {
                                        c = c + dX[b];
                                        r = r + dY[b];
                                        col = col + dX[b];
                                        row = row + dY[b];
                                        zn = output.getValue(row, col);
                                        if (zn > (previousZ - aSmallValue)) {
                                            output.setValue(row, col, previousZ - aSmallValue);
                                        }
                                        previousZ = output.getValue(row, col);
                                    } else {
                                        flag = false;
                                    }
                                } while (flag);
                                numLarge++;
                                foundSolution = true;
                            } else {
                                if (maxDist == largeNeighbourhoodmaxDist) {
                                    numUnsolvedCells++;
                                    foundSolution = true; //not really but we need this to get out of the do loop.
                                }
                            }

                            if (foundSolution == false && maxDist == smallNeighbourhoodmaxDist) {
                                maxDist = largeNeighbourhoodmaxDist;
                                subgridSize = largeNeighbourhoodsubgridSize;
                            }
                        } while (foundSolution == false);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }

                solvedCells++;
                if (solvedCells % oneHundredthOfNumNoFlowCells == 0) {
                    progress = (int) ((solvedCells * 100.0) / numNoFlowCells);
                    updateProgress("Loop 2 of 2:", progress);

                }

            } while (solvedCells < numNoFlowCells);


            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            output.close();

            // returning a header file string displays the image.
            returnData(outputHeader);

            String results = "Depression Breaching Results:\n";
            results += "Solved Depression Cells:\t" + String.valueOf(solvedCells - numUnsolvedCells);
            results += "\nUnsolved Depression Cells:\t" + numUnsolvedCells;
            returnData(results);
        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }

    class DepGridCell implements Comparable<DepGridCell> {

        public int row;
        public int col;
        public double z;

        public DepGridCell(int row, int col, double z) {
            this.row = row;
            this.col = col;
            this.z = z;
        }

        @Override
        public int compareTo(DepGridCell cell) {
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (this.z < cell.z) {
                return BEFORE;
            } else if (this.z > cell.z) {
                return AFTER;
            }

            if (this.row < cell.row) {
                return BEFORE;
            } else if (this.row > cell.row) {
                return AFTER;
            }

            if (this.col < cell.col) {
                return BEFORE;
            } else if (this.col > cell.col) {
                return AFTER;
            }

            return EQUAL;
        }
    }
//    
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        BreachDepressions bd = new BreachDepressions();
//        args = new String[5];
//        //args[0] = "/Users/johnlindsay/Documents/agricultural site no OTOs.dep";
//        //args[1] = "/Users/johnlindsay/Documents/agricultural site no OTOs breached.dep";
//        args[0] = "/Users/johnlindsay/Documents/DEM.dep";
//        args[1] = "/Users/johnlindsay/Documents/DEM breached.dep";
//        args[2] = "80";
//        
//        bd.setArgs(args);
//        bd.run();
//        
//    }
}