import {
  Camera,
  ColorRgb,
  ColorRgba,
  ColorTextureFixture,
  ControlledRGBAImageHDRUncompressed,
  ImageTexture,
  ImageToSolidTextureInterpolationTypes,
  ImageToSolidTextureProjectionMethods,
  IndexedColorImageHDRUncompressed,
  InfinitePlaneGizmo,
  Intersection,
  Light,
  Matrix4x4d,
  PointLight,
  ProceduralNoise,
  RGBAColorPalette,
  RGBAPixelHDR,
  RGBImageUncompressed,
  Ray,
  RayGizmo,
  RayHit,
  RendererConfiguration,
  SimpleBody,
  SimpleBodyGroup,
  SimpleScene,
  TextureUtils,
  Vector3Dd,
  type Image,
} from '@vitral/base';
import {
  SolidTextureExampleColorNames,
  solidTextureColorNameNext,
  solidTextureColorNamePrevious,
} from './solid-texture-example-color-names';
import { OPERATION_MODE_VALUES, OperationMode } from './operation-mode';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/model/SolidTextureModel.java`.
 *
 * The model owns the scene, the camera, the two lights and the two gizmos, and
 * — what makes this example the one that exercises solid texturing — the
 * procedural volume itself: a cube of `solidTextureSize` cubed RGB samples,
 * rebuilt from scratch whenever the selected texture or the size changes, plus
 * the same samples kept as a stack of 2D slices so the second operation mode
 * can display them directly.
 *
 * Java's `byte[] solidTextureVolumeRgb8` holds signed bytes that are handed
 * straight to `glTexImage3D`; here it is a `Uint8Array`, which is what
 * `texImage3D` takes and which removes the sign masking Java's readers would
 * otherwise need. Nothing else about the generation differs: the palettes, the
 * per-texture coordinate scales, the 16-bit-to-8-bit channel reduction and the
 * `IMAGE_MAP` / `MATERIAL_MAP` fixtures are the Java ones.
 *
 * One cost is worth stating because it is Java's too, not a port defect:
 * rebuilding is O(size^3) evaluations of multi-octave noise, so the `[3]` key
 * doubles the work eightfold each time. A browser runs it on the one thread it
 * draws on, so the page stops responding for the duration where the Java
 * program's window merely stops redrawing.
 */
export class SolidTextureModel {
  private static readonly MIN_SOLID_TEXTURE_SIZE = 1;
  private static readonly MAX_SOLID_TEXTURE_SIZE = 256;
  private static readonly SMALL_TOLERANCE = 1.0e-6;

  private readonly camera: Camera;
  private readonly lights: Light[];
  private readonly scene: SimpleScene;
  private readonly qualitySelection: RendererConfiguration;
  private readonly rayGizmo: RayGizmo;
  private readonly infinitePlaneGizmo: InfinitePlaneGizmo;
  private readonly texture2DStack: Image[];
  private readonly colorTextureFixture: ColorTextureFixture;
  private readonly imageTexture: ImageTexture;
  private solidTextureSize: number;
  private solidTextureVolumeRgb8: Uint8Array;
  private solidTextureRevision: number;
  private selectedSolidTexture: SolidTextureExampleColorNames;
  private animationEnabled: boolean;
  private hudVisible: boolean;
  private operationMode: OperationMode;
  private tangibleServiceUrl = 'ws://localhost:8090/v1/values';

  constructor() {
    this.scene = new SimpleScene();
    this.camera = new Camera();
    this.qualitySelection = new RendererConfiguration();
    this.rayGizmo = new RayGizmo(this.makeIntersectionCallback(), 1);
    this.infinitePlaneGizmo = new InfinitePlaneGizmo();
    this.texture2DStack = [];
    const textureUtils = new TextureUtils();
    const proceduralNoise: ProceduralNoise = textureUtils.getProceduralNoise();
    proceduralNoise.initialize();
    this.colorTextureFixture = new ColorTextureFixture(proceduralNoise, textureUtils);
    this.imageTexture = new ImageTexture();
    this.solidTextureSize = 32;
    this.solidTextureVolumeRgb8 = new Uint8Array(0);
    this.solidTextureRevision = 0;
    this.selectedSolidTexture = SolidTextureExampleColorNames.CHECKER_TEXTURE;
    this.animationEnabled = false;
    this.hudVisible = true;
    this.rebuildTexture2DStack();
    this.operationMode = OperationMode.MESH_MODEL;
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

  getInfinitePlaneGizmo(): InfinitePlaneGizmo {
    return this.infinitePlaneGizmo;
  }

  getTexture2DStack(): Image[] {
    return this.texture2DStack;
  }

  getSolidTextureVolumeRgb8(): Uint8Array {
    return this.solidTextureVolumeRgb8;
  }

  getSolidTextureRevision(): number {
    return this.solidTextureRevision;
  }

  getSolidTextureSize(): number {
    return this.solidTextureSize;
  }

  getSelectedSolidTexture(): SolidTextureExampleColorNames {
    return this.selectedSolidTexture;
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

  isHudVisible(): boolean {
    return this.hudVisible;
  }

  toggleHudVisible(): void {
    this.hudVisible = !this.hudVisible;
  }

  advanceObjectRotationRadians(deltaRadians: number): void {
    const deltaRotation: Matrix4x4d = new Matrix4x4d().axisRotation(deltaRadians, 0.0, 0.0, 1.0);
    const bodies = this.scene.getSimpleBodies();
    for (let i = 0; i < bodies.size(); i++) {
      const body: SimpleBody = bodies.get(i);
      body.setRotation(deltaRotation.multiply(body.getRotation()));
    }
  }

  selectNextSolidTexture(): void {
    this.selectedSolidTexture = solidTextureColorNameNext(this.selectedSolidTexture);
    this.rebuildTexture2DStack();
  }

  selectPreviousSolidTexture(): void {
    this.selectedSolidTexture = solidTextureColorNamePrevious(this.selectedSolidTexture);
    this.rebuildTexture2DStack();
  }

  increaseSolidTextureSize(): void {
    if (this.solidTextureSize >= SolidTextureModel.MAX_SOLID_TEXTURE_SIZE) {
      return;
    }
    this.solidTextureSize *= 2;
    this.rebuildTexture2DStack();
  }

  decreaseSolidTextureSize(): void {
    if (this.solidTextureSize <= SolidTextureModel.MIN_SOLID_TEXTURE_SIZE) {
      return;
    }
    this.solidTextureSize /= 2;
    this.rebuildTexture2DStack();
  }

  getOperationMode(): OperationMode {
    return this.operationMode;
  }

  rotateOperationMode(): void {
    const modes = OPERATION_MODE_VALUES;
    const next: number = (modes.indexOf(this.operationMode) + 1) % modes.length;
    this.operationMode = modes[next]!;
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

  private rebuildTexture2DStack(): void {
    this.texture2DStack.length = 0;
    const side: number = Math.max(1, this.solidTextureSize);
    this.solidTextureVolumeRgb8 = new Uint8Array(side * side * side * 3);
    for (let i = 0; i < this.solidTextureSize; i++) {
      this.texture2DStack.push(this.generateTextureSlice(i));
    }
    this.solidTextureRevision++;
  }

  private generateTextureSlice(sliceIndex: number): Image {
    const image = new RGBImageUncompressed();
    const side: number = Math.max(1, this.solidTextureSize);
    image.initNoFill(side, side);
    const z: number = SolidTextureModel.unitCoordinate(sliceIndex, this.solidTextureSize);
    let imageMap: ControlledRGBAImageHDRUncompressed | null = null;
    let materialMap: ControlledRGBAImageHDRUncompressed | null = null;

    if (this.selectedSolidTexture === SolidTextureExampleColorNames.IMAGE_MAP_TEXTURE) {
      imageMap = this.buildImageMapTexture();
    }
    if (this.selectedSolidTexture === SolidTextureExampleColorNames.MATERIAL_MAP_TEXTURE) {
      materialMap = this.buildMaterialMapTexture();
    }

    for (let y = 0; y < side; y++) {
      const py: number = SolidTextureModel.unitCoordinate(y, side);
      for (let x = 0; x < side; x++) {
        const px: number = SolidTextureModel.unitCoordinate(x, side);
        const color: ColorRgba = this.colorAt(px, py, z, imageMap, materialMap);
        SolidTextureModel.putColor16As8(image, x, y, color);
        this.putVolumeColor16As8(side, x, y, sliceIndex, color);
      }
    }
    return image;
  }

  private colorAt(
    x: number,
    y: number,
    z: number,
    imageMap: ControlledRGBAImageHDRUncompressed | null,
    materialMap: ControlledRGBAImageHDRUncompressed | null,
  ): ColorRgba {
    const color = new ColorRgba();
    const palette: RGBAColorPalette = SolidTextureModel.defaultPalette();

    switch (this.selectedSolidTexture) {
      case SolidTextureExampleColorNames.NO_TEXTURE:
        color.set(new ColorRgba(0.18, 0.18, 0.18, 1.0));
        break;
      case SolidTextureExampleColorNames.COLOUR_TEXTURE:
        color.set(new ColorRgba(0.72, 0.38, 0.18, 1.0));
        break;
      case SolidTextureExampleColorNames.BOZO_TEXTURE:
        this.colorTextureFixture.bozo(x * 6.0, y * 6.0, z * 6.0, 1.25, 7, palette, color);
        break;
      case SolidTextureExampleColorNames.MARBLE_TEXTURE:
        this.colorTextureFixture.marble(x * 8.0, y * 8.0, z * 8.0, 1.65, 7, palette, color);
        break;
      case SolidTextureExampleColorNames.WOOD_TEXTURE:
        this.colorTextureFixture.wood(
          x * 9.0,
          y * 9.0,
          z * 9.0,
          2.2,
          6,
          SolidTextureModel.woodPalette(),
          color,
        );
        break;
      case SolidTextureExampleColorNames.CHECKER_TEXTURE:
        this.colorTextureFixture.checker(
          x * 8.0,
          y * 8.0,
          z * 8.0,
          color,
          new ColorRgba(0.95, 0.95, 0.95, 1.0),
          new ColorRgba(0.05, 0.08, 0.12, 1.0),
          SolidTextureModel.SMALL_TOLERANCE,
        );
        break;
      case SolidTextureExampleColorNames.CHECKER_TEXTURE_TEXTURE:
        this.checkerTextureTexture(x, y, z, color);
        break;
      case SolidTextureExampleColorNames.SPOTTED_TEXTURE:
        this.colorTextureFixture.spotted(x * 7.0, y * 7.0, z * 7.0, palette, color);
        break;
      case SolidTextureExampleColorNames.AGATE_TEXTURE:
        this.colorTextureFixture.agate(
          x * 8.0,
          y * 8.0,
          z * 8.0,
          7,
          SolidTextureModel.agatePalette(),
          color,
        );
        break;
      case SolidTextureExampleColorNames.GRANITE_TEXTURE:
        this.colorTextureFixture.granite(
          x * 6.0,
          y * 6.0,
          z * 6.0,
          SolidTextureModel.granitePalette(),
          color,
        );
        break;
      case SolidTextureExampleColorNames.GRADIENT_TEXTURE:
        this.colorTextureFixture.gradient(
          x * 4.0,
          y * 4.0,
          z * 4.0,
          0.45,
          palette,
          new Vector3Dd(1.0, 1.0, 1.0),
          5,
          color,
        );
        break;
      case SolidTextureExampleColorNames.IMAGE_MAP_TEXTURE:
        this.imageTexture.imageMap(x, y, z, imageMap!, color, SolidTextureModel.SMALL_TOLERANCE);
        break;
      case SolidTextureExampleColorNames.ONION_TEXTURE:
        this.colorTextureFixture.onion(x * 10.0, y * 10.0, z * 10.0, 0.65, 6, palette, color);
        break;
      case SolidTextureExampleColorNames.LEOPARD_TEXTURE:
        this.colorTextureFixture.leopard(
          x * 12.0,
          y * 12.0,
          z * 12.0,
          1.0,
          6,
          SolidTextureModel.leopardPalette(),
          color,
        );
        break;
      case SolidTextureExampleColorNames.BRICK_TEXTURE:
        this.colorTextureFixture.brick(
          x * 10.0,
          y * 7.0,
          z * 5.0,
          color,
          new ColorRgba(0.78, 0.78, 0.72, 1.0),
          new ColorRgba(0.58, 0.12, 0.07, 1.0),
          0.08,
        );
        break;
      case SolidTextureExampleColorNames.MATERIAL_MAP_TEXTURE:
        this.materialMapTexture(x, y, z, materialMap!, color);
        break;
    }
    if (color.getA() === 0.0) {
      color.setA(1.0);
    }
    return color;
  }

  private checkerTextureTexture(x: number, y: number, z: number, color: ColorRgba): void {
    const index: number = Math.trunc(
      TextureUtils.floorInline(x * 8.0 + SolidTextureModel.SMALL_TOLERANCE) +
        TextureUtils.floorInline(y * 8.0 + SolidTextureModel.SMALL_TOLERANCE) +
        TextureUtils.floorInline(z * 8.0 + SolidTextureModel.SMALL_TOLERANCE),
    );
    if ((index & 1) !== 0) {
      this.colorTextureFixture.wood(
        x * 9.0,
        y * 9.0,
        z * 9.0,
        1.7,
        6,
        SolidTextureModel.woodPalette(),
        color,
      );
    } else {
      this.colorTextureFixture.marble(
        x * 8.0,
        y * 8.0,
        z * 8.0,
        1.25,
        6,
        SolidTextureModel.defaultPalette(),
        color,
      );
    }
  }

  private materialMapTexture(
    x: number,
    y: number,
    z: number,
    materialMap: ControlledRGBAImageHDRUncompressed,
    color: ColorRgba,
  ): void {
    const material: number = this.imageTexture.materialMap(
      new Vector3Dd(x, y, z),
      null,
      materialMap,
      4,
      SolidTextureModel.SMALL_TOLERANCE,
    );
    switch (material) {
      case 0:
        this.colorTextureFixture.wood(
          x * 9.0,
          y * 9.0,
          z * 9.0,
          1.6,
          5,
          SolidTextureModel.woodPalette(),
          color,
        );
        break;
      case 1:
        this.colorTextureFixture.granite(
          x * 6.0,
          y * 6.0,
          z * 6.0,
          SolidTextureModel.granitePalette(),
          color,
        );
        break;
      case 2:
        this.colorTextureFixture.leopard(
          x * 12.0,
          y * 12.0,
          z * 12.0,
          0.9,
          5,
          SolidTextureModel.leopardPalette(),
          color,
        );
        break;
      default:
        this.colorTextureFixture.checker(
          x * 8.0,
          y * 8.0,
          z * 8.0,
          color,
          new ColorRgba(0.12, 0.25, 0.65, 1.0),
          new ColorRgba(0.95, 0.92, 0.4, 1.0),
          SolidTextureModel.SMALL_TOLERANCE,
        );
        break;
    }
  }

  private buildImageMapTexture(): ControlledRGBAImageHDRUncompressed {
    const image = new ControlledRGBAImageHDRUncompressed();
    const side: number = Math.max(1, this.solidTextureSize);
    image.allocate(side, side);
    image.setMapType(ImageToSolidTextureProjectionMethods.PLANAR_MAP);
    image.setInterpolationType(ImageToSolidTextureInterpolationTypes.BI_LINEAR);
    image.setImageGradient(new Vector3Dd(1.0, -1.0, 0.0));

    for (let y = 0; y < side; y++) {
      for (let x = 0; x < side; x++) {
        const u: number = SolidTextureModel.unitCoordinate(x, side);
        const v: number = SolidTextureModel.unitCoordinate(y, side);
        const pixel = new RGBAPixelHDR();
        pixel.r = SolidTextureModel.toChannel8(u);
        pixel.g = SolidTextureModel.toChannel8(v);
        pixel.b = SolidTextureModel.toChannel8((x / 8.0 + y / 8.0) % 2 === 0 ? 0.85 : 0.2);
        pixel.a = SolidTextureModel.toChannel8(1.0);
        image.setPixel(x, y, pixel);
      }
    }
    return image;
  }

  private buildMaterialMapTexture(): ControlledRGBAImageHDRUncompressed {
    const image = new ControlledRGBAImageHDRUncompressed();
    const side: number = Math.max(1, this.solidTextureSize);
    image.allocate(side, side);
    image.setMapType(ImageToSolidTextureProjectionMethods.PLANAR_MAP);
    image.setInterpolationType(ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION);
    image.setImageGradient(new Vector3Dd(1.0, -1.0, 0.0));
    image.setUseColorFlag(false);

    const indexed = new IndexedColorImageHDRUncompressed();
    indexed.allocate(side, side);
    indexed.setColorMapSize(4);
    const table: RGBAPixelHDR[] = new Array<RGBAPixelHDR>(4);
    for (let i = 0; i < table.length; i++) {
      table[i] = new RGBAPixelHDR();
      table[i]!.r = SolidTextureModel.toChannel8(i / 3.0);
      table[i]!.g = SolidTextureModel.toChannel8(1.0 - i / 3.0);
      table[i]!.b = SolidTextureModel.toChannel8((i & 1) === 0 ? 0.2 : 0.85);
      table[i]!.a = SolidTextureModel.toChannel8(1.0);
    }
    indexed.setColorTable(table);
    const cell: number = Math.max(1, Math.trunc(side / 5));
    for (let y = 0; y < side; y++) {
      for (let x = 0; x < side; x++) {
        indexed.setPixel(x, y, (Math.trunc(x / cell) + Math.trunc(y / cell)) & 3);
      }
    }
    image.setIndexedData(indexed);
    return image;
  }

  private static defaultPalette(): RGBAColorPalette {
    const palette = new RGBAColorPalette();
    palette.addColor(0.05, 0.12, 0.28, 1.0);
    palette.addColor(0.15, 0.55, 0.75, 1.0);
    palette.addColor(0.95, 0.78, 0.26, 1.0);
    palette.addColor(0.88, 0.18, 0.14, 1.0);
    return palette;
  }

  private static woodPalette(): RGBAColorPalette {
    const palette = new RGBAColorPalette();
    palette.addColor(0.25, 0.11, 0.04, 1.0);
    palette.addColor(0.64, 0.34, 0.13, 1.0);
    palette.addColor(0.88, 0.61, 0.28, 1.0);
    palette.addColor(0.33, 0.16, 0.07, 1.0);
    return palette;
  }

  private static agatePalette(): RGBAColorPalette {
    const palette = new RGBAColorPalette();
    palette.addColor(0.98, 0.92, 0.72, 1.0);
    palette.addColor(0.72, 0.36, 0.18, 1.0);
    palette.addColor(0.28, 0.12, 0.08, 1.0);
    palette.addColor(0.95, 0.78, 0.5, 1.0);
    return palette;
  }

  private static granitePalette(): RGBAColorPalette {
    const palette = new RGBAColorPalette();
    palette.addColor(0.08, 0.08, 0.09, 1.0);
    palette.addColor(0.32, 0.32, 0.34, 1.0);
    palette.addColor(0.7, 0.68, 0.64, 1.0);
    palette.addColor(0.16, 0.14, 0.13, 1.0);
    return palette;
  }

  private static leopardPalette(): RGBAColorPalette {
    const palette = new RGBAColorPalette();
    palette.addColor(0.05, 0.03, 0.015, 1.0);
    palette.addColor(0.86, 0.53, 0.12, 1.0);
    palette.addColor(0.96, 0.76, 0.26, 1.0);
    palette.addColor(0.12, 0.06, 0.02, 1.0);
    return palette;
  }

  private static unitCoordinate(index: number, count: number): number {
    if (count <= 1) {
      return 0.0;
    }
    return index / (count - 1);
  }

  private static putColor16As8(
    image: RGBImageUncompressed,
    x: number,
    y: number,
    color: ColorRgba,
  ): void {
    image.putPixel(
      x,
      y,
      SolidTextureModel.channel16To8(color.getR()),
      SolidTextureModel.channel16To8(color.getG()),
      SolidTextureModel.channel16To8(color.getB()),
    );
  }

  private putVolumeColor16As8(
    side: number,
    x: number,
    y: number,
    z: number,
    color: ColorRgba,
  ): void {
    const base: number = (z * side * side + y * side + x) * 3;
    this.solidTextureVolumeRgb8[base] = SolidTextureModel.channel16To8(color.getR());
    this.solidTextureVolumeRgb8[base + 1] = SolidTextureModel.channel16To8(color.getG());
    this.solidTextureVolumeRgb8[base + 2] = SolidTextureModel.channel16To8(color.getB());
  }

  /**
   * Java answers a `char`, the 0..65535 payload of an `RGBAPixelHDR` channel;
   * TypeScript has no `char`, and the field is a plain number.
   */
  private static toChannel8(value: number): number {
    const channel: number = Math.round(SolidTextureModel.clamp01(value) * 255.0);
    return channel & 0xff;
  }

  private static channel16To8(value: number): number {
    const channel16: number = Math.round(SolidTextureModel.clamp01(value) * 65535.0);
    return (channel16 >>> 8) & 0xff;
  }

  private static clamp01(value: number): number {
    if (value < 0.0) {
      return 0.0;
    }
    return Math.min(value, 1.0);
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
    const minMax: number[] = group.getMinMax();
    if (minMax.length < 6) {
      return;
    }

    const min = new Vector3Dd(minMax[0]!, minMax[1]!, minMax[2]!);
    const max = new Vector3Dd(minMax[3]!, minMax[4]!, minMax[5]!);
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
