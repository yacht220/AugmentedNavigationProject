#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "Math3d.h"

static void m3dCrossProduct(float *result, float *u, float *v);
static void m3dNormalizeVector(float* u);
static float m3dGetVectorLength(float* u);
static float m3dGetVectorLengthSquared(float* u);
static void m3dScaleVector3(float* v, float scale);

//////////////////////////////////////////////////////////////////////////////////////
// Cross Product
// u x v = result
// We only need one version for floats, and one version for doubles. A 3 component
// vector fits in a 4 component vector. If  M3DVector4d or M3DVector4f are passed
// we will be OK because 4th component is not used.
static void m3dCrossProduct(float *result, float *u, float *v){
	result[0] = u[1]*v[2] - u[2]*v[1];
	result[1] = u[2]*v[0] - u[0]*v[2];
	result[2] = u[0]*v[1] - u[1]*v[0];
}

//////////////////////////////////////////////////////////////////////////////////////
// Normalize a vector
// Scale a vector to unit length. Easy, just scale the vector by it's length
static void m3dNormalizeVector(float* u){
	float scale, length;
	length=(float)sqrt((double)(u[0] * u[0]) + (u[1] * u[1]) + (u[2] * u[2]));
	scale=1.0f/length;
	u[0] *= scale; u[1] *= scale; u[2] *= scale;
}

////////////////////////////////////////////////////////////////////////////////////////
//// Normalize a vector
//// Scale a vector to unit length. Easy, just scale the vector by it's length
//static void m3dNormalizeVector(float* u)
//	{ m3dScaleVector3(u, 1.0f / m3dGetVectorLength(u)); }
//
////////////////////////////////////////////////////////////////////////////////////////
//// Get length of vector
//// Only for three component vectors.
//static float m3dGetVectorLength(float* u)
//	{ return (float)sqrt((double)(m3dGetVectorLengthSquared(u))); }
//
////////////////////////////////////////////////////////////////////////////////////////
//// Get Square of a vectors length
//// Only for three component vectors
//static float m3dGetVectorLengthSquared(float* u)
//	{ return (u[0] * u[0]) + (u[1] * u[1]) + (u[2] * u[2]); }
//
/////////////////////////////////////////////////////////////////////////////////////////
//// Scale Vectors (in place)
//static void m3dScaleVector3(float* v, float scale)
//{
//	v[0] *= scale; v[1] *= scale; v[2] *= scale;
//}

/*
 * Class:     com_yacht_android_augmentednavigation_Math3d
 * Method:    m3dGetPlaneEquationJni
 * Signature: ([F[F[F[F)V
 */
JNIEXPORT void JNICALL Java_com_yacht_android_augmentednavigation_Math3d_m3dGetPlaneEquationJni
  (JNIEnv *env, jclass obj, jfloatArray planeEq, jfloatArray p1, jfloatArray p2, jfloatArray p3){
	//float planeEq[4];

	// Get two vectors... do the cross product
	float v1[3];
	float v2[3];

	jfloat *planeEqBuf;
	jfloat *p1Buf;
	jfloat *p2Buf;
	jfloat *p3Buf;
	planeEqBuf = (*env)->GetFloatArrayElements(env, planeEq, 0);
	p1Buf = (*env)->GetFloatArrayElements(env, p1, 0);
	p2Buf = (*env)->GetFloatArrayElements(env, p2, 0);
	p3Buf = (*env)->GetFloatArrayElements(env, p3, 0);

	// V1 = p3 - p1
	v1[0] = p3Buf[0] - p1Buf[0];
	v1[1] = p3Buf[1] - p1Buf[1];
	v1[2] = p3Buf[2] - p1Buf[2];

	// V2 = P2 - p1
	v2[0] = p2Buf[0] - p1Buf[0];
	v2[1] = p2Buf[1] - p1Buf[1];
	v2[2] = p2Buf[2] - p1Buf[2];

	// Unit normal to plane - Not sure which is the best way here
	m3dCrossProduct((float *)planeEqBuf, v1, v2);
	m3dNormalizeVector((float *)planeEqBuf);
	// Back substitute to get D
	planeEqBuf[3] = -(planeEqBuf[0] * p3Buf[0] + planeEqBuf[1] * p3Buf[1] + planeEqBuf[2] * p3Buf[2]);

	(*env)->ReleaseFloatArrayElements(env, planeEq, planeEqBuf, 0);
	(*env)->ReleaseFloatArrayElements(env, p1, p1Buf, 0);
	(*env)->ReleaseFloatArrayElements(env, p2, p2Buf, 0);
	(*env)->ReleaseFloatArrayElements(env, p3, p3Buf, 0);
}

/*
 * Class:     com_yacht_android_augmentednavigation_Math3d
 * Method:    m3dMakePlanarShadowMatrixJni
 * Signature: ([F[F[F)V
 */
JNIEXPORT void JNICALL Java_com_yacht_android_augmentednavigation_Math3d_m3dMakePlanarShadowMatrixJni
  (JNIEnv *env, jclass obj, jfloatArray shadowMat, jfloatArray planeEq, jfloatArray vLightPos){
	jfloat *shadowMatBuf;
	jfloat *planeEqBuf;
	jfloat *vLightPosBuf;
	shadowMatBuf = (*env)->GetFloatArrayElements(env, shadowMat, 0);
	planeEqBuf = (*env)->GetFloatArrayElements(env, planeEq, 0);
	vLightPosBuf = (*env)->GetFloatArrayElements(env, vLightPos, 0);

	// These just make the code below easier to read. They will be
	// removed by the optimizer.
	float a = planeEqBuf[0];
	float b = planeEqBuf[1];
	float c = planeEqBuf[2];
	float d = planeEqBuf[3];

	float dx = vLightPosBuf[0]; // -vLightPos[0];
	float dy = vLightPosBuf[1]; // -vLightPos[0];
	float dz = vLightPosBuf[2]; // -vLightPos[0];

	// Now build the projection matrix
	shadowMatBuf[0] = b * dy + c * dz + d; //b * dy + c * dz;
	shadowMatBuf[1] = -a * dy;
	shadowMatBuf[2] = -a * dz;
	shadowMatBuf[3] = -a; //0.0f;

	shadowMatBuf[4] = -b * dx;
	shadowMatBuf[5] = a * dx + c * dz + d; //a * dx + c * dz;
	shadowMatBuf[6] = -b * dz;
	shadowMatBuf[7] = -b; //0.0f;

	shadowMatBuf[8] = -c * dx;
	shadowMatBuf[9] = -c * dy;
	shadowMatBuf[10] = a * dx + b * dy + d; //a * dx + b * dy;
	shadowMatBuf[11] = -c; //0.0f;

	shadowMatBuf[12] = -d * dx;
	shadowMatBuf[13] = -d * dy;
	shadowMatBuf[14] = -d * dz;
	shadowMatBuf[15] = a * dx + b * dy + c * dz;

	(*env)->ReleaseFloatArrayElements(env, shadowMat, shadowMatBuf, 0);
	(*env)->ReleaseFloatArrayElements(env, planeEq, planeEqBuf, 0);
	(*env)->ReleaseFloatArrayElements(env, vLightPos, vLightPosBuf, 0);
}
