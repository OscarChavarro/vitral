//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,          =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package vsdk.toolkit.render.gizmo;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.CameraControlGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

/**
Presents a `CameraControlGizmo` (a ViewCube) as 2D primitives of a
`Gizmo2DDrawList`, for drawing surfaces without a 3D API, in the HUD area of
the gizmo:
  - The edges of its 27 parts (26 boxes and the compass tube), as boundary
    representations, with the hidden lines removed by the algorithm of Appel
    [APPE1967] (`HiddenLineRenderer.executeAppelAlgorithm`), seen from the
    HUD camera of the gizmo. Only the visible lines are presented (contours
    thicker), as overlay lines, so they are exact.
  - Under them, the faces of the parts that face the viewer, filled with the
    color of each part (the highlight of the region under the cursor, and
    the translucency of an inactive gizmo), sorted with the painter's
    algorithm [FOLE1992].15.5.
  - Over everything, the labels of the faces of the cube that face the
    viewer.

The boundary representations are exported once, and the lines are kept
while the orientation and size of the gizmo do not change, as the hidden
line removal is the expensive part.

Use one painter (and one draw list) for each gizmo, and draw that list after
the one of the other gizmos, as the HUD is over everything.
*/
public class CameraControlGizmo2DPainter
{
    private static final int COMPASS_SLICES = CameraControlGizmo.COMPASS_SLICES;
    private static final int BASE_LABEL_FONT_SIZE = 9;
    private static final double MIN_LABEL_VISIBILITY = 0.3;

    private CameraControlGizmo cachedGizmo;
    /// Bodies with the boundary representations of the parts of the gizmo
    private final List<SimpleBody> brepParts;
    private Matrix4x4d cachedRotation;
    private int cachedSize;
    private Calligraphic2DBuffer contourLines;
    private Calligraphic2DBuffer visibleLines;

    public CameraControlGizmo2DPainter()
    {
        cachedGizmo = null;
        brepParts = new ArrayList<SimpleBody>();
        cachedRotation = null;
        cachedSize = -1;
        contourLines = new Calligraphic2DBuffer();
        visibleLines = new Calligraphic2DBuffer();
    }

    /**
    Adds the primitives that present the gizmo, in pixels of its viewport.
    @param list list receiving the primitives
    @param gizmo gizmo to present, already placed (see
    `CameraControlGizmo.placeInViewport`) and with its HUD camera updated
    @param scaler scaler of the viewport set (sizes of lines and labels), or
    null for base sizes
    */
    public void paint(Gizmo2DDrawList list, CameraControlGizmo gizmo,
                      ViewportElementScaler scaler)
    {
        if ( !gizmo.isVisible() ) {
            return;
        }
        prepareBreps(gizmo);
        updateHiddenLines(gizmo);

        Camera hud = gizmo.getHudCamera();
        Matrix4x4d projection = hud.calculateProjectionMatrix();
        Vector3Dd viewDirection = hud.getFront();
        double opacity = gizmo.getOpacity();

        //- Faces, depth sorted ------------------------------------------
        for ( int r = 0; r < CameraControlGizmo.NUMBER_OF_PARTS; r++ ) {
            ColorRgb color = gizmo.getPartColor(r);
            if ( r == CameraControlGizmo.COMPASS_REGION ) {
                addCompassFaces(list, gizmo, projection, viewDirection, color,
                    opacity);
            }
            else {
                addBrepFaces(list, gizmo, brepParts.get(r), projection,
                    viewDirection, color, opacity);
            }
        }

        //- Visible lines from the hidden line removal --------------------
        double lineWidth = scaler != null ? scaler.scaleLength(1.0) : 1.0;
        ColorRgb lineColor = gizmo.getLineColor();
        addLines(list, gizmo, visibleLines, lineColor, lineWidth);
        addLines(list, gizmo, contourLines, lineColor, 1.5 * lineWidth);

        //- Labels --------------------------------------------------------
        int fontSize = scaler != null ? scaler.scaleSize(BASE_LABEL_FONT_SIZE) :
            BASE_LABEL_FONT_SIZE;
        for ( int face : gizmo.getFaceRegions() ) {
            if ( gizmo.faceVisibility(face) < MIN_LABEL_VISIBILITY ) {
                continue;
            }
            double[] p = gizmo.projectToViewport(gizmo.getFaceLabelPosition(face));
            list.addOverlayLabel(p[0], p[1], gizmo.getFaceLabel(face),
                gizmo.getLabelColor(), fontSize, Gizmo2DDrawList.Alignment.CENTER);
        }
    }

