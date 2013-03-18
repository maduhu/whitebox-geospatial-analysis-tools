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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import whitebox.cartographic.MapArea;
import whitebox.geospatialfiles.RasterLayerInfo;
import whitebox.geospatialfiles.WhiteboxRasterBase.DataScale;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.interfaces.MapLayer;
import whitebox.interfaces.MapLayer.MapLayerType;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.geospatialfiles.VectorLayerInfo.LegendEntry;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.cartographic.SampleVector;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class LegendEntryPanel extends JPanel implements ItemListener, 
        MouseMotionListener, MouseListener {

    private Color selectionForeground, selectionBackground,
      textForeground, textBackground;
    private DecimalFormat df = null;
    private JCheckBox check = new JCheckBox();
    private MapLayer mapLayer = null;
    private boolean selected = false;
    private WhiteboxPluginHost host = null;
    private String mapTitle;
    private String mapAreaName;
    private JLabel titleLabel;
    private Font myFont;
    private int leftMarginSize = 10;
    private int mapNum = -1;
    private int layerNum = -1;
    private int mapAreaNum = -1;
    private int legendEntryType;
    
    public LegendEntryPanel(MapLayer layer, WhiteboxPluginHost host, Font font, 
       int mapNum, int mapAreaNum, int layerNum, boolean isSelected) {
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        textForeground = renderer.getTextNonSelectionColor();
        textBackground = renderer.getBackgroundNonSelectionColor();
        selectionForeground = renderer.getTextSelectionColor();
        selectionBackground = renderer.getBackgroundSelectionColor();
        renderer = null;
        
        df = new DecimalFormat("###,##0.000#");
        mapLayer = layer;
        check.setOpaque(false);
        check.setSelected(layer.isVisible());
        
        this.host = host;
        this.myFont = font;
        this.selected = isSelected;
        this.mapNum = mapNum;
        this.layerNum = layerNum;
        this.mapAreaNum = mapAreaNum;
        this.legendEntryType = 1;
        createMapLayerLegendEntry();
        
    }
    
    public LegendEntryPanel(String mapTitle, WhiteboxPluginHost host, Font font, 
       int mapNum, int mapAreaNum, int layerNum, boolean isSelected) {
        
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        this.textForeground = renderer.getTextNonSelectionColor();
        this.textBackground = renderer.getBackgroundNonSelectionColor();
        this.selectionForeground = renderer.getTextSelectionColor();
        this.selectionBackground = renderer.getBackgroundSelectionColor();
        renderer = null;
        
        this.mapTitle = mapTitle;
        this.host = host;
        this.myFont = font;
        this.selected = isSelected;
        this.mapNum = mapNum;
        this.layerNum = layerNum;
        this.mapAreaNum = mapAreaNum;
        this.legendEntryType = 0;
        
        createMapLegendEntry();
    }
    
    public LegendEntryPanel(MapArea mapArea, WhiteboxPluginHost host, Font font, 
       int mapNum, int mapAreaNum, int layerNum, boolean isSelected) {
        
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        this.textForeground = renderer.getTextNonSelectionColor();
        this.textBackground = renderer.getBackgroundNonSelectionColor();
        this.selectionForeground = renderer.getTextSelectionColor();
        this.selectionBackground = renderer.getBackgroundSelectionColor();
        renderer = null;
        
        this.mapAreaName = mapArea.getName();
        this.host = host;
        this.myFont = font;
        this.selected = isSelected;
        this.mapNum = mapNum;
        this.layerNum = layerNum;
        this.mapAreaNum = mapAreaNum;
        this.legendEntryType = 2;
        
        createMapAreaLegendEntry();
    }
    
    private void createMapLegendEntry() {
        try {
            this.removeAll();
            String graphicsDirectory = host.getResourcesDirectory() + "Images" + File.separator;
            BufferedImage myPicture = ImageIO.read(new File(graphicsDirectory + "map.png"));
            titleLabel = new JLabel(mapTitle, new ImageIcon(myPicture), JLabel.RIGHT);
            titleLabel.setOpaque(false);
            titleLabel.setFont(myFont);
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.add(Box.createHorizontalStrut(5));
            this.add(titleLabel);
            this.add(Box.createHorizontalGlue());
            //this.setPreferredSize(this.getPreferredSize());
            //this.validate();
            this.setOpaque(true);
            this.setMaximumSize(new Dimension(1000, 15));
            this.addMouseListener(this);
            
            
            if (selected) {
                titleLabel.setForeground(selectionForeground);
                this.setBackground(selectionBackground);
            } else {
                titleLabel.setForeground(textForeground);
                this.setBackground(textBackground);
            }
            
            this.revalidate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void createMapAreaLegendEntry() {
        try {
            this.removeAll();
            String graphicsDirectory = host.getResourcesDirectory() + "Images" + File.separator;
            BufferedImage myPicture = ImageIO.read(new File(graphicsDirectory + "mapArea.png"));
            titleLabel = new JLabel(mapAreaName, new ImageIcon(myPicture), JLabel.RIGHT);
            
            titleLabel.setOpaque(false);
            titleLabel.setFont(myFont);
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.add(Box.createHorizontalStrut(15));
            this.add(titleLabel);
            this.add(Box.createHorizontalGlue());
            
            
            //this.setPreferredSize(this.getPreferredSize());
            //this.validate();
            this.setOpaque(true);
            this.setMaximumSize(new Dimension(1000, 15));
            this.addMouseListener(this);
            
            if (selected) {
                titleLabel.setForeground(selectionForeground);
                this.setBackground(selectionBackground);
            } else {
                titleLabel.setForeground(textForeground);
                this.setBackground(textBackground);
            }
            
            
            this.revalidate();
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void createMapLayerLegendEntry() {
        try {
            this.removeAll();
            int maxHeight = 0;
            boolean isVisible = mapLayer.isVisible();

            Box layerBox = Box.createVerticalBox();

            Box titleBox = Box.createHorizontalBox();
            check.setSelected(isVisible);
            check.addItemListener(this);
            titleBox.add(check);
            titleLabel = new JLabel(mapLayer.getLayerTitle());
            titleLabel.setFont(myFont);
            titleBox.add(titleLabel);
            titleBox.add(Box.createHorizontalGlue());
            layerBox.add(titleBox);
            
            if (mapLayer.getLayerType() == MapLayerType.RASTER) {
                RasterLayerInfo rli = (RasterLayerInfo) mapLayer;

                JLabel maxVal = new JLabel(df.format(rli.getDisplayMaxVal()));
                JLabel minVal = new JLabel(df.format(rli.getDisplayMinVal()));

                //if (isVisible && (rli.getDataScale() != WhiteboxRasterInfo.DataScale.RGB)) {
                if (rli.getDataScale() != WhiteboxRasterInfo.DataScale.RGB) {
                    layerBox.add(Box.createVerticalStrut(5));
                    Box box2 = Box.createHorizontalBox();

                    PaletteImage paletteImage = null;

                    if (!rli.isPaletteReversed()) {
                        paletteImage = new PaletteImage(18, 45, rli.getPaletteFile(), false, PaletteImage.VERTICAL_ORIENTATION);
                    } else {
                        paletteImage = new PaletteImage(18, 45, rli.getPaletteFile(), true, PaletteImage.VERTICAL_ORIENTATION);
                    }
                    if (rli.getDataScale() == DataScale.CATEGORICAL) {
                        paletteImage.isCategorical(true, rli.getMinVal(), rli.getMaxVal());
                    }
                    if (rli.getNonlinearity() != 1) {
                        paletteImage.setNonlinearity(rli.getNonlinearity());
                    }

                    box2.add(Box.createHorizontalStrut(5));
                    box2.add(paletteImage);

                    Box box3 = Box.createVerticalBox();
                    box3.add(maxVal);
                    box3.add(Box.createVerticalGlue());
                    box3.add(minVal);

                    box2.add(Box.createHorizontalStrut(5));
                    box2.add(box3);
                    box2.add(Box.createHorizontalGlue());
                    Box box5 = Box.createVerticalBox();
                    box5.add(box2);
                    box5.add(Box.createVerticalGlue());
                    layerBox.add(box5);
                } else {
                    layerBox.add(Box.createVerticalStrut(2));
                    Box box4 = Box.createHorizontalBox();
                    JLabel notShownLabel = new JLabel("(RGB composite)");
                    notShownLabel.setFont(new Font("SanSerif", Font.ITALIC, 12));
                    box4.add(notShownLabel);
                    box4.add(Box.createHorizontalGlue());
                    layerBox.add(box4);
                }

                layerBox.add(Box.createVerticalStrut(5));

                if (selected) {
                    titleLabel.setForeground(selectionForeground);
                    minVal.setForeground(selectionForeground);
                    maxVal.setForeground(selectionForeground);
                    this.setBackground(selectionBackground);
                } else {
                    titleLabel.setForeground(textForeground);
                    minVal.setForeground(textForeground);
                    maxVal.setForeground(textForeground);
                    this.setBackground(textBackground);
                }
                
                maxHeight = 80;
                
            } else if (mapLayer.getLayerType() == MapLayerType.VECTOR) {
                VectorLayerInfo vli = (VectorLayerInfo) mapLayer;
                ShapeType st = vli.getShapeType();

                LegendEntry[] le = vli.getLegendEntries();
                if (le != null && le[0].getLegendLabel().equals("continuous numerical variable") && le[0].getLegendColour().equals(Color.black)) {
                    // it's a continuous, scaled, numerical variable
                    layerBox.add(Box.createVerticalStrut(5));
                    Box box2 = Box.createHorizontalBox();

                    PaletteImage paletteImage = null;
                    paletteImage = new PaletteImage(18, 50, vli.getPaletteFile(), false, PaletteImage.VERTICAL_ORIENTATION);
                    box2.add(Box.createHorizontalStrut(5));
                    box2.add(paletteImage);

                    JLabel maxVal = new JLabel(df.format(vli.getMaximumValue()));
                    JLabel minVal = new JLabel(df.format(vli.getMinimumValue()));

                    Box box3 = Box.createVerticalBox();
                    box3.add(maxVal);
                    box3.add(Box.createVerticalGlue());
                    box3.add(minVal);

                    box2.add(Box.createHorizontalStrut(5));
                    box2.add(box3);

                    box2.add(Box.createHorizontalGlue());
                    layerBox.add(box2);
                    maxHeight = 80;
                } else {
                    Box sampleVecBox = Box.createHorizontalBox();
                    SampleVector sv = new SampleVector(st, vli, true);
                    sampleVecBox.add(sv);
                    sampleVecBox.add(Box.createHorizontalGlue());
                    layerBox.add(sampleVecBox);
                    maxHeight = sv.getHeight();
                }


                layerBox.add(Box.createVerticalStrut(5));

                
                if (selected) {
                    titleLabel.setForeground(selectionForeground);
                    this.setBackground(selectionBackground);
                } else {
                    titleLabel.setForeground(textForeground);
                    this.setBackground(textBackground);
                }
            }
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Box leftMarginBox = Box.createHorizontalBox();
            leftMarginBox.add(Box.createHorizontalStrut(leftMarginSize));
            leftMarginBox.add(layerBox);
            this.add(leftMarginBox);
            this.setMaximumSize(new Dimension(1000, maxHeight));
            this.addMouseListener(this);
            this.revalidate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void setLeftMarginSize(int leftMarginSize) {
        this.leftMarginSize = leftMarginSize;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (getLegendEntryType() == 0) {
            createMapLegendEntry();
        } else if (getLegendEntryType() == 2) {
            createMapAreaLegendEntry();
        } else {
            createMapLayerLegendEntry();
        }
    }
    
    public int getLegendEntryType() {
        return legendEntryType;
    }

    public void setTitleFont(Font font) {
        if (!font.equals(myFont)) {
            myFont = font;
            if (legendEntryType == 0) {
                createMapLegendEntry();
            } else if (legendEntryType == 1) {
                createMapLayerLegendEntry();
            } else if (legendEntryType == 2) {
                createMapAreaLegendEntry();
            }
        }
    }

    public int getLayerNum() {
        return layerNum;
    }

    public int getMapNum() {
        return mapNum;
    }
    
    public int getMapArea() {
        return mapAreaNum;
    }
    
    @Override
    public void itemStateChanged(ItemEvent ie) {
        mapLayer.setVisible(check.isSelected());
        host.refreshMap(false);
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseMoved(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        WhiteboxGui wb = (WhiteboxGui)host;
        //wb.layersTabMousePress(me, mapNum, layerNum);
        if (me.getClickCount() == 2 && !me.isConsumed()) {
            me.consume();
            wb.layersTabMousePress(me, mapNum, mapAreaNum, layerNum);
        } else if (me.getButton() == 3 || me.isPopupTrigger()) {
            me.consume();
            wb.layersTabMousePress(me, mapNum, mapAreaNum, layerNum);
        } else if (me.getClickCount() == 1 && !me.isConsumed()) {
            me.consume();
            wb.layersTabMousePress(me, mapNum, mapAreaNum, layerNum);
        }
    }

    @Override
    public void mousePressed(MouseEvent me) {

        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseEntered(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseExited(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
