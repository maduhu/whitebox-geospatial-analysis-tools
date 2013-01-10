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

import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.*;
import javax.swing.*;
import whitebox.cartographic.*;
import whitebox.cartographic.properties.ColourProperty;
import whitebox.cartographic.properties.NorthArrowPropertyGrid;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MapProperties extends JDialog implements ActionListener, AdjustmentListener, MouseListener, PropertyChangeListener {
    
    private MapInfo map = null;
    private JButton ok = new JButton("OK");
    private JButton update = new JButton("Update Map");
    private JButton close = new JButton("Close");
    private DecimalFormat df = new DecimalFormat("#0.0");
    private WhiteboxPluginHost host = null;
    private Color backColour = new Color(225, 245, 255);
    private JTabbedPane tabs = new JTabbedPane();
    private JCheckBox checkPageVisible = new JCheckBox();
    private JRadioButton landscape;
    private JRadioButton portrait;
    private JComboBox paperNameCombo;
    private JTextField marginText = null;
    private static double margin;
    
    private ArrayList<CartographicElement> listOfCartographicElements;
    
    private JList mapElementsList;
    
    private JCheckBox checkScaleVisible = new JCheckBox();
    private JCheckBox checkScaleBorderVisible = new JCheckBox();
    private JCheckBox checkScaleShowRF = new JCheckBox();
    private JCheckBox checkScaleBackgroundVisible = new JCheckBox();
    private JTextField scaleUnitText = null;
    private JTextField scaleWidthText = null;
    private JTextField scaleHeightText = null;
    private JTextField scaleMarginText = null;
    
    private ColourProperty outlineColourBox;
    
    private JCheckBox checkNeatlineVisible = new JCheckBox();
    private JCheckBox checkNeatlineDoubleLine = new JCheckBox();
    private JCheckBox checkNeatlineBackgroundVisible = new JCheckBox();
    
    private JCheckBox checkMapAreaVisible = new JCheckBox();
    private JCheckBox checkMapAreaBorderVisible = new JCheckBox();
    private JCheckBox checkMapAreaBackgroundVisible = new JCheckBox();
    private JCheckBox checkMapAreaReferenceMarksVisible = new JCheckBox();
    private JCheckBox checkMapAreaNeatlineVisible = new  JCheckBox();
    
    private JPanel elementPropertiesPanel = new JPanel();
    private JList possibleElementsList = new JList(new DefaultListModel());
    
    private int activeElement;
    
    private int sampleWidth = 30;
    private int sampleHeight = 15;
    
    public MapProperties(Frame owner, boolean modal, MapInfo map) {
        super(owner, modal);
        if (owner != null) {
            Dimension parentSize = owner.getSize(); 
            Point p = owner.getLocation(); 
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        
        this.host = (WhiteboxPluginHost)(owner);
        this.map = map;
        createGui();
        this.tabs.setSelectedIndex(1);
    }
    
    public MapProperties(Frame owner, boolean modal, MapInfo map, int activeElement) {
        super(owner, modal);
        if (owner != null) {
            Dimension parentSize = owner.getSize(); 
            Point p = owner.getLocation(); 
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        
        this.host = (WhiteboxPluginHost)(owner);
        this.map = map;
        //this.activeTab = activeTab.toLowerCase();
        this.activeElement = activeElement;
        
        createGui();
    }
    
    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        if (map == null) {
            System.err.println("Map not set.");
            return;
        }
        
        setTitle("Map Properties: " + map.getMapName());
        
        createPageSizeMap();
        
        // okay and close buttons.
        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalGlue());
        box1.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        ok.setToolTipText("Save changes and exit");
        box1.add(Box.createRigidArea(new Dimension(5, 30)));
        box1.add(update);
        update.setActionCommand("update");
        update.addActionListener(this);
        update.setToolTipText("Save changes without exiting Layer Properties");
        box1.add(Box.createRigidArea(new Dimension(5, 30)));
        box1.add(close);
        close.setActionCommand("close");
        close.addActionListener(this);
        close.setToolTipText("Exit without saving changes");
        box1.add(Box.createHorizontalStrut(100));
        box1.add(Box.createHorizontalGlue());
        
        add(box1, BorderLayout.SOUTH);
       
        tabs.addTab("Map Elements", getMapElementsListing());
        tabs.addTab("Page", getPageBox());
       
        getContentPane().add(tabs, BorderLayout.CENTER);
        
        pack();
    }
    
    JPanel elementsPanel = new JPanel();
    private JPanel getMapElementsListing() {
        
        try {
            if (activeElement < 0) { activeElement = 0; }
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            
            MouseListener ml1 = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    JList theList = (JList) e.getSource();
                    String label = null;
                    int index = theList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Object o = theList.getModel().getElementAt(index);
                        label = o.toString();
                    }
                    if (e.getClickCount() == 1) {
                        
                    } else if (e.getClickCount() == 2) {
                        addElement(label);
                    }

                }
            };
            
            MouseListener ml2 = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        updateElementPropertiesPanel();
                    }

                }
            };


            JPanel listBox = new JPanel();
            listBox.setLayout(new BoxLayout(listBox, BoxLayout.X_AXIS));
            listBox.setBackground(Color.WHITE);
            listBox.add(Box.createHorizontalStrut(10));
            
            Box vbox = Box.createVerticalBox();
            Box hbox = Box.createHorizontalBox();
            label = new JLabel("Carto Elements:");
            label.setForeground(Color.darkGray);
            //Font f = label.getFont();
            //label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            hbox.add(label);
            hbox.add(Box.createHorizontalGlue());
            vbox.add(hbox);
            
            //JList possibleElementsList = new JList(new DefaultListModel());
            possibleElementsList.addMouseListener(ml1);

            DefaultListModel model = new DefaultListModel();
            model.add(0, "Title");
            model.add(1, "Scale");
            model.add(2, "Legend");
            model.add(3, "North Arrow");
            model.add(4, "Neatline");
            model.add(5, "Text Box");
            model.add(6, "Map Area");
            model.add(7, "Label");
            
            possibleElementsList.setModel(model);
            
            JScrollPane scroller1 = new JScrollPane(possibleElementsList);
            vbox.add(scroller1);
            
            
            hbox = Box.createHorizontalBox();
            JButton addButton = new JButton("Add");
            addButton.setActionCommand("addElement");
            addButton.addActionListener(this);
            hbox.add(Box.createHorizontalGlue());
            hbox.add(addButton);
            vbox.add(hbox);
            
            listBox.add(vbox);
            
            listBox.add(Box.createHorizontalStrut(10));
            
            vbox = Box.createVerticalBox();
            label = new JLabel("Current Map Elements:");
            label.setForeground(Color.darkGray);
            hbox = Box.createHorizontalBox();
            hbox.add(label);
            hbox.add(Box.createHorizontalGlue());
            vbox.add(hbox);
            
            mapElementsList = new JList(new DefaultListModel());
            mapElementsList.addMouseListener(ml2);
            populateElementsList();
            
            JScrollPane scroller2 = new JScrollPane(mapElementsList);
            vbox.add(scroller2);
            
            hbox = Box.createHorizontalBox();
            JButton deleteButton = new JButton("Remove");
            deleteButton.setActionCommand("removeElement");
            deleteButton.addActionListener(this);
            hbox.add(Box.createHorizontalGlue());
            hbox.add(deleteButton);
            vbox.add(hbox);
            
            listBox.add(vbox);
            
            vbox = Box.createVerticalBox();
            JButton elementUpButton = new JButton(String.valueOf('\u25B2'));
            elementUpButton.setActionCommand("elementUp");
            elementUpButton.addActionListener(this);
            vbox.add(elementUpButton);
            JButton elementDownButton = new JButton(String.valueOf('\u25BC'));
            elementDownButton.setActionCommand("elementDown");
            elementDownButton.addActionListener(this);
            vbox.add(elementDownButton);
            listBox.add(vbox);
            
            listBox.setMaximumSize(new Dimension(2000, 150));
            
            mainBox.add(listBox);
            
            vbox = Box.createVerticalBox();
            vbox.add(Box.createVerticalStrut(10));
            
            hbox = Box.createHorizontalBox();
            hbox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Elements Properties:");
            //f = label.getFont();
            //label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            label.setForeground(Color.darkGray);
            hbox.add(label);
            hbox.add(Box.createHorizontalGlue());
            vbox.add(hbox);
            //mainBox.add(hbox);
            
            elementPropertiesPanel.setBackground(Color.WHITE);
            vbox.add(elementPropertiesPanel);
            
            mainBox.add(vbox);
            mainBox.add(Box.createVerticalGlue());
            
            //mainBox.add(Box.createVerticalStrut(330));
            
            JScrollPane scroll = new JScrollPane(mainBox);
            elementsPanel.setLayout(new BoxLayout(elementsPanel, BoxLayout.Y_AXIS));
            elementsPanel.add(scroll);
            elementsPanel.add(Box.createVerticalGlue());
            
            if (listOfCartographicElements.size() > 0) {
                mapElementsList.setSelectedIndex(listOfCartographicElements.size() - 1 - activeElement);
                updateElementPropertiesPanel();
            }
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return elementsPanel;
        }
    }
    
    private void updateElementPropertiesPanel() {
        int index = (listOfCartographicElements.size() - 1) - mapElementsList.getSelectedIndex();
        if (index < 0) { index = 0; }
        if (index > listOfCartographicElements.size()) { index = listOfCartographicElements.size(); }
        CartographicElement ce = listOfCartographicElements.get(index);
        elementPropertiesPanel.removeAll();
        //Box box = Box.createHorizontalBox();
        //JScrollPane scroller = new JScrollPane();
        if (ce instanceof MapTitle) {
            elementPropertiesPanel.add(getTitleBox((MapTitle)ce));
        } else if (ce instanceof MapScale) {
            elementPropertiesPanel.add(getScaleBox((MapScale)ce));
        } else if (ce instanceof NorthArrow) {
            elementPropertiesPanel.add(getNorthArrowBox((NorthArrow) ce));
        } else if (ce instanceof NeatLine) {
            elementPropertiesPanel.add(getNeatlineBox((NeatLine) ce));
        } else if (ce instanceof MapArea) {
            elementPropertiesPanel.add(getMapAreaBox((MapArea) ce));
        }
        //elementPropertiesPanel.validate();
        //elementPropertiesPanel.repaint();
        elementsPanel.validate();
        elementsPanel.repaint();
    }
    
    private void populateElementsList() {
        

        listOfCartographicElements = map.getCartographicElementList();
        mapElementsList.removeAll();
        if (listOfCartographicElements.size() > 0) {
            DefaultListModel model = new DefaultListModel();

            // the list is in reverse order so that the bottom element is on the list bottom.
            for (CartographicElement ce : listOfCartographicElements) {
                model.add(0, ce.getName());
            }

            mapElementsList.setModel(model);
        }
        //mapElementsList.update();
            
    }
    
    private JPanel getTitleBox(MapTitle mapTitle) {
        whitebox.cartographic.properties.MapTitlePropertyGrid obj = new whitebox.cartographic.properties.MapTitlePropertyGrid(mapTitle, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
//        JPanel panel = new JPanel();
//        try {
//            int rightMarginSize = 20;
//            int leftMarginSize = 10;
//            
//            panel.setBackground(Color.WHITE);
//            
//            JLabel label = null;
//            Box mainBox = Box.createVerticalBox();
//            JScrollPane scroll = new JScrollPane(mainBox);
//            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//            scroll.setMaximumSize(new Dimension(1000, 230));
//            panel.add(scroll);
//            
//            Font labelFont = mapTitle.getLabelFont();
//            
//            // Title label text
//            JPanel titleLabelBox = new JPanel();
//            titleLabelBox.setLayout(new BoxLayout(titleLabelBox, BoxLayout.X_AXIS));
//            titleLabelBox.setBackground(Color.WHITE);
//            titleLabelBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Label Text:");
//            label.setPreferredSize(new Dimension(180, 24));
//            titleLabelBox.add(label);
//            titleLabelBox.add(Box.createHorizontalGlue());
//            titleLabelText = new JTextField(String.valueOf(mapTitle.getLabel()), 15);
//            titleLabelText.setMaximumSize(new Dimension(40, 22));
//            titleLabelBox.add(titleLabelText);
//            titleLabelBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleLabelBox);
//            
//            // title visibility
//            JPanel titleVisibleBox = new JPanel();
//            titleVisibleBox.setLayout(new BoxLayout(titleVisibleBox, BoxLayout.X_AXIS));
//            titleVisibleBox.setBackground(backColour);
//            titleVisibleBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Is the title visible?");
//            label.setPreferredSize(new Dimension(200, 24));
//            titleVisibleBox.add(label);
//            titleVisibleBox.add(Box.createHorizontalGlue());
//            checkTitleVisible.setSelected(mapTitle.isVisible());
//            checkTitleVisible.addActionListener(this);
//            checkTitleVisible.setActionCommand("checkTitleVisible");
//            titleVisibleBox.add(checkTitleVisible);
//            titleVisibleBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleVisibleBox);
//            
//            whitebox.cartographic.properties.BooleanProperty titleVis = new 
//                    whitebox.cartographic.properties.BooleanProperty("Is the title visible?", 
//                    mapTitle.isVisible());
//            titleVis.setLeftMargin(leftMarginSize);
//            titleVis.setRightMargin(rightMarginSize);
//            titleVis.setBackColour(backColour);
//            titleVis.addPropertyChangeListener("value", this);
//            mainBox.add(titleVis);
//            
//            // Title background visibility
//            JPanel titleBackVisibleBox = new JPanel();
//            titleBackVisibleBox.setLayout(new BoxLayout(titleBackVisibleBox, BoxLayout.X_AXIS));
//            titleBackVisibleBox.setBackground(Color.WHITE);
//            titleBackVisibleBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Is the title background visible?");
//            label.setPreferredSize(new Dimension(220, 24));
//            titleBackVisibleBox.add(label);
//            titleBackVisibleBox.add(Box.createHorizontalGlue());
//            checkTitleBackgroundVisible.setSelected(mapTitle.isBackgroundVisible());
//            checkTitleBackgroundVisible.addActionListener(this);
//            checkTitleBackgroundVisible.setActionCommand("checkTitleBackgroundVisible");
//            titleBackVisibleBox.add(checkTitleBackgroundVisible);
//            titleBackVisibleBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleBackVisibleBox);
//            
//            // Title border visibility
//            JPanel titleBorderVisibleBox = new JPanel();
//            titleBorderVisibleBox.setLayout(new BoxLayout(titleBorderVisibleBox, BoxLayout.X_AXIS));
//            titleBorderVisibleBox.setBackground(backColour);
//            titleBorderVisibleBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Is the title box border visible?");
//            label.setPreferredSize(new Dimension(220, 24));
//            titleBorderVisibleBox.add(label);
//            titleBorderVisibleBox.add(Box.createHorizontalGlue());
//            checkTitleBorderVisible.setSelected(mapTitle.isBorderVisible());
//            checkTitleBorderVisible.addActionListener(this);
//            checkTitleBorderVisible.setActionCommand("checkTitleBorderVisible");
//            titleBorderVisibleBox.add(checkTitleBorderVisible);
//            titleBorderVisibleBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleBorderVisibleBox);
//            
//            // outline visibility
//            JPanel titleOutlineBox = new JPanel();
//            titleOutlineBox.setLayout(new BoxLayout(titleOutlineBox, BoxLayout.X_AXIS));
//            titleOutlineBox.setBackground(Color.WHITE);
//            titleOutlineBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Is the font outline visible?");
//            label.setPreferredSize(new Dimension(200, 24));
//            titleOutlineBox.add(label);
//            titleOutlineBox.add(Box.createHorizontalGlue());
//            checkTitleOutlineVisible.setSelected(mapTitle.isOutlineVisible());
//            checkTitleOutlineVisible.addActionListener(this);
//            checkTitleOutlineVisible.setActionCommand("checkTitleVisible");
//            titleOutlineBox.add(checkTitleOutlineVisible);
//            titleOutlineBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleOutlineBox);
//            
//            // Title margin size
//            JPanel titleMarginBox = new JPanel();
//            titleMarginBox.setLayout(new BoxLayout(titleMarginBox, BoxLayout.X_AXIS));
//            titleMarginBox.setBackground(Color.WHITE);
//            titleMarginBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Margin Size (Points):");
//            label.setPreferredSize(new Dimension(180, 24));
//            titleMarginBox.add(label);
//            titleMarginBox.add(Box.createHorizontalGlue());
//            titleMarginText = new JTextField(String.valueOf(mapTitle.getMargin()), 15);
//            titleMarginText.setHorizontalAlignment(JTextField.RIGHT);
//            titleMarginText.setMaximumSize(new Dimension(40, 22));
//            titleMarginBox.add(titleMarginText);
//            titleMarginBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleMarginBox);
//            
//            // Title label font size
//            JPanel titleFontSizeBox = new JPanel();
//            titleFontSizeBox.setLayout(new BoxLayout(titleFontSizeBox, BoxLayout.X_AXIS));
//            titleFontSizeBox.setBackground(backColour);
//            titleFontSizeBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Font Size:");
//            label.setPreferredSize(new Dimension(180, 24));
//            titleFontSizeBox.add(label);
//            titleFontSizeBox.add(Box.createHorizontalGlue());
//            titleFontSize.setMaximumSize(new Dimension(200, 22));
//            SpinnerModel sm =
//                    new SpinnerNumberModel(labelFont.getSize(), 1, mapTitle.getMaxFontSize(), 1);
//            titleFontSize.setModel(sm);
//            titleFontSizeBox.add(titleFontSize);
//            titleFontSizeBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleFontSizeBox);
//            
//            // title label font bold
//            int fontBold = labelFont.getStyle() & Font.BOLD;
//            JPanel titleFontBoldBox = new JPanel();
//            titleFontBoldBox.setLayout(new BoxLayout(titleFontBoldBox, BoxLayout.X_AXIS));
//            titleFontBoldBox.setBackground(Color.WHITE);
//            titleFontBoldBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Use bold font?");
//            label.setPreferredSize(new Dimension(200, 24));
//            titleFontBoldBox.add(label);
//            titleFontBoldBox.add(Box.createHorizontalGlue());
//            titleFontBold.setSelected(fontBold > 0);
//            titleFontBold.addActionListener(this);
//            //titleFontBold.setActionCommand("checkTitleVisible");
//            titleFontBoldBox.add(titleFontBold);
//            titleFontBoldBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleFontBoldBox);
//            
//            // title label font bold
//            int fontItalicized = labelFont.getStyle() & Font.ITALIC;
//            JPanel titleFontItalicsBox = new JPanel();
//            titleFontItalicsBox.setLayout(new BoxLayout(titleFontItalicsBox, BoxLayout.X_AXIS));
//            titleFontItalicsBox.setBackground(backColour);
//            titleFontItalicsBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Use italicized font?");
//            label.setPreferredSize(new Dimension(200, 24));
//            titleFontItalicsBox.add(label);
//            titleFontItalicsBox.add(Box.createHorizontalGlue());
//            titleFontItalics.setSelected(fontItalicized > 0);
//            titleFontItalics.addActionListener(this);
//            //titleFontItalics.setActionCommand("checkTitleVisible");
//            titleFontItalicsBox.add(titleFontItalics);
//            titleFontItalicsBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(titleFontItalicsBox);
//            
//            JPanel fontColourBox = new JPanel();
//            fontColourBox.setLayout(new BoxLayout(fontColourBox, BoxLayout.X_AXIS));
//            fontColourBox.setBackground(Color.WHITE);
//            fontColourBox.add(Box.createHorizontalStrut(leftMarginSize));
//            label = new JLabel("Font Colour:");
//            label.setPreferredSize(new Dimension(180, 24));
//            fontColourBox.add(label);
//            fontColourBox.add(Box.createHorizontalGlue());
//            fontColour = mapTitle.getFontColour();
//            sampleFontColourPanel = new SampleColour(sampleWidth, sampleHeight, fontColour);
//            sampleFontColourPanel.setToolTipText("Click to select new color.");
//            sampleFontColourPanel.addMouseListener(this);
//            fontColourBox.add(sampleFontColourPanel);
//            fontColourBox.add(Box.createHorizontalStrut(rightMarginSize));
//            mainBox.add(fontColourBox);
//            
//            outlineColourBox = new 
//                    whitebox.cartographic.properties.ColourProperty("Outline Colour:", 
//                    mapTitle.getOutlineColour());
//            outlineColourBox.setLeftMargin(leftMarginSize);
//            outlineColourBox.setRightMargin(rightMarginSize);
//            outlineColourBox.setBackColour(backColour);
//            outlineColourBox.addPropertyChangeListener("colour", this);
//            mainBox.add(outlineColourBox);
//            
//            //mainBox.add(Box.createVerticalStrut(330));
//            
//        } catch (Exception e) {
//            host.showFeedback(e.getMessage());
//        } finally {
//            return panel;
//        }
        
    }
    
    private Color fontColour;
    private SampleColour sampleFontColourPanel;
    
    private JPanel getPageBox() {
        JPanel panel = new JPanel();
        try {
            
            margin = map.getMargin();
            PageFormat pf = map.getPageFormat();
            Paper paper = pf.getPaper();
            
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
        
            JPanel underConstructionBox = new JPanel();
            underConstructionBox.setLayout(new BoxLayout(underConstructionBox, BoxLayout.X_AXIS));
            underConstructionBox.setBackground(Color.WHITE);
            underConstructionBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("This feature is under active development");
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            //label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
            underConstructionBox.add(label);
            underConstructionBox.add(Box.createHorizontalGlue());
            underConstructionBox.add(Box.createHorizontalStrut(10));
            mainBox.add(underConstructionBox);
            
            // page visibility
            JPanel pageVisibleBox = new JPanel();
            pageVisibleBox.setLayout(new BoxLayout(pageVisibleBox, BoxLayout.X_AXIS));
            pageVisibleBox.setBackground(Color.WHITE);
            pageVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Draw the page?");
            label.setPreferredSize(new Dimension(180, 24));
            pageVisibleBox.add(label);
            pageVisibleBox.add(Box.createHorizontalGlue());
            checkPageVisible.setSelected(map.isPageVisible());
            checkPageVisible.addActionListener(this);
            checkPageVisible.setActionCommand("checkPageVisible");
            pageVisibleBox.add(checkPageVisible);
            pageVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(pageVisibleBox);
            
            // page orientation
            JPanel orientationBox = new JPanel();
            orientationBox.setLayout(new BoxLayout(orientationBox, BoxLayout.X_AXIS));
            orientationBox.setBackground(backColour);
            orientationBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Page Orientation:");
            label.setPreferredSize(new Dimension(180, 24));
            orientationBox.add(label);
            orientationBox.add(Box.createHorizontalGlue());
            landscape = new JRadioButton("Landscape", true);
            orientationBox.add(landscape);
            portrait = new JRadioButton("Portrait", true);
            orientationBox.add(portrait);
            orientationBox.add(Box.createHorizontalStrut(10));
            ButtonGroup group = new ButtonGroup();
            group.add(landscape);
            group.add(portrait);
            //Register a listener for the radio buttons.
            landscape.setActionCommand("landscape");
            portrait.setActionCommand("portrait");
            landscape.addActionListener(this);
            portrait.addActionListener(this);
            if (pf.getOrientation() == PageFormat.LANDSCAPE) {
                landscape.setSelected(true);
            } else {
                portrait.setSelected(true);
            }
            mainBox.add(orientationBox);
            
            // page name
            String[] fields = new String[]{"Letter", "Legal", "A0", "A1",
                 "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", 
                 "B0", "B1", "B2"};
            paperNameCombo = new JComboBox(fields);
            JPanel paperNameBox = new JPanel();
            paperNameBox.setLayout(new BoxLayout(paperNameBox, BoxLayout.X_AXIS));
            paperNameBox.setBackground(Color.WHITE);
            paperNameBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Paper Type:");
            label.setPreferredSize(new Dimension(180, 24));
            paperNameBox.add(label);
            paperNameBox.add(Box.createHorizontalGlue());
            // What is the name of the current paper?
            for (Map.Entry<String, Float[]> e : pageSizes.entrySet()) {
                if (e.getValue()[0] == (paper.getWidth() / POINTS_PER_INCH) &&
                        e.getValue()[1] == (paper.getHeight() / POINTS_PER_INCH)) {
                    paperNameCombo.setSelectedItem(e.getKey());
                }
            }
            paperNameCombo.addActionListener(this);
            paperNameCombo.setActionCommand("checkPageVisible");
            paperNameBox.add(paperNameCombo);
            paperNameBox.add(Box.createHorizontalStrut(10));
            mainBox.add(paperNameBox);
            
            // page margins
            JPanel marginBox = new JPanel();
            marginBox.setLayout(new BoxLayout(marginBox, BoxLayout.X_AXIS));
            marginBox.setBackground(backColour);
            marginBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Margin Size:");
            label.setPreferredSize(new Dimension(180, 24));
            marginBox.add(label);
            marginBox.add(Box.createHorizontalGlue());
            marginText = new JTextField(Double.toString(margin), 15);
            marginText.setHorizontalAlignment(JTextField.RIGHT);
            marginText.setMaximumSize(new Dimension(50, 22));
            marginBox.add(marginText);
            marginBox.add(Box.createHorizontalStrut(10));
            mainBox.add(marginBox);
            
            // page height
            JPanel maxBox = new JPanel();
            maxBox.setLayout(new BoxLayout(maxBox, BoxLayout.X_AXIS));
            maxBox.setBackground(Color.white);
            maxBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Display Maximum");
            label.setPreferredSize(new Dimension(180, 24));
            maxBox.add(label);
            maxBox.add(Box.createHorizontalGlue());
//            maxVal = new JTextField(Double.toString(rli.getDisplayMaxVal()), 15);
//            maxVal.setHorizontalAlignment(JTextField.RIGHT);
//            maxVal.setMaximumSize(new Dimension(50, 22));
//            maxBox.add(maxVal);
//            maxValButton = new JButton("Reset");
//            maxValButton.setActionCommand("resetMaximum");
//            maxValButton.addActionListener(this);
//            maxBox.add(maxValButton);
//            maxBox.add(Box.createHorizontalStrut(2));
//            clipUpperTail.setActionCommand("clipUpperTail");
//            clipUpperTail.addActionListener(this);
//            clipAmountUpper = new JTextField("2.0%", 4);
//            clipAmountUpper.setHorizontalAlignment(JTextField.RIGHT);
//            clipAmountUpper.setMaximumSize(new Dimension(50, 22));
//            maxBox.add(clipAmountUpper);
//            maxBox.add(clipUpperTail);
//            maxBox.add(Box.createHorizontalStrut(10));

            
            mainBox.add(Box.createVerticalStrut(330));
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private JPanel getMapAreaBox(MapArea mapArea) {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            // mapArea visibility
            JPanel maVisibleBox = new JPanel();
            maVisibleBox.setLayout(new BoxLayout(maVisibleBox, BoxLayout.X_AXIS));
            maVisibleBox.setBackground(Color.WHITE);
            maVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the map area visible?");
            label.setPreferredSize(new Dimension(200, 24));
            maVisibleBox.add(label);
            maVisibleBox.add(Box.createHorizontalGlue());
            checkMapAreaVisible.setSelected(mapArea.isVisible());
            checkMapAreaVisible.addActionListener(this);
            maVisibleBox.add(checkMapAreaVisible);
            maVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(maVisibleBox);
            
            // mapArea border visibility
            JPanel maBorderVisibleBox = new JPanel();
            maBorderVisibleBox.setLayout(new BoxLayout(maBorderVisibleBox, BoxLayout.X_AXIS));
            maBorderVisibleBox.setBackground(backColour);
            maBorderVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the border visible?");
            label.setPreferredSize(new Dimension(200, 24));
            maBorderVisibleBox.add(label);
            maBorderVisibleBox.add(Box.createHorizontalGlue());
            checkMapAreaBorderVisible.setSelected(mapArea.isBorderVisible());
            checkMapAreaBorderVisible.addActionListener(this);
            maBorderVisibleBox.add(checkMapAreaBorderVisible);
            maBorderVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(maBorderVisibleBox);
            
            // mapArea background visibility
            JPanel mapAreaBackVisibleBox = new JPanel();
            mapAreaBackVisibleBox.setLayout(new BoxLayout(mapAreaBackVisibleBox, BoxLayout.X_AXIS));
            mapAreaBackVisibleBox.setBackground(Color.WHITE);
            mapAreaBackVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the background visible?");
            label.setPreferredSize(new Dimension(220, 24));
            mapAreaBackVisibleBox.add(label);
            mapAreaBackVisibleBox.add(Box.createHorizontalGlue());
            checkMapAreaBackgroundVisible.setSelected(mapArea.isBackgroundVisible());
            checkMapAreaBackgroundVisible.addActionListener(this);
            mapAreaBackVisibleBox.add(checkMapAreaBackgroundVisible);
            mapAreaBackVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(mapAreaBackVisibleBox);
            
            // Reference marks visibility
            JPanel referenceMarksBox = new JPanel();
            referenceMarksBox.setLayout(new BoxLayout(referenceMarksBox, BoxLayout.X_AXIS));
            referenceMarksBox.setBackground(backColour);
            referenceMarksBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Show reference marks?");
            label.setPreferredSize(new Dimension(220, 24));
            referenceMarksBox.add(label);
            referenceMarksBox.add(Box.createHorizontalGlue());
            checkMapAreaReferenceMarksVisible.setSelected(mapArea.isReferenceMarksVisible());
            checkMapAreaReferenceMarksVisible.addActionListener(this);
            referenceMarksBox.add(checkMapAreaReferenceMarksVisible);
            referenceMarksBox.add(Box.createHorizontalStrut(10));
            mainBox.add(referenceMarksBox);
            
            // neatline visibility
            JPanel neatlineBox = new JPanel();
            neatlineBox.setLayout(new BoxLayout(neatlineBox, BoxLayout.X_AXIS));
            neatlineBox.setBackground(Color.WHITE);
            neatlineBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Show neatline?");
            label.setPreferredSize(new Dimension(220, 24));
            neatlineBox.add(label);
            neatlineBox.add(Box.createHorizontalGlue());
            checkMapAreaNeatlineVisible.setSelected(mapArea.isNeatlineVisible());
            checkMapAreaNeatlineVisible.addActionListener(this);
            neatlineBox.add(checkMapAreaNeatlineVisible);
            neatlineBox.add(Box.createHorizontalStrut(10));
            mainBox.add(neatlineBox);
            
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private JPanel getNeatlineBox(NeatLine neatLine) {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            // neatline visibility
            JPanel nlVisibleBox = new JPanel();
            nlVisibleBox.setLayout(new BoxLayout(nlVisibleBox, BoxLayout.X_AXIS));
            nlVisibleBox.setBackground(backColour);
            nlVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the neatline visible?");
            label.setPreferredSize(new Dimension(200, 24));
            nlVisibleBox.add(label);
            nlVisibleBox.add(Box.createHorizontalGlue());
            checkNeatlineVisible.setSelected(neatLine.isVisible());
            checkNeatlineVisible.addActionListener(this);
            nlVisibleBox.add(checkNeatlineVisible);
            nlVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(nlVisibleBox);
            
            // neatline background visibility
            JPanel neatlineBackVisibleBox = new JPanel();
            neatlineBackVisibleBox.setLayout(new BoxLayout(neatlineBackVisibleBox, BoxLayout.X_AXIS));
            neatlineBackVisibleBox.setBackground(Color.WHITE);
            neatlineBackVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the neatline background visible?");
            label.setPreferredSize(new Dimension(220, 24));
            neatlineBackVisibleBox.add(label);
            neatlineBackVisibleBox.add(Box.createHorizontalGlue());
            checkNeatlineBackgroundVisible.setSelected(neatLine.isBackgroundVisible());
            checkNeatlineBackgroundVisible.addActionListener(this);
            neatlineBackVisibleBox.add(checkNeatlineBackgroundVisible);
            neatlineBackVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(neatlineBackVisibleBox);
            
            // Title border visibility
            JPanel neatlineDoubleLineBox = new JPanel();
            neatlineDoubleLineBox.setLayout(new BoxLayout(neatlineDoubleLineBox, BoxLayout.X_AXIS));
            neatlineDoubleLineBox.setBackground(backColour);
            neatlineDoubleLineBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Use double line?");
            label.setPreferredSize(new Dimension(220, 24));
            neatlineDoubleLineBox.add(label);
            neatlineDoubleLineBox.add(Box.createHorizontalGlue());
            checkNeatlineDoubleLine.setSelected(neatLine.isDoubleLine());
            checkNeatlineDoubleLine.addActionListener(this);
            neatlineDoubleLineBox.add(checkNeatlineDoubleLine);
            neatlineDoubleLineBox.add(Box.createHorizontalStrut(10));
            mainBox.add(neatlineDoubleLineBox);
            
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private JPanel getNorthArrowBox(NorthArrow northArrow) {
        NorthArrowPropertyGrid obj = new NorthArrowPropertyGrid(northArrow, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
        //return new NorthArrowPropertyGrid(northArrow, host);
//        JPanel panel = new JPanel();
//        try {
//            JLabel label = null;
//            Box mainBox = Box.createVerticalBox();
//            JScrollPane scroll = new JScrollPane(mainBox);
//            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//            panel.add(scroll);
//            
//            // NA visibility
//            JPanel naVisibleBox = new JPanel();
//            naVisibleBox.setLayout(new BoxLayout(naVisibleBox, BoxLayout.X_AXIS));
//            naVisibleBox.setBackground(Color.WHITE);
//            naVisibleBox.add(Box.createHorizontalStrut(10));
//            label = new JLabel("Is the north arrow visible?");
//            label.setPreferredSize(new Dimension(200, 24));
//            naVisibleBox.add(label);
//            naVisibleBox.add(Box.createHorizontalGlue());
//            checkNAVisible.setSelected(northArrow.isVisible());
//            checkNAVisible.addActionListener(this);
//            checkNAVisible.setActionCommand("checkNAVisible");
//            naVisibleBox.add(checkNAVisible);
//            naVisibleBox.add(Box.createHorizontalStrut(10));
//            mainBox.add(naVisibleBox);
//            
//            // NA background visibility
//            JPanel naBackVisibleBox = new JPanel();
//            naBackVisibleBox.setLayout(new BoxLayout(naBackVisibleBox, BoxLayout.X_AXIS));
//            naBackVisibleBox.setBackground(backColour);
//            naBackVisibleBox.add(Box.createHorizontalStrut(10));
//            label = new JLabel("Is the arrow background visible?");
//            label.setPreferredSize(new Dimension(220, 24));
//            naBackVisibleBox.add(label);
//            naBackVisibleBox.add(Box.createHorizontalGlue());
//            checkNABackgroundVisible.setSelected(northArrow.isBackgroundVisible());
//            checkNABackgroundVisible.addActionListener(this);
//            checkNABackgroundVisible.setActionCommand("checkNABackgroundVisible");
//            naBackVisibleBox.add(checkNABackgroundVisible);
//            naBackVisibleBox.add(Box.createHorizontalStrut(10));
//            mainBox.add(naBackVisibleBox);
//            
//            // NA border visibility
//            JPanel naBorderVisibleBox = new JPanel();
//            naBorderVisibleBox.setLayout(new BoxLayout(naBorderVisibleBox, BoxLayout.X_AXIS));
//            naBorderVisibleBox.setBackground(Color.WHITE);
//            naBorderVisibleBox.add(Box.createHorizontalStrut(10));
//            label = new JLabel("Is the north arrow border visible?");
//            label.setPreferredSize(new Dimension(220, 24));
//            naBorderVisibleBox.add(label);
//            naBorderVisibleBox.add(Box.createHorizontalGlue());
//            checkNABorderVisible.setSelected(northArrow.isBorderVisible());
//            checkNABorderVisible.addActionListener(this);
//            checkNABorderVisible.setActionCommand("checkNABorderVisible");
//            naBorderVisibleBox.add(checkNABorderVisible);
//            naBorderVisibleBox.add(Box.createHorizontalStrut(10));
//            mainBox.add(naBorderVisibleBox);
//            
//            
//            // NA marker size
//            JPanel naMarkerSizeBox = new JPanel();
//            naMarkerSizeBox.setLayout(new BoxLayout(naMarkerSizeBox, BoxLayout.X_AXIS));
//            naMarkerSizeBox.setBackground(backColour);
//            naMarkerSizeBox.add(Box.createHorizontalStrut(10));
//            label = new JLabel("Marker Size (Points):");
//            label.setPreferredSize(new Dimension(180, 24));
//            naMarkerSizeBox.add(label);
//            naMarkerSizeBox.add(Box.createHorizontalGlue());
//            naMarkerSizeText = new JTextField(String.valueOf(northArrow.getMarkerSize()), 15);
//            naMarkerSizeText.setHorizontalAlignment(JTextField.RIGHT);
//            naMarkerSizeText.setMaximumSize(new Dimension(50, 22));
//            naMarkerSizeBox.add(naMarkerSizeText);
//            naMarkerSizeBox.add(Box.createHorizontalStrut(10));
//            mainBox.add(naMarkerSizeBox);
//            
//            // NA margin size
//            JPanel naMarginBox = new JPanel();
//            naMarginBox.setLayout(new BoxLayout(naMarginBox, BoxLayout.X_AXIS));
//            naMarginBox.setBackground(Color.WHITE);
//            naMarginBox.add(Box.createHorizontalStrut(10));
//            label = new JLabel("Margin Size (Points):");
//            label.setPreferredSize(new Dimension(180, 24));
//            naMarginBox.add(label);
//            naMarginBox.add(Box.createHorizontalGlue());
//            naMarginText = new JTextField(String.valueOf(northArrow.getMargin()), 15);
//            naMarginText.setHorizontalAlignment(JTextField.RIGHT);
//            naMarginText.setMaximumSize(new Dimension(50, 22));
//            naMarginBox.add(naMarginText);
//            naMarginBox.add(Box.createHorizontalStrut(10));
//            mainBox.add(naMarginBox);
//            
//            //mainBox.add(Box.createVerticalStrut(330));
//        
//        
//        } catch (Exception e) {
//            host.showFeedback(e.getMessage());
//        } finally {
//            return panel;
//        }
        
    }
    
    private JPanel getLegendBox() {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            JPanel underConstructionBox = new JPanel();
            underConstructionBox.setLayout(new BoxLayout(underConstructionBox, BoxLayout.X_AXIS));
            underConstructionBox.setBackground(Color.WHITE);
            underConstructionBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("This feature is under active development");
            Font f = label.getFont();
            label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            //label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
            underConstructionBox.add(label);
            underConstructionBox.add(Box.createHorizontalGlue());
            underConstructionBox.add(Box.createHorizontalStrut(10));
            mainBox.add(underConstructionBox);
            
            //mainBox.add(Box.createVerticalStrut(330));
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private JPanel getScaleBox(MapScale mapScale) {
        JPanel panel = new JPanel();
        try {
            
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            // scale visibility
            JPanel scaleVisibleBox = new JPanel();
            scaleVisibleBox.setLayout(new BoxLayout(scaleVisibleBox, BoxLayout.X_AXIS));
            scaleVisibleBox.setBackground(Color.WHITE);
            scaleVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the scale visible?");
            label.setPreferredSize(new Dimension(180, 24));
            scaleVisibleBox.add(label);
            scaleVisibleBox.add(Box.createHorizontalGlue());
            checkScaleVisible.setSelected(mapScale.isVisible());
            checkScaleVisible.addActionListener(this);
            checkScaleVisible.setActionCommand("checkScaleVisible");
            scaleVisibleBox.add(checkScaleVisible);
            scaleVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleVisibleBox);
            
            // scale background visibility
            JPanel scaleBackgroundVisibleBox = new JPanel();
            scaleBackgroundVisibleBox.setLayout(new BoxLayout(scaleBackgroundVisibleBox, BoxLayout.X_AXIS));
            scaleBackgroundVisibleBox.setBackground(backColour);
            scaleBackgroundVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the scale background visible?");
            label.setPreferredSize(new Dimension(200, 24));
            scaleBackgroundVisibleBox.add(label);
            scaleBackgroundVisibleBox.add(Box.createHorizontalGlue());
            checkScaleBackgroundVisible.setSelected(mapScale.isBackgroundVisible());
            checkScaleBackgroundVisible.addActionListener(this);
            checkScaleBackgroundVisible.setActionCommand("checkScaleVisible");
            scaleBackgroundVisibleBox.add(checkScaleBackgroundVisible);
            scaleBackgroundVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleBackgroundVisibleBox);
            
            // scale border visibility
            JPanel scaleBorderVisibleBox = new JPanel();
            scaleBorderVisibleBox.setLayout(new BoxLayout(scaleBorderVisibleBox, BoxLayout.X_AXIS));
            scaleBorderVisibleBox.setBackground(Color.WHITE);
            scaleBorderVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the scale border visible?");
            label.setPreferredSize(new Dimension(180, 24));
            scaleBorderVisibleBox.add(label);
            scaleBorderVisibleBox.add(Box.createHorizontalGlue());
            checkScaleBorderVisible.setSelected(mapScale.isBorderVisible());
            checkScaleBorderVisible.addActionListener(this);
            checkScaleBorderVisible.setActionCommand("checkScaleBorderVisible");
            scaleBorderVisibleBox.add(checkScaleBorderVisible);
            scaleBorderVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleBorderVisibleBox);
            
            // scale units
            JPanel scaleUnitBox = new JPanel();
            scaleUnitBox.setLayout(new BoxLayout(scaleUnitBox, BoxLayout.X_AXIS));
            scaleUnitBox.setBackground(backColour);
            scaleUnitBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Scale Units:");
            label.setPreferredSize(new Dimension(180, 24));
            scaleUnitBox.add(label);
            scaleUnitBox.add(Box.createHorizontalGlue());
            scaleUnitText = new JTextField(mapScale.getUnits(), 15);
            scaleUnitText.setHorizontalAlignment(JTextField.RIGHT);
            scaleUnitText.setMaximumSize(new Dimension(50, 22));
            scaleUnitBox.add(scaleUnitText);
            scaleUnitBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleUnitBox);
            
            // scale width
            JPanel scaleWidthBox = new JPanel();
            scaleWidthBox.setLayout(new BoxLayout(scaleWidthBox, BoxLayout.X_AXIS));
            scaleWidthBox.setBackground(Color.WHITE);
            scaleWidthBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Scale Box Width (Points):");
            label.setPreferredSize(new Dimension(180, 24));
            scaleWidthBox.add(label);
            scaleWidthBox.add(Box.createHorizontalGlue());
            scaleWidthText = new JTextField(String.valueOf(mapScale.getWidth()), 15);
            scaleWidthText.setHorizontalAlignment(JTextField.RIGHT);
            scaleWidthText.setMaximumSize(new Dimension(50, 22));
            scaleWidthBox.add(scaleWidthText);
            scaleWidthBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleWidthBox);
            
            
            // scale height
            JPanel scaleHeightBox = new JPanel();
            scaleHeightBox.setLayout(new BoxLayout(scaleHeightBox, BoxLayout.X_AXIS));
            scaleHeightBox.setBackground(backColour);
            scaleHeightBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Scale Box Height (Points):");
            label.setPreferredSize(new Dimension(180, 24));
            scaleHeightBox.add(label);
            scaleHeightBox.add(Box.createHorizontalGlue());
            scaleHeightText = new JTextField(String.valueOf(mapScale.getHeight()), 15);
            scaleHeightText.setHorizontalAlignment(JTextField.RIGHT);
            scaleHeightText.setMaximumSize(new Dimension(50, 22));
            scaleHeightBox.add(scaleHeightText);
            scaleHeightBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleHeightBox);
            
            // scale height
            JPanel scaleMarginBox = new JPanel();
            scaleMarginBox.setLayout(new BoxLayout(scaleMarginBox, BoxLayout.X_AXIS));
            scaleMarginBox.setBackground(Color.WHITE);
            scaleMarginBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Margin Size (Points):");
            label.setPreferredSize(new Dimension(180, 24));
            scaleMarginBox.add(label);
            scaleMarginBox.add(Box.createHorizontalGlue());
            scaleMarginText = new JTextField(String.valueOf(mapScale.getMargin()), 15);
            scaleMarginText.setHorizontalAlignment(JTextField.RIGHT);
            scaleMarginText.setMaximumSize(new Dimension(50, 22));
            scaleMarginBox.add(scaleMarginText);
            scaleMarginBox.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleMarginBox);
            
            // scale representative fraction visibility
            JPanel scaleRFVisible = new JPanel();
            scaleRFVisible.setLayout(new BoxLayout(scaleRFVisible, BoxLayout.X_AXIS));
            scaleRFVisible.setBackground(backColour);
            scaleRFVisible.add(Box.createHorizontalStrut(10));
            label = new JLabel("Show Representative Fraction?");
            label.setPreferredSize(new Dimension(200, 24));
            scaleRFVisible.add(label);
            scaleRFVisible.add(Box.createHorizontalGlue());
            checkScaleShowRF.setSelected(mapScale.isRepresentativeFractionVisible());
            checkScaleShowRF.addActionListener(this);
            checkScaleShowRF.setActionCommand("checkScaleRFVisible");
            scaleRFVisible.add(checkScaleShowRF);
            scaleRFVisible.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleRFVisible);
            
            
            //mainBox.add(Box.createVerticalStrut(330));
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private static Map<String, Float[]> pageSizes = new HashMap<String, Float[]>();
    private static void createPageSizeMap() {
        
        pageSizes.put("Letter", new Float[]{8.5f, 11.0f});
        pageSizes.put("Legal", new Float[]{8.5f, 14.0f});
        pageSizes.put("A0", new Float[]{33.11f, 46.81f});
        pageSizes.put("A1", new Float[]{23.39f, 33.11f});
        pageSizes.put("A2", new Float[]{16.54f, 23.39f});
        pageSizes.put("A3", new Float[]{11.69f, 16.54f});
        pageSizes.put("A4", new Float[]{8.27f, 11.69f});
        pageSizes.put("A5", new Float[]{5.83f, 8.27f});
        pageSizes.put("A6", new Float[]{4.13f, 5.83f});
        pageSizes.put("A7", new Float[]{2.91f, 4.13f});
        pageSizes.put("A8", new Float[]{2.05f, 2.91f});
        pageSizes.put("A9", new Float[]{1.46f, 2.05f});
        pageSizes.put("A10", new Float[]{1.02f, 1.46f});
        pageSizes.put("B0", new Float[]{39.37f, 55.67f});
        pageSizes.put("B1", new Float[]{27.83f, 39.37f});
        pageSizes.put("B2", new Float[]{19.69f, 27.83f});
        
    }
    
    private void updateMap() {
        if (tabs.getSelectedIndex() == 0) {
            // which element is currently selected?
            int whichElement = listOfCartographicElements.size() - 1 - mapElementsList.getSelectedIndex();
            CartographicElement ce = map.getCartographicElement(whichElement);
            if (ce instanceof MapScale) {
                MapScale mapScale = (MapScale) ce;
                if (mapScale.isVisible() != checkScaleVisible.isSelected()) {
                    mapScale.setVisible(checkScaleVisible.isSelected());
                }
                if (mapScale.isBorderVisible() != checkScaleBorderVisible.isSelected()) {
                    mapScale.setBorderVisible(checkScaleBorderVisible.isSelected());
                }
                if (mapScale.isBackgroundVisible() != checkScaleBackgroundVisible.isSelected()) {
                    mapScale.setBackgroundVisible(checkScaleBackgroundVisible.isSelected());
                }
                if (mapScale.isRepresentativeFractionVisible() != checkScaleShowRF.isSelected()) {
                    mapScale.setRepresentativeFractionVisible(checkScaleShowRF.isSelected());
                }
                if (!mapScale.getUnits().equals(scaleUnitText.getText())) {
                    mapScale.setUnits(scaleUnitText.getText());
                }
                if (mapScale.getHeight() != Integer.parseInt(scaleHeightText.getText())) {
                    mapScale.setHeight(Integer.parseInt(scaleHeightText.getText()));
                }
                if (mapScale.getWidth() != Integer.parseInt(scaleWidthText.getText())) {
                    mapScale.setWidth(Integer.parseInt(scaleWidthText.getText()));
                }
                if (mapScale.getMargin() != Integer.parseInt(scaleMarginText.getText())) {
                    mapScale.setMargin(Integer.parseInt(scaleMarginText.getText()));
                }
                //mapScale.setScale(mapScale.getScale()); // this is just to refresh the map scale.
                map.modifyElement(whichElement, ce);
            } else if (ce instanceof NeatLine) {
                NeatLine neatline = (NeatLine)ce;
                if (neatline.isVisible() != checkNeatlineVisible.isSelected()) {
                    neatline.setVisible(checkNeatlineVisible.isSelected());
                }
                if (neatline.isBackgroundVisible() != checkNeatlineBackgroundVisible.isSelected()) {
                    neatline.setBackgroundVisible(checkNeatlineBackgroundVisible.isSelected());
                }
                if (neatline.isDoubleLine() != checkNeatlineDoubleLine.isSelected()) {
                    neatline.setDoubleLine(checkNeatlineDoubleLine.isSelected());
                }
                
            } else if (ce instanceof MapArea) {
                MapArea mapArea = (MapArea)ce;
                if (mapArea.isVisible() != checkMapAreaVisible.isSelected()) {
                    mapArea.setVisible(checkMapAreaVisible.isSelected());
                }
                if (mapArea.isBackgroundVisible() != checkMapAreaBackgroundVisible.isSelected()) {
                    mapArea.setBackgroundVisible(checkMapAreaBackgroundVisible.isSelected());
                }
                if (mapArea.isReferenceMarksVisible() != checkMapAreaReferenceMarksVisible.isSelected()) {
                    mapArea.setReferenceMarksVisible(checkMapAreaReferenceMarksVisible.isSelected());
                }
                if (mapArea.isNeatlineVisible() != checkMapAreaNeatlineVisible.isSelected()) {
                    mapArea.setNeatlineVisible(checkMapAreaNeatlineVisible.isSelected());
                }
                if (mapArea.isBorderVisible() != checkMapAreaBorderVisible.isSelected()) {
                    mapArea.setBorderVisible(checkMapAreaBorderVisible.isSelected());
                }
            }
        } else if (tabs.getSelectedIndex() == 1) {
            map.setPageVisible(checkPageVisible.isSelected());

            PageFormat pf = map.getPageFormat();

            //final Media media = (Media) attributeSet.get(Media.class);
            margin = Double.parseDouble(marginText.getText());
            String paperSize = paperNameCombo.getSelectedItem().toString();
            MediaSize mediaSize = new MediaSize(pageSizes.get(paperSize)[0], pageSizes.get(paperSize)[1], Size2DSyntax.INCH);
            
            boolean changedOrientation = false;
            if (landscape.isSelected() && pf.getOrientation() != PageFormat.LANDSCAPE) {
                pf.setOrientation(PageFormat.LANDSCAPE);
                changedOrientation = true;
            } else if (portrait.isSelected() && pf.getOrientation() != PageFormat.PORTRAIT) {
                pf.setOrientation(PageFormat.PORTRAIT);
                changedOrientation = true;
            }
            
            Paper paper = createPaper(mediaSize);
            if (paper.getHeight() != pf.getPaper().getHeight() || 
                    paper.getWidth() != pf.getPaper().getWidth() ||
                    changedOrientation) {
                pf.setPaper(paper);
                // resize the page extent
                BoundingBox pageExtent = map.getPageExtent();
                pageExtent.setMinX(-6);
                pageExtent.setMinY(-6);
                pageExtent.setMaxX(pf.getWidth() + 12);
                pageExtent.setMaxY(pf.getHeight() + 12);

                map.setPageExtent(pageExtent);

            }
            map.setMargin(margin);
        }
        
        host.refreshMap(true);
    }
    
    private static final double POINTS_PER_INCH = 72.0;
    
    private static Paper createPaper(final MediaSize mediaSize) {
        final Paper paper = new Paper();
        if (mediaSize != null) {
            paper.setSize(mediaSize.getX(Size2DSyntax.INCH) * POINTS_PER_INCH,
                    mediaSize.getY(Size2DSyntax.INCH) * POINTS_PER_INCH);
        }
        paper.setImageableArea(margin * POINTS_PER_INCH,
                    margin * POINTS_PER_INCH,
                    paper.getWidth() - 2 * margin * POINTS_PER_INCH, 
                    paper.getHeight() - 2 * margin * POINTS_PER_INCH);
        return paper;
    }
    
    private static MediaSize lookupMediaSize(final Media media) {

        if (media instanceof MediaSizeName) {
            return MediaSize.getMediaSizeForName((MediaSizeName) media);
        } else if (media instanceof MediaName) {
            if (media.equals(MediaName.ISO_A4_TRANSPARENT)
                    || media.equals(MediaName.ISO_A4_WHITE)) {
                return MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
            } else if (media.equals(MediaName.NA_LETTER_TRANSPARENT)
                    || media.equals(MediaName.NA_LETTER_WHITE)) {
                return MediaSize.getMediaSizeForName(MediaSizeName.NA_LETTER);
            }
        }
        return null;
    }
    
    public MediaPrintableArea getMediaPrintableArea(MediaSize size, int MM) {
//        return new MediaPrintableArea(getX1(MM),
//                getY1(MM),
//                size.getX(MM) - getX1(MM) - getX2(MM),
//                size.getY(MM) - getY1(MM) - getY2(MM),
//                MM);
        return new MediaPrintableArea(MM,
                MM,
                size.getX(MM) - MM - MM,
                size.getY(MM) - MM - MM,
                MM);
    }
    
    private void addElement() {
        String label = possibleElementsList.getSelectedValue().toString();
        if (label.toLowerCase().equals("scale")) {
            map.addMapScale();
            populateElementsList();
        } else if (label.toLowerCase().equals("legend")) {
            map.addLegend();
            populateElementsList();
        } else if (label.toLowerCase().equals("north arrow")) {
            map.addNorthArrow();
            populateElementsList();
        } else if (label.toLowerCase().equals("map area")) {
            map.addMapArea();
            populateElementsList();
        } else if (label.toLowerCase().equals("title")) {
            map.addMapTitle();
            populateElementsList();
        } else if (label.toLowerCase().equals("neatline")) {
            map.addNeatline();
            populateElementsList();
        }
        mapElementsList.setSelectedIndex(0);
        updateElementPropertiesPanel();
    }
    
    private void addElement(String elementType) {
        if (elementType.toLowerCase().equals("scale")) {
            map.addMapScale();
            populateElementsList();
        } else if (elementType.toLowerCase().equals("legend")) {
            map.addLegend();
            populateElementsList();
        } else if (elementType.toLowerCase().equals("north arrow")) {
            map.addNorthArrow();
            populateElementsList();
        } else if (elementType.toLowerCase().equals("map area")) {
            map.addMapArea();
            populateElementsList();
        } else if (elementType.toLowerCase().equals("title")) {
            map.addMapTitle();
            populateElementsList();
        } else if (elementType.toLowerCase().equals("neatline")) {
            map.addNeatline();
            populateElementsList();
        }
        mapElementsList.setSelectedIndex(0);
        host.refreshMap(false);
        updateElementPropertiesPanel();
    }
    
    private void removeElement() {
        int elementNumber = listOfCartographicElements.size() - 1 - mapElementsList.getSelectedIndex();
        map.removeCartographicElement(elementNumber);
        host.refreshMap(true);
        populateElementsList();
        if (elementNumber >= listOfCartographicElements.size() - 1) {
            mapElementsList.setSelectedIndex(0);
        } else {
            mapElementsList.setSelectedIndex(listOfCartographicElements.size() - 1 - elementNumber);
        }
        updateElementPropertiesPanel();
    }
    
    private void elementUp() {
        int elementNumber = listOfCartographicElements.size() - 1 - mapElementsList.getSelectedIndex();
        map.promoteMapElement(elementNumber);
        host.refreshMap(true);
        populateElementsList();
        mapElementsList.setSelectedIndex(listOfCartographicElements.size() - 2 - elementNumber);
        updateElementPropertiesPanel();
    }
    
    private void elementDown() {
        int elementNumber = listOfCartographicElements.size() - 1 - mapElementsList.getSelectedIndex();
        map.demoteMapElement(elementNumber);
        host.refreshMap(true);
        populateElementsList();
        mapElementsList.setSelectedIndex(listOfCartographicElements.size() - elementNumber);
        updateElementPropertiesPanel();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            setVisible(false);
            this.dispose();
        } else if (actionCommand.equals("ok")) {
            updateMap();
            this.dispose();
        } else if (actionCommand.equals("update")) {
            updateMap();
        } else if (actionCommand.equals("addElement")) {
            addElement();
        } else if (actionCommand.equals("removeElement")) {
            removeElement();
        } else if (actionCommand.equals("elementUp")) {
            elementUp();
        } else if (actionCommand.equals("elementDown")) {
            elementDown();
        }
    }
    
    @Override
    public void adjustmentValueChanged(AdjustmentEvent evt) {
        
    }
    
    @Override
    public void mouseClicked(MouseEvent me) {
        
    }

    @Override
    public void mousePressed(MouseEvent me) {
        Object source = me.getSource();
        if (source == sampleFontColourPanel) {
            Color newColour = JColorChooser.showDialog(this, "Choose Color", fontColour);
            if (newColour != null) {
                fontColour = newColour;
                sampleFontColourPanel.setBackColour(newColour);
            }
            
        }
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        if (source == outlineColourBox) {
            if (evt.getPropertyName().equals("colour")) {
                updateMap();
            }
        }
    }
    
    
    private class SampleColour extends JPanel {
        Color backColour;
        
        protected SampleColour(int width, int height, Color clr) {
            this.setMaximumSize(new Dimension(width, height));
            this.setPreferredSize(new Dimension(width, height));
            backColour = clr;
        }
        
        protected void setBackColour(Color clr) {
            backColour = clr;
            repaint();
        }
        
        @Override
        public void paint (Graphics g) {
            g.setColor(backColour);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            
            g.setColor(Color.black);
            g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
            
        }
    }
}
