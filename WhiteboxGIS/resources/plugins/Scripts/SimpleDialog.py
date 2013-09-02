import os
from threading import Thread
from whitebox.ui.plugin_dialog import ScriptDialog
from java.awt.event import ActionListener

# The following four variables are required for this 
# script to be integrated into the tool tree panel. 
# Comment them out if you want to remove the script.
#name = "MyScript"
#descriptiveName = "My Script"
#description = "This tool processes counts the number of records in a shapefile."
#toolboxes = ["ConversionTools", "StatisticalTools"]
	
class MyScript(ActionListener):
	def __init__(self):
		self.sd = ScriptDialog(pluginHost, "My Script", self)	
		self.sd.setHelpFile("FilterMean")
		self.sd.setSourceFile(os.path.abspath(__file__))

		# add some components to the dialog
		self.sd.addDialogFile("Input file", "Input File:", "open", "Raster Files (*.dep), DEP", True, False)
		self.sd.addDialogFile("Output file", "Output File:", "close", "Raster Files (*.dep), DEP", True, False)
#		self.sd.addDialogMultiFile("This is a multifile input", "Choose some raster files:", "Raster Files (*.dep), DEP")
		self.sd.addDialogDataInput("Enter data here", "Enter a number", "1.3", True, False)
		self.sd.addDialogCheckBox("Do you want it to rain?", "Will it rain?", True)
#		self.sd.addDialogComboBox("Weather type", "Weather type:", ["rain", "snow", "sun", "hail"], 1)
#		self.sd.addDialogOption("This is an option", "Choose one:", "Plants", "Animals")
#		self.sd.addDialogFieldSelector("A field selector", "Choose a field", True)

		self.sd.setSize(800, 400)
		self.sd.visible = True
		
	def execute(self, args):
		try:
			for s in args:
				print s

			maxNum = 50000000
			oldProgress = -1
			k = 0
			for i in xrange(maxNum):
				k += 1
				progress = int(100.0 * float(k + 1) / maxNum)
				if progress > oldProgress:
					pluginHost.updateProgress(progress)
					oldProgress = progress
					if pluginHost.isRequestForOperationCancelSet():
						pluginHost.showFeedback("Operation cancelled")
						return

			pluginHost.updateProgress(0)
			print "Operation complete!"
			
		except:
			pluginHost.showFeedback("Error in script.")
			return

	def actionPerformed(self, event):
		if event.getActionCommand() == "ok":
			args = self.sd.collectParameters()
			t = Thread(target=lambda: self.execute(args))
			t.start()
			
		elif event.getActionCommand() == "close":
			print "The user cancelled the operation.

			
a = MyScript()
