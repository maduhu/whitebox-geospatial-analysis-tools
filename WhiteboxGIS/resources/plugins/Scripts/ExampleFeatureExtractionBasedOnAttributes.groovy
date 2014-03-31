import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.geospatialfiles.shapefile.attributes.DBFField
import whitebox.geospatialfiles.shapefile.attributes.*

def inputFile = pluginHost.getWorkingDirectory() + "landuse3.shp"
def outputFile = pluginHost.getWorkingDirectory() + "urban areas.shp"

ShapeFile shape = new ShapeFile(inputFile)

AttributeTable table = shape.getAttributeTable()
DBFField[] fields = table.getAllFields()

ShapeFile output = new ShapeFile(outputFile, shape.getShapeType(), fields)

int numRecords = shape.getNumberOfRecords()
String fieldName = "REV_CLASS"
int i = 0
Object[] rowData
ShapeFileRecord record
Geometry poly
int progress
int oldProgress = -1
for (int rec in 0..<numRecords) {
	String val = (String)table.getValue(rec, fieldName)
	if (val.contains("urban")) { 
		record = shape.getRecord(rec)
		poly = record.getGeometry()
		if (poly.getArea() / 10000.0 > 5) {
			rowData = table.getRecord(rec)
			output.addRecord(poly, rowData)
			i++ 
		}
	}

	progress = (int)(100f * rec / (numRecords - 1))
	if (progress != oldProgress) {
		pluginHost.updateProgress(progress)
		oldProgress = progress
	}
}

output.write()

println("${i} records found")
pluginHost.returnData(outputFile)
pluginHost.updateProgress(0)
