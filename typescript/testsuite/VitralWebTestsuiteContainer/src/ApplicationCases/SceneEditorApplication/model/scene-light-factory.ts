import {
  ColorRgb,
  JavaMath,
  PointLight,
  Random,
  type ArrayList,
  type Camera,
  type Light,
  type Ray,
  type Vector3Dd,
  type Viewport,
  type ViewportSet,
} from '@vitral/base';

/**
 * Port of `model.SceneLightFactory`.
 *
 * Creates the point lights of the scene editor, following these rules:
 *   - A scene starts without lights.
 *   - Every new light is placed inside the view volume (frustum) of the camera
 *     of one of the visible viewports, so its gizmo can be seen when created.
 *   - The first light of the scene is white and is placed at a fixed screen
 *     position of the selected viewport.
 *   - Every other light gets a random light color and a random position, inside
 *     the frustum of a randomly chosen visible viewport.
 * This is a plain model class: it knows nothing about the DOM or WebGL.
 */
export class SceneLightFactory {
  /// Random positions keep this fraction of the viewport as margin, so the
  /// light gizmo is not cut by the viewport borders: |u|, |v| <= 0.4
  private static readonly MAX_SCREEN_OFFSET = 0.4;
  /// The first light is placed at the upper left quadrant of the viewport
  private static readonly FIRST_LIGHT_U = -0.25;
  private static readonly FIRST_LIGHT_V = 0.25;
  /// Depth range for random lights, as a factor of the camera focal
  /// distance, so lights are created near the region being looked at
  private static readonly MIN_DEPTH_FACTOR = 0.5;
  private static readonly MAX_DEPTH_FACTOR = 1.5;
  /// Light colors have all their channels in [MIN_LIGHT_CHANNEL, 1]
  private static readonly MIN_LIGHT_CHANNEL = 0.6;

  private readonly random: Random;

  /**
   * @param random source of randomness, injectable for reproducible results
   */
  constructor(random: Random = new Random()) {
    this.random = random;
  }

  /**
   * Builds the next point light for a scene. The light is not added to the
   * scene: that is up to the caller.
   * @param existingLights lights currently in the scene, used to know if the
   * new one is the first
   * @param viewportSet viewports whose cameras define where a light can be
   * created
   * @return a new light, or null if there is no camera to place it in view
   */
  createLight(existingLights: ArrayList<Light>, viewportSet: ViewportSet): PointLight | null {
    const first: boolean = existingLights.isEmpty();
    const viewport: Viewport | null = this.chooseViewport(viewportSet, first);

    if (viewport === null) {
      return null;
    }
    const camera: Camera = viewport.getActiveCamera();

    if (first) {
      return new PointLight(
        this.pointInFrustum(
          camera,
          SceneLightFactory.FIRST_LIGHT_U,
          SceneLightFactory.FIRST_LIGHT_V,
          this.calculateFocalDistance(camera),
        ),
        new ColorRgb(1, 1, 1),
      );
    }
    const u: number = this.randomInRange(-SceneLightFactory.MAX_SCREEN_OFFSET, SceneLightFactory.MAX_SCREEN_OFFSET);
    const v: number = this.randomInRange(-SceneLightFactory.MAX_SCREEN_OFFSET, SceneLightFactory.MAX_SCREEN_OFFSET);
    const focal: number = this.calculateFocalDistance(camera);
    const depth: number = this.randomInRange(
      this.clampDepth(camera, focal * SceneLightFactory.MIN_DEPTH_FACTOR),
      this.clampDepth(camera, focal * SceneLightFactory.MAX_DEPTH_FACTOR),
    );

    return new PointLight(this.pointInFrustum(camera, u, v, depth), this.createRandomLightColor());
  }

  /**
   * @return a pastel color, with every channel in [MIN_LIGHT_CHANNEL, 1]
   */
  private createRandomLightColor(): ColorRgb {
    return new ColorRgb(
      this.randomInRange(SceneLightFactory.MIN_LIGHT_CHANNEL, 1.0),
      this.randomInRange(SceneLightFactory.MIN_LIGHT_CHANNEL, 1.0),
      this.randomInRange(SceneLightFactory.MIN_LIGHT_CHANNEL, 1.0),
    );
  }

  /**
   * The first light uses the selected viewport. Any other light uses a random
   * one. Only visible (active) viewports are considered.
   * @return the chosen viewport, or null if there are no visible viewports
   */
  private chooseViewport(viewportSet: ViewportSet, first: boolean): Viewport | null {
    const selected: Viewport | null = viewportSet.getSelectedViewport();

    if (first && selected !== null && selected.isActive()) {
      return selected;
    }
    const activeCount: number = viewportSet.countActiveViewports();

    if (activeCount < 1) {
      return null;
    }
    let chosen: number = first ? 0 : this.random.nextInt(activeCount);

    for (const viewport of viewportSet.getViewports()) {
      if (!viewport.isActive()) continue;
      if (chosen === 0) {
        return viewport;
      }
      chosen--;
    }
    return null;
  }

  private calculateFocalDistance(camera: Camera): number {
    return camera.getFocusedPosition().subtract(camera.getPosition()).length();
  }

  private clampDepth(camera: Camera, depth: number): number {
    const near: number = camera.getNearPlaneDistance();
    const far: number = camera.getFarPlaneDistance();

    return Math.max(near, Math.min(far, depth));
  }

  /**
   * Calculates the world point that projects at a given place of the viewport
   * of a camera, at a given depth along the camera viewing direction. This is
   * valid for perspective and orthogonal cameras, as it uses the camera's own
   * ray generation, and a point with a depth between the near and far planes
   * is inside the frustum by construction.
   * @param camera camera whose view volume must contain the point
   * @param u horizontal screen offset from the viewport center, in
   * [-0.5, 0.5] (positive to the right)
   * @param v vertical screen offset from the viewport center, in
   * [-0.5, 0.5] (positive up)
   * @param depth distance from the eye plane, along the viewing direction
   * @return a point in world coordinates
   */
  private pointInFrustum(camera: Camera, u: number, v: number, depth: number): Vector3Dd {
    camera.updateVectors();

    const width: number = camera.getViewportXSize();
    const height: number = camera.getViewportYSize();
    const x: number = JavaMath.round(width * (0.5 + u));
    const y: number = JavaMath.round(height * (0.5 - v) - 1);
    const ray: Ray = camera.generateRay(x, y);
    const front: Vector3Dd = camera.getFront().normalized();
    const alongFront: number = ray.getDirection().dotProduct(front);

    return ray.getOrigin().add(ray.getDirection().multiply(depth / alongFront));
  }

  private randomInRange(min: number, max: number): number {
    return min + this.random.nextDouble() * (max - min);
  }
}
