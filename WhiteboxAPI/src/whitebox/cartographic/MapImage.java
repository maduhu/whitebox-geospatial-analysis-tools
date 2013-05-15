/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import whitebox.interfaces.CartographicElement;

/**
 *
 * @author johnlindsay
 */
public class MapImage implements CartographicElement, Comparable<CartographicElement> {

    private String cartoElementType = "MapImage";
    private int upperLeftX = 0;
    private int upperLeftY = 0;
    private int height = -1; // in points
    private int width = -1; // in points
    private int imageHeight = -1;
    private int imageWidth = -1;
    private double aspectRatio = 0;
    private boolean visible = true;
    private boolean borderVisible = true;
    private boolean selected = false;
    private Color borderColour = Color.BLACK;
    private float lineWidth = 0.75f;
    private int number = -1;
    private String name = "MapImage";
    private int selectedOffsetX;
    private int selectedOffsetY;
    private String fileName = "";
    private BufferedImage bufferedImage = null;
    private boolean maintainAspectRatio = false;

    public MapImage() {
        // no-arg constructor
    }

    public MapImage(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
        init();
    }

    private void init() {
        try {
            // figure out the width and hieght of an image
            File imgSrc = new File(fileName);
            if (imgSrc.exists()) {
                BufferedImage img = ImageIO.read(imgSrc);
                width = img.getWidth(null);
                height = img.getHeight(null);
                bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics g = bufferedImage.getGraphics();
                g.drawImage(img, 0, 0, null);
            }
        } catch (Exception e) {
            // do nothing.
        }
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        init();
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
        if (maintainAspectRatio) {
            this.height = (int) (width / aspectRatio);
        }
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
        if (maintainAspectRatio) {
            this.width = (int) (height * aspectRatio);
        }
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

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public BufferedImage getBufferedImage() {
        if (bufferedImage == null) {
            init();
        }
        return bufferedImage;
    }

    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        imageWidth = width;
        imageHeight = height;
        aspectRatio = (double) (width) / height;
        this.maintainAspectRatio = maintainAspectRatio;
    }

    @Override
    public void resize(int x, int y, int resizeMode) {
        int minSize = 1;
        int deltaX, deltaY;
        int w = width, h = height;
        switch (resizeMode) {
            case 0: // off the north edge
                deltaY = y - upperLeftY;
                if (h - deltaY >= minSize) {
                    upperLeftY = y;
                    h -= deltaY;
                }
                break;
            case 1: // off the south edge
                deltaY = y - (upperLeftY + h);
                if (h + deltaY >= minSize) {
                    h += deltaY;
                }
                break;
            case 2: // off the east edge
                deltaX = x - (upperLeftX + w);
                if (w + deltaX >= minSize) {
                    w += deltaX;
                }
                break;
            case 3: // off the west edge
                deltaX = x - upperLeftX;
                if (w - deltaX >= minSize) {
                    upperLeftX = x;
                    w -= deltaX;
                }
                break;
            case 4: // off the northeast edge
                deltaY = y - upperLeftY;
                if (h - deltaY >= minSize) {
                    upperLeftY = y;
                    h -= deltaY;
                }
                deltaX = x - (upperLeftX + w);
                if (w + deltaX >= minSize) {
                    w += deltaX;
                }
                break;
            case 5: // off the northwest edge
                deltaY = y - upperLeftY;
                if (h - deltaY >= minSize) {
                    upperLeftY = y;
                    h -= deltaY;
                }
                deltaX = x - upperLeftX;
                if (w - deltaX >= minSize) {
                    upperLeftX = x;
                    w -= deltaX;
                }
                break;
            case 6: // off the southeast edge
                deltaY = y - (upperLeftY + h);
                if (h + deltaY >= minSize) {
                    h += deltaY;
                }
                deltaX = x - (upperLeftX + w);
                if (w + deltaX >= minSize) {
                    w += deltaX;
                }
                break;
            case 7: // off the southwest edge
                deltaY = y - (upperLeftY + h);
                if (h + deltaY >= minSize) {
                    h += deltaY;
                }
                deltaX = x - upperLeftX;
                if (w - deltaX >= minSize) {
                    upperLeftX = x;
                    w -= deltaX;
                }
                break;
        }
        // this will take care of the case of maintainAspectRatio being set to true;
        height = h;
        setWidth(w);
    }

    @Override
    public CartographicElement.CartographicElementType getCartographicElementType() {
        return CartographicElement.CartographicElementType.NEATLINE;
    }
}
