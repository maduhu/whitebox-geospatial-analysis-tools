package photogrammetry.util.model;

public class Point2d implements HasCoordinates2d {

	public double x, y;

	public Point2d() {
	}

	public Point2d(double x, double y) {
		set(x, y);
	}

	public Point2d(HasCoordinates2d other) {
		set(other);
	}

	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void set(HasCoordinates2d v) {
		this.x = v.getX();
		this.y = v.getY();
	}

	public void round() {
		x = Math.round(x);
		y = Math.round(y);
	}

	public void floor() {
		x = Math.floor(x);
		y = Math.floor(y);
	}

	public void ceil() {
		x = Math.ceil(x);
		y = Math.ceil(y);
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double distanceTo(HasCoordinates2d pt) {
		double dx = x - pt.getX();
		double dy = y - pt.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
