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
package whitebox.geospatialfiles;

import java.awt.Color;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.Arrays;
import whitebox.interfaces.MapLayer;
import whitebox.structures.BoundingBox;
import whitebox.structures.BooleanBitArray1D;
import whitebox.geospatialfiles.LASReader.PointRecord;
import whitebox.structures.XYPoint;
//import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author Dr. John Lindsay
 */
public class LasLayerInfo implements MapLayer {

    public final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private String fileName;
    private LASReader lasFile;
    String layerTitle = "";
    private BoundingBox fullExtent = null;
    private BoundingBox currentExtent = null;
    private boolean visible = true;
    private int overlayNumber;
    private boolean visibleInLegend = true;
    private int alpha = 255;
    private String paletteFile = "";
    private String pathSep;
    private BooleanBitArray1D selectedFeatures;
    private String paletteDirectory = "";
    private boolean filledWithOneColour = false;
    private int selectedFeatureNumber = -1;
    private double minimumValue = -32768.0;
    private double maximumValue = -32768.0;
    private double displayMinValue = -32768.0;
    private double displayMaxValue = -32768.0;
    private Color fillColour = Color.red;
    private float markerSize = 2.0f;
    private long numPointRecords = -1;
    private double[][] xyData;
    private double[] zData;
    private int[] intensityData;
    private byte[] classData;
    private byte fillCriterion = 0; // z value
    private int numPaletteEntries = 0;
    private int[] paletteData = null;
//    private WhiteboxPluginHost host;

    // Constructors
    public LasLayerInfo() {
    }

    public LasLayerInfo(String fileName, String paletteDirectory, int alpha, int overlayNumber) { //, WhiteboxPluginHost host) {
        this.fileName = fileName;
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found.");
        }
        this.paletteDirectory = paletteDirectory;

        this.layerTitle = file.getName().replace(".las", "");
        this.alpha = alpha;
        this.overlayNumber = overlayNumber;

//        this.host = host;
        lasFile = new LASReader(fileName);
        numPointRecords = lasFile.getNumPointRecords();

        currentExtent = new BoundingBox(lasFile.getMinX(), lasFile.getMinY(),
                lasFile.getMaxX(), lasFile.getMaxY());

        fullExtent = currentExtent.clone();

        pathSep = File.separator;

        paletteFile = paletteDirectory + "spectrum.pal";

        selectedFeatures = new BooleanBitArray1D((int) lasFile.getNumPointRecords() + 1);
    }

    public LASReader getLASFile() {
        return lasFile;
    }

