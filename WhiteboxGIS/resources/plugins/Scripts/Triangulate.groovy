import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder
import whitebox.geospatialfiles.ShapeFile
import whitebox.geospatialfiles.shapefile.*
import whitebox.geospatialfiles.shapefile.attributes.*
import whitebox.geospatialfiles.shapefile.ShapeFileRecord
import whitebox.utilities.Topology
import java.util.ArrayList
import org.apache.commons.math3.geometry.euclidean.threed.*


double fx, fy, tanSlope, aspect, hillshade, z, x, y, d
double term1, term2, term3
double azimuth = Math.toRadians(315.0 - 90)
double altitude = Math.toRadians(30.0)
double sinTheta = Math.sin(altitude)
double cosTheta = Math.cos(altitude)

String inputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/Fall 2013/Lab materials/Lab5/Data/right tiepoints combined.shp"
ShapeFile input = new ShapeFile(inputFile)
AttributeTable table = new AttributeTable(inputFile.replace(".shp", ".dbf"))
ArrayList<com.vividsolutions.jts.geom.Geometry> pointList = new ArrayList<>()

double[][] point
Object[] recData
Coordinate c
GeometryFactory geomFactory = new GeometryFactory()
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
String outputFile = "/Users/johnlindsay/Documents/Teaching/GEOG2420/Fall 2013/Lab materials/Lab5/Data/right TIN2.shp"
DBFField[] fields = new DBFField[9];
fields[0] = new DBFField();
fields[0].setName("FID");
fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
fields[0].setFieldLength(10);
fields[0].setDecimalCount(0);

fields[1] = new DBFField();
fields[1].setName("EQ_AX");
fields[1].setDataType(DBFField.DBFDataType.FLOAT);
fields[1].setFieldLength(10);
fields[1].setDecimalCount(3);

fields[2] = new DBFField();
fields[2].setName("EQ_BY");
fields[2].setDataType(DBFField.DBFDataType.FLOAT);
fields[2].setFieldLength(10);
fields[2].setDecimalCount(3);

fields[3] = new DBFField();
fields[3].setName("EQ_CZ");
fields[3].setDataType(DBFField.DBFDataType.FLOAT);
fields[3].setFieldLength(10);
fields[3].setDecimalCount(3);

fields[4] = new DBFField();
fields[4].setName("EQ_D");
fields[4].setDataType(DBFField.DBFDataType.FLOAT);
fields[4].setFieldLength(10);
fields[4].setDecimalCount(3);

fields[5] = new DBFField();
fields[5].setName("SLOPE");
fields[5].setDataType(DBFField.DBFDataType.FLOAT);
fields[5].setFieldLength(10);
fields[5].setDecimalCount(3);

fields[6] = new DBFField();
fields[6].setName("ASPECT");
fields[6].setDataType(DBFField.DBFDataType.FLOAT);
fields[6].setFieldLength(10);
fields[6].setDecimalCount(3);

fields[7] = new DBFField();
fields[7].setName("HILLSHADE");
fields[7].setDataType(DBFField.DBFDataType.FLOAT);
fields[7].setFieldLength(10);
fields[7].setDecimalCount(3);

fields[8] = new DBFField();
fields[8].setName("CNTR_PT_Z");
fields[8].setDataType(DBFField.DBFDataType.FLOAT);
fields[8].setFieldLength(10);
fields[8].setDecimalCount(3);

ShapeFile output = new ShapeFile(outputFile, ShapeType.POLYGONZ, fields);

int FID = 1
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
                pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y, buffCoords[i].z, 0.0));
            }
        } else {
            for (i = buffCoords.length - 1; i >= 0; i--) {
                pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y, buffCoords[i].z, 0.0));
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
                    pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y, buffCoords[i].z, 0.0));
                }
            } else {
                for (i = 0; i < buffCoords.length; i++) {
                    pnts.add(new ShapefilePoint(buffCoords[i].x, buffCoords[i].y, buffCoords[i].z, 0.0));
                }
            }
        }

        PointsList pl = new PointsList(pnts);
		PolygonZ wbPoly = new PolygonZ(parts, pl.getPointsArray(), pl.getZArray());
		
		ShapefilePoint sfp = pl.getPoint(0)
		Vector3D pt1 = new Vector3D(sfp.x, sfp.y, sfp.z)
		sfp = pl.getPoint(1)
		Vector3D pt2 = new Vector3D(sfp.x, sfp.y, sfp.z)
		sfp = pl.getPoint(2)
		Vector3D pt3 = new Vector3D(sfp.x, sfp.y, sfp.z)
		Plane plane = new Plane(pt1, pt2, pt3)

		Vector3D normal = plane.getNormal()
		
		d = normal.getX() * sfp.x + normal.getY() * sfp.y + normal.getZ() * sfp.z
		
		if (normal.getZ() != 0) {
			fx = normal.getX() / normal.getZ()
			fy = normal.getY() / normal.getZ()
			if (fx != 0) {
	            tanSlope = Math.sqrt(fx * fx + fy * fy);
	            aspect = Math.toRadians(180 - Math.toDegrees(Math.atan(fy / fx)) + 90 * (fx / Math.abs(fx)))
//	        	println fx + " " + fy + " " + tanSlope + " " + aspect
	            term1 = tanSlope / Math.sqrt(1 + tanSlope * tanSlope);
	            term2 = sinTheta / tanSlope;
	            term3 = cosTheta * Math.sin(azimuth - aspect);
	            hillshade = term1 * (term2 - term3);
	        } else {
	            hillshade = 0.5;
	        }
	        hillshade = (int)(hillshade * 255);
	        if (hillshade < 0) {
	            hillshade = 0;
	        }
		} else {
			hillshade = 0.0
		}
		
		z = 0
		double[] zVals = pl.getZArray()
		for (int j in 0..(zVals.length - 2)) {
			z += zVals[j]
		}
		z = z / (zVals.length - 1)
		                
        Object[] rowData = new Object[9]
        rowData[0] = new Double(FID)
        rowData[1] = new Double(normal.getX())
        rowData[2] = new Double(normal.getY())
        rowData[3] = new Double(normal.getZ())
        rowData[4] = new Double(d)
        rowData[5] = new Double(Math.toDegrees(Math.atan(tanSlope)))
        rowData[6] = new Double(Math.toDegrees(aspect))
        rowData[7] = new Double(hillshade)
        rowData[8] = new Double(z)
        
        output.addRecord(wbPoly, rowData);
        FID++
    }
}

output.write()

pluginHost.returnData(outputFile)

println "Done!"
