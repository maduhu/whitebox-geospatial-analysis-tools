///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package whiteboxgis;
//
//import javax.swing.*;
//import javax.swing.tree.*;
//import java.awt.event.*;
////import java.awt.*;
//import java.io.*;
//import java.util.*;
//import java.util.Comparator;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import whitebox.interfaces.WhiteboxPlugin;
//import whitebox.interfaces.WhiteboxPluginHost;
//
///**
// *
// * @author john
// */
//public class wbToolbox extends JSplitPane {
//    private String toolboxFile = null;
//    private String graphicsDirectory = null;
//    private JList allTools;
//    private JList recentTools;
//    private JList mostUsedTools;
//    private JTree tree = null;
//    private JLabel toolDescriptionLabel = null;
//    private PluginService pluginService = null;
//    private WhiteboxPluginHost myHost = null;
//
//    public wbToolbox(String toolboxFile, String graphicsDirectory, PluginService pluginService, WhiteboxPluginHost host) {
//        this.toolboxFile = toolboxFile;
//        this.graphicsDirectory = graphicsDirectory;
//        this.pluginService = pluginService;
//        this.myHost = host;
//
//        initGui();
//    }
//
//    private void initGui() {
//        try {
//            // create the tool treeview
//            File file = new File(toolboxFile);
//
//            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            DocumentBuilder db = dbf.newDocumentBuilder();
//            Document doc = db.parse(file);
//            doc.getDocumentElement().normalize();
//            Node topNode = doc.getFirstChild();
//            tree = new JTree(populateTree(topNode));
//            toolDescriptionLabel = new JLabel();
//            //toolDescriptionLabel.setMinimumSize(new Dimension(0, 50));
//
//            MouseListener ml = new MouseAdapter() {
//                @Override
//                public void mousePressed(MouseEvent e) {
//                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
//                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
//                    String label;
//                    if (selRow != -1) {
//                        if (e.getClickCount() == 1) {
//                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) selPath.getLastPathComponent();
//                            if (n.getChildCount() == 0) {
//                                label = selPath.getLastPathComponent().toString();
//                                toolDescriptionLabel.setText(label);
//                            }
//                        } else if (e.getClickCount() == 2) {
//                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) selPath.getLastPathComponent();
//                            if (n.getChildCount() == 0) {
//                                label = selPath.getLastPathComponent().toString();
//                                myHost.launchDialog(label);
//
//                            }
//                        }
//                    }
//                }
//            };
//            tree.addMouseListener(ml);
//
//            ImageIcon leafIcon = new ImageIcon(graphicsDirectory + "tool.png", "");
//            ImageIcon stemIcon = new ImageIcon(graphicsDirectory + "opentools.png", "");
//            if (leafIcon != null && stemIcon != null) {
//                DefaultTreeCellRenderer renderer =
//                    new DefaultTreeCellRenderer();
//                renderer.setLeafIcon(leafIcon);
//                renderer.setClosedIcon(stemIcon);
//                renderer.setOpenIcon(stemIcon);
//                tree.setCellRenderer(renderer);
//            }
//
//            JScrollPane treeView = new JScrollPane(tree);
//
//            // create the quick launch
//            JTabbedPane qlTabs = new JTabbedPane();
//
//            DefaultListModel model = new DefaultListModel();
//            ArrayList<String> plugs = new ArrayList<String>();
//            Iterator<WhiteboxPlugin> iterator = pluginService.getPlugins();
//
//            while(iterator.hasNext()) {
//                WhiteboxPlugin plugin = iterator.next();
//                plugs.add(plugin.getDescriptiveName());
//            }
//            Collections.sort(plugs);
//            for (int i = 0; i < plugs.size(); i++) {
//                model.add(i, plugs.get(i));
//
//            }
//            allTools = new JList();
//            recentTools = new JList();
//            mostUsedTools = new JList();
//            recentTools.setModel(model);
//            allTools.setModel(model);
//            mostUsedTools.setModel(model);
//            JScrollPane scroller1 = new JScrollPane(allTools);
//            JScrollPane scroller2 = new JScrollPane(recentTools);
//            JScrollPane scroller3 = new JScrollPane(mostUsedTools);
//            qlTabs.insertTab("Recent", null, scroller2, "", 0);
//            qlTabs.insertTab("Most Used", null, scroller3, "", 1);
//            qlTabs.insertTab("All", null, scroller1, "", 2);
//
//            this.setOrientation(JSplitPane.VERTICAL_SPLIT);
//            this.add(treeView);
//            this.add(qlTabs);
//            //splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeView, qlTabs);
//            this.setDividerLocation(450);
//
//        } catch (Exception e) {
//            System.err.println(e.getStackTrace());
//        }
//    }
//
//    private DefaultMutableTreeNode populateTree(Node n) {
//        Element e = (Element) n;
//        String label = e.getAttribute("label");
//        String toolboxName = e.getAttribute("name");
//        DefaultMutableTreeNode result = new DefaultMutableTreeNode(label);
//
//        NodeList nodeList = n.getChildNodes();
//        for (int i = 0; i < nodeList.getLength(); i++) {
//            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
//                Element childElement = (Element) nodeList.item(i);
//                label = childElement.getAttribute("label");
//                toolboxName = childElement.getAttribute("name");
//                DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(label);
//
//                ArrayList<String> t = findToolsInToolbox(toolboxName);
//
//                if (nodeList.item(i).getFirstChild() != null) {
//                    NodeList childNodeList = nodeList.item(i).getChildNodes();
//                    for (int j = 0; j < childNodeList.getLength(); j++) {
//                        if (childNodeList.item(j).getNodeType() == Node.ELEMENT_NODE) {
//                            childTreeNode.add(populateTree(childNodeList.item(j)));
//                        }
//                    }
//
//                }
//
//                if (t.size() > 0) {
//                    for (int k = 0; k < t.size(); k++) {
//                        childTreeNode.add(new DefaultMutableTreeNode(t.get(k)));
//                    }
//                } else if (nodeList.item(i).getFirstChild() == null) {
//                    childTreeNode.add(new DefaultMutableTreeNode("No tools"));
//                }
//
//                result.add(childTreeNode);
//            }
//        }
//        if (nodeList.getLength() == 0) {
//            ArrayList<String> t = findToolsInToolbox(toolboxName);
//            if (t.size() > 0) {
//                for (int k = 0; k < t.size(); k++) {
//                    result.add(new DefaultMutableTreeNode(t.get(k)));
//                }
//            } else {
//                result.add(new DefaultMutableTreeNode("No tools"));
//            }
//        }
//
//        return result;
//
//    }
//
//    private ArrayList<String> findToolsInToolbox(String toolbox) {
//        Iterator<WhiteboxPlugin> iterator = pluginService.getPlugins();
//        ArrayList<String> plugs = new ArrayList<String>(); //pluginService.getPluginArrayList();
//
//        while (iterator.hasNext()) {
//            WhiteboxPlugin plugin = iterator.next();
//            String[] tb = plugin.getToolbox();
//            for (int i = 0; i < tb.length; i++) {
//                if (tb[i].equals(toolbox)) {
//                    plugs.add(plugin.getDescriptiveName());
//                }
//            }
//
//        }
//        Collections.sort(plugs, new SortIgnoreCase());
//        return plugs;
//    }
//    
//    public class SortIgnoreCase implements Comparator<Object> {
//        @Override
//        public int compare(Object o1, Object o2) {
//            String s1 = (String) o1;
//            String s2 = (String) o2;
//            return s1.toLowerCase().compareTo(s2.toLowerCase());
//        }
//    }
//}
