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

import whiteboxgis.user_interfaces.PaletteManager;
import whiteboxgis.user_interfaces.AttributesFileViewer;
import whitebox.cartographic.MapInfo;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import javax.swing.*;
import whitebox.cartographic.PointMarkers;
import whitebox.cartographic.PointMarkers.MarkerStyle;
import whitebox.geospatialfiles.LASReader;
import whitebox.geospatialfiles.RasterLayerInfo;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.WhiteboxRasterInfo;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.interfaces.Communicator;
import whitebox.interfaces.MapLayer;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.geospatialfiles.LasLayerInfo;
import whitebox.geospatialfiles.shapefile.ShapeTypeDimension;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class LayerProperties extends JDialog implements ActionListener, AdjustmentListener, MouseListener {

    private MapLayer layer = null;
    private MapInfo map = null;
    private JButton ok = new JButton("OK");
    private JButton update = new JButton("Update");
    private JButton close = new JButton("Close");
    private JTextField minVal = null;
    private JTextField maxVal = null;
    private JScrollBar scrollAlpha = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 255);
    private JLabel labelAlpha = new JLabel();
    private JCheckBox checkVisible = null;
    private JCheckBox checkReversePalette = null;
    private JCheckBox checkScalePalette = null;
    private JScrollBar scrollNonlinearity = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 200);
    private JLabel labelNonlinearity = new JLabel();
    private DecimalFormat df = new DecimalFormat("#0.00");
    private JTextField titleText = null;
    private JButton minValButton = null;
    private JButton maxValButton = null;
    private WhiteboxPluginHost host = null;
    private PaletteImage paletteImage = new PaletteImage();
    private JButton paletteButton = new JButton("...");
    private JButton clipUpperTail = new JButton("Clip");
    private JButton clipLowerTail = new JButton("Clip");
    private JTextField clipAmountLower = null;
    private JTextField clipAmountUpper = null;
    private JCheckBox checkFilled = null;
    private JCheckBox checkOutlined = null;
    private JLabel labelLineThickness = new JLabel();
    private int scrollbarMax = 200;
    private JScrollBar scrollLineThickness = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, scrollbarMax);
    private float minLineThickness = 0.00f;
    private float maxLineThickness = 10.0f;
    private SampleColour sampleColourPanelLine;
    private SampleColour sampleColourPanelLine2;
    private int sampleWidth = 30;
    private int sampleHeight = 15;
    private Color sampleColourLine = new Color(0, 0, 255);
    private SampleColour sampleColourPanelFill;
    private Color sampleColourFill = null;
    private JLabel labelMarkerSize = new JLabel();
    private JScrollBar scrollMarkerSize = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 40);
    private JComboBox dashCombo = new JComboBox();
    private float[][] dashArray = new float[][]{{-1}, {12}, {4}, {12, 4, 12, 4},
    {4, 4, 12, 4}, {16}, {2}, {2, 4}, {4, 12}, {12, 4, 2, 4}};
    private JComboBox markerCombo = new JComboBox();
    private float markerSize;
    private JRadioButton fieldBasedFillColour;
    private JRadioButton uniqueFillColour;
    private JRadioButton fieldBasedLineColour;
    private JRadioButton uniqueLineColour;
    private JPanel valueFieldBox;
    private JPanel paletteBox;
    private JPanel scalePaletteBox;
    private JPanel minBox;
    private JPanel maxBox;
    private JButton viewAttributesTable = new JButton("View Attributes");
    private JComboBox valueFieldCombo;
    private JTabbedPane tabs;
    private Color backColour = new Color(225, 245, 255); //210, 230, 255);
    private JTextField noDataText;
    private JTextField XYUnitsText;
    private JTextField ZUnitsText;
    private boolean updatedFileHeader = false;
    private JScrollBar scrollGeneralizeLevel = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 100);
    private ResourceBundle bundle;

    public LayerProperties(Frame owner, boolean modal, MapLayer layer, MapInfo map) {
        super(owner, modal);
        if (owner != null) {
            Dimension parentSize = owner.getSize();
            Point p = owner.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }

        this.host = (WhiteboxPluginHost) (owner);
        bundle = host.getGuiLabelsBundle();
        this.layer = layer;
        if (layer instanceof RasterLayerInfo) {
            RasterLayerInfo rli = (RasterLayerInfo) layer;
            this.paletteFile = rli.getPaletteFile();
        } else if (layer instanceof VectorLayerInfo) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            this.paletteFile = vli.getPaletteFile();
        } else if (layer instanceof LasLayerInfo) {
            LasLayerInfo lli = (LasLayerInfo) layer;
            this.paletteFile = lli.getPaletteFile();
        }
        this.map = map;
        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        if (layer == null) {
            System.err.println("LayerInfo not set.");
            return;
        }

        setTitle(bundle.getString("LayerProperties") + ": " + layer.getLayerTitle());

        ok = new JButton(bundle.getString("OK"));
        update = new JButton(bundle.getString("UpdateMap"));
        close = new JButton(bundle.getString("Close"));
        clipUpperTail = new JButton(bundle.getString("Clip"));
        clipLowerTail = new JButton(bundle.getString("Clip"));

        // okay and close buttons.
        Box box1 = Box.createHorizontalBox();
        box1.add(Box.createHorizontalStrut(10));
        box1.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        ok.setToolTipText(bundle.getString("SaveChangesAndExit"));
        box1.add(Box.createRigidArea(new Dimension(5, 30)));
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

        JLabel label = null;
        Box mainBox = Box.createVerticalBox();

        if (layer instanceof RasterLayerInfo) {
            RasterLayerInfo rli = (RasterLayerInfo) layer;

            boolean isRGBlayer = false;
            if (rli.getDataScale() == WhiteboxRaster.DataScale.RGB) {
                isRGBlayer = true;
            }

            JPanel titleBox = new JPanel();
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.X_AXIS));
            titleBox.setBackground(Color.white);
            titleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LegendTitle"));
            label.setPreferredSize(new Dimension(180, 24));
            titleBox.add(label);
            titleBox.add(Box.createHorizontalGlue());
            titleText = new JTextField(layer.getLayerTitle(), 20);
            titleText.setMaximumSize(new Dimension(600, 22));
            titleBox.add(titleText);
            titleBox.add(Box.createHorizontalStrut(10));

            minBox = new JPanel();
            minBox.setLayout(new BoxLayout(minBox, BoxLayout.X_AXIS));
            minBox.setBackground(backColour);
            minBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("DisplayMinimum"));
            label.setPreferredSize(new Dimension(180, 24));
            minBox.add(label);
            minBox.add(Box.createHorizontalGlue());
            minVal = new JTextField(Double.toString(rli.getDisplayMinVal()), 15);
            minVal.setHorizontalAlignment(JTextField.RIGHT);
            minVal.setMaximumSize(new Dimension(50, 22));
            minBox.add(minVal);
            minValButton = new JButton(bundle.getString("Reset"));
            minValButton.setActionCommand("resetMinimum");
            minValButton.addActionListener(this);
            minBox.add(minValButton);
            minBox.add(Box.createHorizontalStrut(2));
            clipAmountLower = new JTextField("1.0%", 4);
            clipAmountLower.setHorizontalAlignment(JTextField.RIGHT);
            clipAmountLower.setMaximumSize(new Dimension(50, 22));
            minBox.add(clipAmountLower);
            minBox.add(clipLowerTail);
            clipLowerTail.setActionCommand("clipLowerTail");
            clipLowerTail.addActionListener(this);
            minBox.add(Box.createHorizontalStrut(10));

            maxBox = new JPanel();
            maxBox.setLayout(new BoxLayout(maxBox, BoxLayout.X_AXIS));
            maxBox.setBackground(Color.white);
            maxBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("DisplayMaximum"));
            label.setPreferredSize(new Dimension(180, 24));
            maxBox.add(label);
            maxBox.add(Box.createHorizontalGlue());
            maxVal = new JTextField(Double.toString(rli.getDisplayMaxVal()), 15);
            maxVal.setHorizontalAlignment(JTextField.RIGHT);
            maxVal.setMaximumSize(new Dimension(50, 22));
            maxBox.add(maxVal);
            maxValButton = new JButton(bundle.getString("Reset"));
            maxValButton.setActionCommand("resetMaximum");
            maxValButton.addActionListener(this);
            maxBox.add(maxValButton);
            maxBox.add(Box.createHorizontalStrut(2));
            clipUpperTail.setActionCommand("clipUpperTail");
            clipUpperTail.addActionListener(this);
            clipAmountUpper = new JTextField("1.0%", 4);
            clipAmountUpper.setHorizontalAlignment(JTextField.RIGHT);
            clipAmountUpper.setMaximumSize(new Dimension(50, 22));
            maxBox.add(clipAmountUpper);
            maxBox.add(clipUpperTail);
            maxBox.add(Box.createHorizontalStrut(10));

            JPanel overlayBox = new JPanel();
            overlayBox.setLayout(new BoxLayout(overlayBox, BoxLayout.X_AXIS));
            overlayBox.setBackground(backColour);
            overlayBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("OverlayNumber"));
            label.setPreferredSize(new Dimension(180, 24));
            overlayBox.add(label);
            overlayBox.add(Box.createHorizontalGlue());
            JSpinner spin1 = new JSpinner();
            spin1.setMaximumSize(new Dimension(200, 22));
            SpinnerModel sm
                    = new SpinnerNumberModel(layer.getOverlayNumber(), 0, map.getActiveMapArea().getNumLayers() - 1, 1);
            spin1.setModel(sm);
            overlayBox.add(spin1);
            overlayBox.add(Box.createHorizontalStrut(10));

            paletteBox = new JPanel();
            paletteBox.setLayout(new BoxLayout(paletteBox, BoxLayout.X_AXIS));
            paletteBox.setBackground(Color.white);
            paletteBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("Palette"));
            label.setPreferredSize(new Dimension(180, 24));
            paletteBox.add(label);
            paletteBox.add(Box.createHorizontalGlue());
            paletteImage.initialize(256, 18, paletteFile, rli.isPaletteReversed(), PaletteImage.HORIZONTAL_ORIENTATION);
            paletteImage.setNonlinearity(rli.getNonlinearity());
            paletteImage.setMinimumSize(new Dimension(50, 20));
            paletteBox.add(paletteImage);
            paletteButton.setActionCommand("changePalette");
            paletteButton.addActionListener(this);
            paletteBox.add(paletteButton);
            paletteBox.add(Box.createHorizontalStrut(10));

            JPanel reversePaletteBox = new JPanel();
            reversePaletteBox.setLayout(new BoxLayout(reversePaletteBox, BoxLayout.X_AXIS));
            reversePaletteBox.setBackground(backColour);
            reversePaletteBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("IsPaletteReversed"));
            label.setPreferredSize(new Dimension(180, 24));
            reversePaletteBox.add(label);
            reversePaletteBox.add(Box.createHorizontalGlue());
            checkReversePalette = new JCheckBox("");
            checkReversePalette.setOpaque(false);
            checkReversePalette.setSelected(rli.isPaletteReversed());
            checkReversePalette.addActionListener(this);
            checkReversePalette.setActionCommand("reversePalette");
            reversePaletteBox.add(checkReversePalette);
            reversePaletteBox.add(Box.createHorizontalStrut(10));

            scalePaletteBox = new JPanel();
            scalePaletteBox.setLayout(new BoxLayout(scalePaletteBox, BoxLayout.X_AXIS));
            scalePaletteBox.setBackground(Color.white);
            scalePaletteBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("StretchPalette"));
            label.setPreferredSize(new Dimension(180, 24));
            scalePaletteBox.add(label);
            scalePaletteBox.add(Box.createHorizontalGlue());
            checkScalePalette = new JCheckBox("");
            checkScalePalette.setOpaque(false);
            if (rli.getDataScale() == WhiteboxRaster.DataScale.CONTINUOUS) {
                checkScalePalette.setSelected(true);
            } else {
                checkScalePalette.setSelected(false);
            }
            scalePaletteBox.add(checkScalePalette);
            scalePaletteBox.add(Box.createHorizontalStrut(10));

            JPanel nonlinearityBox = new JPanel();
            nonlinearityBox.setLayout(new BoxLayout(nonlinearityBox, BoxLayout.X_AXIS));
            nonlinearityBox.setBackground(backColour);
            nonlinearityBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("PaletteNonlinearity") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            nonlinearityBox.add(label);
            nonlinearityBox.add(Box.createHorizontalGlue());
            String str = df.format(rli.getNonlinearity());
            labelNonlinearity.setText("Gamma: " + str);
            nonlinearityBox.add(labelNonlinearity);
            nonlinearityBox.add(Box.createHorizontalStrut(10));
            scrollNonlinearity.setValue((int) (rli.getNonlinearity() * 10));
            scrollNonlinearity.setMaximumSize(new Dimension(200, 22));
            scrollNonlinearity.addAdjustmentListener(this);
            nonlinearityBox.add(scrollNonlinearity);
            nonlinearityBox.add(Box.createHorizontalStrut(10));

            JPanel visibleBox = new JPanel();
            visibleBox.setLayout(new BoxLayout(visibleBox, BoxLayout.X_AXIS));
            visibleBox.setBackground(Color.white);
            visibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("IsLayerVisible"));
            label.setPreferredSize(new Dimension(180, 24));
            visibleBox.add(label);
            visibleBox.add(Box.createHorizontalGlue());
            checkVisible = new JCheckBox("");
            checkVisible.setOpaque(false);
            checkVisible.setSelected(rli.isVisible());
            visibleBox.add(checkVisible);
            visibleBox.add(Box.createHorizontalStrut(10));

            JPanel alphaBox = new JPanel();
            alphaBox.setLayout(new BoxLayout(alphaBox, BoxLayout.X_AXIS));
            alphaBox.setBackground(backColour);
            alphaBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("Opacity"));
            label.setPreferredSize(new Dimension(180, 24));
            alphaBox.add(label);
            alphaBox.add(Box.createHorizontalGlue());
            labelAlpha.setText("Alpha: " + Integer.toString(rli.getAlpha()));
            alphaBox.add(labelAlpha);
            alphaBox.add(Box.createHorizontalStrut(10));
            scrollAlpha.setValue(rli.getAlpha());
            scrollAlpha.setMaximumSize(new Dimension(200, 22));
            scrollAlpha.addAdjustmentListener(this);
            alphaBox.add(scrollAlpha);
            alphaBox.add(Box.createHorizontalStrut(10));

            if (!isRGBlayer) {
                mainBox.add(titleBox);
                mainBox.add(minBox);
                mainBox.add(maxBox);
                mainBox.add(overlayBox);
                mainBox.add(paletteBox);
                mainBox.add(reversePaletteBox);
                mainBox.add(scalePaletteBox);
                mainBox.add(nonlinearityBox);
                mainBox.add(visibleBox);
                mainBox.add(alphaBox);
            } else {
                mainBox.add(titleBox);
                mainBox.add(overlayBox);
                mainBox.add(visibleBox);
                mainBox.add(alphaBox);
                mainBox.add(Box.createVerticalStrut(160));
            }

        } else if (layer instanceof VectorLayerInfo) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            ShapeType st = vli.getShapeType();

            JPanel titleBox = new JPanel();
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.X_AXIS));
            titleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LegendTitle"));
            label.setPreferredSize(new Dimension(180, 24));
            titleBox.add(label);
            titleBox.add(Box.createHorizontalGlue());
            titleText = new JTextField(layer.getLayerTitle(), 20);
            titleText.setMaximumSize(new Dimension(600, 22));
            titleBox.add(titleText);
            titleBox.add(Box.createHorizontalStrut(10));

            JPanel overlayBox = new JPanel();
            overlayBox.setLayout(new BoxLayout(overlayBox, BoxLayout.X_AXIS));
            overlayBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("OverlayNumber"));
            label.setPreferredSize(new Dimension(180, 24));
            overlayBox.add(label);
            overlayBox.add(Box.createHorizontalGlue());
            JSpinner spin1 = new JSpinner();
            spin1.setMaximumSize(new Dimension(200, 22));
            SpinnerModel sm
                    = new SpinnerNumberModel(layer.getOverlayNumber(), 0, map.getActiveMapArea().getNumLayers() - 1, 1);
            spin1.setModel(sm);
            overlayBox.add(spin1);
            overlayBox.add(Box.createHorizontalStrut(10));

            JPanel visibleBox = new JPanel();
            visibleBox.setLayout(new BoxLayout(visibleBox, BoxLayout.X_AXIS));
            visibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("IsLayerVisible"));
            label.setPreferredSize(new Dimension(180, 24));
            visibleBox.add(label);
            visibleBox.add(Box.createHorizontalGlue());
            checkVisible = new JCheckBox("");
            checkVisible.setOpaque(false);
            checkVisible.setSelected(vli.isVisible());
            visibleBox.add(checkVisible);
            visibleBox.add(Box.createHorizontalStrut(10));

            JPanel filledBox = new JPanel();
            filledBox.setLayout(new BoxLayout(filledBox, BoxLayout.X_AXIS));
            filledBox.add(Box.createHorizontalStrut(10));
            if (st == ShapeType.POLYGON || st == ShapeType.POLYGONM
                    || st == ShapeType.POLYGONZ || st == ShapeType.MULTIPATCH) {
                label = new JLabel(bundle.getString("FillPolygons"));
            } else {
                label = new JLabel(bundle.getString("FillPoints"));
            }
            label.setPreferredSize(new Dimension(180, 24));
            filledBox.add(label);
            filledBox.add(Box.createHorizontalGlue());
            checkFilled = new JCheckBox("");
            checkFilled.setOpaque(false);
            checkFilled.setSelected(vli.isFilled());
            filledBox.add(checkFilled);
            filledBox.add(Box.createHorizontalStrut(10));

            JPanel outlinedBox = new JPanel();
            outlinedBox.setLayout(new BoxLayout(outlinedBox, BoxLayout.X_AXIS));
            outlinedBox.add(Box.createHorizontalStrut(10));
            if (st == ShapeType.POLYGON || st == ShapeType.POLYGONM
                    || st == ShapeType.POLYGONZ || st == ShapeType.MULTIPATCH) {
                label = new JLabel(bundle.getString("OutlinePolygons"));
            } else {
                label = new JLabel(bundle.getString("OutlinePoints"));
            }
            label.setPreferredSize(new Dimension(180, 24));
            outlinedBox.add(label);
            outlinedBox.add(Box.createHorizontalGlue());
            checkOutlined = new JCheckBox("");
            checkOutlined.setOpaque(false);
            checkOutlined.setSelected(vli.isOutlined());
            outlinedBox.add(checkOutlined);
            outlinedBox.add(Box.createHorizontalStrut(10));

            JPanel lineThicknessBox = new JPanel();
            lineThicknessBox.setLayout(new BoxLayout(lineThicknessBox, BoxLayout.X_AXIS));
            lineThicknessBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LineThickness") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            lineThicknessBox.add(label);
            lineThicknessBox.add(Box.createHorizontalGlue());
            labelLineThickness.setText(bundle.getString("Value") + ": " + df.format(vli.getLineThickness()));
            lineThicknessBox.add(labelLineThickness);
            lineThicknessBox.add(Box.createHorizontalStrut(10));
            scrollLineThickness.setValue((int) ((vli.getLineThickness() - minLineThickness) / (maxLineThickness - minLineThickness) * scrollbarMax));
            scrollLineThickness.setMaximumSize(new Dimension(200, 22));
            scrollLineThickness.addAdjustmentListener(this);
            lineThicknessBox.add(scrollLineThickness);
            lineThicknessBox.add(Box.createHorizontalStrut(10));

            JPanel lineColourBox = new JPanel();
            lineColourBox.setLayout(new BoxLayout(lineColourBox, BoxLayout.X_AXIS));
            lineColourBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LineColor") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            lineColourBox.add(label);
            lineColourBox.add(Box.createHorizontalGlue());
            sampleColourLine = vli.getLineColour();
            sampleColourPanelLine = new SampleColour(sampleWidth, sampleHeight, sampleColourLine);
            sampleColourPanelLine.setToolTipText(bundle.getString("ClickToSelectColor"));
            sampleColourPanelLine.addMouseListener(this);
            lineColourBox.add(sampleColourPanelLine);
            lineColourBox.add(Box.createHorizontalStrut(10));

            JPanel markerBox = new JPanel();
            markerBox.setLayout(new BoxLayout(markerBox, BoxLayout.X_AXIS));
            markerBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("MarkerSize") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            markerBox.add(label);
            markerBox.add(Box.createHorizontalGlue());
            labelMarkerSize.setText(bundle.getString("Value") + ": " + Float.toString(vli.getMarkerSize()));
            markerSize = vli.getMarkerSize();
            markerBox.add(labelMarkerSize);
            markerBox.add(Box.createHorizontalStrut(10));
            scrollMarkerSize.setValue((int) (vli.getMarkerSize()));
            scrollMarkerSize.setMaximumSize(new Dimension(200, 22));
            scrollMarkerSize.addAdjustmentListener(this);
            markerBox.add(scrollMarkerSize);
            markerBox.add(Box.createHorizontalStrut(10));

            JPanel dashComboBox = new JPanel();
            dashComboBox.setLayout(new BoxLayout(dashComboBox, BoxLayout.X_AXIS));
            dashComboBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LineStyle") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            dashComboBox.add(label);
            dashComboBox.add(Box.createHorizontalGlue());
            Integer[] intArray = new Integer[dashArray.length];
            for (int k = 0; k < dashArray.length; k++) {
                intArray[k] = new Integer(k);
            }
            dashCombo = new JComboBox(intArray);
            initializeLineStyle();
            dashComboBox.add(dashCombo);
            dashComboBox.add(Box.createHorizontalStrut(10));

            JPanel fillColourBox = new JPanel();
            fillColourBox.setLayout(new BoxLayout(fillColourBox, BoxLayout.X_AXIS));
            fillColourBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("FillColor") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            fillColourBox.add(label);
            fillColourBox.add(Box.createHorizontalGlue());
            sampleColourFill = vli.getFillColour();
            sampleColourPanelFill = new SampleColour(sampleWidth, sampleHeight, sampleColourFill);
            sampleColourPanelFill.setToolTipText(bundle.getString("ClickToSelectColor"));
            sampleColourPanelFill.addMouseListener(this);
            uniqueFillColour = new JRadioButton(bundle.getString("UseUniqueColor"), true);
            uniqueFillColour.setOpaque(false);
            fillColourBox.add(uniqueFillColour);
            fillColourBox.add(Box.createHorizontalStrut(5));
            fillColourBox.add(sampleColourPanelFill);
            fillColourBox.add(Box.createHorizontalStrut(10));
            fieldBasedFillColour = new JRadioButton(bundle.getString("FillBasedOnAttribute"), false);
            fieldBasedFillColour.setOpaque(false);
            fillColourBox.add(fieldBasedFillColour);
            fillColourBox.add(Box.createHorizontalStrut(10));

            JPanel markerComboBox = new JPanel();
            markerComboBox.setLayout(new BoxLayout(markerComboBox, BoxLayout.X_AXIS));
            markerComboBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("MarkerStyle") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            markerComboBox.add(label);
            markerComboBox.add(Box.createHorizontalGlue());
            initializeMarkerStyle();
            markerComboBox.add(markerCombo);
            markerComboBox.add(Box.createHorizontalStrut(10));

            JPanel alphaBox = new JPanel();
            alphaBox.setLayout(new BoxLayout(alphaBox, BoxLayout.X_AXIS));
            alphaBox.setBackground(backColour);
            alphaBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("Opacity"));
            label.setPreferredSize(new Dimension(180, 24));
            alphaBox.add(label);
            alphaBox.add(Box.createHorizontalGlue());
            labelAlpha.setText("Alpha: " + Integer.toString(vli.getAlpha()));
            alphaBox.add(labelAlpha);
            alphaBox.add(Box.createHorizontalStrut(10));
            scrollAlpha.setValue(vli.getAlpha());
            scrollAlpha.setMaximumSize(new Dimension(200, 22));
            scrollAlpha.addAdjustmentListener(this);
            alphaBox.add(scrollAlpha);
            alphaBox.add(Box.createHorizontalStrut(10));

            valueFieldBox = new JPanel();
            valueFieldBox.setLayout(new BoxLayout(valueFieldBox, BoxLayout.X_AXIS));
            valueFieldBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("ChooseAnAttribute") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            valueFieldBox.add(label);
            valueFieldBox.add(Box.createHorizontalGlue());
            String[] fields = vli.getAttributeTableFields();

            if (vli.getShapeType().getDimension() == ShapeTypeDimension.Z) {
                String[] fields2 = new String[fields.length + 2];
                fields2[0] = "Feature Z Value";
                fields2[1] = "Feature Measure";
                System.arraycopy(fields, 0, fields2, 2, fields.length);
                valueFieldCombo = new JComboBox(fields2);
                if (!vli.getFillAttribute().equals("")) {
                    valueFieldCombo.setSelectedItem(vli.getFillAttribute());
                }
            } else if (vli.getShapeType().getDimension() == ShapeTypeDimension.M) {
                String[] fields2 = new String[fields.length + 1];
                fields2[0] = "Feature Measure";
                System.arraycopy(fields, 0, fields2, 1, fields.length);
                valueFieldCombo = new JComboBox(fields2);
                if (!vli.getFillAttribute().equals("")) {
                    valueFieldCombo.setSelectedItem(vli.getFillAttribute());
                }
            } else { // XY
                valueFieldCombo = new JComboBox(fields);
                if (!vli.getFillAttribute().equals("")) {
                    valueFieldCombo.setSelectedItem(vli.getFillAttribute());
                }
            }
            valueFieldBox.add(Box.createHorizontalStrut(10));
            valueFieldBox.add(valueFieldCombo);
            valueFieldBox.add(Box.createHorizontalStrut(5));
            viewAttributesTable.setActionCommand("viewAttributesTable");
            viewAttributesTable.addActionListener(this);
            viewAttributesTable.setToolTipText(bundle.getString("ViewAttributeTable"));
            valueFieldBox.add(viewAttributesTable);
            valueFieldBox.add(Box.createHorizontalStrut(10));

            ButtonGroup group = new ButtonGroup();
            group.add(uniqueFillColour);
            group.add(fieldBasedFillColour);
            //Register a listener for the radio buttons.
            uniqueFillColour.setActionCommand("uniqueFill");
            fieldBasedFillColour.setActionCommand("fieldBasedFill");
            uniqueFillColour.addActionListener(this);
            fieldBasedFillColour.addActionListener(this);

            JPanel lineColourBox2 = new JPanel();
            lineColourBox2.setLayout(new BoxLayout(lineColourBox2, BoxLayout.X_AXIS));
            lineColourBox2.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LineColor") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            lineColourBox2.add(label);
            lineColourBox2.add(Box.createHorizontalGlue());
            sampleColourLine = vli.getLineColour();
            sampleColourPanelLine2 = new SampleColour(sampleWidth, sampleHeight, sampleColourLine);
            sampleColourPanelLine2.setToolTipText(bundle.getString("ClickToSelectColor"));
            sampleColourPanelLine2.addMouseListener(this);
            uniqueLineColour = new JRadioButton(bundle.getString("UseUniqueColor"), true);
            uniqueLineColour.setOpaque(false);
            lineColourBox2.add(uniqueLineColour);
            lineColourBox2.add(Box.createHorizontalStrut(5));
            lineColourBox2.add(sampleColourPanelLine2);
            lineColourBox2.add(Box.createHorizontalStrut(10));
            fieldBasedLineColour = new JRadioButton(bundle.getString("ColorBasedOnAttribute"), false);
            fieldBasedLineColour.setOpaque(false);
            lineColourBox2.add(fieldBasedLineColour);
            lineColourBox2.add(Box.createHorizontalStrut(10));

            ButtonGroup group2 = new ButtonGroup();
            group2.add(uniqueLineColour);
            group2.add(fieldBasedLineColour);
            //Register a listener for the radio buttons.
            uniqueLineColour.setActionCommand("uniqueLine");
            fieldBasedLineColour.setActionCommand("fieldBasedLine");
            uniqueLineColour.addActionListener(this);
            fieldBasedLineColour.addActionListener(this);

            paletteBox = new JPanel();
            paletteBox.setLayout(new BoxLayout(paletteBox, BoxLayout.X_AXIS));
            paletteBox.setBackground(Color.white);
            paletteBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("ChoosePalette"));
            label.setPreferredSize(new Dimension(180, 24));
            paletteBox.add(label);
            paletteBox.add(Box.createHorizontalGlue());
            paletteImage.initialize(256, 18, paletteFile, false, PaletteImage.HORIZONTAL_ORIENTATION);
            paletteImage.setMinimumSize(new Dimension(50, 20));
            paletteBox.add(paletteImage);
            paletteButton.setActionCommand("changePalette");
            paletteButton.addActionListener(this);
            paletteBox.add(paletteButton);
            paletteBox.add(Box.createHorizontalStrut(10));

            scalePaletteBox = new JPanel();
            scalePaletteBox.setLayout(new BoxLayout(scalePaletteBox, BoxLayout.X_AXIS));
            scalePaletteBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("StretchPalette"));
            label.setPreferredSize(new Dimension(180, 24));
            scalePaletteBox.add(label);
            scalePaletteBox.add(Box.createHorizontalGlue());
            checkScalePalette = new JCheckBox("");
            checkScalePalette.setOpaque(false);
            if (vli.isPaletteScaled()) {
                checkScalePalette.setSelected(true);
            } else {
                checkScalePalette.setSelected(false);
            }
            checkScalePalette.addActionListener(this);
            checkScalePalette.setActionCommand("scalePalette");
            scalePaletteBox.add(checkScalePalette);
            scalePaletteBox.add(Box.createHorizontalStrut(10));

            final JPanel nonlinearityBox = new JPanel();
            nonlinearityBox.setLayout(new BoxLayout(nonlinearityBox, BoxLayout.X_AXIS));
            nonlinearityBox.setBackground(backColour);
            nonlinearityBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("PaletteNonlinearity") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            nonlinearityBox.add(label);
            nonlinearityBox.add(Box.createHorizontalGlue());
            String str = df.format(vli.getNonlinearity());
            labelNonlinearity.setText("Gamma: " + str);
            nonlinearityBox.add(labelNonlinearity);
            nonlinearityBox.add(Box.createHorizontalStrut(10));
            scrollNonlinearity.setValue((int) (vli.getNonlinearity() * 10));
            scrollNonlinearity.setMaximumSize(new Dimension(200, 22));
            scrollNonlinearity.addAdjustmentListener(this);
            nonlinearityBox.add(scrollNonlinearity);
            nonlinearityBox.add(Box.createHorizontalStrut(10));
            nonlinearityBox.setVisible(checkScalePalette.isSelected());

            checkScalePalette.addActionListener((ActionEvent e) -> {
                if (checkScalePalette.isSelected()) {
                    nonlinearityBox.setVisible(true);
                } else {
                    nonlinearityBox.setVisible(false);
                }
            });

            ShapeType shapeType = vli.getShapeType();
            if (shapeType == ShapeType.POLYLINE || shapeType == ShapeType.POLYLINEM
                    || shapeType == ShapeType.POLYLINEZ) {
                if (vli.isOutlinedWithOneColour()) {
                    uniqueLineColour.setSelected(true);
                    fieldBasedLineColour.setSelected(false);
                    valueFieldBox.setVisible(false);
                    paletteBox.setVisible(false);
                    scalePaletteBox.setVisible(false);
                } else {
                    uniqueLineColour.setSelected(false);
                    fieldBasedLineColour.setSelected(true);
                    valueFieldBox.setVisible(true);
                    paletteBox.setVisible(true);
                    scalePaletteBox.setVisible(true);
                }

            } else {
                if (vli.isFilledWithOneColour()) {
                    uniqueFillColour.setSelected(true);
                    fieldBasedFillColour.setSelected(false);
                    valueFieldBox.setVisible(false);
                    paletteBox.setVisible(false);
                    scalePaletteBox.setVisible(false);
                } else {
                    uniqueFillColour.setSelected(false);
                    fieldBasedFillColour.setSelected(true);
                    valueFieldBox.setVisible(true);
                    paletteBox.setVisible(true);
                    scalePaletteBox.setVisible(true);
                }

            }

            JPanel generalizedBox = new JPanel();
            generalizedBox.setLayout(new BoxLayout(generalizedBox, BoxLayout.X_AXIS));
            generalizedBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("CartographicGeneralization"));
            generalizedBox.setToolTipText(bundle.getString("CartographicGeneralizationTooltip"));
            label.setPreferredSize(new Dimension(180, 24));
            generalizedBox.add(label);
            generalizedBox.add(Box.createHorizontalGlue());
            generalizedBox.add(new JLabel(bundle.getString("Low")));
            generalizedBox.add(Box.createHorizontalStrut(5));
            scrollGeneralizeLevel.setValue((int) (vli.getCartographicGeneralizationLevel() / 5.0 * 100));
            scrollGeneralizeLevel.setMaximumSize(new Dimension(200, 22));
            generalizedBox.add(scrollGeneralizeLevel);
            generalizedBox.add(Box.createHorizontalStrut(5));
            generalizedBox.add(new JLabel(bundle.getString("High")));
            generalizedBox.add(Box.createHorizontalStrut(10));

            if (st.getBaseType() == ShapeType.POLYGON || st == ShapeType.MULTIPATCH) {
                titleBox.setBackground(Color.white);
                mainBox.add(titleBox);
                overlayBox.setBackground(backColour);
                mainBox.add(overlayBox);
                visibleBox.setBackground(Color.white);
                mainBox.add(visibleBox);
                outlinedBox.setBackground(backColour);
                mainBox.add(outlinedBox);
                lineColourBox.setBackground(Color.white);
                mainBox.add(lineColourBox);
                lineThicknessBox.setBackground(backColour);
                mainBox.add(lineThicknessBox);
                dashComboBox.setBackground(Color.white);
                mainBox.add(dashComboBox);
                filledBox.setBackground(backColour);
                mainBox.add(filledBox);
                fillColourBox.setBackground(Color.white);
                mainBox.add(fillColourBox);
                valueFieldBox.setBackground(Color.white);
                mainBox.add(valueFieldBox);
                paletteBox.setBackground(Color.white);
                mainBox.add(paletteBox);
                scalePaletteBox.setBackground(Color.white);
                mainBox.add(scalePaletteBox);
                nonlinearityBox.setBackground(Color.white);
                mainBox.add(nonlinearityBox);

                alphaBox.setBackground(backColour);
                mainBox.add(alphaBox);
                generalizedBox.setBackground(Color.white);
                mainBox.add(generalizedBox);
                mainBox.add(Box.createVerticalStrut(80));
            } else if (st.getBaseType() == ShapeType.POINT
                    || st.getBaseType() == ShapeType.MULTIPOINT) {
                titleBox.setBackground(Color.white);
                mainBox.add(titleBox);
                overlayBox.setBackground(backColour);
                mainBox.add(overlayBox);
                visibleBox.setBackground(Color.white);
                mainBox.add(visibleBox);
                outlinedBox.setBackground(backColour);
                mainBox.add(outlinedBox);
                lineColourBox.setBackground(Color.white);
                mainBox.add(lineColourBox);
                lineThicknessBox.setBackground(backColour);
                mainBox.add(lineThicknessBox);
                filledBox.setBackground(Color.white);
                mainBox.add(filledBox);
                fillColourBox.setBackground(backColour);
                mainBox.add(fillColourBox);
                valueFieldBox.setBackground(backColour);
                mainBox.add(valueFieldBox);
                paletteBox.setBackground(backColour);
                mainBox.add(paletteBox);
                scalePaletteBox.setBackground(backColour);
                mainBox.add(scalePaletteBox);
                nonlinearityBox.setBackground(backColour);
                mainBox.add(nonlinearityBox);
                markerComboBox.setBackground(Color.white);
                mainBox.add(markerComboBox);
                markerBox.setBackground(backColour);
                mainBox.add(markerBox);
                alphaBox.setBackground(Color.white);
                mainBox.add(alphaBox);
                mainBox.add(Box.createVerticalStrut(80));

            } else if (st == ShapeType.POLYLINE || st == ShapeType.POLYLINEM
                    | st == ShapeType.POLYLINEZ) {
                titleBox.setBackground(Color.white);
                mainBox.add(titleBox);
                overlayBox.setBackground(backColour);
                mainBox.add(overlayBox);
                visibleBox.setBackground(Color.white);
                mainBox.add(visibleBox);
                lineThicknessBox.setBackground(backColour);
                mainBox.add(lineThicknessBox);
                dashComboBox.setBackground(Color.white);
                mainBox.add(dashComboBox);
                lineColourBox2.setBackground(backColour);
                mainBox.add(lineColourBox2);
                valueFieldBox.setBackground(backColour);
                mainBox.add(valueFieldBox);
                paletteBox.setBackground(backColour);
                mainBox.add(paletteBox);
                scalePaletteBox.setBackground(backColour);
                mainBox.add(scalePaletteBox);
                nonlinearityBox.setBackground(backColour);
                mainBox.add(nonlinearityBox);
                alphaBox.setBackground(Color.white);
                mainBox.add(alphaBox);
                generalizedBox.setBackground(backColour);
                mainBox.add(generalizedBox);
                mainBox.add(Box.createVerticalStrut(130));
            }
        } else if (layer instanceof LasLayerInfo) {
            LasLayerInfo lli = (LasLayerInfo) layer;

            JPanel titleBox = new JPanel();
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.X_AXIS));
            titleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("LegendTitle"));
            label.setPreferredSize(new Dimension(180, 24));
            titleBox.add(label);
            titleBox.add(Box.createHorizontalGlue());
            titleText = new JTextField(layer.getLayerTitle(), 20);
            titleText.setMaximumSize(new Dimension(600, 22));
            titleBox.add(titleText);
            titleBox.add(Box.createHorizontalStrut(10));

            JPanel overlayBox = new JPanel();
            overlayBox.setLayout(new BoxLayout(overlayBox, BoxLayout.X_AXIS));
            overlayBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("OverlayNumber"));
            label.setPreferredSize(new Dimension(180, 24));
            overlayBox.add(label);
            overlayBox.add(Box.createHorizontalGlue());
            JSpinner spin1 = new JSpinner();
            spin1.setMaximumSize(new Dimension(200, 22));
            SpinnerModel sm
                    = new SpinnerNumberModel(layer.getOverlayNumber(), 0, map.getActiveMapArea().getNumLayers() - 1, 1);
            spin1.setModel(sm);
            overlayBox.add(spin1);
            overlayBox.add(Box.createHorizontalStrut(10));

            JPanel visibleBox = new JPanel();
            visibleBox.setLayout(new BoxLayout(visibleBox, BoxLayout.X_AXIS));
            visibleBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("IsLayerVisible"));
            label.setPreferredSize(new Dimension(180, 24));
            visibleBox.add(label);
            visibleBox.add(Box.createHorizontalGlue());
            checkVisible = new JCheckBox("");
            checkVisible.setOpaque(false);
            checkVisible.setSelected(lli.isVisible());
            visibleBox.add(checkVisible);
            visibleBox.add(Box.createHorizontalStrut(10));

            JPanel markerBox = new JPanel();
            markerBox.setLayout(new BoxLayout(markerBox, BoxLayout.X_AXIS));
            markerBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("MarkerSize") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            markerBox.add(label);
            markerBox.add(Box.createHorizontalGlue());
            markerSize = lli.getMarkerSize();
            labelMarkerSize.setText(bundle.getString("Value") + ": " + Float.toString(markerSize));
            markerBox.add(labelMarkerSize);
            markerBox.add(Box.createHorizontalStrut(10));
            scrollMarkerSize = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 20);
            scrollMarkerSize.setValue((int) (lli.getMarkerSize() * 2));
            scrollMarkerSize.setMaximumSize(new Dimension(200, 22));
            scrollMarkerSize.addAdjustmentListener(this);
            markerBox.add(scrollMarkerSize);
            markerBox.add(Box.createHorizontalStrut(10));

            minBox = new JPanel();
            minBox.setLayout(new BoxLayout(minBox, BoxLayout.X_AXIS));
            minBox.setBackground(backColour);
            minBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("DisplayMinimum"));
            label.setPreferredSize(new Dimension(180, 24));
            minBox.add(label);
            minBox.add(Box.createHorizontalGlue());
            minVal = new JTextField(Double.toString(lli.getDisplayMinValue()), 15);
            minVal.setHorizontalAlignment(JTextField.RIGHT);
            minVal.setMaximumSize(new Dimension(50, 22));
            minBox.add(minVal);
            minValButton = new JButton(bundle.getString("Reset"));
            minValButton.addActionListener(
                    ae -> {
                        minVal.setText(String.valueOf(lli.getMinimumValue()));
                    }
            );
            minBox.add(minValButton);
            minBox.add(Box.createHorizontalStrut(2));
            clipAmountLower = new JTextField("2.0%", 4);
            clipAmountLower.setHorizontalAlignment(JTextField.RIGHT);
            clipAmountLower.setMaximumSize(new Dimension(50, 22));
            minBox.add(clipAmountLower);
            minBox.add(clipLowerTail);
            clipLowerTail.addActionListener(
                    ae -> {
                        double clipAmount = Double.parseDouble(clipAmountLower.getText().replace("%", ""));
                        lli.clipLowerTailForDisplayMinimum(clipAmount);
                        minVal.setText(String.valueOf(lli.getDisplayMinValue()));
                    }
            );
            minBox.add(Box.createHorizontalStrut(10));

            maxBox = new JPanel();
            maxBox.setLayout(new BoxLayout(maxBox, BoxLayout.X_AXIS));
            maxBox.setBackground(Color.white);
            maxBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("DisplayMaximum"));
            label.setPreferredSize(new Dimension(180, 24));
            maxBox.add(label);
            maxBox.add(Box.createHorizontalGlue());
            maxVal = new JTextField(Double.toString(lli.getDisplayMaxValue()), 15);
            maxVal.setHorizontalAlignment(JTextField.RIGHT);
            maxVal.setMaximumSize(new Dimension(50, 22));
            maxBox.add(maxVal);
            maxValButton = new JButton(bundle.getString("Reset"));
            maxValButton.addActionListener(
                    ae -> {
                        maxVal.setText(String.valueOf(lli.getMaximumValue()));
                    }
            );
            maxBox.add(maxValButton);
            maxBox.add(Box.createHorizontalStrut(2));
            clipAmountUpper = new JTextField("2.0%", 4);
            clipAmountUpper.setHorizontalAlignment(JTextField.RIGHT);
            clipAmountUpper.setMaximumSize(new Dimension(50, 22));
            clipUpperTail.addActionListener(
                    ae -> {
                        double clipAmount = Double.parseDouble(clipAmountUpper.getText().replace("%", ""));
                        lli.clipUpperTailForDisplayMaximum(clipAmount);
                        maxVal.setText(String.valueOf(lli.getDisplayMaxValue()));
                    }
            );
            maxBox.add(clipAmountUpper);
            maxBox.add(clipUpperTail);
            maxBox.add(Box.createHorizontalStrut(10));

            JPanel fillColourBox = new JPanel();
            fillColourBox.setLayout(new BoxLayout(fillColourBox, BoxLayout.X_AXIS));
            fillColourBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("FillColor") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            fillColourBox.add(label);
            fillColourBox.add(Box.createHorizontalGlue());
            sampleColourFill = lli.getFillColour();
            sampleColourPanelFill = new SampleColour(sampleWidth, sampleHeight, sampleColourFill);
            sampleColourPanelFill.setToolTipText(bundle.getString("ClickToSelectColor"));
            sampleColourPanelFill.addMouseListener(this);
            uniqueFillColour = new JRadioButton(bundle.getString("UseUniqueColor"), lli.isFilledWithOneColour());
            uniqueFillColour.setOpaque(false);
            fillColourBox.add(uniqueFillColour);
            fillColourBox.add(Box.createHorizontalStrut(5));
            fillColourBox.add(sampleColourPanelFill);
            fillColourBox.add(Box.createHorizontalStrut(10));
            fieldBasedFillColour = new JRadioButton(bundle.getString("FillBasedOnAttribute"), !lli.isFilledWithOneColour());
            fieldBasedFillColour.setOpaque(false);
            fillColourBox.add(fieldBasedFillColour);
            fillColourBox.add(Box.createHorizontalStrut(10));

            JPanel alphaBox = new JPanel();
            alphaBox.setLayout(new BoxLayout(alphaBox, BoxLayout.X_AXIS));
            alphaBox.setBackground(backColour);
            alphaBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("Opacity"));
            label.setPreferredSize(new Dimension(180, 24));
            alphaBox.add(label);
            alphaBox.add(Box.createHorizontalGlue());
            labelAlpha.setText("Alpha: " + Integer.toString(lli.getAlpha()));
            alphaBox.add(labelAlpha);
            alphaBox.add(Box.createHorizontalStrut(10));
            scrollAlpha.setValue(lli.getAlpha());
            scrollAlpha.setMaximumSize(new Dimension(200, 22));
            scrollAlpha.addAdjustmentListener(this);
            alphaBox.add(scrollAlpha);
            alphaBox.add(Box.createHorizontalStrut(10));

            valueFieldBox = new JPanel();
            valueFieldBox.setLayout(new BoxLayout(valueFieldBox, BoxLayout.X_AXIS));
            valueFieldBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("ChooseAnAttribute") + ":");
            label.setPreferredSize(new Dimension(180, 24));
            valueFieldBox.add(label);
            valueFieldBox.add(Box.createHorizontalGlue());
            String[] fields = {"Elevation (z)", "Intensity", "Class", "Scan Angle", "GPS Time"};
            valueFieldCombo = new JComboBox(fields);
            valueFieldCombo.addItemListener(il -> {
                // If the fill criterion is different, we need to update the min and max values
                String fc = (String)valueFieldCombo.getSelectedItem();
                lli.updateMinAndMaxValsForCriterion(fc);
                minVal.setText(Double.toString(lli.getDisplayMinValue()));
                maxVal.setText(Double.toString(lli.getDisplayMaxValue()));
            });

            valueFieldBox.add(Box.createHorizontalStrut(10));
            valueFieldBox.add(valueFieldCombo);
            valueFieldBox.add(Box.createHorizontalStrut(5));
            //valueFieldBox.add(viewAttributesTable);
            valueFieldBox.add(Box.createHorizontalStrut(10));
            
            String fillCriterion = lli.getFillCriterion().toLowerCase();
            if (fillCriterion.contains("z")) {
                valueFieldCombo.setSelectedItem("Elevation (z)");
            } else if (fillCriterion.contains("int")) {
                valueFieldCombo.setSelectedItem("Intensity");
            } else if (fillCriterion.contains("class")) {
                valueFieldCombo.setSelectedItem("Class");
            } else if (fillCriterion.contains("scan")) {
                valueFieldCombo.setSelectedItem("Scan Angle");
            } else if (fillCriterion.contains("time")) {
                valueFieldCombo.setSelectedItem("GPS Time");
            }

            ButtonGroup group = new ButtonGroup();
            group.add(uniqueFillColour);
            group.add(fieldBasedFillColour);
            //Register a listener for the radio buttons.
            uniqueFillColour.addActionListener(
                    ae -> {
                        if (uniqueFillColour.isSelected()) {
                            valueFieldBox.setVisible(false);
                            paletteBox.setVisible(false);
                            minBox.setVisible(false);
                            maxBox.setVisible(false);
                        } else {
                            valueFieldBox.setVisible(true);
                            paletteBox.setVisible(true);
                            minBox.setVisible(true);
                            maxBox.setVisible(true);
                        }
                    }
            );
            fieldBasedFillColour.addActionListener(
                    ae -> {
                        if (uniqueFillColour.isSelected()) {
                            valueFieldBox.setVisible(false);
                            paletteBox.setVisible(false);
                            minBox.setVisible(false);
                            maxBox.setVisible(false);
                        } else {
                            valueFieldBox.setVisible(true);
                            paletteBox.setVisible(true);
                            minBox.setVisible(true);
                            maxBox.setVisible(true);
                        }
                    }
            );

            paletteBox = new JPanel();
            paletteBox.setLayout(new BoxLayout(paletteBox, BoxLayout.X_AXIS));
            paletteBox.setBackground(Color.white);
            paletteBox.add(Box.createHorizontalStrut(10));
            label = new JLabel(bundle.getString("ChoosePalette"));
            label.setPreferredSize(new Dimension(180, 24));
            paletteBox.add(label);
            paletteBox.add(Box.createHorizontalGlue());
            paletteImage.initialize(256, 18, paletteFile, false, PaletteImage.HORIZONTAL_ORIENTATION);
            paletteImage.setMinimumSize(new Dimension(50, 20));
            paletteBox.add(paletteImage);
            paletteButton.setActionCommand("changePalette");
            paletteButton.addActionListener(this);
            paletteBox.add(paletteButton);
            paletteBox.add(Box.createHorizontalStrut(10));

