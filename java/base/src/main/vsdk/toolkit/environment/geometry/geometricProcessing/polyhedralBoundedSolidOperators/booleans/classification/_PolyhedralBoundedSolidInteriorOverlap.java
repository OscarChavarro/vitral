//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidFaceGeometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Decides whether the interiors of two solids overlap: whether some point is
strictly inside both. The "no proper intersection" preflights of the set
operator ([MANT1988] 15.4, problem 15.1) and the "this result cannot be
empty" guard of `setOp` rest on this decision.

<p>The decision follows from the boundaries, with no sampling of space
(plan-stage11 Phase D). If the interiors overlap in a region R, the
boundary of R is made of pieces of the two boundaries, so one of these
holds, and each of them is enough:</p>
<ul>
<li>part of a face of one solid is strictly inside the other;</li>
<li>a face of one lies on a face of the other with the material of both on
    the same side (then the points just under the shared part are inside
    both).</li>
</ul>

<p>Each face F of one solid is therefore cut by everything of the other
solid Y that meets its plane: the segments where the faces of Y cross that
plane and the edges of Y that lie in it. Together with the edges of F
these segments divide F into cells, and inside one cell the answer to "is
this point inside Y" does not change. Every cell has one of those segments
on its border, so a point beside the middle of every piece of every
segment, on both sides, visits every cell. When such a point is inside Y
or on its boundary, the point just under it, inside the solid of F, is
tried: if it is inside both solids it is the witness. The answer is true
only with a witness in hand.</p>

<p>The points stand at 4 tolerances from the segments and from the face,
so an overlap thinner than about 5 tolerances is not seen: at that size
the two boundaries are the same surface for the kernel, and the pair is
a contact.</p>

