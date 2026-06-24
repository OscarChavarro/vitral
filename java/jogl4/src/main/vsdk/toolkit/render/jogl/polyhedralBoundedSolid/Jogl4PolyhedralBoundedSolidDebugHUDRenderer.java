package vsdk.toolkit.render.jogl.polyhedralBoundedSolid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

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

public class Jogl4PolyhedralBoundedSolidDebugHUDRenderer
{
    private static final double VERTEX_LABEL_GROUPING_PIXELS = 18.0;
    private static final double SCREEN_DISTANCE_DELTA = 1.0;

    public static void drawSelectedFaceLabel(
        Graphics2D g,
        PolyhedralBoundedSolid solid,
        int faceIndex,
        Camera camera,
        int viewportWidth,
        int viewportHeight)
    {
        if ( g == null || solid == null || solid.getPolygonsList() == null ||
             faceIndex < 0 || faceIndex >= solid.getPolygonsList().size() ) {
            return;
        }

        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(faceIndex);
        ArrayList<Vector3Dd> projectedVertices = collectProjectedFaceVertices(
            face, camera, viewportWidth, viewportHeight);
        if ( projectedVertices.isEmpty() ) {
            return;
        }

        Vector3Dd projectedMidpoint = averageProjectedPosition(projectedVertices);
        g.setColor(Color.CYAN);
        g.drawString(Integer.toString(face.id),
            (int)Math.round(projectedMidpoint.x()),
            (int)Math.round(projectedMidpoint.y()));
    }

    public static void drawDebugVertexLabels(
        Graphics2D g,
        PolyhedralBoundedSolid solid,
        Camera camera,
        int viewportWidth,
        int viewportHeight)
    {
        if ( g == null || solid == null || solid.getVerticesList() == null ) {
            return;
        }

        ArrayList<VertexLabelGroup> vertexGroups = buildVertexGroups(solid,
            camera, viewportWidth, viewportHeight);

        for ( int i = 0; i < vertexGroups.size(); i++ ) {
            VertexLabelGroup group = vertexGroups.get(i);
            ArrayList<_PolyhedralBoundedSolidVertex> visibleVertices =
                filterVisibleVertices(group.vertices, solid, camera);

            if ( !visibleVertices.isEmpty() ) {
                g.setColor(Color.WHITE);
                g.drawString(buildVertexIdsLabel(visibleVertices),
                    (int)Math.round(group.projectedPosition.x()) + 4,
                    (int)Math.round(group.projectedPosition.y()) + 4);
            }
        }
    }

    private static ArrayList<VertexLabelGroup> buildVertexGroups(
        PolyhedralBoundedSolid solid,
        Camera camera,
        int viewportWidth,
        int viewportHeight)
    {
        ArrayList<VertexLabelGroup> vertexGroups =
            new ArrayList<VertexLabelGroup>();
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        double spatialTolerance = numericContext.bigEpsilon() * SCREEN_DISTANCE_DELTA;

        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(i);
            Vector3Dd projectedPosition = projectVertexToViewport(vertex.position,
                camera, viewportWidth, viewportHeight);
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

    private static ArrayList<Vector3Dd> collectProjectedFaceVertices(
        _PolyhedralBoundedSolidFace face,
        Camera camera,
        int viewportWidth,
        int viewportHeight)
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
                        vertex.position, camera, viewportWidth, viewportHeight);
                    if ( projectedVertex != null ) {
                        projected.add(projectedVertex);
                    }
                }
                he = he.next();
            } while ( he != start );
        }
        return projected;
    }

    private static Vector3Dd projectVertexToViewport(
        Vector3Dd worldPosition,
        Camera camera,
        int viewportWidth,
        int viewportHeight)
    {
        if ( worldPosition == null || camera == null ) {
            return null;
        }

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

        double width = Math.max(1, viewportWidth);
        double height = Math.max(1, viewportHeight);
        double x = ((ndcX + 1.0) * 0.5) * width;
        double y = height - (((ndcY + 1.0) * 0.5) * height);
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
        if ( vertex == null || vertex.position == null || camera == null ) {
            return true;
        }

        Ray visibilityRay = new Ray(camera.getPosition(),
            vertex.position.subtract(camera.getPosition()));
        double vertexRayT = vertex.position.subtract(visibilityRay.getOrigin())
            .dotProduct(visibilityRay.getDirection());
        if ( vertexRayT <= VSDK.EPSILON ) {
            return true;
        }

        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        Vector3Dd closestPointOnRay = visibilityRay.getOrigin().add(
            visibilityRay.getDirection().multiply(vertexRayT));
        if ( closestPointOnRay.subtract(vertex.position).length() >=
             numericContext.bigEpsilon() ) {
            return true;
        }

        Ray hit = solid.doIntersectionFirstHit(visibilityRay);
        if ( hit == null ) {
            return true;
        }

        return !(vertexRayT - hit.getT() >= numericContext.bigEpsilon());
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
}
