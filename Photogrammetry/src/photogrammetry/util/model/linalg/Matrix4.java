package edu.nus.cs4243.recon.model.linalg;

/**
 * Matrix class for 3d graphics. See respective man pages.
 * 
 * @author johannes
 */
public class Matrix4 {

	private double[] m = new double[16];

	public Matrix4() {
	}

	/**
	 * <p>
	 * replace the matrix with the identity matrix.
	 * </p>
	 * <p>
	 * <code>loadIdentity</code> replaces the matrix with the identity matrix:
	 * </p>
	 * 
	 * <pre>
	 *           1 0 0 0
	 *           0 1 0 0
	 *           0 0 1 0
	 *           0 0 0 1
	 * </pre>
	 */
	public void loadIdentity() {
		for (int i = 0; i < 16; i++) {
			int row = i / 4;
			int col = i % 4;
			m[i] = row == col ? 1 : 0;
		}
	}

	/**
	 * <p>
	 * multiply the matrix by a translation matrix.
	 * </p>
	 * <p>
	 * <code>translate</code> produces a translation by (x,y,z). The matrix is multiplied by this
	 * translation matrix, with the product replacing the matrix, as if <code>multMatrix</code> were
	 * called with the following matrix for its argument:
	 * </p>
	 * 
	 * <pre>
	 *           1 0 0 x
	 *           0 1 0 y
	 *           0 0 1 z
	 *           0 0 0 1
	 * </pre>
	 * 
	 * This function performs 9 adds and 9 muls.
	 * 
	 * @param x
	 *            Specifies the x coordinate of a translation vector.
	 * @param y
	 *            Specifies the x coordinate of a translation vector.
	 * @param z
	 *            Specifies the x coordinate of a translation vector.
	 */
	public void translate(double x, double y, double z) {
		for (int i = 0; i <= 8; i += 4) {
			m[i + 3] += m[i] * x + m[i + 1] * y + m[i + 2] * z;
		}
	}

