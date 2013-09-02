package photogrammetry.util.model;

public interface HasCoordinates3d extends HasCoordinates2d {

	public double getZ();
	public double distanceTo(HasCoordinates3d pt);

}
