package whiteboxgis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author john
 */
public class PaletteChooser extends JDialog implements MouseListener, ActionListener, WindowListener {
    private String paletteDirectory;
    private String selectedFile;
    private JLabel label = new JLabel();
    private ArrayList<PaletteImage> images = new ArrayList<PaletteImage>();
    private double nonlinearity = 1.0;
    
    public PaletteChooser(Frame owner, boolean modal, String paletteDirectory, String selectedFile, boolean isPaletteReversed, double nonlinearity) {
        super(owner, modal);
        this.paletteDirectory = paletteDirectory;
        this.selectedFile = selectedFile;
        this.isPaletteReversed = isPaletteReversed;
        this.nonlinearity = nonlinearity;
        initUI();
    }

    private void initUI() {
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        JButton newPalette = new JButton ("New Palette");
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        setTitle("Choose Palette");
        String paletteFile = "";
        
        label = new JLabel("Palette: " + selectedFile.replace(paletteDirectory, "").replace(".pal", ""));
        
        Box box = Box.createVerticalBox();
        
        getImages();
        updateSelectedImage();
        
        for (int i = 0; i < images.size(); i++) {
            box.add(images.get(i));
            box.add(Box.createVerticalStrut(2));
        }
        
        JScrollPane scroll = new JScrollPane(box);
        
        this.add(scroll);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(300, 44));
        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalStrut(2));
        box1.add(label);
        box1.add(Box.createHorizontalGlue());
        Box box2 = Box.createHorizontalBox();
        ok.addActionListener(this);
        ok.setActionCommand("ok");
        box2.add(ok);
        box2.add(Box.createHorizontalStrut(5));
        box2.add(newPalette);
        newPalette.addActionListener(this);
        newPalette.setActionCommand("newPalette");
        cancel.addActionListener(this);
        cancel.setActionCommand("cancel");
        box2.add(Box.createHorizontalStrut(5));
        box2.add(cancel);
        panel.add(box1);
        panel.add(box2);
        this.getContentPane().add(panel, BorderLayout.SOUTH);
        this.addWindowListener(this);
        
    }
    
    private void getImages() {
        File dir = new File(paletteDirectory);

        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".pal");
            }
        };
        
        String paletteFile;
        String[] children = dir.list(filter);
        
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                paletteFile = children[i];
                PaletteImage paletteImage = new PaletteImage(256, 18, paletteDirectory + paletteFile, isPaletteReversed, PaletteImage.HORIZONTAL_ORIENTATION);
                paletteImage.setNonlinearity(nonlinearity);
                paletteImage.setMinimumSize(new Dimension(256, 18));
                paletteImage.addMouseListener(this);
                images.add(paletteImage);
            }
        }
    }
    
    private void updateSelectedImage() {
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getPaletteFile().equals(myValue)) {
                images.get(i).isSelected = true;
            } else {
                images.get(i).isSelected = false;
            }
            images.get(i).repaint();
        }
    }

    boolean isPaletteReversed = false;
    public boolean getIsPaletteReversed() {
        return isPaletteReversed;
    }

    public void setIsPaletteReversed(boolean value) {
        isPaletteReversed = value;
    }

    String myValue = selectedFile;
    public String getValue() {
        return myValue;
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getSource() instanceof PaletteImage) {
            PaletteImage image = (PaletteImage) (e.getSource());
            myValue = image.getPaletteFile();
            updateSelectedImage();
            label.setText("Palette: " + image.getPaletteFile().replace(paletteDirectory, "").replace(".pal", ""));
            if (e.getClickCount() == 2) {
                this.setVisible(false);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
        
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        
    }

    @Override
    public void windowActivated(WindowEvent e) {
        
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        
    }
    
    @Override
    public void windowClosing(WindowEvent we) {
        myValue = "";      
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("ok")) {
            this.setVisible(false);
        } else if (actionCommand.equals("cancel")) {
            myValue = "";
            this.setVisible(false);
        } else if (actionCommand.equals("newPalette")) {
            myValue = "createNewPalette";
            this.setVisible(false);
        }
    }

}
