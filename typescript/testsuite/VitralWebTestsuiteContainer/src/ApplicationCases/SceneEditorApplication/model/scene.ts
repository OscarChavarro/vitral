import {
  ArrayList,
  Camera,
  ColorRgb,
  CubemapBackground,
  DepthBufferMode,
  FixedBackground,
  JavaMath,
  Matrix4x4d,
  RayHit,
  RendererConfiguration,
  SimpleBackground,
  SimpleBody,
  SimpleMaterial,
  SimpleScene,
  Vector3Dd,
  type Background,
  type CameraSnapshot,
  type Geometry,
  type Ray,
  type RGBAImageUncompressed,
  type RGBImageUncompressed,
  type SimpleBodyGroup,
  type SimpleSceneSnapshot,
  type ZBuffer,
} from '@vitral/base';
import { ImageFiles, type ImageFileAccess } from '../io/image-files';
import { SelectionSet } from './selection/selection-set';

/**
 * What a `Scene` needs from the platform to raytrace: Java's static
 * `ParallelRaytracer` shares the snapshot with its threads, while in a browser
 * the snapshot travels to Web Workers, whose modules only the application can
 * locate (see `render/scene-raytracing-backend.ts`).
 */
export interface SceneRaytracingBackend {
  /**
   * Java's `ParallelRaytracer.execute(image, depth, depthMode, quality,
   * snapshot, interactiveReport)`.
   */
  execute(
    image: RGBImageUncompressed,
    depth: ZBuffer | null,
    depthMode: DepthBufferMode,
    quality: RendererConfiguration,
    snapshot: SimpleSceneSnapshot,
    interactiveReport: boolean,
  ): Promise<void>;
}

/**
 * Port of `model.Scene`.
 *
 * The scene being edited, with its cameras, backgrounds, selections and the
 * raytracing of its images. Runtime boundaries (recorded here, next to the
 * code):
 *   - Java loads background images and writes `output.jpg` synchronously
 *     through `java.io.File`; here they go through the `ImageFiles` the
 *     application installs, so `buildCubemap`, `buildFixedmap`,
 *     `activateSelectedBackground` and `raytrace*` are asynchronous.
 *   - Java's static `ParallelRaytracer` becomes the `SceneRaytracingBackend`
 *     the application installs (Web Workers need bundler-located modules).
 *   - Java's `../../../../etc/...` paths, relative to the folder of the Java
 *     example, are `./etc/...` paths relative to the page, as served by the
 *     web container.
 */
export class Scene {
  /// Shared by all scenes: its workers (one per processor) are reused
  private static raytracer: SceneRaytracingBackend | null = null;
  private static readonly ETC_FOLDER = './etc';

  scene: SimpleScene;

  //- 1. Camera ----------------------------------------------------------
  camera: Camera;
  activeCamera: Camera;

  //- 3. Background ------------------------------------------------------
  simpleBackground: SimpleBackground;
  cubemapBackground: CubemapBackground | null;
  fixedBackground: FixedBackground | null;
  selectedBackground: number;

  //- 4. Objects ---------------------------------------------------------
  /// Test corridor is drawn by the rendering technology, not stored here
  showCorridor: boolean;
  debugThingGroups: ArrayList<SimpleBodyGroup>;

  selectedThings: SelectionSet;
  selectedLights: SelectionSet;
  selectedDebugThingGroups: SelectionSet;

  // Others
  qualityTemplate: RendererConfiguration;
  private acumObject = 1;
  /// Size factor of light gizmos, shared by rendering and picking
  private lightGizmoScale = 1.0;

  /**
   * @param raytracer how scenes are raytraced (the application installs it)
   */
  static setRaytracingBackend(raytracer: SceneRaytracingBackend | null): void {
    Scene.raytracer = raytracer;
  }

  constructor() {
    this.scene = new SimpleScene();

    //-----------------------------------------------------------------
    this.debugThingGroups = new ArrayList<SimpleBodyGroup>();

    let R: Matrix4x4d = new Matrix4x4d();
    this.camera = new Camera();

    R = R.eulerAnglesRotation(JavaMath.toRadians(45), JavaMath.toRadians(-35), 0);
    this.camera.setPosition(new Vector3Dd(-5, -5, 5));
    this.camera.setRotation(R);

    this.activeCamera = this.camera;
    this.selectedThings = new SelectionSet(this.scene.getSimpleBodies() as unknown as ArrayList<unknown>);
    this.selectedLights = new SelectionSet(this.scene.getLights() as unknown as ArrayList<unknown>);
    this.selectedDebugThingGroups = new SelectionSet(this.debugThingGroups as unknown as ArrayList<unknown>);

    //-----------------------------------------------------------------
    this.simpleBackground = new SimpleBackground();
    this.simpleBackground.setColor(0.49, 0.49, 0.49);

    this.cubemapBackground = null;
    this.fixedBackground = null;

    this.selectedBackground = 0;

    //-----------------------------------------------------------------
    this.showCorridor = false;

    this.qualityTemplate = new RendererConfiguration();
    this.qualityTemplate.setSurfaces(true);
    this.qualityTemplate.setWires(false);
  }

