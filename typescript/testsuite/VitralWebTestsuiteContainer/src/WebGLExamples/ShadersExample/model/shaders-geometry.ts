/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/model/ShadersGeometry.java`
 * (cpp `ShadersGeometry.h`).
 *
 * Geometry shown by the example. SPHERE keeps the textured, bump mapped
 * sphere; the others are `ShadersGeometryFixture` scenes. As with
 * `ShaderOperationMode`, the Java `enum` is a union of its constant names.
 */
export type ShadersGeometry = 'SPHERE' | 'QUARTIC' | 'BLOB' | 'HEIGHT_FIELD' | 'CSG' | 'PATCH';
