package whiteboxgis;

import javax.swing.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
//import java.awt.print.Book;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.util.Map;
import java.util.HashMap;
//import java.io.*;
import java.text.DecimalFormat;
//import whitebox.geospatialfiles.WhiteboxRaster;
//import whitebox.geospatialfiles.WhiteboxRasterInfo;
//import whitebox.geospatialfiles.shapefile.ShapeType;
//import whitebox.geospatialfiles.ShapeFile;
import whitebox.interfaces.WhiteboxPluginHost;
//import whitebox.interfaces.Communicator;
//import whitebox.interfaces.MapLayer;
//import java.util.Arrays;
import java.awt.*;
//import java.util.ArrayList;
//import whitebox.cartographic.PointMarkers;
//import whitebox.cartographic.PointMarkers.MarkerStyle;

/**
 *
 * @author johnlindsay
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
       
        tabs.addTab("Page", getPageBox());
        tabs.addTab("Title", getTitleBox());
        tabs.addTab("Legend", getLegendBox());
        tabs.addTab("Neatline", getNeatlineBox());
        tabs.addTab("North Arrow", getNorthArrowBox());
        tabs.addTab("Scale", getScaleBox());
        
        getContentPane().add(tabs, BorderLayout.CENTER);
        
        pack();
    }
    
    private JPanel getTitleBox() {
        JPanel panel = new JPanel();
        try {
            JLabel label = null;
            Box mainBox = Box.createVerticalBox();
            JScrollPane scroll = new JScrollPane(mainBox);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(scroll);
        
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
