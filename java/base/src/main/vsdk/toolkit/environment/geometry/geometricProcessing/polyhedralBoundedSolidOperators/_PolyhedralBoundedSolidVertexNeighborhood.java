//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.function.Supplier;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Neighbourhood of a vertex classified against a plane through it, following
section [MANT1988].14.5.2 and program [MANT1988].14.4. Problem
[MANT1988].15.4 observes that the vertex/face classifier of the set
operations is the same procedure as the splitter's; both build their
neighbourhood here (Fable5.2Analysis.md pending item 11) and keep their own
reclassification rules: [MANT1988].14.5 for the splitter, table
[MANT1988].15.3 (which depends on the operation) for the set operations.
*/
final class _PolyhedralBoundedSolidVertexNeighborhood
    extends _PolyhedralBoundedSolidOperator
{
    private _PolyhedralBoundedSolidVertexNeighborhood()
    {
    }

    /**
    Classifies the neighbourhood of `vtx` against `plane`. The loop turns
    with mirror(he).next(), so the sector of `he` (its face, between
    he.previous() and he) lies between the entry of the previous half-edge
    and the entry of `he`: an entry for the far end of each edge, preceded
    by an entry for the bisector of the sector when the sector is reflex.
    Convex and flat sectors are bounded by their two edge entries; a
    reflex one is split by its bisector into two sectors narrower than a
    half-turn, as the book requires. Edge entries that lie on the plane are
    tagged INPLANE_EDGE.
    @param factory creates the entry type of the caller
    */
    static <T extends _PolyhedralBoundedSolidSectorClassification>
    ArrayList<T> classify(_PolyhedralBoundedSolidVertex vtx,
        InfinitePlane plane, Supplier<T> factory)
    {
        ArrayList<T> neighbors = new ArrayList<T>();
        _PolyhedralBoundedSolidHalfEdge he = vtx.emanatingHalfEdge;
        T c;

        do {
            if ( isReflexSector(he) ) {
                Vector3Dd bisect = inwardReflexBisector(he);
                c = factory.get();
                c.sector = he;
                c.classification = compareToZero(plane.pointDistance(bisect));
                c.isWide = true;
                c.position = new Vector3Dd(bisect);
                c.situation =
                    _PolyhedralBoundedSolidSectorClassification.CROSSING_EDGE;
                neighbors.add(c);
            }
            Vector3Dd far = he.next().startingVertex.position;
            c = factory.get();
            c.sector = he;
            c.classification = compareToZero(plane.pointDistance(far));
            c.isWide = false;
            c.position = new Vector3Dd(far);
            c.situation = c.classification == _PolyhedralBoundedSolidSectorClassification.ON
                ? _PolyhedralBoundedSolidSectorClassification.INPLANE_EDGE
                : _PolyhedralBoundedSolidSectorClassification.UNDEFINED;
            neighbors.add(c);
            he = he.mirrorHalfEdge().next();
        } while ( he != vtx.emanatingHalfEdge );

        return neighbors;
    }

    /**
    True when the face sector at he.startingVertex, between he.previous()
    and he, is reflex: (next - v) x (prev - v) points against the face
    normal (loops run counterclockwise about the normal). A flat sector (a
    vertex inserted inside an edge) is not: its two edge entries already
    bound it, and a bisector sample there lies on the plane whenever the
    face crosses it, which the edge reclassification would then turn into a
    spurious entry. The test compares the sine of the angle, so it does not
    depend on the size of the model.
    */
    static boolean isReflexSector(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3Dd v = he.startingVertex.position;
        Vector3Dd a = he.next().startingVertex.position.subtract(v);
        Vector3Dd b = he.previous().startingVertex.position.subtract(v);
        Vector3Dd n = he.parentLoop.parentFace.getContainingPlane()
            .getNormal();
        Vector3Dd axb = a.crossProduct(b);

        if ( axb.length() <= numericContext.unitVectorTolerance() *
                 a.length() * b.length() ) {
            return false;
        }
        return axb.dotProduct(n) < 0.0;
    }

    /**
    A point on the bisector of the reflex face corner at
    `he.startingVertex`, inside the face, at half the length of the shorter
    of its two edges: only its side of a plane through the vertex is used,
    and at that distance the comparison with the tolerance is relative to
    the local size of the model. For a reflex corner the sum of the two
    unit edge directions points out of the face, so it is negated.
    */
    static Vector3Dd inwardReflexBisector(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3Dd v = he.startingVertex.position;
        Vector3Dd toNext = he.next().startingVertex.position.subtract(v);
        Vector3Dd toPrevious =
            he.previous().startingVertex.position.subtract(v);
        double reach = 0.5 * Math.min(toNext.length(), toPrevious.length());
        Vector3Dd a = unit(toNext);
        Vector3Dd b = unit(toPrevious);
        Vector3Dd sum = a.add(b);

        if ( sum.length() <= numericContext.unitVectorTolerance() ) {
            // Straight angle: any inward normal works.
            sum = he.parentLoop.parentFace.getContainingPlane().getNormal()
                .crossProduct(a).multiply(-1.0);
        }
        return v.subtract(unit(sum).multiply(reach));
    }

    private static Vector3Dd unit(Vector3Dd v)
    {
        double length = v.length();
        return length > 0.0 ? v.multiply(1.0 / length) : v;
    }

    private static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value,
            numericContext);
    }
}
