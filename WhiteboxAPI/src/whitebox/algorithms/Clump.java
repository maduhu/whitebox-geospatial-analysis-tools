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
package whitebox.algorithms;

import java.util.Date;
import whitebox.geospatialfiles.WhiteboxRaster;

/**
 * This algorithm takes an input raster of categorical data (e.g. a land-use
 * image) and assigns a unique identifier value to each contiguous group of
 * same-valued grid cells (i.e. region).
 *
 * @author johnlindsay
 */
public class Clump {

    double noData = -32768;
    WhiteboxRaster image;
    WhiteboxRaster output;
    double currentPatchNumber = 0;
    double currentImageValue = 0;
    int maxDepth = 1000;
    int depth = 0;
    int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
    int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
    int numScanCells = 8;
    boolean blnIncludeDiagNeighbour = true;
    boolean blnTreatZerosAsBackground = false;
    String outputHeader = "";

    public Clump(String inputHeaderFile) {
        this.image = new WhiteboxRaster(inputHeaderFile, "r");
    }

    public Clump(WhiteboxRaster inputImage) {
        this.image = inputImage;
    }

    public Clump(WhiteboxRaster inputImage, boolean includeDiagonalNeighbours) {
        this.image = inputImage;
        this.blnIncludeDiagNeighbour = includeDiagonalNeighbours;

        if (!blnIncludeDiagNeighbour) {
            dX = new int[]{0, 1, 0, -1};
            dY = new int[]{-1, 0, 1, 0};
        }
    }

    public Clump(WhiteboxRaster inputImage, boolean includeDiagonalNeighbours,
            boolean treatZerosAsBackground) {
        this.image = inputImage;
        this.blnIncludeDiagNeighbour = includeDiagonalNeighbours;

        if (!blnIncludeDiagNeighbour) {
            dX = new int[]{0, 1, 0, -1};
            dY = new int[]{-1, 0, 1, 0};
        }
        this.blnTreatZerosAsBackground = treatZerosAsBackground;
    }

    public String getOutputHeader() {
        return outputHeader;
    }

    public void setOutputHeader(String outputHeader) {
        this.outputHeader = outputHeader;
    }

    public void setIncludeDiagonalNeighbours(boolean value) {
        this.blnIncludeDiagNeighbour = value;

        if (!blnIncludeDiagNeighbour) {
            dX = new int[]{0, 1, 0, -1};
            dY = new int[]{-1, 0, 1, 0};
        }
    }

    public WhiteboxRaster run() throws Exception {
        int row, col, x, y, i;
        boolean blnFoundNeighbour;
        double maxPatchValue = 1;
        numScanCells = dY.length;
        int rows = image.getNumberRows();
        int cols = image.getNumberColumns();
        noData = image.getNoDataValue();

        double initialValue = -1;
        if (outputHeader.isEmpty()) {
            outputHeader = image.getHeaderFile().replace(".dep", "_clumped.dep");
        }
        output = new WhiteboxRaster(outputHeader, "rw", image.getHeaderFile(), WhiteboxRaster.DataType.FLOAT, initialValue);
        output.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);
        output.setPreferredPalette("qual.pal");

