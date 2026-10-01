import {
  Intersection,
  Ray,
  RayGizmo,
  RayHit,
  VSDK,
  ViewportSet,
  type ArrayList,
  type Camera,
  type Light,
  type PointLight,
  type RGBColorPalette,
  type RGBImageUncompressed,
  type SimpleBody,
  type Widget,
  type ZBuffer,
} from '@vitral/base';
import { DrawingArea } from './drawing-area';
import { GuiState } from './gui-state';
import { EditHistory } from './history/edit-history';
import type { Scene } from './scene';
import { SceneLightFactory } from './scene-light-factory';

/**
 * Port of `model.ApplicationModel`.
 *
 * Java's `getViewportSets` returns a read-only `List` view; here it is a
 * `readonly` array view of the same list.
 */
export class ApplicationModel {
  private scene: Scene | null = null;
  private raytracedImage: RGBImageUncompressed | null = null;
  /// OpenGL depth of `raytracedImage` when it is shown in a viewport
  private raytracedDepth: ZBuffer | null = null;
  private zbufferImage: RGBImageUncompressed | null = null;
  private raytracedImageWidth = 0;
  private raytracedImageHeight = 0;
  private palette: RGBColorPalette | null = null;
  private rayGizmo: RayGizmo | null = null;
  private readonly viewportSets: ViewportSet[];
  private activeViewportSetIndex: number;
  private i18nContext: Widget | null = null;
  private readonly lightFactory: SceneLightFactory = new SceneLightFactory();
  private readonly drawingArea: DrawingArea;
  private readonly guiState: GuiState = new GuiState();
  private readonly editHistory: EditHistory = new EditHistory(() => this.getScene());

  /**
   * Creates the model with one standard `ViewportSet`. More sets can be added
   * to support users working with several displays.
   */
  constructor() {
    this.viewportSets = [];
    this.viewportSets.push(ViewportSet.createStandardSet('Display 1'));
    this.activeViewportSetIndex = 0;
    this.drawingArea = new DrawingArea(this.viewportSets[0]);
  }

  /**
   * @return state of the GUI that does not depend on the GUI technology
   */
  getGuiState(): GuiState {
    return this.guiState;
  }

  /**
   * @return the drawing area presenting the viewport set of the first display
   */
  getDrawingArea(): DrawingArea {
    return this.drawingArea;
  }

  /**
   * @return a read-only view of the viewport sets, one for each display
   */
  getViewportSets(): readonly ViewportSet[] {
    return this.viewportSets;
  }

  addViewportSet(viewportSet: ViewportSet | null): void {
    if (viewportSet !== null) {
      viewportSet.setI18nContext(this.i18nContext);
      this.viewportSets.push(viewportSet);
    }
  }

  /**
   * @return the I18N context (GUI definition in the current language) shared by
   * the viewport sets, or null if there is none yet
   */
  getI18nContext(): Widget | null {
    return this.i18nContext;
  }

  /**
   * Sets the I18N context and propagates it to every viewport set, so they are
   * presented with the messages of the language currently selected by the
   * user. It must be called each time the GUI definition is loaded.
   */
  setI18nContext(i18nContext: Widget | null): void {
    this.i18nContext = i18nContext;
    for (const viewportSet of this.viewportSets) {
      viewportSet.setI18nContext(i18nContext);
    }
  }

  getActiveViewportSetIndex(): number {
    return this.activeViewportSetIndex;
  }

  setActiveViewportSetIndex(activeViewportSetIndex: number): void {
    if (activeViewportSetIndex >= 0 && activeViewportSetIndex < this.viewportSets.length) {
      this.activeViewportSetIndex = activeViewportSetIndex;
    }
  }

  /**
   * @return the viewport set of the display currently receiving interaction
   */
  getActiveViewportSet(): ViewportSet {
    return this.viewportSets[this.activeViewportSetIndex];
  }

  getScene(): Scene {
    return this.scene!;
  }

  /**
   * Replaces the edited scene. The operations of the scene history belong to
   * the former scene, so they are forgotten.
   * @param scene the new scene
   */
  setScene(scene: Scene): void {
    this.scene = scene;
    this.rayGizmo = new RayGizmo(this.makeRayGizmoIntersectionCallback(), 2);
    this.editHistory.getSceneHistory().clear();
  }

  /**
   * @return undo/redo history of the scene and of the views of the viewports
   */
  getEditHistory(): EditHistory {
    return this.editHistory;
  }

