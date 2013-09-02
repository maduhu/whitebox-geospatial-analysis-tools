package edu.nus.cs4243.recon.model.linalg;

public class Vector4 {

	public double x, y, z, w;

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + "," + w + ")";
	}
	
	public void scale(double f) {
		x *= f;
		y *= f;
		z *= f;
		w *= f;
	}

}
