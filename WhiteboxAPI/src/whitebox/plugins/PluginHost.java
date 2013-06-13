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
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.Communicator;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.ui.plugin_dialog.ToolDialog;
import java.util.ArrayList;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class PluginHost implements WhiteboxPluginHost, Communicator {

    private ArrayList<PluginInfo> plugInfo = null;
    private static PluginService pluginService = null;
    private String pathSep;
    public String pluginsDirectory = null;
    private String helpDirectory = null;
    private String applicationDirectory = null;
    
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
            
            pluginService = PluginServiceFactory.createPluginService(applicationDirectory); //pluginsDirectory);
            pluginService.initPlugins();
            plugInfo = pluginService.getPluginList();
            return true;
        } catch (Exception e) {
            return false;
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
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int showFeedback(String message, int optionType, int messageType) {
        System.err.println(message);
        return 0;
    }

    @Override
    public void updateProgress(String progressLabel, int progress) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateProgress(int progress) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void refreshMap(boolean updateLayersTab) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getWorkingDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getApplicationDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setApplicationDirectory(String applicationDirectory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getResourcesDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
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
}
