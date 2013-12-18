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
package whiteboxgis.user_interfaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import whitebox.interfaces.WhiteboxPluginHost;


/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class AboutWhitebox extends JDialog implements ActionListener {

    private String graphicsDirectory = "";
    private String versionNumber = "";
    private String versionName = "";
    private ResourceBundle bundle;
    
    public AboutWhitebox(Frame owner, boolean modal, String graphicsDirectory, 
            String versionName, String versionNumber) {
        super(owner, modal);
        if (bundle != null) {
            this.setTitle(bundle.getString("About") + " Whitebox GAT");
        } else {
            this.setTitle("About Whitebox GAT");
        }
        this.graphicsDirectory = graphicsDirectory;
        this.versionName = versionName;
        this.versionNumber = versionNumber;
        if (owner instanceof WhiteboxPluginHost) {
            WhiteboxPluginHost host = (WhiteboxPluginHost) owner;
            this.bundle = host.getGuiLabelsBundle();
        }
        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        mainPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        
        File file = new File(graphicsDirectory + "WhiteboxLogo.png");
        if (!file.exists()) {
            return;
        }
        ImagePanel imagePane = new ImagePanel(file.toString());
        
        mainPane.add(imagePane);
        mainPane.add(Box.createVerticalStrut(10));
        
        JLabel label4 = new JLabel(versionName + " (" + versionNumber + ") released 2014");
        Box box4 = Box.createHorizontalBox();
        box4.add(Box.createHorizontalGlue());
        box4.add(label4);
        box4.add(Box.createHorizontalGlue());
        mainPane.add(box4);
        mainPane.add(Box.createVerticalStrut(10));
        
        
        JTextArea contributors = new JTextArea();
        JScrollPane scroll = new JScrollPane(contributors);
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Dr. John Lindsay (Lead Developer)\n");
        strBuilder.append("Department of Geography\n");
        strBuilder.append("The University of Guelph, Canada\n");
        strBuilder.append("e-mail: jlindsay@uoguelph.ca\n\n");
        
        strBuilder.append(bundle.getString("i18nContributors")).append("\n\n");
        //strBuilder.append("Heikki Doeleman\n");
        strBuilder.append("Carlo Alberto Brunori (cab)\n");
        strBuilder.append("Agustin Diez Castillo\n");
        strBuilder.append("Sergi Gumà\n");
        strBuilder.append("Hu Xuemei\n");
        strBuilder.append("Annie C. Laviolette\n");
        strBuilder.append("George Miliaresis\n");
        strBuilder.append("Andreas Paukner-Ruzicka\n");
        strBuilder.append("Hannes Reuter\n");
        strBuilder.append("Raf Roset\n");
        strBuilder.append("Ehsan Roshani\n");
        strBuilder.append("Evgenia Selezneva\n");
        strBuilder.append("Rafal Wawer\n");
        strBuilder.append("秦承志 (QIN Cheng-Zhi)\n");
        contributors.setText(strBuilder.toString());
        contributors.setLineWrap(true);
        contributors.setWrapStyleWord(true);
        contributors.setCaretPosition(0);
        contributors.setEditable(false);
        mainPane.add(scroll);
        
        mainPane.add(Box.createVerticalStrut(10));
        

        // buttons
        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setActionCommand("ok");

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));

        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(ok);
        buttonPane.add(Box.createHorizontalGlue());

        Container contentPane = getContentPane();
        contentPane.add(mainPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        pack();

        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("ok")) {
            this.dispose();
        }
    }
    
    
    class ImagePanel extends JPanel {

        private BufferedImage image;

        public ImagePanel(String fileName) {
            try {
                image = ImageIO.read(new File(fileName));
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                setMinimumSize(new Dimension(image.getWidth(), image.getHeight()));
                setMaximumSize(new Dimension(image.getWidth(), image.getHeight()));
                
            } catch (IOException ex) {
                // handle exception...
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);

        }
    }
}
