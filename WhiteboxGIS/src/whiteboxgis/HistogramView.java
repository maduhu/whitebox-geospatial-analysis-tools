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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.structures.ExtensionFileFilter;

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
    private WhiteboxPluginHost host;
    private ResourceBundle bundle;
    private ResourceBundle messages;
    
    public HistogramView(Frame owner, boolean modal, String headerFile, String workingDirectory) {
        super(owner, modal);
        this.owner = owner;
        if (owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost) owner;
            bundle = host.getGuiLabelsBundle();
            messages = host.getMessageBundle();
        }
        this.headerFile = headerFile;
        this.workingDirectory = workingDirectory;
        createGui();
    }
    
    private void createGui() {
        setTitle(bundle.getString("Histogram"));
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        
        histo = new Histogram(headerFile);
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        JButton btnSaveImage = createButton(bundle.getString("Save"), 
                bundle.getString("ExportHistogram"));
        JButton btnPrint = createButton(bundle.getString("Print"), 
                bundle.getString("PrintHistogram"));
        JButton btnExit = createButton(bundle.getString("Exit"), 
                bundle.getString("Exit"));
        cumulative = createButton(bundle.getString("SwitchToCDF"), 
                bundle.getString("SwitchToCDFTip"));
        JButton btnRefresh = createButton(bundle.getString("Refresh"), 
                bundle.getString("Refresh"));
        
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
        if (host != null) {
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
        ArrayList<ExtensionFileFilter> filters = new ArrayList<>();
        String[] extensions = ImageIO.getReaderFormatNames(); //{"PNG", "JPEG", "JPG"};
        String filterDescription = bundle.getString("ImageFiles") + " (" + extensions[0];
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
                        messages.getString("FileExists") + "\n"
                        + messages.getString("Overwrite"),
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
                showFeedback(messages.getString("ErrorWhileSaving"));
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
                showFeedback(messages.getString("PrintingError") + " " + ex);
                /* The job did not successfully complete */
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        String ac = ae.getActionCommand().toLowerCase();
        switch (ac) {
            case "exit":
                this.dispose();
                break;
            case "print":
                printHisto();
                break;
            case "save":
                saveHistoAsImage();
                break;
            case "switch to cdf":
                cumulativeBool = !cumulativeBool;
                histo.setCumulative(cumulativeBool);
                if (!cumulativeBool) {
                    cumulative.setText(bundle.getString("SwitchToCDF"));
                    cumulative.setToolTipText(bundle.getString("SwitchToCDFTip"));
                } else {
                    cumulative.setText(bundle.getString("SwitchToPDF"));
                    cumulative.setToolTipText(bundle.getString("SwitchToPDFTip"));
                }
                break;
            case "refresh":
                histo.refresh();
                break;
        }
    }
}
