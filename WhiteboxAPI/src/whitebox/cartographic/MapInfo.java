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
package whitebox.cartographic;

import java.awt.Font;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import whitebox.interfaces.CartographicElement;
import whitebox.structures.BoundingBox;

/**
 * This class is used to manage the layers and properties of maps. The actual
 * map display is handled by the MapRenderer class.
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MapInfo implements java.io.Serializable {
    // Fields.

    private String mapName = "";
    private transient boolean dirty = false;
    private String fileName = "";
    private boolean pageVisible = true;
    private PageFormat pageFormat = new PageFormat();
    private double margin = 0.0;
    private int numMapAreas = 0;
    private BoundingBox pageBox = new BoundingBox();
    private boolean showInLegend = true;
    private ArrayList<CartographicElement> listOfCartographicElements = new ArrayList<>();
    private Font defaultFont = new Font("SanSerif", Font.PLAIN, 11);

    /**
     * MapInfo constructor
     */
    public MapInfo(String mapTitle) {
        try {
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
            Paper paper = pageFormat.getPaper();
            double width = paper.getWidth();
            double height = paper.getHeight();
            double marginInPoints = margin * 72;
            paper.setImageableArea(marginInPoints, marginInPoints,
                    width - 2 * marginInPoints, height - 2 * marginInPoints);
            pageFormat.setPaper(paper);

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public MapInfo() {
        // no-arg constructor
        pageFormat.setOrientation(PageFormat.LANDSCAPE);
        Paper paper = pageFormat.getPaper();
        double width = paper.getWidth();
        double height = paper.getHeight();
        double marginInPoints = margin * 72;
        paper.setImageableArea(marginInPoints, marginInPoints,
                width - 2 * marginInPoints, height - 2 * marginInPoints);
        pageFormat.setPaper(paper);

    }

    public final void addNewCartographicElement(CartographicElement ce) {
        ce.setElementNumber(listOfCartographicElements.size());
        listOfCartographicElements.add(ce);
        if (ce instanceof MapArea) {
            numMapAreas++;
            mapAreas.add((MapArea) ce);
            activeMapArea = ce.getElementNumber();
        } //else if (ce instanceof CartographicElementGroup) {
//            // see if it contains any MapAreas
//            CartographicElementGroup ceg = (CartographicElementGroup) ce;
//            List<CartographicElement> myCEs = ceg.getElementList();
//            for (CartographicElement ce2 : myCEs) {
//                if (ce instanceof MapArea) {
//                    numMapAreas++;
//                    mapAreas.add((MapArea) ce);
//                    activeMapArea = ce.getElementNumber();
//                }
//            }
//        }
    }

    public void removeCartographicElement(int elementNumber) {
        try {
            Collections.sort(listOfCartographicElements);
            listOfCartographicElements.remove(elementNumber);
            // re-order the elements
            Collections.sort(listOfCartographicElements);
            int i = 0;
            for (CartographicElement ce : listOfCartographicElements) {
                ce.setElementNumber(i);
                i++;
            }
            if (elementNumber == activeMapArea) { activeMapArea = -1; }
            mapAreas.clear();
            numMapAreas = 0;
            for (CartographicElement ce : listOfCartographicElements) {
                if (ce instanceof MapArea) {
                    mapAreas.add((MapArea) ce);
                    numMapAreas++;
                    activeMapArea = ce.getElementNumber();
                }
            }
            Collections.sort(listOfCartographicElements);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public void removeAllCartographicElements() {
        listOfCartographicElements.clear();
        mapAreas.clear();
        numMapAreas = 0;
        activeMapArea = -1;
    }

    public ArrayList<CartographicElement> getCartographicElementList() {
        Collections.sort(listOfCartographicElements);
        return listOfCartographicElements;
    }

    public CartographicElement getCartographicElement(int n) {
        if (n >= 0) {
            Collections.sort(listOfCartographicElements);
            return listOfCartographicElements.get(n);
        } else {
            return null;
        }
    }

    public void deslectAllCartographicElements() {
        for (CartographicElement ce : listOfCartographicElements) {
            ce.setSelected(false);
        }
        activeMapArea = -1;
    }

    public void zoomToPage() {
        pageBox.setMinX(Float.NEGATIVE_INFINITY);
        pageBox.setMinY(Float.NEGATIVE_INFINITY);
        pageBox.setMaxX(Float.NEGATIVE_INFINITY);
        pageBox.setMaxY(Float.NEGATIVE_INFINITY);
    }

    /**
     * Used to zoom out of the map page
     *
     * @param x, x-coordinate of the new centre point
     * @param y, y-coordinate of the new centre point
     */
    public void zoomOut(int x, int y) {
        double rangeX = Math.abs(pageBox.getMaxX() - pageBox.getMinX());
        double rangeY = Math.abs(pageBox.getMaxY() - pageBox.getMinY());
        pageBox.setMinX(x - (rangeX * 1.15) / 2.0);
        pageBox.setMinY(y - (rangeY * 1.15) / 2.0);
        pageBox.setMaxX(x + (rangeX * 1.15) / 2.0);
        pageBox.setMaxY(y + (rangeY * 1.15) / 2.0);
    }

    /**
     * Used to zoom into the map page
     *
     * @param x, x-coordinate of the new centre point
     * @param y, y-coordinate of the new centre point
     */
    public void zoomIn(int x, int y) {
        double rangeX = Math.abs(pageBox.getMaxX() - pageBox.getMinX());
        double rangeY = Math.abs(pageBox.getMaxY() - pageBox.getMinY());
        pageBox.setMinX(x - (rangeX * 0.85) / 2.0);
        pageBox.setMinY(y - (rangeY * 0.85) / 2.0);
        pageBox.setMaxX(x + (rangeX * 0.85) / 2.0);
        pageBox.setMaxY(y + (rangeY * 0.85) / 2.0);
    }

    public void zoom(int x, int y, double factor) {
        double rangeX = Math.abs(pageBox.getMaxX() - pageBox.getMinX());
        double rangeY = Math.abs(pageBox.getMaxY() - pageBox.getMinY());
        pageBox.setMinX(x - (rangeX * factor) / 2.0);
        pageBox.setMinY(y - (rangeY * factor) / 2.0);
        pageBox.setMaxX(x + (rangeX * factor) / 2.0);
        pageBox.setMaxY(y + (rangeY * factor) / 2.0);
    }

    public void addMapTitle() {
        // how many map titles are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapTitle) {
                i++;
            }
        }
        String name = "MapTitle" + (i + 1);
        MapTitle ce = new MapTitle(getMapName(), name);
        ce.setLabelFont(new Font(defaultFont.getName(), Font.BOLD, 20));
        addNewCartographicElement(ce);
    }

    public void addMapTextArea() {
        // how many map titles are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapTextArea) {
                i++;
            }
        }
        String name = "MapTextArea" + (i + 1);
        MapTextArea ce = new MapTextArea(name);
        ce.setLabelFont(defaultFont);
        addNewCartographicElement(ce);
    }

    public void addMapScale() {
        // how many map scales are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapScale) {
                i++;
            }
        }
        String name = "MapScale" + (i + 1);
        MapScale ms = new MapScale(name);
        ms.setMapArea(getActiveMapArea());
        ms.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
        addNewCartographicElement((CartographicElement) ms);
    }

    public void addNorthArrow() {
        // how many north arrows are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof NorthArrow) {
                i++;
            }
        }
        String name = "NorthArrow" + (i + 1);
        CartographicElement ce = new NorthArrow(name);
        addNewCartographicElement(ce);
    }

    public void addNeatline() {
        // how many neat lines are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof Neatline) {
                i++;
            }
            ce.setElementNumber(ce.getElementNumber() + 1);
        }
        String name = "Neatline" + (i + 1);
        CartographicElement ce = new Neatline(name);
        ce.setElementNumber(0); // neatlines are added to the bottom of the list
        listOfCartographicElements.add(ce);
    }

    public void addMapImage(String fileName) {
        // how many neat lines are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapImage) {
                i++;
            }
        }
        String name = "MapImage" + (i + 1);
        CartographicElement ce = new MapImage(name, fileName);
        addNewCartographicElement(ce);
    }

    public void addLegend() {
        // how many legends are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof Legend) {
                i++;
            }
        }
        String name = "Legend" + (i + 1);
        Legend ce = new Legend(name);
        for (MapArea ma : mapAreas) {
            ce.addMapArea(ma);
        }
        ce.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
        addNewCartographicElement(ce);
    }
    private transient ArrayList<MapArea> mapAreas = new ArrayList<MapArea>();

    public void addMapArea() {
        // how many map areas are there already?
        int i = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce instanceof MapArea) {
                i++;
            }
        }
        String name = "MapArea" + (i + 1);
        MapArea ce = new MapArea(name);
        ce.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
        addNewCartographicElement(ce);
    }

    public ArrayList<MapArea> getMapAreas() {
        return mapAreas;
    }

    public void promoteMapElement(int index) {
        Collections.sort(listOfCartographicElements);
        if (index < listOfCartographicElements.size() - 1 && index >= 0) {
            listOfCartographicElements.get(index).setElementNumber(index + 1);
            listOfCartographicElements.get(index + 1).setElementNumber(index);
        }
        Collections.sort(listOfCartographicElements);
        activeMapArea = -1;
    }

    public void demoteMapElement(int index) {
        Collections.sort(listOfCartographicElements);
        if (index <= listOfCartographicElements.size() - 1 && index > 0) {
            listOfCartographicElements.get(index).setElementNumber(index - 1);
            listOfCartographicElements.get(index - 1).setElementNumber(index);
        }
        Collections.sort(listOfCartographicElements);
        activeMapArea = -1;
    }

    public void modifyElement(int elementNumber, CartographicElement ce) {
        Collections.sort(listOfCartographicElements);
        listOfCartographicElements.set(elementNumber, ce);
    }

    // Properties
    public void setWorkingDirectory(String directory) {
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String title) {
        mapName = title;
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

    public Font getDefaultFont() {
        return defaultFont;
    }

    public void setDefaultFont(Font font) {
        this.defaultFont = font;

    }

    public BoundingBox getPageExtent() {
        return pageBox.clone();
    }

    public void setPageExtent(BoundingBox extent) {
        pageBox = extent.clone();
    }

    public boolean isPageVisible() {
        return pageVisible;
    }

    public void setPageVisible(boolean pageVisible) {
        this.pageVisible = pageVisible;
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

    public int getNumberOfCartographicElements() {
        return listOfCartographicElements.size();
    }
    // Methods
    private int activeMapArea = -1;

    public int getActiveMapAreaElementNumber() {
        if (activeMapArea < 0) {
            return findActiveMapArea();
        } else {
            return activeMapArea;
        }
    }

    public MapArea getActiveMapArea() {
        if (activeMapArea < 0) {
            getActiveMapAreaElementNumber();
        }
        for (MapArea mapArea : mapAreas) {
            if (mapArea.getElementNumber() == activeMapArea) {
                return mapArea;
            }
        }
        return null;
    }

    public void setActiveMapAreaByElementNum(int elementNum) {
        activeMapArea = elementNum;
    }

    public MapArea getMapAreaByElementNum(int elementNum) {
        for (MapArea mapArea : mapAreas) {
            if (mapArea.getElementNumber() == elementNum) {
                return mapArea;
            }
        }
        return null;
    }

    public int howManyElementsAreSelected() {
        int numSelectedElements = 0;
        for (CartographicElement ce : listOfCartographicElements) {
            if (ce.isSelected()) {
                numSelectedElements++;
            }
        }
        return numSelectedElements;
    }

    public boolean centerSelectedElementsVertically() {
        try {
            int x, x2;
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                x = 0;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        x += ce.getUpperLeftX() + (ce.getLowerRightX() - ce.getUpperLeftX()) / 2;
                    }
                }
                x = x / numSelectedElements; // AVERAGE MIDPOINT
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        x2 = x - (ce.getLowerRightX() - ce.getUpperLeftX()) / 2;
                        ce.setUpperLeftX(x2);
                    }
                }

            } else if (numSelectedElements == 1) { // center in page
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        int width = ce.getLowerRightX() - ce.getUpperLeftX();
                        x = (int) (pageFormat.getWidth() / 2 - width / 2);
                        ce.setUpperLeftX(x);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean centerSelectedElementsHorizontally() {
        try {
            int y, y2;
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                y = 0;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        y += ce.getLowerRightY() + (ce.getUpperLeftY() - ce.getLowerRightY()) / 2;
                    }
                }
                y = (int) (y / numSelectedElements); // AVERAGE MIDPOINT
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        y2 = y + (ce.getUpperLeftY() - ce.getLowerRightY()) / 2;
                        ce.setUpperLeftY(y2);
                    }
                }

            } else if (numSelectedElements == 1) { // center in page
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        int height = ce.getLowerRightY() - ce.getUpperLeftY();
                        y = (int) (pageFormat.getHeight() / 2 - height / 2);
                        ce.setUpperLeftY(y);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean alignSelectedElementsRight() {
        try {
            int x, width;
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                x = Integer.MIN_VALUE;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        if (ce.getLowerRightX() > x) {
                            x = ce.getLowerRightX();
                        }
                    }
                }
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        width = ce.getLowerRightX() - ce.getUpperLeftX();
                        ce.setUpperLeftX(x - width);
                    }
                }

            } else if (numSelectedElements == 1) {
                x = (int) (pageFormat.getWidth() - (margin * 72));
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        width = ce.getLowerRightX() - ce.getUpperLeftX();
                        ce.setUpperLeftX(x - width);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean alignSelectedElementsLeft() {
        try {
            int x;
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                x = Integer.MAX_VALUE;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        if (ce.getUpperLeftX() < x) {
                            x = ce.getUpperLeftX();
                        }
                    }
                }
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        ce.setUpperLeftX(x);
                    }
                }

            } else if (numSelectedElements == 1) {
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        ce.setUpperLeftX((int) (margin * 72));
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean alignSelectedElementsTop() {
        try {
            int y;
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                y = Integer.MAX_VALUE;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        if (ce.getUpperLeftY() < y) {
                            y = ce.getUpperLeftY();
                        }
                    }
                }
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        ce.setUpperLeftY(y);
                    }
                }

            } else if (numSelectedElements == 1) {
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        ce.setUpperLeftY((int) ((margin * 72)));
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean alignSelectedElementsBottom() {
        try {
            int y, height;
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                y = Integer.MIN_VALUE;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        if (ce.getLowerRightY() > y) {
                            y = ce.getLowerRightY();
                        }
                    }
                }
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        height = ce.getLowerRightY() - ce.getUpperLeftY();
                        ce.setUpperLeftY(y - height);
                    }
                }

            } else if (numSelectedElements == 1) {
                y = (int) (pageFormat.getHeight() - (margin * 72));
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        height = ce.getLowerRightY() - ce.getUpperLeftY();
                        ce.setUpperLeftY(y - height);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean distributeSelectedElementsVertically() {
        try {
            int y, i, top, topElementNum = 0, bottom, bottomElementNum = 0;
            int numSelectedElements = howManyElementsAreSelected();
            int[] selectedElementNum = new int[numSelectedElements];
            int[] midPoints = new int[numSelectedElements];
            int[] heights = new int[numSelectedElements];
            int[] order = new int[numSelectedElements];
            boolean[] ordered = new boolean[numSelectedElements];
            int totalHeight = 0;
            if (numSelectedElements > 2) { // need to have at least 3 elements
                top = Integer.MAX_VALUE;
                bottom = Integer.MIN_VALUE;
                i = 0;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        selectedElementNum[i] = ce.getElementNumber();
                        heights[i] = ce.getLowerRightY() - ce.getUpperLeftY();
                        totalHeight += heights[i];
                        midPoints[i] = ce.getUpperLeftY() + heights[i] / 2;
                        if (ce.getUpperLeftY() < top) {
                            top = ce.getUpperLeftY();
                            topElementNum = i;
                        }
                        if (ce.getLowerRightY() > bottom) {
                            bottom = ce.getLowerRightY();
                            bottomElementNum = i;
                        }
                        i++;
                    }
                }

                int gapSpace = (bottom - top) - totalHeight;
                int avgSpace = gapSpace / (numSelectedElements - 1);

                // sort out the order from top to bottom elements
                order[0] = topElementNum;
                ordered[topElementNum] = true;
                order[numSelectedElements - 1] = bottomElementNum;
                ordered[bottomElementNum] = true;
                // now for the intermediate elements
                boolean flag = true;
                i = 1;
                do {
                    // find the next lowest mid-point
                    int j = Integer.MAX_VALUE;
                    int k = -1;
                    for (int a = 0; a < numSelectedElements; a++) {
                        if (!ordered[a] && midPoints[a] < j) {
                            j = midPoints[a];
                            k = a;
                        }
                    }
                    if (k >= 0) {
                        order[i] = k;
                        ordered[k] = true;
                    } else {
                        flag = false;
                    }
                    i++;
                } while (flag);

                for (int a = 1; a < (numSelectedElements - 1); a++) { // intermediate elements
                    int thisElement = order[a];
                    int previousElement = order[a - 1];
                    CartographicElement cePrev = listOfCartographicElements.get(selectedElementNum[previousElement]);
                    CartographicElement ce = listOfCartographicElements.get(selectedElementNum[thisElement]);
                    y = cePrev.getLowerRightY() + avgSpace;
                    ce.setUpperLeftY(y);
                }

            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean distributeSelectedElementsHorizontally() {
        try {
            int x, i, left, leftElementNum = 0, right, rightElementNum = 0;
            int numSelectedElements = howManyElementsAreSelected();
            int[] selectedElementNum = new int[numSelectedElements];
            int[] midPoints = new int[numSelectedElements];
            int[] widths = new int[numSelectedElements];
            int[] order = new int[numSelectedElements];
            boolean[] ordered = new boolean[numSelectedElements];
            int totalWidth = 0;
            if (numSelectedElements > 2) { // need to have at least 3 elements
                left = Integer.MAX_VALUE;
                right = Integer.MIN_VALUE;
                i = 0;
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        selectedElementNum[i] = ce.getElementNumber();
                        widths[i] = ce.getLowerRightX() - ce.getUpperLeftX();
                        totalWidth += widths[i];
                        midPoints[i] = ce.getUpperLeftX() + widths[i] / 2;
                        if (ce.getUpperLeftX() < left) {
                            left = ce.getUpperLeftX();
                            leftElementNum = i;
                        }
                        if (ce.getLowerRightX() > right) {
                            right = ce.getLowerRightX();
                            rightElementNum = i;
                        }
                        i++;
                    }
                }

                int gapSpace = (right - left) - totalWidth;
                int avgSpace = gapSpace / (numSelectedElements - 1);

                // sort out the order from top to bottom elements
                order[0] = leftElementNum;
                ordered[leftElementNum] = true;
                order[numSelectedElements - 1] = rightElementNum;
                ordered[rightElementNum] = true;
                // now for the intermediate elements
                boolean flag = true;
                i = 1;
                do {
                    // find the next lowest mid-point
                    int j = Integer.MAX_VALUE;
                    int k = -1;
                    for (int a = 0; a < numSelectedElements; a++) {
                        if (!ordered[a] && midPoints[a] < j) {
                            j = midPoints[a];
                            k = a;
                        }
                    }
                    if (k >= 0) {
                        order[i] = k;
                        ordered[k] = true;
                    } else {
                        flag = false;
                    }
                    i++;
                } while (flag);

                for (int a = 1; a < (numSelectedElements - 1); a++) { // intermediate elements
                    int thisElement = order[a];
                    int previousElement = order[a - 1];
                    CartographicElement cePrev = listOfCartographicElements.get(selectedElementNum[previousElement]);
                    CartographicElement ce = listOfCartographicElements.get(selectedElementNum[thisElement]);
                    x = cePrev.getLowerRightX() + avgSpace;
                    ce.setUpperLeftX(x);
                }

            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean groupElements() {
        try {
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 1) {
                int howManyGroups = 0;
                List<CartographicElement> myCEs = new ArrayList<>();
                List<Integer> selectedNums = new ArrayList<>();
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        ce.setSelected(false);
                        myCEs.add(ce);
                        selectedNums.add(ce.getElementNumber());
                    }
                    if (ce instanceof CartographicElementGroup) {
                        howManyGroups++;
                    }
                }

                // now remove the non-group elments so there aren't duplicates
                for (int i = (numSelectedElements - 1); i >= 0; i--) {
                    removeCartographicElement(selectedNums.get(i));
                }

                String name = "Group" + String.valueOf(howManyGroups + 1);
                CartographicElementGroup ceg = new CartographicElementGroup(name, myCEs);
                ceg.setSelected(true);
                addNewCartographicElement(ceg);

            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean ungroupElements() {
        try {
            int numSelectedElements = howManyElementsAreSelected();
            if (numSelectedElements > 0) {
                List<CartographicElementGroup> selectedCEGs = new ArrayList<>();
                List<CartographicElement> otherElements = new ArrayList<>();
                for (CartographicElement ce : listOfCartographicElements) {
                    if (ce.isSelected()) {
                        if (ce instanceof CartographicElementGroup) {
                            selectedCEGs.add((CartographicElementGroup) ce);
                        } else {
                            otherElements.add(ce);
                        }
                    } else {
                        otherElements.add(ce);
                    }
                }
                
                removeAllCartographicElements();
                
                for (CartographicElement ce : otherElements) {
                    addNewCartographicElement(ce);
                }
                
                for (CartographicElementGroup ceg : selectedCEGs) {
                    List<CartographicElement> myCEs = ceg.getElementList();
                    for (CartographicElement ce2 : myCEs) {
                        addNewCartographicElement(ce2);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int findActiveMapArea() {
        // if there is only one MapArea then return it's element number
        if (numMapAreas == 1) {
            activeMapArea = mapAreas.get(0).getElementNumber();
        } else if (numMapAreas > 1) {
            // return the element number of the first selected mapArea
            boolean foundSelectedMapArea = false;
            for (MapArea ma : mapAreas) {
                if (ma.isSelected()) {
                    activeMapArea = ma.getElementNumber();
                    foundSelectedMapArea = true;
                }
            }
            if (!foundSelectedMapArea) {
                // return the top map.
                Collections.sort(mapAreas);
                activeMapArea = mapAreas.get(0).getElementNumber();
            }
        }// else {
//            // you need to add a mapArea
//            String name = "MapArea1";
//            CartographicElement ce = new MapArea(name);
//            addNewCartographicElement(ce);
//            findActiveMapArea();
//        }
        return activeMapArea;
    }
//    private transient String retFile = "";
//    private transient boolean flag = true;
//    private void findFile(File dir, String fileName) {
//        if (flag) {
//            File[] files = dir.listFiles();
//            for (int i = 0; i < files.length; i++) {
//                if (files[i].isDirectory()) {
//                    findFile(files[i], fileName);
//                } else if (files[i].getName().equals(fileName)) {
//                    retFile = files[i].toString();
//                    flag = false;
//                    break;
//                }
//            }
//        }
//    }
//    public boolean openMap() {
//        return open();
//    }
//    
//    public boolean openMap(String fileName) {
//        this.fileName = fileName;
//        return open();
//    }
}
