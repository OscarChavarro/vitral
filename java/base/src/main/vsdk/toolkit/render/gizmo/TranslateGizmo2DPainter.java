package vsdk.toolkit.render.gizmo;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.GizmoSolidTessellator;
import vsdk.toolkit.gui.gizmo.ReferenceFrameGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmoLineSegment;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
Presents a {@link TranslateGizmo} as 2D primitives (see `Gizmo2DDrawList`),
with the same elements `Jogl4TranslateGizmoRenderer` draws: the lines of the
axes and plane handles (as quads facing the camera), the cones of the axis
heads (with a darker base) and the translucent quad of the selected plane
handle; plus the labels of the axes `Jogl4ViewportWindow` draws next to their
tips (yellow when the axis is highlighted).
*/
public class TranslateGizmo2DPainter
{
    private static final double CONE_BASE_SHADE = 0.5;
    private static final ColorRgb HIGHLIGHT_COLOR = new ColorRgb(1, 1, 0);
    // Sizes, in pixels, designed for legacy resolutions
    private static final int BASE_LABEL_FONT_SIZE = 14;
    private static final int BASE_LABEL_OFFSET_X = -3;
    private static final int BASE_LABEL_OFFSET_Y = 12;

    private static final ReferenceFrameGizmo AXES = new ReferenceFrameGizmo();

    private TranslateGizmo2DPainter()
    {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera and transformation matrix set
    @param camera camera of the viewport, with its viewport size updated
    @param scaler sizes of the elements for the resolution of the screen
    */
    public static void paint(Gizmo2DDrawList out, TranslateGizmo gizmo,
                             Camera camera, ViewportElementScaler scaler)
    {
        if ( out == null || gizmo == null || camera == null ) {
            return;
        }
        Gizmo2DProjector projector = new Gizmo2DProjector(camera, out);
        ArrayList<SimpleBody> elements = gizmo.getElements3dsmax();

        //- Opaque elements -----------------------------------------------
        for ( TranslateGizmoLineSegment segment : gizmo.getLineSegments() ) {
            Vector3Dd[] strip = gizmo.buildLineStrip(segment);

            if ( strip != null ) {
                projector.addStrip(strip, segment.color(), 1.0);
            }
        }
        for ( SimpleBody element : elements ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Cone ) {
                addCone(projector, element, (Cone)g);
            }
        }

        //- Translucent elements ------------------------------------------
        for ( SimpleBody element : elements ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Box ) {
                Box box = (Box)g;
                Vector3Dd[] quad = GizmoSolidTessellator.buildPlaneQuad(
                    GizmoSolidTessellator.localTransform(element),
                    box.getSize().x(), box.getSize().y());

                projector.addStrip(quad, element.getMaterial().getDiffuse(),
                    element.getMaterial().getOpacity());
            }
        }

        addLabels(projector, gizmo, scaler);
    }

    private static void addCone(Gizmo2DProjector projector, SimpleBody element,
                                Cone cone)
    {
        double radius = cone.getBottomRadius();
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = GizmoSolidTessellator.localTransform(element);
        ColorRgb dark = new ColorRgb(c.r()*CONE_BASE_SHADE, c.g()*CONE_BASE_SHADE,
            c.b()*CONE_BASE_SHADE);
        Vector3Dd[] baseFan = GizmoSolidTessellator.buildConeBaseFan(local, radius);
        Vector3Dd[] perimeter = new Vector3Dd[baseFan.length - 1];

        System.arraycopy(baseFan, 1, perimeter, 0, perimeter.length);
        projector.addPolygon(perimeter, dark, 1.0);
        projector.addFanTriangles(GizmoSolidTessellator.buildConeSideFan(
            local, radius, cone.getHeight()), c, 1.0);
    }

    /**
    Labels of the axes, a little beyond the tip of each one.
    */
    private static void addLabels(Gizmo2DProjector projector,
                                  TranslateGizmo gizmo,
                                  ViewportElementScaler scaler)
    {
        int fontSize = scaler.scaleSize(BASE_LABEL_FONT_SIZE);
        double offsetX = Math.round(scaler.scaleLength(BASE_LABEL_OFFSET_X));
        double offsetY = Math.round(scaler.scaleLength(BASE_LABEL_OFFSET_Y));
        ArrayList<SimpleBody> things = gizmo.getElements();

        for ( int i = 0; i < things.size() && i < ReferenceFrameGizmo.NUMBER_OF_AXES; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();

            if ( g == null ) {
                continue;
            }
            double tip = 1;
            if ( g instanceof Arrow ) {
                Arrow a = (Arrow)g;
                tip = (a.getHeadLength() + a.getBaseLength()) * 1.1;
            }
            Matrix4x4d R = new Matrix4x4d().translation(r.getPosition())
                .multiply(r.getRotation());
            double[] p = projector.projectPoint(R.multiply(new Vector3Dd(0, 0, tip)));

            if ( p != null ) {
                boolean highlighted = ColorRgb.distance(HIGHLIGHT_COLOR,
                    r.getMaterial().getDiffuse()) < VSDK.EPSILON;

                projector.getDrawList().addOverlayLabel(
                    (int)p[0] + offsetX, (int)p[1] + offsetY, AXES.getAxisLabel(i),
                    highlighted ? HIGHLIGHT_COLOR : AXES.getAxisColor(i),
                    fontSize, Gizmo2DDrawList.Alignment.LOWER_LEFT);
            }
        }
    }
}
