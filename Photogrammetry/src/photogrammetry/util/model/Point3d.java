package photogrammetry.util.model;

public class Point3d extends Point2d implements HasCoordinates3d {

	public double z;

	/**
	 * Construct a new point.
	 * 
	 * @param x
	 *            the initial x coordinate.
	 * @param y
	 *            the initial y coordinate.
	 * @param z
	 *            the initial z coordinate.
	 */
	public Point3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * <p>
	 * Construct a new point.
	 * </p>
	 * <p>
	 * Initially, (x,y,z) will be (0,0,0).
	 * </p>
	 */
	public Point3d() {
	}

	@Override
	public double getZ() {
		return z;
	}

	@Override
	public double distanceTo(HasCoordinates2d pt) {
		double dx = x - pt.getX();
		double dy = y - pt.getY();
		double dz = z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public double distanceTo(HasCoordinates3d pt) {
		double dx = x - pt.getX();
		double dy = y - pt.getY();
		double dz = z - pt.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public String toString() {
		return toString(", ", true);
	}

	/**
	 * Create a string representation of this point.
	 * 
	 * @param separator
	 *            the separator to use between coordinates
	 * @param parentheses
	 *            whether or not to surround the coordinates with parentheses
	 * @return a string representation of this point.
	 */
	public String toString(String separator, boolean parentheses) {
		StringBuilder b = new StringBuilder(50);
		if (parentheses)
			b.append("(");
		b.append(x);
		b.append(separator);
		b.append(y);
		b.append(separator);
		b.append(z);
		if (parentheses)
			b.append(")");
		return b.toString();
	}

}