//    public WhiteboxPluginHost getHost() {
//        return host;
//    }
//
//    public void setHost(WhiteboxPluginHost host) {
//        this.host = host;
//    }
    @Override
    public String getLayerTitle() {
        return layerTitle;
    }

    @Override
    public void setLayerTitle(String title) {
        layerTitle = title;
    }

    @Override
    public MapLayerType getLayerType() {
        return MapLayer.MapLayerType.LAS;
    }

    @Override
    public BoundingBox getFullExtent() {
        return fullExtent.clone();
    }

    @Override
    public BoundingBox getCurrentExtent() {
        return currentExtent.clone();
    }

    @Override
    public void setCurrentExtent(BoundingBox bb) {
        if (!bb.equals(currentExtent)) {
            currentExtent = bb.clone();
            getXYDataInExtent();
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean value) {
        visible = value;
    }

    @Override
    public int getOverlayNumber() {
        return overlayNumber;
    }

    @Override
    public void setOverlayNumber(int value) {
        overlayNumber = value;
    }

    @Override
    public boolean isVisibleInLegend() {
        return visibleInLegend;
    }

    @Override
    public void setVisibleInLegend(boolean value) {
        this.visibleInLegend = value;
    }

    public float getMarkerSize() {
        return markerSize;
    }

    public void setMarkerSize(float markerSize) {
        this.markerSize = markerSize;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int value) {
        if (value < 0) {
            value = 0;
        }
        if (value > 255) {
            value = 255;
        }
        alpha = value;
        //setRecordsColourData();
    }

    public boolean isFilledWithOneColour() {
        return filledWithOneColour;
    }

    public void setFilledWithOneColour(boolean filledWithOneColour) {
        this.filledWithOneColour = filledWithOneColour;
    }

    public String getFillCriterion() {
        switch (fillCriterion) {
            case 0:
                return "z-value";
            case 1:
                return "intensity";
            default:
                return "class";
        }
    }

    public void setFillCriterion(String value) {
        filledWithOneColour = false;
        if (value.toLowerCase().contains("z")) {
            fillCriterion = 0; // elevation
            paletteFile = paletteDirectory + "spectrum.pal";
            //readPalette();
        } else if (value.toLowerCase().contains("i")) {
            fillCriterion = 1; // intensity
            paletteFile = paletteDirectory + "grey.pal";
            //readPalette();
        } else if (value.toLowerCase().contains("class")) {
            fillCriterion = 2; // classification
        }
        setRecordsColourData();
    }

    public void updateMinAndMaxValsForCriterion(String criterion) {
        if (criterion.toLowerCase().contains("z")) {
            fillCriterion = 0; // elevation
            if (zData == null) {
                readZData();
            } else {
                minimumValue = lasFile.getMinZ();
                maximumValue = lasFile.getMaxZ();

                double range = maximumValue - minimumValue;
                binSize = range / numPaletteEntries;
                histo = new int[numPaletteEntries + 1];
                int bin;
                for (int i = 0; i < numPointRecords; i++) {
                    bin = (int) ((zData[i] - minimumValue) / binSize);
                    histo[bin]++;
                }
                int onePercent = (int) (numPointRecords * 0.01);

                int sum = 0;
                for (int i = histo.length - 1; i >= 0; i--) {
                    sum += histo[i];
                    if (sum >= onePercent) {
                        displayMaxValue = (i * binSize) + minimumValue;
                        break;
                    }
                }

                sum = 0;
                for (int i = 0; i < histo.length; i++) {
                    sum += histo[i];
                    if (sum >= onePercent) {
                        displayMinValue = (i * binSize) + minimumValue;
                        break;
                    }
                }

            }
        } else if (criterion.toLowerCase().contains("i")) {
            fillCriterion = 1; // intensity
            if (intensityData == null) {
                readIntensityData();
            } else {

                int minIntensity = Integer.MAX_VALUE;
                int maxIntensity = Integer.MIN_VALUE;
                for (int i = 0; i < intensityData.length; i++) {
                    int intensity = intensityData[i];
                    if (intensity < minIntensity) {
                        minIntensity = intensity;
                    }
                    if (intensity > maxIntensity) {
                        maxIntensity = intensity;
                    }
                }
                minimumValue = minIntensity;
                maximumValue = maxIntensity;

                int range = (int) (maximumValue - minimumValue) + 1;
                histo = new int[range];
                binSize = 1;
                int min = (int) minimumValue;
                for (int i = 0; i < numPointRecords; i++) {
                    histo[intensityData[i] - min]++;
                }
                int onePercent = (int) (numPointRecords * 0.01);
                int sum = 0;
                for (int i = 0; i < histo.length; i++) {
                    sum += histo[i];
                    if (sum >= onePercent) {
                        displayMinValue = i + min;
                        break;
                    }
                }

                sum = 0;
                for (int i = histo.length - 1; i >= 0; i--) {
                    sum += histo[i];
                    if (sum >= onePercent) {
                        displayMaxValue = i + min;
                        break;
                    }
                }
            }

        } else if (criterion.toLowerCase().contains("class")) {
            fillCriterion = 2; // classification
        }
    }

    private void findPaletteDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                if (files[x].toString().endsWith(pathSep + "palettes")) {
                    paletteDirectory = files[x].toString() + pathSep;
                    break;
                } else {
                    findPaletteDirectory(files[x]);
                }
            }
        }
    }

    /**
     * Clears all of the selected features from a LAS layer.
     */
    public void clearSelectedFeatures() {
        selectedFeatures = new BooleanBitArray1D((int) lasFile.getNumPointRecords() + 1);
        selectedFeatureNumbers.clear();

//        int oldValue = this.selectedFeatureNumber;
        this.selectedFeatureNumber = -2;
        this.pcs.firePropertyChange("selectedFeatureNumber", -1, selectedFeatureNumber);
    }

    /**
     * Determines if a specified record number is selected.
     *
     * @param recordNumber The one-based record ID.
     * @return boolean A boolean value.
     */
    public boolean isFeatureSelected(int recordNumber) {
        if (recordNumber >= selectedFeatures.getLength()) {
            return false;
        }
        return selectedFeatures.getValue(recordNumber);
    }
    private ArrayList<Integer> selectedFeatureNumbers = new ArrayList<>();

    /**
     * Gets a list of all selected feature record numbers.
     *
     * @return ArrayList of selected feature numbers.
     */
    public ArrayList<Integer> getSelectedFeatureNumbers() {
        return selectedFeatureNumbers;
    }

    public Color getFillColour() {
        return fillColour;
    }

    public void setFillColour(Color fillColour) {
        this.fillColour = fillColour;
        setRecordsColourData();
    }

    public String getPaletteFile() {
        return paletteFile;
    }

    public void setPaletteFile(String fileName) {
        if (!paletteFile.equals(fileName)) {
            paletteFile = fileName;
        }
    }

    public double getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(double value) {
        this.maximumValue = value;
    }

    public double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(double value) {
        this.minimumValue = value;
    }

    public double getDisplayMaxValue() {
        return displayMaxValue;
    }

    public void setDisplayMaxValue(double value) {
        this.displayMaxValue = value;
    }

    public double getDisplayMinValue() {
        return displayMinValue;
    }

    public void setDisplayMinValue(double value) {
        this.displayMinValue = value;
    }

    ArrayList<XYPoint> pointXYData;

    public ArrayList<XYPoint> getPointXYData() {
        if (pointXYData == null) {
            getXYDataInExtent();
        }
        return pointXYData;
    }

    private void readXYData() {
        xyData = new double[(int) numPointRecords][2];
        PointRecord rec;
        if (isFilledWithOneColour()) {
            //int oldProgress = -1;
            //int progress = 0;
            for (int i = 0; i < numPointRecords; i++) {
                rec = lasFile.getPointRecord(i);
                if (!rec.isPointWithheld()) {
                    xyData[i][0] = rec.getX();
                    xyData[i][1] = rec.getY();
                }
//            progress = (int) (100d * i / (numPointRecords + 1));
//            if (progress != oldProgress) {
//                oldProgress = progress;
//                if (host != null) {
//                    host.updateProgress("Reading data:", progress);
//                }
//            }
            }
//        if (host != null) {
//            host.updateProgress(0);
//        }
        } else if (fillCriterion == 0) { // elevation is the fill criterion
            // If you're going to have to read every record in the file, you 
            // may as well grab the elevation data at the same time.
            minimumValue = lasFile.getMinZ();
            maximumValue = lasFile.getMaxZ();

            zData = new double[(int) numPointRecords];
            for (int i = 0; i < numPointRecords; i++) {
                rec = lasFile.getPointRecord(i);
                if (!rec.isPointWithheld()) {
                    xyData[i][0] = rec.getX();
                    xyData[i][1] = rec.getY();
                    zData[i] = rec.getZ();
                }
            }

            double range = maximumValue - minimumValue;
            readPalette();
            binSize = range / numPaletteEntries;
            histo = new int[numPaletteEntries + 1];
            int bin;
            for (int i = 0; i < numPointRecords; i++) {
                bin = (int) ((zData[i] - minimumValue) / binSize);
                histo[bin]++;
            }
            int onePercent = (int) (numPointRecords * 0.01);

            int sum = 0;
            for (int i = histo.length - 1; i >= 0; i--) {
                sum += histo[i];
                if (sum >= onePercent) {
                    displayMaxValue = (i * binSize) + minimumValue;
                    break;
                }
            }

            sum = 0;
            for (int i = 0; i < histo.length; i++) {
                sum += histo[i];
                if (sum >= onePercent) {
                    displayMinValue = (i * binSize) + minimumValue;
                    break;
                }
            }
        } else if (fillCriterion == 1) { // intensity is the fill criterion
            int intensity;
            int minIntensity = Integer.MAX_VALUE;
            int maxIntensity = Integer.MIN_VALUE;
            intensityData = new int[(int) numPointRecords];
            for (int i = 0; i < numPointRecords; i++) {
                rec = lasFile.getPointRecord(i);
                if (!rec.isPointWithheld()) {
                    xyData[i][0] = rec.getX();
                    xyData[i][1] = rec.getY();
                    intensity = rec.getIntensity();
                    intensityData[i] = intensity;
                    if (intensity < minIntensity) {
                        minIntensity = intensity;
                    }
                    if (intensity > maxIntensity) {
                        maxIntensity = intensity;
                    }
                }
            }

            minimumValue = minIntensity;
            maximumValue = maxIntensity;

            int range = (int) (maximumValue - minimumValue) + 1;
            histo = new int[range];
            binSize = 1;
            int min = (int) minimumValue;
            for (int i = 0; i < numPointRecords; i++) {
                histo[intensityData[i] - min]++;
            }
            int onePercent = (int) (numPointRecords * 0.01);
            int sum = 0;
            for (int i = 0; i < histo.length; i++) {
                sum += histo[i];
                if (sum >= onePercent) {
                    displayMinValue = i + min;
                    break;
                }
            }

            sum = 0;
            for (int i = histo.length - 1; i >= 0; i--) {
                sum += histo[i];
                if (sum >= onePercent) {
                    displayMaxValue = i + min;
                    break;
                }
            }
        }
    }

    int[] histo;
    double binSize;

    private void readZData() {
        minimumValue = lasFile.getMinZ();
        maximumValue = lasFile.getMaxZ();

        zData = new double[(int) numPointRecords];
        PointRecord rec;
//        int oldProgress = -1;
//        int progress = 0;
        for (int i = 0; i < numPointRecords; i++) {
            rec = lasFile.getPointRecord(i);
            if (!rec.isPointWithheld()) {
                zData[i] = rec.getZ();
            }
//            progress = (int) (100d * i / (numPointRecords + 1));
//            if (progress != oldProgress) {
//                oldProgress = progress;
//                if (host != null) {
//                    host.updateProgress("Reading data:", progress);
//                }
//            }
        }

//        if (host != null) {
//            host.updateProgress(0);
//        }
        double range = maximumValue - minimumValue;
        binSize = range / numPaletteEntries;
        histo = new int[numPaletteEntries + 1];
        int bin;
        for (int i = 0; i < numPointRecords; i++) {
            bin = (int) ((zData[i] - minimumValue) / binSize);
            histo[bin]++;
        }
        int onePercent = (int) (numPointRecords * 0.01);

        int sum = 0;
        for (int i = histo.length - 1; i >= 0; i--) {
            sum += histo[i];
            if (sum >= onePercent) {
                displayMaxValue = (i * binSize) + minimumValue;
                break;
            }
        }

        sum = 0;
        for (int i = 0; i < histo.length; i++) {
            sum += histo[i];
            if (sum >= onePercent) {
                displayMinValue = (i * binSize) + minimumValue;
                break;
            }
        }

    }

    private void readIntensityData() {
        int intensity;
        int minIntensity = Integer.MAX_VALUE;
        int maxIntensity = Integer.MIN_VALUE;
        intensityData = new int[(int) numPointRecords];
        PointRecord rec;
//        int oldProgress = -1;
//        int progress = 0;
        for (int i = 0; i < numPointRecords; i++) {
            rec = lasFile.getPointRecord(i);
            if (!rec.isPointWithheld()) {
                intensity = rec.getIntensity();
                intensityData[i] = intensity;
                if (intensity < minIntensity) {
                    minIntensity = intensity;
                }
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                }
            }
//            progress = (int) (100d * i / (numPointRecords + 1));
//            if (progress != oldProgress) {
//                oldProgress = progress;
//                if (host != null) {
//                    host.updateProgress("Reading data:", progress);
//                }
//            }
        }

