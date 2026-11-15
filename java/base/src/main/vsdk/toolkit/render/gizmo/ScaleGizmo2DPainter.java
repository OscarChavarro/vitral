package vsdk.toolkit.render.gizmo;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.GizmoSolidTessellator;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;

/**
Presents a {@link ScaleGizmo} as 2D primitives (see `Gizmo2DDrawList`),
with the same elements `Jogl4ScaleGizmoRenderer` draws: the shafts of the
axes and the cubes at their tips (solid), the contour of the flat handles
(lines) and the translucent interior of the handle selected.
*/
public class ScaleGizmo2DPainter
{
    /// Opacity of the interior of the flat handle currently selected
    private static final double HANDLE_OPACITY = 0.55;
    /// Depth bias (towards the viewer) of the contour of the flat handles
    private static final double CONTOUR_DEPTH_BIAS = -0.0002;

    private ScaleGizmo2DPainter()
    {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera, transformation matrix and scale set
    @param camera camera of the viewport, with its viewport size updated
    */
    public static void paint(Gizmo2DDrawList out, ScaleGizmo gizmo, Camera camera)
    {
        if ( out == null || gizmo == null || camera == null ) {
            return;
        }
        Gizmo2DProjector projector = new Gizmo2DProjector(camera, out);

        for ( SimpleBody element : gizmo.getElements() ) {
            Geometry g = element.getGeometry();
            Matrix4x4d local = GizmoSolidTessellator.localTransform(element);

            if ( g instanceof Cone ) {
                Cone cone = (Cone)g;
                projector.addStrip(GizmoSolidTessellator.buildShaftStrip(local,
                    cone.getBottomRadius(), cone.getTopRadius(), cone.getHeight()),
                    element.getMaterial().getDiffuse(), 1.0);
            }
            else if ( g instanceof Box ) {
                for ( Vector3Dd[] face : GizmoSolidTessellator.buildBoxFaceStrips(
                          local, ((Box)g).getSize()) ) {
                    projector.addStrip(face, element.getMaterial().getDiffuse(), 1.0);
                }
            }
        }

        for ( ScaleGizmo.ContourSegment segment : gizmo.buildContourSegments() ) {
            projector.addSegment(segment.start(), segment.end(), segment.color(),
                gizmo.getLineWidth(), CONTOUR_DEPTH_BIAS);
        }

        for ( int group : ScaleGizmo.BAND_GROUPS ) {
            if ( gizmo.isBandHighlighted(group) ) {
                projector.addStrip(gizmo.buildBandQuad(group),
                    ScaleGizmo.HANDLE_FILL_COLOR, HANDLE_OPACITY);
            }
        }
        if ( gizmo.isUniformHighlighted() ) {
            projector.addTriangles(gizmo.buildUniformTriangles(),
                ScaleGizmo.HANDLE_FILL_COLOR, HANDLE_OPACITY);
        }
    }
}
