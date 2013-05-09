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

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapTextArea implements CartographicElement, Comparable<CartographicElement>, java.io.Serializable {

    private String cartoElementType = "MapTextArea";
    boolean visible = true;
    boolean selected = false;
    String name = "MapTextArea";
    int number = -1;
    String label = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in "
            + "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla "
            + "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.";
    boolean borderVisible = false;
    boolean backgroundVisible = false;
    int upperLeftX = 0;
    int upperLeftY = 0;
    int height = 200; // in points
    int width = 280; // in points
    int margin = 5;
    Color backColour = Color.WHITE;
    Color borderColour = Color.BLACK;
    Color fontColour = Color.BLACK;
    Font labelFont = new Font("SanSerif", Font.PLAIN, 12);
//    Font[] availableFonts; // = {"Serif", "SanSerif", "Monospaced", "Dialog", "DialogInput"};
    int fontHeight = 0;
    int maxFontSize = 300;
    float lineWidth = 0.75f;
    private int selectedOffsetX;
    private int selectedOffsetY;
    private float interlineSpace = 1.25f;

    public MapTextArea() {
        measureFontSizes();
//        findAvailableFonts();
    }

    public MapTextArea(String name) {
        this.name = name;
        measureFontSizes();
//        findAvailableFonts();
    }

    public MapTextArea(String label, String name) {
        this.label = label;
        //GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        //Font[] availableFonts = e.getAllFonts();
        this.name = name;
        measureFontSizes();
//        findAvailableFonts();
    }
    static int[] fontHeights = new int[300];
    static int[] fontWidths = new int[300];

    private void measureFontSizes() {
        fontHeights = new int[maxFontSize];
        fontWidths = new int[maxFontSize];
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = bi.getGraphics();
        int style = labelFont.getStyle();
        for (int i = 1; i < fontHeights.length; i++) {
            Font font = new Font(labelFont.getName(), style, i);
            FontMetrics metrics = g.getFontMetrics(font);
            fontWidths[i] = metrics.stringWidth(label);
            fontHeights[i] = metrics.getHeight();
        }
        g.dispose();
        bi = null;
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public boolean isBackgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean backgroundVisible) {
        this.backgroundVisible = backgroundVisible;
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
        measureFontSizes();
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color outlineColour) {
        this.borderColour = outlineColour;
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

    public void setLowerRightX(int lowerRightX) {
        width = lowerRightX - upperLeftX;
    }

    public void setLowerRightY(int lowerRightY) {
        height = upperLeftY - lowerRightY;
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

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        measureFontSizes();
    }

    public int getFontHeight() {
        return fontHeight;
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

    public int getMaxFontSize() {
        return maxFontSize;
    }

    public void setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public float getInterlineSpace() {
        return interlineSpace;
    }

    public void setInterlineSpace(float interlineSpace) {
        this.interlineSpace = interlineSpace;
    }

//    public Font[] getAvailableFonts() {
//        return availableFonts;
//    }
    
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
    public void resize(int x, int y, int resizeMode) {
        try {
            int minSize = 50;
            int deltaX, deltaY;
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public CartographicElement.CartographicElementType getCartographicElementType() {
        return CartographicElement.CartographicElementType.MAPTEXTAREA;
    }
    
//    private void findAvailableFonts() {
//        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        availableFonts = e.getAllFonts();
////        for (Font f : availableFonts) {
////            System.out.println(f.getFontName());
////        }
//    }
}
