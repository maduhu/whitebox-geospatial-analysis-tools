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
import java.text.DecimalFormat;
import java.util.*;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.*;
import javax.swing.*;
import whitebox.cartographic.*;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MapProperties extends JDialog implements ActionListener, AdjustmentListener, MouseListener {
    
    private MapInfo map = null;
    private JButton ok = new JButton("OK");
    private JButton update = new JButton("Update");
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
    
    private JCheckBox checkNAVisible = new JCheckBox();
    private JCheckBox checkNABackgroundVisible = new JCheckBox();
    private JCheckBox checkNABorderVisible = new JCheckBox();
    private JTextField naMarkerSizeText = null;
    private JTextField naMarginText = null;
    
    private JCheckBox checkTitleVisible = new JCheckBox();
    private JCheckBox checkTitleBackgroundVisible = new JCheckBox();
    private JCheckBox checkTitleBorderVisible = new JCheckBox();
    private JTextField titleMarginText = null;
    private JTextField titleLabelText = null;
    private JSpinner titleFontSize = new JSpinner();
    private JCheckBox titleFontBold = new JCheckBox();
    private JCheckBox titleFontItalics = new JCheckBox();
    
    private String activeTab = "";
    
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
    }
    
    public MapProperties(Frame owner, boolean modal, MapInfo map, String activeTab) {
        super(owner, modal);
        if (owner != null) {
            Dimension parentSize = owner.getSize(); 
            Point p = owner.getLocation(); 
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        
        this.host = (WhiteboxPluginHost)(owner);
        this.map = map;
        this.activeTab = activeTab.toLowerCase();
        
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
        
        setTitle("Map Properties: " + map.getMapTitle());
        
        createPageSizeMap();
        
        // okay and close buttons.
        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalStrut(10));
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
       
        tabs.addTab("Scale", getScaleBox());
        tabs.addTab("Legend", getLegendBox());
        tabs.addTab("North Arrow", getNorthArrowBox());
        tabs.addTab("Title", getTitleBox());
        tabs.addTab("Neatline", getNeatlineBox());
        tabs.addTab("Page", getPageBox());
        tabs.addTab("Map Elements", getMapElementsListing());
        
        if (activeTab.length() > 0) {
            if (activeTab.equals("scale")) {
                tabs.setSelectedIndex(0);
            } else if (activeTab.equals("legend")) {
                tabs.setSelectedIndex(1);
            } else if (activeTab.contains("north")) {
                tabs.setSelectedIndex(2);
            } else if (activeTab.equals("title")) {
                tabs.setSelectedIndex(3);
            } else if (activeTab.contains("neat")) {
                tabs.setSelectedIndex(4);
            } else if (activeTab.equals("page")) {
                tabs.setSelectedIndex(5);
            } else if (activeTab.equals("map elements")) {
                tabs.setSelectedIndex(6);
            }
        }
        
        getContentPane().add(tabs, BorderLayout.CENTER);
        
        pack();
    }
    
    private JPanel getMapElementsListing() {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            
            MouseListener ml = new MouseAdapter() {

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
                        //showToolDescription(label);
                    } else if (e.getClickCount() == 2) {
                        CartographicElement ce = listOfCartographicElements.get(index);
                        if (ce instanceof MapTitle) {
                            tabs.setSelectedIndex(3);
                        }
                    }

                }
            };


            JPanel mapElementsBox = new JPanel();
            mapElementsBox.setLayout(new BoxLayout(mapElementsBox, BoxLayout.X_AXIS));
            mapElementsBox.setBackground(Color.WHITE);
            mapElementsBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("The map contains these cartographic elements:");
            //Font f = label.getFont();
            //label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            mapElementsBox.add(label);
            mapElementsBox.add(Box.createHorizontalGlue());
            mapElementsBox.add(Box.createHorizontalStrut(10));
            mainBox.add(mapElementsBox);

            populateElementsList();
            
            mapElementsList.addMouseListener(ml);
            
            JScrollPane scroller1 = new JScrollPane(mapElementsList);
            
            mainBox.add(scroller1);
            
            mainBox.add(Box.createVerticalStrut(330));
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
    }
    
    private void populateElementsList() {
        mapElementsList = new JList();
            
            int maxIndex;
            listOfCartographicElements = map.getCartographicElementList();
            if (listOfCartographicElements.size() <= 10) {
                maxIndex = listOfCartographicElements.size();
            } else {
                maxIndex = 10;
            }
            mapElementsList.removeAll();
            DefaultListModel model = new DefaultListModel();
            int i = 0;
            for (CartographicElement ce : listOfCartographicElements) {
                model.add(i, ce.getName());
                i++;
            }
            mapElementsList.setModel(model);
            
    }
    
    private JPanel getTitleBox() {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            Font labelFont = map.mapTitle.getLabelFont();
            
            // Title label text
            JPanel titleLabelBox = new JPanel();
            titleLabelBox.setLayout(new BoxLayout(titleLabelBox, BoxLayout.X_AXIS));
            titleLabelBox.setBackground(Color.WHITE);
            titleLabelBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Label Text:");
            label.setPreferredSize(new Dimension(180, 24));
            titleLabelBox.add(label);
            titleLabelBox.add(Box.createHorizontalGlue());
            titleLabelText = new JTextField(String.valueOf(map.mapTitle.getLabel()), 15);
            titleLabelText.setHorizontalAlignment(JTextField.RIGHT);
            titleLabelText.setMaximumSize(new Dimension(50, 22));
            titleLabelBox.add(titleLabelText);
            titleLabelBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleLabelBox);
            
            // title visibility
            JPanel titleVisibleBox = new JPanel();
            titleVisibleBox.setLayout(new BoxLayout(titleVisibleBox, BoxLayout.X_AXIS));
            titleVisibleBox.setBackground(backColour);
            titleVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the title visible?");
            label.setPreferredSize(new Dimension(200, 24));
            titleVisibleBox.add(label);
            titleVisibleBox.add(Box.createHorizontalGlue());
            checkTitleVisible.setSelected(map.mapTitle.isVisible());
            checkTitleVisible.addActionListener(this);
            checkTitleVisible.setActionCommand("checkTitleVisible");
            titleVisibleBox.add(checkTitleVisible);
            titleVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleVisibleBox);
            
            // Title background visibility
            JPanel titleBackVisibleBox = new JPanel();
            titleBackVisibleBox.setLayout(new BoxLayout(titleBackVisibleBox, BoxLayout.X_AXIS));
            titleBackVisibleBox.setBackground(Color.WHITE);
            titleBackVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the title background visible?");
            label.setPreferredSize(new Dimension(220, 24));
            titleBackVisibleBox.add(label);
            titleBackVisibleBox.add(Box.createHorizontalGlue());
            checkTitleBackgroundVisible.setSelected(map.mapTitle.isBackgroundVisible());
            checkTitleBackgroundVisible.addActionListener(this);
            checkTitleBackgroundVisible.setActionCommand("checkTitleBackgroundVisible");
            titleBackVisibleBox.add(checkTitleBackgroundVisible);
            titleBackVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleBackVisibleBox);
            
            // Title border visibility
            JPanel titleBorderVisibleBox = new JPanel();
            titleBorderVisibleBox.setLayout(new BoxLayout(titleBorderVisibleBox, BoxLayout.X_AXIS));
            titleBorderVisibleBox.setBackground(backColour);
            titleBorderVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the title box border visible?");
            label.setPreferredSize(new Dimension(220, 24));
            titleBorderVisibleBox.add(label);
            titleBorderVisibleBox.add(Box.createHorizontalGlue());
            checkTitleBorderVisible.setSelected(map.mapTitle.isBorderVisible());
            checkTitleBorderVisible.addActionListener(this);
            checkTitleBorderVisible.setActionCommand("checkTitleBorderVisible");
            titleBorderVisibleBox.add(checkTitleBorderVisible);
            titleBorderVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleBorderVisibleBox);
            
            // Title margin size
            JPanel titleMarginBox = new JPanel();
            titleMarginBox.setLayout(new BoxLayout(titleMarginBox, BoxLayout.X_AXIS));
            titleMarginBox.setBackground(Color.WHITE);
            titleMarginBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Margin Size (Points):");
            label.setPreferredSize(new Dimension(180, 24));
            titleMarginBox.add(label);
            titleMarginBox.add(Box.createHorizontalGlue());
            titleMarginText = new JTextField(String.valueOf(map.mapTitle.getMargin()), 15);
            titleMarginText.setHorizontalAlignment(JTextField.RIGHT);
            titleMarginText.setMaximumSize(new Dimension(50, 22));
            titleMarginBox.add(titleMarginText);
            titleMarginBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleMarginBox);
            
            // Title label font size
            JPanel titleFontSizeBox = new JPanel();
            titleFontSizeBox.setLayout(new BoxLayout(titleFontSizeBox, BoxLayout.X_AXIS));
            titleFontSizeBox.setBackground(backColour);
            titleFontSizeBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Font Size:");
            label.setPreferredSize(new Dimension(180, 24));
            titleFontSizeBox.add(label);
            titleFontSizeBox.add(Box.createHorizontalGlue());
            titleFontSize.setMaximumSize(new Dimension(200, 22));
            SpinnerModel sm =
                    new SpinnerNumberModel(labelFont.getSize(), 1, 150, 1);
            titleFontSize.setModel(sm);
            titleFontSizeBox.add(titleFontSize);
            titleFontSizeBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleFontSizeBox);
            
            // title label font bold
            int fontBold = labelFont.getStyle() & Font.BOLD;
            JPanel titleFontBoldBox = new JPanel();
            titleFontBoldBox.setLayout(new BoxLayout(titleFontBoldBox, BoxLayout.X_AXIS));
            titleFontBoldBox.setBackground(Color.WHITE);
            titleFontBoldBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Use bold font?");
            label.setPreferredSize(new Dimension(200, 24));
            titleFontBoldBox.add(label);
            titleFontBoldBox.add(Box.createHorizontalGlue());
            titleFontBold.setSelected(fontBold > 0);
            titleFontBold.addActionListener(this);
            //titleFontBold.setActionCommand("checkTitleVisible");
            titleFontBoldBox.add(titleFontBold);
            titleFontBoldBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleFontBoldBox);
            
            // title label font bold
            int fontItalicized = labelFont.getStyle() & Font.ITALIC;
            JPanel titleFontItalicsBox = new JPanel();
            titleFontItalicsBox.setLayout(new BoxLayout(titleFontItalicsBox, BoxLayout.X_AXIS));
            titleFontItalicsBox.setBackground(backColour);
            titleFontItalicsBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Use italicized font?");
            label.setPreferredSize(new Dimension(200, 24));
            titleFontItalicsBox.add(label);
            titleFontItalicsBox.add(Box.createHorizontalGlue());
            titleFontItalics.setSelected(fontItalicized > 0);
            titleFontItalics.addActionListener(this);
            //titleFontItalics.setActionCommand("checkTitleVisible");
            titleFontItalicsBox.add(titleFontItalics);
            titleFontItalicsBox.add(Box.createHorizontalStrut(10));
            mainBox.add(titleFontItalicsBox);
            
            mainBox.add(Box.createVerticalStrut(330));
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
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
            checkPageVisible.setSelected(map.isCartoView());
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
                 "B0", "B1", "B2", "Custom"};
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
                //System.out.println(e.getKey() + ": " + e.getValue()[0] + " " + e.getValue()[1]);
            }
            //paperNameCombo.setSelectedIndex(map.isCartoView());
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
    
    private JPanel getNeatlineBox() {
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
            
            mainBox.add(Box.createVerticalStrut(330));
        
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private JPanel getNorthArrowBox() {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
            
            // NA visibility
            JPanel naVisibleBox = new JPanel();
            naVisibleBox.setLayout(new BoxLayout(naVisibleBox, BoxLayout.X_AXIS));
            naVisibleBox.setBackground(Color.WHITE);
            naVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the north arrow visible?");
            label.setPreferredSize(new Dimension(200, 24));
            naVisibleBox.add(label);
            naVisibleBox.add(Box.createHorizontalGlue());
            checkNAVisible.setSelected(map.northArrow.isVisible());
            checkNAVisible.addActionListener(this);
            checkNAVisible.setActionCommand("checkNAVisible");
            naVisibleBox.add(checkNAVisible);
            naVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(naVisibleBox);
            
            // NA background visibility
            JPanel naBackVisibleBox = new JPanel();
            naBackVisibleBox.setLayout(new BoxLayout(naBackVisibleBox, BoxLayout.X_AXIS));
            naBackVisibleBox.setBackground(backColour);
            naBackVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the arrow background visible?");
            label.setPreferredSize(new Dimension(220, 24));
            naBackVisibleBox.add(label);
            naBackVisibleBox.add(Box.createHorizontalGlue());
            checkNABackgroundVisible.setSelected(map.northArrow.isBackgroundVisible());
            checkNABackgroundVisible.addActionListener(this);
            checkNABackgroundVisible.setActionCommand("checkNABackgroundVisible");
            naBackVisibleBox.add(checkNABackgroundVisible);
            naBackVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(naBackVisibleBox);
            
            // NA border visibility
            JPanel naBorderVisibleBox = new JPanel();
            naBorderVisibleBox.setLayout(new BoxLayout(naBorderVisibleBox, BoxLayout.X_AXIS));
            naBorderVisibleBox.setBackground(Color.WHITE);
            naBorderVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Is the north arrow border visible?");
            label.setPreferredSize(new Dimension(220, 24));
            naBorderVisibleBox.add(label);
            naBorderVisibleBox.add(Box.createHorizontalGlue());
            checkNABorderVisible.setSelected(map.northArrow.isBorderVisible());
            checkNABorderVisible.addActionListener(this);
            checkNABorderVisible.setActionCommand("checkNABorderVisible");
            naBorderVisibleBox.add(checkNABorderVisible);
            naBorderVisibleBox.add(Box.createHorizontalStrut(10));
            mainBox.add(naBorderVisibleBox);
            
            
            // NA marker size
            JPanel naMarkerSizeBox = new JPanel();
            naMarkerSizeBox.setLayout(new BoxLayout(naMarkerSizeBox, BoxLayout.X_AXIS));
            naMarkerSizeBox.setBackground(backColour);
            naMarkerSizeBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Marker Size (Points):");
            label.setPreferredSize(new Dimension(180, 24));
            naMarkerSizeBox.add(label);
            naMarkerSizeBox.add(Box.createHorizontalGlue());
            naMarkerSizeText = new JTextField(String.valueOf(map.northArrow.getMarkerSize()), 15);
            naMarkerSizeText.setHorizontalAlignment(JTextField.RIGHT);
            naMarkerSizeText.setMaximumSize(new Dimension(50, 22));
            naMarkerSizeBox.add(naMarkerSizeText);
            naMarkerSizeBox.add(Box.createHorizontalStrut(10));
            mainBox.add(naMarkerSizeBox);
            
            // NA margin size
            JPanel naMarginBox = new JPanel();
            naMarginBox.setLayout(new BoxLayout(naMarginBox, BoxLayout.X_AXIS));
            naMarginBox.setBackground(Color.WHITE);
            naMarginBox.add(Box.createHorizontalStrut(10));
            label = new JLabel("Margin Size (Points):");
            label.setPreferredSize(new Dimension(180, 24));
            naMarginBox.add(label);
            naMarginBox.add(Box.createHorizontalGlue());
            naMarginText = new JTextField(String.valueOf(map.northArrow.getMargin()), 15);
            naMarginText.setHorizontalAlignment(JTextField.RIGHT);
            naMarginText.setMaximumSize(new Dimension(50, 22));
            naMarginBox.add(naMarginText);
            naMarginBox.add(Box.createHorizontalStrut(10));
            mainBox.add(naMarginBox);
            
            mainBox.add(Box.createVerticalStrut(330));
        
        
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
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
            
            mainBox.add(Box.createVerticalStrut(330));
            
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }
        
    }
    
    private JPanel getScaleBox() {
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
            checkScaleVisible.setSelected(map.mapScale.isVisible());
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
            checkScaleBackgroundVisible.setSelected(map.mapScale.isBackgroundVisible());
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
            checkScaleBorderVisible.setSelected(map.mapScale.isBorderVisible());
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
            scaleUnitText = new JTextField(map.mapScale.getUnits(), 15);
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
            scaleWidthText = new JTextField(String.valueOf(map.mapScale.getWidth()), 15);
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
            scaleHeightText = new JTextField(String.valueOf(map.mapScale.getHeight()), 15);
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
            scaleMarginText = new JTextField(String.valueOf(map.mapScale.getMargin()), 15);
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
            checkScaleShowRF.setSelected(map.mapScale.isRepresentativeFractionVisible());
            checkScaleShowRF.addActionListener(this);
            checkScaleShowRF.setActionCommand("checkScaleRFVisible");
            scaleRFVisible.add(checkScaleShowRF);
            scaleRFVisible.add(Box.createHorizontalStrut(10));
            mainBox.add(scaleRFVisible);
            
            
            mainBox.add(Box.createVerticalStrut(330));
            
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
        map.setCartoView(checkPageVisible.isSelected());
        
        PageFormat pf = map.getPageFormat();
        
        //final Media media = (Media) attributeSet.get(Media.class);
        margin = Double.parseDouble(marginText.getText());
        String paperSize = paperNameCombo.getSelectedItem().toString();
        MediaSize mediaSize = new MediaSize(pageSizes.get(paperSize)[0], pageSizes.get(paperSize)[1], Size2DSyntax.INCH);
        pf.setPaper(createPaper(mediaSize));
        if (landscape.isSelected() && pf.getOrientation() != PageFormat.LANDSCAPE) {
            pf.setOrientation(PageFormat.LANDSCAPE);
        } else if (portrait.isSelected() && pf.getOrientation() != PageFormat.PORTRAIT) {
            pf.setOrientation(PageFormat.PORTRAIT);
        }
        map.setMargin(margin);
        
        // map scale
        map.mapScale.setVisible(checkScaleVisible.isSelected());
        map.mapScale.setBorderVisible(checkScaleBorderVisible.isSelected());
        map.mapScale.setBackgroundVisible(checkScaleBackgroundVisible.isSelected());
        map.mapScale.setRepresentativeFractionVisible(checkScaleShowRF.isSelected());
        if (!map.mapScale.getUnits().equals(scaleUnitText.getText())) {
            map.mapScale.setUnits(scaleUnitText.getText());
        }
        if (map.mapScale.getHeight() != Integer.parseInt(scaleHeightText.getText())) {
            map.mapScale.setHeight(Integer.parseInt(scaleHeightText.getText()));
        }
        if (map.mapScale.getWidth() != Integer.parseInt(scaleWidthText.getText())) {
            map.mapScale.setWidth(Integer.parseInt(scaleWidthText.getText()));
        }
        if (map.mapScale.getMargin() != Integer.parseInt(scaleMarginText.getText())) {
            map.mapScale.setMargin(Integer.parseInt(scaleMarginText.getText()));
        }
        map.mapScale.setScale(map.mapScale.getScale()); // this is just to refresh the map scale.
        
        // north arrow
        map.northArrow.setVisible(checkNAVisible.isSelected());
        map.northArrow.setBorderVisible(checkNABorderVisible.isSelected());
        map.northArrow.setBackgroundVisible(checkNABackgroundVisible.isSelected());
        if (map.northArrow.getMarkerSize() != Integer.parseInt(naMarkerSizeText.getText())) {
            map.northArrow.setMarkerSize(Integer.parseInt(naMarkerSizeText.getText()));
        }
        if (map.northArrow.getMargin() != Integer.parseInt(naMarginText.getText())) {
            map.northArrow.setMargin(Integer.parseInt(naMarginText.getText()));
        }
        
        // map title
        if (map.mapTitle.isVisible() != checkTitleVisible.isSelected()) {
            map.mapTitle.setVisible(checkTitleVisible.isSelected());
        }
        map.mapTitle.setBorderVisible(checkTitleBorderVisible.isSelected());
        map.mapTitle.setBackgroundVisible(checkTitleBackgroundVisible.isSelected());
        if (map.mapTitle.getMargin() != Integer.parseInt(titleMarginText.getText())) {
            map.mapTitle.setMargin(Integer.parseInt(titleMarginText.getText()));
        }
        if (!map.mapTitle.getLabel().toLowerCase().equals(titleLabelText.getText().toLowerCase())) {
            map.mapTitle.setLabel(titleLabelText.getText());
        }
        Font labelFont = map.mapTitle.getLabelFont();
        int fontSize = (Integer)(titleFontSize.getValue());
        int style = 0;
        if (titleFontBold.isSelected()) {
            style += Font.BOLD;
        }
        if (titleFontItalics.isSelected()) {
            style += Font.ITALIC;
        }
        Font newFont = new Font(labelFont.getName(), style, fontSize);
        if (!labelFont.equals(newFont)) {
            map.mapTitle.setLabelFont(newFont);
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

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            setVisible(false);
            this.dispose();
        } else if (actionCommand.equals("ok")) {
            updateMap();
            this.dispose();
        } else if (actionCommand.equals("update")) {
            updateMap();
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
