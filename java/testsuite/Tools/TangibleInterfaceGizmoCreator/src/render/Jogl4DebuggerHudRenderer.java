package render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;

import models.GizmoNames;
import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;

public class Jogl4DebuggerHudRenderer
{
    private static final int LINE_HEIGHT = 34;
    private static final int HUD_TOP_PADDING = 28;
    private static final int HUD_LEFT_PADDING = 16;
    private static final int HUD_BOTTOM_PADDING = 16;
    private static final double VERTEX_LABEL_GROUPING_PIXELS = 18.0;
    private static final double SCREEN_DISTANCE_DELTA = 1.0;

    private final TangibleInterfaceGizmosModel model;
    private final Font hudFont;
    private final Font labelFont;
    private RGBAImageUncompressed overlayImage;
    private BufferedImage bufferedOverlay;
    private int viewportWidth;
    private int viewportHeight;

    public Jogl4DebuggerHudRenderer(TangibleInterfaceGizmosModel model)
    {
        this.model = model;
        this.hudFont = new Font("SansSerif", Font.BOLD, 18);
        this.labelFont = new Font("SansSerif", Font.PLAIN, 12);
        this.overlayImage = null;
        this.bufferedOverlay = null;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
    }

    public void init(GLAutoDrawable drawable)
    {
        updateViewportSize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    public void updateViewportSize(int width, int height)
    {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
    }

    public void draw(GLAutoDrawable drawable)
    {
        if ( drawable == null || model == null ) {
            return;
        }

        GL4 gl = drawable.getGL().getGL4();
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int width = viewportWidth > 0 ? viewportWidth : Math.max(1, viewport[2]);
        int height = viewportHeight > 0 ? viewportHeight : Math.max(1, viewport[3]);
        updateViewportSize(width, height);
        ensureOverlayBuffers(width, height);

        Graphics2D g = bufferedOverlay.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, width, height);

        drawHudText(g, width, height);
        drawSelectedFaceLabel(g);
        drawDebugVertexLabels(g);
        g.dispose();

        copyBufferedOverlayToImage(width, height);
        Jogl4ImageRenderer.unload(gl, overlayImage);
        Jogl4ImageRenderer.draw(gl, overlayImage);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        if ( drawable != null && overlayImage != null ) {
            Jogl4ImageRenderer.unload(drawable.getGL().getGL4(), overlayImage);
        }
        overlayImage = null;
        bufferedOverlay = null;
    }

    private void drawHudText(Graphics2D g, int width, int height)
    {
        int blockHeight = HUD_TOP_PADDING + 5 * LINE_HEIGHT + HUD_BOTTOM_PADDING;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, Math.min(height, blockHeight));
        g.setFont(hudFont);
        g.setColor(new Color(255, 242, 51));

        String showingFaceLoopMessage = "Face [1, 2]: " + formatFaceLoopLabel();
        String selectedModelMessage = "Selected model [3, 4]: "
            + model.getSolidModelName().name()
            + " (" + model.getSolidModelName().getDisplayIndex()
            + "/" + GizmoNames.getTotalModels() + ")";
        String verticesMessage = "Vertices [v]: "
            + (model.notDebugVertices() ? "OFF" : "ON");
        String referenceFrameMessage = "Reference frame [Space]: "
            + (model.isShowCoordinateSystem() ? "ON" : "OFF");
        String geometryControlsMessage = String.format(Locale.US,
            "Inner radius (5/6): %.2f, outterRadius (7/8): %.2f, base height (9/0): %.2f",
            model.getInnerRadius(), model.getOuterRadius(), model.getBaseHeight());

        g.drawString(showingFaceLoopMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING);
        g.drawString(selectedModelMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + LINE_HEIGHT);
        g.drawString(verticesMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + 2 * LINE_HEIGHT);
        g.drawString(referenceFrameMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + 3 * LINE_HEIGHT);
        g.drawString(geometryControlsMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + 4 * LINE_HEIGHT);

