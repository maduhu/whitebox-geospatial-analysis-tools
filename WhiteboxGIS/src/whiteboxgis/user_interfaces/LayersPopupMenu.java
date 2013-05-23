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
package whiteboxgis.user_interfaces;

import javax.swing.*;
import java.awt.event.ActionListener;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.interfaces.MapLayer;

/**
 *
 * @author johnlindsay
 */
public class LayersPopupMenu extends JPopupMenu {
    // global variables

    MapLayer myLayer;
    String graphicsDirectory;
    ActionListener listener;

    // constructors
    public LayersPopupMenu() {
        // no-arg constructor
    }

    public LayersPopupMenu(MapLayer layer, ActionListener listener, String graphicsDirectory) {
        this.myLayer = layer;
        this.graphicsDirectory = graphicsDirectory;
        this.listener = listener;
        initialize();
    }

    // properties
    public MapLayer getMyLayer() {
        return myLayer;
    }

    public void setMyLayer(MapLayer myLayer) {
        this.myLayer = myLayer;
    }

    public String getGraphicsDirectory() {
        return graphicsDirectory;
    }

    public void setGraphicsDirectory(String graphicsDirectory) {
        this.graphicsDirectory = graphicsDirectory;
    }

    public ActionListener getListener() {
        return listener;
    }

