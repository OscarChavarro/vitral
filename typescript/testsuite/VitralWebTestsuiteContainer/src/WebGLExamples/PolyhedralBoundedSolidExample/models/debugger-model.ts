import {
  Camera,
  ColorRgb,
  CsgKurlanderBowlFixture,
  JavaMath,
  Light,
  Matrix4x4d,
  PointLight,
  RendererConfiguration,
  SimpleMaterial,
  Vector3Dd,
  type PolyhedralBoundedSolid,
} from '@vitral/base';
import { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';
import { ExampleRendererConfigurationInteraction } from '../../_shared/example-renderer-configuration-interaction';
import { appelDisplayModeNextCircular, type AppelDisplayMode } from './appel-display-mode';
import type { CsgOperationNames } from './csg-operation-names';
import { csgSamplePreferredOperation, type CsgSampleNames } from './csg-sample-names';
import type { SolidModelNames } from './solid-model-names';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/DebuggerModel.java`.
 *
 * The debugger's whole state: the model being shown and the parameters it is
 * built from, the camera, the material and the two point lights, the solid
 * with its two CSG preview operands, the face and edge selections, every
 * display switch, the error message the HUD prints, and the Z rotation the
 * animation advances. The field set, the defaults — `STEP_IMPORT`, the
 * Kurlander bowl motif 38, sixteen by eight subdivisions, wires on under Phong
 * shading, the camera at (2, -1, 2) turned 135 and -35 degrees, the bluish
 * material, the white and the pink light — and every accessor are Java's.
 *
 * Three Java fields have no counterpart. `mainFrame`, `windowedBounds` and
 * `fullScreenMode` keep the `JFrame` state Java's `toggleFullscreenMode`
 * rebuilds; a page asks the browser for its fullscreen element instead, and
 * the browser keeps that state, as the `PolygonClippingExample` module
 * records. `canvas` is the `<canvas>` the component draws into, where Java's
 * is the JOGL `GLCanvas`.
 *
 * The camera and quality controllers are the container's stand-ins for
 * `CameraControllerOrbiter` and `RendererConfigurationController`, whose Phase
 * 38 group is not ported. The orbit stand-in imposes its own default view when
 * it is constructed, so the two camera lines of Java's constructor live in
 * `configureInitialView()` and are applied again once it exists, and
 * `adoptCameraState()` then takes that view as the orbit state — the note
 * `MD2Example` and `ShadersExample` carry.
 */
export class DebuggerModel {
  private static readonly MIN_SUBDIVISION_CIRCUMFERENCE = 3;
  private static readonly MIN_SUBDIVISION_HEIGHT = 1;

  private solidModelName: SolidModelNames = 'STEP_IMPORT';
  private csgSample: CsgSampleNames = 'KURLANDER_BOWL_SINGLE_MOTIF';
  private kurlanderBowlSingleMotifIndex = 38;
  private subdivisionCircumference = 16;
  private subdivisionHeight = 8;
  private readonly camera: Camera;
  private readonly material: SimpleMaterial;
  private readonly light1: Light;
  private readonly light2: Light;
  private solid: PolyhedralBoundedSolid | null = null;
  private csgPreviewOperandA: PolyhedralBoundedSolid | null = null;
  private csgPreviewOperandB: PolyhedralBoundedSolid | null = null;
  private faceIndex = -2;
  private edgeIndex = -2;
  private debugVertices = false;
  private readonly quality: RendererConfiguration;
  private readonly qualityController: ExampleRendererConfigurationInteraction;
  private readonly cameraController: ExampleCameraInteraction;
  private canvas: HTMLCanvasElement | null = null;
  private csgOperation: CsgOperationNames = 'DIFFERENCE_A_MINUS_B';
  private debugEdges = false;
  private appelDisplayMode: AppelDisplayMode = 'OFF';
  private showCoordinateSystem = false;
  private debugCsg = false;
  private errorState = false;
  private errorMessage = '';
  private hudEnabled = true;
  private solidAnimationEnabled = false;
  private solidRotationZDegrees = 0.0;

  constructor() {
    this.camera = new Camera();
    this.configureInitialView();

    this.quality = new RendererConfiguration();
    this.quality.changeWires();
    this.quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
    this.qualityController = new ExampleRendererConfigurationInteraction(this.quality);
    this.cameraController = new ExampleCameraInteraction(this.camera);
    this.configureInitialView();
    this.cameraController.adoptCameraState();

    this.material = this.defaultMaterial();
    this.light1 = new PointLight(new Vector3Dd(3, -3, 2), new ColorRgb(1, 1, 1));
    this.light2 = new PointLight(new Vector3Dd(-2, 5, -2), new ColorRgb(0.9, 0.5, 0.5));
    this.light1.setId(0);
    this.light2.setId(1);
  }

  /**
   * The two camera lines of Java's constructor, in a method so that they can
   * be applied again after the orbit stand-in has imposed its own view; see
   * the class comment.
   */
  configureInitialView(): void {
    this.camera.setPosition(new Vector3Dd(2, -1, 2));
    let rotationMatrix = new Matrix4x4d();
    rotationMatrix = rotationMatrix.eulerAnglesRotation(
      JavaMath.toRadians(135),
      JavaMath.toRadians(-35),
      0,
    );
    this.camera.setRotation(rotationMatrix);
  }

  private defaultMaterial(): SimpleMaterial {
    let m = new SimpleMaterial();

    m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(new ColorRgb(0.5, 0.5, 0.9));
    m = m.withSpecular(new ColorRgb(1, 1, 1));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(100);
    return m;
  }

  clearErrorState(): void {
    this.errorState = false;
    this.errorMessage = '';
  }

  setErrorState(message: string): void {
    this.errorState = true;
    this.errorMessage = message;
    console.error('[PolyhedralBoundedSolidExample] ' + this.errorMessage);
  }

  clampSubdivisions(): void {
    if (this.subdivisionCircumference < DebuggerModel.MIN_SUBDIVISION_CIRCUMFERENCE) {
      this.subdivisionCircumference = DebuggerModel.MIN_SUBDIVISION_CIRCUMFERENCE;
    }
    if (this.subdivisionHeight < DebuggerModel.MIN_SUBDIVISION_HEIGHT) {
      this.subdivisionHeight = DebuggerModel.MIN_SUBDIVISION_HEIGHT;
    }
  }

  getFaceCount(): number {
    if (this.solid === null || this.solid.getPolygonsList() === null) {
      return 0;
    }
    return this.solid.getPolygonsList().size();
  }

  clampFaceIndex(): void {
    if (this.faceIndex < -2) {
      this.faceIndex = -2;
      return;
    }

    const totalFaces: number = this.getFaceCount();
    const maxFaceIndex: number = totalFaces - 1;
    if (this.faceIndex > maxFaceIndex) {
      this.faceIndex = maxFaceIndex;
    }
  }

  getSolidModelName(): SolidModelNames {
    return this.solidModelName;
  }

  setSolidModelName(solidModelName: SolidModelNames): void {
    this.solidModelName = solidModelName;
  }

  getSubdivisionCircumference(): number {
    return this.subdivisionCircumference;
  }

  setSubdivisionCircumference(subdivisionCircumference: number): void {
    this.subdivisionCircumference = subdivisionCircumference;
  }

  getSubdivisionHeight(): number {
    return this.subdivisionHeight;
  }

  setSubdivisionHeight(subdivisionHeight: number): void {
    this.subdivisionHeight = subdivisionHeight;
  }

  getCamera(): Camera {
    return this.camera;
  }

  getMaterial(): SimpleMaterial {
    return this.material;
  }

  getLight1(): Light {
    return this.light1;
  }

  getLight2(): Light {
    return this.light2;
  }

  getSolid(): PolyhedralBoundedSolid | null {
    return this.solid;
  }

  setSolid(solid: PolyhedralBoundedSolid | null): void {
    this.solid = solid;
  }

  getCsgPreviewOperandA(): PolyhedralBoundedSolid | null {
    return this.csgPreviewOperandA;
  }

  setCsgPreviewOperandA(csgPreviewOperandA: PolyhedralBoundedSolid | null): void {
    this.csgPreviewOperandA = csgPreviewOperandA;
  }

  getCsgPreviewOperandB(): PolyhedralBoundedSolid | null {
    return this.csgPreviewOperandB;
  }

  setCsgPreviewOperandB(csgPreviewOperandB: PolyhedralBoundedSolid | null): void {
    this.csgPreviewOperandB = csgPreviewOperandB;
  }

  getFaceIndex(): number {
    return this.faceIndex;
  }

  setFaceIndex(faceIndex: number): void {
    this.faceIndex = faceIndex;
  }

  getEdgeIndex(): number {
    return this.edgeIndex;
  }

  setEdgeIndex(edgeIndex: number): void {
    this.edgeIndex = edgeIndex;
  }

  getAppelDisplayMode(): AppelDisplayMode {
    return this.appelDisplayMode;
  }

  setAppelDisplayMode(appelDisplayMode: AppelDisplayMode): void {
    this.appelDisplayMode = appelDisplayMode;
  }

  cycleAppelDisplayMode(): void {
    this.appelDisplayMode = appelDisplayModeNextCircular(this.appelDisplayMode);
  }

  notDebugVertices(): boolean {
    return !this.debugVertices;
  }

  setDebugVertices(debugVertices: boolean): void {
    this.debugVertices = debugVertices;
  }

  getQuality(): RendererConfiguration {
    return this.quality;
  }

  getQualityController(): ExampleRendererConfigurationInteraction {
    return this.qualityController;
  }

  getCameraController(): ExampleCameraInteraction {
    return this.cameraController;
  }

  getCanvas(): HTMLCanvasElement | null {
    return this.canvas;
  }

  setCanvas(canvas: HTMLCanvasElement | null): void {
    this.canvas = canvas;
  }

  getCsgOperation(): CsgOperationNames {
    return this.csgOperation;
  }

  setCsgOperation(csgOperation: CsgOperationNames): void {
    this.csgOperation = csgOperation;
  }

  getCsgSample(): CsgSampleNames {
    return this.csgSample;
  }

  setCsgSample(csgSample: CsgSampleNames): void {
    this.csgSample = csgSample;
    this.csgOperation = csgSamplePreferredOperation(csgSample, this.csgOperation);
  }

  getKurlanderBowlSingleMotifIndex(): number {
    return this.kurlanderBowlSingleMotifIndex;
  }

  setKurlanderBowlSingleMotifIndex(motifIndex: number): void {
    this.kurlanderBowlSingleMotifIndex =
      CsgKurlanderBowlFixture.normalizeSingleMotifIndex(motifIndex);
  }

  getKurlanderBowlSingleMotifLabel(): string {
    return CsgKurlanderBowlFixture.describeSingleMotif(this.kurlanderBowlSingleMotifIndex);
  }

  usesKurlanderBowlSingleMotifControls(): boolean {
    if (this.csgSample !== 'KURLANDER_BOWL_SINGLE_MOTIF') {
      return false;
    }
    return (
      this.solidModelName === 'CSG_DIRECT' ||
      this.solidModelName === 'CSG_OPERAND1_PARTIAL' ||
      this.solidModelName === 'CSG_OPERAND2_PARTIAL'
    );
  }

  isDebugEdges(): boolean {
    return this.debugEdges;
  }

  setDebugEdges(debugEdges: boolean): void {
    this.debugEdges = debugEdges;
  }

  isShowCoordinateSystem(): boolean {
    return this.showCoordinateSystem;
  }

  setShowCoordinateSystem(showCoordinateSystem: boolean): void {
    this.showCoordinateSystem = showCoordinateSystem;
  }

  isDebugCsg(): boolean {
    return this.debugCsg;
  }

  setDebugCsg(debugCsg: boolean): void {
    this.debugCsg = debugCsg;
  }

  isErrorState(): boolean {
    return this.errorState;
  }

  getErrorMessage(): string {
    return this.errorMessage;
  }

  isHudEnabled(): boolean {
    return this.hudEnabled;
  }

  setHudEnabled(hudEnabled: boolean): void {
    this.hudEnabled = hudEnabled;
  }

  isSolidAnimationEnabled(): boolean {
    return this.solidAnimationEnabled;
  }

  setSolidAnimationEnabled(solidAnimationEnabled: boolean): void {
    this.solidAnimationEnabled = solidAnimationEnabled;
  }

  toggleSolidAnimationEnabled(): void {
    this.solidAnimationEnabled = !this.solidAnimationEnabled;
  }

  getSolidRotationZDegrees(): number {
    return this.solidRotationZDegrees;
  }

  rotateSolidAroundZDegrees(deltaDegrees: number): void {
    this.solidRotationZDegrees += deltaDegrees;
  }

  getSolidModelMatrix(): Matrix4x4d {
    return new Matrix4x4d().axisRotation(JavaMath.toRadians(this.solidRotationZDegrees), 0, 0, 1);
  }
}
