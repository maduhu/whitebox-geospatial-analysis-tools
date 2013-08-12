/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;
import whitebox.structures.ExtensionFileFilter;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PaletteManager extends JFrame implements ActionListener, MouseMotionListener, MouseListener {

    //private PaletteImage paletteImage = new PaletteImage();
    private String paletteDirectory = null;
    private int paletteImageWidth = 750;
    private int paletteImageHeight = 50;
    private String paletteFile = null;
    private String paletteName = "MyPalette";
    private int[] paletteData = new int[2048];
    private int numPaletteEntries = 2048;
    private JPanel paletteImage = new JPanel();
    private JPanel sampleColorPanel = new JPanel();
    private int sampleWidth = 24;
    private int sampleHeight = 24;
    private Color sampleColor = new Color(0, 0, 255);
    private JTextField lineNumText = new JTextField("0");
    private JLabel rgbLabel = new JLabel("Entry 0 : Red 0 : Green 0 : Blue 0");
    private JTextField numLinesText = new JTextField("2048");
    private JTextField fromText = new JTextField("0");
    private JTextField toText = new JTextField("2047");
    private JLabel paletteNameLabel = new JLabel();
    private ResourceBundle bundle;

    public PaletteManager(String paletteDirectory, ResourceBundle bundle) {
        this.paletteDirectory = paletteDirectory;
        this.bundle = bundle;
        initUI();
    }

    private void initUI() {
        this.setTitle(bundle.getString("PaletteManager"));
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        Toolkit kit = this.getToolkit();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Insets in = kit.getScreenInsets(gs[0].getDefaultConfiguration());

        Dimension d = kit.getScreenSize();
        int max_width = (d.width - in.left - in.right);
        int max_height = (d.height - in.top - in.bottom);
        this.setSize(Math.min(max_width, 800), Math.min(max_height, 300));
        this.setLocation((int) (max_width - this.getWidth()) / 2, (int) (max_height - this.getHeight()) / 2);

        Container pane = this.getContentPane();

        Box mainBox = Box.createVerticalBox();

        mainBox.add(Box.createVerticalStrut(5));

        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalGlue());
        //box1.add(Box.createHorizontalStrut(10));
        JButton ok = new JButton(bundle.getString("OK"));
        JButton close = new JButton(bundle.getString("Close"));
        //box1.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        //ok.setToolTipText("Save changes and exit");
        //box1.add(Box.createRigidArea(new Dimension(5, 30)));
        box1.add(close);
        close.setActionCommand("close");
        close.addActionListener(this);
        box1.add(Box.createHorizontalStrut(20));
        pane.add(box1, BorderLayout.SOUTH);

        JPanel nameLabelBox = new JPanel();
        nameLabelBox.setLayout(new BoxLayout(nameLabelBox, BoxLayout.X_AXIS));
        nameLabelBox.setMaximumSize(new Dimension(paletteImageWidth, 24));
        nameLabelBox.setPreferredSize(new Dimension(paletteImageWidth, 24));
        paletteNameLabel = new JLabel(bundle.getString("PaletteName") + ": " + paletteName);
        nameLabelBox.add(paletteNameLabel);
        mainBox.add(nameLabelBox);
        mainBox.add(Box.createVerticalStrut(5));

        JPanel labelBox = new JPanel();
        labelBox.setLayout(new BoxLayout(labelBox, BoxLayout.X_AXIS));
        labelBox.setMaximumSize(new Dimension(paletteImageWidth, 24));
        labelBox.setPreferredSize(new Dimension(paletteImageWidth, 24));
        labelBox.add(Box.createHorizontalGlue());
        JLabel label = new JLabel(bundle.getString("PaletteInstructions"));
        labelBox.add(label);
        mainBox.add(labelBox);

        Arrays.fill(paletteData, (int) (255 << 24 | (255 << 16) | (255 << 8) | 255));
        paletteImage.setMaximumSize(new Dimension(paletteImageWidth, paletteImageHeight));
        paletteImage.setPreferredSize(new Dimension(paletteImageWidth, paletteImageHeight));
        paletteImage.addMouseListener(this);
        paletteImage.addMouseMotionListener(this);
        mainBox.add(paletteImage);

        mainBox.add(Box.createVerticalStrut(5));
        Box rgbBox = Box.createHorizontalBox();
        rgbBox.add(rgbLabel);
        JPanel underPaletteBox = new JPanel();
        underPaletteBox.setLayout(new BoxLayout(underPaletteBox, BoxLayout.X_AXIS));
        underPaletteBox.setMaximumSize(new Dimension(paletteImageWidth, 24));
        underPaletteBox.setPreferredSize(new Dimension(paletteImageWidth, 24));
        underPaletteBox.add(rgbBox);
        underPaletteBox.add(Box.createHorizontalGlue());
        mainBox.add(underPaletteBox);

        mainBox.add(Box.createVerticalStrut(15));
        Box optionBox = Box.createHorizontalBox();
        optionBox.setMaximumSize(new Dimension(paletteImageWidth, 24));
        optionBox.setPreferredSize(new Dimension(paletteImageWidth, 24));
        optionBox.add(new JLabel(bundle.getString("NumberOfEntries") + ":"));
        optionBox.add(Box.createHorizontalStrut(2));
        numLinesText.setMaximumSize(new Dimension(70, 22));
        numLinesText.setPreferredSize(new Dimension(70, 22));
        numLinesText.setHorizontalAlignment(JTextField.RIGHT);
        optionBox.add(numLinesText);
        JButton changeNumLines = new JButton(bundle.getString("Update"));
        changeNumLines.setActionCommand("changeNumLines");
        changeNumLines.addActionListener(this);
        optionBox.add(changeNumLines);
        optionBox.add(Box.createHorizontalGlue());
        mainBox.add(optionBox);

        mainBox.add(Box.createVerticalStrut(15));

        sampleColorPanel.setMaximumSize(new Dimension(sampleWidth, sampleHeight));
        sampleColorPanel.setPreferredSize(new Dimension(sampleWidth, sampleHeight));
        sampleColorPanel.setToolTipText(bundle.getString("ClickToSelect"));
        sampleColorPanel.addMouseListener(this);
        Box sampleBox = Box.createHorizontalBox();
        sampleBox.setMaximumSize(new Dimension(paletteImageWidth, 24));
        sampleBox.setPreferredSize(new Dimension(paletteImageWidth, 24));
        label = new JLabel(bundle.getString("EntryNumber") + ":");
        sampleBox.add(label);
        sampleBox.add(Box.createHorizontalStrut(2));
        lineNumText.setMaximumSize(new Dimension(70, 22));
        lineNumText.setPreferredSize(new Dimension(70, 22));
        lineNumText.setHorizontalAlignment(JTextField.RIGHT);
        sampleBox.add(lineNumText);
        sampleBox.add(Box.createHorizontalStrut(5));
        label = new JLabel(bundle.getString("Color") + ":");
        sampleBox.add(label);
        sampleBox.add(Box.createHorizontalStrut(5));
        sampleBox.add(sampleColorPanel);
        sampleBox.add(Box.createHorizontalStrut(5));
        JButton updateLine = new JButton(bundle.getString("Update"));
        updateLine.setActionCommand("updateLine");
        updateLine.addActionListener(this);
        sampleBox.add(updateLine);

        Box blendBox = Box.createHorizontalBox();
        blendBox.setMaximumSize(new Dimension(paletteImageWidth, 24));
        blendBox.setPreferredSize(new Dimension(paletteImageWidth, 24));
        blendBox.add(Box.createHorizontalGlue());
        label = new JLabel(bundle.getString("From") + ":");
        blendBox.add(label);
        blendBox.add(Box.createHorizontalStrut(5));
        fromText.setMaximumSize(new Dimension(70, 22));
        fromText.setPreferredSize(new Dimension(70, 22));
        fromText.setHorizontalAlignment(JTextField.RIGHT);
        blendBox.add(fromText);
        blendBox.add(Box.createHorizontalStrut(5));
        label = new JLabel(bundle.getString("To") + ":");
        blendBox.add(label);
        blendBox.add(Box.createHorizontalStrut(5));
        toText.setMaximumSize(new Dimension(70, 22));
        toText.setPreferredSize(new Dimension(70, 22));
        toText.setHorizontalAlignment(JTextField.RIGHT);
        blendBox.add(toText);
        blendBox.add(Box.createHorizontalStrut(5));
        JButton blend = new JButton(bundle.getString("Blend"));
        blend.setActionCommand("blend");
        blend.addActionListener(this);
        blendBox.add(blend);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setMaximumSize(new Dimension(paletteImageWidth, 24));
        panel.setPreferredSize(new Dimension(paletteImageWidth, 24));
        panel.add(sampleBox);
        panel.add(Box.createHorizontalGlue());
        panel.add(blendBox);

        mainBox.add(panel);

        mainBox.add(Box.createVerticalGlue());

        pane.add(mainBox);

        JMenuBar menubar = createMenu();
        this.setJMenuBar(menubar);

        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void updateSampleColor() {
        try {
            Graphics g = sampleColorPanel.getGraphics();
            g.setColor(sampleColor);
            g.fillRect(0, 0, sampleColorPanel.getWidth(), sampleColorPanel.getHeight());

            g.setColor(Color.black);
            g.drawRect(0, 0, sampleColorPanel.getWidth() - 1, sampleColorPanel.getHeight() - 1);

        } catch (Exception e) {
            sampleColorPanel.setBackground(Color.black);
        }
    }

    private void createPaletteImage() {
        try {
            Graphics2D g = (Graphics2D) paletteImage.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (numPaletteEntries > 50) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
            if (paletteFile != null && (paletteFile.toLowerCase().contains("qual") || paletteFile.toLowerCase().contains("categorical"))) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            }

            Image image = null;

            image = createImage(new MemoryImageSource(numPaletteEntries, 1, paletteData, 0, 1));
            g.drawImage(image, 0, 0, paletteImageWidth, paletteImageHeight, paletteImage);

            g.setColor(Color.black);
            g.drawRect(0, 0, paletteImageWidth - 1, paletteImageHeight - 1);

        } catch (Exception e) {
            paletteImage.setBackground(Color.white);
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        createPaletteImage();
        updateSampleColor();
    }

    private JMenuBar createMenu() {
        JMenuBar menubar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu(bundle.getString("File"));

        JMenuItem newPalette = new JMenuItem(bundle.getString("NewPalette"));
        newPalette.setActionCommand("new");
        newPalette.addActionListener(this);
        fileMenu.add(newPalette);

        JMenuItem openPalette = new JMenuItem(bundle.getString("OpenPalette"));
        openPalette.setActionCommand("open");
        openPalette.addActionListener(this);
        fileMenu.add(openPalette);

        JMenuItem savePalette = new JMenuItem(bundle.getString("Save"));
        savePalette.setActionCommand("save");
        savePalette.addActionListener(this);
        savePalette.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(savePalette);

        JMenuItem savePaletteAs = new JMenuItem(bundle.getString("SaveAs"));
        savePaletteAs.setActionCommand("saveAs");
        savePaletteAs.addActionListener(this);
        fileMenu.add(savePaletteAs);

        fileMenu.addSeparator();
        JMenuItem closeMenuButton = new JMenuItem(bundle.getString("Close"));
        closeMenuButton.setActionCommand("close");
        closeMenuButton.addActionListener(this);
        fileMenu.add(closeMenuButton);

        menubar.add(fileMenu);

        // Tool menu
        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem clearPalette = new JMenuItem(bundle.getString("ClearPalette"));
        clearPalette.setActionCommand("clear");
        clearPalette.addActionListener(this);
        toolsMenu.add(clearPalette);

        JMenuItem fillWithRandomVals = new JMenuItem(bundle.getString("RandomPalette"));
        fillWithRandomVals.setActionCommand("random");
        fillWithRandomVals.addActionListener(this);
        toolsMenu.add(fillWithRandomVals);

        JMenuItem inversePalette = new JMenuItem(bundle.getString("InvertPalette"));
        inversePalette.setActionCommand("inverse");
        inversePalette.addActionListener(this);
        toolsMenu.add(inversePalette);

        JMenuItem reversePalette = new JMenuItem(bundle.getString("ReversePalette"));
        reversePalette.setActionCommand("reverse");
        reversePalette.addActionListener(this);
        toolsMenu.add(reversePalette);

        menubar.add(toolsMenu);

        return menubar;
    }

    private void updateLine() {
        try {
            int lineNum = Integer.parseInt(lineNumText.getText());
            if (lineNum < 0) {
                lineNum = 0;
            }
            if (lineNum >= numPaletteEntries) {
                lineNum = numPaletteEntries - 1;
            }
            int r = sampleColor.getRed();
            int g = sampleColor.getGreen();
            int b = sampleColor.getBlue();
            paletteData[lineNum] = (255 << 24) | (r << 16) | (g << 8) | b;
            createPaletteImage();
        } catch (Exception e) {
        }
    }

    private void open() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileHidingEnabled(true);

        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<>();
        String filterDescription = "Palette Files (*.pal)";
        String[] extensions = {"PAL"};
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);

        fc.setFileFilter(eff);


        fc.setCurrentDirectory(new File(paletteDirectory));
        int result = fc.showOpenDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            paletteFile = file.toString();
            readPalette();
            createPaletteImage();
            paletteName = file.getName().replace(".pal", "");
            paletteNameLabel.setText(bundle.getString("PaletteName") + ": " + paletteName);
        }
    }

    private void writePalette() {

        RandomAccessFile rOut = null;
        ByteBuffer buf = null;
        try {

            // See if the data file exists.
            File file = new File(paletteFile);
            if (!file.exists()) {
                file.delete();
            }

            rOut = new RandomAccessFile(paletteFile, "rw");
            FileChannel outChannel = rOut.getChannel();
            outChannel.position(0);

            //int writeLengthInCells = numPaletteEntries + 1;
            //int[] intArray = new int[writeLengthInCells];
            //intArray[0] = numPaletteEntries;
            //for (int j = 1; j < writeLengthInCells; j++) {
            //    intArray[j] = paletteData[j - 1];
            //}
            buf = ByteBuffer.allocate(4 * numPaletteEntries);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ib = buf.asIntBuffer();
            ib.put(paletteData);
            ib = null;
            //intArray = null;
            outChannel.write(buf);

            outChannel.close();

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            buf = null;
            if (rOut != null) {
                try {
                    rOut.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void readPalette() {
        RandomAccessFile rIn = null;
        ByteBuffer buf = null;
        try {

            // See if the data file exists.
            File file = new File(paletteFile);
            if (!file.exists()) {
                return;
            }

            numPaletteEntries = (int) (file.length() / 4);

            buf = ByteBuffer.allocate(numPaletteEntries * 4);

            rIn = new RandomAccessFile(paletteFile, "r");

            FileChannel inChannel = rIn.getChannel();

            inChannel.position(0);
            inChannel.read(buf);

            // Check the byte order.
            buf.order(ByteOrder.LITTLE_ENDIAN);


            buf.rewind();
            IntBuffer ib = buf.asIntBuffer();
            paletteData = new int[numPaletteEntries];
            ib.get(paletteData);
            ib = null;

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.toString());
            System.err.println(e.getStackTrace());
        } finally {
            if (rIn != null) {
                try {
                    rIn.close();
                } catch (Exception e) {
                }
            }
        }

        /*RandomAccessFile raf = null;
         String deliminator = "\t";
         String line;
                
         try {
            
         if (paletteFile != null) {
         File file = new File(paletteFile);
         raf = new RandomAccessFile(file, "r");
         // find out how many entries there are in the palette file.
         numPaletteEntries = 0;
         while ((line = raf.readLine()) != null) {
         if (!line.trim().equals("")) { numPaletteEntries++; }
         }
         numLinesText.setText(String.valueOf(numPaletteEntries));
         paletteData = new int[numPaletteEntries];
         raf.seek(0);
                
         String[] values;
         int i = 0;
         int r, g, b;
         //Read File Line By Line
         while ((line = raf.readLine()) != null) {
         values = line.split(deliminator);
         // make sure that the default deliminator is correct.
         if (!line.trim().equals("") && values.length < 3) {
         deliminator = " ";
         values = line.split(deliminator);
         if (!line.trim().equals("") && values.length == 1) {
         deliminator = ",";
         values = line.split(deliminator);
         }
         }
         if (values.length > 2) {
         r = Integer.parseInt(values[0]);
         g = Integer.parseInt(values[1]);
         b = Integer.parseInt(values[2]);
         paletteData[i] = (255 << 24) | (r << 16) | (g << 8) | b;
         i++;
         }
         }
         raf.close();
                
         }
            

         } catch (java.io.IOException e) {
         System.err.println("Error: " + e.getMessage());
         } catch (Exception e) {
         System.err.println("Error: " + e.getMessage());
         } finally {
         try {
         if (raf != null) {
         raf.close();
         }
         } catch (java.io.IOException ex) {
         }

         }*/
    }

    private void blend(int from, int to) {
        if (to > (numPaletteEntries - 1)) {
            to = numPaletteEntries - 1;
        }

        if (to == from) {
            return;
        }

        double lineRange = to - from;

        int r, g, b, val;
        val = paletteData[from];
        //Color newColor = new Color(paletteData[from]);
        int fromR = (val >> 16) & 0xFF; //newColor.getRed();
        int fromG = (val >> 8) & 0xFF; //newColor.getGreen();
        int fromB = val & 0xFF; //newColor.getBlue();
        val = paletteData[to];
        //newColor = new Color(paletteData[to]);
        int redRange = ((val >> 16) & 0xFF) - fromR;
        int greenRange = ((val >> 8) & 0xFF) - fromG;
        int blueRange = (val & 0xFF) - fromB;

        double proportion = 0;

        if (from < to) {
            for (int i = from; i <= to; i++) {
                proportion = (i - from) / lineRange;
                r = (int) (fromR + proportion * redRange);
                g = (int) (fromG + proportion * greenRange);
                b = (int) (fromB + proportion * blueRange);
                paletteData[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        } else {
            for (int i = from; i >= to; i--) {
                proportion = (i - from) / lineRange;
                r = (int) (fromR + proportion * redRange);
                g = (int) (fromG + proportion * greenRange);
                b = (int) (fromB + proportion * blueRange);
                paletteData[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        }

        createPaletteImage();

    }

    private void reverse() {
        int i, j;
        int numPaletteEntriesLessOne = numPaletteEntries - 1;
        int[] rawData = paletteData.clone();
        for (i = 0; i < numPaletteEntries; i++) {
            j = numPaletteEntriesLessOne - i;
            paletteData[i] = rawData[j];
        }
        createPaletteImage();
    }

    private void inverse() {
        int i, r, g, b;
        //Color newColor;
        for (i = 0; i < numPaletteEntries; i++) {
            //newColor = new Color(paletteData[i]);
            r = 255 - (paletteData[i] >> 16) & 0xFF;//newColor.getRed();
            g = 255 - (paletteData[i] >> 8) & 0xFF; //newColor.getGreen();  
            b = 255 - paletteData[i] & 0xFF; //newColor.getBlue();  
            paletteData[i] = (255 << 24) | (r << 16) | (g << 8) | b;
        }
        createPaletteImage();
    }

    private void changeNumLines() {
        try {
            int val = Integer.parseInt(numLinesText.getText());
            if (val > 0 && val < 10000) {
                numPaletteEntries = val;
                paletteData = new int[numPaletteEntries];
                Arrays.fill(paletteData, (int) (255 << 24 | (255 << 16) | (255 << 8) | 255));
                createPaletteImage();
            }
        } catch (Exception e) {
        }
    }

    private void save() {
        if (paletteFile == null) {
            saveAs();
        } else {
            writePalette();
        }
    }

    private void saveAs() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileHidingEnabled(true);

        // set the filter.
        //ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String filterDescription = "Palette Files (*.pal)";
        String[] extensions = {"PAL"};
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);
        fc.setFileFilter(eff);

        fc.setCurrentDirectory(new File(paletteDirectory));
        int result = fc.showSaveDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            if (file.exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(this,
                        "The file already exists.\n"
                        + "Would you like to overwrite it?",
                        "Whitebox GAT Message",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, //do not use a custom Icon
                        options, //the titles of buttons
                        options[0]); //default button title

                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            paletteFile = file.toString();
            if (!paletteFile.contains(".pal")) {
                paletteFile = paletteFile + ".pal";
            }
            paletteName = file.getName().replace(".pal", "");
            paletteNameLabel.setText("Palette Name: " + paletteName);
            writePalette();
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("ok")) {
            Color newColor = JColorChooser.showDialog(this, "Choose Color", Color.white);
        } else if (actionCommand.equals("close")) {
            this.dispose();
        } else if (actionCommand.equals("open")) {
            open();
        } else if (actionCommand.equals("updateLine")) {
            updateLine();
        } else if (actionCommand.equals("reverse")) {
            reverse();
        } else if (actionCommand.equals("clear")) {
            Arrays.fill(paletteData, (int) (255 << 24 | (255 << 16) | (255 << 8) | 255));
            createPaletteImage();
        } else if (actionCommand.equals("inverse")) {
            inverse();
        } else if (actionCommand.equals("changeNumLines")) {
            changeNumLines();
        } else if (actionCommand.equals("save")) {
            save();
        } else if (actionCommand.equals("saveAs")) {
            saveAs();
        } else if (actionCommand.equals("new")) {
            numPaletteEntries = 2048;
            paletteFile = null;
            paletteName = "NewPalette";
            paletteNameLabel.setText("Palette Name: " + paletteName);
            paletteData = new int[numPaletteEntries];
            Arrays.fill(paletteData, (int) (255 << 24 | (255 << 16) | (255 << 8) | 255));
            numLinesText.setText(String.valueOf(numPaletteEntries));
            createPaletteImage();
        } else if (actionCommand.equals("blend")) {
            int from = Integer.parseInt(fromText.getText());
            int to = Integer.parseInt(toText.getText());
            blend(from, to);
        } else if (actionCommand.equals("random")) {
            Random generator = new Random();
            int r, g, b;
            for (int i = 0; i < numPaletteEntries; i++) {
                r = (int) (255 * generator.nextFloat());
                g = (int) (255 * generator.nextFloat());
                b = (int) (255 * generator.nextFloat());
//                r = (int)(130 + 35 * generator.nextFloat());
//                g = (int)(130 + 35 * generator.nextFloat());
//                b = (int)(190 + 65 * generator.nextFloat());

                paletteData[i] = (int) (255 << 24 | (r << 16) | (g << 8) | b);
            }
            createPaletteImage();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Object source = e.getSource();
        if (source == paletteImage) {
            int lineNum = (int) ((double) (e.getX()) / paletteImageWidth * numPaletteEntries);
            int paletteValue = paletteData[lineNum];
            //Color newColor = new Color(paletteValue);
            int r = (paletteData[lineNum] >> 16) & 0xFF;//newColor.getRed();
            int g = (paletteData[lineNum] >> 8) & 0xFF; //newColor.getGreen();  
            int b = paletteData[lineNum] & 0xFF; //newColor.getBlue();  
            rgbLabel.setText(bundle.getString("Entry") + " " + lineNum + " : "
                    + bundle.getString("Red")
                    + " " + r + " : " + bundle.getString("Green") + " "
                    + g + " : " + bundle.getString("Blue") + " " + b);
        }
    }
    boolean mouseDragged = false;

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseDragged = true;
        Object source = e.getSource();
        if (source == paletteImage) {
            int lineNum = (int) ((double) (e.getX()) / paletteImageWidth * numPaletteEntries);
            int paletteValue = paletteData[lineNum];
            //Color newColor = new Color(paletteValue);
            int r = (paletteData[lineNum] >> 16) & 0xFF;//newColor.getRed();
            int g = (paletteData[lineNum] >> 8) & 0xFF; //newColor.getGreen();  
            int b = paletteData[lineNum] & 0xFF; //newColor.getBlue();  
            rgbLabel.setText(bundle.getString("Entry") + " " + lineNum + " : "
                    + bundle.getString("Red")
                    + " " + r + " : " + bundle.getString("Green") + " "
                    + g + " : " + bundle.getString("Blue") + " " + b);
            blend(startLineNum, lineNum);
        }
    }
    private int startLineNum = -1;

    @Override
    public void mousePressed(MouseEvent e) {
        Object source = e.getSource();
        if (source == sampleColorPanel) {
            sampleColor = JColorChooser.showDialog(this, bundle.getString("ChooseColor"), sampleColor);
            updateSampleColor();
        } else if (source == paletteImage) {
            int lineNum = (int) ((double) (e.getX()) / paletteImageWidth * numPaletteEntries);
            startLineNum = lineNum;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Object source = e.getSource();
        if (source == paletteImage) {
            int lineNum = (int) ((double) (e.getX()) / paletteImageWidth * numPaletteEntries);
            if ((startLineNum != lineNum) && (mouseDragged)
                    && (startLineNum >= 0)) {
                blend(startLineNum, lineNum);
                startLineNum = -1;
            }

        }
        mouseDragged = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseDragged = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        try {
            Object source = e.getSource();
            if (source == paletteImage) {
                if (e.getClickCount() == 2) {
                    int lineNum = (int) ((double) (e.getX()) / paletteImageWidth * numPaletteEntries);
                    Color newColor = JColorChooser.showDialog(this, bundle.getString("ChooseColor"),
                            new Color(paletteData[lineNum]));
                    int r = newColor.getRed();
                    int g = newColor.getGreen();
                    int b = newColor.getBlue();
                    paletteData[lineNum] = (255 << 24) | (r << 16) | (g << 8) | b;
                    createPaletteImage();
                }
            }
        } catch (Exception ex) {
            // do nothing
        }
    }
}
