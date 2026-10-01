import {
  Arrow,
  Box,
  Cone,
  Curve,
  FunctionalExplicitSurface,
  Geometry,
  HalfSpace,
  InfinitePlane,
  ParametricBiCubicPatch,
  ParametricCurve,
  Polygon2D,
  PolyhedralBoundedSolid,
  QuadMesh,
  Solid,
  Sphere,
  Surface,
  Torus,
  TriangleMesh,
  TriangleMeshGroup,
  TriangleStripMesh,
  Volume,
  VoxelVolume,
  AmbientLight,
  DirectionalLight,
  PointLight,
  SpotLight,
} from '@vitral/base';

/**
 * Java's `object.getClass().getName()` for the geometries and lights of the
 * toolkit.
 *
 * A production bundle renames classes, so `constructor.name` is not the Java
 * name. The geometry classes the editor reports (i.e. in the description of
 * the selection, or the scene description of the agent API) are told apart
 * here with `instanceof`, subclasses first, and
 * named after the Java package of each class (the folder of its port).
 */
const JAVA_CLASS_NAMES: readonly [abstract new (...args: never[]) => object, string][] = [
  [Arrow, 'vsdk.toolkit.environment.geometry.volume.Arrow'],
  [Box, 'vsdk.toolkit.environment.geometry.volume.Box'],
  [Cone, 'vsdk.toolkit.environment.geometry.volume.Cone'],
  [InfinitePlane, 'vsdk.toolkit.environment.geometry.surface.InfinitePlane'],
  [PolyhedralBoundedSolid, 'vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid'],
  [Sphere, 'vsdk.toolkit.environment.geometry.volume.Sphere'],
  [Torus, 'vsdk.toolkit.environment.geometry.volume.Torus'],
  [VoxelVolume, 'vsdk.toolkit.environment.geometry.volume.VoxelVolume'],
  [FunctionalExplicitSurface, 'vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface'],
  [HalfSpace, 'vsdk.toolkit.environment.geometry.surface.HalfSpace'],
  [ParametricBiCubicPatch, 'vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch'],
  [ParametricCurve, 'vsdk.toolkit.environment.geometry.curve.ParametricCurve'],
  [Polygon2D, 'vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D'],
  [QuadMesh, 'vsdk.toolkit.environment.geometry.surface.QuadMesh'],
  [Solid, 'vsdk.toolkit.environment.geometry.volume.Solid'],
  [TriangleMesh, 'vsdk.toolkit.environment.geometry.surface.TriangleMesh'],
  [TriangleMeshGroup, 'vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup'],
  [TriangleStripMesh, 'vsdk.toolkit.environment.geometry.surface.TriangleStripMesh'],
  [Curve, 'vsdk.toolkit.environment.geometry.curve.Curve'],
  [Surface, 'vsdk.toolkit.environment.geometry.surface.Surface'],
  [Volume, 'vsdk.toolkit.environment.geometry.volume.Volume'],
  [Geometry, 'vsdk.toolkit.environment.geometry.Geometry'],
  [SpotLight, 'vsdk.toolkit.environment.light.SpotLight'],
  [PointLight, 'vsdk.toolkit.environment.light.PointLight'],
  [DirectionalLight, 'vsdk.toolkit.environment.light.DirectionalLight'],
  [AmbientLight, 'vsdk.toolkit.environment.light.AmbientLight'],
];

/**
 * @param object an object
 * @return the fully qualified name of its Java class, or the name of its
 * JavaScript class if it is not one of the known ones
 */
export function javaClassName(object: object): string {
  for (const [type, name] of JAVA_CLASS_NAMES) {
    if (object instanceof type) {
      return name;
    }
  }
  return object.constructor.name;
}

/**
 * @param object an object
 * @return Java's `object.getClass().getSimpleName()`
 */
export function javaSimpleClassName(object: object): string {
  const name: string = javaClassName(object);
  return name.substring(name.lastIndexOf('.') + 1);
}