<p>It replaced a test by sampling: 27 probes in the common bounding box,
then a grid built from the vertex coordinates of both solids. That test
missed overlaps that fell between the samples (two prisms with coplanar
ends overlapping in a corner, InteriorOverlapPredicateTest) and its cost
grew with the cube of the number of vertices.</p>
*/
final class _PolyhedralBoundedSolidInteriorOverlap
    extends _PolyhedralBoundedSolidOperator
{
    /** Distance of the probe points from the cutting segments, in tolerances. */
    private static final double CLEARANCE_IN_TOLERANCES = 4.0;

    private _PolyhedralBoundedSolidInteriorOverlap()
    {
    }

    /** A segment in the plane of the face being cut, in plane coordinates. */
    private static final class Cut
    {
        final double startX;
        final double startY;
        final double endX;
        final double endY;

        Cut(double startX, double startY, double endX, double endY)
        {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    /**
    True when some point is strictly inside both solids. The numeric
    context must be the one of the pair (the caller sets it).
    */
    static boolean interiorsOverlap(PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        if ( solidA == null || solidB == null ||
             solidA.getPolygonsList().size() < 1 ||
             solidB.getPolygonsList().size() < 1 ) {
            return false;
        }
        double eps = numericContext.bigEpsilon();
        double[] a = solidA.getMinMax();
        double[] b = solidB.getMinMax();
        int axis;

        for ( axis = 0; axis < 3; axis++ ) {
            if ( Math.min(a[axis + 3], b[axis + 3]) -
                 Math.max(a[axis], b[axis]) <= eps ) {
                return false;
            }
        }

        PolyhedralBoundedSolidFaceGeometry[] facesA =
            PolyhedralBoundedSolidFaceGeometry.of(solidA);
        PolyhedralBoundedSolidFaceGeometry[] facesB =
            PolyhedralBoundedSolidFaceGeometry.of(solidB);

        return someFaceCellIsInside(solidA, facesA, solidB, facesB) ||
            someFaceCellIsInside(solidB, facesB, solidA, facesA);
    }

    private static boolean someFaceCellIsInside(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidFaceGeometry[] faces,
        PolyhedralBoundedSolid other,
        PolyhedralBoundedSolidFaceGeometry[] otherFaces)
    {
        double eps = numericContext.bigEpsilon();
        int i;
        int j;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            InfinitePlane plane = faces[i].getPlane();
            if ( plane == null ) {
                continue;
            }
            Vector3Dd normal = unit(plane.getNormal());
            Vector3Dd origin = firstVertex(face);
            if ( normal == null || origin == null ) {
                continue;
            }
            Vector3Dd u = inPlaneAxis(normal);
            Vector3Dd v = normal.crossProduct(u);
            ArrayList<Cut> cuts = new ArrayList<Cut>();

            addEdges(face, origin, u, v, cuts);
            for ( j = 0; j < other.getPolygonsList().size(); j++ ) {
                if ( otherFaces[j].getPlane() == null ||
                     faces[i].boxSeparatedFrom(otherFaces[j], eps) ) {
                    continue;
                }
                addCutsOfFace(plane, normal, origin, u, v,
                    other.getPolygonsList().get(j), otherFaces[j].getPlane(),
                    cuts);
            }
            if ( someCellIsInside(face, plane, normal, origin, u, v, cuts,
                     solid, faces, other, otherFaces) ) {
                return true;
            }
        }
        return false;
    }

    /** The edges of every loop of `face`, as cuts. */
    private static void addEdges(_PolyhedralBoundedSolidFace face,
        Vector3Dd origin, Vector3Dd u, Vector3Dd v, ArrayList<Cut> cuts)
    {
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            if ( he == null ) {
                continue;
            }
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                addCut(he.startingVertex.position,
                    next.startingVertex.position, origin, u, v, cuts);
                he = next;
            } while ( he != start );
        }
    }

    /**
    What `otherFace` leaves on the plane of the face being cut: its edges
    that lie in the plane, and the stretches of the line where the two
    planes meet that are inside `otherFace`.
    */
    private static void addCutsOfFace(InfinitePlane plane, Vector3Dd normal,
        Vector3Dd origin, Vector3Dd u, Vector3Dd v,
        _PolyhedralBoundedSolidFace otherFace, InfinitePlane otherPlane,
        ArrayList<Cut> cuts)
    {
        double eps = numericContext.bigEpsilon();
        ArrayList<Vector3Dd> crossings = new ArrayList<Vector3Dd>();
        int i;
        int j;

        for ( i = 0; i < otherFace.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop =
                otherFace.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            if ( he == null ) {
                continue;
            }
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                Vector3Dd p = he.startingVertex.position;
                Vector3Dd q = next.startingVertex.position;
                double dp = plane.pointDistance(p);
                double dq = plane.pointDistance(q);
                boolean pOn = Math.abs(dp) <= eps;
                boolean qOn = Math.abs(dq) <= eps;
                if ( pOn && qOn ) {
                    addCut(p, q, origin, u, v, cuts);
                }
                if ( pOn ) {
                    crossings.add(p);
                }
                else if ( !qOn && (dp < 0.0) != (dq < 0.0) ) {
                    crossings.add(p.add(q.subtract(p).multiply(
                        dp / (dp - dq))));
                }
                he = next;
            } while ( he != start );
        }
        if ( crossings.size() < 2 ) {
            return;
        }
        Vector3Dd direction = normal.crossProduct(otherPlane.getNormal());
        if ( direction.length() <= numericContext.unitVectorTolerance() *
                 otherPlane.getNormal().length() ) {
            // Coplanar faces: the edges in the plane are all there is.
            return;
        }
        // Sort the crossings along the line where the planes meet.
        for ( i = 1; i < crossings.size(); i++ ) {
            Vector3Dd moving = crossings.get(i);
            double key = moving.dotProduct(direction);
            for ( j = i - 1; j >= 0 &&
                  crossings.get(j).dotProduct(direction) > key; j-- ) {
                crossings.set(j + 1, crossings.get(j));
            }
            crossings.set(j + 1, moving);
        }
        for ( i = 0; i + 1 < crossings.size(); i++ ) {
            Vector3Dd p = crossings.get(i);
            Vector3Dd q = crossings.get(i + 1);
            if ( q.subtract(p).length() <= eps ) {
                continue;
            }
            Vector3Dd middle = p.add(q).multiply(0.5);
            if ( otherFace.testPointInside(middle, eps, otherPlane) !=
                 Geometry.OUTSIDE ) {
                addCut(p, q, origin, u, v, cuts);
            }
        }
    }

    private static void addCut(Vector3Dd p, Vector3Dd q, Vector3Dd origin,
        Vector3Dd u, Vector3Dd v, ArrayList<Cut> cuts)
    {
        Vector3Dd a = p.subtract(origin);
        Vector3Dd b = q.subtract(origin);

        cuts.add(new Cut(a.dotProduct(u), a.dotProduct(v), b.dotProduct(u),
            b.dotProduct(v)));
    }

    /**
    Visits every cell the cuts leave on the face, through a point on each
    side of the middle of every piece of every cut.
    */
    private static boolean someCellIsInside(_PolyhedralBoundedSolidFace face,
        InfinitePlane plane, Vector3Dd normal, Vector3Dd origin, Vector3Dd u,
        Vector3Dd v, ArrayList<Cut> cuts, PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidFaceGeometry[] faces,
        PolyhedralBoundedSolid other,
        PolyhedralBoundedSolidFaceGeometry[] otherFaces)
    {
        double eps = numericContext.bigEpsilon();
        double clearance = CLEARANCE_IN_TOLERANCES * eps;
        int i;
        int k;
        int side;

        for ( i = 0; i < cuts.size(); i++ ) {
            Cut cut = cuts.get(i);
            double rx = cut.endX - cut.startX;
            double ry = cut.endY - cut.startY;
            double length = Math.sqrt(rx * rx + ry * ry);
            if ( length <= eps ) {
                continue;
            }
            double[] stops = stopsAlong(cut, i, cuts, eps);
            for ( k = 0; k + 1 < stops.length; k++ ) {
                if ( (stops[k + 1] - stops[k]) * length <= eps ) {
                    continue;
                }
                double t = (stops[k] + stops[k + 1]) / 2.0;
                for ( side = -1; side <= 1; side += 2 ) {
                    double x = cut.startX + t * rx - side * clearance * ry / length;
                    double y = cut.startY + t * ry + side * clearance * rx / length;
                    Vector3Dd point = origin.add(u.multiply(x))
                        .add(v.multiply(y));
                    if ( face.testPointInside(point, eps, plane) !=
                         Geometry.INSIDE ) {
                        continue;
                    }
                    if ( isWitness(point, normal, clearance, solid, faces,
                             other, otherFaces) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
    `point` is in the interior of a face of `solid`, and inside `other` or
    on its boundary. It proves the overlap when a point just off the face,
    at the clearance distance, is inside both solids. Asking for that
    point, and not only for `point` to be inside `other`, keeps the answer
    out of the tolerance band: a face that enters the other solid by one
    tolerance is a contact, not an overlap.
    */
    private static boolean isWitness(Vector3Dd point, Vector3Dd normal,
        double clearance, PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidFaceGeometry[] faces,
        PolyhedralBoundedSolid other,
        PolyhedralBoundedSolidFaceGeometry[] otherFaces)
    {
        int status = _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .classifyPointAgainstSolid(other, otherFaces, point);
        int side;

        if ( status == Geometry.OUTSIDE ) {
            return false;
        }
        for ( side = -1; side <= 1; side += 2 ) {
            Vector3Dd under = point.add(normal.multiply(side * clearance));
            if ( _PolyhedralBoundedSolidSetNonIntersectingClassifier
                     .classifyPointAgainstSolid(solid, faces, under) ==
                     Geometry.INSIDE &&
                 _PolyhedralBoundedSolidSetNonIntersectingClassifier
                     .classifyPointAgainstSolid(other, otherFaces, under) ==
                     Geometry.INSIDE ) {
                return true;
            }
        }
        return false;
    }

    /**
    Parameters in [0, 1] where `cut` is met by the other cuts, sorted, with
    0 and 1 at the ends: crossings, T-junctions and the ends of the cuts
    that run along it.
    */
    private static double[] stopsAlong(Cut cut, int index,
        ArrayList<Cut> cuts, double eps)
    {
        double rx = cut.endX - cut.startX;
        double ry = cut.endY - cut.startY;
        double length2 = rx * rx + ry * ry;
        double length = Math.sqrt(length2);
        ArrayList<Double> stops = new ArrayList<Double>();
        int i;
        int j;

        stops.add(Double.valueOf(0.0));
        stops.add(Double.valueOf(1.0));
        for ( i = 0; i < cuts.size(); i++ ) {
            if ( i == index ) {
                continue;
            }
            Cut o = cuts.get(i);
            double wx = o.endX - o.startX;
            double wy = o.endY - o.startY;
            double otherLength = Math.sqrt(wx * wx + wy * wy);
            double cross = rx * wy - ry * wx;
            // Ends of the other cut that lie on this one.
            addStopAt(o.startX, o.startY, cut, rx, ry, length, length2, eps, stops);
            addStopAt(o.endX, o.endY, cut, rx, ry, length, length2, eps, stops);
            if ( otherLength <= eps || Math.abs(cross) <=
                 numericContext.unitVectorTolerance() * length *
                     otherLength ) {
                continue;
            }
            double qx = o.startX - cut.startX;
            double qy = o.startY - cut.startY;
            double t = (qx * wy - qy * wx) / cross;
            double s = (qx * ry - qy * rx) / cross;
            if ( t > 0.0 && t < 1.0 && s * otherLength >= -eps &&
                 (s - 1.0) * otherLength <= eps ) {
                stops.add(Double.valueOf(t));
            }
        }
        double[] sorted = new double[stops.size()];
        for ( i = 0; i < sorted.length; i++ ) {
            sorted[i] = stops.get(i).doubleValue();
        }
        for ( i = 1; i < sorted.length; i++ ) {
            double moving = sorted[i];
            for ( j = i - 1; j >= 0 && sorted[j] > moving; j-- ) {
                sorted[j + 1] = sorted[j];
            }
            sorted[j + 1] = moving;
        }
        return sorted;
    }

    private static void addStopAt(double x, double y, Cut cut, double rx,
        double ry, double length, double length2, double eps,
        ArrayList<Double> stops)
    {
        double qx = x - cut.startX;
        double qy = y - cut.startY;
        double t = (qx * rx + qy * ry) / length2;

        if ( t > 0.0 && t < 1.0 &&
             Math.abs(qx * ry - qy * rx) / length <= eps ) {
            stops.add(Double.valueOf(t));
        }
    }

    private static Vector3Dd firstVertex(_PolyhedralBoundedSolidFace face)
    {
        if ( face.boundariesList.size() < 1 ||
             face.boundariesList.get(0).boundaryStartHalfEdge == null ) {
            return null;
        }
        return face.boundariesList.get(0).boundaryStartHalfEdge
            .startingVertex.position;
    }

    private static Vector3Dd unit(Vector3Dd vector)
    {
        double length = vector.length();

        return length > 0.0 ? vector.multiply(1.0 / length) : null;
    }

    /** A unit vector perpendicular to the unit vector `normal`. */
    private static Vector3Dd inPlaneAxis(Vector3Dd normal)
    {
        double ax = Math.abs(normal.x());
        double ay = Math.abs(normal.y());
        double az = Math.abs(normal.z());
        Vector3Dd helper = ax <= ay && ax <= az ? new Vector3Dd(1, 0, 0) :
            (ay <= az ? new Vector3Dd(0, 1, 0) : new Vector3Dd(0, 0, 1));

        return unit(normal.crossProduct(helper));
    }
}