//            final JPanel nonlinearityBox = new JPanel();
//            nonlinearityBox.setLayout(new BoxLayout(nonlinearityBox, BoxLayout.X_AXIS));
//            nonlinearityBox.setBackground(backColour);
//            nonlinearityBox.add(Box.createHorizontalStrut(10));
//            label = new JLabel(bundle.getString("PaletteNonlinearity") + ":");
//            label.setPreferredSize(new Dimension(180, 24));
//            nonlinearityBox.add(label);
//            nonlinearityBox.add(Box.createHorizontalGlue());
//            String str = df.format(vli.getNonlinearity());
//            labelNonlinearity.setText("Gamma: " + str);
//            nonlinearityBox.add(labelNonlinearity);
//            nonlinearityBox.add(Box.createHorizontalStrut(10));
//            scrollNonlinearity.setValue((int) (vli.getNonlinearity() * 10));
//            scrollNonlinearity.setMaximumSize(new Dimension(200, 22));
//            scrollNonlinearity.addAdjustmentListener(this);
//            nonlinearityBox.add(scrollNonlinearity);
//            nonlinearityBox.add(Box.createHorizontalStrut(10));
//            nonlinearityBox.setVisible(checkScalePalette.isSelected());
            titleBox.setBackground(Color.white);
            mainBox.add(titleBox);
            overlayBox.setBackground(backColour);
            mainBox.add(overlayBox);
            visibleBox.setBackground(Color.white);
            mainBox.add(visibleBox);
            markerBox.setBackground(backColour);
            mainBox.add(markerBox);
            fillColourBox.setBackground(Color.white);
            mainBox.add(fillColourBox);
            valueFieldBox.setBackground(backColour);
            mainBox.add(valueFieldBox);
            paletteBox.setBackground(Color.white);
            mainBox.add(paletteBox);
            minBox.setBackground(backColour);
            mainBox.add(minBox);
            maxBox.setBackground(Color.white);
            mainBox.add(maxBox);

