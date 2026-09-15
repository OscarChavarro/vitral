import {
  Camera,
  ColorRgb,
  PointLight,
  Polygon2D,
  RendererConfiguration,
  Vector3Dd,
  type Light,
  type _Polygon2DWA,
} from '@vitral/base';
import { POLYGON_CLIPPING_CASES } from './polygon-clipping-fixtures';
import type { PolygonClippingTestCase } from './polygon-clipping-test-case';
import {
  nextPolygonClippingOperation,
  type PolygonClippingOperation,
} from './polygon-clipping-operation';
import {
  nextPolygonSurfaceTessellationMode,
  type PolygonSurfaceTessellationMode,
} from './polygon-surface-tessellation-mode';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/model/PolygonClippingDebuggerModel.java`.
 *
 * The constructor builds the same state in the same order: an orthogonal
 * camera looking at the panel from `(14, -18, 12)` with the zoom divided by
 * sixteen, a wires-only renderer configuration, the orbiter and quality
 * controllers, one white point light, no polygons yet, the first test case,
 * the intersection operation, the GLU surface mode, every display flag on, and
 * no error.
 *
 * Three groups of Java fields have no counterpart here and are left out rather
 * than stubbed:
 *
 *   - `mainFrame`, `windowedBounds` and `fullScreenMode` describe the
 *     `JFrame` the Java program owns, and drive `toggleFullscreenMode`. A
 *     module inside the container owns no window; the `[F]` key asks the
 *     browser for the fullscreen element instead, which is state the
 *     browser keeps.
 *   - `camera`, `quality`, `qualityController` and `cameraController` are
 *     settable in Java through plain setters no caller uses. They are
 *     constructed once here and only read, as the program does.
 *   - `setErrorState(boolean)` and `setErrorMessage` are the second, unused
 *     half of the error pair; the program only ever calls
 *     `setErrorState(String)` and `clearErrorState`, which are both here.
 *
 * The controllers themselves are the container's `ExampleCameraInteraction`
 * and `ExampleRendererConfigurationInteraction`, standing in for
 * `CameraControllerOrbiter` and `RendererConfigurationController` until the
 * plan's decision-gated Phase 38 ports that family; the component owns them,
 * because they need the camera this model builds.
 */
export class PolygonClippingDebuggerModel {
  private readonly camera: Camera;
  private readonly light: Light;
  private readonly quality: RendererConfiguration;

  private clipPolygon: Polygon2D | null;
  private subjectPolygon: Polygon2D | null;
  private innerPolygon: Polygon2D | null;
  private outerPolygon: Polygon2D | null;
  private clipPolygonWA: _Polygon2DWA | null;
  private subjectPolygonWA: _Polygon2DWA | null;

  private testIndex: number;
  private operation: PolygonClippingOperation;
  private polygonSurfaceTessellationMode: PolygonSurfaceTessellationMode;
  private showReferenceFrame: boolean;
  private showClipPolygon: boolean;
  private showSubjectPolygon: boolean;
  private showInnerPolygon: boolean;
  private showOuterPolygon: boolean;
  private showIntersections: boolean;
  private showFilledPolygons: boolean;
  private takeSnapshot: boolean;
  private snapshotNumber: number;

  private errorState: boolean;
  private errorMessage: string;

  constructor() {
    this.camera = new Camera();
    this.camera.setPosition(new Vector3Dd(14, -18, 12));
    this.camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(6, 0, 4));
    this.camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
    this.camera.setOrthogonalZoom(this.camera.getOrthogonalZoom() / 16.0);

    this.quality = new RendererConfiguration();
    this.quality.changeWires();

    this.light = new PointLight(new Vector3Dd(10, -20, 50), new ColorRgb(1, 1, 1));

    this.clipPolygon = null;
    this.subjectPolygon = null;
    this.innerPolygon = null;
    this.outerPolygon = null;
    this.clipPolygonWA = null;
    this.subjectPolygonWA = null;

    this.testIndex = 0;
    this.operation = 'INTERSECTION';
    this.polygonSurfaceTessellationMode = 'GLU';
    this.showReferenceFrame = true;
    this.showClipPolygon = true;
    this.showSubjectPolygon = true;
    this.showInnerPolygon = true;
    this.showOuterPolygon = true;
    this.showIntersections = true;
    this.showFilledPolygons = true;
    this.takeSnapshot = false;
    this.snapshotNumber = 1;

    this.errorState = false;
    this.errorMessage = '';
  }

  getCurrentTestCase(): PolygonClippingTestCase {
    return POLYGON_CLIPPING_CASES[this.testIndex]!;
  }

  getTotalTestCases(): number {
    return POLYGON_CLIPPING_CASES.length;
  }

  stepTest(delta: number): void {
    const total: number = this.getTotalTestCases();
    this.testIndex = (this.testIndex + delta) % total;
    if (this.testIndex < 0) {
      this.testIndex += total;
    }
  }

  cycleOperation(): void {
    this.operation = nextPolygonClippingOperation(this.operation);
  }

  cyclePolygonSurfaceTessellationMode(): void {
    this.polygonSurfaceTessellationMode = nextPolygonSurfaceTessellationMode(
      this.polygonSurfaceTessellationMode,
    );
  }

  clearErrorState(): void {
    this.errorState = false;
    this.errorMessage = '';
  }

  setErrorState(message: string): void {
    this.errorState = true;
    this.errorMessage = message;
    console.error('[PolygonClippingExample] ' + this.errorMessage);
  }

  getCamera(): Camera {
    return this.camera;
  }

  getLight(): Light {
    return this.light;
  }

  getQuality(): RendererConfiguration {
    return this.quality;
  }

  getClipPolygon(): Polygon2D | null {
    return this.clipPolygon;
  }

  setClipPolygon(clipPolygon: Polygon2D | null): void {
    this.clipPolygon = clipPolygon;
  }

  getSubjectPolygon(): Polygon2D | null {
    return this.subjectPolygon;
  }

  setSubjectPolygon(subjectPolygon: Polygon2D | null): void {
    this.subjectPolygon = subjectPolygon;
  }

  getInnerPolygon(): Polygon2D | null {
    return this.innerPolygon;
  }

  setInnerPolygon(innerPolygon: Polygon2D | null): void {
    this.innerPolygon = innerPolygon;
  }

  getOuterPolygon(): Polygon2D | null {
    return this.outerPolygon;
  }

  setOuterPolygon(outerPolygon: Polygon2D | null): void {
    this.outerPolygon = outerPolygon;
  }

  getClipPolygonWA(): _Polygon2DWA | null {
    return this.clipPolygonWA;
  }

  setClipPolygonWA(clipPolygonWA: _Polygon2DWA | null): void {
    this.clipPolygonWA = clipPolygonWA;
  }

  getSubjectPolygonWA(): _Polygon2DWA | null {
    return this.subjectPolygonWA;
  }

  setSubjectPolygonWA(subjectPolygonWA: _Polygon2DWA | null): void {
    this.subjectPolygonWA = subjectPolygonWA;
  }

  getTestIndex(): number {
    return this.testIndex;
  }

  setTestIndex(testIndex: number): void {
    this.testIndex = testIndex;
  }

  getOperation(): PolygonClippingOperation {
    return this.operation;
  }

  setOperation(operation: PolygonClippingOperation): void {
    this.operation = operation;
  }

  getPolygonSurfaceTessellationMode(): PolygonSurfaceTessellationMode {
    return this.polygonSurfaceTessellationMode;
  }

  setPolygonSurfaceTessellationMode(mode: PolygonSurfaceTessellationMode): void {
    this.polygonSurfaceTessellationMode = mode;
  }

  isShowReferenceFrame(): boolean {
    return this.showReferenceFrame;
  }

  setShowReferenceFrame(showReferenceFrame: boolean): void {
    this.showReferenceFrame = showReferenceFrame;
  }

  isShowClipPolygon(): boolean {
    return this.showClipPolygon;
  }

  setShowClipPolygon(showClipPolygon: boolean): void {
    this.showClipPolygon = showClipPolygon;
  }

  isShowSubjectPolygon(): boolean {
    return this.showSubjectPolygon;
  }

  setShowSubjectPolygon(showSubjectPolygon: boolean): void {
    this.showSubjectPolygon = showSubjectPolygon;
  }

  isShowInnerPolygon(): boolean {
    return this.showInnerPolygon;
  }

  setShowInnerPolygon(showInnerPolygon: boolean): void {
    this.showInnerPolygon = showInnerPolygon;
  }

  isShowOuterPolygon(): boolean {
    return this.showOuterPolygon;
  }

  setShowOuterPolygon(showOuterPolygon: boolean): void {
    this.showOuterPolygon = showOuterPolygon;
  }

  isShowIntersections(): boolean {
    return this.showIntersections;
  }

  setShowIntersections(showIntersections: boolean): void {
    this.showIntersections = showIntersections;
  }

  isShowFilledPolygons(): boolean {
    return this.showFilledPolygons;
  }

  setShowFilledPolygons(showFilledPolygons: boolean): void {
    this.showFilledPolygons = showFilledPolygons;
  }

  isTakeSnapshot(): boolean {
    return this.takeSnapshot;
  }

  setTakeSnapshot(takeSnapshot: boolean): void {
    this.takeSnapshot = takeSnapshot;
  }

  getSnapshotNumber(): number {
    return this.snapshotNumber;
  }

  setSnapshotNumber(snapshotNumber: number): void {
    this.snapshotNumber = snapshotNumber;
  }

  isErrorState(): boolean {
    return this.errorState;
  }

  getErrorMessage(): string {
    return this.errorMessage;
  }
}
