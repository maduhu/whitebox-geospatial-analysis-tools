import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.geospatialfiles.shapefile.attributes.*
import whitebox.geospatialfiles.shapefile.ShapeFileRecord
import whitebox.utilities.Topology
import java.util.ArrayList

String inputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/Fall 2013/Lab materials/Lab5/Data/right tiepoints combined.shp"
ShapeFile input = new ShapeFile(inputFile)
AttributeTable table = new AttributeTable(inputFile.replace(".shp", ".dbf"))
ArrayList<com.vividsolutions.jts.geom.Geometry> pointList = new ArrayList<>()

double[][] point
Object[] recData
Coordinate c
GeometryFactory geomFactory = new GeometryFactory()
double x, y, z
int i = 0
for (ShapeFileRecord record : input.records) {
	point = record.getGeometry().getPoints()
	x = point[0][0]
	y = point[0][1]
	recData = table.getRecord(i)
	z = (Double)(recData[1])
	c = new Coordinate(x, y, z)
	pointList.add(geomFactory.createPoint(c))
	i++
}

com.vividsolutions.jts.geom.Geometry geom = geomFactory.buildGeometry(pointList)

DelaunayTriangulationBuilder dtb = new DelaunayTriangulationBuilder()
dtb.setSites(geom)
com.vividsolutions.jts.geom.Geometry polys = dtb.getTriangles(geomFactory)

// set up the output files of the shapefile and the dbf
String outputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/Fall 2013/Lab materials/Lab5/Data/right TIN.shp"
DBFField[] fields = new DBFField[1];
fields[0] = new DBFField();
fields[0].setName("FID");
fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
fields[0].setFieldLength(10);
fields[0].setDecimalCount(0);
ShapeFile output = new ShapeFile(outputFile, ShapeType.POLYGON, fields);
            
for (int a = 0; a < polys.getNumGeometries(); a++) {
    parentRecNum = 0;
    com.vividsolutions.jts.geom.Geometry g = polys.getGeometryN(a);
    if (g instanceof com.vividsolutions.jts.geom.Polygon) {
        com.vividsolutions.jts.geom.Polygon p = (com.vividsolutions.jts.geom.Polygon) g;
        ArrayList<ShapefilePoint> pnts = new ArrayList<>();
        int[] parts = new int[p.getNumInteriorRing() + 1];

        Coordinate[] buffCoords = p.getExteriorRing().getCoordinates();
        if (!Topology.isLineClosed(buffCoords)) {
            System.out.println("Exterior ring not closed.");
        }
        if (Topology.isClockwisePolygon(buffCoords)) {
            for (i = 0; i < buffCoords.length; i++) {
                pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
            }
        } else {
            for (i = buffCoords.length - 1; i >= 0; i--) {
                pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
            }
        }

        for (int b = 0; b < p.getNumInteriorRing(); b++) {
            parts[b + 1] = pnts.size();
            buffCoords = p.getInteriorRingN(b).getCoordinates();
            if (!Topology.isLineClosed(buffCoords)) {
                System.out.println("Interior ring not closed.");
            }
            if (Topology.isClockwisePolygon(buffCoords)) {
                for (i = buffCoords.length - 1; i >= 0; i--) {
                    pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
                }
            } else {
                for (i = 0; i < buffCoords.length; i++) {
                    pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y));
                }
            }
        }

        PointsList pl = new PointsList(pnts);

        whitebox.geospatialfiles.shapefile.Polygon wbPoly = new whitebox.geospatialfiles.shapefile.Polygon(parts, pl.getPointsArray());

        Object[] rowData = new Object[1]
        rowData[0] = new Double(1)
        output.addRecord(wbPoly, rowData);
    }
}

output.write()

pluginHost.returnData(outputFile)

println "I'm done"