import {
  KeyEvent,
  Matrix4x4d,
  MouseEvent,
  RayGizmoInteractionTechniques,
  ViewportInteractionTechniques,
  ViewportSetInteractionTechniques,
  type CursorWarp,
  type RendererConfiguration,
  type RotateGizmo,
  type ScaleGizmo,
  type SimpleBody,
  type TranslateGizmo,
  type Vector3Dd,
  type Viewport,
  type ViewportSet,
} from '@vitral/base';
import type { ApplicationModel } from '../model/application-model';
import type { DrawingArea } from '../model/drawing-area';
import { InteractionMode } from '../model/interaction-mode';
import type { Scene } from '../model/scene';
import { ScenePicker } from '../model/selection/scene-picker';
import { SceneSelectionEditor } from '../model/selection/scene-selection-editor';
import type { DrawingAreaInteractionListener } from './drawing-area-interaction-listener';
import { InteractionEditRecorder } from './history/interaction-edit-recorder';
import type { UndoRedoCommand } from './history/undo-redo-command';
import { UndoRedoInteractionTechnique, type UndoRedoResult } from './history/undo-redo-interaction-technique';
import { PointerCursor } from './pointer-cursor';
import { SelectedBodyMapToggles } from './selected-body-map-toggles';

/**
 * Port of `gui.DrawingAreaInteractionTechniques`.
 *
 * Mouse and keyboard interaction techniques of the drawing area of the editor:
 * camera control, selection of things, gizmo manipulation, mode selection and
 * global commands. It processes only vitral events, so callers must convert
 * events from the GUI technology in use (i.e. with `WebSystem`) before calling
 * it. Anything the user must see or the GUI technology must do (pointer shape,
 * messages, dialogs) is requested through the `DrawingAreaInteractionListener`.
 *
 * Mouse events must have coordinates in canvas pixels (see `DrawingArea`).
 *
 * Keyboard commands: `c`, `q`, `w`, `e`, `r` select the camera, selection,
 * translation, rotation and scale modes; `LEFT` / `RIGHT` select things
 * sequentially; `=` / `-` change the size of the translation gizmo (in
 * translation, rotation and scale modes with a selection `-` belongs to the
 * input gizmo); in translation, rotation and scale modes, digits, `-`, `.`,
 * `TAB` and `BACKSPACE` (and `ENTER`, `ESC` while editing) edit the numeric
 * boxes of the gizmo (see `InputGizmo`), that show coordinates, angles in
 * degrees or scale factors; in rotation mode the mouse highlights (hover) and
 * chooses (click) the rings of the gizmo (see
 * `RotateGizmoInteractionTechnique`); `DELETE`
 * removes the selected things; `F10` requests a raytraced image; `T` / `B`
 * toggle a sample texture / bump map on the selected body; `h` shows the object
 * selector; `ESC` closes the application. Commands of the visual debug ray and
 * of the viewport set are processed by their own techniques.
 *
 * Undo and redo (see `UndoRedoInteractionTechnique`): `Ctrl+Z` / `Ctrl+Y` over
 * the scene (creation, deletion and transformation of things), `Ctrl+Shift+Z`
 * / `Ctrl+Shift+Y` over the view of the selected viewport (camera placement,
 * projection, display settings). Every mouse gesture (press to release), key
 * press, click and viewport menu command is recorded in the edition history of
 * the model by an `InteractionEditRecorder`.
 *
 * The sample texture and bump map of `T` / `B` are fetched from the server
 * (see `SelectedBodyMapToggles`): the repaint is requested again when they
 * arrive.
 */
export class DrawingAreaInteractionTechniques {
  private readonly drawingArea: DrawingArea;
  private readonly model: ApplicationModel;
  private readonly scene: Scene;
  private readonly viewportSet: ViewportSet;
  private readonly selectionEditor: SceneSelectionEditor;
  private readonly scenePicker: ScenePicker;
  private readonly interactionTechniques: ViewportInteractionTechniques;
  private readonly viewportSetTechniques: ViewportSetInteractionTechniques;
  private readonly rayGizmoTechniques: RayGizmoInteractionTechniques;
  private readonly mapToggles: SelectedBodyMapToggles;
  private readonly translationGizmo: TranslateGizmo;
  private readonly rotateGizmo: RotateGizmo;
  private readonly scaleGizmo: ScaleGizmo;
  private readonly listener: DrawingAreaInteractionListener;
  private readonly editRecorder: InteractionEditRecorder;
  private readonly undoRedoTechnique: UndoRedoInteractionTechnique;

  private qualitySelection: RendererConfiguration;

