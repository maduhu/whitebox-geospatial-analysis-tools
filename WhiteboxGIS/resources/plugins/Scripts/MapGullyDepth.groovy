import whitebox.geospatialfiles.WhiteboxRaster
import groovy.time.*

String[] args

def wd = pluginHost.getWorkingDirectory()
def demFile = wd + "greater ung ir.dep"

// calculate the difference from mean elevation
def start = new Date()
def dfmeFile = wd + "dfme.dep"
def neighbourhoodSize = "15"
args = [demFile, dfmeFile, neighbourhoodSize]
pluginHost.runPlugin("DifferenceFromMeanElevation", args, false, true)
println "DifferenceFromMeanElevation: " + TimeCategory.minus(new Date(), start)

// find areas lower than the mean elevation
start = new Date()
def lowAreasFile = wd + "low areas.dep"
args = [dfmeFile, "0.0", lowAreasFile]
pluginHost.runPlugin("LessThan", args, false, true)
println "LessThan: " + TimeCategory.minus(new Date(), start)

// calculate the plan curvature on a smoothed DEM and find 
// areas of high plan curv.
start = new Date()
def smoothDEMFile = wd + "smooth DEM.dep" 
def xDim = "9" 
def yDim = "9" 
def rounded = "true" 
def reflectEdges = "true" 
args = [demFile, smoothDEMFile, xDim, yDim, rounded, reflectEdges] 
pluginHost.runPlugin("FilterMean", args, false, true)
println "FilterMean: " + TimeCategory.minus(new Date(), start)

start = new Date()
def planCurvFile = wd + "plan curv.dep" 
args = [smoothDEMFile, planCurvFile, "1.0"]
pluginHost.runPlugin("PlanCurv", args, false, true)
println "PlanCurv: " + TimeCategory.minus(new Date(), start)

start = new Date()
def highPlanCurvFile = wd + "high plan curv.dep"
args = [planCurvFile, "5.0", highPlanCurvFile]
pluginHost.runPlugin("GreaterThan", args, false, false)
println "GreaterThan: " + TimeCategory.minus(new Date(), start)

println "Done!"
