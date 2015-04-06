package com.yacht.android.augmentednavigation;

public class Math3d {
	static{
		System.loadLibrary("Math3d");
	}
/////////////////////////////////////////////////////////////////////////////////////////
	// Calculate the plane equation of the plane that the three specified points lay in. The
	// points are given in clockwise winding order, with normal pointing out of clockwise face
	// planeEq contains the A,B,C, and D of the plane equation coefficients
	public static float[] m3dGetPlaneEquation( float[] p1, float[] p2, float[] p3){
		// 返回值
		float[] planeEq = new float[4];
		
		// Get two vectors... do the cross product
		float[] v1 = new float[3];
		float[] v2 = new float[3];

	    // V1 = p3 - p1
	    v1[0] = p3[0] - p1[0];
	    v1[1] = p3[1] - p1[1];
	    v1[2] = p3[2] - p1[2];

	    // V2 = P2 - p1
	    v2[0] = p2[0] - p1[0];
	    v2[1] = p2[1] - p1[1];
	    v2[2] = p2[2] - p1[2];

	    // Unit normal to plane - Not sure which is the best way here
	    planeEq = m3dCrossProduct(v1, v2);
	    planeEq = m3dNormalizeVector(planeEq);
	    // Back substitute to get D
	    planeEq[3] = -(planeEq[0] * p3[0] + planeEq[1] * p3[1] + planeEq[2] * p3[2]);
	    
	    return planeEq;
	}
	
	public static native void m3dGetPlaneEquationJni( float[] planeEq, float[] p1, float[] p2, float[] p3);
	
//////////////////////////////////////////////////////////////////////////////////////
	// Cross Product
	// u x v = result
	// We only need one version for floats, and one version for doubles. A 3 component
	// vector fits in a 4 component vector. If  M3DVector4d or M3DVector4f are passed
	// we will be OK because 4th component is not used.
	public static float[] m3dCrossProduct(float[] u, float[] v){
		// 返回值
		float[] result = new float[4];
		
		result[0] = u[1]*v[2] - v[1]*u[2]; 
		result[1] = -u[0]*v[2] + v[0]*u[2]; 
		result[2] = u[0]*v[1] - v[0]*u[1];
		
		return result;
	}
	
//////////////////////////////////////////////////////////////////////////////////////
	// Normalize a vector
	// Scale a vector to unit length. Easy, just scale the vector by it's length
	public static float[] m3dNormalizeVector(float[] u)
		{ return m3dScaleVector3(u, 1.0f / m3dGetVectorLength(u)); }
	
//////////////////////////////////////////////////////////////////////////////////////
	// Get length of vector
	// Only for three component vectors.
	public static float m3dGetVectorLength(float[] u)
		{ return (float)Math.sqrt((double)(m3dGetVectorLengthSquared(u))); }
	
//////////////////////////////////////////////////////////////////////////////////////
	// Get Square of a vectors length
	// Only for three component vectors
	public static float m3dGetVectorLengthSquared(float[] u)
		{ return (u[0] * u[0]) + (u[1] * u[1]) + (u[2] * u[2]); }
	
///////////////////////////////////////////////////////////////////////////////////////
	// Scale Vectors (in place)
	public static float[] m3dScaleVector3(float[] v, float scale) 
	{		
		v[0] *= scale; v[1] *= scale; v[2] *= scale; 
		return v; 
	}
	
///////////////////////////////////////////////////////////////////////////
	// Create a projection to "squish" an object into the plane.
	// Use m3dGetPlaneEquationf(planeEq, point1, point2, point3);
	// to get a plane equation.
	public static float[] m3dMakePlanarShadowMatrix(float[] planeEq, float[] vLightPos)
	{
		// 返回值
		float[] proj = new float[16];
		
		// These just make the code below easier to read. They will be 
		// removed by the optimizer.	
		float a = planeEq[0];
		float b = planeEq[1];
		float c = planeEq[2];
		float d = planeEq[3];

		float dx = vLightPos[0]; // -vLightPos[0];
		float dy = vLightPos[1]; // -vLightPos[0];
		float dz = vLightPos[2]; // -vLightPos[0];

		// Now build the projection matrix
		proj[0] = b * dy + c * dz + d; //b * dy + c * dz;
		proj[1] = -a * dy;
		proj[2] = -a * dz;
		proj[3] = -a; //0.0f;

		proj[4] = -b * dx;
		proj[5] = a * dx + c * dz + d; //a * dx + c * dz;
		proj[6] = -b * dz;
		proj[7] = -b; //0.0f;

		proj[8] = -c * dx;
		proj[9] = -c * dy;
		proj[10] = a * dx + b * dy + d; //a * dx + b * dy;
		proj[11] = -c; //0.0f;

		proj[12] = -d * dx;
		proj[13] = -d * dy;
		proj[14] = -d * dz;
		proj[15] = a * dx + b * dy + c * dz;
		// Shadow matrix ready
		
		return proj;
	}
	
	public static native void m3dMakePlanarShadowMatrixJni(float[] shadowMat, float[] planeEq, float[] vLightPos);
}