    //= Caches ============================================================

    private void prepareBreps(CameraControlGizmo gizmo)
    {
        if ( cachedGizmo == gizmo ) {
            return;
        }
        cachedGizmo = gizmo;
        cachedRotation = null;
        brepParts.clear();
        for ( int p = 0; p < CameraControlGizmo.NUMBER_OF_PARTS; p++ ) {
            SimpleBody part = gizmo.getParts().get(p);
            PolyhedralBoundedSolid brep = gizmo.exportPartToPolyhedralBoundedSolid(p);
            SimpleBody body = new SimpleBody();
            body.setGeometry(brep);
            body.setPosition(part.getPosition());
            body.setRotation(new Matrix4x4d());
            body.setRotationInverse(new Matrix4x4d());
            body.setScale(new Vector3Dd(1, 1, 1));
            body.setMaterial(part.getMaterial());
            brepParts.add(body);
        }
    }

    /**
    [APPE1967] over the parts, only when the orientation or size changed.
    */
    private void updateHiddenLines(CameraControlGizmo gizmo)
    {
        Camera hud = gizmo.getHudCamera();
        Matrix4x4d rotation = hud.getRotation();

        if ( cachedRotation != null && cachedSize == gizmo.getSizeInPixels() &&
             cachedRotation.epsilonEquals(rotation, 1e-9) ) {
            return;
        }
        cachedRotation = rotation;
        cachedSize = gizmo.getSizeInPixels();
        contourLines = new Calligraphic2DBuffer();
        visibleLines = new Calligraphic2DBuffer();
        Calligraphic2DBuffer hiddenLines = new Calligraphic2DBuffer();

        HiddenLineRenderer.executeAppelAlgorithm(brepParts, hud, contourLines,
            visibleLines, hiddenLines);
    }

    //= Primitives ========================================================

    private static void addLines(Gizmo2DDrawList list, CameraControlGizmo gizmo,
        Calligraphic2DBuffer lines, ColorRgb color, double width)
    {
        double size = gizmo.getSizeInPixels();

        for ( int i = 0; i < lines.getNumLines(); i++ ) {
            Vector3Dd[] line = lines.get2DLine(i);
            list.addOverlayLine(
                gizmo.getHudX() + (line[0].x() + 1.0) / 2.0 * size,
                gizmo.getHudY() + (1.0 - line[0].y()) / 2.0 * size,
                gizmo.getHudX() + (line[1].x() + 1.0) / 2.0 * size,
                gizmo.getHudY() + (1.0 - line[1].y()) / 2.0 * size,
                color, width);
        }
    }

    /**
    A little of shading, so the sides of the cube seen at once differ: the
    color is dimmed by how much the face turns away from the viewer.
    */
    private static double lambert(Vector3Dd normal, Vector3Dd viewDirection)
    {
        double c = -normal.normalized().dotProduct(viewDirection.normalized());
        return 0.65 + 0.35 * Math.max(0.0, c);
    }

    /**
    Adds the polygon of a face if it faces the viewer.
    */
    private static void addPolygon(Gizmo2DDrawList list, CameraControlGizmo gizmo,
        Matrix4x4d projection, Vector3Dd viewDirection, Vector3Dd[] vertices,
        Vector3Dd normal, ColorRgb color, double opacity)
    {
        if ( normal.dotProduct(viewDirection) >= 0.0 ) {
            return;
        }
        int n = vertices.length;
        double[] x = new double[n];
        double[] y = new double[n];
        double depth = 0;
        double size = gizmo.getSizeInPixels();

        for ( int i = 0; i < n; i++ ) {
            Vector4Dd ndc = projection.multiply(new Vector4Dd(vertices[i])).dividedByW();
            x[i] = gizmo.getHudX() + (ndc.x() + 1.0) / 2.0 * size;
            y[i] = gizmo.getHudY() + (1.0 - ndc.y()) / 2.0 * size;
            depth += ndc.z();
        }
        list.addPolygon(x, y, depth / n, color.multiply(lambert(normal, viewDirection)),
            opacity);
    }