  getCamera(): Camera {
    return this.getScene().camera;
  }

  getActiveCamera(): Camera {
    return this.getScene().activeCamera;
  }

  setActiveCamera(activeCamera: Camera): void {
    this.getScene().activeCamera = activeCamera;
  }

  getLights(): ArrayList<Light> {
    return this.getScene().scene.getLights();
  }

  /**
   * Adds a new point light to the scene, placed inside the view volume of a
   * camera of the active viewport set (see `SceneLightFactory`).
   * @return the added light, or null if no viewport is visible
   */
  addNewLight(): PointLight | null {
    const light: PointLight | null = this.lightFactory.createLight(this.getLights(), this.getActiveViewportSet());

    if (light !== null) {
      this.getLights().add(light);
    }
    return light;
  }

  getSimpleBodies(): ArrayList<SimpleBody> {
    return this.getScene().scene.getSimpleBodies();
  }

  /**
   * @return window space (OpenGL) depth of each pixel of the raytraced image
   * of a viewport in CPU render mode, or null if there is none
   */
  getRaytracedDepth(): ZBuffer | null {
    return this.raytracedDepth;
  }

  /**
   * @param raytracedDepth window space (OpenGL) depth of each pixel of the
   * raytraced image, or null
   */
  setRaytracedDepth(raytracedDepth: ZBuffer | null): void {
    this.raytracedDepth = raytracedDepth;
  }

  getRaytracedImage(): RGBImageUncompressed {
    return this.raytracedImage!;
  }

  setRaytracedImage(raytracedImage: RGBImageUncompressed): void {
    this.raytracedImage = raytracedImage;
  }

  getZbufferImage(): RGBImageUncompressed | null {
    return this.zbufferImage;
  }

  setZbufferImage(zbufferImage: RGBImageUncompressed | null): void {
    this.zbufferImage = zbufferImage;
  }

  getRaytracedImageWidth(): number {
    return this.raytracedImageWidth;
  }

  setRaytracedImageWidth(raytracedImageWidth: number): void {
    this.raytracedImageWidth = raytracedImageWidth;
  }

  getRaytracedImageHeight(): number {
    return this.raytracedImageHeight;
  }

  setRaytracedImageHeight(raytracedImageHeight: number): void {
    this.raytracedImageHeight = raytracedImageHeight;
  }

  getPalette(): RGBColorPalette | null {
    return this.palette;
  }

  setPalette(palette: RGBColorPalette | null): void {
    this.palette = palette;
  }

  isWithVisualDebugRay(): boolean {
    return this.rayGizmo !== null && this.rayGizmo.isVisible();
  }

  setWithVisualDebugRay(withVisualDebugRay: boolean): void {
    if (this.rayGizmo !== null) {
      this.rayGizmo.setVisible(withVisualDebugRay);
    }
  }

  getVisualDebugRay(): Ray | null {
    if (this.rayGizmo === null) {
      return null;
    }
    return new Ray(this.rayGizmo.getPosition(), this.rayGizmo.getDirection());
  }

  setVisualDebugRay(visualDebugRay: Ray | null): void {
    if (this.rayGizmo !== null) {
      this.rayGizmo.setRay(visualDebugRay, 0.0);
    }
  }

  getVisualDebugRayLevels(): number {
    return this.rayGizmo !== null ? this.rayGizmo.getMaxNumOfReflections() : 0;
  }

  setVisualDebugRayLevels(visualDebugRayLevels: number): void {
    if (this.rayGizmo !== null) {
      this.rayGizmo.setMaxNumOfReflections(visualDebugRayLevels);
    }
  }

  getRayGizmo(): RayGizmo | null {
    return this.rayGizmo;
  }

  private makeRayGizmoIntersectionCallback(): (ray: Ray) => Intersection | null {
    return (ray: Ray): Intersection | null => {
      if (this.scene === null) {
        return null;
      }

      let closest: Intersection | null = null;
      let closestT = Number.MAX_VALUE;

      for (const body of this.scene.scene.getSimpleBodies()) {
        const hit: RayHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        if (body.doIntersectionFirstHit(ray, hit) && hit.hasHitDistance()) {
          const t: number = hit.getHitDistance();
          if (t > VSDK.EPSILON && t < closestT) {
            closestT = t;
            closest = new Intersection(t, hit.point, hit.normal);
          }
        }
      }
      return closest;
    };
  }
}
