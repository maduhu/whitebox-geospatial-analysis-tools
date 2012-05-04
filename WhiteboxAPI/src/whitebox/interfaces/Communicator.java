package whitebox.interfaces;

/**
 *
 * @author johnlindsay
 */
public interface Communicator {
    public String getWorkingDirectory();
    
    public void setWorkingDirectory(String workingDirectory);
    
    public String getApplicationDirectory();
    
    public void setApplicationDirectory(String applicationDirectory);
    
    public String getResourcesDirectory();
    
    public int showFeedback(String message);
    
    public int showFeedback(String message, int optionType, int messageType);

    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     */
    public void runPlugin(String pluginName, String[] args);

}
