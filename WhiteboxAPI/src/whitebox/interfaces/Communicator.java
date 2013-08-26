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

package whitebox.interfaces;

import java.util.ResourceBundle;
import java.util.logging.*;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public interface Communicator {
    public String getWorkingDirectory();
    
    public void setWorkingDirectory(String workingDirectory);
    
    public String getApplicationDirectory();
    
    public void setApplicationDirectory(String applicationDirectory);
    
    public String getResourcesDirectory();
    
    public String getLogDirectory();
    
    public int showFeedback(String message);
    
    public int showFeedback(String message, int optionType, int messageType);
    
    public ResourceBundle getGuiLabelsBundle();
    
    public ResourceBundle getMessageBundle();
    
    public void logException(String message, Exception e);
    
    public void logThrowable(String message, Throwable t);
    
    public void logMessage(Level level, String message);
    
    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     */
    public void runPlugin(String pluginName, String[] args);
    
    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     * @param runOnDedicatedThread  boolean value; set to true if the tool should 
     *        be run on a dedicated thread and false if it should be run on the 
     *        same thread as the calling Communicator.
     */
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread);
    
    
    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     * @param runOnDedicatedThread  boolean value; set to true if the tool should 
     *        be run on a dedicated thread and false if it should be run on the 
     *        same thread as the calling Communicator.
     * @param suppressReturnedData boolean value; set to true if the tool should
     *        be run without accepting any returned data (e.g. an automatically 
     *        displayed image). This is a useful parameter for scripting.
     */
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread, 
            boolean suppressReturnedData);
}
