package render;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.InputGizmo;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.viewport.ViewportSet;

import model.DrawingArea;
import model.InteractionMode;
import model.selection.SceneSelectionEditor;

/**
Decides which manipulation gizmo of a `DrawingArea` must be presented in a
view (following the interaction mode and the selection) and places it over
the selection, seen from the camera of the view. It does not draw: renderers
of each technology draw the gizmo it prepares.
*/
public class DrawingAreaGizmoPresenter
{
    /**
    Gizmo to present in a view.
    */
    public enum GizmoKind
    {
        NONE,
        TRANSLATE,
        ROTATE,
        SCALE
    }

    private final DrawingArea drawingArea;
    private final ViewportSet viewportSet;
    private final SceneSelectionEditor selectionEditor;
    private final TranslateGizmo translationGizmo;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;
    /// Input gizmo of the gizmo prepared, or null if it has none
    private InputGizmo inputGizmo;

    /**
    @param drawingArea drawing area whose interaction mode is followed
    @param selectionEditor selection over which gizmos are placed
    @param translationGizmo gizmo to present in translation mode
    @param rotateGizmo gizmo to present in rotation mode
    @param scaleGizmo gizmo to present in scale mode
    */
    public DrawingAreaGizmoPresenter(DrawingArea drawingArea,
                                     SceneSelectionEditor selectionEditor,
                                     TranslateGizmo translationGizmo,
                                     RotateGizmo rotateGizmo,
                                     ScaleGizmo scaleGizmo)
    {
        this.drawingArea = drawingArea;
        this.viewportSet = drawingArea.getViewportSet();
        this.selectionEditor = selectionEditor;
        this.translationGizmo = translationGizmo;
        this.rotateGizmo = rotateGizmo;
        this.scaleGizmo = scaleGizmo;
        this.inputGizmo = null;
    }

    public TranslateGizmo getTranslationGizmo()
    {
        return translationGizmo;
    }

    public RotateGizmo getRotateGizmo()
    {
        return rotateGizmo;
    }

    public ScaleGizmo getScaleGizmo()
    {
        return scaleGizmo;
    }

    /**
    @return the input gizmo of the gizmo prepared by the last call to
    `prepareForView`, or null if it has none
    */
    public InputGizmo getInputGizmo()
    {
        return inputGizmo;
    }

    /**
    Selects the gizmo to present in a view and places it.
    @param camera camera of the view
    @return the gizmo to draw, already placed and scaled for the view
    */
    public GizmoKind prepareForView(Camera camera)
    {
        inputGizmo = null;

        translationGizmo.setCamera(camera);
        rotateGizmo.setCamera(camera);
        scaleGizmo.setCamera(camera);
        // Size and line width follow the resolution of the screen
        translationGizmo.applyScale(viewportSet.getElementScaler());
        rotateGizmo.applyScale(viewportSet.getElementScaler());
        scaleGizmo.applyScale(viewportSet.getElementScaler());

        SimpleBody selectedBody = selectionEditor.getFirstSelectedBody();
        InteractionMode mode = drawingArea.getInteractionMode();

        if ( drawingArea.shouldDrawTranslationGizmo() ) {
            Vector3Dd centroid = selectionEditor.computeSelectionCentroid();

            if ( centroid != null ) {
                translationGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createTranslationGizmoMatrix(centroid));
                inputGizmo = translationGizmo.getInputGizmo();
                return GizmoKind.TRANSLATE;
            }
        }
        else if ( mode == InteractionMode.ROTATE ) {
            if ( selectedBody != null ) {
                rotateGizmo.setTransformationMatrix(
                    SceneSelectionEditor.createRotationGizmoMatrix(selectedBody));
                inputGizmo = rotateGizmo.getInputGizmo();
                return GizmoKind.ROTATE;
            }
        }
        else if ( mode == InteractionMode.SCALE && selectedBody != null ) {
            scaleGizmo.setTransformationMatrix(
                SceneSelectionEditor.createRotationGizmoMatrix(selectedBody));
            scaleGizmo.setScale(selectedBody.getScale());
            inputGizmo = scaleGizmo.getInputGizmo();
            return GizmoKind.SCALE;
        }
        return GizmoKind.NONE;
    }
}
