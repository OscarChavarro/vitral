import type {
  Camera,
  InputGizmo,
  RotateGizmo,
  ScaleGizmo,
  SimpleBody,
  TranslateGizmo,
  Vector3Dd,
  ViewportSet,
} from '@vitral/base';
import type { DrawingArea } from '../model/drawing-area';
import { InteractionMode } from '../model/interaction-mode';
import { SceneSelectionEditor } from '../model/selection/scene-selection-editor';

/**
 * Gizmo to present in a view (Java's enum `DrawingAreaGizmoPresenter.GizmoKind`).
 */
export enum GizmoKind {
  NONE = 'NONE',
  TRANSLATE = 'TRANSLATE',
  ROTATE = 'ROTATE',
  SCALE = 'SCALE',
}

/**
 * Port of `render.DrawingAreaGizmoPresenter`.
 *
 * Decides which manipulation gizmo of a `DrawingArea` must be presented in a
 * view (following the interaction mode and the selection) and places it over
 * the selection, seen from the camera of the view. It does not draw: renderers
 * of each technology draw the gizmo it prepares.
 */
export class DrawingAreaGizmoPresenter {
  private readonly drawingArea: DrawingArea;
  private readonly viewportSet: ViewportSet;
  private readonly selectionEditor: SceneSelectionEditor;
  private readonly translationGizmo: TranslateGizmo;
  private readonly rotateGizmo: RotateGizmo;
  private readonly scaleGizmo: ScaleGizmo;
  /// Input gizmo of the gizmo prepared, or null if it has none
  private inputGizmo: InputGizmo | null;

  /**
   * @param drawingArea drawing area whose interaction mode is followed
   * @param selectionEditor selection over which gizmos are placed
   * @param translationGizmo gizmo to present in translation mode
   * @param rotateGizmo gizmo to present in rotation mode
   * @param scaleGizmo gizmo to present in scale mode
   */
  constructor(
    drawingArea: DrawingArea,
    selectionEditor: SceneSelectionEditor,
    translationGizmo: TranslateGizmo,
    rotateGizmo: RotateGizmo,
    scaleGizmo: ScaleGizmo,
  ) {
    this.drawingArea = drawingArea;
    this.viewportSet = drawingArea.getViewportSet();
    this.selectionEditor = selectionEditor;
    this.translationGizmo = translationGizmo;
    this.rotateGizmo = rotateGizmo;
    this.scaleGizmo = scaleGizmo;
    this.inputGizmo = null;
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
   * @return the input gizmo of the gizmo prepared by the last call to
   * `prepareForView`, or null if it has none
   */
  getInputGizmo(): InputGizmo | null {
    return this.inputGizmo;
  }

  /**
   * Selects the gizmo to present in a view and places it.
   * @param camera camera of the view
   * @return the gizmo to draw, already placed and scaled for the view
   */
  prepareForView(camera: Camera): GizmoKind {
    this.inputGizmo = null;

    this.translationGizmo.setCamera(camera);
    this.rotateGizmo.setCamera(camera);
    this.scaleGizmo.setCamera(camera);
    // Size and line width follow the resolution of the screen
    this.translationGizmo.applyScale(this.viewportSet.getElementScaler());
    this.rotateGizmo.applyScale(this.viewportSet.getElementScaler());
    this.scaleGizmo.applyScale(this.viewportSet.getElementScaler());

    const selectedBody: SimpleBody | null = this.selectionEditor.getFirstSelectedBody();
    const mode: InteractionMode = this.drawingArea.getInteractionMode();

    if (this.drawingArea.shouldDrawTranslationGizmo()) {
      const centroid: Vector3Dd | null = this.selectionEditor.computeSelectionCentroid();

      if (centroid !== null) {
        this.translationGizmo.setTransformationMatrix(SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
        this.inputGizmo = this.translationGizmo.getInputGizmo();
        return GizmoKind.TRANSLATE;
      }
    } else if (mode === InteractionMode.ROTATE) {
      if (selectedBody !== null) {
        this.rotateGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(selectedBody));
        this.inputGizmo = this.rotateGizmo.getInputGizmo();
        return GizmoKind.ROTATE;
      }
    } else if (mode === InteractionMode.SCALE && selectedBody !== null) {
      this.scaleGizmo.setTransformationMatrix(SceneSelectionEditor.createRotationGizmoMatrix(selectedBody));
      this.scaleGizmo.setScale(selectedBody.getScale());
      this.inputGizmo = this.scaleGizmo.getInputGizmo();
      return GizmoKind.SCALE;
    }
    return GizmoKind.NONE;
  }
}