    private static void addBrepFaces(Gizmo2DDrawList list, CameraControlGizmo gizmo,
        SimpleBody body, Matrix4x4d projection, Vector3Dd viewDirection,
        ColorRgb color, double opacity)
    {
        PolyhedralBoundedSolid solid = (PolyhedralBoundedSolid)body.getGeometry();
        Vector3Dd offset = body.getPosition();

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( face.boundariesList.size() != 1 ) {
                continue;
            }
            ArrayList<Vector3Dd> points = new ArrayList<Vector3Dd>();
            _PolyhedralBoundedSolidHalfEdge start =
                face.boundariesList.get(0).boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            do {
                points.add(he.startingVertex.position.add(offset));
                he = he.next();
            } while ( he != start );
            addPolygon(list, gizmo, projection, viewDirection,
                points.toArray(new Vector3Dd[0]),
                face.getContainingPlane().getNormal(), color, opacity);
        }
    }

    /**
    The compass is filled with quads computed from its parameters, as its
    caps (annuli) are not simple polygons.
    */
    private static void addCompassFaces(Gizmo2DDrawList list,
        CameraControlGizmo gizmo, Matrix4x4d projection, Vector3Dd viewDirection,
        ColorRgb color, double opacity)
    {
        SimpleBody part = gizmo.getParts().get(CameraControlGizmo.COMPASS_REGION);
        Tube tube = (Tube)part.getGeometry();
        double ri = tube.getInternalRadius();
        double re = tube.getExternalRadius();
        double z0 = part.getPosition().z();
        double z1 = z0 + tube.getHeight();

        for ( int i = 0; i < COMPASS_SLICES; i++ ) {
            double a0 = 2 * Math.PI * i / COMPASS_SLICES;
            double a1 = 2 * Math.PI * (i + 1) / COMPASS_SLICES;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);
            double cm = Math.cos((a0 + a1) / 2), sm = Math.sin((a0 + a1) / 2);

            // Caps
            addPolygon(list, gizmo, projection, viewDirection, new Vector3Dd[] {
                new Vector3Dd(ri * c0, ri * s0, z1), new Vector3Dd(re * c0, re * s0, z1),
                new Vector3Dd(re * c1, re * s1, z1), new Vector3Dd(ri * c1, ri * s1, z1)},
                new Vector3Dd(0, 0, 1), color, opacity);
            addPolygon(list, gizmo, projection, viewDirection, new Vector3Dd[] {
                new Vector3Dd(ri * c0, ri * s0, z0), new Vector3Dd(ri * c1, ri * s1, z0),
                new Vector3Dd(re * c1, re * s1, z0), new Vector3Dd(re * c0, re * s0, z0)},
                new Vector3Dd(0, 0, -1), color, opacity);
            // Walls
            addPolygon(list, gizmo, projection, viewDirection, new Vector3Dd[] {
                new Vector3Dd(re * c0, re * s0, z0), new Vector3Dd(re * c1, re * s1, z0),
                new Vector3Dd(re * c1, re * s1, z1), new Vector3Dd(re * c0, re * s0, z1)},
                new Vector3Dd(cm, sm, 0), color, opacity);
            addPolygon(list, gizmo, projection, viewDirection, new Vector3Dd[] {
                new Vector3Dd(ri * c1, ri * s1, z0), new Vector3Dd(ri * c0, ri * s0, z0),
                new Vector3Dd(ri * c0, ri * s0, z1), new Vector3Dd(ri * c1, ri * s1, z1)},
                new Vector3Dd(-cm, -sm, 0), color, opacity);
        }
    }
}