//        if (host != null) {
//            host.updateProgress(0);
//        }
        minimumValue = minIntensity;
        maximumValue = maxIntensity;

        int range = (int) (maximumValue - minimumValue) + 1;
        histo = new int[range];
        binSize = 1;
        int min = (int) minimumValue;
        for (int i = 0; i < numPointRecords; i++) {
            histo[intensityData[i] - min]++;
        }
        int onePercent = (int) (numPointRecords * 0.01);
        int sum = 0;
        for (int i = 0; i < histo.length; i++) {
            sum += histo[i];
            if (sum >= onePercent) {
                displayMinValue = i + min;
                break;
            }
        }

        sum = 0;
        for (int i = histo.length - 1; i >= 0; i--) {
            sum += histo[i];
            if (sum >= onePercent) {
                displayMaxValue = i + min;
                break;
            }
        }

    }

    public void clipLowerTailForDisplayMinimum(double percent) {
        int target = (int) (numPointRecords * percent / 100d);
        int sum = 0;
        for (int i = 0; i < histo.length; i++) {
            sum += histo[i];
            if (sum >= target) {
                displayMinValue = (i * binSize) + minimumValue;
                break;
            }
        }
    }

    public void clipUpperTailForDisplayMaximum(double percent) {
        int target = (int) (numPointRecords * percent / 100d);
        int sum = 0;
        for (int i = histo.length - 1; i >= 0; i--) {
            sum += histo[i];
            if (sum >= target) {
                displayMaxValue = (i * binSize) + minimumValue;
                break;
            }
        }
    }

    private void readClassData() {
        classData = new byte[(int) numPointRecords];
        PointRecord rec;
        for (int i = 0; i < numPointRecords; i++) {
            rec = lasFile.getPointRecord(i);
            if (!rec.isPointWithheld()) {
                classData[i] = rec.getClassification();
            }
        }
    }

    int maxNumDisplayedPoints = 50000;

    private void getXYDataInExtent() {
        if (xyData == null) {
            readXYData();
        }
        if (colourData == null) {
            setRecordsColourData();
        }

        double minXbb = currentExtent.getMinX();
        double minYbb = currentExtent.getMinY();
        double maxXbb = currentExtent.getMaxX();
        double maxYbb = currentExtent.getMaxY();
        double x, y;

        pointXYData = new ArrayList<>();
        PointRecord rec;
        if (filledWithOneColour) {
            colourDataOfExtent = new ArrayList<>();
            colourDataOfExtent.add(colourData[0]);
            for (int i = 0; i < xyData.length; i++) {
                x = xyData[i][0];
                y = xyData[i][1];
                if (maxYbb < y || maxXbb < x || minYbb > y || minXbb > x) {
                    // do nothing it's outside the bounds
                } else {
                    pointXYData.add(new XYPoint(x, y));
                }
            }
        } else {
            colourDataOfExtent = new ArrayList<>();
            for (int i = 0; i < xyData.length; i++) {
                x = xyData[i][0];
                y = xyData[i][1];
                if (maxYbb < y || maxXbb < x || minYbb > y || minXbb > x) {
                    // do nothing it's outside the bounds
                } else {
                    pointXYData.add(new XYPoint(x, y));
                    colourDataOfExtent.add(colourData[i]);
                }
            }
        }

//        ArrayList<XYPoint> ret = new ArrayList<>();
//        PointRecord rec;
//        for (int i = 0; i < numPointRecords; i++) {
//            rec = lasFile.getPointRecord(i);
//            if (!rec.isPointWithheld()) {
//                x = rec.getX();
//                y = rec.getY();
//                if (maxYbb < y || maxXbb < x || minYbb > y || minXbb > x) {
//                    // do nothing it's outside the bounds
//                } else {
//                    ret.add(new XYPoint(x, y));
//                }
//            }
//        }
//        int numPointsInExtent = ret.size();
//        if (numPointsInExtent > maxNumDisplayedPoints) {
//            int skipVal = (int)(Math.ceil(numPointsInExtent / maxNumDisplayedPoints));
//            pointXYData = new ArrayList<>(numPointsInExtent / skipVal);
//            PointRecord point;
//            for (int i = 0; i < numPointsInExtent; i += skipVal) {
//                pointXYData.add(ret.get(i));
//            }
//            
//        } else {
//            pointXYData = new ArrayList<>(ret);
//        }
    }

    /**
     * Returns the number of features that are currently selected.
     *
     * @return int of the number of selected features.
     */
    public int getNumSelectedFeatures() {
        return selectedFeatureNumbers.size();
    }

    private Color[] colourData;
    private ArrayList<Color> colourDataOfExtent;

    public ArrayList<Color> getColourData() {
        if (colourData == null) {
            setRecordsColourData();
        }
        if (colourDataOfExtent == null) {
            getXYDataInExtent();
        }
        return colourDataOfExtent;
    }

    public void setRecordsColourData() {
        colourDataOfExtent = null;
        if (filledWithOneColour) {
            colourData = new Color[1];
            colourData[0] = new Color(fillColour.getRed(), fillColour.getGreen(), fillColour.getBlue(), alpha);
        } else {
            readPalette();
            colourData = new Color[(int) numPointRecords];
            int a1 = this.getAlpha();
            double value, range;
            int numPaletteEntriesLessOne = numPaletteEntries - 1;
            int entryNum;
            double gamma = 1;
            Color clr;

            switch (fillCriterion) {
                case 0: // elevation
                    if (zData == null) {
                        readZData();
                    }
                    range = displayMaxValue - displayMinValue;

                    IntStream.range(0, (int) numPointRecords - 1).parallel().forEach(i
                            -> colourData[i] = getColourFromElevation(i, range));

                    break;

                case 1: // intensity
                    if (intensityData == null) {
                        readIntensityData();
                    }
                    range = displayMaxValue - displayMinValue;

                    IntStream.range(0, (int) numPointRecords - 1).parallel().forEach(i
                            -> colourData[i] = getColourFromIntensity(i, range));

//                    for (int i = 0; i < (int) numPointRecords; i++) {
//                        value = intensityData[i];
//                        if (gamma == 1) {
//                            entryNum = (int) ((value - displayMinValue) / range * numPaletteEntriesLessOne);
//                        } else {
//                            entryNum = (int) (Math.pow((value - displayMinValue) / range, gamma) * numPaletteEntriesLessOne);
//                        }
//
//                        //entryNum = (int) (((value - minimumValue) / range) * (numPaletteEntries - 1));
//                        if (entryNum < 0) {
//                            entryNum = 0;
//                        }
//                        if (entryNum > numPaletteEntries - 1) {
//                            entryNum = numPaletteEntries - 1;
//                        }
//                        clr = new Color(paletteData[entryNum]);
//                        colourData[i] = new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), a1);
//                    }

                    break;
                case 2: // classification
                    if (classData == null) {
                        readClassData();
                    }

                    break;

            }
        }
    }

    private Color getColourFromElevation(int i, double range) {
        int entryNum;
        double value = zData[i];
        entryNum = (int) ((value - displayMinValue) / range * (numPaletteEntries - 1));

        if (entryNum < 0) {
            entryNum = 0;
        }
        if (entryNum > numPaletteEntries - 1) {
            entryNum = numPaletteEntries - 1;
        }
        Color clr = new Color(paletteData[entryNum]);
        return new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), alpha);
    }

    private Color getColourFromIntensity(int i, double range) {
        int entryNum;
        double value = intensityData[i];
        entryNum = (int) ((value - displayMinValue) / range * (numPaletteEntries - 1));

        if (entryNum < 0) {
            entryNum = 0;
        }
        if (entryNum > numPaletteEntries - 1) {
            entryNum = numPaletteEntries - 1;
        }
        Color clr = new Color(paletteData[entryNum]);
        return new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), alpha);
    }

    private void readPalette() {
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        int i;
        try {
            // see if the file exists, if not, set it to the default palette.
            File file = new File(paletteFile);
            if (!file.exists()) {
                return;
            }

            numPaletteEntries = (int) (file.length() / 4);

            buf = ByteBuffer.allocate(numPaletteEntries * 4);

            rIn = new RandomAccessFile(paletteFile, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // Read the data.
            buf.rewind();
            IntBuffer ib = buf.asIntBuffer();
            paletteData = new int[numPaletteEntries];
            ib.get(paletteData);
            ib = null;

            // Update the palette for the alpha value.
            if (alpha < 255) {
                int r, g, b, val;
                for (i = 0; i < numPaletteEntries; i++) {
                    val = paletteData[i];
                    r = (val >> 16) & 0xFF;
                    g = (val >> 8) & 0xFF;
                    b = val & 0xFF;
                    paletteData[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
                }
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