//                nonlinearityBox.setBackground(Color.white);
//                mainBox.add(nonlinearityBox);
            alphaBox.setBackground(backColour);
            mainBox.add(alphaBox);
            mainBox.add(Box.createVerticalStrut(80));

            if (lli.isFilledWithOneColour()) {
                uniqueFillColour.setSelected(true);
                fieldBasedFillColour.setSelected(false);
                valueFieldBox.setVisible(false);
                paletteBox.setVisible(false);
                minBox.setVisible(false);
                maxBox.setVisible(false);
            } else {
                uniqueFillColour.setSelected(false);
                fieldBasedFillColour.setSelected(true);
                valueFieldBox.setVisible(true);
                paletteBox.setVisible(true);
            }

        }

        tabs = new JTabbedPane();

        JScrollPane scroll = new JScrollPane(mainBox);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
        panel1.add(scroll);
        tabs.addTab(bundle.getString("Display"), panel1);

        Box fileMainBox = getFileMainBox();
        JScrollPane scroll2 = new JScrollPane(fileMainBox);
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        panel2.add(scroll2);
        tabs.addTab(bundle.getString("File"), panel2);

        getContentPane().add(tabs, BorderLayout.CENTER);

        pack();

//        // Centre the dialog on the screen.
//        // Get the size of the screen
//        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
//        int screenHeight = dim.height;
//        int screenWidth = dim.width;
//        //setSize(screenWidth / 2, screenHeight / 2);
//        this.setLocation(screenWidth / 4, screenHeight / 4);
    }

    private Box getFileMainBox() {
        Box fileMainBox = Box.createVerticalBox();
        try {
            JLabel label;
            JLabel label2;
            if (layer instanceof RasterLayerInfo) {
                RasterLayerInfo rli = (RasterLayerInfo) layer;
                WhiteboxRasterInfo wri = rli.getWhiteboxRasterInfo();

                // file name
                JPanel fileNameBox = new JPanel();
                fileNameBox.setLayout(new BoxLayout(fileNameBox, BoxLayout.X_AXIS));
                fileNameBox.setBackground(Color.white);
                fileNameBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("FileName"));
                label.setPreferredSize(new Dimension(180, 24));
                fileNameBox.add(label);
                fileNameBox.add(Box.createHorizontalGlue());
                JTextField fileName = new JTextField(rli.getHeaderFile(), 30);
                fileName.setMaximumSize(new Dimension(600, 30));
                fileName.setEditable(false);
                fileNameBox.add(fileName);
                fileNameBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(fileNameBox);

                // dataType
                JPanel dataTypeBox = new JPanel();
                dataTypeBox.setLayout(new BoxLayout(dataTypeBox, BoxLayout.X_AXIS));
                dataTypeBox.setBackground(backColour);
                dataTypeBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("DataType"));
                dataTypeBox.add(label);
                dataTypeBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getDataType().name()));
                dataTypeBox.add(label2);
                dataTypeBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(dataTypeBox);

                // dataScale
                JPanel dataScaleBox = new JPanel();
                dataScaleBox.setLayout(new BoxLayout(dataScaleBox, BoxLayout.X_AXIS));
                dataScaleBox.setBackground(Color.white);
                dataScaleBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("DataScale"));
                dataScaleBox.add(label);
                dataScaleBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getDataScale().name()));
                dataScaleBox.add(label2);
                dataScaleBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(dataScaleBox);

                // numRows
                JPanel numRowsBox = new JPanel();
                numRowsBox.setLayout(new BoxLayout(numRowsBox, BoxLayout.X_AXIS));
                numRowsBox.setBackground(backColour);
                numRowsBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("NumberOfRows"));
                numRowsBox.add(label);
                numRowsBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getNumberRows()));
                numRowsBox.add(label2);
                numRowsBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(numRowsBox);

                // numCols
                JPanel numColsBox = new JPanel();
                numColsBox.setLayout(new BoxLayout(numColsBox, BoxLayout.X_AXIS));
                numColsBox.setBackground(Color.white);
                numColsBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("NumberOfColumns"));
                numColsBox.add(label);
                numColsBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getNumberColumns()));
                numColsBox.add(label2);
                numColsBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(numColsBox);

                // xMin
                JPanel xMinBox = new JPanel();
                xMinBox.setLayout(new BoxLayout(xMinBox, BoxLayout.X_AXIS));
                xMinBox.setBackground(backColour);
                xMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("South"));
                xMinBox.add(label);
                xMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getSouth()));
                xMinBox.add(label2);
                xMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(xMinBox);

                // xMax
                JPanel xMaxBox = new JPanel();
                xMaxBox.setLayout(new BoxLayout(xMaxBox, BoxLayout.X_AXIS));
                xMaxBox.setBackground(Color.white);
                xMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("North"));
                xMaxBox.add(label);
                xMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getNorth()));
                xMaxBox.add(label2);
                xMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(xMaxBox);

                // yMin
                JPanel yMinBox = new JPanel();
                yMinBox.setLayout(new BoxLayout(yMinBox, BoxLayout.X_AXIS));
                yMinBox.setBackground(backColour);
                yMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("East"));
                yMinBox.add(label);
                yMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getEast()));
                yMinBox.add(label2);
                yMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(yMinBox);

                // yMax
                JPanel yMaxBox = new JPanel();
                yMaxBox.setLayout(new BoxLayout(yMaxBox, BoxLayout.X_AXIS));
                yMaxBox.setBackground(Color.white);
                yMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("West"));
                yMaxBox.add(label);
                yMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getWest()));
                yMaxBox.add(label2);
                yMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(yMaxBox);

                // minVal
                JPanel minValBox = new JPanel();
                minValBox.setLayout(new BoxLayout(minValBox, BoxLayout.X_AXIS));
                minValBox.setBackground(backColour);
                minValBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("MinimumValue"));
                minValBox.add(label);
                minValBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getMinimumValue()));
                minValBox.add(label2);
                minValBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(minValBox);

                // maxVal
                JPanel maxValBox = new JPanel();
                maxValBox.setLayout(new BoxLayout(maxValBox, BoxLayout.X_AXIS));
                maxValBox.setBackground(Color.white);
                maxValBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("MaximumValue"));
                maxValBox.add(label);
                maxValBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getMaximumValue()));
                maxValBox.add(label2);
                maxValBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(maxValBox);

                // minDispVal
                JPanel minDispValBox = new JPanel();
                minDispValBox.setLayout(new BoxLayout(minDispValBox, BoxLayout.X_AXIS));
                minDispValBox.setBackground(backColour);
                minDispValBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("MinDisplayValue"));
                minDispValBox.add(label);
                minDispValBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getDisplayMinimum()));
                minDispValBox.add(label2);
                minDispValBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(minDispValBox);

                // maxDispVal
                JPanel maxDispValBox = new JPanel();
                maxDispValBox.setLayout(new BoxLayout(maxDispValBox, BoxLayout.X_AXIS));
                maxDispValBox.setBackground(Color.white);
                maxDispValBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("MinDisplayValue"));
                maxDispValBox.add(label);
                maxDispValBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getDisplayMaximum()));
                maxDispValBox.add(label2);
                maxDispValBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(maxDispValBox);

                // cellSizeX
                JPanel cellSizeXBox = new JPanel();
                cellSizeXBox.setLayout(new BoxLayout(cellSizeXBox, BoxLayout.X_AXIS));
                cellSizeXBox.setBackground(backColour);
                cellSizeXBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("CellSizeX"));
                cellSizeXBox.add(label);
                cellSizeXBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getCellSizeX()));
                cellSizeXBox.add(label2);
                cellSizeXBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(cellSizeXBox);

                // cellSizeY
                JPanel cellSizeYBox = new JPanel();
                cellSizeYBox.setLayout(new BoxLayout(cellSizeYBox, BoxLayout.X_AXIS));
                cellSizeYBox.setBackground(Color.white);
                cellSizeYBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("CellSizeY"));
                cellSizeYBox.add(label);
                cellSizeYBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getCellSizeY()));
                cellSizeYBox.add(label2);
                cellSizeYBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(cellSizeYBox);

                // noData
                JPanel noDataBox = new JPanel();
                noDataBox.setLayout(new BoxLayout(noDataBox, BoxLayout.X_AXIS));
                noDataBox.setBackground(backColour);
                noDataBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("NoDataValue"));
                label.setPreferredSize(new Dimension(180, 24));
                noDataBox.add(label);
                noDataBox.add(Box.createHorizontalGlue());
                //label2 = new JLabel(String.valueOf(wri.getXYUnits()));
                noDataText = new JTextField(String.valueOf(wri.getNoDataValue()), 30);
                noDataText.setMaximumSize(new Dimension(600, 30));
                noDataText.setHorizontalAlignment(JTextField.RIGHT);
                noDataText.addKeyListener(new myKeyListener());
                noDataBox.add(noDataText); //label2);
                noDataBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(noDataBox);
