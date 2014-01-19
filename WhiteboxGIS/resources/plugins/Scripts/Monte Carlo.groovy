def wd = pluginHost.getWorkingDirectory()
String[] args
def demFile = wd + "Vermont DEM.dep"

// Create a random grid
def outputFile = wd + "random grid2.dep"
def range = "2000.0" 
def numIterations = "500"
def fastMode = "true"
args = [demFile, outputFile, range, numIterations, fastMode]
pluginHost.runPlugin("TurningBands", args, false)

// Rescales the random grid to -10 to 10
def temp1File = wd + "random grid2.dep"
def randomGridFile = wd + "rescaled random grid.dep"
def newMinimum = "-10.0"
def newMaximum = "10.0"
args = [temp1File, randomGridFile, newMinimum, newMaximum]
pluginHost.runPlugin("RescaleImageValueRange", args, false)

// Add the error grid to the DEM
def demRealizationFile = wd + "dem realization.dep"
args = [demFile, randomGridFile, demRealizationFile]
pluginHost.runPlugin("Add", args, false)