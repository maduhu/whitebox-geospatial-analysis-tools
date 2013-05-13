/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.cartographic;


import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import whitebox.geospatialfiles.RasterLayerInfo;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.MapLayer;
import whitebox.structures.BoundingBox;
import whitebox.structures.GridCell;

/**
 *
 * @author johnlindsay
 */
public class MapArea implements CartographicElement, Comparable<CartographicElement>, java.io.Serializable {
    private String cartoElementType = "MapArea";
    
    private transient MapLayer activeLayer = null;
    private int activeLayerOverlayNumber = -1;
    private int activeLayerIndex = -1;
    private ArrayList<MapLayer> layers = new ArrayList<>();
    private int numLayers = 0;
    private BoundingBox currentExtent = null;
    private int listOfExtentsIndex = -1;
    private ArrayList<BoundingBox> listOfExtents = new ArrayList<>();
    private BoundingBox fullExtent = null;
    private transient boolean dirty = false; 
    private String XYUnits = "";
    int upperLeftX = -32768;
    int upperLeftY = -32768;
    int height = -1; // in points
    int width = -1; // in points
    boolean visible = true;
    boolean borderVisible = true;
    boolean backgroundVisible = true;
    boolean selected = false;
    boolean referenceMarksVisible = true;
    boolean neatlineVisible = false;
    Color borderColour = Color.BLACK;
    Color fontColour = Color.BLACK;
    Color backgroundColour = Color.WHITE;
    Font labelFont = new Font("SanSerif", Font.PLAIN, 10);
    int number = -1;
    String name = "MapArea";
    float lineWidth = 0.75f;
    private static double ppm = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() * 39.3701;
    private double scale = 0;
    private int referenceMarkSize = 10;
    private int selectedOffsetX;
    private int selectedOffsetY;
    private boolean maximumScreenSize = false;
    private double rotation = 0;

    public MapArea(String name) {
        this.name = name;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public int getElementNumber() {
        return number;
    }

    @Override
    public void setElementNumber(int number) {
        this.number = number;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(CartographicElement other) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // compare them based on their element (overlay) numbers
        if (this.number < other.getElementNumber()) {
            return BEFORE;
        } else if (this.number > other.getElementNumber()) {
            return AFTER;
        }

        return EQUAL;
    }

    @Override
    public int getUpperLeftX() {
        return upperLeftX;
    }

    @Override
    public void setUpperLeftX(int upperLeftX) {
        this.upperLeftX = upperLeftX;
    }

    @Override
    public int getUpperLeftY() {
        return upperLeftY;
    }

    @Override
    public void setUpperLeftY(int upperLeftY) {
        this.upperLeftY = upperLeftY;
    }

    @Override
    public int getLowerRightX() {
        return upperLeftX + width;
    }

    @Override
    public int getLowerRightY() {
        return upperLeftY + height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;

    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color borderColour) {
        this.borderColour = borderColour;
    }

    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    public Color getFontColour() {
        return fontColour;
    }

    public void setFontColour(Color fontColour) {
        this.fontColour = fontColour;
    }

    public Font getLabelFont() {
        return labelFont;
    }

    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
    }

    public boolean isReferenceMarksVisible() {
        return referenceMarksVisible;
    }

    public void setReferenceMarksVisible(boolean referenceMarksVisible) {
        this.referenceMarksVisible = referenceMarksVisible;
    }

    public boolean isNeatlineVisible() {
        return neatlineVisible;
    }

    public void setNeatlineVisible(boolean neatlineVisible) {
        this.neatlineVisible = neatlineVisible;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public Color getBackgroundColour() {
        return backgroundColour;
    }

    public void setBackgroundColour(Color backgroundClour) {
        this.backgroundColour = backgroundClour;
    }

    public boolean isBackgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
    }

    public boolean isSizeMaximizedToScreenSize() {
        return maximumScreenSize;
    }

    public void setSizeMaximizedToScreenSize(boolean maximumScreenSize) {
        this.maximumScreenSize = maximumScreenSize;
    }
    
