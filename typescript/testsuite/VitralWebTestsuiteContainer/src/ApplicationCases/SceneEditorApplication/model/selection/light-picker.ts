import { LightGizmoOmniBillboard, type Camera, type Light, type Ray, type Vector3Dd } from '@vitral/base';

/**
 * Port of `model.selection.LightPicker`.
 *
 * Picks lights with a viewport ray. A light has no geometry, so it is picked
 * with a sphere centered at its position, whose radius is the size of its gizmo
 * in world units. As the gizmo keeps a constant apparent size in the viewport,
 * that sphere projects, at any distance and for any kind of camera, over the
 * area of the viewport covered by the gizmo. This is a plain model class: it
 * knows nothing about the DOM or WebGL.
 */
export class LightPicker {
  private constructor() {}

  /**
   * @param camera camera of the viewport being picked, with the size of the
   * viewport already set
   * @param light light to pick
   * @param gizmoScale scale applied to the gizmo when drawing it
   * @return the radius of the pick sphere, in world units
   */
  static calculatePickRadius(camera: Camera, light: Light, gizmoScale: number): number {
    return (
      LightGizmoOmniBillboard.calculateWorldHalfSize(
        camera,
        light.getPosition(),
        Math.trunc(camera.getViewportXSize()),
        Math.trunc(camera.getViewportYSize()),
      ) * gizmoScale
    );
  }

  /**
   * Intersects a ray with a sphere.
   * @param ray ray to test, its direction is assumed to be a unit vector
   * @param center center of the sphere
   * @param radius radius of the sphere
   * @return the distance from the ray origin to the first point of the sphere
   * in front of the origin (or to the ray origin if it is inside the sphere),
   * or -1 if the sphere is not hit
   */
  static intersectSphere(ray: Ray, center: Vector3Dd, radius: number): number {
    const toCenter: Vector3Dd = center.subtract(ray.getOrigin());
    const projection: number = toCenter.dotProduct(ray.getDirection());
    const discriminant: number =
      radius * radius - (toCenter.dotProduct(toCenter) - projection * projection);

    if (discriminant < 0) {
      return -1;
    }
    const t: number = projection - Math.sqrt(discriminant);

    if (t >= 0) {
      return t;
    }
    // Origin inside the sphere or sphere behind the ray
    return projection + Math.sqrt(discriminant) >= 0 ? 0 : -1;
  }

  /**
   * @param ray ray fired from the camera through the picked pixel
   * @param camera camera of the viewport being picked
   * @param light light to pick
   * @param gizmoScale scale applied to the gizmo when drawing it
   * @return the distance along the ray to the light, or -1 if it is not hit
   */
  static pick(ray: Ray, camera: Camera, light: Light, gizmoScale: number): number {
    return LightPicker.intersectSphere(
      ray,
      light.getPosition(),
      LightPicker.calculatePickRadius(camera, light, gizmoScale),
    );
  }
}
