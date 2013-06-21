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
        DialogFile dfIn;
        DialogFile dfOut;
        double a, b, f, bigE, esq, ePrimeSq, e1;
        double bigA;
        double c;
        double bigT;
        double bigN;
        double Mterm4;
        double MTerm3;
        double Mterm2;
        double MTerm1;
        double bigD;
        double R1;
        double n1;
        double T1;
        double C1;
        double phi1;
        double mu;
        double bigM;
        final double pi = Math.PI;
        final double deg2Rad = pi / 180;
        double easting, northing;
        double lambdaNot;
        double FN, FE;
        double north, south, east, west;
        double maxPhi, minNorthing, minEasting, maxEasting, maxNorthing, minPhi, minLambda, maxLambda;
        String projectionDirection = "ll2utm";
        boolean western, northern;
        double phi, lambda;

        public CoordinateTransformDialog() {
            test();
            
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
            dfIn = new DialogFile(communicator);
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
            dfOut = new DialogFile(communicator);
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


            // ellipsoid
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

            // interpolation method
            String[] interpolationMethods = {"Nearest Neighbour", "Bilinear"};
            ComboBoxProperty interpolationChooser = new ComboBoxProperty(
                    "Interpolation:", interpolationMethods, 0);
            interpolationChooser.setName("interpolationChooser");
            ItemListener il2 = new ItemListener() {
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
            interpolationChooser.setParentListener(il2);
            interpolationChooser.setBackColour(this.getBackground());
            mainBox.add(interpolationChooser);

            JRadioButton ll2utmButton = new JRadioButton("Lat/Long to UTM");
            //LL2UTMButton.setMnemonic(KeyEvent.VK_R);
            ll2utmButton.setActionCommand("ll2utm");
            ll2utmButton.setSelected(true);

            JRadioButton utm2llButton = new JRadioButton("UTM to Lat/Long");
            //utm2llButton.setMnemonic(KeyEvent.VK_P);
            utm2llButton.setActionCommand("utm2ll");

            //Group the radio buttons.
            ButtonGroup group = new ButtonGroup();
            group.add(ll2utmButton);
            group.add(utm2llButton);

            //Register a listener for the radio buttons.
            ll2utmButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    projectionDirection = "ll2utm";
                }
            });
            utm2llButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    projectionDirection = "utm2ll";
                }
            });

            Box box1 = Box.createHorizontalBox();
            box1.add(ll2utmButton);
            box1.add(utm2llButton);
            mainBox.add(box1);


            // N or S
            JRadioButton northButton = new JRadioButton("North");
            //LL2UTMButton.setMnemonic(KeyEvent.VK_R);
            northButton.setActionCommand("north");
            northButton.setSelected(true);

            JRadioButton southButton = new JRadioButton("South");
            //utm2llButton.setMnemonic(KeyEvent.VK_P);
            southButton.setActionCommand("south");

            //Group the radio buttons.
            ButtonGroup group2 = new ButtonGroup();
            group2.add(northButton);
            group2.add(southButton);

            //Register a listener for the radio buttons.
            //ll2utmButton.addActionListener(this);
            //utm2llButton.addActionListener(this);

            Box box2 = Box.createHorizontalBox();
            box2.add(northButton);
            box2.add(southButton);
            mainBox.add(box2);

            // interpolation method
            String[] zones = new String[60];
            for (int a = 0; a < 60; a++) {
                zones[a] = String.valueOf(a + 1);
            }
            ComboBoxProperty zoneChooser = new ComboBoxProperty(
                    "Zone:", zones, 0);
            zoneChooser.setName("zoneChooser");
            ItemListener il3 = new ItemListener() {
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
            zoneChooser.setParentListener(il3);
            zoneChooser.setBackColour(this.getBackground());
            mainBox.add(zoneChooser);

            Box btnBox = Box.createHorizontalBox();
            btnBox.add(Box.createHorizontalGlue());
            JButton ok = new JButton(bundle.getString("OK"));
            ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    run();
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

        private void test() {
            double lat, lon, lon0, a, b, f, k0, e, e2, n, rho, nu, p;
            double S, A0, B0, C0, D0, E0;
            double tmp;
            long zone;
            
            lat = 40.5 * deg2Rad;
            lon = -73.5 * deg2Rad;
            tmp = (-73.5 - (-180)) / 6;
            zone = Math.round(tmp) + 1;
            if (zone > tmp) {
                zone = zone - 1;
            }
            lon0 = (-180 + zone * 6 - 3) * deg2Rad;
            
            // datum constants
            a = ellipsoid.majorAxis();
            b = ellipsoid.minorAxis();
            f = (a - b) / a;
            k0 = 0.9996;
            e = Math.sqrt(1 - (b * b) / (a * a));
            e2 = (e * e) / (1 - e * e);
            n = (a - b) /(a + b);
            rho = a * (1 - e * e) / (Math.pow((1 - (e * Math.sin(lat)) * (e * Math.sin(lat))), (3.0 / 2)));
            nu = a / Math.sqrt((1 - (e * Math.sin(lat)) * (e * Math.sin(lat))));
            
            
            // Calculate Meridional Arc Length
            double n4 = Math.pow(n, 4);
            A0 = a * (1 - n + (5 * n * n / 4.0) * (1 - n) +(81 * n4 / 64.0) * (1 - n));
            B0 = (3 * a * n / 2.0) * (1 - n - (7 * n * n / 8.0) * (1 - n) + 55 * n4 / 64.0);
            C0 = (15 * a * n * n / 16.0) * (1 - n +(3 * n * n / 4.0) * (1 - n));
            D0 = (35 * a * Math.pow(n, 3) / 48.0) * (1 - n + 11 * n * n / 16.0);
            E0 = (315 * a * n4 / 51.0) * (1 - n);
            S = A0 * lat - B0 * Math.sin(2 * lat) + C0 * Math.sin(4 * lat) - D0 * Math.sin(6 * lat) + E0 * Math.sin(8 * lat);
            
            
            
            bigE = Math.sqrt((a * a - b * b) / (a * a));
            esq = (a * a - b * b) / (a * a);
            ePrimeSq = (bigE * bigE) / (1 - bigE * bigE);
            e1 = (1 - Math.sqrt(1 - esq)) / (1 + Math.sqrt(1 - esq));

            east = -73.5;
            north = 40.5;
            western = (east <= 0) ? true : false;
            northern = (north >= 0) ? true : false;
            tmp = (east - (-180)) / 6;
            zone = Math.round(tmp) + 1;
            if (zone > tmp) {
                zone = zone - 1;
            }
            lambdaNot = (-180 + zone * 6 - 3) * deg2Rad; // in radians
            FN = (northern) ? 0 : 10000000;
            FE = 500000;

            phi = north * deg2Rad;
            lambda = east * deg2Rad;
            ll2UTM(phi, lambda);
            System.out.println("Easting: " + easting + "\tNorthing: " + northing);

        }

        private void run() {
            
            double tmp;
            long tmp1;
            // get the file names
            String inputFile = dfIn.getValue();
            String outputFile = dfOut.getValue();

            // get the grid resolution



            // calculate ellipsoid parameters; a = major axis, b = minor axis
            a = ellipsoid.majorAxis();
            b = ellipsoid.minorAxis();
            f = (a - b) / a;
            bigE = Math.sqrt((a * a - b * b) / (a * a));
            esq = (a * a - b * b) / (a * a);
            ePrimeSq = bigE * bigE / (1 - bigE * bigE);
            e1 = (1 - Math.sqrt(1 - esq)) / (1 + Math.sqrt(1 - esq));

            WhiteboxRaster input = new WhiteboxRaster(inputFile, "r");

//            Image.HeaderFileName = InputFile
//            Image.WriteChangesToFile = False
//            totalCol = Image.NumberColumns
//            totalRow = Image.NumberRows
//            GridRes = Image.GridResolution
//
//            If File.Exists(OutputFile) Then File.Delete(OutputFile)
//            If File.Exists(Replace(OutputFile, ".dep", ".tas")) Then File.Delete(Replace(OutputFile, ".dep", ".tas"))


            minEasting = Double.POSITIVE_INFINITY;
            maxEasting = Double.NEGATIVE_INFINITY;
            minNorthing = Double.POSITIVE_INFINITY;
            maxNorthing = Double.NEGATIVE_INFINITY;

            east = input.getEast();
            west = input.getWest();
            south = input.getSouth();
            north = input.getNorth();

            if (projectionDirection.equals("ll2utm")) {

                western = (east <= 0) ? true : false;
                northern = (north >= 0) ? true : false;
                tmp = (east - (-180)) / 6;
                tmp1 = Math.round(tmp) + 1;
                if (tmp1 > tmp) {
                    tmp1 = tmp1 - 1;
                }
                lambdaNot = (-180 + tmp1 * 6 - 3) * deg2Rad; // in radians
                FN = (northern) ? 0 : 10000000;
                FE = 500000;


                //Calculate the Eastings and northings of each of the four corners and find the min and max
                phi = north * deg2Rad;
                lambda = west * deg2Rad;
                ll2UTM(phi, lambda);
                if (easting < minEasting) {
                    minEasting = easting;
                }
                if (northing < minNorthing) {
                    minNorthing = northing;
                }
                if (easting > maxEasting) {
                    maxEasting = easting;
                }
                if (northing > maxNorthing) {
                    maxNorthing = northing;
                }

                phi = north * deg2Rad;
                lambda = east * deg2Rad;
                ll2UTM(phi, lambda);
                if (easting < minEasting) {
                    minEasting = easting;
                }
                if (northing < minNorthing) {
                    minNorthing = northing;
                }
                if (easting > maxEasting) {
                    maxEasting = easting;
                }
                if (northing > maxNorthing) {
                    maxNorthing = northing;
                }

                phi = south * deg2Rad;
                lambda = east * deg2Rad;
                ll2UTM(phi, lambda);
                if (easting < minEasting) {
                    minEasting = easting;
                }
                if (northing < minNorthing) {
                    minNorthing = northing;
                }
                if (easting > maxEasting) {
                    maxEasting = easting;
                }
                if (northing > maxNorthing) {
                    maxNorthing = northing;
                }

                phi = south * deg2Rad;
                lambda = west * deg2Rad;
                ll2UTM(phi, lambda);
                if (easting < minEasting) {
                    minEasting = easting;
                }
                if (northing < minNorthing) {
                    minNorthing = northing;
                }
                if (easting > maxEasting) {
                    maxEasting = easting;
                }
                if (northing > maxNorthing) {
                    maxNorthing = northing;
                }

//                newTotalCol = (maxEasting - minEasting) / SpecifiedGridRes
//                newTotalRow = (maxNorthing - minNorthing) / SpecifiedGridRes
//
//                Output.HeaderFileName = OutputFile
//                Output.WriteChangesToFile = True
//                With Output
//                    .East = maxEasting
//                    .GridResolution = SpecifiedGridRes
//                    .North = maxNorthing
//                    .NumberColumns = newTotalCol
//                    .NumberRows = newTotalRow
//                    If northern Then
//                        .Projection = "UTM" & " " & tmp1.ToString & "N"
//                    Else
//                        .Projection = "UTM" & " " & tmp1.ToString & "S"
//                    End If
//                    .South = minNorthing
//                    .West = minEasting
//                    .XYUnits = "meters"
//                    .ZUnits = Image.ZUnits
//                    .DataType = Image.DataType
//                    .DataScale = Image.DataScale
//                    .PreferredPalette = Image.PreferredPalette
//                    .SetBlockData()
//                    .InitializeGrid(newTotalCol, newTotalRow)
//                End With
//
//                If cbResample.SelectedItem.ToString = "Nearest neighbour" Then
//                    nearestNeighbor_ll2UTM()
//                Else
//                    Bilinear_ll2UTM()
//                End If


            }

            amIActive = false;
        }

        private void ll2UTM(double phi, double lambda) {
            try {
                /*from Snyder (1987)
                 phi and lambda should be in radians
                 note: phi = latitude, lambda = longitude
                 */

                MTerm1 = (1 - (esq / 4) - (3 * bigE * bigE * bigE * bigE / 64) - (5
                        * bigE * bigE * bigE * bigE * bigE * bigE / 256)) * phi;
                Mterm2 = ((3 * esq / 8) + (3 * bigE * bigE * bigE * bigE / 32) + 45 * (bigE * bigE * bigE * bigE * bigE * bigE / 1024)) * Math.sin(2 * phi);
                MTerm3 = ((15 * bigE * bigE * bigE * bigE / 256)
                        + (45 * bigE * bigE * bigE * bigE * bigE * bigE / 1024)) * Math.sin(4 * phi);
                Mterm4 = (35 * bigE * bigE * bigE * bigE * bigE * bigE / 3072) * Math.sin(6 * phi);
                bigM = a * (MTerm1 - Mterm2 + MTerm3 - Mterm4);
                if ((phi != (pi / 2)) || (phi == (-pi / 2))) {
                    bigN = Math.sqrt(a / (1 - esq * (Math.sin(phi) * Math.sin(phi))));
                    bigT = (Math.tan(phi) * Math.tan(phi));
                    c = ePrimeSq * (Math.cos(phi) * Math.cos(phi));
                    bigA = (lambda - lambdaNot) * Math.cos(phi);
                    easting = FE + 0.9996 * bigN * (bigA + (1 - bigT + c)
                            * (bigA * bigA * bigA) / 6 + (5 - 18 * bigT + (bigT * bigT)
                            + 72 * c - 58 * ePrimeSq) * (bigA * bigA * bigA * bigA * bigA) / 120);
                    northing = FN + 0.9996 * (bigM - 0 + bigN * Math.tan(phi)
                            * ((bigA * bigA) / 2 + (5 - bigT + 9 * c + 4 * (c * c))
                            * (bigA * bigA * bigA * bigA) / 24 + (61 - 58 * bigT
                            + (bigT * bigT) + 600 * c - 330 * ePrimeSq)
                            * (bigA * bigA * bigA * bigA * bigA * bigA) / 720));
                } else {
                    easting = FE;
                    northing = FN + 0.9996 * bigM;
                }
            } catch (Exception e) {
                showFeedback(e.toString());
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