    public int getNumLayers() {
        return numLayers;
    }

    public String getXYUnits() {
        return XYUnits;
    }

    public void setXYUnits(String XYUnits) {
        this.XYUnits = XYUnits;
    }
    
    public int getReferenceMarksSize() {
        return referenceMarkSize;
    }
    
    public void setReferenceMarksSize(int size) {
        this.referenceMarkSize = size;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    
    public double getScale() {
        int viewAreaWidth = width - 2 * referenceMarkSize;
        int viewAreaHeight = height - 2 * referenceMarkSize;
        double xRange = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
        double yRange = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
        scale = 1 / Math.min(((viewAreaWidth / ppm) / xRange), ((viewAreaHeight / ppm) / yRange));       
        return scale;
    }
    
    public void setScale(double scale) {
        // need to update the currentExtent to reflect the new scale
        double viewAreaWidth = ((width - 2 * referenceMarkSize) / ppm);
        double viewAreaHeight = ((height - 2 * referenceMarkSize) / ppm);
        double xRange = Math.abs(currentExtent.getMaxX() - currentExtent.getMinX());
        double yRange = Math.abs(currentExtent.getMaxY() - currentExtent.getMinY());
        double newXRange = scale * viewAreaWidth;
        double deltaX = (newXRange - xRange) / 2;
        currentExtent.setMinX(currentExtent.getMinX() - deltaX);
        currentExtent.setMaxX(currentExtent.getMaxX() + deltaX);
        double newYRange = scale * viewAreaHeight;
        double deltaY = (newYRange - yRange) / 2;
        currentExtent.setMinY(currentExtent.getMinY() - deltaY);
        currentExtent.setMaxY(currentExtent.getMaxY() + deltaY);
        
    }
    
    public int getNumRasterLayers() {
        int i = 0;
        for (int a = 0; a < layers.size(); a++) {
            if (layers.get(a).getLayerType() == MapLayer.MapLayerType.RASTER) {
                i++;
            }
        }
        return i;
    }

    @Override
    public int getSelectedOffsetX() {
        return selectedOffsetX;
    }

    @Override
    public void setSelectedOffsetX(int selectedOffsetX) {
        this.selectedOffsetX = selectedOffsetX;
    }

    @Override
    public int getSelectedOffsetY() {
        return selectedOffsetY;
    }

    @Override
    public void setSelectedOffsetY(int selectedOffsetY) {
        this.selectedOffsetY = selectedOffsetY;
    }
    
    public boolean isActiveLayerAVector() {
        if (activeLayer != null) {
            if (activeLayer.getLayerType() == MapLayer.MapLayerType.VECTOR) {
                return true;
            }
        }
        return false;
    }
    
    public int getSelectedFeatureFromActiveVector() {
        if (activeLayer.getLayerType() == MapLayer.MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo)activeLayer;
            return vli.getSelectedFeatureNumber();
        }
        return -1;
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
    
    BoundingBox currentMapExtent;
    public BoundingBox getCurrentMapExtent() {
        if (currentMapExtent == null) {
            calculateFullExtent();
            currentMapExtent = getFullExtent();
        }
        return currentMapExtent.clone();
    }
    
    public void setCurrentMapExtent(BoundingBox extent) {
        currentMapExtent = extent.clone();
        //addToExtentHistory(extent);
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
        if (activeLayer.getLayerType() == MapLayer.MapLayerType.RASTER) {
            RasterLayerInfo rli = (RasterLayerInfo)activeLayer;
            return rli.getNoDataValue();
        } else {
            return -32768;
        }
    }
    
    public void deselectVectorFeatures() {
        if (activeLayer.getLayerType() == MapLayer.MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo)activeLayer;
            vli.setSelectedFeatureNumber(-1);
        }
    }
    
