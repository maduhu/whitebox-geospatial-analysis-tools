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
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import whitebox.cartographic.PointMarkers;
import whitebox.cartographic.PointMarkers.MarkerStyle;
import whitebox.cartographic.MapScale;
import whitebox.cartographic.NorthArrow;
import whitebox.cartographic.MapTitle;
import whitebox.cartographic.MapDataView;
import whitebox.cartographic.Legend;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.MapLayer;
import whitebox.interfaces.MapLayer.MapLayerType;
import whitebox.structures.BoundingBox;
import whitebox.structures.GridCell;

/**
 * This class is used to manage the layers and properties of maps. The actual 
 * map display is handled by the MapRenderer class.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MapInfo {
    // Fields.
    private ArrayList<MapLayer> layers = new ArrayList<MapLayer>();
    private int numLayers = 0;
    private MapLayer activeLayer = null;
    private int activeLayerOverlayNumber = -1;
    private int activeLayerIndex = -1;
    //private String mapTitle = "New Map";
    private boolean dirty = false; 
    private ArrayList<BoundingBox> listOfExtents = new ArrayList<BoundingBox>();
    private String fileName = "";
    private BoundingBox fullExtent = null;
    private BoundingBox currentExtent = null;
    private int listOfExtentsIndex = -1;
    private String workingDirectory = "";
    private String applicationDirectory = "";
    private String paletteDirectory = "";
    private String pathSep;
    private boolean cartoView = false;
    private PageFormat pageFormat = new PageFormat();
    private double margin = 0.25;
    
    // Public Fields
    public MapScale mapScale = new MapScale();
    public NorthArrow northArrow = new NorthArrow();
    public MapTitle mapTitle = new MapTitle("New Map", "default title");
    
    private ArrayList<CartographicElement> listOfCartographicElements = new ArrayList<CartographicElement>();
            
    /**
     * MapInfo constructor
     */
    public MapInfo(String mapTitle) {
        try {
            pathSep = File.separator;
            applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            paletteDirectory = applicationDirectory + pathSep + "resources" + pathSep + "palettes" + pathSep;
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
            Paper paper = pageFormat.getPaper();
            double width = paper.getWidth();
            double height = paper.getHeight();
            double marginInPoints = margin * 72;
            paper.setImageableArea(marginInPoints, marginInPoints, 
                    width - 2 * marginInPoints, height - 2 * marginInPoints);
            pageFormat.setPaper(paper);
            
            mapScale.setUnits("metres");
            
            CartographicElement ce = new MapDataView("default map data view");
            addNewCartographicElement(ce); // the default MapDataView;
            ce = new MapTitle(mapTitle, "default title"); // the default map title
            addNewCartographicElement(ce);
            
            
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
    
    public final void addNewCartographicElement(CartographicElement ce) {
        ce.setElementNumber(listOfCartographicElements.size());
        listOfCartographicElements.add(ce);
    }
    
    public ArrayList<CartographicElement> getCartographicElementList() {
        Collections.sort(listOfCartographicElements);
        return listOfCartographicElements;
    }
    
    
    // Properties
    
    public void setWorkingDirectory(String directory) {
        
    }
    
    public int getNumLayers() {
        return layers.size();
    }
    
    public int getNumRasterLayers() {
        int i = 0;
        for (int a = 0; a < layers.size(); a++) {
            if (layers.get(a).getLayerType() == MapLayerType.RASTER) {
                i++;
            }
        }
        return i;
    }
    
    public String getMapTitle() {
        return mapTitle.getLabel();
    }
    
    public void setMapTitle(String title) {
        mapTitle.setLabel(title);
        dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public BoundingBox getFullExtent() {
        return fullExtent = calculateFullExtent();
    }
    
    public BoundingBox getCurrentExtent() {
        if (currentExtent == null) {
            calculateFullExtent();
            currentExtent = getFullExtent();
        }
        return currentExtent.clone();
    }
    
    public void setCurrentExtent(BoundingBox extent) {
        currentExtent = extent.clone();
        addToExtentHistory(extent);
    }
    
    public int getActiveLayerOverlayNumber() {
        return activeLayer.getOverlayNumber();
    }
    
    public MapLayer getActiveLayer() {
        return activeLayer;
    }
    
    public void setActiveLayer(int overlayNumber) {
        this.activeLayerOverlayNumber = overlayNumber;
        activeLayerIndex = findLayerIndexByOverlayNum(overlayNumber);
        activeLayer = layers.get(activeLayerIndex);
        dirty = true;
    }
    
    public double getActiveLayerNoDataValue() {
        if (activeLayer.getLayerType() == MapLayerType.RASTER) {
            RasterLayerInfo rli = (RasterLayerInfo)activeLayer;
            return rli.getNoDataValue();
        } else {
            return -32768;
        }
    }

    public boolean isCartoView() {
        return cartoView;
    }

    public void setCartoView(boolean pageVisible) {
        this.cartoView = pageVisible;
    }

    public PageFormat getPageFormat() {
        return pageFormat;
    }

    public void setPageFormat(PageFormat pageFormat) {
        this.pageFormat = pageFormat;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }
    
    
    // Methods
    
    private void addToExtentHistory(BoundingBox extent) {
        if (listOfExtentsIndex == listOfExtents.size() - 1) {
            listOfExtents.add(extent.clone());
            listOfExtentsIndex = listOfExtents.size() - 1;
        } else {
            // delete all forward extents.
            for (int i = listOfExtents.size() - 1; i > listOfExtentsIndex; i--) {
                listOfExtents.remove(i);
            }
            listOfExtents.add(extent.clone());
            listOfExtentsIndex = listOfExtents.size() - 1;
        }
    }
    
    public boolean previousExtent() {
        if (listOfExtentsIndex == 0) { 
            return false; 
        } else {
            listOfExtentsIndex--;
            currentExtent = listOfExtents.get(listOfExtentsIndex).clone();
            return true;
        }
    }
    
    public boolean nextExtent() {
        if (listOfExtentsIndex == listOfExtents.size() - 1) { 
            return false; 
        } else {
            listOfExtentsIndex++;
            currentExtent = listOfExtents.get(listOfExtentsIndex).clone();
            return true;
        }
    }
    
    public GridCell getRowAndColumn(double easting, double northing) {
        GridCell point = new GridCell(-1, -1, Double.NaN, Double.NaN, -1);
        try {
            if (activeLayer.getLayerType() == MapLayerType.RASTER) {
                RasterLayerInfo layer = (RasterLayerInfo) (activeLayer);
                double noDataValue = layer.getNoDataValue();
                // first see if this point is within the active layer.
                BoundingBox db = layers.get(activeLayerIndex).getFullExtent();
                double top = db.getMaxY();
                double bottom = db.getMinY();
                double left = db.getMinX();
                double right = db.getMaxX();
                double z;
                boolean flag = false;

                int rows = layer.getNumberRows();
                int columns = layer.getNumberColumns();
                if (((northing >= bottom) && (northing <= top))
                        || ((northing <= bottom) && (northing >= top))) {
                    if (((easting >= left) && (easting <= right))
                            || ((easting <= left) && (easting >= right))) {
                        flag = true;
                    }

                }

                if (flag) {
                    int row = (int) ((top - northing) / (top - bottom) * (rows - 0.5));
                    int col = (int) ((easting - left) / (right - left) * (columns - 0.5));
                    z = layer.getDataValue(row, col);
                    point = new GridCell(row, col, z, layer.getNoDataValue(), layer.getOverlayNumber());
                    if (layer.getDataScale() == WhiteboxRasterInfo.DataScale.RGB) {
                        point.isRGB = true;
                    }
                } else {
                    // search from the top layer to the bottom for a layer in which this point resides.
                    for (int i = numLayers - 1; i >= 0; i--) {
                        if (getLayer(i) instanceof RasterLayerInfo) {
                            RasterLayerInfo rli = (RasterLayerInfo) getLayer(i);
                            db = rli.getFullExtent();
                            top = db.getMaxY();
                            bottom = db.getMinY();
                            left = db.getMinX();
                            right = db.getMaxX();
                            flag = false;
                            rows = rli.getNumberRows();
                            columns = rli.getNumberColumns();
                            if (((northing >= bottom) && (northing <= top))
                                    || ((northing <= bottom) && (northing >= top))) {
                                if (((easting >= left) && (easting <= right))
                                        || ((easting <= left) && (easting >= right))) {
                                    flag = true;
                                }
                            }

                            if (flag) {
                                int row = (int) ((top - northing) / (top - bottom) * (rows - 0.5));
                                int col = (int) ((easting - left) / (right - left) * (columns - 0.5));
                                z = rli.getDataValue(row, col);
                                point = new GridCell(row, col, z, rli.getNoDataValue(), rli.getOverlayNumber());
                                if (rli.getDataScale() == WhiteboxRasterInfo.DataScale.RGB) {
                                    point.isRGB = true;
                                }
                                return point;
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            // do nothing
        }
        return point;
        
    }
    
    public BoundingBox calculateFullExtent() {
        double top = -Double.MAX_VALUE;
        double bottom = Double.MAX_VALUE;
        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        
        for (MapLayer rli: layers) {
            BoundingBox db = rli.getFullExtent();
            if (db.getMaxY() > top) { top = db.getMaxY(); }
            if (db.getMinY() < bottom) { bottom = db.getMinY(); }
            if (db.getMaxX() > right) { right = db.getMaxX(); }
            if (db.getMinX() < left) { left = db.getMinX(); }
            
        }
        
        //fullExtent = new BoundingBox(top, right, bottom, left);
        fullExtent = new BoundingBox(left, bottom, right, top);
        
        return fullExtent.clone();
    }
    
    public void zoomIn() {
        double rangeX = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
        double rangeY = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
        double z;
        z = currentExtent.getMinY();
        currentExtent.setMinY(z + rangeY * 0.1);
        z = currentExtent.getMaxY();
        currentExtent.setMaxY(z - rangeY * 0.1);
        z = currentExtent.getMinX();
        currentExtent.setMinX(z + rangeX * 0.1);
        z = currentExtent.getMaxX();
        currentExtent.setMaxX(z - rangeX * 0.1);
        addToExtentHistory(currentExtent);
    }
    
    public void zoomOut() {
        double rangeX = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
        double rangeY = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
        double z;
        z = currentExtent.getMinY();
        currentExtent.setMinY(z - rangeY * 0.1);
        z = currentExtent.getMaxY();
        currentExtent.setMaxY(z + rangeY * 0.1);
        z = currentExtent.getMinX();
        currentExtent.setMinX(z - rangeX * 0.1);
        z = currentExtent.getMaxX();
        currentExtent.setMaxX(z + rangeX * 0.1);
        addToExtentHistory(currentExtent);
    }
    
    public void panUp() {
        double rangeY = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
        double z;
        z = currentExtent.getMinY();
        currentExtent.setMinY(z + rangeY * 0.1);
        z = currentExtent.getMaxY();
        currentExtent.setMaxY(z + rangeY * 0.1);
        addToExtentHistory(currentExtent);
    }
    
    public void panDown() {
        double rangeY = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
        double z;
        z = currentExtent.getMinY();
        currentExtent.setMinY(z - rangeY * 0.1);
        z = currentExtent.getMaxY();
        currentExtent.setMaxY(z - rangeY * 0.1);
        addToExtentHistory(currentExtent);
    } 
    
    public void panLeft() {
        double rangeX = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
        double z;
        z = currentExtent.getMinX();
        currentExtent.setMinX(z - rangeX * 0.1);
        z = currentExtent.getMaxX();
        currentExtent.setMaxX(z - rangeX * 0.1);
        addToExtentHistory(currentExtent);
    }
    
    public void panRight() {
        double rangeX = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
        double z;
        z = currentExtent.getMinX();
        currentExtent.setMinX(z + rangeX * 0.1);
        z = currentExtent.getMaxX();
        currentExtent.setMaxX(z + rangeX * 0.1);
        addToExtentHistory(currentExtent);
    }
    
    public MapLayer getLayer(int overlayNumber) {
        try {
            int i = findLayerIndexByOverlayNum(overlayNumber);
            return layers.get(i);
        } catch (Exception e) {
            return null;
        }
    }
    
    public void addLayer(MapLayer newLayer) {
        layers.add(newLayer);
        numLayers = layers.size();
        currentExtent = calculateFullExtent();
        listOfExtents.add(currentExtent.clone());
        listOfExtentsIndex = listOfExtents.size() - 1;
        dirty = true;
    }
    
    public void removeLayer(int overlayNumber) {
        try {
            if (numLayers > 0) {
                // which layer has an overlayNumber equal to layerNum?
                int indexOfLayerToRemove = findLayerIndexByOverlayNum(overlayNumber);

                if (indexOfLayerToRemove != -1) { // it exists
                    if (indexOfLayerToRemove == activeLayerIndex) {
                        // we're removing the active layer, so we'll need a new one.
                        // first, are there any other layers?
                        if (numLayers > 1) {
                            // if the active layer was the not the bottommost, set the active
                            // layer to the one currently beneath it, else the one above it.
                            if (activeLayerIndex > 0) {
                                activeLayerOverlayNumber--;
                                activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
                                activeLayer = layers.get(activeLayerIndex);
                            } else {
                                activeLayerOverlayNumber++;
                                activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
                                activeLayer = layers.get(activeLayerIndex);
                            }
                            layers.remove(indexOfLayerToRemove);
                            reorderLayers();
                            // what is the new overlay number and index of the active layer?
                            activeLayerOverlayNumber = activeLayer.getOverlayNumber();
                            activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
                        } else {
                            // we're removing the active and only layer.
                            layers.remove(indexOfLayerToRemove);
                            activeLayer = null;
                            activeLayerOverlayNumber = -1;
                            activeLayerIndex = -1;
                        }
                    } else {
                        // we're not removing the active layer. As a result, 
                        // there must be at least one other layer.
                        layers.remove(indexOfLayerToRemove);
                        reorderLayers();
                        // what is the new overlay number and index of the active layer?
                        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
                        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
                    }

                    numLayers = layers.size();
                }

                currentExtent = calculateFullExtent();
                listOfExtents.add(currentExtent.clone());
            }
        } catch (Exception e) {
            // do nothing.
        }
    }
    
    private void reorderLayers() {
        
        numLayers = layers.size();
        
        if (numLayers == 0) { return; }
        
        // find current highest value
        int highestVal = 0;
        int highestValIndex = 0;
        for (int i = 0; i < numLayers; i++) {
            int overlayNum = layers.get(i).getOverlayNumber();
            if (overlayNum > highestVal) { 
                highestVal = overlayNum; 
                highestValIndex = i;
            }
        }
        
        int currentLowest = -99;
        int nextLowest;
        int nextLowestIndex = 0;
        
        int[] orderArray = new int[numLayers];
        
        for (int i = 0; i < numLayers; i++) {
            nextLowest = highestVal;
            nextLowestIndex = highestValIndex;
            for (int j = 0; j < numLayers; j++) {
                int overlayNum = layers.get(j).getOverlayNumber();
                if ((overlayNum > currentLowest) && (overlayNum < nextLowest)) {
                    nextLowest = overlayNum;
                    nextLowestIndex = j;
                }
            }
            currentLowest = nextLowest;
            orderArray[i] = nextLowestIndex;
        }
        
        for (int i = 0; i < numLayers; i++) {
            layers.get(orderArray[i]).setOverlayNumber(i);
        }
        
        dirty = true;
    }
    
    public void promoteLayerToTop(int overlayNumber) {
        if (overlayNumber == numLayers - 1) { // it's already topmost
            return;
        }
        
        // which layer has an overlayNumber equal to overlayNumber?
        int layerToMove = findLayerIndexByOverlayNum(overlayNumber);
        
        layers.get(layerToMove).setOverlayNumber(numLayers);
        reorderLayers();
        
        // update the active layer overlay number and index.
        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
        
        dirty = true;
    }
   
    public void demoteLayerToBottom(int overlayNumber) {
        if (overlayNumber == 0) { // it's already bottommost
            return;
        }
        
        // which layer has an overlayNumber equal to overlayNumber?
        int layerToMove = findLayerIndexByOverlayNum(overlayNumber);
        
        layers.get(layerToMove).setOverlayNumber(-1);
        reorderLayers();
        
        // update the active layer overlay number and index.
        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
        
        dirty = true;
    }
    
    public void promoteLayer(int overlayNumber) {
        if (overlayNumber == numLayers - 1) { // it's already topmost
            return;
        }
        
        if (numLayers < 2) { // you need at least two layers to promote one
            return;
        }
        
        int layerToPromote = findLayerIndexByOverlayNum(overlayNumber);
        int layerToDemote = findLayerIndexByOverlayNum(overlayNumber + 1);
        layers.get(layerToPromote).setOverlayNumber(overlayNumber + 1);
        layers.get(layerToDemote).setOverlayNumber(overlayNumber);
        
        // update the active layer overlay number and index.
        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
        
        dirty = true;
    }
    
    public void demoteLayer(int overlayNumber) {
        if (overlayNumber == 0) {
            return;
        }
        
        if (numLayers < 2) { // you need at least two layers to demote one
            return;
        }
        
        int layerToDemote = findLayerIndexByOverlayNum(overlayNumber);
        int layerToPromote = findLayerIndexByOverlayNum(overlayNumber - 1);
        layers.get(layerToDemote).setOverlayNumber(overlayNumber - 1);
        layers.get(layerToPromote).setOverlayNumber(overlayNumber);
        
        // update the active layer overlay number and index.
        activeLayerOverlayNumber = activeLayer.getOverlayNumber();
        activeLayerIndex = findLayerIndexByOverlayNum(activeLayerOverlayNumber);
        
        
        dirty = true;
    }
    
    public void toggleLayerVisibility(int overlayNumber) {
        // which layer has an overlayNumber equal to overlayNumber?
        int layerToToggle = findLayerIndexByOverlayNum(overlayNumber);
        boolean value = layers.get(layerToToggle).isVisible();
        if (value) {
            layers.get(layerToToggle).setVisible(false);
        } else {
            layers.get(layerToToggle).setVisible(true);
        }
    }
    
    public void reversePaletteOfLayer(int overlayNumber) {
        // which layer has an overlayNumber equal to overlayNumber?
        int layerToChange = findLayerIndexByOverlayNum(overlayNumber);
        if (layers.get(layerToChange).getLayerType() == MapLayerType.RASTER) {
            RasterLayerInfo rli = (RasterLayerInfo)layers.get(layerToChange);
            boolean value = !rli.isPaletteReversed();
            rli.setPaletteReversed(value);
        }
    }
    
    public int findLayerIndexByOverlayNum(int overlayNumber) {
        // which layer has an overlayNumber equal to layerNum?
        int layerIndex = -1;
        for (int i = 0; i < numLayers; i++) {
            if (layers.get(i).getOverlayNumber() == overlayNumber) {
                layerIndex = i;
                break;
            }
        }
        
        return layerIndex;
    }
    
    private boolean save() {
        if (fileName.equals("")) { return false; }
        try {

            Element layertitle;
            Element isvisible;
            Element alpha;
            Element overlayNum;
            
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("MapInfo");
            doc.appendChild(rootElement);

            // map elements
            Element mapElements = doc.createElement("MapElements");
            rootElement.appendChild(mapElements);

            Element title = doc.createElement("MapTitle");
            title.appendChild(doc.createTextNode(mapTitle.getLabel()));
            mapElements.appendChild(title);

            if (fullExtent == null) {
                calculateFullExtent();
            }
            Element fullextent = doc.createElement("FullExtent");
            mapElements.appendChild(fullextent);
            Element feTop = doc.createElement("Top");
            feTop.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMaxY())));
            fullextent.appendChild(feTop);
            Element feBottom = doc.createElement("Bottom");
            feBottom.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMinY())));
            fullextent.appendChild(feBottom);
            Element feLeft = doc.createElement("Left");
            feLeft.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMinX())));
            fullextent.appendChild(feLeft);
            Element feRight = doc.createElement("Right");
            feRight.appendChild(doc.createTextNode(String.valueOf(fullExtent.getMaxX())));
            fullextent.appendChild(feRight);

            Element currentextent = doc.createElement("CurrentExtent");
            mapElements.appendChild(currentextent);
            Element ceTop = doc.createElement("Top");
            ceTop.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMaxY())));
            currentextent.appendChild(ceTop);
            Element ceBottom = doc.createElement("Bottom");
            ceBottom.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMinY())));
            currentextent.appendChild(ceBottom);
            Element ceLeft = doc.createElement("Left");
            ceLeft.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMinX())));
            currentextent.appendChild(ceLeft);
            Element ceRight = doc.createElement("Right");
            ceRight.appendChild(doc.createTextNode(String.valueOf(currentExtent.getMaxX())));
            currentextent.appendChild(ceRight);

            Element activelayer = doc.createElement("ActiveLayerNum");
            activelayer.appendChild(doc.createTextNode(String.valueOf(getActiveLayerOverlayNumber())));
            mapElements.appendChild(activelayer);

            // map layers
            Element mapLayers = doc.createElement("MapLayers");
            rootElement.appendChild(mapLayers);

            for (int i = 0; i < layers.size(); i++) {
                Element maplayer = doc.createElement("Layer");
                Element layertype = doc.createElement("LayerType");
                layertype.appendChild(doc.createTextNode(
                        String.valueOf(layers.get(i).getLayerType())));
                maplayer.appendChild(layertype);
                switch (layers.get(i).getLayerType()) {
                    case RASTER:
                        RasterLayerInfo rli = (RasterLayerInfo) (layers.get(i));
                        Element headerfile = doc.createElement("HeaderFile");
                        headerfile.appendChild(doc.createTextNode(rli.getHeaderFile()));
                        maplayer.appendChild(headerfile);

                        layertitle = doc.createElement("LayerTitle");
                        layertitle.appendChild(doc.createTextNode(rli.getLayerTitle()));
                        maplayer.appendChild(layertitle);

                        isvisible = doc.createElement("IsVisible");
                        isvisible.appendChild(doc.createTextNode(String.valueOf(rli.isVisible())));
                        maplayer.appendChild(isvisible);

                        Element paletteReversed = doc.createElement("IsPaletteReversed");
                        paletteReversed.appendChild(doc.createTextNode(String.valueOf(rli.isPaletteReversed())));
                        maplayer.appendChild(paletteReversed);

                        Element nonlinearity = doc.createElement("Nonlinearity");
                        nonlinearity.appendChild(doc.createTextNode(String.valueOf(rli.getNonlinearity())));
                        maplayer.appendChild(nonlinearity);

                        Element displaymin = doc.createElement("DisplayMinVal");
                        displaymin.appendChild(doc.createTextNode(String.valueOf(rli.getDisplayMinVal())));
                        maplayer.appendChild(displaymin);

                        Element displaymax = doc.createElement("DisplayMaxVal");
                        displaymax.appendChild(doc.createTextNode(String.valueOf(rli.getDisplayMaxVal())));
                        maplayer.appendChild(displaymax);

                        Element palette = doc.createElement("Palette");
                        maplayer.appendChild(palette);
                        palette.appendChild(doc.createTextNode(rli.getPaletteFile()));

                        alpha = doc.createElement("Alpha");
                        alpha.appendChild(doc.createTextNode(String.valueOf(rli.getAlpha())));
                        maplayer.appendChild(alpha);

                        overlayNum = doc.createElement("OverlayNum");
                        overlayNum.appendChild(doc.createTextNode(String.valueOf(rli.getOverlayNumber())));
                        maplayer.appendChild(overlayNum);

                        break;
                    case VECTOR:
                        VectorLayerInfo vli = (VectorLayerInfo) (layers.get(i));
                        Element shapefile = doc.createElement("ShapeFile");
                        shapefile.appendChild(doc.createTextNode(vli.getFileName()));
                        maplayer.appendChild(shapefile);

                        layertitle = doc.createElement("LayerTitle");
                        layertitle.appendChild(doc.createTextNode(vli.getLayerTitle()));
                        maplayer.appendChild(layertitle);

                        isvisible = doc.createElement("IsVisible");
                        isvisible.appendChild(doc.createTextNode(String.valueOf(vli.isVisible())));
                        maplayer.appendChild(isvisible);

                        alpha = doc.createElement("Alpha");
                        alpha.appendChild(doc.createTextNode(String.valueOf(vli.getAlpha())));
                        maplayer.appendChild(alpha);

                        overlayNum = doc.createElement("OverlayNum");
                        overlayNum.appendChild(doc.createTextNode(String.valueOf(vli.getOverlayNumber())));
                        maplayer.appendChild(overlayNum);

                        Element fillColour = doc.createElement("FillColour");
                        fillColour.appendChild(doc.createTextNode(String.valueOf(vli.getFillColour().getRGB())));
                        maplayer.appendChild(fillColour);
                        
                        Element lineColour = doc.createElement("LineColour");
                        lineColour.appendChild(doc.createTextNode(String.valueOf(vli.getLineColour().getRGB())));
                        maplayer.appendChild(lineColour);
                        
                        Element lineThickness = doc.createElement("LineThickness");
                        lineThickness.appendChild(doc.createTextNode(String.valueOf(vli.getLineThickness())));
                        maplayer.appendChild(lineThickness);
                        
                        Element markerSize = doc.createElement("MarkerSize");
                        markerSize.appendChild(doc.createTextNode(String.valueOf(vli.getMarkerSize())));
                        maplayer.appendChild(markerSize);
                        
                        Element markerStyle = doc.createElement("MarkerStyle");
                        markerStyle.appendChild(doc.createTextNode(String.valueOf(vli.getMarkerStyle().toString())));
                        maplayer.appendChild(markerStyle);
                        
                        Element isFilled = doc.createElement("IsFilled");
                        isFilled.appendChild(doc.createTextNode(String.valueOf(vli.isFilled())));
                        maplayer.appendChild(isFilled);
                        
                        Element isOutlined = doc.createElement("IsOutlined");
                        isOutlined.appendChild(doc.createTextNode(String.valueOf(vli.isOutlined())));
                        maplayer.appendChild(isOutlined);
                        
                        Element isDashed = doc.createElement("IsDashed");
                        isDashed.appendChild(doc.createTextNode(String.valueOf(vli.isDashed())));
                        maplayer.appendChild(isDashed);
                        
                        Element dashArray = doc.createElement("DashArray");
                        float[] dashArrayFlt = vli.getDashArray();
                        String dashArrayStr = "";
                        for (int a = 0; a < dashArrayFlt.length; a++) {
                            if (a < dashArrayFlt.length - 1) {
                                dashArrayStr += String.valueOf(dashArrayFlt[a] + ",");
                            } else {
                                dashArrayStr += String.valueOf(dashArrayFlt[a]);
                            }
                        }
                        dashArray.appendChild(doc.createTextNode(dashArrayStr));
                        maplayer.appendChild(dashArray);
                        
                        Element isFilledWithOneColour = doc.createElement("IsFilledWithOneColour");
                        isFilledWithOneColour.appendChild(doc.createTextNode(String.valueOf(vli.isFilledWithOneColour())));
                        maplayer.appendChild(isFilledWithOneColour);
                        
                        Element isOutlinedWithOneColour = doc.createElement("IsOutlinedWithOneColour");
                        isOutlinedWithOneColour.appendChild(doc.createTextNode(String.valueOf(vli.isOutlinedWithOneColour())));
                        maplayer.appendChild(isOutlinedWithOneColour);
                        
                        Element isPaletteScaled = doc.createElement("IsPaletteScaled");
                        isPaletteScaled.appendChild(doc.createTextNode(String.valueOf(vli.isPaletteScaled())));
                        maplayer.appendChild(isPaletteScaled);
                        
                        Element paletteFile = doc.createElement("PaletteFile");
                        paletteFile.appendChild(doc.createTextNode(String.valueOf(vli.getPaletteFile())));
                        maplayer.appendChild(paletteFile);
                        
                        Element colouringAttribute = doc.createElement("ColouringAttribute");
                        colouringAttribute.appendChild(doc.createTextNode(String.valueOf(vli.getFillAttribute())));
                        maplayer.appendChild(colouringAttribute);
                        
                        break;

                    case MULTISPECTRAL:
                        /* This will need to be added when support for multispectral
                         * data has been added.
                         */
                        break;
                }
                mapLayers.appendChild(maplayer);
            }
                
