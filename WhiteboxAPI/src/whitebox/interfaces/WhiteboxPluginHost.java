package whitebox.interfaces;

import java.util.List;
/**
 *
 * @author johnlindsay
 */
public interface WhiteboxPluginHost {
    
    public String getWorkingDirectory();
    
    public void setWorkingDirectory(String workingDirectory);
    
    public String getApplicationDirectory();
    
    public void setApplicationDirectory(String applicationDirectory);
    
    public String getResourcesDirectory();
    
    public List returnPluginList();
    /**
     * Used to cancel any currently running plugin.
     */
    public void cancelOperation();
    
    /**
     * Used to launch a plugin dialog box for retrieval of plugin parameters.
     * @param pluginName String containing the descriptive name of the plugin.
     */
    public void launchDialog(String pluginName);

    /**
     * Used to communicate a return object from a plugin tool to the main Whitebox user-interface.
     * @return Object, such as an output WhiteboxRaster.
     */
    public void returnData(Object ret);

    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     */
    public void runPlugin(String pluginName, String[] args);
    
    public void pluginComplete();

    /**
     * Used to communicate feedback pop-up messages between a plugin tool and the main Whitebox user-interface.
     * @param feedback String containing the text to display.
     */
    public int showFeedback(String message);
    
    public int showFeedback(String message, int optionType, int messageType);

    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    public void updateProgress(String progressLabel, int progress);

    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    public void updateProgress(int progress);
    
    /**
     * Used to refresh a displayed map.
     */
    public void refreshMap(boolean updateLayersTab);
}