        if ( model.isErrorState() ) {
            g.setColor(new Color(255, 38, 38));
            g.drawString(model.getErrorMessage(), HUD_LEFT_PADDING, height - 16);
        }
    }

    private void drawDebugVertexLabels(Graphics2D g)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        if ( model.notDebugVertices() || solid == null || solid.getVerticesList() == null ) {
            return;
        }

        ArrayList<VertexLabelGroup> vertexGroups = buildVertexGroups(solid);
        g.setFont(labelFont);

        for ( int i = 0; i < vertexGroups.size(); i++ ) {
            VertexLabelGroup group = vertexGroups.get(i);
            ArrayList<_PolyhedralBoundedSolidVertex> visibleVertices =
                filterVisibleVertices(group.vertices, solid, model.getCamera());

            if ( !visibleVertices.isEmpty() ) {
                g.setColor(Color.WHITE);
                g.drawString(buildVertexIdsLabel(visibleVertices),
                    (int)Math.round(group.projectedPosition.x()) + 4,
                    (int)Math.round(group.projectedPosition.y()) + 4);
            }
        }
    }

    private void drawSelectedFaceLabel(Graphics2D g)
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
        ArrayList<Vector3Dd> projectedVertices = collectProjectedFaceVertices(face);
        if ( projectedVertices.isEmpty() ) {
            return;
        }

        Vector3Dd projectedMidpoint = averageProjectedPosition(projectedVertices);
        g.setFont(labelFont);
        g.setColor(Color.CYAN);
        g.drawString(Integer.toString(face.id),
            (int)Math.round(projectedMidpoint.x()),
            (int)Math.round(projectedMidpoint.y()));
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

    private void ensureOverlayBuffers(int width, int height)
    {
        if ( overlayImage != null && bufferedOverlay != null &&
             overlayImage.getXSize() == width && overlayImage.getYSize() == height ) {
            return;
        }
        overlayImage = new RGBAImageUncompressed();
        overlayImage.init(width, height);
        bufferedOverlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private void copyBufferedOverlayToImage(int width, int height)
    {
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                int rgba = bufferedOverlay.getRGB(x, y);
                byte r = (byte)((rgba >> 16) & 0xFF);
                byte g = (byte)((rgba >> 8) & 0xFF);
                byte b = (byte)(rgba & 0xFF);
                byte a = (byte)((rgba >> 24) & 0xFF);
                overlayImage.putPixel(x, y, r, g, b, a);
            }
        }
    }

    private ArrayList<VertexLabelGroup> buildVertexGroups(PolyhedralBoundedSolid solid)
    {
        ArrayList<VertexLabelGroup> vertexGroups =
            new ArrayList<VertexLabelGroup>();
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        double spatialTolerance = numericContext.bigEpsilon() * SCREEN_DISTANCE_DELTA;

        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(i);
            Vector3Dd projectedPosition = projectVertexToViewport(vertex.position);
            VertexLabelGroup group;

            if ( projectedPosition == null ) {
                continue;
            }
            group = findVertexGroup(vertexGroups, vertex, projectedPosition,
                spatialTolerance);
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

    private ArrayList<Vector3Dd> collectProjectedFaceVertices(
        _PolyhedralBoundedSolidFace face)
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
                    Vector3Dd projectedVertex = projectVertexToViewport(vertex.position);
                    if ( projectedVertex != null ) {
                        projected.add(projectedVertex);
                    }
                }
                he = he.next();
            } while ( he != start );
        }
        return projected;
    }

    private Vector3Dd projectVertexToViewport(Vector3Dd worldPosition)
    {
        if ( worldPosition == null || model.getCamera() == null ) {
            return null;
        }

        Camera camera = model.getCamera();
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        Vector4Dd clip = projection.multiply(new Vector4Dd(worldPosition.x(),
            worldPosition.y(), worldPosition.z(), 1.0));
        if ( Math.abs(clip.w()) <= VSDK.EPSILON ) {
            return null;
        }

        double ndcX = clip.x() / clip.w();
        double ndcY = clip.y() / clip.w();
        double ndcZ = clip.z() / clip.w();
        if ( ndcX < -1.0 || ndcX > 1.0 || ndcY < -1.0 || ndcY > 1.0 ||
             ndcZ < -1.0 || ndcZ > 1.0 ) {
            return null;
        }

        double x = ((ndcX + 1.0) * 0.5) * viewportWidth;
        double y = viewportHeight - (((ndcY + 1.0) * 0.5) * viewportHeight);
        double z = (ndcZ + 1.0) * 0.5;
        return new Vector3Dd(x, y, z);
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
            boolean isVisible = isVertexLabelVisible(vertex, solid, camera);
            if ( isVisible ) {
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
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext;
        Ray visibilityRay;
        double vertexRayT;
        Vector3Dd closestPointOnRay;
        Ray hit;

        if ( vertex == null || vertex.position == null || camera == null ) {
            return true;
        }

        visibilityRay = new Ray(camera.getPosition(),
            vertex.position.subtract(camera.getPosition()));
        vertexRayT = vertex.position.subtract(visibilityRay.origin())
            .dotProduct(visibilityRay.direction());
        if ( vertexRayT <= VSDK.EPSILON ) {
            return true;
        }

        numericContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        closestPointOnRay = visibilityRay.origin().add(
            visibilityRay.direction().multiply(vertexRayT));
        if ( closestPointOnRay.subtract(vertex.position).length() >=
             numericContext.bigEpsilon() ) {
            return true;
        }

        hit = solid.doIntersection(visibilityRay);
        if ( hit == null ) {
            return true;
        }

        return !(vertexRayT - hit.t() >= numericContext.bigEpsilon());
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

    private static void drawTopRight(Graphics2D g, int width, String text, int baselineY)
    {
        Rectangle2D textBounds = g.getFontMetrics().getStringBounds(text, g);
        int x = width - HUD_LEFT_PADDING - (int)Math.ceil(textBounds.getWidth());
        g.drawString(text, Math.max(HUD_LEFT_PADDING, x), baselineY);
    }
}
