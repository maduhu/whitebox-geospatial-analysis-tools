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
package whitebox.plugins;

import java.awt.Font;
import java.io.BufferedReader;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.ui.plugin_dialog.ToolDialog;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import whitebox.interfaces.MapLayer;
import whitebox.utilities.FileUtilities;

/**
 * Serves as a basic PluginHost used for running plugin tools outside of the Whitebox GAT user interface.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PluginHost implements WhiteboxPluginHost {

    private ArrayList<PluginInfo> plugInfo = null;
    private static PluginService pluginService = null;
    private String pathSep;
    public String pluginsDirectory = null;
    private String helpDirectory = null;
    private String applicationDirectory = null;
    private String resourcesDirectory = null;
    private String workingDirectory = null;
    
    public PluginHost() {
        loadPlugins();
    }
    
    private boolean loadPlugins() {
        try {
            pathSep = File.separator;

            applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            //getClass().getProtectionDomain().
            if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                applicationDirectory = new File(applicationDirectory).getParent();
            } else {
                // Add the path to the class files
                applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                // Step one level up as we are only interested in the
                // directory containing the class files
                applicationDirectory = new File(applicationDirectory).getParent();
            }

            resourcesDirectory = applicationDirectory + pathSep + "resources" + pathSep;
            findFile(new File(new File(new File(applicationDirectory).getParent()).getParent()), "toolbox.xml");
            if (retFile != null && !retFile.isEmpty()) {
                resourcesDirectory = new File(retFile).getParent() + pathSep;
            }
            
            workingDirectory = resourcesDirectory + "samples" + pathSep;
    
            String seedDirectory = applicationDirectory;
            findPluginsDirectory(new File(seedDirectory));
            if (pluginsDirectory == null) {
                do {
                    int i = seedDirectory.lastIndexOf(pathSep);
                    seedDirectory = seedDirectory.substring(0, i);
                    findPluginsDirectory(new File(seedDirectory));
                } while (pluginsDirectory == null && seedDirectory.length() > 0);
            }
            
            seedDirectory = applicationDirectory;
            findHelpDirectory(new File(seedDirectory));
            if (helpDirectory == null) {
                do {
                    int i = seedDirectory.lastIndexOf(pathSep);
                    seedDirectory = seedDirectory.substring(0, i);
                    findHelpDirectory(new File(seedDirectory));
                } while (helpDirectory == null && seedDirectory.length() > 0);
            }
            
            pluginService = PluginServiceFactory.createPluginService(applicationDirectory);
            pluginService.initPlugins();
            plugInfo = pluginService.getPluginList();
            
            loadScripts();
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void loadScripts() {
        ArrayList<String> pythonScripts = FileUtilities.findAllFilesWithExtension(resourcesDirectory, ".py", true);
        ArrayList<String> groovyScripts = FileUtilities.findAllFilesWithExtension(resourcesDirectory, ".groovy", true);
        ArrayList<String> jsScripts = FileUtilities.findAllFilesWithExtension(resourcesDirectory, ".js", true);
        //ArrayList<PluginInfo> scriptPlugins = new ArrayList<>();
        for (String str : pythonScripts) {
            try {
                // Open the file
                FileInputStream fstream = new FileInputStream(str);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                //Read File Line By Line
                boolean containsName = false;
                boolean containsDescriptiveName = false;
                boolean containsDescription = false;
                boolean containsToolboxes = false;
                String name = "";
                String descriptiveName = "";
                String description = "";
                String[] toolboxes = null;
                while ((strLine = br.readLine()) != null
                        && (!containsName || !containsDescriptiveName
                        || !containsDescription || !containsToolboxes)) {
                    if (strLine.startsWith("name = \"")) {
                        containsName = true;
                        // now retreive the name
                        String[] str2 = strLine.split("=");
                        name = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.startsWith("descriptiveName = \"")) {
                        containsDescriptiveName = true;
                        String[] str2 = strLine.split("=");
                        descriptiveName = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.startsWith("description = \"")) {
                        containsDescription = true;
                        String[] str2 = strLine.split("=");
                        description = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.startsWith("toolboxes = [\"")) {
                        containsToolboxes = true;
                        String[] str2 = strLine.split("=");
                        toolboxes = str2[str2.length - 1].replace("\"", "").replace("\'", "").replace("[", "").replace("]", "").trim().split(",");
                        for (int i = 0; i < toolboxes.length; i++) {
                            toolboxes[i] = toolboxes[i].trim();
                        }
                    }
                }

                //Close the input stream
                br.close();

                if (containsName && containsDescriptiveName
                        && containsDescription && containsToolboxes) {
                    // it's a plugin!
                    PluginInfo pi = new PluginInfo(name, descriptiveName,
                            description, toolboxes, PluginInfo.SORT_MODE_NAMES);
                    pi.setScript(true);
                    pi.setScriptFile(str);
                    plugInfo.add(pi);
                    //scriptPlugins.add(pi);
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getStackTrace());
            }
        }

        for (String str : jsScripts) {
            try {
                // Open the file
                FileInputStream fstream = new FileInputStream(str);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                //Read File Line By Line
                boolean containsName = false;
                boolean containsDescriptiveName = false;
                boolean containsDescription = false;
                boolean containsToolboxes = false;
                String name = "";
                String descriptiveName = "";
                String description = "";
                String[] toolboxes = null;
                while ((strLine = br.readLine()) != null
                        && (!containsName || !containsDescriptiveName
                        || !containsDescription || !containsToolboxes)) {
                    if (strLine.toLowerCase().contains("name = \"")
                            && !strLine.toLowerCase().contains("descriptivename")) {
                        containsName = true;
                        // now retreive the name
                        String[] str2 = strLine.split("=");
                        name = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("descriptivename = \"")) {
                        containsDescriptiveName = true;
                        String[] str2 = strLine.split("=");
                        descriptiveName = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("description = \"")) {
                        containsDescription = true;
                        String[] str2 = strLine.split("=");
                        description = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("toolboxes = [\"")) {
                        containsToolboxes = true;
                        String[] str2 = strLine.split("=");
                        toolboxes = str2[str2.length - 1].replace("\"", "").replace("\'", "").replace("[", "").replace("]", "").trim().split(",");
                        for (int i = 0; i < toolboxes.length; i++) {
                            toolboxes[i] = toolboxes[i].trim();
                        }
                    }
                }

                //Close the input stream
                br.close();

                if (containsName && containsDescriptiveName
                        && containsDescription && containsToolboxes) {
                    // it's a plugin!
                    PluginInfo pi = new PluginInfo(name, descriptiveName,
                            description, toolboxes, PluginInfo.SORT_MODE_NAMES);
                    pi.setScript(true);
                    pi.setScriptFile(str);
                    plugInfo.add(pi);
                    //scriptPlugins.add(pi);
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getStackTrace());
            }
        }

        for (String str : groovyScripts) {
            try {
                // Open the file
                FileInputStream fstream = new FileInputStream(str);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                //Read File Line By Line
                boolean containsName = false;
                boolean containsDescriptiveName = false;
                boolean containsDescription = false;
                boolean containsToolboxes = false;
                String name = "";
                String descriptiveName = "";
                String description = "";
                String[] toolboxes = null;
                while ((strLine = br.readLine()) != null
                        && (!containsName || !containsDescriptiveName
                        || !containsDescription || !containsToolboxes)) {
                    if (strLine.toLowerCase().contains("name = \"")
                            && !strLine.toLowerCase().contains("descriptivename")) {
                        containsName = true;
                        // now retreive the name
                        String[] str2 = strLine.split("=");
                        name = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("descriptivename = \"")) {
                        containsDescriptiveName = true;
                        String[] str2 = strLine.split("=");
                        descriptiveName = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("description = \"")) {
                        containsDescription = true;
                        String[] str2 = strLine.split("=");
                        description = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("toolboxes = [\"")) {
                        containsToolboxes = true;
                        String[] str2 = strLine.split("=");
                        toolboxes = str2[str2.length - 1].replace("\"", "").replace("\'", "").replace("[", "").replace("]", "").trim().split(",");
                        for (int i = 0; i < toolboxes.length; i++) {
                            toolboxes[i] = toolboxes[i].trim();
                        }
                    }
                }

                //Close the input stream
                br.close();

                if (containsName && containsDescriptiveName
                        && containsDescription && containsToolboxes) {
                    // it's a plugin!
                    PluginInfo pi = new PluginInfo(name, descriptiveName,
                            description, toolboxes, PluginInfo.SORT_MODE_NAMES);
                    pi.setScript(true);
                    pi.setScriptFile(str);
                    plugInfo.add(pi);
                    //scriptPlugins.add(pi);
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getStackTrace());
            }
        }
    }
    
    @Override
    public List returnPluginList() {
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < plugInfo.size(); i++) {
            ret.add(plugInfo.get(i).getName());
        }
        Collections.sort(ret);
        return ret;
    }
    
    public int getNumberOfPlugins() {
        return plugInfo.size();
    }
    
    private void findPluginsDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                if (files[x].toString().endsWith(pathSep + "resources" + pathSep + "plugins")) {
                    pluginsDirectory = files[x].toString() + pathSep;
                    break;
                } else {
                    findPluginsDirectory(files[x]);
                }
            }
        }
    }
    
    private void findHelpDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                if (files[x].toString().endsWith(pathSep + "resources" + pathSep + "Help")) {
                    helpDirectory = files[x].toString() + pathSep;
                    break;
                } else {
                    findHelpDirectory(files[x]);
                }
            }
        }
    }
    
    @Override
    public void cancelOperation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void launchDialog(String pluginName) {
        WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.DESCRIPTIVE_NAME);
        String helpFile = helpDirectory + plug.getName() + ".html";
        ToolDialog dlg = new ToolDialog(this, true, plug.getName(), plug.getDescriptiveName(), helpFile);
        dlg.setSize(800, 400);
        dlg.setVisible(true);
        dlg.dispose();
    }

    @Override
    public void returnData(Object ret) {
        
    }
    
    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void runPlugin(String pluginName, String[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pluginComplete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int showFeedback(String message) {
        System.err.println(message);
        return 0;
    }

    @Override
    public int showFeedback(String message, int optionType, int messageType) {
        System.err.println(message);
        return 0;
    }

    @Override
    public void updateProgress(String progressLabel, int progress) {
        System.out.println(progressLabel + " " + progress + "%");
    }

    @Override
    public void updateProgress(int progress) {
        System.out.println(progress + "%");
    }

    @Override
    public void refreshMap(boolean updateLayersTab) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getApplicationDirectory() {
        return applicationDirectory;
    }

    @Override
    public void setApplicationDirectory(String applicationDirectory) {
        this.applicationDirectory = applicationDirectory;
    }

    @Override
    public String getResourcesDirectory() {
        return resourcesDirectory;
    }
    
    @Override
    public String getHelpDirectory() {
        return helpDirectory;
    }

    @Override
    public void editVector() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteFeature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    Font defaultFont = new Font("SanSerif", Font.PLAIN, 10);
    @Override
    public Font getDefaultFont() {
        return defaultFont;
    }

    @Override
    public String getLogDirectory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceBundle getGuiLabelsBundle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceBundle getMessageBundle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getLanguageCountryCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLanguageCountryCode(String code) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logException(String message, Exception e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logThrowable(String message, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    @Override
    public void logMessage(Level level, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private String retFile;
    private boolean flag = true;

    private void findFile(File dir, String fileName) {
        if (flag) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (files[i].getName().equals(fileName)) {
                        retFile = files[i].toString();
                        flag = false;
                        break;
                    } else {
                        findFile(files[i], fileName);
                    }
                } else if (files[i].getName().equals(fileName)) {
                    retFile = files[i].toString();
                    flag = false;
                    break;
                }
            }
        }
    }

    @Override
    public boolean isRequestForOperationCancelSet() {
        return false;
    }

    @Override
    public void resetRequestForOperationCancel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread, boolean suppressReturnedData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] getCurrentlyDisplayedFiles() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showHelp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showHelp(String helpFile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteLastNodeInFeature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSelectFeature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delectedAllFeaturesInActiveLayer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void saveSelection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MapLayer getActiveMapLayer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<MapLayer> getAllMapLayers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
