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
    
    public int showFeedback(String message);
    
    public int showFeedback(String message, int optionType, int messageType);

    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     */
    public void runPlugin(String pluginName, String[] args);

}