  /**
   * @return size factor of light gizmos, used both to draw and to pick them
   */
  getLightGizmoScale(): number {
    return this.lightGizmoScale;
  }

  /**
   * @param lightGizmoScale new size factor of light gizmos
   */
  setLightGizmoScale(lightGizmoScale: number): void {
    this.lightGizmoScale = lightGizmoScale;
  }

  async buildCubemap(): Promise<boolean> {
    const folder = Scene.ETC_FOLDER + '/cubemaps/dorise1/';

    try {
      const files: ImageFileAccess = ImageFiles.get();
      let progress = 'Loading background: 1';
      const front: RGBAImageUncompressed = await files.importRGBA(folder + 'entorno0.jpg');
      progress += '2';
      const right: RGBAImageUncompressed = await files.importRGBA(folder + 'entorno1.jpg');
      progress += '3';
      const back: RGBAImageUncompressed = await files.importRGBA(folder + 'entorno2.jpg');
      progress += '4';
      const left: RGBAImageUncompressed = await files.importRGBA(folder + 'entorno3.jpg');
      progress += '5';
      const down: RGBAImageUncompressed = await files.importRGBA(folder + 'entorno4.jpg');
      progress += '6';
      const up: RGBAImageUncompressed = await files.importRGBA(folder + 'entorno5.jpg');
      console.log(progress + ' OK!');

      this.cubemapBackground = new CubemapBackground(this.camera, front, right, back, left, down, up);
    } catch (e) {
      console.error(e);
      return false;
    }
    return true;
  }

  async buildFixedmap(): Promise<boolean> {
    try {
      const img: RGBAImageUncompressed = await ImageFiles.get().importRGBA(
        Scene.ETC_FOLDER + '/cubemaps/dorise1/entorno0.jpg',
      );
      console.log('Loading background: OK!');

      this.fixedBackground = new FixedBackground(this.camera, img);
    } catch (e) {
      console.error(e);
      return false;
    }
    return true;
  }

  defaultMaterial(): SimpleMaterial {
    let m: SimpleMaterial = new SimpleMaterial();

    /*
        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m = m.withSpecular(new ColorRgb(1, 1, 1));
        m = m.withDoubleSided(false);
        m = m.withPhongExponent(100.0);
    */

    m = m.withAmbient(new ColorRgb(0, 0, 0));
    m = m.withDiffuse(new ColorRgb(1, 1, 1));
    m = m.withSpecular(new ColorRgb(1, 1, 1));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(40.0);

    return m;
  }

  addThing(g: Geometry): SimpleBody {
    const thing: SimpleBody = new SimpleBody();
    thing.setGeometry(g);
    thing.setPosition(new Vector3Dd());
    thing.setRotation(new Matrix4x4d());
    thing.setRotationInverse(new Matrix4x4d());
    thing.setMaterial(this.defaultMaterial());
    thing.setName('Geometric object ' + this.acumObject);
    this.scene.getSimpleBodies().add(thing);

    this.acumObject++;
    this.selectedThings.sync();
    return thing;
  }

  doIntersectionFirstHit(r: Ray, info: RayHit): boolean {
    // Java's Float.MAX_VALUE
    let nearestDistance = 3.4028234663852886e38;
    const things = this.scene.getSimpleBodies();
    let intersected = false;
    const ii: RayHit = new RayHit();
    for (let i = 0; i < things.size(); i++) {
      const gi: SimpleBody = things.get(i);
      const hit: RayHit = new RayHit();
      if (gi.doIntersectionFirstHit(r, hit) && hit.getRay()!.getT() < nearestDistance) {
        ii.clone(hit);
        nearestDistance = hit.getRay()!.getT();
        r = hit.getRay()!;
        intersected = true;
      }
    }
    if (intersected) {
      r = r.withT(nearestDistance);
      info.clone(ii);
      return true;
    }
    return false;
  }

  /**
   * Selects the next background, cycling over the three available ones.
   */
  rotateBackground(): void {
    this.selectedBackground++;
    if (this.selectedBackground > 2) {
      this.selectedBackground = 0;
    }
  }

