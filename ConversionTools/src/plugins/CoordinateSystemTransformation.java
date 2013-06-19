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
package plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;
import java.util.ResourceBundle;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.Communicator;
import whitebox.ui.ComboBoxProperty;
import whitebox.ui.plugin_dialog.DialogFile;
import whitebox.internationalization.WhiteboxInternationalizationTools;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class CoordinateSystemTransformation implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "CoordinateSystemTransformation";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Coordinate System Transformation";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Converts an image between coordinate systems.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"ConversionTools"};
        return ret;
    }

    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the
     * class that the plugin will send all feedback messages, progress updates,
     * and return objects.
     *
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    /**
     * Used to communicate feedback pop-up messages between a plugin tool and
     * the main Whitebox user-interface.
     *
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }

    /**
     * Used to communicate a return object from a plugin tool to the main
     * Whitebox user-interface.
     *
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.println("Progress: " + progress + "%");
        }
    }

    /**
     * Sets the arguments (parameters) used by the plugin.
     *
     * @param args
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     *
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    private boolean amIActive = false;

    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     *
     * @return a boolean describing whether or not the plugin is actively being
     * used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;

        returnData(new CoordinateTransformDialog());

    }

    class CoordinateTransformDialog extends JPanel {

        private ResourceBundle bundle;
        private Ellipsoid ellipsoid = Ellipsoid.WGS_84;
        private JDialog myDialog;
        private boolean myDialogFound = false;

        public CoordinateTransformDialog() {
            initUi();
        }

        private void initUi() {
            this.setName("Coordinate System Transformation"); // sets the title on it's dialog

            if (myHost == null) {
                bundle = WhiteboxInternationalizationTools.getGuiLabelsBundle();
                myHost = new whitebox.plugins.PluginHost();
            } else {
                bundle = myHost.getGuiLabelsBundle();
            }
            
            Box mainBox = Box.createVerticalBox();


            // input file
            args = new String[7];
            args[0] = "inputFileDialog";
            args[1] = "Input file";
            args[2] = "Input File";
            args[3] = "0"; // open mode
            args[4] = "true";
            args[5] = "Raster Files (*.dep), DEP";
            args[6] = "false";
            Communicator communicator = (Communicator) myHost;
            DialogFile dfIn = new DialogFile(communicator);
            dfIn.setArgs(args);

            dfIn.setTextFieldActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    showFeedback("Hello");
                }
            });

            mainBox.add(dfIn);

            mainBox.add(Box.createVerticalStrut(10));

            // output file
            args[0] = "outputFileDialog";
            args[1] = "Output file";
            args[2] = "Output File";
            args[3] = "1"; // save mode
            args[4] = "true";
            args[5] = "Raster Files (*.dep), DEP";
            args[6] = "false";
            DialogFile dfOut = new DialogFile(communicator);
            dfOut.setArgs(args);

            dfOut.setTextFieldActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    //showFeedback("Hello");
                }
            });

            mainBox.add(dfOut);

            // ellipsoid combobox
            String[] ellipsoids = new String[Ellipsoid.values().length];

            int i = 0;
            for (Ellipsoid ellipse : Ellipsoid.values()) {
                ellipsoids[i] = ellipse.ellipsoidName();
                i++;
            }


            ComboBoxProperty ellipsoidChooser = new ComboBoxProperty(
                    bundle.getString("Ellipsoid") + ":", ellipsoids, 0);
            ellipsoidChooser.setName("ellipsoidChooser");
            ItemListener il = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        Object item = e.getItem();
                        String code;
                        switch (item.toString().toLowerCase()) {
                            case "chinese (china)":
                                code = "zh_CN";
                                break;

                        }

                    }
                }
            };
            ellipsoidChooser.setParentListener(il);
            ellipsoidChooser.setBackColour(this.getBackground());
            mainBox.add(ellipsoidChooser);

            Box btnBox = Box.createHorizontalBox();
            btnBox.add(Box.createHorizontalGlue());
            JButton ok = new JButton(bundle.getString("OK"));
            ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //setVisible(false);
                }
            });
            btnBox.add(ok);

            btnBox.add(Box.createHorizontalStrut(10));

            JButton close = new JButton(bundle.getString("Close"));
            close.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeParentDialog();
                }
            });
            btnBox.add(close);

            btnBox.add(Box.createHorizontalGlue());

            mainBox.add(Box.createVerticalGlue());
            mainBox.add(btnBox);
            mainBox.add(Box.createVerticalStrut(15));

            this.add(mainBox);

            this.setPreferredSize(new Dimension(600, 700));
        }

        private void closeParentDialog() {
            try {
                this.getTopLevelAncestor().setVisible(false);
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }
    
    public static void main(String[] args) {
        CoordinateSystemTransformation cst = new CoordinateSystemTransformation();
        cst.testLaunch();
        
        
    }
    
    private void testLaunch() {
        JFrame frame = new JFrame();
        frame.add(new CoordinateTransformDialog());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
