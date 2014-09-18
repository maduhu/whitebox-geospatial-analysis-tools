/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import whiteboxgis.WhiteboxGui;

/**
 *
 * @author johnlindsay
 */
public class CartographicToolbar extends JToolBar {

    private JToggleButton pageVisible = new JToggleButton();
    private JToggleButton alignAndDistribute = new JToggleButton();
    private JButton alignRight = new JButton();
    private JButton alignLeft = new JButton();
    private JButton alignTop = new JButton();
    private JButton alignBottom = new JButton();
    private JButton centerVerticalBtn = new JButton();
    private JButton centerHorizontalBtn = new JButton();
    private JButton distributeVertically = new JButton();
    private JButton distributeHorizontally = new JButton();
    private JButton group = new JButton();
    private JButton ungroup = new JButton();
    private WhiteboxGui host;
    private static String pathSep = File.separator;
    private boolean buttonVisibility = false;
    private ResourceBundle bundle;
    // constructors
    public CartographicToolbar() {
        // no-arg constructor
        init();
    }

    public CartographicToolbar(WhiteboxGui host, boolean buttonVisibility) {
        this.host = host;
        this.bundle = host.getGuiLabelsBundle();
        this.buttonVisibility = buttonVisibility;
        init();
    }

    // properties
    public WhiteboxGui getHost() {
        return host;
    }

    public void setHost(WhiteboxGui host) {
        this.host = host;
    }

    public boolean isButtonVisibility() {
        return buttonVisibility;
    }

    public void setButtonVisibility(boolean buttonVisibility) {
        this.buttonVisibility = buttonVisibility;
    }

    // methods
    private void init() {
        if (host == null) {
            return;
        }
        this.setOrientation(SwingConstants.VERTICAL);

        String imgLocation2 = host.getResourcesDirectory() + "Images" + pathSep + "NewMap.png";
        ImageIcon image2 = new ImageIcon(imgLocation2, "");

        //Create and initialize the button.
        pageVisible.setToolTipText(bundle.getString("DrawThePage"));
        pageVisible.setSelected(host.isPageVisible());
        pageVisible.setIcon(image2);
        pageVisible.addActionListener((ActionEvent e) -> {
            host.setPageVisibility(pageVisible.isSelected());
        });
        
        String imgLocation = host.getResourcesDirectory() + "Images" + pathSep + "AlignAndDistribute.png";
        ImageIcon image = new ImageIcon(imgLocation, "");

        //Create and initialize the button.
         alignAndDistribute.setToolTipText(bundle.getString("AlignAndDistribute"));
        alignAndDistribute.setSelected(false);
//        alignAndDistribute.addActionListener((ActionEvent e) -> {
//                if (buttonVisibility) {
//                    buttonVisibility = false;
//                } else {
//                    buttonVisibility = true;
//                }
//                createToolbar();
//        });
        alignAndDistribute.addItemListener((ItemEvent ie) -> {
            if (alignAndDistribute.isSelected()) {
                createToolbar(true);
            } else {
                createToolbar(false);
            }
        });
        alignAndDistribute.setOpaque(false);

        try {
            alignAndDistribute.setIcon(image);
        } catch (Exception e) {
            alignAndDistribute.setText("alignAndDistribute");
            host.showFeedback(e.getMessage());
        }

        alignRight = makeToolBarButton("AlignRight.png", "alignRight", 
                bundle.getString("AlignRight"), "alignRight");

        centerVerticalBtn = makeToolBarButton("CenterVertical.png",
                "centerVertical", bundle.getString("CenterVertically"), 
                "centerVertical");

        alignLeft = makeToolBarButton("AlignLeft.png", "alignLeft",
                bundle.getString("AlignLeft"), "alignLeft");

        alignTop = makeToolBarButton("AlignTop.png", "alignTop",
                bundle.getString("AlignTop"), "alignTop");

        centerHorizontalBtn = makeToolBarButton("CenterHorizontal.png",
                "centerHorizontal", bundle.getString("CenterHorizontally"), 
                "centerHorizontal");

        alignBottom = makeToolBarButton("AlignBottom.png", "alignBottom",
                bundle.getString("AlignBottom"), "alignBottom");

        distributeVertically = makeToolBarButton("DistributeVertically.png",
                "distributeVertically", bundle.getString("DistributeVertically"), 
                "distributeVertically");

        distributeHorizontally = makeToolBarButton("DistributeHorizontally.png",
                "distributeHorizontally", bundle.getString("DistributeHorizontally"), 
                "distributeHorizontally");

        group = makeToolBarButton("GroupElements.png",
                "groupElements", bundle.getString("GroupElements"), 
                "groupElements");

        ungroup = makeToolBarButton("UngroupElements.png",
                "ungroupElements", bundle.getString("UngroupElements"), 
                "ungroupElements");


        createToolbar(false);
    }

    private void createToolbar(boolean fullBar) {
        this.removeAll();
        
        this.add(pageVisible);

        this.add(alignAndDistribute);

        if (fullBar) {
            this.addSeparator();
            this.add(alignRight);
            this.add(centerVerticalBtn);
            this.add(alignLeft);
            this.add(alignTop);
            this.add(centerHorizontalBtn);
            this.add(alignBottom);
            this.addSeparator();
            this.add(distributeVertically);
            this.add(distributeHorizontally);
            this.addSeparator();
            this.add(group);
            this.add(ungroup);
        }
        this.revalidate();
        this.repaint();
    }

    private JButton makeToolBarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        //Look for the image.
        String imgLocation = host.getResourcesDirectory() + "Images" + pathSep + imageName;
        ImageIcon image = new ImageIcon(imgLocation, "");

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(host);
        button.setOpaque(false);
        button.setBorderPainted(false);
        try {
            button.setIcon(image);
        } catch (Exception e) {
            button.setText(altText);
            host.showFeedback(e.getMessage());
        }

        return button;
    }
}
