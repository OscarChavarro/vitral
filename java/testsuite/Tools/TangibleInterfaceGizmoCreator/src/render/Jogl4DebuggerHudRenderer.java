package render;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;

import models.GizmoNames;
import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

public class Jogl4DebuggerHudRenderer
{
    private static final int LINE_HEIGHT = 34;
    private static final double VERTEX_LABEL_GROUPING_PIXELS = 18.0;
    private static final double SCREEN_DISTANCE_DELTA = 1;

    private final TangibleInterfaceGizmosModel model;
    private TextRenderer hudTextRenderer;
    private TextRenderer vertexLabelRenderer;
    private int viewportWidth;
    private int viewportHeight;

    public Jogl4DebuggerHudRenderer(TangibleInterfaceGizmosModel model)
    {
        this.model = model;
        this.hudTextRenderer = null;
        this.vertexLabelRenderer = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        hudTextRenderer = new TextRenderer(
            new Font("SansSerif", Font.BOLD, 18), true, true);
        vertexLabelRenderer = new TextRenderer(
            new Font("SansSerif", Font.PLAIN, 12), true, true);
        updateViewportSize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    public void updateViewportSize(int width, int height)
    {
        viewportWidth = width;
        viewportHeight = height;
    }

    public void draw(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer == null || vertexLabelRenderer == null ) {
            return;
        }

        int width = viewportWidth > 0 ? viewportWidth : drawable.getSurfaceWidth();
        int height = viewportHeight > 0 ? viewportHeight : drawable.getSurfaceHeight();
        String showingFaceLoopMessage = "Face [1, 2]: " + formatFaceLoopLabel();
        String selectedModelMessage = "Selected model [3, 4]: "
            + model.getSolidModelName().name()
            + " (" + model.getSolidModelName().getDisplayIndex()
            + "/" + GizmoNames.getTotalModels() + ")";
        String verticesMessage = "Vertices [v]: "
            + (model.notDebugVertices() ? "OFF" : "ON");
        String referenceFrameMessage = "Reference frame [Space]: "
            + (model.isShowCoordinateSystem() ? "ON" : "OFF");
        String nrMessage = "NR [q, Q]: " + model.getSubdivisionCircumference();
        String nhMessage = "NH [w, W]: " + model.getSubdivisionHeight();

        hudTextRenderer.beginRendering(width, height);
        hudTextRenderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);
        hudTextRenderer.draw(showingFaceLoopMessage, 16, height - 28);
        hudTextRenderer.draw(selectedModelMessage, 16, height - (28 + LINE_HEIGHT));
        hudTextRenderer.draw(verticesMessage, 16, height - (28 + 2 * LINE_HEIGHT));
        hudTextRenderer.draw(referenceFrameMessage, 16,
            height - (28 + 3 * LINE_HEIGHT));
        drawTopRight(hudTextRenderer, width, height, nrMessage, 28);
        drawTopRight(hudTextRenderer, width, height, nhMessage,
            28 + LINE_HEIGHT);
        if ( model.isErrorState() ) {
            hudTextRenderer.setColor(1.0f, 0.1f, 0.1f, 1.0f);
            hudTextRenderer.draw(model.getErrorMessage(), 16, 16);
        }
        hudTextRenderer.endRendering();

