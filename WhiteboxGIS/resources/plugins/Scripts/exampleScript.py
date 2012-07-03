# Retrieves the available plugins
# plugs = PluginHost.returnPluginList()
# print("Number of Plugins: " + str(plugs.size()))
# print("List of Plugins:")
# for a in plugs:
#     print(a)

# Sets the working directory
wd = "/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab5/Data/2011/"
wd2 = "/Users/johnlindsay/Documents/Teaching/GEOG3420/Winter 2012/Labs/Lab5/Data/1992/"
#PluginHost.setWorkingDirectory(wd)

# Create the base rasters

# Set the arguments
baseFile = wd2 + "band1.dep"
outputFile = wd + "band2.dep"
initialValue = "NoData"
dataType = "float"
args = [baseFile, outputFile, initialValue, dataType]
PluginHost.runPlugin("NewRasterFromBase", args)

# Set the arguments
outputFile = wd + "band1.dep"
args = [baseFile, outputFile, initialValue, dataType]
PluginHost.runPlugin("NewRasterFromBase", args)

# Set the arguments
outputFile = wd + "band3.dep"
args = [baseFile, outputFile, initialValue, dataType]
PluginHost.runPlugin("NewRasterFromBase", args)

# Set the arguments
outputFile = wd + "band4.dep"
args = [baseFile, outputFile, initialValue, dataType]
PluginHost.runPlugin("NewRasterFromBase", args)

# Set the arguments
outputFile = wd + "band5.dep"
args = [baseFile, outputFile, initialValue, dataType]
PluginHost.runPlugin("NewRasterFromBase", args)

# Set the arguments
outputFile = wd + "band7.dep"
args = [baseFile, outputFile, initialValue, dataType]
PluginHost.runPlugin("NewRasterFromBase", args)

# Resample into the grids
inputFile = wd + "L5018030_03020110608_B10.dep"
destinationFile = wd + "band1.dep"
resampleAlgorithm = "nearest neighbour"
args = [inputFile, destinationFile, resampleAlgorithm]
PluginHost.runPlugin("Resample", args)

inputFile = wd + "L5018030_03020110608_B20.dep"
destinationFile = wd + "band2.dep"
args = [inputFile, destinationFile, resampleAlgorithm]
PluginHost.runPlugin("Resample", args)

inputFile = wd + "L5018030_03020110608_B30.dep"
destinationFile = wd + "band3.dep"
args = [inputFile, destinationFile, resampleAlgorithm]
PluginHost.runPlugin("Resample", args)

inputFile = wd + "L5018030_03020110608_B40.dep"
destinationFile = wd + "band4.dep"
args = [inputFile, destinationFile, resampleAlgorithm]
PluginHost.runPlugin("Resample", args)

inputFile = wd + "L5018030_03020110608_B50.dep"
destinationFile = wd + "band5.dep"
args = [inputFile, destinationFile, resampleAlgorithm]
PluginHost.runPlugin("Resample", args)

inputFile = wd + "L5018030_03020110608_B70.dep"
destinationFile = wd + "band7.dep"
args = [inputFile, destinationFile, resampleAlgorithm]
PluginHost.runPlugin("Resample", args)

print("Operation complete!")

# NewRasterFromBase
