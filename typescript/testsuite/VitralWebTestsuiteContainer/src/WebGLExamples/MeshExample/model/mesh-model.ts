import {
  Camera,
  ColorRgb,
  Intersection,
  Light,
  PointLight,
  Ray,
  RayGizmo,
  RayHit,
  RendererConfiguration,
  SimpleBody,
  SimpleBodyGroup,
  SimpleScene,
  Vector3Dd,
} from '@vitral/base';

/**
 * Port of `java/testsuite/Jogl4Examples/MeshExample/src/model/MeshModel.java`.
 */
export class MeshModel {
  private readonly camera: Camera;
  private readonly lights: Light[];
  private readonly scene: SimpleScene;
  private readonly qualitySelection: RendererConfiguration;
  private readonly rayGizmo: RayGizmo;
  private tangibleServiceUrl = 'ws://localhost:8090/v1/values';

  constructor() {
    this.scene = new SimpleScene();
    this.camera = new Camera();
    this.qualitySelection = new RendererConfiguration();
    this.rayGizmo = new RayGizmo(this.makeIntersectionCallback(), 1);
    this.lights = [];
    const light0: Light = new PointLight(new Vector3Dd(10, -20, 50), new ColorRgb(1, 1, 1));
    light0.setId(0);
    const light1: Light = new PointLight(new Vector3Dd(-10, 20, 50), new ColorRgb(1, 1, 1));
    light1.setId(1);
    this.lights.push(light0);
    this.lights.push(light1);
  }

  getCamera(): Camera {
    return this.camera;
  }

  getLights(): Light[] {
    return this.lights;
  }

  getScene(): SimpleScene {
    return this.scene;
  }

  getQualitySelection(): RendererConfiguration {
    return this.qualitySelection;
  }

  getRayGizmo(): RayGizmo {
    return this.rayGizmo;
  }

  getTangibleServiceUrl(): string {
    return this.tangibleServiceUrl;
  }

  setTangibleServiceUrl(tangibleServiceUrl: string | null): void {
    if (tangibleServiceUrl === null || tangibleServiceUrl.trim().length === 0) {
      return;
    }
    this.tangibleServiceUrl = tangibleServiceUrl;
  }

  /**
   * Returns a callback that tests a world-space ray against all bodies in
   * the scene and returns the closest intersection, or null if none.
   */
  private makeIntersectionCallback(): (ray: Ray) => Intersection | null {
    return (ray: Ray): Intersection | null => {
      let closest: Intersection | null = null;
      let closestT = Number.MAX_VALUE;
      const bodies = this.scene.getSimpleBodies();
      for (let i = 0; i < bodies.size(); i++) {
        const body: SimpleBody = bodies.get(i);
        const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        if (body.doIntersectionFirstHit(ray, hit) && hit.hasHitDistance()) {
          const t: number = hit.hitDistance();
          if (t > 1e-6 && t < closestT) {
            closestT = t;
            closest = new Intersection(t, hit.p, hit.n);
          }
        }
      }
      return closest;
    };
  }

  configureInitialViewAndLightToScene(): void {
    if (this.scene.getSimpleBodies().isEmpty()) {
      return;
    }

    const group = new SimpleBodyGroup();
    const bodies = this.scene.getSimpleBodies();
    for (let i = 0; i < bodies.size(); i++) {
      group.getBodies().add(bodies.get(i));
    }
    const minmax: number[] = group.getMinMax();
    if (minmax.length < 6) {
      return;
    }

    const min = new Vector3Dd(minmax[0]!, minmax[1]!, minmax[2]!);
    const max = new Vector3Dd(minmax[3]!, minmax[4]!, minmax[5]!);
    const center: Vector3Dd = min.add(max).multiply(0.5);
    let radius: number = max.subtract(min).length() * 0.5;
    if (radius < 0.001) {
      radius = 1.0;
    }

    const fovRad: number = (this.camera.getFov() * Math.PI) / 180.0;
    let viewDistance: number = (radius / Math.tan(fovRad * 0.5)) * 1.35;
    if (viewDistance < radius * 1.5) {
      viewDistance = radius * 1.5;
    }

    const eyeDirection: Vector3Dd = new Vector3Dd(0, -1, 0.35).normalized();
    const eye: Vector3Dd = center.add(eyeDirection.multiply(viewDistance));
    this.camera.setPosition(eye);
    this.camera.setUpMaintainingOrthogonality(new Vector3Dd(0, 0, 1));
    this.camera.setFocusedPositionMaintainingOrthogonality(center);

    const nearPlane: number = Math.max(0.01, viewDistance - radius * 2.2);
    const farPlane: number = Math.max(nearPlane + 1.0, viewDistance + radius * 4.0);
    this.camera.setNearPlaneDistance(nearPlane);
    this.camera.setFarPlaneDistance(farPlane);
    this.camera.updateVectors();

    const lightDirection: Vector3Dd = new Vector3Dd(1, -1, 1).normalized();
    const lightPos0: Vector3Dd = center.add(lightDirection.multiply(radius * 3.0));
    const lightPos1: Vector3Dd = center.add(
      new Vector3Dd(-lightDirection.x(), -lightDirection.y(), lightDirection.z())
        .normalized()
        .multiply(radius * 3.0),
    );

    if (this.lights.length !== 0) {
      this.lights[0]!.setPosition(lightPos0);
    }
    if (this.lights.length > 1) {
      this.lights[1]!.setPosition(lightPos1);
    }
  }
}
