/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whiteboxgis;

import java.awt.Color;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import whitebox.cartographic.PointMarkers.MarkerStyle;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.DBF.DBFException;
import whitebox.geospatialfiles.shapefile.DBF.DBFField;
import whitebox.geospatialfiles.shapefile.DBF.DBFReader;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.interfaces.MapLayer;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class VectorLayerInfo implements MapLayer {

    private String fileName;
    private ShapeFile shapefile;
    private int overlayNumber;
    private String paletteFile = "";
    private String layerTitle = "";
    private int alpha = 255;
    private double gamma = 1.0;
    private boolean visible = true;
    private BoundingBox fullExtent = null;
    private BoundingBox currentExtent = null;
    private float markerSize = 6.0f;
    private float lineThickness = 1.0f;
    private Color lineColour = Color.black;
    private Color fillColour = Color.red;
    private ShapeType shapeType;
    private boolean filled = true;
    private boolean outlined = true;
    private boolean dashed = false;
    private float[] dashArray = new float[]{4, 4, 12, 4};
    private MarkerStyle markerStyle = MarkerStyle.CIRCLE;
    private String xyUnits = "";
    private String[] attributeTableFields = new String[1];
    private String paletteDirectory;
    private String pathSep;
    private boolean filledWithOneColour = true;
    private boolean outlinedWithOneColour = true;
    private boolean paletteScaled = false;
    private String colouringAttribute = "";
    private int numPaletteEntries = 0;
    private int[] paletteData = null;
    private double minimumValue = 0;
    private double maximumValue = 0;
    private double cartographicGeneralizationLevel = 0.5;
//    private boolean dirty;
    
    public VectorLayerInfo(String fileName, int alpha, int overlayNumber) {
        this.fileName = fileName;
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found.");
        }
        this.layerTitle = file.getName().replace(".shp", "");
        this.alpha = alpha;
        this.overlayNumber = overlayNumber;

        shapefile = new ShapeFile(fileName);

        currentExtent = new BoundingBox(shapefile.getxMin(), shapefile.getyMin(),
                shapefile.getxMax(), shapefile.getyMax());

        fullExtent = currentExtent.clone();
        shapeType = shapefile.getShapeType();
        if (shapeType == ShapeType.POLYLINE && layerTitle.toLowerCase().contains("roads")) {
            lineColour = Color.black;
        } else if (shapeType == ShapeType.POLYLINE || shapeType == ShapeType.POLYLINEM
                || shapeType == ShapeType.POLYLINEZ) {
            lineColour = Color.RED; // new Color(153, 204, 255);
        } else if (shapeType == ShapeType.POLYGON || shapeType == ShapeType.POLYGONM
                || shapeType == ShapeType.POLYGONZ) {
            lineColour = Color.black;
            //fillColour = new Color(153, 204, 255);
            // set the fill colour to a random light colour
            Random generator = new Random();
            int r = (int)(100 + 155 * generator.nextFloat());
            int g = (int)(100 + 155 * generator.nextFloat());
            int b = (int)(100 + 155 * generator.nextFloat());
            fillColour = new Color(r, g, b);
        }
        this.xyUnits = shapefile.getXYUnits();
        this.attributeTableFields = shapefile.getAttributeTableFields();
        
        pathSep = File.separator;
        try {
            String applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                applicationDirectory = new File(applicationDirectory).getParent();
            } else {
                // Add the path to the class files
                applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                // Step one level up as we are only interested in the
                // directory containing the class files
                applicationDirectory = new File(applicationDirectory).getParent();
            }
            findPaletteDirectory(new File(applicationDirectory));
            paletteFile = paletteDirectory + "categorical1.pal";
        } catch (Exception e) {
            
        }
            
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    
    public double getNonlinearity() {
        return gamma;
    }

    public void setNonlinearity(double value) {
        gamma = value;
    }

    public String getPaletteFile() {
        return paletteFile;
    }

    public void setPaletteFile(String fileName) {
        if (!paletteFile.equals(fileName)) {
            paletteFile = fileName;
        }
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public ArrayList<ShapeFileRecord> getData() {
        return recs;
        
    }

    public float getMarkerSize() {
        return markerSize;
    }

    public void setMarkerSize(float markerSize) {
        this.markerSize = markerSize;
    }

    public float getLineThickness() {
        return lineThickness;
    }

    public void setLineThickness(float lineThickness) {
        this.lineThickness = lineThickness;
    }

    public Color getFillColour() {
        return fillColour;
    }

    public void setFillColour(Color fillColour) {
        this.fillColour = fillColour;
        //setRecordsColourData();
    }

    public ShapeFile getShapefile() {
        return shapefile;
    }

    public Color getLineColour() {
        return lineColour;
    }

    public void setLineColour(Color lineColour) {
        this.lineColour = lineColour;
        setRecordsColourData();
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public boolean isOutlined() {
        return outlined;
    }

    public void setOutlined(boolean outlined) {
        this.outlined = outlined;
    }
    
    public void setMarkerStyle(MarkerStyle markerStyle) {
        this.markerStyle = markerStyle;
    }

    public MarkerStyle getMarkerStyle() {
        return markerStyle;
    }

    public String getXYUnits() {
        return xyUnits;
    }

    public void setXYUnits(String xyUnits) {
        this.xyUnits = xyUnits;
    }

    public String[] getAttributeTableFields() {
        return attributeTableFields;
    }
    
    public double getCartographicGeneralizationLevel() {
        return cartographicGeneralizationLevel;
    }
    
    boolean generalizationLevelDirty = false;
    public void setCartographicGeneralizationLevel(double generalizeLevel) {
        cartographicGeneralizationLevel = generalizeLevel;
        generalizationLevelDirty = true;
    }
    
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
        return MapLayerType.VECTOR;
    }

    @Override
    public BoundingBox getFullExtent() {
        return fullExtent.clone();
    }

    
    @Override
    public BoundingBox getCurrentExtent() {
        return currentExtent.clone();
    }

    ArrayList<ShapeFileRecord> recs;
    
    @Override
    public final void setCurrentExtent(BoundingBox bb) {
        if (!bb.equals(currentExtent)) {
            currentExtent = bb.clone();
        }
    }
    
    public final void setCurrentExtent(BoundingBox bb, double minSize) {
        if (!bb.equals(currentExtent) || recs == null || generalizationLevelDirty) {
            currentExtent = bb.clone();
            recs = shapefile.getRecordsInBoundingBox(currentExtent, minSize);
        }
        generalizationLevelDirty = false;
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

    public boolean isDashed() {
        return dashed;
    }

    public void setDashed(boolean dashed) {
        this.dashed = dashed;
    }

    public float[] getDashArray() {
        return dashArray;
    }

    public void setDashArray(float[] dashArray) {
        this.dashArray = dashArray;
    }

    public boolean isFilledWithOneColour() {
        return filledWithOneColour;
    }

    public void setFilledWithOneColour(boolean filledWithOneColour) {
        this.filledWithOneColour = filledWithOneColour;
    }

    public boolean isOutlinedWithOneColour() {
        return outlinedWithOneColour;
    }

    public void setOutlinedWithOneColour(boolean outlinedWithOneColour) {
        this.outlinedWithOneColour = outlinedWithOneColour;
    }

    public boolean isPaletteScaled() {
        return paletteScaled;
    }

    public void setPaletteScaled(boolean paletteScaled) {
        this.paletteScaled = paletteScaled;
    }

    public String getFillAttribute() {
        return colouringAttribute;
    }

    public void setFillAttribute(String fillAttribute) {
        this.colouringAttribute = fillAttribute;
        //setRecordsColourData();
    }
    
    public String getLineAttribute() {
        return colouringAttribute;
    }

    public void setLineAttribute(String lineAttribute) {
        this.colouringAttribute = lineAttribute;
        //setRecordsColourData();
    }
    
    private Color[] colourData;
    public Color[] getColourData() {
        if (colourData == null) {
            setRecordsColourData();
            
        }
        return colourData;
    }

    public LegendEntry[] getLegendEntries() {
        return legendEntries;
    }

    public double getMaximumValue() {
        return maximumValue;
    }

    public double getMinimumValue() {
        return minimumValue;
    }
    
    // methods
    private LegendEntry[] legendEntries;
    public void setRecordsColourData() {
        int numRecords = shapefile.getNumberOfRecords();
        int entryNum, a, i;
        int r1, g1, b1;
        int a1 = this.getAlpha();
        //double nullDataFlag = Integer.MIN_VALUE;
        Color legendColour;
        colourData = new Color[numRecords];
        
        boolean singleColour = true;
        Color clr;
        if (shapeType == ShapeType.POLYLINE || shapeType == ShapeType.POLYLINEM
                    || shapeType == ShapeType.POLYLINEZ) {
            singleColour = outlinedWithOneColour;
            clr = lineColour;
        } else {
            singleColour = filledWithOneColour;
            clr = fillColour;
        }
        
        if (singleColour) {
            clr = new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), a1);
            for (i = 0; i < numRecords; i++) {
                colourData[i] = clr;
            }
            legendEntries = new LegendEntry[1];
            legendEntries[0] = new LegendEntry(this.layerTitle, clr);
        } else {
            // read the palette data
            readPalette();

            // find the data type of the fill attribute
            String dbfFileName = fileName.replace(".shp", ".dbf");

            try {
                DBFReader dbfReader = new DBFReader(dbfFileName);
                DBFField[] fields = dbfReader.getAllFields();
                byte dataType = 0;
                int fieldNum = -1;
                for (a = 0; a < fields.length; a++) {
                    if (fields[a].getName().equals(colouringAttribute)) {
                        dataType = fields[a].getDataType();
                        fieldNum = a;
                        break;
                    }
                }
                // read the records
                Object[][] data = new Object[numRecords][3];
                Object[] rec;
                a = 0;
                while ((rec = dbfReader.nextRecord()) != null) {
                    data[a][0] = a;
                    if (rec[fieldNum] != null) {
                        data[a][1] = rec[fieldNum];
                    } else {
                        data[a][1] = null; //new Double(nullDataFlag);
                    }
                    a++;
                }
                
                if (!(dataType == 'N') && !(dataType == 'F')) {
                    // sort the data based on the field data
                    Arrays.sort(data, new Comparator<Object[]>() {

                        @Override
                        public int compare(final Object[] entry1, final Object[] entry2) {
                            // still need to handle date and boolean data types.
                            if (entry1[1] instanceof Integer) {
                                final int val1 = (Integer) entry1[1];
                                final int val2 = (Integer) entry2[1];
                                return Integer.valueOf(val1).compareTo(val2);
                            } else if (entry1[1] instanceof Double) {
                                final double val1 = (Double) entry1[1];
                                final double val2 = (Double) entry2[1];
                                return Double.valueOf(val1).compareTo(val2);
                            } else if (entry1[1] instanceof Float) {
                                final float val1 = (Float) entry1[1];
                                final float val2 = (Float) entry2[1];
                                return Float.valueOf(val1).compareTo(val2);
                            } else if (entry1[1] instanceof String) {
                                final String val1 = (String) entry1[1];
                                final String val2 = (String) entry2[1];
                                return String.valueOf(val1).compareTo(val2);
                            } else if (entry1[1] instanceof Date) {
                                final Date val1 = (Date) entry1[1];
                                final Date val2 = (Date) entry2[1];
                                return val1.compareTo(val2);
                            } else {
                                final double val1 = (Double) entry1[1];
                                final double val2 = (Double) entry2[1];
                                return Double.valueOf(val1).compareTo(val2);
                            }
                        }
                    });

                    int clrNumber = 0;
                    data[0][2] = clrNumber;
                    if (numRecords > 1) {
                        for (i = 1; i < numRecords; i++) {
                            if (data[i][1].equals(data[i - 1][1])) {
                                data[i][2] = clrNumber;
                            } else {
                                clrNumber++;
                                data[i][2] = clrNumber;
                            }
                        }
                    }
                    
                    // figure out the legend entries.
                    legendEntries = new LegendEntry[clrNumber + 1];
                    entryNum = (Integer) (data[0][2]) % numPaletteEntries;
                    if (data[0][1] == null) {
                        legendEntries[0] = new LegendEntry("Null", new Color(255, 255, 255, 0));
                    } else {
                        legendEntries[0] = new LegendEntry(String.valueOf(data[0][1]), new Color(paletteData[entryNum]));
                    }
                    int legendEntryNum = 1;
                    double maxValue = legendEntries.length - 1;
                    if (numRecords > 1) {
                        for (i = 1; i < numRecords; i++) {
                            if (!data[i][1].equals(data[i - 1][1])) {
                                if (!paletteScaled) {
                                    entryNum = (Integer)(data[i][2]) % numPaletteEntries;
                                } else {
                                    entryNum = (int) (((Integer)data[i][2] / maxValue) * (numPaletteEntries - 1));
                                }
                                if (data[i][1] == null) {
                                    legendEntries[legendEntryNum] = new LegendEntry("Null", new Color(255, 255, 255, 0));
                                    
                                } else {
                                    legendEntries[legendEntryNum] = new LegendEntry(String.valueOf(data[i][1]), new Color(paletteData[entryNum]));
                                }
                                legendEntryNum++;
                            }
                        }
                    }
                    
                    // sort it back into the record number order.
                    Arrays.sort(data, new Comparator<Object[]>() {

                        @Override
                        public int compare(final Object[] entry1, final Object[] entry2) {
                            final int int1 = (Integer) entry1[0];
                            final int int2 = (Integer) entry2[0];
                            return Integer.valueOf(int1).compareTo(int2);
                        }
                    });

                    // fill the colourData array.
                        
                    if (!paletteScaled) {
                        for (i = 0; i < numRecords; i++) {
                            if (data[i][1] == null) {
                                colourData[i] = new Color(255, 255, 255, 0);
                            } else {
                                clrNumber = (Integer) (data[i][2]);
                                entryNum = clrNumber % numPaletteEntries;
                                legendColour = new Color(paletteData[entryNum]);
                                r1 = legendColour.getRed();
                                g1 = legendColour.getGreen();
                                b1 = legendColour.getBlue();
                                colourData[i] = new Color(r1, g1, b1, a1);
                            }
                        }
                    } else { // the palette is scaled
                        int value = 0;
                        //double maxValue = legendEntries.length - 1;
                        for (i = 0; i < numRecords; i++) {
                            if (data[i][1] == null) {
                                colourData[i] = new Color(255, 255, 255, 0);
                            } else {
                                value = (Integer) data[i][2];
                                entryNum = (int) ((value / maxValue) * (numPaletteEntries - 1));
                                legendColour = new Color(paletteData[entryNum]);
                                r1 = legendColour.getRed();
                                g1 = legendColour.getGreen();
                                b1 = legendColour.getBlue();
                                colourData[i] = new Color(r1, g1, b1, a1);
                            }
                        }
                    }
                } else {  // it's a number
                    if (paletteScaled) {
                        // it's a numerical field
                        
                        legendEntries = new LegendEntry[1];
                        legendEntries[0] = new LegendEntry("continuous numerical variable", Color.black);
                        // find the min and max values
                        double minValue = Float.POSITIVE_INFINITY;
                        double maxValue = Float.NEGATIVE_INFINITY;
                        if (numRecords > 1) {
                            for (i = 0; i < numRecords; i++) {
                                if (data[i][1] == null) {
                                    // do nothing
                                } else {
                                    if ((Double)data[i][1] > maxValue) {
                                        maxValue = (Double)data[i][1];
                                    }
                                }
                                if (data[i][1] == null) {
                                    // do nothing
                                } else {
                                    if ((Double)data[i][1] < minValue) {
                                        minValue = (Double)data[i][1];
                                    }
                                }
                            }
                            double range = maxValue - minValue;
                            double value;
                            for (i = 0; i < numRecords; i++) {
                                if (data[i][1] == null) {
                                    colourData[i] = new Color(255, 255, 255, 0);
                                } else {
                                    value = (Double) data[i][1];
                                    entryNum = (int) (((value - minValue) / range) * (numPaletteEntries - 1));
                                    clr = new Color(paletteData[entryNum]);
                                    colourData[i] = new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), a1);
                                }
                            }
                            
                            minimumValue = minValue;
                            maximumValue = maxValue;
                            
                        } else {
                            colourData[0] = new Color(paletteData[0]);
                        }
                        
                    } else {
                        // it's not scaled
                        
                        // sort the data based on the field data
                        Arrays.sort(data, new Comparator<Object[]>() {

                            @Override
                            public int compare(final Object[] entry1, final Object[] entry2) {
                                // still need to handle date and boolean data types.
                                if (entry1[1] instanceof Integer) {
                                    final int val1 = (Integer) entry1[1];
                                    final int val2 = (Integer) entry2[1];
                                    return Integer.valueOf(val1).compareTo(val2);
                                } else if (entry1[1] instanceof Double) {
                                    final double val1 = (Double) entry1[1];
                                    final double val2 = (Double) entry2[1];
                                    return Double.valueOf(val1).compareTo(val2);
                                } else if (entry1[1] instanceof Float) {
                                    final float val1 = (Float) entry1[1];
                                    final float val2 = (Float) entry2[1];
                                    return Float.valueOf(val1).compareTo(val2);
                                } else if (entry1[1] instanceof String) {
                                    final String val1 = (String) entry1[1];
                                    final String val2 = (String) entry2[1];
                                    return String.valueOf(val1).compareTo(val2);
                                } else if (entry1[1] instanceof Date) {
                                    final Date val1 = (Date) entry1[1];
                                    final Date val2 = (Date) entry2[1];
                                    return val1.compareTo(val2);
                                } else {
                                    final double val1 = (Double) entry1[1];
                                    final double val2 = (Double) entry2[1];
                                    return Double.valueOf(val1).compareTo(val2);
                                }

                            }
                        });

                        int clrNumber = 0;
                        data[0][2] = clrNumber;
                        if (numRecords > 1) {
                            for (i = 1; i < numRecords; i++) {
                                if (data[i][1].equals(data[i - 1][1])) {
                                    data[i][2] = clrNumber;
                                } else {
                                    clrNumber++;
                                    data[i][2] = clrNumber;
                                }
                            }
                        }

                        // figure out the legend entries.
                        legendEntries = new LegendEntry[clrNumber + 1];
                        entryNum = (Integer) (data[0][2]) % numPaletteEntries;
                        if (data[0][1] == null) {
                            legendEntries[0] = new LegendEntry("Null", new Color(255, 255, 255, 0));
                        } else {
                            legendEntries[0] = new LegendEntry(String.valueOf(data[0][1]), new Color(paletteData[entryNum]));
                        }
                        int legendEntryNum = 1;
                        if (numRecords > 1) {
                            for (i = 1; i < numRecords; i++) {
                                if (!data[i][1].equals(data[i - 1][1])) {
                                    entryNum = (Integer) (data[i][2]) % numPaletteEntries;
                                    if (data[i][1] == null) {
                                        legendEntries[legendEntryNum] = new LegendEntry("Null", new Color(255, 255, 255, 0));
                                    } else {
                                        legendEntries[legendEntryNum] = new LegendEntry(String.valueOf(data[i][1]), new Color(paletteData[entryNum]));
                                    }
                                    legendEntryNum++;
                                }
                            }
                        }

                        // sort it back into the record number order.
                        Arrays.sort(data, new Comparator<Object[]>() {

                            @Override
                            public int compare(final Object[] entry1, final Object[] entry2) {
                                final int int1 = (Integer) entry1[0];
                                final int int2 = (Integer) entry2[0];
                                return Integer.valueOf(int1).compareTo(int2);
                            }
                        });

                        // fill the colourData array.
                        for (i = 0; i < numRecords; i++) {
                            if (data[i][1] == null) {
                                colourData[i] = new Color(255, 255, 255, 0);
                            } else {
                                clrNumber = (Integer) (data[i][2]);
                                entryNum = clrNumber % numPaletteEntries;
                                legendColour = new Color(paletteData[entryNum]);
                                r1 = legendColour.getRed();
                                g1 = legendColour.getGreen();
                                b1 = legendColour.getBlue();
                                colourData[i] = new Color(r1, g1, b1, a1);
                            }
                        }

                    }
                }
                
            } catch (DBFException dbfe) {
                System.out.println(dbfe.getMessage());

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            
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
    
    public class LegendEntry {
        Color legendColour;
        String legendLabel;
        protected LegendEntry(String label, Color clr) {
            legendLabel = label;
            legendColour = clr;
        }

        public Color getLegendColour() {
            return legendColour;
        }

        public String getLegendLabel() {
            return legendLabel;
        }
        
        
    }
}
