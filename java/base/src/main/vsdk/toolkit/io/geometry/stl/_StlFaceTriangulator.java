package vsdk.toolkit.io.geometry.stl;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

final class _StlFaceTriangulator
{
    private static final class ProjectedVertex {
        final Vector3Dd original;
        final double x;
        final double y;

        ProjectedVertex(Vector3Dd original, double x, double y)
        {
            this.original = original;
            this.x = x;
            this.y = y;
        }
    }

    private static final class FaceBasis {
        final Vector3Dd origin;
        final Vector3Dd u;
        final Vector3Dd v;
        final Vector3Dd normal;

        FaceBasis(Vector3Dd origin, Vector3Dd u, Vector3Dd v, Vector3Dd normal)
        {
            this.origin = origin;
            this.u = u;
            this.v = v;
            this.normal = normal;
        }
    }

    private _StlFaceTriangulator()
    {
    }

    static List<_StlFacetEmitter.Facet> triangulateSolid(PolyhedralBoundedSolid solid)
    {
        ArrayList<_StlFacetEmitter.Facet> facets = new ArrayList<>();
        int i;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            facets.addAll(triangulateFace(solid.getPolygonsList().get(i)));
        }
        return facets;
    }

    private static List<_StlFacetEmitter.Facet> triangulateFace(
        _PolyhedralBoundedSolidFace face)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forFace(face);
        InfinitePlane plane = face.getContainingPlane();
        if ( plane == null ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id
                + " has no containing plane");
        }

        FaceBasis basis = buildBasis(face, plane);
        ArrayList<ProjectedVertex> flattenedVertices = new ArrayList<>();
        Polygon2D polygon = buildProjectedPolygon(face, basis, numericContext,
            flattenedVertices);

        ArrayList<MonotoneDecompositionTriangulator.Triangle> triangles =
            new ArrayList<>();
        MonotoneDecompositionTriangulator triangulator =
            new MonotoneDecompositionTriangulator();
        int triangleCount = triangulator.triangulate(polygon, triangles);
        if ( triangleCount <= 0 || triangles.isEmpty() ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id
                + " triangulation produced no triangles");
        }

        ArrayList<_StlFacetEmitter.Facet> facets = new ArrayList<>(triangles.size());
        int i;
        for ( i = 0; i < triangles.size(); i++ ) {
            MonotoneDecompositionTriangulator.Triangle triangle = triangles.get(i);
            facets.add(buildFacet(face, triangle, flattenedVertices, basis,
                numericContext, i));
        }
        return facets;
    }

    private static FaceBasis buildBasis(_PolyhedralBoundedSolidFace face,
                                        InfinitePlane plane)
    {
        Vector3Dd normal = plane.getNormal().normalized();
        Vector3Dd origin = chooseFaceAnchor(face);
        Vector3Dd referenceAxis = chooseReferenceAxis(normal);
        Vector3Dd u = referenceAxis.crossProduct(normal).normalized();
        Vector3Dd v = normal.crossProduct(u).normalized();
        return new FaceBasis(origin, u, v, normal);
    }

    private static Polygon2D buildProjectedPolygon(
        _PolyhedralBoundedSolidFace face,
        FaceBasis basis,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        List<ProjectedVertex> flattenedVertices)
    {
        Polygon2D polygon = new Polygon2D();
        polygon.loops.clear();

        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i
                    + " has no traversable half-edge");
            }

            ArrayList<ProjectedVertex> loopVertices = collectLoopVertices(face, i,
                loop, basis);
            if ( loopVertices.size() < 3 ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i
                    + " has fewer than 3 vertices");
            }

            double area = signedArea(loopVertices);
            if ( Math.abs(area) <= numericContext.bigEpsilon() ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i
                    + " has near-zero projected area");
            }

            polygon.nextLoop();
            int j;
            for ( j = 0; j < loopVertices.size(); j++ ) {
                ProjectedVertex vertex = loopVertices.get(j);
                polygon.addVertex(vertex.x, vertex.y);
                flattenedVertices.add(vertex);
            }
        }

        if ( !polygon.loops.isEmpty() && polygon.loops.get(0).vertices.isEmpty() ) {
            polygon.eraseLastLoop();
        }
        return polygon;
    }

    private static ArrayList<ProjectedVertex> collectLoopVertices(
        _PolyhedralBoundedSolidFace face,
        int loopIndex,
        _PolyhedralBoundedSolidLoop loop,
        FaceBasis basis)
    {
        ArrayList<ProjectedVertex> vertices = new ArrayList<>();
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge current = start;
        do {
            if ( current == null || current.startingVertex == null ||
                 current.startingVertex.position == null ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop "
                    + loopIndex + " is not traversable");
            }
            Vector3Dd point = current.startingVertex.position;
            Vector3Dd delta = point.subtract(basis.origin);
            vertices.add(new ProjectedVertex(point,
                delta.dotProduct(basis.u), delta.dotProduct(basis.v)));
            current = current.next();
            if ( current == null ) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop "
                    + loopIndex + " is not closed");
            }
        } while ( current != start );
        return vertices;
    }

    private static _StlFacetEmitter.Facet buildFacet(
        _PolyhedralBoundedSolidFace face,
        MonotoneDecompositionTriangulator.Triangle triangle,
        List<ProjectedVertex> flattenedVertices,
        FaceBasis basis,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
        int triangleIndex)
    {
        validateTriangleIndices(face, triangle, flattenedVertices.size());

        Vector3Dd a = reproject(flattenedVertices.get(triangle.a), basis);
        Vector3Dd b = reproject(flattenedVertices.get(triangle.b), basis);
        Vector3Dd c = reproject(flattenedVertices.get(triangle.c), basis);

        if ( !PolyhedralBoundedSolidNumericPolicy.pointsSeparated(a, b, numericContext) ||
             !PolyhedralBoundedSolidNumericPolicy.pointsSeparated(b, c, numericContext) ||
             !PolyhedralBoundedSolidNumericPolicy.pointsSeparated(a, c, numericContext) ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id + " triangulation produced "
                + "a repeated-vertex triangle at index " + triangleIndex);
        }

        Vector3Dd normal = b.subtract(a).crossProduct(c.subtract(a));
        double normalLength = normal.length();
        if ( normalLength <= numericContext.bigEpsilon() ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id + " triangulation produced "
                + "a zero-area triangle at index " + triangleIndex);
        }
        normal = normal.multiply(1.0 / normalLength);

        if ( normal.dotProduct(basis.normal) < 0.0 ) {
            Vector3Dd temp = b;
            b = c;
            c = temp;
            normal = b.subtract(a).crossProduct(c.subtract(a)).normalized();
            if ( normal.dotProduct(basis.normal) < 0.0 ) {
                normal = basis.normal;
            }
        }

        return new _StlFacetEmitter.Facet(normal, a, b, c);
    }

    private static void validateTriangleIndices(
        _PolyhedralBoundedSolidFace face,
        MonotoneDecompositionTriangulator.Triangle triangle,
        int vertexCount)
    {
        if ( triangle.a < 0 || triangle.a >= vertexCount ||
             triangle.b < 0 || triangle.b >= vertexCount ||
             triangle.c < 0 || triangle.c >= vertexCount ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id
                + " triangulation returned an out-of-range index");
        }
    }

    private static Vector3Dd reproject(ProjectedVertex projected, FaceBasis basis)
    {
        return basis.origin
            .add(basis.u.multiply(projected.x))
            .add(basis.v.multiply(projected.y));
    }

    private static double signedArea(List<ProjectedVertex> vertices)
    {
        double areaTwice = 0.0;
        int i;
        for ( i = 0; i < vertices.size(); i++ ) {
            ProjectedVertex current = vertices.get(i);
            ProjectedVertex next = vertices.get((i + 1) % vertices.size());
            areaTwice += current.x * next.y - next.x * current.y;
        }
        return areaTwice * 0.5;
    }

    private static Vector3Dd chooseFaceAnchor(_PolyhedralBoundedSolidFace face)
    {
        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop != null && loop.boundaryStartHalfEdge != null &&
                 loop.boundaryStartHalfEdge.startingVertex != null ) {
                return loop.boundaryStartHalfEdge.startingVertex.position;
            }
        }
        throw new IllegalStateException(
            "STL export rejected: face " + face.id + " has no anchor vertex");
    }

    private static Vector3Dd chooseReferenceAxis(Vector3Dd normal)
    {
        double ax = Math.abs(normal.x());
        double ay = Math.abs(normal.y());
        double az = Math.abs(normal.z());
        if ( ax <= ay && ax <= az ) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        if ( ay <= az ) {
            return new Vector3Dd(0.0, 1.0, 0.0);
        }
        return new Vector3Dd(0.0, 0.0, 1.0);
    }
}
