from java.io import File

# The following four variables are required for this 
# script to be integrated into the tool tree panel. 
name = "FeatureRequest"
descriptiveName = "New Feature Request"
description = "Send a request for a new feature (requires internet connection)"
toolboxes = ["topmost"]

# The following lines are necessary for the script 
# to be recognized as a menu extension.
parentMenu = "Help"
menuLabel = "New Feature Request"

pathSep = File.separator
helpDirectory = pluginHost.getHelpDirectory()
featureRequestFile = helpDirectory + "other" + pathSep + "FeatureRequest.html"

# This line just returns the html file within the help directory
# that contains the script for emailing the suggestion. Clearly 
# an internet connection is required.
pluginHost.returnData(featureRequestFile)