    public void setListener(ActionListener listener) {
        this.listener = listener;
    }
    
    
    // methods
    public void initialize() {
        if (myLayer == null || graphicsDirectory == null || listener == null) {
            System.err.println("LayersPopupMenu has not been properly initialized");
        }

        if (myLayer.getLayerType() == MapLayer.MapLayerType.VECTOR) {

            JMenuItem mi = new JMenuItem("Layer Display Properties");
            mi.addActionListener(listener);
            mi.setActionCommand("layerProperties");
            this.add(mi);

            JMenuItem menuItemAttributeTable = new JMenuItem("View Attribute Table", new ImageIcon(graphicsDirectory + "AttributeTable.png"));
            menuItemAttributeTable.addActionListener(listener);
            menuItemAttributeTable.setActionCommand("viewAttributeTable");
            this.add(menuItemAttributeTable);

            this.addSeparator();

            mi = new JMenuItem("Toggle Layer Visibility");
            mi.addActionListener(listener);
            mi.setActionCommand("toggleLayerVisibility");
            this.add(mi);

            mi = new JMenuItem("Change Layer Title");
            mi.addActionListener(listener);
            mi.setActionCommand("changeLayerTitle");
            this.add(mi);

            mi = new JMenuItem("Set As Active Layer");
            mi.addActionListener(listener);
            mi.setActionCommand("setAsActiveLayer");
            this.add(mi);

            mi = new JMenuItem("Toggle Layer Visibility In Legend");
            mi.addActionListener(listener);
            mi.setActionCommand("toggleLayerVisibilityInLegend");
            this.add(mi);

            this.addSeparator();

            mi = new JMenuItem("Add Layer", new ImageIcon(graphicsDirectory + "AddLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("addLayer");
            this.add(mi);

            mi = new JMenuItem("Remove Layer", new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("removeLayer");
            this.add(mi);

            this.addSeparator();

            mi = new JMenuItem("Raise Layer", new ImageIcon(graphicsDirectory + "PromoteLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("raiseLayer");
            this.add(mi);

            mi = new JMenuItem("Lower Layer", new ImageIcon(graphicsDirectory + "DemoteLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("lowerLayer");
            this.add(mi);

            mi = new JMenuItem("Layer to Top", new ImageIcon(graphicsDirectory + "LayerToTop.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("layerToTop");
            this.add(mi);

            mi = new JMenuItem("Layer to Bottom", new ImageIcon(graphicsDirectory + "LayerToBottom.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("layerToBottom");
            this.add(mi);

            this.addSeparator();

            mi = new JMenuItem("Zoom To Layer", new ImageIcon(graphicsDirectory + "ZoomToActiveLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("zoomToLayer");
            this.add(mi);

            mi = new JMenuItem("Clip Layer to Current Extent");
            mi.addActionListener(listener);
            mi.setActionCommand("clipLayerToExtent");
            this.add(mi);

            this.addSeparator();

            JCheckBoxMenuItem editLayerMenuItem = new JCheckBoxMenuItem("Edit Layer", new ImageIcon(graphicsDirectory + "Digitize.png"));
            editLayerMenuItem.addActionListener(listener);
            editLayerMenuItem.setActionCommand("editVector");
            this.add(editLayerMenuItem);

            VectorLayerInfo vli = (VectorLayerInfo) myLayer;
            if (vli.isActivelyEdited()) {
                editLayerMenuItem.setState(true);

                mi = new JMenuItem("Digitize New Feature", new ImageIcon(graphicsDirectory + "DigitizeNewFeature.png"));
                mi.addActionListener(listener);
                mi.setActionCommand("digitizeNewFeature");
                this.add(mi);

                mi = new JMenuItem("Move Feature Nodes", new ImageIcon(graphicsDirectory + "MoveNodes.png"));
                mi.addActionListener(listener);
                mi.setActionCommand("moveNodes");
                this.add(mi);

                mi = new JMenuItem("Delete Selected Feature");
                mi.addActionListener(listener);
                mi.setActionCommand("deleteSelectedFeature");
                this.add(mi);

            } else {
                editLayerMenuItem.setState(false);
            }


        } else if (myLayer.getLayerType() == MapLayer.MapLayerType.RASTER) {

            JMenuItem mi = new JMenuItem("Layer Display Properties");
            mi.addActionListener(listener);
            mi.setActionCommand("layerProperties");
            this.add(mi);

            JMenuItem menuItemHisto = new JMenuItem("View Histogram");
            menuItemHisto.addActionListener(listener);
            menuItemHisto.setActionCommand("viewHistogram");
            this.add(menuItemHisto);

            this.addSeparator();

            mi = new JMenuItem("Toggle Layer Visibility");
            mi.addActionListener(listener);
            mi.setActionCommand("toggleLayerVisibility");
            this.add(mi);

            mi = new JMenuItem("Change Layer Title");
            mi.addActionListener(listener);
            mi.setActionCommand("changeLayerTitle");
            this.add(mi);

            mi = new JMenuItem("Set As Active Layer");
            mi.addActionListener(listener);
            mi.setActionCommand("setAsActiveLayer");
            this.add(mi);

            JMenuItem menuChangePalette = new JMenuItem("Change Palette");
            menuChangePalette.addActionListener(listener);
            menuChangePalette.setActionCommand("changePalette");
            this.add(menuChangePalette);

            JMenuItem menuReversePalette = new JMenuItem("Reverse Palette");
            menuReversePalette.addActionListener(listener);
            menuReversePalette.setActionCommand("reversePalette");
            this.add(menuReversePalette);

            mi = new JMenuItem("Toggle Layer Visibility In Legend");
            mi.addActionListener(listener);
            mi.setActionCommand("toggleLayerVisibilityInLegend");
            this.add(mi);

            this.addSeparator();

            mi = new JMenuItem("Add Layer", new ImageIcon(graphicsDirectory + "AddLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("addLayer");
            this.add(mi);

            mi = new JMenuItem("Remove Layer", new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("removeLayer");
            this.add(mi);

            this.addSeparator();

            mi = new JMenuItem("Raise Layer", new ImageIcon(graphicsDirectory + "PromoteLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("raiseLayer");
            this.add(mi);

            mi = new JMenuItem("Lower Layer", new ImageIcon(graphicsDirectory + "DemoteLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("lowerLayer");
            this.add(mi);

            mi = new JMenuItem("Layer to Top", new ImageIcon(graphicsDirectory + "LayerToTop.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("layerToTop");
            this.add(mi);

            mi = new JMenuItem("Layer to Bottom", new ImageIcon(graphicsDirectory + "LayerToBottom.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("layerToBottom");
            this.add(mi);

            this.addSeparator();

            mi = new JMenuItem("Zoom To Layer", new ImageIcon(graphicsDirectory + "ZoomToActiveLayer.png"));
            mi.addActionListener(listener);
            mi.setActionCommand("zoomToLayer");
            this.add(mi);

            mi = new JMenuItem("Clip Layer to Current Extent");
            mi.addActionListener(listener);
            mi.setActionCommand("clipLayerToExtent");
            this.add(mi);
        }

        this.setOpaque(true);
        this.setLightWeightPopupEnabled(true);

    }
}