    public int selectVectorFeatures(double x, double y) {
        if (activeLayer.getLayerType() == MapLayer.MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo)activeLayer;
            return vli.selectFeatureByLocation(x, y);
        }
        return -1; // it should not hit this point
    }

    public boolean isFitToData() {
        if (numLayers > 0) {
            calculateFullExtent();
            double targetAspectRatio = fullExtent.getWidth() / fullExtent.getHeight();
            double currentAspectRatio = (double)(width - 2 * referenceMarkSize) / (height - 2 * referenceMarkSize);
            if (Math.abs(currentAspectRatio - targetAspectRatio) < 0.005) {
                return true;
            }
        }
        return false;
    }
    
    public void setFitToData() {
        fitToData();
    }
    
    public void fitToData() {
        if (numLayers > 0) {
            calculateFullExtent();
            double targetAspectRatio = fullExtent.getWidth() / fullExtent.getHeight();
            double currentAspectRatio = (double)(width - 2 * referenceMarkSize) / (height - 2 * referenceMarkSize);
            if (currentAspectRatio > targetAspectRatio) {
                width = (int)((height - 2 * referenceMarkSize) * targetAspectRatio) + 2 * referenceMarkSize;
            } else {
                height = (int)((width - 2 * referenceMarkSize) / targetAspectRatio) + 2 * referenceMarkSize;
            }
        }
    }
    
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
            if (activeLayer.getLayerType() == MapLayer.MapLayerType.RASTER) {
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
                    int row = (int) ((top - northing) / (top - bottom) * (rows));// - 0.5));
                    int col = (int) ((easting - left) / (right - left) * (columns));// - 0.5));
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
                                int row = (int) ((top - northing) / (top - bottom) * (rows)); // - 0.5));
                                int col = (int) ((easting - left) / (right - left) * (columns)); // - 0.5));
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
    
    public void addLayer(MapLayer newLayer) {
        layers.add(newLayer);
        numLayers = layers.size();
        if (currentExtent == null || currentExtent.getMinX() > currentExtent.getMaxX()) {
            currentExtent = calculateFullExtent();
            listOfExtents.add(currentExtent.clone());
            listOfExtentsIndex = listOfExtents.size() - 1;
        } 
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
                
                if (numLayers == 0) {
                    currentExtent = null;
                } else {
                    calculateFullExtent();
                }
                //currentExtent = calculateFullExtent();
                //listOfExtents.add(currentExtent.clone());
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
        if (layers.get(layerToChange).getLayerType() == MapLayer.MapLayerType.RASTER) {
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
    public MapLayer getLayer(int overlayNumber) {
        try {
            if (overlayNumber >= numLayers) { overlayNumber = numLayers - 1; }
            int i = findLayerIndexByOverlayNum(overlayNumber);
            return layers.get(i);
        } catch (Exception e) {
            return null;
        }
    }
    
    public ArrayList<MapLayer> getLayersList() {
        return layers;
    }
    
    @Override
    public void resize(int x, int y, int resizeMode) {
        int minSize = 50;
        int deltaX = 0;
        int deltaY = 0;
        switch (resizeMode) {
            case 0: // off the north edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSize) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                break;
            case 1: // off the south edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSize) {
                    height += deltaY;
                }
                break;
            case 2: // off the east edge
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSize) {
                    width += deltaX;
                }
                break;
            case 3: // off the west edge
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSize) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
            case 4: // off the northeast edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSize) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSize) {
                    width += deltaX;
                }
                break;
            case 5: // off the northwest edge
                deltaY = y - upperLeftY;
                if (height - deltaY >= minSize) {
                    upperLeftY = y;
                    height -= deltaY;
                }
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSize) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
            case 6: // off the southeast edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSize) {
                    height += deltaY;
                }
                deltaX = x - (upperLeftX + width);
                if (width + deltaX >= minSize) {
                    width += deltaX;
                }
                break;
            case 7: // off the southwest edge
                deltaY = y - (upperLeftY + height);
                if (height + deltaY >= minSize) {
                    height += deltaY;
                }
                deltaX = x - upperLeftX;
                if (width - deltaX >= minSize) {
                    upperLeftX = x;
                    width -= deltaX;
                }
                break;
        }
    }
    
    
    @Override
    public CartographicElementType getCartographicElementType() {
        return CartographicElementType.MAPAREA;
    }
}