        drawSelectedFaceLabel(drawable, width, height);
        drawDebugVertexLabels(drawable, width, height);
    }

    private String formatFaceLoopLabel()
    {
        if ( model.getFaceIndex() == -2 ) {
            return "NONE";
        }
        if ( model.getFaceIndex() == -1 ) {
            return "ALL";
        }

        int currentFace = model.getFaceIndex() + 1;
        int totalFaces = 0;
        if ( model.getSolid() != null && model.getSolid().getPolygonsList() != null ) {
            totalFaces = model.getSolid().getPolygonsList().size();
        }
        return "[" + currentFace + "/" + totalFaces + "]";
    }

    public void dispose(GLAutoDrawable drawable)
    {
        if ( hudTextRenderer != null ) {
            hudTextRenderer.dispose();
            hudTextRenderer = null;
        }
        if ( vertexLabelRenderer != null ) {
            vertexLabelRenderer.dispose();
            vertexLabelRenderer = null;
        }
    }

    private static void drawTopRight(TextRenderer renderer, int width,
        int height, String text, int offsetFromTop)
    {
        Rectangle2D textBounds = renderer.getBounds(text);
        int x = width - 16 - (int)Math.ceil(textBounds.getWidth());
        int y = height - (int)Math.round((double)offsetFromTop);
        renderer.draw(text, x, y);
    }

    private void drawDebugVertexLabels(GLAutoDrawable drawable, int width, int height)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        if ( model.notDebugVertices() || solid == null || solid.getVerticesList() == null ) {
            return;
        }

        GL2 gl = drawable.getGL().getGL2();
        double[] modelview = new double[16];
        double[] projection = new double[16];
        int[] viewport = new int[4];

        gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
        gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        if ( viewport[2] > 0 && viewport[3] > 0 ) {
            model.getCamera().updateViewportResize(viewport[2], viewport[3]);
        }

        ArrayList<VertexLabelGroup> vertexGroups = buildVertexGroups(solid,
            modelview, projection, viewport);

        vertexLabelRenderer.beginRendering(width, height);
        for ( int i = 0; i < vertexGroups.size(); i++ ) {
            VertexLabelGroup group = vertexGroups.get(i);
            Vector3Dd projectedPosition = group.projectedPosition;
            ArrayList<_PolyhedralBoundedSolidVertex> visibleVertices =
                filterVisibleVertices(group.vertices, solid, model.getCamera());

            if ( !visibleVertices.isEmpty() ) {
                vertexLabelRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                vertexLabelRenderer.draw(buildVertexIdsLabel(visibleVertices),
                    (int)Math.round(projectedPosition.x()) + 4,
                    (int)Math.round(projectedPosition.y()) + 4);
            }
        }
        vertexLabelRenderer.endRendering();
    }

    private void drawSelectedFaceLabel(GLAutoDrawable drawable, int width, int height)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        int faceIndex = model.getFaceIndex();

        if ( solid == null || solid.getPolygonsList() == null || faceIndex < 0 ) {
            return;
        }
        if ( faceIndex >= solid.getPolygonsList().size() ) {
            return;
        }

        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(faceIndex);
        GL2 gl = drawable.getGL().getGL2();
        double[] modelview = new double[16];
        double[] projection = new double[16];
        int[] viewport = new int[4];

        gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
        gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

        ArrayList<Vector3Dd> projectedVertices = collectProjectedFaceVertices(face,
            modelview, projection, viewport);
        if ( projectedVertices.isEmpty() ) {
            return;
        }

        Vector3Dd projectedMidpoint = averageProjectedPosition(projectedVertices);
        String label = Integer.toString(face.id);

        vertexLabelRenderer.beginRendering(width, height);
        vertexLabelRenderer.setColor(0.0f, 1.0f, 1.0f, 1.0f);
        vertexLabelRenderer.draw(label,
            (int)Math.round(projectedMidpoint.x()),
            (int)Math.round(projectedMidpoint.y()));
        vertexLabelRenderer.endRendering();
    }

    private static ArrayList<Vector3Dd> collectProjectedFaceVertices(
        _PolyhedralBoundedSolidFace face,
        double[] modelview,
        double[] projection,
        int[] viewport)
    {
        ArrayList<Vector3Dd> projected = new ArrayList<Vector3Dd>();
        Set<Integer> visitedVertexIds = new LinkedHashSet<Integer>();

        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }

            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            do {
                _PolyhedralBoundedSolidVertex vertex = he.startingVertex;
                if ( vertex != null &&
                     visitedVertexIds.add(vertex.id) &&
                     vertex.position != null ) {
                    Vector3Dd projectedVertex = projectVertexToViewport(
                        vertex.position, modelview, projection, viewport);
                    if ( projectedVertex != null ) {
                        projected.add(projectedVertex);
                    }
                }
                he = he.next();
            } while ( he != start );
        }
        return projected;
    }

    private static Vector3Dd averageProjectedPosition(
        ArrayList<Vector3Dd> projectedVertices)
    {
        double sx = 0.0;
        double sy = 0.0;
        double sz = 0.0;

        for ( int i = 0; i < projectedVertices.size(); i++ ) {
            Vector3Dd p = projectedVertices.get(i);
            sx += p.x();
            sy += p.y();
            sz += p.z();
        }

        double n = projectedVertices.size();
        return new Vector3Dd(sx / n, sy / n, sz / n);
    }

    private static ArrayList<_PolyhedralBoundedSolidVertex> filterVisibleVertices(
        ArrayList<_PolyhedralBoundedSolidVertex> vertices,
        PolyhedralBoundedSolid solid,
        Camera camera)
    {
        ArrayList<_PolyhedralBoundedSolidVertex> visibleVertices =
            new ArrayList<_PolyhedralBoundedSolidVertex>();

        for ( int i = 0; i < vertices.size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = vertices.get(i);
            if ( isVertexLabelVisible(vertex, solid, camera) ) {
                visibleVertices.add(vertex);
            }
        }
        return visibleVertices;
    }

    private static boolean isVertexLabelVisible(
        _PolyhedralBoundedSolidVertex vertex,
        PolyhedralBoundedSolid solid,
        Camera camera)
    {
        if ( vertex == null || vertex.position == null || camera == null ) {
            return true;
        }

        Ray visibilityRay = new Ray(camera.getPosition(),
            vertex.position.subtract(camera.getPosition()));
        double vertexRayT = vertex.position.subtract(visibilityRay.origin())
            .dotProduct(visibilityRay.direction());
        if ( vertexRayT <= VSDK.EPSILON ) {
            return true;
        }

        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        Vector3Dd closestPointOnRay = visibilityRay.origin().add(
            visibilityRay.direction().multiply(vertexRayT));
        if ( closestPointOnRay.subtract(vertex.position).length() >=
             numericContext.bigEpsilon() ) {
            return true;
        }

        Ray hit = solid.doIntersection(visibilityRay);
        if ( hit == null ) {
            return true;
        }

        return !(vertexRayT - hit.t() >= numericContext.bigEpsilon());
    }

    private static ArrayList<VertexLabelGroup> buildVertexGroups(
        PolyhedralBoundedSolid solid,
        double[] modelview,
        double[] projection,
        int[] viewport)
    {
        ArrayList<VertexLabelGroup> vertexGroups =
            new ArrayList<VertexLabelGroup>();
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        double spatialTolerance = numericContext.bigEpsilon() * SCREEN_DISTANCE_DELTA;

        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(i);
            Vector3Dd projectedPosition = projectVertexToViewport(
                vertex.position, modelview, projection, viewport);
            if ( projectedPosition == null ) {
                continue;
            }

            VertexLabelGroup group = findVertexGroup(vertexGroups, vertex,
                projectedPosition, spatialTolerance);
            if ( group == null ) {
                vertexGroups.add(new VertexLabelGroup(vertex, projectedPosition));
            }
            else {
                group.add(vertex, projectedPosition);
            }
        }
        return vertexGroups;
    }

    private static VertexLabelGroup findVertexGroup(
        ArrayList<VertexLabelGroup> vertexGroups,
        _PolyhedralBoundedSolidVertex vertex,
        Vector3Dd projectedPosition,
        double spatialTolerance)
    {
        for ( int i = 0; i < vertexGroups.size(); i++ ) {
            VertexLabelGroup group = vertexGroups.get(i);
            if ( group.containsCloseVertex(vertex, projectedPosition,
                 spatialTolerance) ) {
                return group;
            }
        }
        return null;
    }

    private static double distanceSquared3D(Vector3Dd a, Vector3Dd b)
    {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceSquared2D(Vector3Dd a, Vector3Dd b)
    {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return dx * dx + dy * dy;
    }

    private static String buildVertexIdsLabel(
        ArrayList<_PolyhedralBoundedSolidVertex> vertices)
    {
        StringBuilder label = new StringBuilder();
        for ( int i = 0; i < vertices.size(); i++ ) {
            if ( i > 0 ) {
                label.append(", ");
            }
            label.append(vertices.get(i).id);
        }
        return label.toString();
    }

    private static Vector3Dd projectVertexToViewport(
        Vector3Dd worldPosition,
        double[] modelview,
        double[] projection,
        int[] viewport)
    {
        double[] projected = new double[4];
        if ( !(new GLU()).gluProject(worldPosition.x(), worldPosition.y(),
                worldPosition.z(), modelview, 0, projection, 0,
                viewport, 0, projected, 0) ) {
            return null;
        }
        if ( projected[2] < 0.0 || projected[2] > 1.0 ) {
            return null;
        }
        return new Vector3Dd(projected[0], projected[1], projected[2]);
    }

    private static final class VertexLabelGroup
    {
        private final ArrayList<_PolyhedralBoundedSolidVertex> vertices;
        private final ArrayList<Vector3Dd> projectedPositions;
        private final Vector3Dd projectedPosition;

        private VertexLabelGroup(_PolyhedralBoundedSolidVertex vertex,
            Vector3Dd projectedPosition)
        {
            this.vertices = new ArrayList<_PolyhedralBoundedSolidVertex>();
            this.projectedPositions = new ArrayList<Vector3Dd>();
            this.projectedPosition = projectedPosition;
            add(vertex, projectedPosition);
        }

        private void add(_PolyhedralBoundedSolidVertex vertex,
            Vector3Dd projectedPosition)
        {
            vertices.add(vertex);
            projectedPositions.add(projectedPosition);
        }

        private boolean containsCloseVertex(
            _PolyhedralBoundedSolidVertex vertex,
            Vector3Dd projectedPosition,
            double spatialTolerance)
        {
            double spatialToleranceSquared = spatialTolerance * spatialTolerance;
            double viewportToleranceSquared = VERTEX_LABEL_GROUPING_PIXELS *
                VERTEX_LABEL_GROUPING_PIXELS;

            for ( int i = 0; i < vertices.size(); i++ ) {
                if ( distanceSquared3D(vertices.get(i).position,
                     vertex.position) <= spatialToleranceSquared ) {
                    return true;
                }
                if ( distanceSquared2D(projectedPositions.get(i),
                     projectedPosition) <= viewportToleranceSquared ) {
                    return true;
                }
            }
            return false;
        }
    }
}
