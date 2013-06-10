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

import whitebox.cartographic.MapInfo;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.*;
import javax.swing.*;
import whitebox.cartographic.*;
import whitebox.cartographic.Neatline;
import whitebox.ui.carto_properties.ColourProperty;
import whitebox.ui.carto_properties.*;
import whitebox.interfaces.CartographicElement;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.structures.BoundingBox;
import java.util.ResourceBundle;

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
    private ColourProperty outlineColourBox;
    private JCheckBox checkNeatlineVisible = new JCheckBox();
    private JCheckBox checkNeatlineDoubleLine = new JCheckBox();
    private JCheckBox checkNeatlineBackgroundVisible = new JCheckBox();
    private JPanel elementPropertiesPanel = new JPanel();
    private JList possibleElementsList = new JList(new DefaultListModel());
    private int activeElement;
    private ResourceBundle bundle;
    private ResourceBundle messages;

    public MapProperties(Frame owner, boolean modal, MapInfo map) {
        super(owner, modal);
        if (owner != null) {
            Dimension parentSize = owner.getSize();
            Point p = owner.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }

        this.host = (WhiteboxPluginHost) (owner);
        bundle = this.host.getGuiLabelsBundle();
        messages = this.host.getMessageBundle();
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

        this.host = (WhiteboxPluginHost) (owner);
        bundle = this.host.getGuiLabelsBundle();
        messages = this.host.getMessageBundle();
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

        setTitle(bundle.getString("MapProperties") + ": " + map.getMapName());

        createPageSizeMap();

        // okay and close buttons.
        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalGlue());
        ok = new JButton(bundle.getString("OK"));
        box1.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        ok.setToolTipText(bundle.getString("SaveChangesAndExit"));
        box1.add(Box.createRigidArea(new Dimension(5, 30)));

        update = new JButton(bundle.getString("UpdateMap"));
        box1.add(update);
        update.setActionCommand("update");
        update.addActionListener(this);
        update.setToolTipText(bundle.getString("UpdateMapTooltip"));
        box1.add(Box.createRigidArea(new Dimension(5, 30)));
        box1.add(close);
        close.setActionCommand("close");
        close.addActionListener(this);
        close.setToolTipText(bundle.getString("CloseTooltip"));
        box1.add(Box.createHorizontalStrut(100));
        box1.add(Box.createHorizontalGlue());

        add(box1, BorderLayout.SOUTH);

        tabs.addTab(bundle.getString("MapElements"), getMapElementsListing());
        tabs.addTab(bundle.getString("Page"), getPageBox());

        getContentPane().add(tabs, BorderLayout.CENTER);

        pack();
    }
    JPanel elementsPanel = new JPanel();

    private JPanel getMapElementsListing() {

        try {
            if (activeElement < 0) {
                activeElement = 0;
            }
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
            //listBox.setBackground(Color.WHITE);
            listBox.add(Box.createHorizontalStrut(10));

            Box vbox = Box.createVerticalBox();
            Box hbox = Box.createHorizontalBox();
            label = new JLabel(bundle.getString("MapElements") + ":");
            label.setForeground(Color.darkGray);
            //Font f = label.getFont();
            //label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            hbox.add(label);
            hbox.add(Box.createHorizontalGlue());
            vbox.add(hbox);

            //JList possibleElementsList = new JList(new DefaultListModel());
            possibleElementsList.addMouseListener(ml1);

            DefaultListModel model = new DefaultListModel();
            model.add(0, bundle.getString("Legend"));
            model.add(1, bundle.getString("MapArea"));
            model.add(2, bundle.getString("Neatline"));
            model.add(3, bundle.getString("NorthArrow"));
            model.add(4, bundle.getString("Scale"));
            model.add(5, bundle.getString("TextArea"));
            model.add(6, bundle.getString("Title"));
            model.add(7, bundle.getString("Image"));

            possibleElementsList.setModel(model);

            JScrollPane scroller1 = new JScrollPane(possibleElementsList);
            vbox.add(scroller1);


            Box hbox4 = Box.createHorizontalBox();
            JButton addButton = new JButton(bundle.getString("Add"));
            addButton.setActionCommand("addElement");
            addButton.addActionListener(this);
            hbox4.add(Box.createHorizontalGlue());
            hbox4.add(addButton);
            vbox.add(hbox4);

            listBox.add(vbox);

            listBox.add(Box.createHorizontalStrut(10));

            vbox = Box.createVerticalBox();
            label = new JLabel(bundle.getString("CurrentMapElements") + ":");
            label.setForeground(Color.darkGray);
            Box hbox1 = Box.createHorizontalBox();
            hbox1.add(label);
            hbox1.add(Box.createHorizontalGlue());
            vbox.add(hbox1);

            mapElementsList = new JList(new DefaultListModel());
            mapElementsList.addMouseListener(ml2);
            populateElementsList();

            JScrollPane scroller2 = new JScrollPane(mapElementsList);
            vbox.add(scroller2);

            Box hbox2 = Box.createHorizontalBox();
            JButton deleteButton = new JButton(bundle.getString("Remove"));
            deleteButton.setActionCommand("removeElement");
            deleteButton.addActionListener(this);
            hbox2.add(Box.createHorizontalGlue());
            hbox2.add(deleteButton);
            vbox.add(hbox2);

            listBox.add(vbox);

            Box vbox2 = Box.createVerticalBox();
            JButton elementUpButton = new JButton(String.valueOf('\u25B2'));
            elementUpButton.setActionCommand("elementUp");
            elementUpButton.addActionListener(this);
            elementUpButton.setPreferredSize(new Dimension(10,
                    elementUpButton.getPreferredSize().height));
            vbox2.add(elementUpButton);
            JButton elementDownButton = new JButton(String.valueOf('\u25BC'));
            elementDownButton.setActionCommand("elementDown");
            elementDownButton.addActionListener(this);
            vbox2.add(elementDownButton);
            listBox.add(vbox2);

            listBox.setMaximumSize(new Dimension(2000, 150));

            mainBox.add(listBox);

            Box vbox3 = Box.createVerticalBox();
            vbox3.add(Box.createVerticalStrut(10));

            Box hbox3 = Box.createHorizontalBox();
            hbox3.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("ElementsProperties") + ":");
            //f = label.getFont();
            //label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            label.setForeground(Color.darkGray);
            hbox3.add(label);
            hbox3.add(Box.createHorizontalGlue());
            vbox3.add(hbox3);

            //elementPropertiesPanel.setBackground(Color.WHITE);
            JScrollPane scroll = new JScrollPane(elementPropertiesPanel);
            scroll.setPreferredSize(new Dimension(150, 250));
            vbox3.add(scroll); //elementPropertiesPanel);

            mainBox.add(vbox3);
            mainBox.add(Box.createVerticalGlue());

            elementsPanel.setLayout(new BorderLayout());
            elementsPanel.add(mainBox, BorderLayout.NORTH);
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
        if (index < 0) {
            index = 0;
        }
        if (index > listOfCartographicElements.size()) {
            index = listOfCartographicElements.size();
        }
        CartographicElement ce = listOfCartographicElements.get(index);
        elementPropertiesPanel.removeAll();
        if (ce instanceof MapTitle) {
            elementPropertiesPanel.add(getTitleBox((MapTitle) ce), BorderLayout.CENTER);
        } else if (ce instanceof MapScale) {
            elementPropertiesPanel.add(getScaleBox((MapScale) ce), BorderLayout.CENTER);
        } else if (ce instanceof NorthArrow) {
            elementPropertiesPanel.add(getNorthArrowBox((NorthArrow) ce), BorderLayout.CENTER);
        } else if (ce instanceof Neatline) {
            elementPropertiesPanel.add(getNeatlineBox((Neatline) ce), BorderLayout.CENTER);
        } else if (ce instanceof MapArea) {
            elementPropertiesPanel.add(getMapAreaBox((MapArea) ce), BorderLayout.CENTER);
        } else if (ce instanceof MapTextArea) {
            elementPropertiesPanel.add(getMapTextArea((MapTextArea) ce), BorderLayout.CENTER);
        } else if (ce instanceof Legend) {
            elementPropertiesPanel.add(getLegendBox((Legend) ce), BorderLayout.CENTER);
        } else if (ce instanceof MapImage) {
            elementPropertiesPanel.add(getImageBox((MapImage) ce), BorderLayout.CENTER);
        }
        elementPropertiesPanel.validate();
        elementPropertiesPanel.repaint();
//        elementsPanel.validate();
//        elementsPanel.repaint();
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
        whitebox.ui.carto_properties.MapTitlePropertyGrid obj = new whitebox.ui.carto_properties.MapTitlePropertyGrid(mapTitle, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
    }

    private JPanel getMapTextArea(MapTextArea mapTextArea) {
        whitebox.ui.carto_properties.MapTextAreaPropertyGrid obj = new whitebox.ui.carto_properties.MapTextAreaPropertyGrid(mapTextArea, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
    }

    private JPanel getImageBox(MapImage mapImage) {
        whitebox.ui.carto_properties.MapImagePropertyGrid obj = new whitebox.ui.carto_properties.MapImagePropertyGrid(mapImage, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
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

            // page visibility
            JPanel pageVisibleBox = new JPanel();
            pageVisibleBox.setLayout(new BoxLayout(pageVisibleBox, BoxLayout.X_AXIS));
            pageVisibleBox.setBackground(Color.WHITE);
            pageVisibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("DrawThePage"));
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
            label = new JLabel(bundle.getString("PageOrientation") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            orientationBox.add(label);
            orientationBox.add(Box.createHorizontalGlue());
            landscape = new JRadioButton(bundle.getString("Landscape"), true);
            orientationBox.add(landscape);
            portrait = new JRadioButton(bundle.getString("Portrait"), true);
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
            label = new JLabel(bundle.getString("PaperType") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            paperNameBox.add(label);
            paperNameBox.add(Box.createHorizontalGlue());
            // What is the name of the current paper?
            for (Map.Entry<String, Float[]> e : pageSizes.entrySet()) {
                if (e.getValue()[0] == (paper.getWidth() / POINTS_PER_INCH)
                        && e.getValue()[1] == (paper.getHeight() / POINTS_PER_INCH)) {
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
            label = new JLabel(bundle.getString("MarginSize") + ":");
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

            mainBox.add(Box.createVerticalStrut(330));
        } catch (Exception e) {
            host.showFeedback(e.getMessage());
        } finally {
            return panel;
        }

    }

    private JPanel getMapAreaBox(MapArea mapArea) {
        whitebox.ui.carto_properties.MapAreaPropertyGrid obj = new whitebox.ui.carto_properties.MapAreaPropertyGrid(mapArea, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
    }

    private JPanel getNeatlineBox(Neatline neatline) {
        whitebox.ui.carto_properties.NeatlinePropertyGrid obj = new whitebox.ui.carto_properties.NeatlinePropertyGrid(neatline, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
    }

    private JPanel getNorthArrowBox(NorthArrow northArrow) {
        NorthArrowPropertyGrid obj = new NorthArrowPropertyGrid(northArrow, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;

    }

    private JPanel getLegendBox(Legend legend) {
        LegendPropertyGrid obj = new LegendPropertyGrid(legend, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
    }

    private JPanel getScaleBox(MapScale mapScale) {
        whitebox.ui.carto_properties.ScalePropertyGrid obj = new whitebox.ui.carto_properties.ScalePropertyGrid(mapScale, host);
        obj.setPreferredSize(new Dimension(this.getPreferredSize().width - 8, 300));
        return obj;
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
        if (tabs.getSelectedIndex() == 1) {
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
            if (paper.getHeight() != pf.getPaper().getHeight()
                    || paper.getWidth() != pf.getPaper().getWidth()
                    || changedOrientation) {
                pf.setPaper(paper);
                // resize the page extent
                BoundingBox pageExtent = map.getPageExtent();
                pageExtent.setMinX(-6);
                pageExtent.setMinY(-6);
                pageExtent.setMaxX(pf.getWidth() + 12);
                pageExtent.setMaxY(pf.getHeight() + 12);

                map.setPageExtent(pageExtent);

            }
            if (map.getMargin() != margin) {
                map.setMargin(margin);
                WhiteboxGui wb = (WhiteboxGui) host;
                wb.setDefaultMapMargin(margin);
            }
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

//    private static MediaSize lookupMediaSize(final Media media) {
//
//        if (media instanceof MediaSizeName) {
//            return MediaSize.getMediaSizeForName((MediaSizeName) media);
//        } else if (media instanceof MediaName) {
//            if (media.equals(MediaName.ISO_A4_TRANSPARENT)
//                    || media.equals(MediaName.ISO_A4_WHITE)) {
//                return MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
//            } else if (media.equals(MediaName.NA_LETTER_TRANSPARENT)
//                    || media.equals(MediaName.NA_LETTER_WHITE)) {
//                return MediaSize.getMediaSizeForName(MediaSizeName.NA_LETTER);
//            }
//        }
//        return null;
//    }
    public MediaPrintableArea getMediaPrintableArea(MediaSize size, int MM) {
        return new MediaPrintableArea(MM,
                MM,
                size.getX(MM) - MM - MM,
                size.getY(MM) - MM - MM,
                MM);
    }

    private void addElement() {
        String label = possibleElementsList.getSelectedValue().toString();
        if (label.equals(bundle.getString("Scale"))) {
            map.addMapScale();
        } else if (label.equals(bundle.getString("Legend"))) {
            map.addLegend();
        } else if (label.equals(bundle.getString("NorthArrow"))) {
            map.addNorthArrow();
        } else if (label.equals(bundle.getString("MapArea"))) {
            map.addMapArea();
            populateElementsList();
        } else if (label.equals(bundle.getString("Title"))) {
            map.addMapTitle();
        } else if (label.equals(bundle.getString("Neatline"))) {
            map.addNeatline();
        } else if (label.equals(bundle.getString("TextArea"))) {
            map.addMapTextArea();
        } else if (label.equals(bundle.getString("Image"))) {
            whitebox.ui.ImageFileChooser ifc = new whitebox.ui.ImageFileChooser();
            ifc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            ifc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            ifc.setMultiSelectionEnabled(false);
            ifc.setAcceptAllFileFilterUsed(false);
            ifc.setCurrentDirectory(new File(host.getWorkingDirectory()));

            int result = ifc.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = ifc.getSelectedFile();
                String selectedFile = file.toString();
                String fileName = "";
                map.addMapImage(selectedFile);
            }

        }
        populateElementsList();

        mapElementsList.setSelectedIndex(0);
        updateElementPropertiesPanel();
    }

    private void addElement(String elementType) {

        if (elementType.equals(bundle.getString("Scale"))) {
            map.addMapScale();
        } else if (elementType.equals(bundle.getString("Legend"))) {
            map.addLegend();
        } else if (elementType.equals(bundle.getString("NorthArrow"))) {
            map.addNorthArrow();
        } else if (elementType.equals(bundle.getString("MapArea"))) {
            map.addMapArea();
        } else if (elementType.equals(bundle.getString("Title"))) {
            map.addMapTitle();
        } else if (elementType.equals(bundle.getString("Neatline"))) {
            map.addNeatline();
        } else if (elementType.equals(bundle.getString("TextArea"))) {
            map.addMapTextArea();
        } else if (elementType.equals(bundle.getString("Image"))) {
            whitebox.ui.ImageFileChooser ifc = new whitebox.ui.ImageFileChooser();
            ifc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            ifc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            ifc.setMultiSelectionEnabled(false);
            ifc.setAcceptAllFileFilterUsed(false);
            ifc.setCurrentDirectory(new File(host.getWorkingDirectory()));

            int result = ifc.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = ifc.getSelectedFile();
                String selectedFile = file.toString();
                String fileName = "";
                map.addMapImage(selectedFile);
            }
        }

        populateElementsList();

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
        switch (actionCommand) {
            case "close":
                setVisible(false);
                this.dispose();
                break;
            case "ok":
                updateMap();
                this.dispose();
                break;
            case "update":
                updateMap();
                break;
            case "addElement":
                addElement();
                break;
            case "removeElement":
                removeElement();
                break;
            case "elementUp":
                elementUp();
                break;
            case "elementDown":
                elementDown();
                break;
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
        public void paint(Graphics g) {
            g.setColor(backColour);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());

            g.setColor(Color.black);
            g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);

        }
    }
}
