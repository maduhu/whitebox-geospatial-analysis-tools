/* This script shows how to select features in the active 
 * map layer (vector) based on attributes within the attribute 
 * table. */
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable
import whitebox.interfaces.MapLayer
import whitebox.geospatialfiles.VectorLayerInfo
import whitebox.interfaces.MapLayer.MapLayerType
import java.util.ArrayList

// Make sure that the 'select features' button is active, otherwise
// the selected features won't be displayed.
pluginHost.setSelectFeature()
// Retrieve the active map layer and see if it is a vector
def activeLayer = pluginHost.getActiveMapLayer()
if (activeLayer.getLayerType() == MapLayerType.VECTOR) {
	VectorLayerInfo vli = (VectorLayerInfo)activeLayer
	vli.clearSelectedFeatures()
	AttributeTable table = vli.getShapefile().getAttributeTable()
	Object[] rec;
	for (int i = 0; i < table.getNumberOfRecords(); i++) {
		rec = table.getRecord(i)
		// Get some attribute values, in this case a description 
		// field and an area field.
		String description = rec[1].toString().trim()
		double area = (double)rec[3]
		// Select the feature if it meets the specified criteria.
		if (description.equals("Fields") && area > 50000.0) {
			vli.selectFeature(i + 1) // uses the base-1 record number
		}
	}
	pluginHost.refreshMap(false)
} else {
	pluginHost.showFeedback("The active layer is not a vector")
}

println "Done!"