  /**
   * @param model application model, providing the scene to interact with
   * @param listener who presents the requests derived from interaction
   */
  constructor(model: ApplicationModel, listener: DrawingAreaInteractionListener) {
    this.model = model;
    this.drawingArea = model.getDrawingArea();
    this.viewportSet = this.drawingArea.getViewportSet();
    this.scene = model.getScene();
    this.listener = listener;

    this.selectionEditor = new SceneSelectionEditor(this.scene);
    this.scenePicker = new ScenePicker(this.scene);
    this.qualitySelection = this.scene.qualityTemplate;
    this.interactionTechniques = new ViewportInteractionTechniques(this.scene.camera, this.qualitySelection);
    this.viewportSetTechniques = new ViewportSetInteractionTechniques(this.viewportSet);
    this.rayGizmoTechniques = new RayGizmoInteractionTechniques(model.getRayGizmo()!);
    this.mapToggles = new SelectedBodyMapToggles();
    this.translationGizmo = this.interactionTechniques.getTranslationGizmo();
    this.rotateGizmo = this.interactionTechniques.getRotateGizmo();
    this.scaleGizmo = this.interactionTechniques.getScaleGizmo();
    this.editRecorder = new InteractionEditRecorder(model.getEditHistory(), this.viewportSet);
    this.undoRedoTechnique = new UndoRedoInteractionTechnique(model.getEditHistory());
  }

  getViewportSetTechniques(): ViewportSetInteractionTechniques {
    return this.viewportSetTechniques;
  }

  getTranslationGizmo(): TranslateGizmo {
    return this.translationGizmo;
  }

  getRotateGizmo(): RotateGizmo {
    return this.rotateGizmo;
  }

  getScaleGizmo(): ScaleGizmo {
    return this.scaleGizmo;
  }

  /**
   * @param enabled true if the caller is able to place the pointer when the
   * translation gizmo technique requests it (see
   * `DrawingAreaInteractionListener.cursorWarpRequested`)
   */
  setCursorWrapEnabled(enabled: boolean): void {
    this.interactionTechniques.setTranslationCursorWrapEnabled(enabled);
  }

  /**
   * Makes the camera controllers work over the camera and configuration of
   * the given viewport. Viewport selection is processed by the viewport set
   * interaction techniques.
   * @param viewport the viewport to work over, or null for none
   */
  activateViewport(viewport: Viewport | null): void {
    if (viewport === null) {
      return;
    }

    this.interactionTechniques.setCamera(viewport.getActiveCamera());
    this.interactionTechniques.setRendererConfiguration(viewport.getRendererConfiguration());
    this.qualitySelection = viewport.getRendererConfiguration();
  }

  //= Pointer shape =====================================================

  private getModeCursor(): PointerCursor {
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (mode === InteractionMode.CAMERA) {
      return PointerCursor.CAMERA_ROTATE;
    }

    // Transformation modes show what can be done only if there is
    // something selected to transform
    if (this.selectionEditor.computeSelectionCentroid() === null) {
      return PointerCursor.SELECT;
    }
    switch (mode) {
      case InteractionMode.TRANSLATE:
        return PointerCursor.TRANSLATE;
      case InteractionMode.ROTATE:
        return PointerCursor.ROTATE;
      case InteractionMode.SCALE:
        return PointerCursor.SCALE;
      default:
        return PointerCursor.SELECT;
    }
  }

  /**
   * Requests the pointer shape for the pointer position: a normal pointer over
   * the title of a viewport (feedback that it can be clicked), the given one
   * elsewhere.
   */
  private requestCursorForPointer(event: MouseEvent, cursor: PointerCursor): void {
    let wanted: PointerCursor = cursor;

    if (this.viewportSetTechniques.isPointerOverTitle(this.drawingArea.toSurfaceEvent(event))) {
      wanted = PointerCursor.VIEWPORT_TITLE;
    }
    this.listener.cursorRequested(wanted);
  }

  /**
   * Requests the pointer shape for the current interaction mode, at the
   * position of the event.
   * @param event pointer position
   */
  updateModeCursor(event: MouseEvent): void {
    this.requestCursorForPointer(event, this.getModeCursor());
  }

  //= Mouse =============================================================

  private getViewportAtPointer(event: MouseEvent): Viewport | null {
    return this.viewportSetTechniques.findViewportAt(this.drawingArea.toSurfaceEvent(event));
  }

  /**
   * A translation gesture (drag of the gizmo) belongs to the viewport where it
   * started, even if the cursor goes over another one.
   * @return the viewport where the gesture in course started, or the one under
   * the pointer if there is no gesture
   */
  private getInteractionViewportAtPointer(event: MouseEvent): Viewport | null {
    const dragViewport: Viewport | null = this.getGestureViewport();

    if (dragViewport !== null) {
      return dragViewport;
    }
    return this.getViewportAtPointer(event);
  }

  /**
   * @return the viewport of the gizmo gesture (drag of a translation handle or
   * of a rotation ring) in course, or null if there is none
   */
  private getGestureViewport(): Viewport | null {
    const dragViewport: Viewport | null = this.interactionTechniques.getTranslationDragViewport();

    if (dragViewport !== null) {
      return dragViewport;
    }
    return this.interactionTechniques.getRotationDragViewport();
  }

