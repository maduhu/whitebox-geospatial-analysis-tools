import whitebox.geospatialfiles.ShapeFile
import whitebox.utilities.FileUtilities

// This script example reads all the shapefiles in a specified directory and
// prints their names and the number of records contained within.

File dir = new File("/Users/johnlindsay/Documents/Research/Contracts/NRcan 2012/Data/")
def k = FileUtilities.findAllFilesWithExtension(dir, "shp", true)

for (str in k) {
    ShapeFile s = new ShapeFile(str)
    
    println s.getNumberOfRecords()
    println str
}