        if (blnTreatZerosAsBackground) {
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (image.getValue(row, col) == 0) {
                        output.setValue(row, col, 0);
                    }
                }
            }
            if (output.getValue(0, 0) == -1) {
                output.setValue(0, 0, maxPatchValue);
                // recursively scan all connected cells of equal value in Image
                depth = 0;
                ScanConnectedCells(0, 0, image.getValue(0, 0), initialValue, maxPatchValue);
            }
        } else {
            output.setValue(0, 0, maxPatchValue);
            // recursively scan all connected cells of equal value in Image
            depth = 0;
            ScanConnectedCells(0, 0, image.getValue(0, 0), initialValue, maxPatchValue);
        }

        double patchValue = 0;
        double neighbourPatchValue = 0;
        double newPatchValue = 0;
        double imageValue = 0;
        for (row = 0; row < rows; row++) {
            for (col = 0; col < cols; col++) {
                imageValue = image.getValue(row, col);
                if (imageValue != noData) {
                    patchValue = output.getValue(row, col);
                    if (patchValue == initialValue) {
                        // see if any neighbour has the same value in the input image
                        blnFoundNeighbour = false;
                        for (i = 0; i < numScanCells; i++) {
                            x = col + dX[i];
                            y = row + dY[i];
                            neighbourPatchValue = output.getValue(y, x);
                            if (neighbourPatchValue != initialValue
                                    && image.getValue(y, x) == imageValue) {
                                // cell is neighbouring a cell with the same value in image that
                                // has already been assigned a patch value
                                output.setValue(row, col, neighbourPatchValue);
                                newPatchValue = neighbourPatchValue;
                                blnFoundNeighbour = true;
                                break;
                            }
                        }
                        if (!blnFoundNeighbour) {
                            // no neighbouring cell has the same value in Image and has 
                            // already been assigned a value. A new one is needed.
                            maxPatchValue++;
                            newPatchValue = maxPatchValue;
                            output.setValue(row, col, newPatchValue);
                        }

                        // recursively scan all connected cells of equal value in Image
                        depth = 0;
                        ScanConnectedCells(row, col, imageValue, initialValue, newPatchValue);
                    }
                } else {
                    output.setValue(row, col, noData);
                }
            }
        }

        // find all cells with neighbouring cells that have the same value in
        // the input image but different patch values in the output image.
        // Recursively scan them to change the larger patch ID to the lower value.
        // Iterate this process until there are no further changes to the image.
        boolean somethingDone;
        double[] reclass = new double[(int) maxPatchValue + 1];
        // this array is used to keep track of the eliminated patches.
        do {
            somethingDone = false;
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    imageValue = image.getValue(row, col);
                    if (imageValue != noData) {
                        patchValue = output.getValue(row, col);
                        for (i = 0; i < numScanCells; i++) {
                            x = col + dX[i];
                            y = row + dY[i];
                            neighbourPatchValue = output.getValue(y, x);
                            if (neighbourPatchValue != patchValue
                                    && image.getValue(y, x) == imageValue) {
                                // The two patches are equivalent. Find the 
                                // lower valued cell and initiate a recursive
                                // scan from there.
                                somethingDone = true;
                                if (patchValue < neighbourPatchValue) {
                                    reclass[(int) neighbourPatchValue] = -1;
                                    output.setValue(y, x, patchValue);
                                    ScanConnectedCells(y, x, imageValue, neighbourPatchValue, patchValue);
                                } else {
                                    reclass[(int) patchValue] = -1;
                                    output.setValue(row, col, neighbourPatchValue);
                                    ScanConnectedCells(row, col, imageValue, patchValue, neighbourPatchValue);
                                    patchValue = neighbourPatchValue;
                                }

                            }
                        }
                    }
                }
            }
        } while (somethingDone);

        i = 0;
        for (int a = 0; a < maxPatchValue + 1; a++) {
            if (reclass[a] != -1) {
                reclass[a] = i;
                i++;
            }
        }

        for (row = 0; row < rows; row++) {
            for (col = 0; col < cols; col++) {
                patchValue = output.getValue(row, col);
                if (patchValue != noData) {
                    output.setValue(row, col, reclass[(int) patchValue]);
                }
            }
        }

        output.addMetadataEntry("Created by the "
                + "Clump algorithm.");
        output.addMetadataEntry("Created on " + new Date());

        output.flush();
        output.writeHeaderFile();
        return output;
    }

    private void ScanConnectedCells(int row, int col, double imageValue, double currentPatchValue, double newPatchValue) {
        depth++;
        int x, y;
        if (depth < maxDepth) {
            for (int c = 0; c < numScanCells; c++) {
                x = col + dX[c];
                y = row + dY[c];
                if ((output.getValue(y, x) == currentPatchValue)
                        && (image.getValue(y, x) == imageValue)) {
                    // cell should be assigned the new patch value and has the same value in Image
                    output.setValue(y, x, newPatchValue);
                    ScanConnectedCells(y, x, imageValue, currentPatchValue, newPatchValue);
                }
            }
        }
        depth--;
    }
}