  private isTranslationGestureConfined(): boolean {
    return this.getGestureViewport() !== null;
  }

  private toViewportEvent(event: MouseEvent, viewport: Viewport): MouseEvent {
    return this.viewportSetTechniques.toViewportEvent(this.drawingArea.toSurfaceEvent(event), viewport);
  }

  /**
   * Places the pointer as requested by the translation technique when the
   * cursor leaves the viewport of the gesture (infinite drag).
   * @param viewport viewport of the gesture
   */
  private wrapCursorIfRequested(viewport: Viewport | null): void {
    const warp: CursorWarp | null = this.interactionTechniques.consumeTranslationCursorWarp();

    if (warp === null || viewport === null) {
      return;
    }
    this.listener.cursorWarpRequested(
      this.viewportSet.toSetX(viewport, warp.x()),
      this.viewportSet.toSetY(viewport, warp.y()),
    );
  }

  /**
   * Places the translation gizmo over the selection centroid, seen from the
   * viewport, and lets the technique process the event. If the gizmo moves the
   * selection, it is moved with it.
   * @return false if there is no viewport to process the event
   */
  private processTranslationEvent(
    event: MouseEvent,
    viewport: Viewport | null,
    centroid: Vector3Dd,
    technique: (e: MouseEvent) => boolean,
  ): boolean {
    if (viewport === null) {
      return false;
    }
    this.translationGizmo.setCamera(viewport.getActiveCamera());
    this.translationGizmo.setTransformationMatrix(SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
    if (technique(this.toViewportEvent(event, viewport))) {
      this.selectionEditor.applyTranslationToSelectedObjects(centroid, this.translationGizmo.getPosition());
      this.listener.repaintRequested();
    }
    return true;
  }

  /**
   * Places the rotation gizmo over the first selected body, seen from the
   * viewport, and lets the technique process the event.
   * @return false if there is no viewport or selected body to process the event
   */
  private processRotationEvent(
    event: MouseEvent,
    viewport: Viewport | null,
    technique: (e: MouseEvent) => boolean,
  ): boolean {
    const body: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();

    if (viewport === null || body === null) {
      return false;
    }
    this.rotateGizmo.setCamera(viewport.getActiveCamera());
    this.rotateGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(body));
    if (technique(this.toViewportEvent(event, viewport))) {
      this.listener.repaintRequested();
    }
    return true;
  }