//		// set attribute to staff element
//		Attr attr = doc.createAttribute("id");
//		attr.setValue("1");
//		staff.setAttributeNode(attr);
// 
//		// shorten way
//		// staff.setAttribute("id", "1");
 
		// write the content into xml file
                File file = new File(fileName);
                if (file.exists()) { file.delete(); }
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
                transformer.transform(source, result);
                return true;
	  } catch (ParserConfigurationException pce) {
		System.out.println(pce);
                return false;
	  } catch (TransformerException tfe) {
		System.out.println(tfe);
                return false;
	  }   
    }
    
    public boolean saveMap() {
        return save();
    }
    
    public boolean saveMap(String fileName) {
        this.fileName = fileName;
        return save();
    }
    
    private boolean open() {
        File file = new File(fileName);

        if (!file.exists()) {
            return false;
        }
        try {                    
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            
            mapTitle.setLabel(doc.getElementsByTagName("MapTitle").item(0).getTextContent());
            int activeLayerNum = Integer.parseInt(doc.getElementsByTagName("ActiveLayerNum").item(0).getTextContent());
            
            // get the current extent
            NodeList feList = doc.getElementsByTagName("FullExtent");
            for (int s = 0; s < feList.getLength(); s++) {
                if(feList.item(s).getNodeType() == Node.ELEMENT_NODE){
                    Element el = (Element)feList.item(s);
                    double top = Double.parseDouble(el.getElementsByTagName("Top").item(0).getTextContent());
                    double bottom = Double.parseDouble(el.getElementsByTagName("Bottom").item(0).getTextContent());
                    double right = Double.parseDouble(el.getElementsByTagName("Right").item(0).getTextContent());
                    double left = Double.parseDouble(el.getElementsByTagName("Left").item(0).getTextContent());
                    fullExtent = new BoundingBox(left, bottom, right, top);
                }
            }
            
            // get the current extent
            NodeList ceList = doc.getElementsByTagName("CurrentExtent");
            for (int s = 0; s < ceList.getLength(); s++) {
                if(ceList.item(s).getNodeType() == Node.ELEMENT_NODE){
                    Element el = (Element)ceList.item(s);
                    double top = Double.parseDouble(el.getElementsByTagName("Top").item(0).getTextContent());
                    double bottom = Double.parseDouble(el.getElementsByTagName("Bottom").item(0).getTextContent());
                    double right = Double.parseDouble(el.getElementsByTagName("Right").item(0).getTextContent());
                    double left = Double.parseDouble(el.getElementsByTagName("Left").item(0).getTextContent());
                    currentExtent = new BoundingBox(left, bottom, right, top);
                }
            }
            
            NodeList layersList = doc.getElementsByTagName("Layer");

            int nlayers = layersList.getLength();
            
            for(int s = 0; s < nlayers; s++){
                if(layersList.item(s).getNodeType() == Node.ELEMENT_NODE){
                    Element el = (Element)layersList.item(s);
                    // get the layer type.
                    String layertype = el.getElementsByTagName("LayerType").item(0).getTextContent();
                    String layertitle = el.getElementsByTagName("LayerTitle").item(0).getTextContent();
                    boolean visibility = Boolean.parseBoolean(el.getElementsByTagName("IsVisible").item(0).getTextContent());
                    int overlayNumber = Integer.parseInt(el.getElementsByTagName("OverlayNum").item(0).getTextContent());
                    if (layertype.equals("RASTER")) {
                        String headerFile = el.getElementsByTagName("HeaderFile").item(0).getTextContent();
                        // see whether it exists, and if it doesn't, see whether a file of the same
                        // name exists in the working directory or an of its subdirectories.
                        if (!new File(headerFile).exists()) {
                            flag = true;
                            findFile(new File(workingDirectory), new File(headerFile).getName());
                            if (!retFile.equals("")) {
                                headerFile = retFile;
                            } else {
                                return false;
                            }
                        }
                        
                        String paletteFile = el.getElementsByTagName("Palette").item(0).getTextContent();
                        // see whether it exists, and if it doesn't, see whether a file of the same
                        // name exists in the working directory or an of its subdirectories.
                        if (!new File(paletteFile).exists()) {
                            flag = true;
                            findFile(new File(paletteDirectory), new File(paletteFile).getName());
                            if (!retFile.equals("")) {
                                paletteFile = retFile;
                            } else {
                                paletteFile = paletteDirectory + "spectrum.pal";
                            }
                        }
                        
                        int alpha = Integer.parseInt(el.getElementsByTagName("Alpha").item(0).getTextContent());
                        double nonlinearity = Double.parseDouble(el.getElementsByTagName("Nonlinearity").item(0).getTextContent());
                        boolean paletteReversed = Boolean.parseBoolean(el.getElementsByTagName("IsPaletteReversed").item(0).getTextContent());
                        double displaymin = Double.parseDouble(el.getElementsByTagName("DisplayMinVal").item(0).getTextContent());
                        double displaymax = Double.parseDouble(el.getElementsByTagName("DisplayMaxVal").item(0).getTextContent());
                        
                        RasterLayerInfo rli = new RasterLayerInfo(headerFile, paletteFile, alpha, overlayNumber);
                        rli.setDisplayMinVal(displaymin);
                        rli.setDisplayMaxVal(displaymax);
                        rli.setNonlinearity(nonlinearity);
                        rli.setPaletteReversed(paletteReversed);
                        rli.setLayerTitle(layertitle);
                        
                        layers.add(rli);
                        numLayers = layers.size();
        
                    } else if (layertype.equals("VECTOR")) {
                        String shapeFile = el.getElementsByTagName("ShapeFile").item(0).getTextContent();
                        // see whether it exists, and if it doesn't, see whether a file of the same
                        // name exists in the working directory or an of its subdirectories.
                        if (!new File(shapeFile).exists()) {
                            flag = true;
                            findFile(new File(workingDirectory), new File(shapeFile).getName());
                            if (!retFile.equals("")) {
                                shapeFile = retFile;
                            } else {
                                return false;
                            }
                        }
                        
                        int alpha = Integer.parseInt(el.getElementsByTagName("Alpha").item(0).getTextContent());
                        Color fillColour = new Color(Integer.parseInt(el.getElementsByTagName("FillColour").item(0).getTextContent()));
                        Color lineColour = new Color(Integer.parseInt(el.getElementsByTagName("LineColour").item(0).getTextContent()));
                        float lineThickness = Float.parseFloat(el.getElementsByTagName("LineThickness").item(0).getTextContent());
                        float markerSize = Float.parseFloat(el.getElementsByTagName("MarkerSize").item(0).getTextContent());
                        boolean isFilled = Boolean.parseBoolean(el.getElementsByTagName("IsFilled").item(0).getTextContent());
                        boolean isOutlined = Boolean.parseBoolean(el.getElementsByTagName("IsOutlined").item(0).getTextContent());
                        boolean isDashed = Boolean.parseBoolean(el.getElementsByTagName("IsDashed").item(0).getTextContent());
                        String[] dashArrayStr = el.getElementsByTagName("DashArray").item(0).getTextContent().split(",");
                        float[] dashArray = new float[dashArrayStr.length];
                        for (int a = 0; a < dashArray.length; a++) {
                            dashArray[a] = Float.parseFloat(dashArrayStr[a]);
                        }
                        String markerStyleStr = el.getElementsByTagName("MarkerStyle").item(0).getTextContent();
                        MarkerStyle markerStyle = PointMarkers.findMarkerStyleFromString(markerStyleStr);
                        
                        boolean isFilledWithOneColour = Boolean.parseBoolean(el.getElementsByTagName("IsFilledWithOneColour").item(0).getTextContent());
                        boolean isOutlinedWithOneColour = Boolean.parseBoolean(el.getElementsByTagName("IsOutlinedWithOneColour").item(0).getTextContent());
                        boolean isPaletteScaled = Boolean.parseBoolean(el.getElementsByTagName("IsPaletteScaled").item(0).getTextContent());
                        String colouringAttribute = el.getElementsByTagName("ColouringAttribute").item(0).getTextContent();
                        String paletteFile = el.getElementsByTagName("PaletteFile").item(0).getTextContent();
                        // see whether it exists, and if it doesn't, see whether a file of the same
                        // name exists in the working directory or an of its subdirectories.
                        if (!new File(paletteFile).exists()) {
                            flag = true;
                            findFile(new File(paletteDirectory), new File(paletteFile).getName());
                            if (!retFile.equals("")) {
                                paletteFile = retFile;
                            } else {
                                paletteFile = paletteDirectory + "spectrum.pal";
                            }
                        }
                        
                        VectorLayerInfo vli = new VectorLayerInfo(shapeFile, alpha, overlayNumber);
                        vli.setVisible(visibility);
                        vli.setFillColour(fillColour);
                        vli.setLineColour(lineColour);
                        vli.setLineThickness(lineThickness);
                        vli.setMarkerSize(markerSize);
                        vli.setFilled(isFilled);
                        vli.setOutlined(isOutlined);
                        vli.setDashed(isDashed);
                        vli.setDashArray(dashArray);
                        vli.setMarkerStyle(markerStyle);
                        vli.setLayerTitle(layertitle);
                        vli.setFilledWithOneColour(isFilledWithOneColour);
                        vli.setOutlinedWithOneColour(isOutlinedWithOneColour);
                        vli.setPaletteScaled(isPaletteScaled);
                        vli.setPaletteFile(paletteFile);
                        vli.setFillAttribute(colouringAttribute);
                        vli.setRecordsColourData();
                        
                        layers.add(vli);
                        numLayers = layers.size();
        
                    }
//                    String palette = 
//                    RasterLayerInfo newLayer = new RasterLayerInfo(files[i].toString(), paletteDirectory,
//                        defaultPalettes, 255, openMaps.get(mapNum).getNumLayers());
//                    addLayer(activeLayer);
                    //Element firstPersonElement = (Element)firstPersonNode;
                }
            }
            
            setActiveLayer(activeLayerNum);
            
            return true;
        } catch (ParserConfigurationException pce) {
            System.out.println(pce);
            return false;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
    
    private String retFile = "";
    private boolean flag = true;
    private void findFile(File dir, String fileName) {
        if (flag) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    findFile(files[i], fileName);
                } else if (files[i].getName().equals(fileName)) {
                    retFile = files[i].toString();
                    flag = false;
                    break;
                }
            }
        }
    }
    
    public boolean openMap() {
        return open();
    }
    
    public boolean openMap(String fileName) {
        this.fileName = fileName;
        return open();
    }
}