//                JPanel noDataBox = new JPanel();
//                noDataBox.setLayout(new BoxLayout(noDataBox, BoxLayout.X_AXIS));
//                noDataBox.setBackground(backColour);
//                noDataBox.add(Box.createHorizontalStrut(10));
//                label = new JLabel(bundle.getString("NoDataValue"));
//                noDataBox.add(label);
//                noDataBox.add(Box.createHorizontalGlue());
//                label2 = new JLabel(String.valueOf(wri.getNoDataValue()));
//                noDataBox.add(label2);
//                noDataBox.add(Box.createHorizontalStrut(10));
//                fileMainBox.add(noDataBox);

                // XYUnits
                JPanel XYUnitBox = new JPanel();
                XYUnitBox.setLayout(new BoxLayout(XYUnitBox, BoxLayout.X_AXIS));
                XYUnitBox.setBackground(Color.white);
                XYUnitBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("XYUnits"));
                label.setPreferredSize(new Dimension(180, 24));
                XYUnitBox.add(label);
                XYUnitBox.add(Box.createHorizontalGlue());
                //label2 = new JLabel(String.valueOf(wri.getXYUnits()));
                XYUnitsText = new JTextField(wri.getXYUnits(), 30);
                XYUnitsText.setMaximumSize(new Dimension(600, 30));
                XYUnitsText.setHorizontalAlignment(JTextField.RIGHT);
                XYUnitsText.addKeyListener(new myKeyListener());
                XYUnitBox.add(XYUnitsText); //label2);
                XYUnitBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(XYUnitBox);

                // zUnits
                JPanel ZUnitBox = new JPanel();
                ZUnitBox.setLayout(new BoxLayout(ZUnitBox, BoxLayout.X_AXIS));
                ZUnitBox.setBackground(backColour);
                ZUnitBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("ZUnits"));
                label.setPreferredSize(new Dimension(180, 24));
                ZUnitBox.add(label);
                ZUnitBox.add(Box.createHorizontalGlue());
                //label2 = new JLabel(String.valueOf(wri.getZUnits()));
                ZUnitsText = new JTextField(wri.getZUnits(), 30);
                ZUnitsText.setMaximumSize(new Dimension(600, 30));
                ZUnitsText.setHorizontalAlignment(JTextField.RIGHT);
                ZUnitsText.addKeyListener(new myKeyListener());
                ZUnitBox.add(ZUnitsText); //label2);
                ZUnitBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(ZUnitBox);

                // byteOrder
                JPanel byteOrderBox = new JPanel();
                byteOrderBox.setLayout(new BoxLayout(byteOrderBox, BoxLayout.X_AXIS));
                byteOrderBox.setBackground(Color.white);
                byteOrderBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("ByteOrder"));
                byteOrderBox.add(label);
                byteOrderBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(wri.getByteOrder()));
                byteOrderBox.add(label2);
                byteOrderBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(byteOrderBox);

                // fileLength
                JPanel fileLengthBox = new JPanel();
                fileLengthBox.setLayout(new BoxLayout(fileLengthBox, BoxLayout.X_AXIS));
                fileLengthBox.setBackground(backColour);
                fileLengthBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("FileLength"));
                fileLengthBox.add(label);
                fileLengthBox.add(Box.createHorizontalGlue());
                String fileLengthUnits = " kB";
                double fileLength = (rli.getDataFileSize()) / 1024.0;
                if (fileLength > 1024) {
                    fileLengthUnits = " MB";
                    fileLength = fileLength / 1024;
                }
                if (fileLength > 1024) {
                    fileLengthUnits = " GB";
                    fileLength = fileLength / 1024;
                }
                DecimalFormat df2 = new DecimalFormat("###,###,###.0#");
                label2 = new JLabel(df2.format(fileLength) + fileLengthUnits);
                fileLengthBox.add(label2);
                fileLengthBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(fileLengthBox);

                // metaData
                JPanel metaDataBox = new JPanel();
                metaDataBox.setLayout(new BoxLayout(metaDataBox, BoxLayout.X_AXIS));
                metaDataBox.setBackground(Color.white);
                metaDataBox.add(Box.createHorizontalStrut(10));
                Box metaDataVBox1 = Box.createVerticalBox();
                metaDataVBox1.setBackground(Color.white);
                metaDataVBox1.add(new JLabel(bundle.getString("Metadata")));
                metaDataVBox1.add(Box.createVerticalGlue());
                metaDataBox.add(metaDataVBox1);
                metaDataBox.add(Box.createHorizontalGlue());
                Box metaDataVBox2 = Box.createVerticalBox();
                ArrayList<String> metaDataArray = wri.getMetadata();
                for (String str : metaDataArray) {
                    metaDataVBox2.add(new JLabel(str));
                }
                metaDataVBox2.setBackground(Color.white);
                metaDataBox.add(metaDataVBox2);
                metaDataBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(metaDataBox);

            } else if (layer instanceof VectorLayerInfo) {
                VectorLayerInfo vli = (VectorLayerInfo) layer;
                ShapeFile shapefile = vli.getShapefile();

                // file name
                JPanel fileNameBox = new JPanel();
                fileNameBox.setLayout(new BoxLayout(fileNameBox, BoxLayout.X_AXIS));
                fileNameBox.setBackground(Color.white);
                fileNameBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("FileName"));
                label.setPreferredSize(new Dimension(180, 24));
                fileNameBox.add(label);
                fileNameBox.add(Box.createHorizontalGlue());
                JTextField fileName = new JTextField(vli.getFileName(), 30);
                fileName.setMaximumSize(new Dimension(600, 30));
                fileName.setEditable(false);
                fileNameBox.add(fileName);
                fileNameBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(fileNameBox);

                // shape type
                JPanel shapeTypeBox = new JPanel();
                shapeTypeBox.setLayout(new BoxLayout(shapeTypeBox, BoxLayout.X_AXIS));
                shapeTypeBox.setBackground(backColour);
                shapeTypeBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("ShapeType"));
                shapeTypeBox.add(label);
                shapeTypeBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(shapefile.getShapeType().name());
                shapeTypeBox.add(label2);
                shapeTypeBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(shapeTypeBox);

                // xMin
                JPanel xMinBox = new JPanel();
                xMinBox.setLayout(new BoxLayout(xMinBox, BoxLayout.X_AXIS));
                xMinBox.setBackground(Color.white);
                xMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("XMin"));
                xMinBox.add(label);
                xMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(shapefile.getxMin()));
                xMinBox.add(label2);
                xMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(xMinBox);

                // xMax
                JPanel xMaxBox = new JPanel();
                xMaxBox.setLayout(new BoxLayout(xMaxBox, BoxLayout.X_AXIS));
                xMaxBox.setBackground(backColour);
                xMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("XMax"));
                xMaxBox.add(label);
                xMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(shapefile.getxMax()));
                xMaxBox.add(label2);
                xMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(xMaxBox);

                // yMin
                JPanel yMinBox = new JPanel();
                yMinBox.setLayout(new BoxLayout(yMinBox, BoxLayout.X_AXIS));
                yMinBox.setBackground(Color.white);
                yMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("YMin"));
                yMinBox.add(label);
                yMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(shapefile.getyMin()));
                yMinBox.add(label2);
                yMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(yMinBox);

                // yMax
                JPanel yMaxBox = new JPanel();
                yMaxBox.setLayout(new BoxLayout(yMaxBox, BoxLayout.X_AXIS));
                yMaxBox.setBackground(backColour);
                yMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("YMax"));
                yMaxBox.add(label);
                yMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(shapefile.getyMax()));
                yMaxBox.add(label2);
                yMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(yMaxBox);

                ShapeType st = shapefile.getShapeType();
                if (st == ShapeType.POINTZ || st == ShapeType.POLYLINEZ
                        || st == ShapeType.POLYGONZ || st == ShapeType.MULTIPOINTZ) {
                    // zMin
                    JPanel zMinBox = new JPanel();
                    zMinBox.setLayout(new BoxLayout(zMinBox, BoxLayout.X_AXIS));
                    zMinBox.setBackground(Color.white);
                    zMinBox.add(Box.createHorizontalStrut(10));
                    label = new JLabel(bundle.getString("ZMin"));
                    zMinBox.add(label);
                    zMinBox.add(Box.createHorizontalGlue());
                    label2 = new JLabel(String.valueOf(shapefile.getzMin()));
                    zMinBox.add(label2);
                    zMinBox.add(Box.createHorizontalStrut(10));
                    fileMainBox.add(zMinBox);

                    // zMax
                    JPanel zMaxBox = new JPanel();
                    zMaxBox.setLayout(new BoxLayout(zMaxBox, BoxLayout.X_AXIS));
                    zMaxBox.setBackground(backColour);
                    zMaxBox.add(Box.createHorizontalStrut(10));
                    label = new JLabel(bundle.getString("ZMax"));
                    zMaxBox.add(label);
                    zMaxBox.add(Box.createHorizontalGlue());
                    label2 = new JLabel(String.valueOf(shapefile.getzMax()));
                    zMaxBox.add(label2);
                    zMaxBox.add(Box.createHorizontalStrut(10));
                    fileMainBox.add(zMaxBox);
                }

                // fileLength
                JPanel fileLengthBox = new JPanel();
                fileLengthBox.setLayout(new BoxLayout(fileLengthBox, BoxLayout.X_AXIS));
                fileLengthBox.setBackground(Color.white);
                fileLengthBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("FileLength"));
                fileLengthBox.add(label);
                fileLengthBox.add(Box.createHorizontalGlue());
                String fileLengthUnits = " kB";
                double fileLength = (new File(vli.getFileName()).length()) / 1024.0;
                if (fileLength > 1024) {
                    fileLengthUnits = " MB";
                    fileLength = fileLength / 1024;
                }
                if (fileLength > 1024) {
                    fileLengthUnits = " GB";
                    fileLength = fileLength / 1024;
                }
                DecimalFormat df2 = new DecimalFormat("###,###,###.0#");
                label2 = new JLabel(df2.format(fileLength) + fileLengthUnits);
                fileLengthBox.add(label2);
                fileLengthBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(fileLengthBox);

                // numRecords
                JPanel numRecsBox = new JPanel();
                numRecsBox.setLayout(new BoxLayout(numRecsBox, BoxLayout.X_AXIS));
                numRecsBox.setBackground(backColour);
                numRecsBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("NumRecords"));
                numRecsBox.add(label);
                numRecsBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(shapefile.getNumberOfRecords()));
                numRecsBox.add(label2);
                numRecsBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(numRecsBox);

                fileMainBox.add(Box.createVerticalStrut(80));
            }  else if (layer instanceof LasLayerInfo) {
                LasLayerInfo lli = (LasLayerInfo) layer;
                LASReader lasfile = lli.getLASFile();

                // file name
                JPanel fileNameBox = new JPanel();
                fileNameBox.setLayout(new BoxLayout(fileNameBox, BoxLayout.X_AXIS));
                fileNameBox.setBackground(Color.white);
                fileNameBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("FileName"));
                label.setPreferredSize(new Dimension(180, 24));
                fileNameBox.add(label);
                fileNameBox.add(Box.createHorizontalGlue());
                JTextField fileName = new JTextField(lasfile.getFileName(), 30);
                fileName.setMaximumSize(new Dimension(600, 30));
                fileName.setEditable(false);
                fileNameBox.add(fileName);
                fileNameBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(fileNameBox);

                // xMin
                JPanel xMinBox = new JPanel();
                xMinBox.setLayout(new BoxLayout(xMinBox, BoxLayout.X_AXIS));
                xMinBox.setBackground(backColour);
                xMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("XMin"));
                xMinBox.add(label);
                xMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getMinX()));
                xMinBox.add(label2);
                xMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(xMinBox);

                // xMax
                JPanel xMaxBox = new JPanel();
                xMaxBox.setLayout(new BoxLayout(xMaxBox, BoxLayout.X_AXIS));
                xMaxBox.setBackground(Color.white);
                xMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("XMax"));
                xMaxBox.add(label);
                xMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getMaxX()));
                xMaxBox.add(label2);
                xMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(xMaxBox);

                // yMin
                JPanel yMinBox = new JPanel();
                yMinBox.setLayout(new BoxLayout(yMinBox, BoxLayout.X_AXIS));
                yMinBox.setBackground(backColour);
                yMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("YMin"));
                yMinBox.add(label);
                yMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getMinY()));
                yMinBox.add(label2);
                yMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(yMinBox);

                // yMax
                JPanel yMaxBox = new JPanel();
                yMaxBox.setLayout(new BoxLayout(yMaxBox, BoxLayout.X_AXIS));
                yMaxBox.setBackground(Color.white);
                yMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("YMax"));
                yMaxBox.add(label);
                yMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getMaxY()));
                yMaxBox.add(label2);
                yMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(yMaxBox);
                
                // zMin
                JPanel zMinBox = new JPanel();
                zMinBox.setLayout(new BoxLayout(zMinBox, BoxLayout.X_AXIS));
                zMinBox.setBackground(backColour);
                zMinBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("ZMin"));
                zMinBox.add(label);
                zMinBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getMinZ()));
                zMinBox.add(label2);
                zMinBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(zMinBox);

                // zMax
                JPanel zMaxBox = new JPanel();
                zMaxBox.setLayout(new BoxLayout(zMaxBox, BoxLayout.X_AXIS));
                zMaxBox.setBackground(Color.white);
                zMaxBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("ZMax"));
                zMaxBox.add(label);
                zMaxBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getMaxZ()));
                zMaxBox.add(label2);
                zMaxBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(zMaxBox);

                // fileLength
                JPanel fileLengthBox = new JPanel();
                fileLengthBox.setLayout(new BoxLayout(fileLengthBox, BoxLayout.X_AXIS));
                fileLengthBox.setBackground(backColour);
                fileLengthBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("FileLength"));
                fileLengthBox.add(label);
                fileLengthBox.add(Box.createHorizontalGlue());
                String fileLengthUnits = " kB";
                double fileLength = (new File(lasfile.getFileName()).length()) / 1024.0;
                if (fileLength > 1024) {
                    fileLengthUnits = " MB";
                    fileLength = fileLength / 1024;
                }
                if (fileLength > 1024) {
                    fileLengthUnits = " GB";
                    fileLength = fileLength / 1024;
                }
                DecimalFormat df2 = new DecimalFormat("###,###,###.0#");
                label2 = new JLabel(df2.format(fileLength) + fileLengthUnits);
                fileLengthBox.add(label2);
                fileLengthBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(fileLengthBox);

                // numRecords
                JPanel numRecsBox = new JPanel();
                numRecsBox.setLayout(new BoxLayout(numRecsBox, BoxLayout.X_AXIS));
                numRecsBox.setBackground(Color.white);
                numRecsBox.add(Box.createHorizontalStrut(10));
                label = new JLabel(bundle.getString("NumRecords"));
                numRecsBox.add(label);
                numRecsBox.add(Box.createHorizontalGlue());
                label2 = new JLabel(String.valueOf(lasfile.getNumPointRecords()));
                numRecsBox.add(label2);
                numRecsBox.add(Box.createHorizontalStrut(10));
                fileMainBox.add(numRecsBox);

                fileMainBox.add(Box.createVerticalStrut(80));
            }

        } catch (Exception e) {

        } finally {
            return fileMainBox;
        }

    }

    private ArrayList<double[][]> markers = new ArrayList<>();

    private void initializeMarkerStyle() {
        VectorLayerInfo vli = (VectorLayerInfo) layer;
        markers = PointMarkers.getAllSymbols(markerSize);

        Integer[] intArray = new Integer[markers.size()];
        for (int k = 0; k < markers.size(); k++) {
            intArray[k] = new Integer(k);
        }
        markerCombo = new JComboBox(intArray);
        MarkerStyle ms = vli.getMarkerStyle();
        markerCombo.setSelectedIndex(ms.ordinal());

        float lineThick = minLineThickness + scrollLineThickness.getValue() / (float) scrollbarMax * (maxLineThickness - minLineThickness);
        MarkerStyleComboBoxRenderer renderer = new MarkerStyleComboBoxRenderer(markers,
                lineThick,
                sampleColourLine, sampleColourFill, markerSize, checkFilled.isSelected(),
                checkOutlined.isSelected());
//        MarkerStyleComboBoxRenderer renderer = new MarkerStyleComboBoxRenderer(markers,
//                (float) ((scrollLineThickness.getValue() + minLineThickness) / 100f),
//                sampleColourLine, sampleColourFill, markerSize, checkFilled.isSelected(),
//                checkOutlined.isSelected());
        markerCombo.setRenderer(renderer);
    }

    private void initializeLineStyle() {
        VectorLayerInfo vli = (VectorLayerInfo) layer;
        Integer[] intArray = new Integer[dashArray.length];
        for (int k = 0; k < dashArray.length; k++) {
            intArray[k] = new Integer(k);
        }
        //dashCombo = new JComboBox(intArray);
        // find the current dashArray
        float[] myDash = vli.getDashArray();
        if (vli.isDashed()) {
            for (int k = 0; k < dashArray.length; k++) {
                if (Arrays.equals(dashArray[k], myDash)) {
                    dashCombo.setSelectedIndex(k);
                }
            }
        } else {
            dashCombo.setSelectedIndex(0);
        }
        float lineThick = minLineThickness + scrollLineThickness.getValue() / ((float) scrollbarMax) * (maxLineThickness - minLineThickness);
        LineStyleComboBoxRenderer renderer = new LineStyleComboBoxRenderer(dashArray,
                lineThick, sampleColourLine);

//        LineStyleComboBoxRenderer renderer = new LineStyleComboBoxRenderer(dashArray,
//                (float) ((scrollLineThickness.getValue() + minLineThickness) / 100f), sampleColourLine);
        dashCombo.setRenderer(renderer);
    }

    public void updateLayer() {
        if (layer instanceof RasterLayerInfo) {
            RasterLayerInfo rli = (RasterLayerInfo) layer;
            rli.setDisplayMinVal(Double.parseDouble(minVal.getText()));
            rli.setDisplayMaxVal(Double.parseDouble(maxVal.getText()));
            // see if the user has modified information in the header.
            if (updatedFileHeader) {
                WhiteboxRasterInfo wri = rli.getWhiteboxRasterInfo();
                if (wri.getNoDataValue() != Double.parseDouble(noDataText.getText())) {
                    wri.setNoDataValue(Double.parseDouble(noDataText.getText()));
                    wri.findMinAndMaxVals();
                    wri.setDisplayMinimum(wri.getMinimumValue());
                    wri.setDisplayMaximum(wri.getMaximumValue());
//                    rli.setDisplayMinVal(wri.getMinimumValue());
//                    rli.setDisplayMaxVal(wri.getMaximumValue());
                }
                wri.setXYUnits(XYUnitsText.getText());
                wri.setZUnits(ZUnitsText.getText());
                wri.writeHeaderFile();
                rli.resyncWithRasterFile();
            }
            rli.setLayerTitle(titleText.getText());
            rli.setAlpha((Integer) (scrollAlpha.getValue()));
            rli.setVisible(checkVisible.isSelected());
            rli.setPaletteFile(paletteFile);
            rli.setNonlinearity(scrollNonlinearity.getValue() / 10d);
            if (checkScalePalette.isSelected() && ((rli.getDataScale() == WhiteboxRaster.DataScale.CATEGORICAL) || (rli.getDataScale() == WhiteboxRaster.DataScale.BOOLEAN))) {
                rli.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            } else if (!checkScalePalette.isSelected() && rli.getDataScale() == WhiteboxRaster.DataScale.CONTINUOUS) {
                rli.setDataScale(WhiteboxRaster.DataScale.CATEGORICAL);
            }
            rli.setPaletteReversed(checkReversePalette.isSelected());
            rli.update();
            host.refreshMap(true);
            updatePaletteImage();
        } else if (layer instanceof VectorLayerInfo) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            vli.setLayerTitle(titleText.getText());
            vli.setVisible(checkVisible.isSelected());
            vli.setFilled(checkFilled.isSelected());
            vli.setOutlined(checkOutlined.isSelected());
            float lineThick = minLineThickness + scrollLineThickness.getValue() / ((float) scrollbarMax) * (maxLineThickness - minLineThickness);
            vli.setLineThickness(lineThick); //(scrollLineThickness.getValue() + minLineThickness) / 100f);
            vli.setFillColour(sampleColourFill);
            vli.setLineColour(sampleColourLine);
            vli.setMarkerSize(scrollMarkerSize.getValue());
            int selectedDashLine = dashCombo.getSelectedIndex();
            if (selectedDashLine == 0) {
                vli.setDashed(false);
            } else {
                vli.setDashed(true);
                vli.setDashArray(dashArray[selectedDashLine]);
            }
            vli.setAlpha((int) (scrollAlpha.getValue()));
            vli.setMarkerStyle(PointMarkers.findMarkerStyleFromIndex(markerCombo.getSelectedIndex()));
            vli.setCartographicGeneralizationLevel(scrollGeneralizeLevel.getValue() / 100.0 * 5.0);
            ShapeType shapeType = vli.getShapeType();
            if (shapeType.getBaseType() == ShapeType.POLYLINE) {
                //vli.setLineAttribute("");
                vli.setOutlinedWithOneColour(uniqueLineColour.isSelected());
                vli.setPaletteFile(paletteFile);
                vli.setPaletteScaled(checkScalePalette.isSelected());
                if (!uniqueLineColour.isSelected()) {
                    vli.setLineAttribute(String.valueOf(valueFieldCombo.getSelectedItem()));

                } else {
                    vli.setLineAttribute("");
                }
            } else {
                vli.setFilledWithOneColour(uniqueFillColour.isSelected());
                vli.setPaletteFile(paletteFile);
                vli.setPaletteScaled(checkScalePalette.isSelected());
                if (!uniqueFillColour.isSelected()) {
                    vli.setFillAttribute(String.valueOf(valueFieldCombo.getSelectedItem()));

                } else {
                    vli.setFillAttribute("");
                }
            }

            vli.setNonlinearity(scrollNonlinearity.getValue() / 10d);

            vli.setRecordsColourData();

            host.refreshMap(true);
        } else if (layer instanceof LasLayerInfo) {
            boolean updateColours = false;
            LasLayerInfo lli = (LasLayerInfo) layer;
            lli.setLayerTitle(titleText.getText());
            lli.setVisible(checkVisible.isSelected());
            lli.setFillColour(sampleColourFill);
            if (uniqueFillColour.isSelected()) {
                if (!lli.isFilledWithOneColour()) {
                    lli.setFilledWithOneColour(true);
                    updateColours = true;
                }
            } else {
                if (lli.isFilledWithOneColour()) {
                    lli.setFilledWithOneColour(false);
                    updateColours = true;
                    if (valueFieldCombo.getSelectedIndex() == 0) {
                        if (!lli.getFillCriterion().contains("z")) {
                            lli.setFillCriterion("z-value");
                            updateColours = true;
                        }
                    } else if (valueFieldCombo.getSelectedIndex() == 1) {
                        if (!lli.getFillCriterion().contains("int")) {
                            lli.setFillCriterion("intensity");
                            updateColours = true;
                        }
                    }
                }
            }

            if (valueFieldCombo.getSelectedIndex() == 0) {
                if (!lli.getFillCriterion().contains("z")) {
                    lli.setFillCriterion("z-value");
                    updateColours = true;
                }
            } else if (valueFieldCombo.getSelectedIndex() == 1) {
                if (!lli.getFillCriterion().contains("int")) {
                    lli.setFillCriterion("intensity");
                    updateColours = true;
                }
            }

            lli.setMarkerSize(scrollMarkerSize.getValue() / 2.0f);
            lli.setAlpha((int) (scrollAlpha.getValue()));

            double disMin = Double.parseDouble(minVal.getText());
            double disMax = Double.parseDouble(maxVal.getText());

            if (disMin != lli.getDisplayMinValue() || disMax != lli.getDisplayMaxValue()) {
                lli.setDisplayMinValue(disMin);
                lli.setDisplayMaxValue(disMax);
                updateColours = true;
            }

            if (!paletteFile.equals(lli.getPaletteFile())) {
                lli.setPaletteFile(paletteFile);
                updateColours = true;
            }

            //lli.setNonlinearity(scrollNonlinearity.getValue() / 10d);
            if (updateColours) {
                lli.setRecordsColourData();
            }

            host.refreshMap(true);
        }
    }

    private void updatePaletteImage() {
        paletteImage.setNonlinearity(scrollNonlinearity.getValue() / 10d);
    }

    public void resetMinimum() {
        if (layer instanceof RasterLayerInfo) {
            RasterLayerInfo rli = (RasterLayerInfo) layer;
            minVal.setText(Double.toString(rli.getMinVal()));
        }
    }

    public void resetMaximum() {
        if (layer instanceof RasterLayerInfo) {
            RasterLayerInfo rli = (RasterLayerInfo) layer;
            maxVal.setText(Double.toString(rli.getMaxVal()));
        }
    }

    private String paletteFile;

    private void changePalette() {
        if (host != null && layer instanceof RasterLayerInfo) {
            RasterLayerInfo rli = (RasterLayerInfo) layer;
            Communicator communicator = (Communicator) (host);
            String pathSep = File.separator;
            String paletteDirectory = communicator.getResourcesDirectory()
                    + pathSep + "palettes" + pathSep;
            PaletteChooser chooser = new PaletteChooser((Frame) this.getOwner(), true, paletteDirectory, rli.getPaletteFile(),
                    checkReversePalette.isSelected(), rli.getNonlinearity());
            chooser.setSize(300, 300);
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int screenHeight = dim.height;
            int screenWidth = dim.width;
            chooser.setLocation(screenWidth / 4, screenHeight / 4);
            chooser.setVisible(true);
            String newPaletteFile = chooser.getValue();
            chooser.dispose();
            if (newPaletteFile != null) {
                if (!newPaletteFile.equals("") && !newPaletteFile.equals("createNewPalette")) {
                    paletteFile = newPaletteFile;
                    paletteImage.initialize(256, 18, paletteFile,
                            checkReversePalette.isSelected(),
                            PaletteImage.HORIZONTAL_ORIENTATION);
                    paletteImage.repaint();
                } else if (newPaletteFile.equals("createNewPalette")) {
                    PaletteManager pm = new PaletteManager(paletteDirectory,
                            host.getGuiLabelsBundle());
                    pm.setVisible(true);
                }
            }
        } else if (host != null && layer instanceof VectorLayerInfo) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            Communicator communicator = (Communicator) (host);
            String pathSep = File.separator;
            String paletteDirectory = communicator.getResourcesDirectory()
                    + pathSep + "palettes" + pathSep;
            PaletteChooser chooser = new PaletteChooser((Frame) this.getOwner(),
                    true, paletteDirectory, vli.getPaletteFile(),
                    false, 1.0);
            chooser.setSize(300, 300);
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int screenHeight = dim.height;
            int screenWidth = dim.width;
            chooser.setLocation(screenWidth / 4, screenHeight / 4);
            chooser.setVisible(true);
            String newPaletteFile = chooser.getValue();
            chooser.dispose();
            if (newPaletteFile != null) {
                if (!newPaletteFile.equals("") && !newPaletteFile.equals("createNewPalette")) {
                    paletteFile = newPaletteFile;
                    paletteImage.initialize(256, 18, paletteFile, false,
                            PaletteImage.HORIZONTAL_ORIENTATION);
                    paletteImage.repaint();
                } else if (newPaletteFile.equals("createNewPalette")) {
                    PaletteManager pm = new PaletteManager(paletteDirectory,
                            host.getGuiLabelsBundle());
                    pm.setVisible(true);
                }
            }
        } else if (host != null && layer instanceof LasLayerInfo) {
            LasLayerInfo lli = (LasLayerInfo) layer;
            Communicator communicator = (Communicator) (host);
            String pathSep = File.separator;
            String paletteDirectory = communicator.getResourcesDirectory()
                    + pathSep + "palettes" + pathSep;
            PaletteChooser chooser = new PaletteChooser((Frame) this.getOwner(),
                    true, paletteDirectory, lli.getPaletteFile(),
                    false, 1.0);
            chooser.setSize(300, 300);
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int screenHeight = dim.height;
            int screenWidth = dim.width;
            chooser.setLocation(screenWidth / 4, screenHeight / 4);
            chooser.setVisible(true);
            String newPaletteFile = chooser.getValue();
            chooser.dispose();
            if (newPaletteFile != null) {
                if (!newPaletteFile.equals("") && !newPaletteFile.equals("createNewPalette")) {
                    paletteFile = newPaletteFile;
                    paletteImage.initialize(256, 18, paletteFile, false,
                            PaletteImage.HORIZONTAL_ORIENTATION);
                    paletteImage.repaint();
                } else if (newPaletteFile.equals("createNewPalette")) {
                    PaletteManager pm = new PaletteManager(paletteDirectory,
                            host.getGuiLabelsBundle());
                    pm.setVisible(true);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            setVisible(false);
            this.dispose();
        } else if (actionCommand.equals("ok")) {
            updateLayer();
            this.dispose();
        } else if (actionCommand.equals("resetMinimum")) {
            resetMinimum();
        } else if (actionCommand.equals("resetMaximum")) {
            resetMaximum();
        } else if (actionCommand.equals("update")) {
            updateLayer();
        } else if (actionCommand.equals("changePalette")) {
            changePalette();
        } else if (actionCommand.equals("reversePalette")) {
            paletteImage.setPaletteIsReversed(checkReversePalette.isSelected());
        } else if (actionCommand.equals("clipLowerTail")) {
            if (layer instanceof RasterLayerInfo) {
                RasterLayerInfo rli = (RasterLayerInfo) layer;
                double value = Double.parseDouble(clipAmountLower.getText().replace("%", ""));
                minVal.setText(Double.toString(rli.clipLowerTail(value)));
            }
        } else if (actionCommand.equals("clipUpperTail")) {
            if (layer instanceof RasterLayerInfo) {
                RasterLayerInfo rli = (RasterLayerInfo) layer;
                double value = Double.parseDouble(clipAmountUpper.getText().replace("%", ""));
                maxVal.setText(Double.toString(rli.clipUpperTail(value)));
            }
        } else if (actionCommand.equals("uniqueFill")) {
            valueFieldBox.setVisible(false);
            paletteBox.setVisible(false);
            scalePaletteBox.setVisible(false);
        } else if (actionCommand.equals("fieldBasedFill")) {
            valueFieldBox.setVisible(true);
            paletteBox.setVisible(true);
            scalePaletteBox.setVisible(true);
        } else if (actionCommand.equals("uniqueLine")) {
            valueFieldBox.setVisible(false);
            paletteBox.setVisible(false);
            scalePaletteBox.setVisible(false);
        } else if (actionCommand.equals("fieldBasedLine")) {
            valueFieldBox.setVisible(true);
            paletteBox.setVisible(true);
            scalePaletteBox.setVisible(true);
        } else if (actionCommand.equals("viewAttributesTable")) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            AttributesFileViewer afv = new AttributesFileViewer((Frame) this.getOwner(), false, vli.getFileName());
            int height = 500;
            afv.setSize((int) (height * 1.61803399), height);
            afv.setVisible(true);
        } else if (actionCommand.equals("scalePalette")) {

        }
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent evt) {
        String str;
        if (layer instanceof RasterLayerInfo) {
            labelAlpha.setText("Alpha: " + scrollAlpha.getValue());
            str = df.format(scrollNonlinearity.getValue() / 10d);
            labelNonlinearity.setText("Gamma: " + str);
            paletteImage.setNonlinearity(scrollNonlinearity.getValue() / 10d);
        } else if (layer instanceof VectorLayerInfo) {
            float lineThick = minLineThickness + scrollLineThickness.getValue() / ((float) scrollbarMax) * (maxLineThickness - minLineThickness);
            str = df.format(lineThick);
            labelLineThickness.setText("Value: " + str);
            str = df.format(scrollMarkerSize.getValue());
            labelMarkerSize.setText("Value: " + str);
            markerSize = Float.parseFloat(str);
            initializeLineStyle();
            dashCombo.repaint();
            //initializeMarkerStyle();
            //markerCombo.repaint();
            labelAlpha.setText("Alpha: " + scrollAlpha.getValue());

            str = df.format(scrollNonlinearity.getValue() / 10d);
            labelNonlinearity.setText("Gamma: " + str);
            paletteImage.setNonlinearity(scrollNonlinearity.getValue() / 10d);
        } else if (layer instanceof LasLayerInfo) {
            str = df.format(scrollMarkerSize.getValue() / 2.0f);
            labelMarkerSize.setText("Value: " + str);
            markerSize = Float.parseFloat(str);
            labelAlpha.setText("Alpha: " + scrollAlpha.getValue());

//            str = df.format(scrollNonlinearity.getValue() / 10d);
//            labelNonlinearity.setText("Gamma: " + str);
//            paletteImage.setNonlinearity(scrollNonlinearity.getValue() / 10d);
        }
    }

    @Override
    public void mouseClicked(MouseEvent me) {

    }

    @Override
    public void mousePressed(MouseEvent me) {
        Object source = me.getSource();
        if (source == sampleColourPanelLine || source == sampleColourPanelLine2) {
            Color newColour = null;
            newColour = JColorChooser.showDialog(this, "Choose Color", sampleColourLine);
            if (newColour != null) {
                sampleColourLine = newColour;
                sampleColourPanelLine.setBackColour(newColour);
                sampleColourPanelLine2.setBackColour(newColour);
            }

        } else if (source == sampleColourPanelFill) {
            Color newColour = null;
            newColour = JColorChooser.showDialog(this, "Choose Color", sampleColourFill);
            if (newColour != null) {
                sampleColourFill = newColour;
                sampleColourPanelFill.setBackColour(newColour);
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

    private class myKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent ke) {

        }

        @Override
        public void keyPressed(KeyEvent ke) {

        }

        @Override
        public void keyReleased(KeyEvent ke) {
            updatedFileHeader = true;
        }

    }
}