  /**
   * Places the scale gizmo over the first selected body (its frame: the same
   * one the rotation gizmo uses, and its current scale factors), seen from the
   * viewport, and lets the technique process the event.
   * @return false if there is no viewport or selected body to process the event
   */
  private processScaleEvent(
    event: MouseEvent,
    viewport: Viewport | null,
    technique: (e: MouseEvent) => boolean,
  ): boolean {
    const body: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();

    if (viewport === null || body === null) {
      return false;
    }
    this.scaleGizmo.setCamera(viewport.getActiveCamera());
    this.scaleGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(body));
    this.scaleGizmo.setScale(body.getScale());
    if (technique(this.toViewportEvent(event, viewport))) {
      this.listener.repaintRequested();
    }
    return true;
  }

  /**
   * Discards the numbers typed in the boxes of the gizmos, so they show the
   * real state of what the gizmos manipulate again.
   */
  private cancelInputGizmoEditing(): void {
    this.interactionTechniques.getTranslationInputGizmo().cancelEditing();
    this.interactionTechniques.getRotationInputGizmo().cancelEditing();
    this.interactionTechniques.getScaleInputGizmo().cancelEditing();
  }

  /**
   * Makes the first selected body take the orientation of the rotation gizmo.
   * @param body first selected body
   */
  private applyRotationGizmoToBody(body: SimpleBody): void {
    body.setRotation(new Matrix4x4d(this.rotateGizmo.getTransformationMatrix()).withoutTranslation());
  }

  /**
   * Makes the first selected body take the scale factors of the scale gizmo.
   * @param body first selected body
   */
  private applyScaleGizmoToBody(body: SimpleBody): void {
    body.setScale(this.scaleGizmo.getScale());
  }

  /**
   * The pointer entered the drawing area.
   */
  processMouseEnteredEvent(event: MouseEvent): void {
    // WARNING / TODO
    // There should be a cameraController.getFutureAction(e) that calculates
    // the proper icon for display ... here an Aquynza operation is
    // assumed and hard-coded
    this.updateModeCursor(event);
  }

  processMousePressedEvent(event: MouseEvent): void {
    const mouseView: Viewport | null = this.getViewportAtPointer(event);
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (mouseView === null) {
      return;
    }
    // Everything the gesture changes, up to its release, is one operation
    this.editRecorder.beginGesture(DrawingAreaInteractionTechniques.sceneOperationName(mode));
    this.viewportSetTechniques.processMousePressedEvent(this.drawingArea.toSurfaceEvent(event));
    this.activateViewport(mouseView);

    //-----------------------------------------------------------------
    // WARNING / TODO
    // There should be a cameraController.getFutureAction(e) that calculates
    // the proper icon for display ... here an Aquynza operation is
    // assumed and hard-coded
    const m: number = event.getModifiers();
    let cursor: PointerCursor;

    if (mode === InteractionMode.CAMERA && (m & MouseEvent.BUTTON1_DOWN_MASK) !== 0) {
      cursor = PointerCursor.CAMERA_ROTATE;
    } else if (mode === InteractionMode.CAMERA && (m & MouseEvent.BUTTON2_DOWN_MASK) !== 0) {
      cursor = PointerCursor.CAMERA_TRANSLATE;
    } else if (mode === InteractionMode.CAMERA && (m & MouseEvent.BUTTON3_DOWN_MASK) !== 0) {
      cursor = PointerCursor.CAMERA_ADVANCE;
    } else {
      cursor = this.getModeCursor();
    }
    this.requestCursorForPointer(event, cursor);

    //-----------------------------------------------------------------
    if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraMousePressedEvent(event)) {
      // The camera technique took the press
    } else if (mode !== InteractionMode.CAMERA) {
      const composite: boolean = (m & MouseEvent.CTRL_DOWN_MASK) !== 0;
      const viewportEvent: MouseEvent = this.toViewportEvent(event, mouseView);

      this.scene.activeCamera = mouseView.getActiveCamera();

      // A press over a gizmo handle grabs the gizmo: the ray selection
      // is skipped, so the whole selected group is kept
      let gizmoGrabbed: boolean =
        mode === InteractionMode.TRANSLATE &&
        this.interactionTechniques.getTranslationTechnique().isActive() &&
        this.selectionEditor.computeSelectionCentroid() !== null;

      if (mode === InteractionMode.ROTATE) {
        // The ring under the pointer is found again, as the press
        // could come without a previous movement
        gizmoGrabbed =
          this.processRotationEvent(event, mouseView, (e: MouseEvent): boolean =>
            this.interactionTechniques.processRotationMousePressedEvent(e, mouseView),
          ) && this.interactionTechniques.getRotationTechnique().isActive();
      } else if (mode === InteractionMode.SCALE) {
        // The handle under the pointer is found again, as the press
        // could come without a previous movement
        gizmoGrabbed =
          this.processScaleEvent(event, mouseView, (e: MouseEvent): boolean =>
            this.interactionTechniques.processScaleMousePressedEvent(e, mouseView),
          ) && this.interactionTechniques.getScaleTechnique().isActive();
      }

      if (!gizmoGrabbed) {
        // Numbers typed belong to the previous selection
        this.cancelInputGizmoEditing();
        // The picking ray aims the visual debug ray, but only the
        // IDC_TOOLS_RAY command (or the keypad) shows or hides it
        const rayVisible: boolean = this.model.isWithVisualDebugRay();
        this.rayGizmoTechniques.setRay(
          this.scenePicker.selectObjectWithMouse(viewportEvent.getX(), viewportEvent.getY(), composite),
        );
        this.model.setWithVisualDebugRay(rayVisible);
      }

      const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

      if (centroid !== null) {
        this.translationGizmo.setCamera(mouseView.getActiveCamera());
        this.translationGizmo.setTransformationMatrix(SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
        // Only the translation gizmo has mouse interaction (a gesture
        // started in other mode would never be ended)
        if (mode === InteractionMode.TRANSLATE) {
          this.interactionTechniques.processTranslationMousePressedEvent(viewportEvent, mouseView);
        }
      }

      this.reportObjectSelection();

      // The selection may have changed what the pointer can do
      this.updateModeCursor(event);
    }
    this.listener.repaintRequested();
  }

  processMouseReleasedEvent(event: MouseEvent): void {
    const mouseView: Viewport | null = this.getInteractionViewportAtPointer(event);
    const confinedGesture: boolean = this.isTranslationGestureConfined();
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (mouseView !== null && !confinedGesture) {
      this.viewportSetTechniques.processMouseReleasedEvent(this.drawingArea.toSurfaceEvent(event));
    }
    this.activateViewport(mouseView);

    // WARNING / TODO
    // There should be a cameraController.getFutureAction(e) that calculates
    // the proper icon for display ... here an Aquynza operation is
    // assumed and hard-coded

    const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

    this.updateModeCursor(event);

    if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraMouseReleasedEvent(event)) {
      this.listener.repaintRequested();
    } else if (mode === InteractionMode.TRANSLATE && centroid !== null) {
      this.processTranslationEvent(event, mouseView, centroid, (e: MouseEvent): boolean =>
        this.interactionTechniques.processTranslationMouseReleasedEvent(e),
      );
    } else if (mode === InteractionMode.ROTATE) {
      this.processRotationEvent(event, mouseView, (e: MouseEvent): boolean =>
        this.interactionTechniques.processRotationMouseReleasedEvent(e),
      );
    } else if (mode === InteractionMode.SCALE) {
      this.processScaleEvent(event, mouseView, (e: MouseEvent): boolean =>
        this.interactionTechniques.processScaleMouseReleasedEvent(e),
      );
    }
    this.editRecorder.endGesture();
  }

  processMouseClickedEvent(event: MouseEvent): void {
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    this.editRecorder.record(DrawingAreaInteractionTechniques.sceneOperationName(mode), (): void =>
      this.processMouseClickedEventWithoutRecording(event),
    );
  }

  private processMouseClickedEventWithoutRecording(event: MouseEvent): void {
    const mouseView: Viewport | null = this.getViewportAtPointer(event);
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (mouseView !== null) {
      this.viewportSetTechniques.processMouseClickedEvent(this.drawingArea.toSurfaceEvent(event));
    }
    this.activateViewport(mouseView);

    const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

    if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraMouseClickedEvent(event)) {
      this.listener.repaintRequested();
    } else if (mode === InteractionMode.TRANSLATE && centroid !== null) {
      this.processTranslationEvent(event, mouseView, centroid, (e: MouseEvent): boolean =>
        this.interactionTechniques.processTranslationMouseClickedEvent(e),
      );
    } else if (mode === InteractionMode.ROTATE) {
      this.processRotationEvent(event, mouseView, (e: MouseEvent): boolean =>
        this.interactionTechniques.processRotationMouseClickedEvent(e),
      );
    } else if (mode === InteractionMode.SCALE) {
      this.processScaleEvent(event, mouseView, (e: MouseEvent): boolean =>
        this.interactionTechniques.processScaleMouseClickedEvent(e),
      );
    }
  }

  /**
   * The pointer moved without buttons pressed.
   */
  processMouseMovedEvent(event: MouseEvent): void {
    const mouseView: Viewport | null = this.getInteractionViewportAtPointer(event);
    const confinedGesture: boolean = this.isTranslationGestureConfined();
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    // Moved events can come while dragging the gizmo (i.e. when the cursor
    // is wrapped): the viewport of the gesture keeps on being the one used
    if (mouseView !== null && !confinedGesture) {
      this.interactionTechniques.setCamera(mouseView.getActiveCamera());
    }

    //-----------------------------------------------------------------
    const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

    if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraMouseMovedEvent(event)) {
      this.listener.repaintRequested();
    } else if (mode === InteractionMode.TRANSLATE && centroid !== null) {
      this.processTranslationEvent(event, mouseView, centroid, (e: MouseEvent): boolean =>
        this.interactionTechniques.processTranslationMouseMovedEvent(e),
      );
    } else if (mode === InteractionMode.ROTATE) {
      this.processRotationEvent(event, mouseView, (e: MouseEvent): boolean =>
        this.interactionTechniques.processRotationMouseMovedEvent(e),
      );
    } else if (mode === InteractionMode.SCALE) {
      this.processScaleEvent(event, mouseView, (e: MouseEvent): boolean =>
        this.interactionTechniques.processScaleMouseMovedEvent(e),
      );
    }
  }

  processMouseDraggedEvent(event: MouseEvent): void {
    const mouseView: Viewport | null = this.getInteractionViewportAtPointer(event);
    const confinedGesture: boolean = this.isTranslationGestureConfined();
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    // While dragging the gizmo, the pointer over other viewport does not
    // select it
    if (mouseView !== null && !confinedGesture) {
      this.viewportSetTechniques.processMouseDraggedEvent(this.drawingArea.toSurfaceEvent(event));
    }
    this.activateViewport(mouseView);

    const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

    if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraMouseDraggedEvent(event)) {
      this.listener.repaintRequested();
    } else if (mode === InteractionMode.TRANSLATE && centroid !== null) {
      if (
        this.processTranslationEvent(event, mouseView, centroid, (e: MouseEvent): boolean =>
          this.interactionTechniques.processTranslationMouseDraggedEvent(e),
        )
      ) {
        this.wrapCursorIfRequested(mouseView);
      }
    } else if (mode === InteractionMode.ROTATE) {
      const body: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();

      // What the gizmo turns is oriented as the gizmo is
      this.processRotationEvent(event, mouseView, (e: MouseEvent): boolean => {
        const changed: boolean = this.interactionTechniques.processRotationMouseDraggedEvent(e);

        if (changed && body !== null) {
          this.applyRotationGizmoToBody(body);
        }
        return changed;
      });
    } else if (mode === InteractionMode.SCALE) {
      const body: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();

      this.processScaleEvent(event, mouseView, (e: MouseEvent): boolean => {
        const changed: boolean = this.interactionTechniques.processScaleMouseDraggedEvent(e);

        if (changed && body !== null) {
          this.applyScaleGizmoToBody(body);
        }
        return changed;
      });
    }
  }

  /**
   * WARNING: It is not working... check pending
   */
  processMouseWheelEvent(event: MouseEvent): void {
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    this.editRecorder.beginAction();
    try {
      if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraMouseWheelEvent(event)) {
        this.listener.repaintRequested();
      }
    } finally {
      this.editRecorder.endAction(DrawingAreaInteractionTechniques.sceneOperationName(mode), true);
    }
  }

  //= Keyboard ==========================================================

  /**
   * Processes a key press: undo/redo chords go to the history, any other key
   * is processed recording what it changes.
   * @param event key press, with the key of Ctrl chords in its keycode
   */
  processKeyPressedEvent(event: KeyEvent): void {
    const mode: InteractionMode = this.drawingArea.getInteractionMode();
    const command: UndoRedoCommand | null = UndoRedoInteractionTechnique.recognize(event);

    if (command !== null) {
      this.processUndoRedoCommand(command);
      return;
    }
    if (DrawingAreaInteractionTechniques.isControlLetterChord(event)) {
      // Other Ctrl+letter chords are not commands of the drawing area:
      // the letters alone are, so they must not be taken as them
      return;
    }

    this.editRecorder.beginAction();
    try {
      this.processEditionKeyPressedEvent(event);
    } finally {
      this.editRecorder.endAction(DrawingAreaInteractionTechniques.keyOperationName(mode, event), true);
    }
  }

  private processEditionKeyPressedEvent(event: KeyEvent): void {
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (this.rayGizmoTechniques.processKeyPressedEvent(event)) {
      this.listener.repaintRequested();
      return;
    }

    if (
      (mode === InteractionMode.TRANSLATE || mode === InteractionMode.ROTATE || mode === InteractionMode.SCALE) &&
      this.processInputGizmoKeyPressedEvent(mode, event)
    ) {
      this.listener.repaintRequested();
      return;
    }

    if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraKeyPressedEvent(event)) {
      // The camera technique took the key
    } else {
      this.processModeKeyPressedEvent(mode, event);
    }

    this.processGlobalKeyPressedEvent(event);

    // Viewport set commands (selection, layout, per-viewport display)
    this.viewportSetTechniques.processKeyPressedEvent(event);

    this.processCharacterKeyPressedEvent(event);

    this.listener.cursorRequested(this.getModeCursor());
    this.listener.repaintRequested();
  }

  /**
   * Lets the input gizmo of the gizmo of the interaction mode use a key.
   * @param mode translation, rotation or scale mode
   * @param event key press
   * @return true if the input gizmo used the key, so it must not be processed as any
   * other command
   */
  private processInputGizmoKeyPressedEvent(mode: InteractionMode, event: KeyEvent): boolean {
    if (mode === InteractionMode.ROTATE) {
      return this.processRotationInputGizmoKeyPressedEvent(event);
    }
    if (mode === InteractionMode.SCALE) {
      return this.processScaleInputGizmoKeyPressedEvent(event);
    }
    return this.processTranslationInputGizmoKeyPressedEvent(event);
  }

  /**
   * Lets the input gizmo of the rotation gizmo use a key. If the user accepts
   * what was typed, the first selected body takes the orientation the angles
   * say.
   * @return true if the input gizmo used the key
   */
  private processRotationInputGizmoKeyPressedEvent(event: KeyEvent): boolean {
    const body: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();

    if (body === null) {
      return false;
    }
    this.rotateGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(body));
    if (!this.interactionTechniques.isRotationInputGizmoKey(event)) {
      return false;
    }
    if (this.interactionTechniques.processRotateKeyPressedEvent(event)) {
      this.applyRotationGizmoToBody(body);
    }
    return true;
  }

  /**
   * Lets the input gizmo of the scale gizmo use a key. If the user accepts
   * what was typed, the first selected body takes the scale factors the
   * numbers say.
   * @return true if the input gizmo used the key
   */
  private processScaleInputGizmoKeyPressedEvent(event: KeyEvent): boolean {
    const body: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();

    if (body === null) {
      return false;
    }
    this.scaleGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(body));
    this.scaleGizmo.setScale(body.getScale());
    if (!this.interactionTechniques.isScaleInputGizmoKey(event)) {
      return false;
    }
    if (this.interactionTechniques.processScaleKeyPressedEvent(event)) {
      body.setScale(this.scaleGizmo.getScale());
    }
    return true;
  }

  /**
   * Lets the input gizmo of the translation gizmo use a key. If the user
   * accepts what was typed, the selection is moved so the gizmo is exactly
   * where the numbers say.
   * @return true if the input gizmo used the key
   */
  private processTranslationInputGizmoKeyPressedEvent(event: KeyEvent): boolean {
    const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

    if (centroid === null) {
      return false;
    }
    this.translationGizmo.setTransformationMatrix(SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
    if (!this.interactionTechniques.isTranslationInputGizmoKey(event)) {
      return false;
    }
    if (this.interactionTechniques.processTranslationKeyPressedEvent(event)) {
      this.selectionEditor.applyTranslationToSelectedObjects(centroid, this.translationGizmo.getPosition());
    }
    return true;
  }

  /**
   * Keys with a meaning that depends on the interaction mode.
   */
  private processModeKeyPressedEvent(mode: InteractionMode, event: KeyEvent): void {
    let body: SimpleBody | null;

    switch (mode) {
      case InteractionMode.SELECT:
        if (event.unicodeId === KeyEvent.KEY_NONE) {
          if (event.keycode === KeyEvent.KEY_LEFT) {
            this.cancelInputGizmoEditing();
            this.selectionEditor.selectPrevious();
            this.reportObjectSelection();
          } else if (event.keycode === KeyEvent.KEY_RIGHT) {
            this.cancelInputGizmoEditing();
            this.selectionEditor.selectNext();
            this.reportObjectSelection();
          }
        }
        break;
      case InteractionMode.TRANSLATE: {
        const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

        if (centroid !== null) {
          this.translationGizmo.setTransformationMatrix(SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
          if (this.interactionTechniques.processTranslationKeyPressedEvent(event)) {
            this.selectionEditor.applyTranslationToSelectedObjects(centroid, this.translationGizmo.getPosition());
          }
        }
        break;
      }
      case InteractionMode.ROTATE:
        body = this.selectionEditor.getFirstSelectedBody();
        if (body !== null) {
          this.rotateGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(body));
          if (this.interactionTechniques.processRotateKeyPressedEvent(event)) {
            this.applyRotationGizmoToBody(body);
          }
        }
        break;
      case InteractionMode.SCALE:
        body = this.selectionEditor.getFirstSelectedBody();
        if (body !== null) {
          this.scaleGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(body));
          this.scaleGizmo.setScale(body.getScale());
          if (this.interactionTechniques.processScaleKeyPressedEvent(event)) {
            body.setScale(this.scaleGizmo.getScale());
          }
        }
        break;
      default:
        break;
    }
  }

  /**
   * Keys with the same meaning in every interaction mode.
   */
  private processGlobalKeyPressedEvent(event: KeyEvent): void {
    let apparentSize: number;

    switch (event.keycode) {
      case KeyEvent.KEY_ESC:
        this.listener.closeRequested();
        break;
      case KeyEvent.KEY_EQUALS:
        apparentSize = this.translationGizmo.getBaseApparentSizeInPixels();
        apparentSize += 10;
        if (apparentSize > 300) apparentSize = 300;
        this.translationGizmo.setBaseApparentSizeInPixels(apparentSize);
        break;
      case KeyEvent.KEY_MINUS:
        apparentSize = this.translationGizmo.getBaseApparentSizeInPixels();
        apparentSize -= 20;
        if (apparentSize < 20) apparentSize = 20;
        this.translationGizmo.setBaseApparentSizeInPixels(apparentSize);
        break;
      case KeyEvent.KEY_DELETE:
        this.cancelInputGizmoEditing();
        this.selectionEditor.deleteSelected();
        break;
      case KeyEvent.KEY_F10:
        this.listener.raytracingRequested();
        break;
      default:
        break;
    }

    if (this.interactionTechniques.processQualityKeyPressedEvent(event)) {
      console.log('' + this.qualitySelection);
    }
  }

  /**
   * Keys with a character (letters, digits and symbols).
   */
  private processCharacterKeyPressedEvent(event: KeyEvent): void {
    if (event.unicodeId === KeyEvent.KEY_NONE) {
      return;
    }

    let body: SimpleBody | null;

    switch (event.unicodeId) {
      case KeyEvent.charCode('T'):
        body = this.selectionEditor.getFirstSelectedBody();
        if (body !== null) {
          void this.mapToggles.toggleTexture(body).then((): void => this.listener.repaintRequested());
        }
        break;
      case KeyEvent.charCode('B'):
        body = this.selectionEditor.getFirstSelectedBody();
        if (body !== null) {
          void this.mapToggles.toggleNormalMap(body).then((): void => this.listener.repaintRequested());
        }
        break;
      case KeyEvent.charCode('h'):
        this.listener.selectorDialogRequested();
        this.printBodyNames();
        break;
      case KeyEvent.charCode('c'):
        this.switchMode(
          InteractionMode.CAMERA,
          'Camera mode interaction - drag mouse with different buttons over the scene to change current camera.',
        );
        break;
      case KeyEvent.charCode('q'):
        this.switchMode(
          InteractionMode.SELECT,
          'Selection mode interaction - click mouse to select objects, LEFT/RIGHT arrow keys to select sequencialy.',
        );
        break;
      case KeyEvent.charCode('w'):
        // Alt+w (maximize viewport) is a viewport set command
        if ((event.modifierMask & KeyEvent.MASK_ALT) === 0) {
          this.switchMode(
            InteractionMode.TRANSLATE,
            'Translation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to move it.',
          );
        }
        break;
      case KeyEvent.charCode('e'):
        this.switchMode(
          InteractionMode.ROTATE,
          'Rotation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to rotate it.',
        );
        break;
      case KeyEvent.charCode('r'):
        this.switchMode(
          InteractionMode.SCALE,
          'Scale mode interaction - click mouse to select objects, X, Y, Z/ARROWS keys and gizmo to scale it.',
        );
        break;
      default:
        break;
    }
  }

  private switchMode(mode: InteractionMode, message: string): void {
    this.listener.statusMessageRequested(message);
    this.cancelInputGizmoEditing();
    this.drawingArea.switchInteractionMode(mode);
  }

  private printBodyNames(): void {
    const bodies = this.scene.scene.getSimpleBodies();

    for (let i = 0; i < bodies.size(); i++) {
      console.log('Consultando cosa ' + i + ':');
      let name: string | null = bodies.get(i).getName();
      if (name === null || name === '') {
        name = 'Not named object';
      }
      console.log('Object: ' + name);
    }
  }

  /**
   * @param event key released event
   */
  processKeyReleasedEvent(event: KeyEvent): void {
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (
      UndoRedoInteractionTechnique.recognize(event) !== null ||
      DrawingAreaInteractionTechniques.isControlLetterChord(event)
    ) {
      return;
    }
    this.editRecorder.beginAction();
    try {
      if (mode === InteractionMode.CAMERA && this.interactionTechniques.processCameraKeyReleasedEvent(event)) {
        this.listener.repaintRequested();
      }
    } finally {
      this.editRecorder.endAction(DrawingAreaInteractionTechniques.sceneOperationName(mode), true);
    }
  }

  //= Viewport commands =================================================

  /**
   * Processes one of the standard commands of the viewport set (projection
   * location or render mode, i.e. chosen in the menu of a viewport) over a
   * viewport, recording the change of its view.
   * @param command the id of the command, starting with `IDV_`
   * @param viewport viewport to change
   * @return true if the command was a viewport set one and was processed
   */
  processViewportCommand(command: string, viewport: Viewport): boolean {
    let processed: boolean;

    this.editRecorder.beginAction();
    try {
      processed = this.viewportSetTechniques.processCommand(command, viewport);
    } finally {
      this.editRecorder.endAction(
        DrawingAreaInteractionTechniques.sceneOperationName(this.drawingArea.getInteractionMode()),
        false,
      );
    }
    if (processed && viewport === this.viewportSet.getSelectedViewport()) {
      this.activateViewport(viewport);
    }
    this.listener.repaintRequested();
    return processed;
  }

  //= Undo / redo =======================================================

  /**
   * Undoes or redoes an operation of the scene history or of the view history
   * of the selected viewport. A gesture whose release was lost is finished
   * (recorded) first, so it is what gets undone.
   * @param command the command to execute
   */
  processUndoRedoCommand(command: UndoRedoCommand): void {
    const selectedViewport: Viewport | null = this.viewportSet.getSelectedViewport();

    this.editRecorder.endGesture();
    // Numbers typed in the gizmo boxes belong to the state before
    this.cancelInputGizmoEditing();
    const result: UndoRedoResult = this.undoRedoTechnique.execute(command, selectedViewport);

    if (result.isDone()) {
      if (command.isViewportCommand()) {
        // The viewport may be showing other camera now
        this.activateViewport(selectedViewport);
      } else {
        this.listener.selectionChanged();
      }
    }
    this.listener.statusMessageRequested(result.message());
    this.listener.cursorRequested(this.getModeCursor());
    this.listener.repaintRequested();
  }

  /**
   * @return true if the key press is a Ctrl chord with a letter
   */
  private static isControlLetterChord(event: KeyEvent): boolean {
    return (
      (event.modifierMask & KeyEvent.MASK_CTRL) !== 0 &&
      event.keycode >= KeyEvent.KEY_A &&
      event.keycode <= KeyEvent.KEY_z
    );
  }

  /**
   * @return name of the operation over the scene done in a mode
   */
  private static sceneOperationName(mode: InteractionMode): string {
    switch (mode) {
      case InteractionMode.TRANSLATE:
        return 'Translation';
      case InteractionMode.ROTATE:
        return 'Rotation';
      case InteractionMode.SCALE:
        return 'Scale';
      default:
        return 'Scene edition';
    }
  }

  /**
   * @return name of the operation over the scene done by a key in a mode
   */
  private static keyOperationName(mode: InteractionMode, event: KeyEvent): string {
    if (event.keycode === KeyEvent.KEY_DELETE) {
      return 'Deletion';
    }
    return DrawingAreaInteractionTechniques.sceneOperationName(mode);
  }

  //= Selection feedback ================================================

  private reportObjectSelection(): void {
    this.listener.statusMessageRequested(this.selectionEditor.describeSelection());
    this.listener.selectionChanged();
  }
}