  async activateSelectedBackground(): Promise<void> {
    let currentBackground: Background = this.simpleBackground;
    switch (this.selectedBackground) {
      case 2:
        if (this.cubemapBackground === null) {
          await this.buildCubemap();
        }
        if (this.cubemapBackground !== null) {
          this.cubemapBackground.setCamera(this.activeCamera);
          currentBackground = this.cubemapBackground;
        }
        break;
      case 1:
        if (this.fixedBackground === null) {
          await this.buildFixedmap();
        }
        if (this.fixedBackground !== null) {
          currentBackground = this.fixedBackground;
        }
        break;
    }

    const list = this.scene.getBackgrounds();
    if (list.size() < 1) {
      list.add(currentBackground);
    } else {
      list.remove(0);
      list.add(0, currentBackground);
    }
    this.scene.setActiveBackgroundIndex(0);
  }

  print(): void {
    const things = this.scene.getSimpleBodies();

    console.log('= SCENE REPORT ============================================================');
    console.log('Current camera:\n' + this.activeCamera);
    console.log('Things in scene: ' + things.size());
    for (let i = 0; i < things.size(); i++) {
      const geometry: Geometry | null = things.get(i).getGeometry();
      console.log('  - Thing[' + i + ']: ' + (geometry !== null ? geometry.constructor.name : 'null'));
    }
    console.log('= END OF REPORT ===========================================================');
  }

  /**
   * Raytraces the scene from the active camera, reporting the progress and the
   * time in the console, and exports the result to `output.jpg`.
   * @param outViewport image to fill; its size gives the resolution
   */
  async raytrace(outViewport: RGBImageUncompressed): Promise<void> {
    await this.raytraceWith(outViewport, null, true);
  }

  /**
   * Raytraces the scene from the active camera, silently: used to present a
   * viewport in CPU render mode, once per frame. Java's two overloads (with
   * and without depth) are one method.
   * @param outViewport image to fill; its size gives the resolution
   * @param outDepth depth buffer of the size of the image to fill with window
   * space depth values in [0, 1], so the image can be composited with
   * rasterized elements (grid, gizmos...); or null
   */
  async raytraceViewport(outViewport: RGBImageUncompressed, outDepth: ZBuffer | null = null): Promise<void> {
    await this.raytraceWith(outViewport, outDepth, false);
  }

  /**
   * Raytraces with one worker per available processor (see
   * `ParallelRaytracer`).
   * @param outViewport image to fill
   * @param outDepth depth buffer to fill with OpenGL depth values, or null
   * @param interactiveReport true to report progress and time in the console
   * and export the result to `output.jpg`
   */
  private async raytraceWith(
    outViewport: RGBImageUncompressed,
    outDepth: ZBuffer | null,
    interactiveReport: boolean,
  ): Promise<void> {
    if (Scene.raytracer === null) {
      throw new Error('Scene raytracing backend is not installed (see Scene.setRaytracingBackend)');
    }
    const originalWidth: number = Math.trunc(this.activeCamera.getViewportXSize());
    const originalHeight: number = Math.trunc(this.activeCamera.getViewportYSize());
    const cameraSnapshot: CameraSnapshot = this.activeCamera.exportToCameraSnapshot(
      outViewport.getXSize(),
      outViewport.getYSize(),
    );

    //-----------------------------------------------------------------

    let activeBackground: Background;
    switch (this.selectedBackground) {
      case 2:
        if (this.cubemapBackground === null) {
          await this.buildCubemap();
        }
        activeBackground = this.cubemapBackground !== null ? this.cubemapBackground : this.simpleBackground;
        break;
      case 1:
        if (this.fixedBackground === null) {
          await this.buildFixedmap();
        }
        activeBackground = this.fixedBackground !== null ? this.fixedBackground : this.simpleBackground;
        break;
      case 0:
      default:
        activeBackground = this.simpleBackground;
        break;
    }

    const sceneSnapshot: SimpleSceneSnapshot = this.scene.exportToSimpleSceneSnapshot(
      cameraSnapshot,
      activeBackground,
    );
    const initialTime: number = Date.now();
    await Scene.raytracer.execute(
      outViewport,
      outDepth,
      outDepth !== null ? DepthBufferMode.OPENGL_DEPTH : DepthBufferMode.NONE,
      this.qualityTemplate,
      sceneSnapshot,
      interactiveReport,
    );
    const finalTime: number = Date.now();

    if (interactiveReport) {
      console.log('Image generated in ' + (finalTime - initialTime) + ' miliseconds.');

      console.log('Exporting result image to file: ');
      if (!(await ImageFiles.get().exportJPG('./output.jpg', outViewport))) {
        console.error('Error grabando la imagen!!');
        // Java ends the program here (System.exit); a page can not, and
        // keeps the scene as it is
        return;
      }
      console.log(' OK!');
      console.log('An image has been created in the file output.jpg');
    }

    //-----------------------------------------------------------------
    this.activeCamera.updateViewportResize(originalWidth, originalHeight);
  }
}