	/**
	 * <p>
	 * define a viewing transformation.
	 * </p>
	 * <p>
	 * lookAt creates a viewing matrix derived from an eye point, a reference point indicating the
	 * center of the scene, and an UP vector.
	 * </p>
	 * <p>
	 * The matrix maps the reference point to the negative z axis and the eye point to the origin.
	 * When a typical projection matrix is used, the center of the scene therefore maps to the
	 * center of the viewport. Similarly, the direction described by the UP vector projected onto
	 * the viewing plane is mapped to the positive y axis so that it points upward in the viewport.
	 * The UP vector must not be parallel to the line of sight from the eye point to the reference
	 * point.
	 * </p>
	 * <p>
	 * Let
	 * 
	 * <pre>
	 *             centerX - eyeX
	 *         F = centerY - eyeY
	 *             centerZ - eyeZ
	 * </pre>
	 * 
	 * </p>
	 * <p>
	 * Let UP be the vector (upX, upY, upZ).
	 * </p>
	 * <p>
	 * Then normalize as follows: <code>f = F/ || F ||</code>
	 * </p>
	 * <p>
	 * <code>UP' = UP/|| UP ||</code>
	 * </p>
	 * <p>
	 * Finally, let <code>s = f X UP'</code>, and <code>u = s X f</code>.
	 * </p>
	 * <p>
	 * M is then constructed as follows:
	 * </p>
	 * 
	 * <pre>
	 *        s[0]    s[1]    s[2]    0
	 *        u[0]    u[1]    u[2]    0
	 *   M = -f[0]   -f[1]   -f[2]    0
	 *         0       0       0      1
	 * </pre>
	 * 
	 * and lookAt is equivalent to
	 * 
	 * <pre>
	 * multMatrixf(M);
	 * translate(-eyex, -eyey, -eyez);
	 * </pre>
	 * 
	 * @param eyeX
	 *            Specifies the x coordinate of the eye.
	 * @param eyeY
	 *            Specifies the y coordinate of the eye.
	 * @param eyeZ
	 *            Specifies the z coordinate of the eye.
	 * @param centerX
	 *            Specifies the x coordinate of the point that the camera looks at.
	 * @param centerY
	 *            Specifies the y coordinate of the point that the camera looks at.
	 * @param centerZ
	 *            Specifies the z coordinate of the point that the camera looks at.
	 * @param upX
	 *            Specifies the x coordinate of the up vector.
	 * @param upY
	 *            Specifies the y coordinate of the up vector.
	 * @param upZ
	 *            Specifies the z coordinate of the up vector.
	 */
	public void lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY,
			double centerZ, double upX, double upY, double upZ) {
		Vector3 f = new Vector3(centerX - eyeX, centerY - eyeY, centerZ - eyeZ), up = new Vector3(
				upX, upY, upZ);
		f.normalize();
		up.normalize();
		Vector3 s = f.cross(up, new Vector3()), u = s.cross(f, new Vector3());
		s.normalize();
		u.normalize();
		for (int i = 0; i <= 12; i += 4) {
			double temp1 = m[i], temp2 = m[i + 1], temp3 = m[i + 2];
			m[i + 0] = temp1 * s.x + temp2 * u.x - temp3 * f.x;
			m[i + 1] = temp1 * s.y + temp2 * u.y - temp3 * f.y;
			m[i + 2] = temp1 * s.z + temp2 * u.z - temp3 * f.z;
		}

		translate(-eyeX, -eyeY, -eyeZ);
	}

	/**
	 * <p>
	 * set up a perspective projection matrix
	 * </p>
	 * <p>
	 * <code>perspective</code> specifies a viewing frustum into the world coordinate system. In
	 * general, the aspect ratio in <code>perspective</code> should match the aspect ratio of the
	 * associated viewport. For example, <code>aspect = 2.0</code> means the viewer's angle of view
	 * is twice as wide in x as it is in y. If the viewport is twice as wide as it is tall, it
	 * displays the image without distortion.
	 * </p>
	 * <p>
	 * The matrix generated by perspective is multipled by the current matrix, just as if
	 * <code>multMatrix</code> were called with the generated matrix. To load the perspective matrix
	 * onto the current matrix stack instead, precede the call to <code>perspective</code> with a
	 * call to <code>loadIdentity</code>.
	 * <p/p>
	 * <p>
	 * Given f defined as follows:
	 * </p>
	 * 
	 * <pre>
	 * f = cotangent(fovy / 2)
	 * </pre>
	 * <p>
	 * The generated matrix is
	 * </p>
	 * 
	 * <pre>
	 *              f
	 *         ------------       0              0              0
	 *            aspect
	 * 
	 *             0              f              0              0
	 * 
	 *                                       zFar+zNear    2*zFar*zNear
	 *             0              0          ----------    ------------
	 *                                       zNear-zFar     zNear-zFar
	 * 
	 *             0              0              -1             0
	 * </pre>
	 * 
	 * Note: Depth buffer precision is affected by the values specified for <code>zNear</code> and
	 * <code>zFar</code>. The greater the ratio of <code>zFar</code> to <code>zNear</code> is, the
	 * less effective the depth buffer will be at distinguishing between surfaces that are near each
	 * other. If
	 * 
	 * <pre>
	 * r = zFar / zNear
	 * </pre>
	 * 
	 * roughly <code>log2(r)</Code> bits of depth buffer precision are lost. Because <code>r</code>
	 * approaches infinity as <code>zNear</code> approaches 0, <code>zNear</code> must never be set
	 * to 0.
	 * 
	 * @param fovy
	 *            Specifies the field of view angle (radian), in the y direction.
	 * @param aspect
	 *            Specifies the aspect ratio that determines the field of view in the x direction.
	 *            The aspect ratio is the ratio of x (width) to y (height).
	 * @param zNear
	 *            Specifies the distance from the viewer to the near clipping plane (always
	 *            positive).
	 * @param zFar
	 *            Specifies the distance from the viewer to the far clipping plane (always
	 *            positive).
	 */
	public void perspective(double fovy, double aspect, double zNear, double zFar) {
		double m33 = (zFar + zNear) / (zNear - zFar);
		double m34 = 2 * zFar * zNear / (zNear - zFar);
		double f = 1 / Math.tan(fovy / 2);
		double fa = f / aspect;
		for (int i = 0; i <= 12; i += 4) {
			m[i + 0] *= fa;
			m[i + 1] *= f;
			double temp3 = m[i + 2];
			m[i + 2] = temp3 * m33 - m[i + 3];
			m[i + 3] = temp3 * m34;
		}
	}

	/**
	 * Calculate <code>v = this * v</code>
	 * 
	 * @param v
	 *            the vector to multiply this matrix with
	 */
	public void mul(Vector4 v) {
		double x = v.x, y = v.y, z = v.z, w = v.w;
		v.x = m[0] * x + m[1] * y + m[2] * z + m[3] * w;
		v.y = m[4] * x + m[5] * y + m[6] * z + m[7] * w;
		v.z = m[8] * x + m[9] * y + m[10] * z + m[11] * w;
		v.w = m[12] * x + m[13] * y + m[14] * z + m[15] * w;
	}

}
