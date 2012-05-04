
package whitebox.interfaces;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author johnlindsay
 */
public interface WhiteboxPlugin extends Runnable {
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    public String[] getToolbox();

    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    public String getName();
    
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    public String getDescriptiveName();

    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    public String getToolDescription();

    public boolean isActive();
    
    /**
     * Used to run the plugin tool.
     * @param args Array of Strings containing the parameters used to run the plugin tool.
     */
    public void run();

    /**
     * Sets the arguments (parameters) used by the plugin.
     * @param args 
     */
    public void setArgs(String[] args);
    
    public void setCancelOp(boolean cancel);
    
    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the class
     * that the plugin will send all feedback messages, progress updates, and return objects.
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */
    public void setPluginHost(WhiteboxPluginHost host);
}
