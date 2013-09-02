package photogrammetry.util.model;

public interface HasCoordinates2d {

	public double getX();
	public double getY();
	public double distanceTo(HasCoordinates2d pt);
	
}
