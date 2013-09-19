import os
from shutil import copyfile
from threading import Thread
from whitebox.geospatialfiles import WhiteboxRaster
from whitebox.ui.plugin_dialog import ScriptDialog
from java.awt.event import ActionListener
from java.awt.event import ActionEvent

# The following four variables are required for this 
# script to be integrated into the tool tree panel. 
# Comment them out if you want to remove the script.
name = "Dissolve"
descriptiveName = "Dissolve"
description = "Removes interior boundaries within a vector coverage."
toolboxes = ["VectorTools"]
	
class Dissolve(ActionListener):
	def __init__(self, args):
		if len(args) != 0:
			t = Thread(target=lambda: self.execute(args))
			t.start()
		else:
			# Create a dialog for this tool to collect user-specified
			# tool parameters.
			self.sd = ScriptDialog(pluginHost, descriptiveName, self)	
		
			# Specifying the help file will display the html help
			# file in the help pane. This file should be be located 
			# in the help directory and have the same name as the 
			# class, with an html extension.
			helpFile = self.__class__.__name__
			self.sd.setHelpFile(helpFile)
		
			# Specifying the source file allows the 'view code' 
			# button on the tool dialog to be displayed.
			self.sd.setSourceFile(os.path.abspath(__file__))

			# add some components to the dialog
			self.sd.addDialogFile("Input file", "Input Vector Lines or Polygons File:", "open", "Vector Files (*.shp), SHP", True, False)
			self.sd.addDialogFile("Output file", "Output Vector File:", "save", "Vector Files (*.shp), SHP", True, False)

			# resize the dialog to the standard size and display it
			self.sd.setSize(800, 400)
			self.sd.visible = True
		
	def execute(self, args):
		try:
			inputfile = args[0]
			outputfile = args[1]

			# fill the depressions in the DEM, being sure not to 
			# run on a dedicated thread and supressing
			# the return data (automatically displayed image)
			args2 = [inputfile, outputfile, "0.0"]
			pluginHost.runPlugin("BufferVector", args2, False, True)

			# display the final output
			pluginHost.returnData(outputfile)
			
		except:
			pluginHost.showFeedback("Error during script execution.")
			return

	def actionPerformed(self, event):
		if event.getActionCommand() == "ok":
			args = self.sd.collectParameters()
			self.sd.dispose()
			t = Thread(target=lambda: self.execute(args))
			t.start()

if args is None:
	pluginHost.showFeedback("The arguments array has not been set.")
else:	
	Dissolve(args)
