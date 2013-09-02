package edu.nus.cs4243.recon.model.linalg;

/**
 * A 3-dimensional vector.
 * 
 * @author johannes
 */
public class Vector3 {
	
	public double x, y, z;

	/**
	 * Constructs a new vector with coordinates (0,0,0)
	 */
	public Vector3() {
	}

	/**
	 * constructs a new vector with given coordinates.
	 * 
	 * @param x
	 *            the initial x coordinate
	 * @param y
	 *            the initial y coordinate
	 * @param z
	 *            the initial z coordinate
	 */
	public Vector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * <p>
	 * computes the length of the vector.
	 * </p>
	 * 
	 * @return the length of the vector
	 */
	public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * <p>
	 * normalize the vector, such that this.length() == 1.
	 * </p>
	 * <p>
	 * <code>normalize</code> first calculates <code>a = 1/length()</code> and then multiplies each
	 * coordinate with a. A call to <code>normalize</code> is equivalent to
	 * 
	 * <pre>
	 * scale(1 / length());
	 * </pre>
	 * 
	 * </p>
	 */
	public void normalize() {
		double lenr = 1 / length();
		x *= lenr;
		y *= lenr;
		z *= lenr;
	}

	/**
	 * <p>
	 * scales the vector by a given scaling factor.
	 * </p>
	 * <p>
	 * <code>scale</code> scales the vector by factor <code>k</code>
	 * </p>
	 * 
	 * @param k
	 *            the scaling factor
	 */
	public void scale(double k) {
		x *= k;
		y *= k;
		z *= k;
	}

	/**
	 * <p>
	 * computes the cross product of this vector and another vector.
	 * </p>
	 * <p>
	 * Let <code>a = (this.x, this.y, this.z)</code>, <code>b = (other.x, other.y, other.z)</code>
	 * and <code>c = (target.x, target.y, target.z)</code>. Then:
	 * </p>
	 * 
	 * <pre>
	 * 		cx   ay*bz - az*by  
	 * 		cy = az*bx - ax*bz
	 * 		cz   ax*by - ay*bx
	 * </pre>
	 * 
	 * @param other
	 *            the other vector
	 * @param target
	 *            a vector to write the cross product to. Must be != this and != other.
	 * @return the value passed as target
	 */
	public Vector3 cross(Vector3 other, Vector3 target) {
		target.x = y * other.z - z * other.y;
		target.y = z * other.x - x * other.z;
		target.z = x * other.y - y * other.x;
		return target;
	}

	/**
	 * <p>
	 * computs the dot product of this vector and another vector.
	 * </p>
	 * <p>
	 * A call to <code>vector.dot(other)</code> is equivalent to
	 * </p>
	 * 
	 * <pre>
	 * vector.x * other.x + vector.y * other.y + vector.z * other.z
	 * </pre>
	 * 
	 * @param other
	 * @return dot(this, other)
	 */
	public double dot(Vector3 other) {
		return x * other.x + y * other.y + z * other.z;
	}

	/**
	 * <p>
	 * subtract a vector from this vector
	 * </p>
	 * <p>
	 * A call to <code>vector.sub(other)</code> is equivalent to
	 * </p>
	 * 
	 * <pre>
	 * vector.x -= other.x;
	 * vector.y -= other.y;
	 * vector.z -= other.z;
	 * </pre>
	 * 
	 * @param other
	 *            the vector to subtract
	 */
	public void sub(Vector3 other) {
		x -= other.x;
		y -= other.y;
		z -= other.z;
	}

	/**
	 * <p>
	 * add a vector to this vector</b>
	 * </p>
	 * <p>
	 * A call to <code>vector.add(x,y,z)</code> is equivalent to
	 * </p>
	 * 
	 * <pre>
	 * vector.x += x;
	 * vector.y += y;
	 * vector.z += z;
	 * </pre>
	 * 
	 * @param x
	 *            the number to add to the x coordinate
	 * @param y
	 *            the number to add to the y coordinate
	 * @param z
	 *            the number to add to the z coordinate
	 */
	public void add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
	}

	/**
	 * <p>
	 * initialize the vector
	 * </p>
	 * <p>
	 * A call to <code>vector.set(x,y,z)</code> is equivalent to
	 * </p>
	 * 
	 * <pre>
	 * vector.x = x;
	 * vector.y = y;
	 * vector.z = z;
	 * </pre>
	 * 
	 * @param x
	 *            the number to set the x coordinate to
	 * @param y
	 *            the number to set the y coordinate to
	 * @param z
	 *            the number to set the z coordinate to
	 */
	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

}
