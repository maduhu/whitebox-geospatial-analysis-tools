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

import java.awt.Point;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import whitebox.cartographic.PointMarkers;
import whitebox.geospatialfiles.shapefile.*;
import whitebox.interfaces.MapLayer.MapLayerType;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.structures.BoundingBox;
import whitebox.structures.GridCell;
import whitebox.utilities.OSFinder;

/**
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MapRenderer extends JPanel implements Printable, MouseMotionListener,
        MouseListener, ImageObserver {

    private int borderWidth = 20;
    private BoundingBox mapExtent = new BoundingBox();
    public MapInfo mapinfo = null;
    private StatusBar status = null;
    private WhiteboxPluginHost host = null;
    public static final int MOUSE_MODE_ZOOM = 0;
    public static final int MOUSE_MODE_PAN = 1;
    public static final int MOUSE_MODE_GETINFO = 2;
    public static final int MOUSE_MODE_SELECT = 3;
    private int myMode = 0;
    private boolean modifyingPixels = false;
    private int modifyPixelsX = -1;
    private int modifyPixelsY = -1;
    private boolean usingDistanceTool = false;
    private Cursor zoomCursor = null;
    private Cursor panCursor = null;
    private Cursor panClosedHandCursor = null;
    private Cursor selectCursor = null;
    private String graphicsDirectory;

    public MapRenderer() {
        init();
    }

    private void init() {
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
            graphicsDirectory = applicationDirectory + File.separator
                    + "resources" + File.separator + "Images" + File.separator;

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Point cursorHotSpot = new Point(0, 0);
            if (!OSFinder.isWindows()) {
                zoomCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "ZoomToBoxCursor.png"), cursorHotSpot, "zoomCursor");
                panCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "Pan3.png"), cursorHotSpot, "panCursor");
                panClosedHandCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "Pan4.png"), cursorHotSpot, "panCursor");
                selectCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "select.png"), cursorHotSpot, "selectCursor");
            } else {
                // windows requires 32 x 32 cursors. Otherwise they look terrible.
                zoomCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "ZoomToBoxCursorWin.png"), cursorHotSpot, "zoomCursor");
                panCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "Pan3Win.png"), cursorHotSpot, "panCursor");
                panClosedHandCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "Pan4Win.png"), cursorHotSpot, "panCursor");
                selectCursor = toolkit.createCustomCursor(toolkit.getImage(graphicsDirectory + "selectWin.png"), cursorHotSpot, "selectCursor");
            }
            this.setCursor(zoomCursor);
            this.addMouseMotionListener(this);
            this.addMouseListener(this);
        } catch (Exception e) {
        }
    }

    public MapInfo getMapInfo() {
        return mapinfo;
    }

    public void setMapInfo(MapInfo mapinfo) {
        this.mapinfo = mapinfo;
    }

    public void setStatusBar(StatusBar status) {
        this.status = status;
    }

    public int getMouseMode() {
        return myMode;
    }

    public void setMouseMode(int mouseMode) {
        myMode = mouseMode;

        switch (myMode) {
            case MOUSE_MODE_ZOOM:
                this.setCursor(zoomCursor);
                break;
            case MOUSE_MODE_PAN:
                this.setCursor(panCursor);
                break;
            case MOUSE_MODE_SELECT:
                this.setCursor(selectCursor);
                break;
        }

    }

    public void setHost(WhiteboxPluginHost host) {
        this.host = host;
    }

    public void setModifyingPixels(boolean val) {
        modifyingPixels = val;
    }

    public boolean isModifyingPixels() {
        return modifyingPixels;
    }

    public void setUsingDistanceTool(boolean val) {
        usingDistanceTool = val;
    }

    public boolean isUsingDistanceTool() {
        return usingDistanceTool;
    }

    @Override
    public void paint(Graphics g) {
        if (mapinfo.isCartoView()) {
            drawMapCartoView(g);
        } else {
            drawMapDataView(g);
        }
    }

    private void drawMapCartoView(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
            rh = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);

            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            if (mapinfo != null) {

                // get the drawing area's width and height
                double myWidth = this.getWidth();
                double myHeight = this.getHeight();


                // get the page size information
                PageFormat pageFormat = mapinfo.getPageFormat();
                double pageWidth = pageFormat.getWidth();// / 72.0;
                double pageHeight = pageFormat.getHeight();// / 72.0;
                //double pageAspect = pageWidth / pageHeight;

                // set the scale
                int pageShadowSize = 5;
                double scale = Math.min(((myWidth - 4.0 * pageShadowSize) / pageWidth),
                        ((myHeight - 4.0 * pageShadowSize) / pageHeight));

                // what are the margins of the page on the drawing area?

                double pageTop = myHeight / 2.0 - pageHeight / 2.0 * scale;
                double pageLeft = myWidth / 2.0 - pageWidth / 2.0 * scale;

                // draw the page on the drawing area if it is visible
                if (mapinfo.isCartoView()) {

                    g2d.setColor(Color.GRAY);
                    g2d.fillRect((int) (pageLeft + pageShadowSize),
                            (int) (pageTop + pageShadowSize), (int) (pageWidth * scale),
                            (int) (pageHeight * scale));
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect((int) pageLeft, (int) pageTop, (int) (pageWidth * scale),
                            (int) (pageHeight * scale));

                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawRect((int) pageLeft, (int) pageTop, (int) (pageWidth * scale),
                            (int) (pageHeight * scale));

                }

                int numLayers = mapinfo.getNumLayers();
                if (numLayers == 0) {
                    return;
                }

            }
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
            //System.out.println(e.getMessage());
        }
    }

    private void drawMapDataView(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
            rh = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);

            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            if (mapinfo != null) {
                int numLayers = mapinfo.getNumLayers();
                if (numLayers == 0) {
                    return;
                }

                double myWidth = this.getWidth() - borderWidth * 2;
                double myHeight = this.getHeight() - borderWidth * 2;
                int width, height;
                double scale;
                int x, y;

                BoundingBox currentExtent = mapinfo.getCurrentExtent();
                double xRange = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
                double yRange = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
                scale = Math.min((myWidth / xRange), (myHeight / yRange));

                int left = (int) (borderWidth + ((myWidth - xRange * scale) / 2));
                int top = (int) (borderWidth + ((myHeight - yRange * scale) / 2));

                // what are the edge coordinates of the actual map area
                double deltaY = (top - borderWidth) / scale;
                double deltaX = (left - borderWidth) / scale;
                mapExtent.setMaxY(currentExtent.getMaxY() + deltaY);
                mapExtent.setMinY(currentExtent.getMinY() - deltaY);
                mapExtent.setMinX(currentExtent.getMinX() - deltaX);
                mapExtent.setMaxX(currentExtent.getMaxX() + deltaX);


                String XYUnits = "";

                for (int i = 0; i < numLayers; i++) {
                    if (mapinfo.getLayer(i).getLayerType() == MapLayerType.RASTER) {
                        RasterLayerInfo layer = (RasterLayerInfo) mapinfo.getLayer(i);
                        if (layer.getXYUnits().toLowerCase().contains("met")) {
                            XYUnits = " m";
                        } else if (layer.getXYUnits().toLowerCase().contains("deg")) {
                            XYUnits = "\u00B0";
                        } else if (!layer.getXYUnits().toLowerCase().contains("not specified")) {
                            XYUnits = " " + layer.getXYUnits();
                        }

                        if (layer.isVisible()) {

                            BoundingBox fe = layer.getFullExtent();
                            if (fe.doesIntersect(mapExtent)) {
                                BoundingBox layerCE = fe.intersect(mapExtent);
                                layer.setCurrentExtent(layerCE);
                                x = (int) (left + (layerCE.getMinX() - currentExtent.getMinX()) * scale);
                                int layerWidth = (int) ((Math.abs(layerCE.getMaxX() - layerCE.getMinX())) * scale);
                                y = (int) (top + (currentExtent.getMaxY() - layerCE.getMaxY()) * scale);
                                int layerHeight = (int) ((Math.abs(layerCE.getMaxY() - layerCE.getMinY())) * scale);

                                int startR = (int) (Math.abs(layer.fullExtent.getMaxY() - layerCE.getMaxY()) / layer.getCellSizeY());
                                int endR = (int) (layer.getNumberRows() - (Math.abs(layer.fullExtent.getMinY() - layerCE.getMinY()) / layer.getCellSizeY()));
                                int startC = (int) (Math.abs(layer.fullExtent.getMinX() - layerCE.getMinX()) / layer.getCellSizeX());
                                int endC = (int) (layer.getNumberColumns() - (Math.abs(layer.fullExtent.getMaxX() - layerCE.getMaxX()) / layer.getCellSizeX()));
                                int numRows = endR - startR;
                                int numCols = endC - startC;

                                int res = (int) (Math.min(numRows / (double) layerHeight, numCols / (double) layerWidth));

                                layer.setResolutionFactor(res);

                                if (layer.isDirty()) {
                                    layer.createPixelData();
                                }

                                width = layer.getImageWidth();
                                height = layer.getImageHeight();
                                Image image = createImage(new MemoryImageSource(width, height, layer.getPixelData(), 0, width));
                                if (!g2d.drawImage(image, x, y, layerWidth, layerHeight, this)) {
                                    // do nothing
                                }

                            }
                        }
                    } else if (mapinfo.getLayer(i).getLayerType() == MapLayerType.VECTOR) {
                        VectorLayerInfo layer = (VectorLayerInfo) mapinfo.getLayer(i);
                        if (layer.getXYUnits().toLowerCase().contains("met")) {
                            XYUnits = " m";
                        } else if (layer.getXYUnits().toLowerCase().contains("deg")) {
                            XYUnits = "\u00B0";
                        } else if (!layer.getXYUnits().toLowerCase().contains("not specified")) {
                            XYUnits = " " + layer.getXYUnits();
                        }
                        /*
                         * minDistinguishableLength is used to speed up the
                         * drawing of vectors. Any feature that is smaller than
                         * this value will be excluded from the map. This is an
                         * example of cartographic generalization.
                         */
                        double minDistinguishableLength = layer.getCartographicGeneralizationLevel() / scale;

                        int r;

                        if (layer.isVisible()) {
                            BoundingBox fe = layer.getFullExtent();
                            if (fe.doesIntersect(mapExtent)) {
                                BoundingBox layerCE = fe.intersect(mapExtent);
                                layer.setCurrentExtent(layerCE, minDistinguishableLength);
                                int a1 = layer.getAlpha();
                                //Color fillColour = new Color(r1, g1, b1, a1);
                                int r1 = layer.getLineColour().getRed();
                                int g1 = layer.getLineColour().getGreen();
                                int b1 = layer.getLineColour().getBlue();
                                Color lineColour = new Color(r1, g1, b1, a1);

                                ShapeType shapeType = layer.getShapeType();
                                //ShapeFileRecord[] records = layer.getGeometry();
                                ArrayList<ShapeFileRecord> records = layer.getData();
                                double x1, y1;
                                //int xInt, yInt, x2Int, y2Int;
                                double topCoord = mapExtent.getMaxY();
                                double bottomCoord = mapExtent.getMinY();
                                double leftCoord = mapExtent.getMinX();
                                double rightCoord = mapExtent.getMaxX();
                                double EWRange = rightCoord - leftCoord;
                                double NSRange = topCoord - bottomCoord;

                                double[][] xyData;
                                GeneralPath gp;
                                BasicStroke myStroke;
                                Stroke oldStroke;
                                Color[] colours = layer.getColourData();
                                boolean isFilled = layer.isFilled();
                                boolean isOutlined = layer.isOutlined();
                                double[][] recPoints;

                                int[] partStart;
                                double[][] points;
                                int pointSt;
                                int pointEnd;
                                float xPoints[];
                                float yPoints[];
                                GeneralPath polyline;

                                switch (shapeType) {

                                    case POINT:
                                        xyData = PointMarkers.getMarkerData(layer.getMarkerStyle(), layer.getMarkerSize());
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                whitebox.geospatialfiles.shapefile.Point rec = (whitebox.geospatialfiles.shapefile.Point) (record.getGeometry());
                                                x1 = rec.getX();
                                                y1 = rec.getY();
                                                if (y1 < bottomCoord || x1 < leftCoord
                                                        || y1 > topCoord || x1 > rightCoord) {
                                                    // It's not within the map area; do nothing.
                                                } else {
                                                    x1 = (borderWidth + (x1 - leftCoord) / EWRange * myWidth);
                                                    y1 = (borderWidth + (topCoord - y1) / NSRange * myHeight);
                                                    gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                                                    for (int a = 0; a < xyData.length; a++) {
                                                        if (xyData[a][0] == 0) { // moveTo
                                                            gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                        } else if (xyData[a][0] == 1) { // lineTo
                                                            gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                        } else if (xyData[a][0] == 2) { // elipse2D
                                                            Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);

                                                            gp.append(circle, true);
                                                        }
                                                    }
                                                    //circle = new Ellipse2D.Double((x1 - halfMS), (y1 - halfMS), markerSize, markerSize);
                                                    if (isFilled) {
                                                        g2d.setColor(colours[r]);
                                                        g2d.fill(gp);
                                                    }
                                                    if (isOutlined) {
                                                        g2d.setColor(lineColour);
                                                        g2d.draw(gp);
                                                    }

                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case POINTZ:
                                        xyData = PointMarkers.getMarkerData(layer.getMarkerStyle(), layer.getMarkerSize());
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                PointZ rec = (PointZ) (record.getGeometry());
                                                x1 = rec.getX();
                                                y1 = rec.getY();
                                                if (y1 < bottomCoord || x1 < leftCoord
                                                        || y1 > topCoord || x1 > rightCoord) {
                                                    // It's not within the map area; do nothing.
                                                } else {
                                                    x1 = (borderWidth + (x1 - leftCoord) / EWRange * myWidth);
                                                    y1 = (borderWidth + (topCoord - y1) / NSRange * myHeight);
                                                    gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                                                    for (int a = 0; a < xyData.length; a++) {
                                                        if (xyData[a][0] == 0) { // moveTo
                                                            gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                        } else if (xyData[a][0] == 1) { // lineTo
                                                            gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                        } else if (xyData[a][0] == 2) { // elipse2D
                                                            Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);

                                                            gp.append(circle, true);
                                                        }
                                                    }
                                                    //circle = new Ellipse2D.Double((x1 - halfMS), (y1 - halfMS), markerSize, markerSize);
                                                    if (isFilled) {
                                                        g2d.setColor(colours[r]);
                                                        g2d.fill(gp);
                                                    }
                                                    if (isOutlined) {
                                                        g2d.setColor(lineColour);
                                                        g2d.draw(gp);
                                                    }

                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case POINTM:
                                        xyData = PointMarkers.getMarkerData(layer.getMarkerStyle(), layer.getMarkerSize());
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                PointM rec = (PointM) (record.getGeometry());
                                                x1 = rec.getX();
                                                y1 = rec.getY();
                                                if (y1 < bottomCoord || x1 < leftCoord
                                                        || y1 > topCoord || x1 > rightCoord) {
                                                    // It's not within the map area; do nothing.
                                                } else {
                                                    x1 = (borderWidth + (x1 - leftCoord) / EWRange * myWidth);
                                                    y1 = (borderWidth + (topCoord - y1) / NSRange * myHeight);
                                                    gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                                                    for (int a = 0; a < xyData.length; a++) {
                                                        if (xyData[a][0] == 0) { // moveTo
                                                            gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                        } else if (xyData[a][0] == 1) { // lineTo
                                                            gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                        } else if (xyData[a][0] == 2) { // elipse2D
                                                            Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);

                                                            gp.append(circle, true);
                                                        }
                                                    }
                                                    //circle = new Ellipse2D.Double((x1 - halfMS), (y1 - halfMS), markerSize, markerSize);
                                                    if (isFilled) {
                                                        g2d.setColor(colours[r]);
                                                        g2d.fill(gp);
                                                    }
                                                    if (isOutlined) {
                                                        g2d.setColor(lineColour);
                                                        g2d.draw(gp);
                                                    }

                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case MULTIPOINT:
                                        xyData = PointMarkers.getMarkerData(layer.getMarkerStyle(), layer.getMarkerSize());
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                MultiPoint rec = (MultiPoint) (record.getGeometry());
                                                recPoints = rec.getPoints();
                                                for (int p = 0; p < recPoints.length; p++) {
                                                    x1 = recPoints[p][0];
                                                    y1 = recPoints[p][1];
                                                    if (y1 < bottomCoord || x1 < leftCoord
                                                            || y1 > topCoord || x1 > rightCoord) {
                                                        // It's not within the map area; do nothing.
                                                    } else {
                                                        x1 = (borderWidth + (x1 - leftCoord) / EWRange * myWidth);
                                                        y1 = (borderWidth + (topCoord - y1) / NSRange * myHeight);

                                                        gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                                                        for (int a = 0; a < xyData.length; a++) {
                                                            if (xyData[a][0] == 0) { // moveTo
                                                                gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                            } else if (xyData[a][0] == 1) { // lineTo
                                                                gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                            } else if (xyData[a][0] == 2) { // elipse2D
                                                                Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);

                                                                gp.append(circle, true);
                                                            }
                                                        }
                                                        if (isFilled) {
                                                            g2d.setColor(colours[r]);
                                                            g2d.fill(gp);
                                                        }
                                                        if (isOutlined) {
                                                            g2d.setColor(lineColour);
                                                            g2d.draw(gp);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case MULTIPOINTZ:
                                        xyData = PointMarkers.getMarkerData(layer.getMarkerStyle(), layer.getMarkerSize());
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                MultiPointZ rec = (MultiPointZ) (record.getGeometry());
                                                recPoints = rec.getPoints();
                                                for (int p = 0; p < recPoints.length; p++) {
                                                    x1 = recPoints[p][0];
                                                    y1 = recPoints[p][1];
                                                    if (y1 < bottomCoord || x1 < leftCoord
                                                            || y1 > topCoord || x1 > rightCoord) {
                                                        // It's not within the map area; do nothing.
                                                    } else {
                                                        x1 = (borderWidth + (x1 - leftCoord) / EWRange * myWidth);
                                                        y1 = (borderWidth + (topCoord - y1) / NSRange * myHeight);

                                                        gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                                                        for (int a = 0; a < xyData.length; a++) {
                                                            if (xyData[a][0] == 0) { // moveTo
                                                                gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                            } else if (xyData[a][0] == 1) { // lineTo
                                                                gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                            } else if (xyData[a][0] == 2) { // elipse2D
                                                                Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);

                                                                gp.append(circle, true);
                                                            }
                                                        }
                                                        if (isFilled) {
                                                            g2d.setColor(colours[r]);
                                                            g2d.fill(gp);
                                                        }
                                                        if (isOutlined) {
                                                            g2d.setColor(lineColour);
                                                            g2d.draw(gp);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case MULTIPOINTM:
                                        xyData = PointMarkers.getMarkerData(layer.getMarkerStyle(), layer.getMarkerSize());
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                MultiPointM rec = (MultiPointM) (record.getGeometry());
                                                recPoints = rec.getPoints();
                                                for (int p = 0; p < recPoints.length; p++) {
                                                    x1 = recPoints[p][0];
                                                    y1 = recPoints[p][1];
                                                    if (y1 < bottomCoord || x1 < leftCoord
                                                            || y1 > topCoord || x1 > rightCoord) {
                                                        // It's not within the map area; do nothing.
                                                    } else {
                                                        x1 = (borderWidth + (x1 - leftCoord) / EWRange * myWidth);
                                                        y1 = (borderWidth + (topCoord - y1) / NSRange * myHeight);

                                                        gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 1);
                                                        for (int a = 0; a < xyData.length; a++) {
                                                            if (xyData[a][0] == 0) { // moveTo
                                                                gp.moveTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                            } else if (xyData[a][0] == 1) { // lineTo
                                                                gp.lineTo(x1 + xyData[a][1], y1 + xyData[a][2]);
                                                            } else if (xyData[a][0] == 2) { // elipse2D
                                                                Ellipse2D circle = new Ellipse2D.Double((x1 - xyData[a][1]), (y1 - xyData[a][1]), xyData[a][2], xyData[a][2]);

                                                                gp.append(circle, true);
                                                            }
                                                        }
                                                        if (isFilled) {
                                                            g2d.setColor(colours[r]);
                                                            g2d.fill(gp);
                                                        }
                                                        if (isOutlined) {
                                                            g2d.setColor(lineColour);
                                                            g2d.draw(gp);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case POLYLINE:
                                        //g2d.setColor(lineColour);
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        if (layer.isDashed()) {
                                            myStroke =
                                                    new BasicStroke(layer.getLineThickness(),
                                                    BasicStroke.CAP_BUTT,
                                                    BasicStroke.JOIN_MITER,
                                                    10.0f, layer.getDashArray(), 0.0f);
                                        }
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                PolyLine rec = (PolyLine) (record.getGeometry());
                                                partStart = rec.getParts();
                                                points = rec.getPoints();
                                                for (int p = 0; p < rec.getNumParts(); p++) {
                                                    pointSt = partStart[p];
                                                    if (p < rec.getNumParts() - 1) {
                                                        pointEnd = partStart[p + 1];
                                                    } else {
                                                        pointEnd = points.length;
                                                    }
                                                    xPoints = new float[pointEnd - pointSt];
                                                    yPoints = new float[pointEnd - pointSt];
                                                    for (int k = pointSt; k < pointEnd; k++) {
                                                        xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                        yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                    }
                                                    polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);

                                                    polyline.moveTo(xPoints[0], yPoints[0]);

                                                    for (int index = 1; index < xPoints.length; index++) {
                                                        polyline.lineTo(xPoints[index], yPoints[index]);
                                                    }
                                                    g2d.setColor(colours[r]);
                                                    g2d.draw(polyline);
                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case POLYLINEZ:
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        if (layer.isDashed()) {
                                            myStroke =
                                                    new BasicStroke(layer.getLineThickness(),
                                                    BasicStroke.CAP_BUTT,
                                                    BasicStroke.JOIN_MITER,
                                                    10.0f, layer.getDashArray(), 0.0f);
                                        }
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);
                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                PolyLineZ rec = (PolyLineZ) (record.getGeometry());
                                                partStart = rec.getParts();
                                                points = rec.getPoints();
                                                for (int p = 0; p < rec.getNumParts(); p++) {
                                                    pointSt = partStart[p];
                                                    if (p < rec.getNumParts() - 1) {
                                                        pointEnd = partStart[p + 1];
                                                    } else {
                                                        pointEnd = points.length;
                                                    }
                                                    xPoints = new float[pointEnd - pointSt];
                                                    yPoints = new float[pointEnd - pointSt];
                                                    for (int k = pointSt; k < pointEnd; k++) {
                                                        xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                        yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                    }
                                                    polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);

                                                    polyline.moveTo(xPoints[0], yPoints[0]);

                                                    for (int index = 1; index < xPoints.length; index++) {
                                                        polyline.lineTo(xPoints[index], yPoints[index]);
                                                    }
                                                    g2d.setColor(colours[r]);
                                                    g2d.draw(polyline);
                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case POLYLINEM:
                                        myStroke = new BasicStroke(layer.getLineThickness());
                                        if (layer.isDashed()) {
                                            myStroke =
                                                    new BasicStroke(layer.getLineThickness(),
                                                    BasicStroke.CAP_BUTT,
                                                    BasicStroke.JOIN_MITER,
                                                    10.0f, layer.getDashArray(), 0.0f);
                                        }
                                        oldStroke = g2d.getStroke();
                                        g2d.setStroke(myStroke);

                                        for (ShapeFileRecord record : records) {
                                            r = record.getRecordNumber() - 1;
                                            if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                PolyLineM rec = (PolyLineM) (record.getGeometry());
                                                partStart = rec.getParts();
                                                points = rec.getPoints();
                                                for (int p = 0; p < rec.getNumParts(); p++) {
                                                    pointSt = partStart[p];
                                                    if (p < rec.getNumParts() - 1) {
                                                        pointEnd = partStart[p + 1];
                                                    } else {
                                                        pointEnd = points.length;
                                                    }
                                                    xPoints = new float[pointEnd - pointSt];
                                                    yPoints = new float[pointEnd - pointSt];
                                                    for (int k = pointSt; k < pointEnd; k++) {
                                                        xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                        yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                    }
                                                    polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);

                                                    polyline.moveTo(xPoints[0], yPoints[0]);

                                                    for (int index = 1; index < xPoints.length; index++) {
                                                        polyline.lineTo(xPoints[index], yPoints[index]);
                                                    }
                                                    g2d.setColor(colours[r]);
                                                    g2d.draw(polyline);
                                                }
                                            }
                                        }
                                        g2d.setStroke(oldStroke);
                                        break;
                                    case POLYGON:

                                        if (layer.isFilled()) {
                                            colours = layer.getColourData();
                                            for (ShapeFileRecord record : records) {
                                                r = record.getRecordNumber() - 1;
                                                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                    whitebox.geospatialfiles.shapefile.Polygon rec = (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                                                    partStart = rec.getParts();
                                                    points = rec.getPoints();
                                                    polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, points.length);
                                                    for (int p = 0; p < rec.getNumParts(); p++) {
                                                        pointSt = partStart[p];
                                                        if (p < rec.getNumParts() - 1) {
                                                            pointEnd = partStart[p + 1];
                                                        } else {
                                                            pointEnd = points.length;
                                                        }
                                                        xPoints = new float[pointEnd - pointSt];
                                                        yPoints = new float[pointEnd - pointSt];
                                                        for (int k = pointSt; k < pointEnd; k++) {
                                                            xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                            yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                        }
                                                        polyline.moveTo(xPoints[0], yPoints[0]);

                                                        for (int index = 1; index < xPoints.length; index++) {
                                                            polyline.lineTo(xPoints[index], yPoints[index]);
                                                        }
                                                        polyline.closePath();
                                                    }
                                                    g2d.setColor(colours[r]);
                                                    g2d.fill(polyline);
                                                }
                                            }
                                        }

                                        if (layer.isOutlined()) {
                                            g2d.setColor(lineColour);
                                            myStroke = new BasicStroke(layer.getLineThickness());
                                            if (layer.isDashed()) {
                                                myStroke =
                                                        new BasicStroke(layer.getLineThickness(),
                                                        BasicStroke.CAP_BUTT,
                                                        BasicStroke.JOIN_MITER,
                                                        10.0f, layer.getDashArray(), 0.0f);
                                            }
                                            oldStroke = g2d.getStroke();
                                            g2d.setStroke(myStroke);
                                            for (ShapeFileRecord record : records) {
                                                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                    whitebox.geospatialfiles.shapefile.Polygon rec = (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                                                    partStart = rec.getParts();
                                                    points = rec.getPoints();
                                                    for (int p = 0; p < rec.getNumParts(); p++) {
                                                        pointSt = partStart[p];
                                                        if (p < rec.getNumParts() - 1) {
                                                            pointEnd = partStart[p + 1];
                                                        } else {
                                                            pointEnd = points.length;
                                                        }
                                                        xPoints = new float[pointEnd - pointSt];
                                                        yPoints = new float[pointEnd - pointSt];
                                                        for (int k = pointSt; k < pointEnd; k++) {
                                                            xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                            yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                        }
                                                        polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);
                                                        polyline.moveTo(xPoints[0], yPoints[0]);

                                                        for (int index = 1; index < xPoints.length; index++) {
                                                            polyline.lineTo(xPoints[index], yPoints[index]);
                                                        }
                                                        g2d.draw(polyline);
                                                    }
                                                }
                                            }
                                            g2d.setStroke(oldStroke);
                                        }
                                        break;
                                    case POLYGONZ:
                                        if (layer.isFilled()) {
                                            colours = layer.getColourData();
                                            for (ShapeFileRecord record : records) {
                                                r = record.getRecordNumber() - 1;
                                                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                    PolygonZ rec = (PolygonZ) (record.getGeometry());
                                                    partStart = rec.getParts();
                                                    points = rec.getPoints();
                                                    polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, points.length);
                                                    for (int p = 0; p < rec.getNumParts(); p++) {
                                                        pointSt = partStart[p];
                                                        if (p < rec.getNumParts() - 1) {
                                                            pointEnd = partStart[p + 1];
                                                        } else {
                                                            pointEnd = points.length;
                                                        }
                                                        xPoints = new float[pointEnd - pointSt];
                                                        yPoints = new float[pointEnd - pointSt];
                                                        for (int k = pointSt; k < pointEnd; k++) {
                                                            xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                            yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                        }
                                                        polyline.moveTo(xPoints[0], yPoints[0]);

                                                        for (int index = 1; index < xPoints.length; index++) {
                                                            polyline.lineTo(xPoints[index], yPoints[index]);
                                                        }
                                                        polyline.closePath();
                                                    }
                                                    g2d.setColor(colours[r]);
                                                    g2d.fill(polyline);
                                                }
                                            }
                                        }

                                        if (layer.isOutlined()) {
                                            g2d.setColor(lineColour);
                                            myStroke = new BasicStroke(layer.getLineThickness());
                                            if (layer.isDashed()) {
                                                myStroke =
                                                        new BasicStroke(layer.getLineThickness(),
                                                        BasicStroke.CAP_BUTT,
                                                        BasicStroke.JOIN_MITER,
                                                        10.0f, layer.getDashArray(), 0.0f);
                                            }
                                            oldStroke = g2d.getStroke();
                                            g2d.setStroke(myStroke);
                                            for (ShapeFileRecord record : records) {
                                                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                    PolygonZ rec = (PolygonZ) (record.getGeometry());
                                                    partStart = rec.getParts();
                                                    points = rec.getPoints();
                                                    for (int p = 0; p < rec.getNumParts(); p++) {
                                                        pointSt = partStart[p];
                                                        if (p < rec.getNumParts() - 1) {
                                                            pointEnd = partStart[p + 1];
                                                        } else {
                                                            pointEnd = points.length;
                                                        }
                                                        xPoints = new float[pointEnd - pointSt];
                                                        yPoints = new float[pointEnd - pointSt];
                                                        for (int k = pointSt; k < pointEnd; k++) {
                                                            xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                            yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                        }
                                                        polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);
                                                        polyline.moveTo(xPoints[0], yPoints[0]);

                                                        for (int index = 1; index < xPoints.length; index++) {
                                                            polyline.lineTo(xPoints[index], yPoints[index]);
                                                        }
                                                        g2d.draw(polyline);
                                                    }
                                                }
                                            }
                                            g2d.setStroke(oldStroke);
                                        }
                                        break;
                                    case POLYGONM:
                                        if (layer.isFilled()) {
                                            colours = layer.getColourData();
                                            for (ShapeFileRecord record : records) {
                                                r = record.getRecordNumber() - 1;
                                                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                    PolygonM rec = (PolygonM) (record.getGeometry());
                                                    partStart = rec.getParts();
                                                    points = rec.getPoints();
                                                    polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, points.length);
                                                    for (int p = 0; p < rec.getNumParts(); p++) {
                                                        pointSt = partStart[p];
                                                        if (p < rec.getNumParts() - 1) {
                                                            pointEnd = partStart[p + 1];
                                                        } else {
                                                            pointEnd = points.length;
                                                        }
                                                        xPoints = new float[pointEnd - pointSt];
                                                        yPoints = new float[pointEnd - pointSt];
                                                        for (int k = pointSt; k < pointEnd; k++) {
                                                            xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                            yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                        }
                                                        polyline.moveTo(xPoints[0], yPoints[0]);

                                                        for (int index = 1; index < xPoints.length; index++) {
                                                            polyline.lineTo(xPoints[index], yPoints[index]);
                                                        }
                                                        polyline.closePath();
                                                    }
                                                    g2d.setColor(colours[r]);
                                                    g2d.fill(polyline);
                                                }
                                            }
                                        }

                                        if (layer.isOutlined()) {
                                            g2d.setColor(lineColour);
                                            myStroke = new BasicStroke(layer.getLineThickness());
                                            if (layer.isDashed()) {
                                                myStroke =
                                                        new BasicStroke(layer.getLineThickness(),
                                                        BasicStroke.CAP_BUTT,
                                                        BasicStroke.JOIN_MITER,
                                                        10.0f, layer.getDashArray(), 0.0f);
                                            }
                                            oldStroke = g2d.getStroke();
                                            g2d.setStroke(myStroke);
                                            for (ShapeFileRecord record : records) {
                                                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                                                    PolygonM rec = (PolygonM) (record.getGeometry());
                                                    partStart = rec.getParts();
                                                    points = rec.getPoints();
                                                    for (int p = 0; p < rec.getNumParts(); p++) {
                                                        pointSt = partStart[p];
                                                        if (p < rec.getNumParts() - 1) {
                                                            pointEnd = partStart[p + 1];
                                                        } else {
                                                            pointEnd = points.length;
                                                        }
                                                        xPoints = new float[pointEnd - pointSt];
                                                        yPoints = new float[pointEnd - pointSt];
                                                        for (int k = pointSt; k < pointEnd; k++) {
                                                            xPoints[k - pointSt] = (float) (borderWidth + (points[k][0] - leftCoord) / EWRange * myWidth);
                                                            yPoints[k - pointSt] = (float) (borderWidth + (topCoord - points[k][1]) / NSRange * myHeight);
                                                        }
                                                        polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xPoints.length);
                                                        polyline.moveTo(xPoints[0], yPoints[0]);

                                                        for (int index = 1; index < xPoints.length; index++) {
                                                            polyline.lineTo(xPoints[index], yPoints[index]);
                                                        }
                                                        g2d.draw(polyline);
                                                    }
                                                }
                                            }
                                            g2d.setStroke(oldStroke);
                                        }
                                        break;
                                    case MULTIPATCH:
                                        // this vector type is unsupported
                                        break;
                                }
                            }
                        }
                    }
                }

                int innerBorderWidth = borderWidth - 4;
                int neatLineWidth = borderWidth - 2;

                g2d.setColor(Color.white);
                g2d.fillRect(0, 0, getWidth(), borderWidth);
                g2d.fillRect(0, 0, borderWidth, getHeight());
                g2d.fillRect(0, getHeight() - borderWidth, getWidth(), getHeight());
                g2d.fillRect(getWidth() - borderWidth, 0, getWidth(), getHeight());

                g2d.setColor(Color.black);

                // draw the neat line
                g2d.drawRect(borderWidth - neatLineWidth, borderWidth - neatLineWidth,
                        (int) (myWidth + 2 * neatLineWidth), (int) (myHeight + 2 * neatLineWidth));

                // draw the corner boxes
                int leftEdge = borderWidth;
                int topEdge = borderWidth;
                int rightEdge = borderWidth + (int) myWidth;
                int bottomEdge = borderWidth + (int) myHeight;


                // draw the innermost line
                g2d.drawRect(borderWidth, borderWidth, (int) (myWidth), (int) (myHeight));

                // draw the graticule line
                g2d.drawRect(borderWidth - innerBorderWidth, borderWidth - innerBorderWidth,
                        (int) (myWidth + 2 * innerBorderWidth), (int) (myHeight + 2 * innerBorderWidth));


                g2d.drawRect(leftEdge - innerBorderWidth, topEdge - innerBorderWidth, innerBorderWidth, innerBorderWidth);
                g2d.drawRect(leftEdge - innerBorderWidth, bottomEdge, innerBorderWidth, innerBorderWidth);
                g2d.drawRect(rightEdge, topEdge - innerBorderWidth, innerBorderWidth, innerBorderWidth);
                g2d.drawRect(rightEdge, bottomEdge, innerBorderWidth, innerBorderWidth);


                // labels
                DecimalFormat df = new DecimalFormat("###,###,###.#");
                Font font = new Font("SanSerif", Font.PLAIN, 11);
                g2d.setFont(font);
                FontMetrics metrics = g.getFontMetrics(font);
                int hgt, adv;

                double x2 = currentExtent.getMinX() - (left - borderWidth) / scale;
                String label = df.format(x2) + XYUnits;
                g2d.drawString(label, leftEdge + 3, topEdge - 4);
                g2d.drawString(label, leftEdge + 3, bottomEdge + 13);

                label = df.format(currentExtent.getMaxX()) + XYUnits;
                hgt = metrics.getHeight();
                adv = metrics.stringWidth(label);
                Dimension size = new Dimension(adv + 2, hgt + 2);

                g2d.drawString(label, rightEdge - size.width, topEdge - 4);
                g2d.drawString(label, rightEdge - size.width, bottomEdge + 13);

                // rotate the font
                Font oldFont = g.getFont();
                Font f = oldFont.deriveFont(AffineTransform.getRotateInstance(-Math.PI / 2.0));
                g2d.setFont(f);

                double y2 = currentExtent.getMaxY() + (top - borderWidth) / scale;

                label = df.format(y2) + XYUnits;
                hgt = metrics.getHeight();
                adv = metrics.stringWidth(label);
                size = new Dimension(adv + 2, hgt + 2);

                g2d.drawString(label, leftEdge - 3, topEdge + size.width);
                g2d.drawString(label, rightEdge + 11, topEdge + size.width);

                y2 = currentExtent.getMinY() - (top - borderWidth) / scale;
                label = df.format(y2) + XYUnits;
                hgt = metrics.getHeight();
                adv = metrics.stringWidth(label);
                size = new Dimension(adv + 2, hgt + 2);

                g2d.drawString(label, leftEdge - 3, bottomEdge - 2);
                g2d.drawString(label, rightEdge + 11, bottomEdge - 2);

                // replace the rotated font.
                g2d.setFont(oldFont);

                if (mouseDragged && myMode == MOUSE_MODE_ZOOM && !usingDistanceTool) {
                    g2d.setColor(Color.black);
                    int boxWidth = (int) (Math.abs(startCol - endCol));
                    int boxHeight = (int) (Math.abs(startRow - endRow));
                    x = Math.min(startCol, endCol);
                    y = Math.min(startRow, endRow);
                    g2d.drawRect(x, y, boxWidth, boxHeight);
                    g2d.setColor(Color.white);
                    boxWidth += 2;
                    boxHeight += 2;
                    g2d.drawRect(x - 1, y - 1, boxWidth, boxHeight);

                } else if (mouseDragged && myMode == MOUSE_MODE_PAN && !usingDistanceTool) {
                    g2d.setColor(Color.white);
                    g2d.drawLine(startCol, startRow, endCol, endRow);
                } else if (mouseDragged && usingDistanceTool) {
                    int radius = 3;
                    g2d.setColor(Color.white);
                    g2d.drawOval(startCol - radius - 1, startRow - radius - 1, 2 * radius + 2, 2 * radius + 2);
                    g2d.setColor(Color.black);
                    g2d.drawOval(startCol - radius, startRow - radius, 2 * radius, 2 * radius);
                    g2d.setColor(Color.white);
                    g2d.drawLine(startCol, startRow, endCol, endRow);
                    g2d.setColor(Color.black);
                    g2d.drawLine(startCol - 1, startRow - 1, endCol - 1, endRow - 1);
                    DecimalFormat df2 = new DecimalFormat("###,###,###.##");

                    double dist = Math.sqrt((endCol - startCol) * (endCol - startCol) + (endRow - startRow) * (endRow - startRow)) / scale;
                    status.setMessage("Distance: " + df2.format(dist) + XYUnits);
                }

                if (modifyingPixels) {
                    if (modifyPixelsX > 0 && modifyPixelsY > 0) {
                        int crosshairlength = 13;
                        int radius = 9;
                        g2d.setColor(Color.white);
                        g2d.drawOval(modifyPixelsX - radius - 1, modifyPixelsY - radius - 1, 2 * radius + 2, 2 * radius + 2);
                        g2d.setColor(Color.black);
                        g2d.drawOval(modifyPixelsX - radius, modifyPixelsY - radius, 2 * radius, 2 * radius);


                        g2d.setColor(Color.white);
                        g2d.drawRect(modifyPixelsX - 1, modifyPixelsY - crosshairlength - 1, 2, crosshairlength * 2 + 2);
                        g2d.drawRect(modifyPixelsX - crosshairlength - 1, modifyPixelsY - 1, crosshairlength * 2 + 2, 2);
                        g2d.setColor(Color.black);
                        g2d.drawLine(modifyPixelsX, modifyPixelsY - crosshairlength, modifyPixelsX, modifyPixelsY + crosshairlength);
                        g2d.drawLine(modifyPixelsX - crosshairlength, modifyPixelsY, modifyPixelsX + crosshairlength, modifyPixelsY);

                    }

                }

                /*
                 * if (cursorX > leftEdge && cursorX < rightEdge && cursorY >
                 * topEdge && cursorY < bottomEdge) { g2d.setColor(Color.RED);
                 * g2d.drawLine(cursorX, topEdge - 2, cursorX, topEdge -
                 * innerBorderWidth + 2); g2d.drawLine(leftEdge - 2, cursorY,
                 * leftEdge - innerBorderWidth + 2, cursorY);
                 * g2d.drawLine(cursorX, bottomEdge + 2, cursorX, bottomEdge +
                 * innerBorderWidth - 2); g2d.drawLine(rightEdge + 2, cursorY,
                 * rightEdge + innerBorderWidth - 2, cursorY);
                }
                 */
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Override the ImageObserver imageUpdate method and monitor the loading of
     * the image. Set a flag when it is loaded.
   *
     */
    @Override
    public boolean imageUpdate(Image img, int info_flags,
            int x, int y, int w, int h) {
        if (info_flags != ALLBITS) {
            // Indicates image has not finished loading
            // Returning true will tell the image loading
            // thread to keep drawing until image fully
            // drawn loaded.
            this.repaint();
            return true;
        } else {

            return false;
        }
    } // imageUpdate

    @Override
    public int print(Graphics g, PageFormat pf, int page)
            throws PrinterException {
        if (page > 0) {
            return NO_SUCH_PAGE;
        }

        int i = pf.getOrientation();

        // get the size of the page
        double pageWidth = pf.getImageableWidth();
        double pageHeight = pf.getImageableHeight();
        double myWidth = this.getWidth();// - borderWidth * 2;
        double myHeight = this.getHeight();// - borderWidth * 2;
        double scaleX = pageWidth / myWidth;
        double scaleY = pageHeight / myHeight;
        double minScale = Math.min(scaleX, scaleY);

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        g2d.scale(minScale, minScale);

        drawMapDataView(g);

        return PAGE_EXISTS;
    }

    public boolean saveToImage(String fileName) {
        try {
            int width = (int) this.getWidth();
            int height = (int) this.getHeight();
            // TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
            // into integer pixels
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics ig = bi.createGraphics();
            drawMapDataView(ig);
            int i = fileName.lastIndexOf(".");
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
            if (!ImageIO.write(bi, extension, new File(fileName))) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    //int cursorY;
    //int cursorX;
    @Override
    public void mouseMoved(MouseEvent e) {
        //cursorY = e.getY();
        //cursorX = e.getX();
        //this.repaint();
        updateStatus(e);
    }
    boolean mouseDragged = false;

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseDragged = true;
        if (myMode == MOUSE_MODE_ZOOM || usingDistanceTool) {
            endRow = e.getY();
            endCol = e.getX();
            this.repaint();
        }
    }
    double startX;
    double startY;
    double endX;
    double endY;
    int startCol;
    int startRow;
    int endCol;
    int endRow;

    @Override
    public void mousePressed(MouseEvent e) {
        if (status != null && mapExtent.getMinY() != mapExtent.getMaxY()) {
            double myWidth = this.getWidth() - borderWidth * 2;
            double myHeight = this.getHeight() - borderWidth * 2;
            startRow = e.getY();
            startCol = e.getX();
            startY = mapExtent.getMaxY() - (startRow - borderWidth) / myHeight * (mapExtent.getMaxY() - mapExtent.getMinY());
            startX = mapExtent.getMinX() + (startCol - borderWidth) / myWidth * (mapExtent.getMaxX() - mapExtent.getMinX());

            if (myMode == MOUSE_MODE_PAN) {
                this.setCursor(panClosedHandCursor);
            }
        }
        //int clickCount = e.getClickCount();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (status != null && mapExtent.getMinY() != mapExtent.getMaxY()) {
            //int clickCount = e.getClickCount();
            double myWidth = this.getWidth() - borderWidth * 2;
            double myHeight = this.getHeight() - borderWidth * 2;
            endY = mapExtent.getMaxY() - (e.getY() - borderWidth) / myHeight * (mapExtent.getMaxY() - mapExtent.getMinY());
            endX = mapExtent.getMinX() + (e.getX() - borderWidth) / myWidth * (mapExtent.getMaxX() - mapExtent.getMinX());

            if (mouseDragged && myMode == MOUSE_MODE_ZOOM && !usingDistanceTool) {
                // move the current extent such that it is centered on the point
                BoundingBox db = mapinfo.getCurrentExtent();
                db.setMaxY(Math.max(startY, endY));
                db.setMinY(Math.min(startY, endY));
                db.setMinX(Math.min(startX, endX));
                db.setMaxX(Math.max(startX, endX));
                mapinfo.setCurrentExtent(db);
                modifyPixelsX = -1;
                modifyPixelsY = -1;
                host.refreshMap(false);
            } else if (mouseDragged && myMode == MOUSE_MODE_PAN && !usingDistanceTool) {
                // move the current extent such that it is centered on the point
                BoundingBox db = mapinfo.getCurrentExtent();
                double deltaY = startY - endY;
                double deltaX = startX - endX;
                double z = db.getMaxY();
                db.setMaxY(z + deltaY);
                z = db.getMinY();
                db.setMinY(z + deltaY);
                z = db.getMinX();
                db.setMinX(z + deltaX);
                z = db.getMaxX();
                db.setMaxX(z + deltaX);
                mapinfo.setCurrentExtent(db);
                modifyPixelsX = -1;
                modifyPixelsY = -1;
                host.refreshMap(false);
            } else if (usingDistanceTool) {
                host.refreshMap(false);
            }

            if (myMode == MOUSE_MODE_PAN) {
                this.setCursor(panCursor);
            }
            mouseDragged = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (status != null) {
            status.setMessage("Ready");
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int clickCount = e.getClickCount();
        if (clickCount == 1 && modifyingPixels) {
            modifyPixelsX = e.getX();
            modifyPixelsY = e.getY();
            double myWidth = this.getWidth() - borderWidth * 2;
            double myHeight = this.getHeight() - borderWidth * 2;
            double y = mapExtent.getMaxY() - (modifyPixelsY - borderWidth) / myHeight * (mapExtent.getMaxY() - mapExtent.getMinY());
            double x = mapExtent.getMinX() + (modifyPixelsX - borderWidth) / myWidth * (mapExtent.getMaxX() - mapExtent.getMinX());
            GridCell point = mapinfo.getRowAndColumn(x, y);
            if (point.row >= 0) {
                host.refreshMap(false);
                RasterLayerInfo rli = (RasterLayerInfo) (mapinfo.getLayer(point.layerNum));
                String fileName = new File(rli.getHeaderFile()).getName();
                ModifyPixel mp = new ModifyPixel((Frame) findWindow(this), true, point, fileName);
                if (mp.wasSuccessful()) {
                    point = mp.getValue();
                    rli.setDataValue(point.row, point.col, point.z);
                    rli.update();
                    host.refreshMap(false);
                    //mapinfo.setRowAndColumn(mp.getValue());
                }
            } else {
                modifyPixelsX = -1;
                modifyPixelsY = -1;
                host.refreshMap(false);
            }
        } else if (clickCount == 1) {
            // move the current extent such that it is centered on the point
            BoundingBox db = mapinfo.getCurrentExtent();
            double halfYRange = Math.abs(db.getMaxY() - db.getMinY()) / 2;
            double halfXRange = Math.abs(db.getMaxX() - db.getMinX()) / 2;
            db.setMaxY(startY + halfYRange);
            db.setMinY(startY - halfYRange);
            db.setMinX(startX - halfXRange);
            db.setMaxX(startX + halfXRange);
            mapinfo.setCurrentExtent(db);
            if (e.getButton() == 1) {
                mapinfo.zoomIn();
                host.refreshMap(false);
            } else if (e.getButton() == 3) {
                mapinfo.zoomOut();
                host.refreshMap(false);
            }
        } else if ((clickCount == 2) && (e.getButton() == 3)) {
            mapinfo.setCurrentExtent(mapinfo.getFullExtent());
            host.refreshMap(false);
        }
    }

    private void updateStatus(MouseEvent e) {
        if (status != null && mapExtent.getMinY() != mapExtent.getMaxY()) {
            double myWidth = this.getWidth() - borderWidth * 2;
            double myHeight = this.getHeight() - borderWidth * 2;
            double y = mapExtent.getMaxY() - (e.getY() - borderWidth) / myHeight * (mapExtent.getMaxY() - mapExtent.getMinY());
            double x = mapExtent.getMinX() + (e.getX() - borderWidth) / myWidth * (mapExtent.getMaxX() - mapExtent.getMinX());
            DecimalFormat df = new DecimalFormat("###,###,###.0");
            String xStr = df.format(x);
            String yStr = df.format(y);
            GridCell point = mapinfo.getRowAndColumn(x, y);
            if (point.row >= 0) {
                //double noDataValue = point.noDataValue;
                DecimalFormat dfZ = new DecimalFormat("###,###,###.####");
                String zStr;
                if (!point.isValueNoData() && !Double.isNaN(point.z)) {
                    zStr = dfZ.format(point.z);
                } else if (Double.isNaN(point.z)) {
                    zStr = "Not Available";
                } else {
                    zStr = "NoData";
                }
                if (!point.isRGB || point.isValueNoData()) {
                    status.setMessage("E: " + xStr + "  N: " + yStr
                            + "  Row: " + (int) (point.row) + "  Col: "
                            + (int) (point.col) + "  Z: " + zStr);
                } else {
                    String r = String.valueOf((int) point.z & 0xFF);
                    String g = String.valueOf(((int) point.z >> 8) & 0xFF);
                    String b = String.valueOf(((int) point.z >> 16) & 0xFF);
                    String a = String.valueOf(((int) point.z >> 24) & 0xFF);
                    if (a.equals("255")) {
                        status.setMessage("E: " + xStr + "  N: " + yStr
                                + "  Row: " + (int) (point.row) + "  Col: "
                                + (int) (point.col) + "  R: " + r + "  G: " + g
                                + "  B: " + b);
                    } else {
                        status.setMessage("E: " + xStr + "  N: " + yStr
                                + "  Row: " + (int) (point.row) + "  Col: "
                                + (int) (point.col) + "  R: " + r + "  G: " + g
                                + "  B: " + b + "  A: " + a);
                    }
                }
            } else if (!Double.isNaN(x) && !Double.isNaN(y)) {
                status.setMessage("E: " + xStr + "  N: " + yStr);
            }
        }
    }

    private static Window findWindow(Component c) {
        if (c == null) {
            return JOptionPane.getRootFrame();
        } else if (c instanceof Window) {
            return (Window) c;
        } else {
            return findWindow(c.getParent());
        }
    }

    class ModifyPixel extends JDialog implements ActionListener {

        GridCell point = null;
        JTextField tf = null;
        JTextField tfR = null;
        JTextField tfG = null;
        JTextField tfB = null;
        JTextField tfA = null;

        private ModifyPixel(Frame owner, boolean modal, GridCell point, String fileName) {
            super(owner, modal);
            this.setTitle(fileName);
            this.point = point;

            createGui();
        }

        private void createGui() {
            if (System.getProperty("os.name").contains("Mac")) {
                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }


            JPanel mainPane = new JPanel();
            mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
            mainPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));

            JPanel rowAndColPane = new JPanel();
            rowAndColPane.setLayout(new BoxLayout(rowAndColPane, BoxLayout.X_AXIS));
            rowAndColPane.add(new JLabel("Row: " + point.row));
            rowAndColPane.add(Box.createHorizontalStrut(15));
            rowAndColPane.add(new JLabel("Column: " + point.col));
            rowAndColPane.add(Box.createHorizontalGlue());
            mainPane.add(rowAndColPane);
            mainPane.add(Box.createVerticalStrut(5));

            tf = new JTextField(15);
            tf.setHorizontalAlignment(JTextField.RIGHT);
            if (!point.isValueNoData()) {
                tf.setText(String.valueOf(point.z));
            } else {
                tf.setText("NoData");
            }
            tf.setMaximumSize(new Dimension(35, 22));

            if (!point.isRGB) {
                JPanel valPane = new JPanel();
                valPane.setLayout(new BoxLayout(valPane, BoxLayout.X_AXIS));
                valPane.add(new JLabel("Value: "));
                valPane.add(tf);
                valPane.add(Box.createHorizontalGlue());
                mainPane.add(valPane);
                mainPane.add(Box.createVerticalStrut(5));

            } else {
                JPanel valPane = new JPanel();
                valPane.setLayout(new BoxLayout(valPane, BoxLayout.X_AXIS));
                valPane.add(new JLabel("Value: "));
                valPane.add(tf);
                valPane.add(Box.createHorizontalGlue());
                mainPane.add(valPane);
                mainPane.add(Box.createVerticalStrut(5));

                String r = "";
                String g = "";
                String b = "";
                String a = "";

                if (!point.isValueNoData()) {
                    r = String.valueOf((int) point.z & 0xFF);
                    g = String.valueOf(((int) point.z >> 8) & 0xFF);
                    b = String.valueOf(((int) point.z >> 16) & 0xFF);
                    a = String.valueOf(((int) point.z >> 24) & 0xFF);
                }

                tfR = new JTextField(5);
                tfG = new JTextField(5);
                tfB = new JTextField(5);
                tfA = new JTextField(5);

                tfR.setHorizontalAlignment(JTextField.RIGHT);
                tfG.setHorizontalAlignment(JTextField.RIGHT);
                tfB.setHorizontalAlignment(JTextField.RIGHT);
                tfA.setHorizontalAlignment(JTextField.RIGHT);

                tfR.setText(r);
                tfG.setText(g);
                tfB.setText(b);
                tfA.setText(a);

                JPanel rgbPane = new JPanel();

                rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
                rgbPane.add(new JLabel("R: "));
                rgbPane.add(tfR);
                rgbPane.add(Box.createHorizontalGlue());

                rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
                rgbPane.add(new JLabel(" G: "));
                rgbPane.add(tfG);
                rgbPane.add(Box.createHorizontalGlue());

                rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
                rgbPane.add(new JLabel(" B: "));
                rgbPane.add(tfB);
                rgbPane.add(Box.createHorizontalGlue());

                rgbPane.setLayout(new BoxLayout(rgbPane, BoxLayout.X_AXIS));
                rgbPane.add(new JLabel(" a: "));
                rgbPane.add(tfA);
                rgbPane.add(Box.createHorizontalGlue());

                mainPane.add(rgbPane);
                mainPane.add(Box.createVerticalStrut(5));

            }

            // buttons
            JButton ok = new JButton("OK");
            ok.addActionListener(this);
            ok.setActionCommand("ok");
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(this);
            cancel.setActionCommand("cancel");

            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
            buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            buttonPane.add(Box.createHorizontalGlue());
            buttonPane.add(ok);
            buttonPane.add(Box.createHorizontalStrut(5));
            buttonPane.add(cancel);
            buttonPane.add(Box.createHorizontalGlue());

            Container contentPane = getContentPane();
            contentPane.add(mainPane, BorderLayout.CENTER);
            contentPane.add(buttonPane, BorderLayout.PAGE_END);

            pack();

            this.setVisible(true);
        }

        private void confirmValue() {
            try {
                if (!point.isRGB) {
                    if (tf.getText().toLowerCase().contains("nodata")) {
                        point.z = point.noDataValue;
                    } else {
                        double z = Double.parseDouble(tf.getText());
                        point.z = z;
                    }
                    successful = true;
                } else {
                    if (tf.getText().toLowerCase().contains("nodata")) {
                        point.z = point.noDataValue;
                    } else {
                        int r = Integer.parseInt(tfR.getText());
                        int g = Integer.parseInt(tfG.getText());
                        int b = Integer.parseInt(tfB.getText());
                        int a = Integer.parseInt(tfA.getText());
                        double z = (double) ((a << 24) | (b << 16) | (g << 8) | r);
                        point.z = z;
                    }
                    successful = true;
                }
            } catch (Exception e) {
                System.out.println(e);
                successful = false;
            }
        }
        boolean successful = false;

        private boolean wasSuccessful() {
            return successful;
        }

        private GridCell getValue() {
            return point;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            String actionCommand = e.getActionCommand();
            if (actionCommand.equals("ok")) {
                confirmValue();
                this.dispose();
            } else if (actionCommand.equals("cancel")) {
                this.dispose();
            }
        }
    }
}
