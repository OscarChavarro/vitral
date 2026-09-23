import {
  Camera,
  ColorRgb,
  Light,
  Md2Mesh,
  PointLight,
  RendererConfiguration,
  SimpleScene,
  Vector3Dd,
  Vertex2D,
} from '@vitral/base';

/**
 * Port of `java/testsuite/Jogl4Examples/MD2Example/src/model/DebuggerModel.java`.
 *
 * The program's whole state: the MD2 mesh, the camera looking at it from a
 * hundred units along -Y, three white point lights, the renderer configuration
 * the quality controller edits, and the index of the object the XYZ keys move
 * (-1 meaning the camera). `scene`, `x`, `point0`, `point1` and `point2` are Java's and are
 * carried over unused, as they are unused there.
 *
 * Java exposes every field directly; the container's other modules read their
 * model through getters, and this one follows them, since an Angular component
 * reaching into public fields would be the odd one out. The field set, the
 * light positions and the camera setup are Java's exactly.
 */
export class DebuggerModel {
  private readonly camera: Camera;
  private readonly lights: Light[];
  private readonly qualitySelection: RendererConfiguration;
  private readonly scene: SimpleScene;
  private readonly md2Mesh: Md2Mesh;

  private x = 0.0;
  private point0: Vertex2D | null = null;
  private point1: Vertex2D | null = null;
  private point2: Vertex2D | null = null;
  private selectedObject: number;

  constructor() {
    this.scene = new SimpleScene();
    this.md2Mesh = new Md2Mesh();
    this.camera = new Camera();
    this.configureInitialView();
    this.qualitySelection = new RendererConfiguration();

    this.lights = [];

    const lightOne: Light = new PointLight(new Vector3Dd(65, 20, 20), new ColorRgb(1, 1, 1));
    lightOne.setId(0);
    this.lights.push(lightOne);

    const lightTwo: Light = new PointLight(new Vector3Dd(20, 20, 10), new ColorRgb(1, 1, 1));
    lightTwo.setId(1);
    this.lights.push(lightTwo);

    const lightThree: Light = new PointLight(new Vector3Dd(0, -20, 20), new ColorRgb(1, 1, 1));
    lightThree.setId(2);
    this.lights.push(lightThree);

    this.selectedObject = -1;
  }

  /**
   * The two camera lines of Java's constructor, in a method so that they can
   * be applied again.
   *
   * They need to be, because the container's shared `ExampleCameraInteraction`
   * imposes its own default view when it is constructed — the adapter that
   * stands in for `CameraControllerOrbiter` until Phase 38 ports the
   * `vsdk.toolkit.gui` controller family. Java hands the controller a camera
   * and the controller leaves it alone until the user moves it; here the view
   * has to be restored once the adapter exists, and `adoptCameraState()` then
   * takes it as the orbit state.
   */
  configureInitialView(): void {
    this.camera.setPosition(new Vector3Dd(0, -100, 0));
    this.camera.setFarPlaneDistance(4000);
  }

  getCamera(): Camera {
    return this.camera;
  }

  getLights(): Light[] {
    return this.lights;
  }

  getQualitySelection(): RendererConfiguration {
    return this.qualitySelection;
  }

  getScene(): SimpleScene {
    return this.scene;
  }

  getMd2Mesh(): Md2Mesh {
    return this.md2Mesh;
  }

  getX(): number {
    return this.x;
  }

  setX(x: number): void {
    this.x = x;
  }

  getPoint0(): Vertex2D | null {
    return this.point0;
  }

  setPoint0(p0: Vertex2D | null): void {
    this.point0 = p0;
  }

  getPoint1(): Vertex2D | null {
    return this.point1;
  }

  setPoint1(p1: Vertex2D | null): void {
    this.point1 = p1;
  }

  getPoint2(): Vertex2D | null {
    return this.point2;
  }

  setPoint2(p2: Vertex2D | null): void {
    this.point2 = p2;
  }

  getSelectedObject(): number {
    return this.selectedObject;
  }

  setSelectedObject(selectedObject: number): void {
    this.selectedObject = selectedObject;
  }
}
