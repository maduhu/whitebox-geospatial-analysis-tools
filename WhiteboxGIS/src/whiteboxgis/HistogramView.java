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

import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import java.awt.Frame;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.print.*;
import javax.print.attribute.*;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.util.ArrayList;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.BoxLayout;
import whitebox.structures.ExtensionFileFilter;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class HistogramView extends JDialog implements ActionListener {
    private Histogram histo = null;
    private String headerFile;
    private String workingDirectory;
    private Frame owner;
    private JButton cumulative;
    private boolean cumulativeBool = false;
    
    public HistogramView(Frame owner, boolean modal, String headerFile, String workingDirectory) {
        super(owner, modal);
        this.owner = owner;
        this.headerFile = headerFile;
        this.workingDirectory = workingDirectory;
        createGui();
    }
    
    private void createGui() {
        setTitle("Histogram");
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        
        histo = new Histogram(headerFile);
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        JButton btnSaveImage = createButton("Save", "Export the histogram as an image");
        JButton btnPrint = createButton("Print", "Print histogram");
        JButton btnExit = createButton("Exit", "Exit");
        cumulative = createButton("Switch to CDF", "cumulative distribution function (CDF)");
        JButton btnRefresh = createButton("Refresh", "Refresh histogram");
        
        buttonPane.add(Box.createHorizontalStrut(10));
        buttonPane.add(btnSaveImage);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(btnPrint);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(btnRefresh);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(cumulative);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(btnExit);
        buttonPane.add(Box.createHorizontalStrut(10));
        
            
        Container contentPane = this.getContentPane();
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        contentPane.add(histo, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(600, 400));
        this.pack();
        this.setVisible(true);
        
    }
    
    private JButton createButton(String buttonLabel, String toolTip) {
        JButton btn = new JButton(buttonLabel);
        btn.addActionListener(this);
        btn.setActionCommand(buttonLabel);
        btn.setToolTipText(toolTip);
        //btn.setPreferredSize(new Dimension(width, 22));
        return btn;
    }
    
    private int showFeedback(String message) {
        if (owner instanceof WhiteboxPluginHost) {
            WhiteboxPluginHost host = (WhiteboxPluginHost)owner;
            return host.showFeedback(message);
        } else {
            return -1;
        }
    }
    
    private void saveHistoAsImage() {
        // get the possible image name.
        String imageName = histo.getShortName();

        // Ask the user to specify a file name for saving the histo.
        String pathSep = File.separator;
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(new File(workingDirectory + pathSep + imageName + ".png"));
        fc.setAcceptAllFileFilterUsed(false);

        File f = new File(workingDirectory + pathSep + imageName + ".png");
        fc.setSelectedFile(f);
                
        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String[] extensions = ImageIO.getReaderFormatNames(); //{"PNG", "JPEG", "JPG"};
        String filterDescription = "Image Files (" + extensions[0];
        for (int i = 1; i < extensions.length; i++) {
            filterDescription += ", " + extensions[i];
        }
        filterDescription += ")";
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);
        fc.setFileFilter(eff);

        int result = fc.showSaveDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            // see if file has an extension.
            if (file.toString().lastIndexOf(".") <= 0) {
                String fileName = file.toString() + ".png";
                file = new File(fileName);
            }
            
            String fileDirectory = file.getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                workingDirectory = fileDirectory;
            }

            // see if the file exists already, and if so, should it be overwritten?
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
            if (!histo.saveToImage(file.toString())) {
                showFeedback("An error occurred while saving the map to the image file.");
            }
        }
        
    }

    private void printHisto() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        //PageFormat pf = job.pageDialog(aset);
        job.setPrintable(histo);
        boolean ok = job.printDialog(aset);
        if (ok) {
            try {
                job.print(aset);
            } catch (PrinterException ex) {
                showFeedback("An error was encountered while printing." + ex);
                /* The job did not successfully complete */
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        String ac = ae.getActionCommand().toLowerCase();
        if (ac.equals("exit")) {
            this.dispose();
        } else if (ac.equals("print")) {
            printHisto();
        } else if (ac.equals("save")) {
            saveHistoAsImage();
        } else if (ac.equals("switch to cdf")) {
            cumulativeBool = !cumulativeBool;
            histo.setCumulative(cumulativeBool);
            if (!cumulativeBool) {
                cumulative.setText("Switch to CDF");
                cumulative.setToolTipText("cumulative distribution function (CDF)");
            } else {
                cumulative.setText("Switch to PDF");
                cumulative.setToolTipText("Probability distribution function (PDF)");
            }
        } else if (ac.equals("refresh")) {
            histo.refresh();
        }
    }
}
