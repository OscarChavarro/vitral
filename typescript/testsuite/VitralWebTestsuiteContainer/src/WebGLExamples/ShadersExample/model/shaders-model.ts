import {
  Camera,
  ColorRgb,
  IndexedColorImageUncompressed,
  Light,
  Matrix4x4d,
  MicroFacetedMaterial,
  NormalMap,
  PointLight,
  RendererConfiguration,
  RGBImageUncompressed,
  SimpleMaterial,
  Sphere,
  Vector3Dd,
} from '@vitral/base';
import type { ShadersResources } from '../io/shaders-reader';
import { nextShaderOperationMode, type ShaderOperationMode } from './shader-operation-mode';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/model/ShadersModel.java`.
 *
 * The camera, the renderer configuration, the unit sphere, the point light,
 * the Phong material, the Cook-Torrance material and its name list, the
 * rendering mode, the HUD flag, the two animation flags, the sphere rotation
 * and the meridian/parallel counts with their minima — all of it is the Java
 * model, including `normalizeAngleRadians`, `indexOfMaterialName` and the
 * defaults `loadMicroFacetMaterialNames` falls back to.
 *
 * Two boundaries are crossed.
 *
 * Java's `initializeDefaults` reads three files, so `createDefault()` returns a
 * fully loaded model. A browser fetch is asynchronous and a constructor cannot
 * wait for one, so the resources arrive through {@link setResources} once
 * `io.ShadersReader` has them; everything else is set in the constructor,
 * exactly where Java sets it. Until then the texture, the bump map and the
 * material list are absent, which is the state the component covers with its
 * loading overlay.
 *
 * Java's `cycleCookTorranceMaterial` builds `new MicroFacetedMaterial(csvPath,
 * name)`, re-reading the file on every keypress. The text is read once here
 * and kept, and the same parse runs against it, since re-fetching a static
 * resource on every `[m]` would only add latency to an identical answer.
 *
 * `getCameraController()` and `getQualityController()` have no counterpart:
 * Java's `CameraControllerOrbiter` and `RendererConfigurationController` live
 * in the decision-gated Phase 38 group, and until it is ported the container's
 * shared `ExampleCameraInteraction` and `ExampleRendererConfigurationInteraction`
 * stand in, owned by the component rather than by the model.
 */
export class ShadersModel {
  private static readonly MIN_SPHERE_MERIDIANS = 12;
  private static readonly MIN_SPHERE_PARALLELS = 8;
  private static readonly COOK_TORRANCE_MATERIAL_NAME = 'Copper';

  private readonly camera: Camera;
  private readonly quality: RendererConfiguration;
  private readonly sphere: Sphere;
  private readonly light: Light;
  private material: SimpleMaterial;
  private cookTorranceCopperMaterial: MicroFacetedMaterial | null = null;
  private cookTorranceMaterialNames: string[] = [];
  private cookTorranceMaterialIndex = 0;
  private textureMap: RGBImageUncompressed | null = null;
  private bumpMapHeightRgb: RGBImageUncompressed | null = null;
  private bumpNormalMap: NormalMap | null = null;
  private bumpMapFile: IndexedColorImageUncompressed | null = null;
  private microFacetCsvText: string | null = null;
  private microFacetCsvName = '';
  private softwareFrameImage: RGBImageUncompressed | null = null;
  private renderingMode: ShaderOperationMode;
  private showHud: boolean;
  private animationEnabled: boolean;
  private lightAnimationEnabled: boolean;
  private sphereRotationAngleRadians: number;
  private sphereMeridians: number;
  private sphereParallels: number;

  constructor() {
    this.camera = new Camera();
    this.configureInitialView();

    this.quality = new RendererConfiguration();
    this.quality.setTexture(true);
    this.quality.setBumpMap(true);
    this.quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);

    this.sphere = new Sphere(1.0);

    this.light = new PointLight(new Vector3Dd(1, -3, 1), new ColorRgb(1, 1, 1));
    this.light.setId(0);

    let material = new SimpleMaterial();
    material = material.withAmbient(new ColorRgb(0.1, 0.1, 0.1));
    material = material.withDiffuse(new ColorRgb(1, 1, 1));
    material = material.withSpecular(new ColorRgb(1, 1, 1));
    material = material.withPhongExponent(40);
    this.material = material;

    this.animationEnabled = false;
    this.lightAnimationEnabled = false;
    this.renderingMode = 'OPENGL_4_1';
    this.showHud = true;
    this.sphereRotationAngleRadians = 0.0;
    this.sphereMeridians = 100;
    this.sphereParallels = 50;
    this.updateSoftwareViewportAndCamera(
      Math.trunc(this.camera.getViewportXSize()),
      Math.trunc(this.camera.getViewportYSize()),
    );
  }

  /**
   * The three camera lines of Java's `initializeDefaults`, in a method so that
   * they can be applied again.
   *
   * This has no Java counterpart. Java hands `CameraControllerOrbiter` a camera
   * and the controller leaves it alone until the user moves it; the container's
   * `ExampleCameraInteraction`, which stands in for that controller until Phase
   * 38 ports it, imposes its own default view when it is constructed. So the
   * view has to be restored once the adapter exists, and `adoptCameraState()`
   * then takes it as the orbit state.
   */
  configureInitialView(): void {
    this.camera.setPosition(new Vector3Dd(0, -4, 0));
    const rotation: Matrix4x4d = new Matrix4x4d().eulerAnglesRotation(
      (90.0 * Math.PI) / 180.0,
      0,
      0,
    );
    this.camera.setRotation(rotation);
    this.camera.setFov(30.0);
  }

  /**
   * The three reads of Java's `initializeDefaults`, with the bytes already in
   * hand. `cookTorranceCopperMaterial`, `cookTorranceMaterialNames` and
   * `cookTorranceMaterialIndex` are set here in the same order Java sets them.
   */
  setResources(resources: ShadersResources): void {
    this.textureMap = resources.textureMap;
    this.bumpMapHeightRgb = resources.bumpMapHeightRgb;
    this.bumpNormalMap = resources.bumpNormalMap;
    this.bumpMapFile = resources.bumpMapFile;
    this.microFacetCsvText = resources.microFacetCsvText;
    this.microFacetCsvName = resources.microFacetCsvName;

    this.cookTorranceCopperMaterial = MicroFacetedMaterial.fromCsvText(
      resources.microFacetCsvText,
      ShadersModel.COOK_TORRANCE_MATERIAL_NAME,
      resources.microFacetCsvName,
    );
    this.cookTorranceMaterialNames = ShadersModel.loadMicroFacetMaterialNames(
      resources.microFacetCsvText,
    );
    this.cookTorranceMaterialIndex = ShadersModel.indexOfMaterialName(
      this.cookTorranceMaterialNames,
      ShadersModel.COOK_TORRANCE_MATERIAL_NAME,
    );
  }

  getCamera(): Camera {
    return this.camera;
  }

  getQuality(): RendererConfiguration {
    return this.quality;
  }

  getSphere(): Sphere {
    return this.sphere;
  }

  getLight(): Light {
    return this.light;
  }

  getMaterial(): SimpleMaterial {
    return this.material;
  }

  getActiveMaterialForCurrentShading(): SimpleMaterial {
    if (
      this.quality.getShadingType() === RendererConfiguration.SHADING_TYPE_COOK_TERRANCE &&
      this.cookTorranceCopperMaterial !== null
    ) {
      return this.cookTorranceCopperMaterial;
    }
    return this.material;
  }

  cycleCookTorranceMaterial(): void {
    if (this.cookTorranceMaterialNames.length < 1 || this.microFacetCsvText === null) {
      return;
    }
    this.cookTorranceMaterialIndex =
      (this.cookTorranceMaterialIndex + 1) % this.cookTorranceMaterialNames.length;
    const nextMaterialName: string =
      this.cookTorranceMaterialNames[this.cookTorranceMaterialIndex]!;
    this.cookTorranceCopperMaterial = MicroFacetedMaterial.fromCsvText(
      this.microFacetCsvText,
      nextMaterialName,
      this.microFacetCsvName,
    );
  }

  getCookTorranceMaterialLabel(): string {
    const name: string | null = this.cookTorranceCopperMaterial?.getName() ?? null;
    if (name === null || name.trim().length < 1) {
      return ShadersModel.COOK_TORRANCE_MATERIAL_NAME;
    }
    return name;
  }

  getTextureMap(): RGBImageUncompressed | null {
    return this.textureMap;
  }

  getBumpMapHeightRgb(): RGBImageUncompressed | null {
    return this.bumpMapHeightRgb;
  }

  /**
   * The bump map as read, which `render.SoftwareRaycaster` sends to its
   * workers so that each builds Java's `NormalMap` from the same file.
   */
  getBumpMapFile(): IndexedColorImageUncompressed | null {
    return this.bumpMapFile;
  }

  /**
   * The `NormalMap` Java's `SoftwareRaycaster` keeps for itself, read from the
   * same file with the same scale; see `io.ShadersReader`.
   */
  getBumpNormalMap(): NormalMap | null {
    return this.bumpNormalMap;
  }

  getSoftwareFrameImage(): RGBImageUncompressed | null {
    return this.softwareFrameImage;
  }

  getRenderingMode(): ShaderOperationMode {
    return this.renderingMode;
  }

  setRenderingMode(renderingMode: ShaderOperationMode): void {
    this.renderingMode = renderingMode;
  }

  rotateRenderingMode(): void {
    this.renderingMode = nextShaderOperationMode(this.renderingMode);
  }

  isShowHud(): boolean {
    return this.showHud;
  }

  setShowHud(showHud: boolean): void {
    this.showHud = showHud;
  }

  toggleShowHud(): void {
    this.showHud = !this.showHud;
  }

  updateSoftwareViewportAndCamera(viewportWidth: number, viewportHeight: number): void {
    const width: number = Math.max(1, viewportWidth);
    const height: number = Math.max(1, viewportHeight);
    this.camera.updateViewportResize(width, height);
    if (
      this.softwareFrameImage !== null &&
      this.softwareFrameImage.getXSize() === width &&
      this.softwareFrameImage.getYSize() === height
    ) {
      return;
    }
    this.softwareFrameImage = new RGBImageUncompressed();
    if (!this.softwareFrameImage.init(width, height)) {
      throw new Error('Could not allocate software frame image ' + width + 'x' + height);
    }
  }

  isAnimationEnabled(): boolean {
    return this.animationEnabled;
  }

  setAnimationEnabled(animationEnabled: boolean): void {
    this.animationEnabled = animationEnabled;
  }

  toggleAnimationEnabled(): void {
    this.animationEnabled = !this.animationEnabled;
  }

  isLightAnimationEnabled(): boolean {
    return this.lightAnimationEnabled;
  }

  setLightAnimationEnabled(lightAnimationEnabled: boolean): void {
    this.lightAnimationEnabled = lightAnimationEnabled;
  }

  toggleLightAnimationEnabled(): void {
    this.lightAnimationEnabled = !this.lightAnimationEnabled;
  }

  getSphereRotationAngleRadians(): number {
    return this.sphereRotationAngleRadians;
  }

  setSphereRotationAngleRadians(sphereRotationAngleRadians: number): void {
    this.sphereRotationAngleRadians = ShadersModel.normalizeAngleRadians(
      sphereRotationAngleRadians,
    );
  }

  advanceSphereRotationRadians(deltaRadians: number): void {
    this.setSphereRotationAngleRadians(this.sphereRotationAngleRadians + deltaRadians);
  }

  getSphereMeridians(): number {
    return this.sphereMeridians;
  }

  getSphereParallels(): number {
    return this.sphereParallels;
  }

  setSphereMeridians(sphereMeridians: number): void {
    this.sphereMeridians = Math.max(ShadersModel.MIN_SPHERE_MERIDIANS, sphereMeridians);
  }

  setSphereParallels(sphereParallels: number): void {
    this.sphereParallels = Math.max(ShadersModel.MIN_SPHERE_PARALLELS, sphereParallels);
  }

  changeSphereMeridians(delta: number): void {
    this.setSphereMeridians(this.sphereMeridians + delta);
  }

  changeSphereParallels(delta: number): void {
    this.setSphereParallels(this.sphereParallels + delta);
  }

  private static normalizeAngleRadians(angle: number): number {
    const twoPi: number = 2.0 * Math.PI;
    let normalized: number = angle % twoPi;
    if (normalized < 0.0) {
      normalized += twoPi;
    }
    return normalized;
  }

  private static indexOfMaterialName(materialNames: string[], targetName: string): number {
    let i: number;
    for (i = 0; i < materialNames.length; i++) {
      if (materialNames[i]!.toLowerCase() === targetName.toLowerCase()) {
        return i;
      }
    }
    return 0;
  }

  /**
   * Java's `loadMicroFacetMaterialNames`, over the text of the CSV rather than
   * its path: `Files.readAllLines`, then the first field of every non-empty
   * line after the header. Java answers a single-entry list carrying the
   * default name when the file is empty or cannot be read; an unreadable file
   * never reaches here, because `io.ShadersReader` has already rejected.
   */
  private static loadMicroFacetMaterialNames(csvText: string): string[] {
    const materialNames: string[] = [];
    const lines: string[] = csvText.split('\n').map((line) => line.replace(/\r/g, ''));
    if (lines.length < 1) {
      materialNames.push(ShadersModel.COOK_TORRANCE_MATERIAL_NAME);
      return materialNames;
    }
    let i: number;
    for (i = 1; i < lines.length; i++) {
      const line: string = lines[i]!.trim();
      if (line.length < 1) {
        continue;
      }
      const fields: string[] = line.split(',');
      if (fields.length === 0) {
        continue;
      }
      const materialName: string = fields[0]!.trim();
      if (materialName.length < 1) {
        continue;
      }
      materialNames.push(materialName);
    }
    if (materialNames.length < 1) {
      materialNames.push(ShadersModel.COOK_TORRANCE_MATERIAL_NAME);
    }
    return materialNames;
  }
}
