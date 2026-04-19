//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Encapsulates preflight classification and no-intersection resolution for
Boolean set operations. This keeps containment/touching policy separate from
the main intersection/splitting pipeline.
*/
final class _PolyhedralBoundedSolidSetNonIntersectingClassifier
    extends _PolyhedralBoundedSolidOperator
{
    private static final int NO_INT_RELATION_DISJOINT = 0;
    private static final int NO_INT_RELATION_TOUCHING = 1;
    private static final int NO_INT_RELATION_A_IN_B = 2;
    private static final int NO_INT_RELATION_B_IN_A = 3;

    private _PolyhedralBoundedSolidSetNonIntersectingClassifier()
    {
    }

    static boolean runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int aInB = classifySolidAgainstSolid(inSolidA, inSolidB);
        int bInA = classifySolidAgainstSolid(inSolidB, inSolidA);
        int relation = classifyNoIntersectionRelation(aInB, bInA);

        if ( relation != NO_INT_RELATION_TOUCHING ) {
            return false;
        }

        if ( hasProperEdgeFaceIntersection(inSolidA, inSolidB) ) {
            return false;
        }
        if ( hasProperEdgeFaceIntersection(inSolidB, inSolidA) ) {
            return false;
        }

        return true;
    }

    static PolyhedralBoundedSolid runSetOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int aInB = classifySolidAgainstSolid(inSolidA, inSolidB);
        int bInA = classifySolidAgainstSolid(inSolidB, inSolidA);
        int relation = classifyNoIntersectionRelation(aInB, bInA);

        if ( op == INTERSECTION ) {
            if ( relation == NO_INT_RELATION_A_IN_B ) {
                outRes.merge(inSolidA);
            }
            else if ( relation == NO_INT_RELATION_B_IN_A ) {
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if ( op == UNION ) {
            if ( relation == NO_INT_RELATION_A_IN_B ) {
                outRes.merge(inSolidB);
            }
            else if ( relation == NO_INT_RELATION_B_IN_A ) {
                outRes.merge(inSolidA);
            }
            else {
                outRes.merge(inSolidA);
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if ( relation == NO_INT_RELATION_A_IN_B ) {
            return outRes;
        }
        if ( relation == NO_INT_RELATION_B_IN_A ) {
            outRes.merge(inSolidA);
            inSolidB.revert();
            outRes.merge(inSolidB);
            outRes.compactIds();
            return outRes;
        }
        outRes.merge(inSolidA);
        return outRes;
    }

    private static int compareToZero(double value)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static int pointInFace(_PolyhedralBoundedSolidFace face,
        Vector3D point)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .pointInFace(face, point);
    }

    private static int classifyPointAgainstSolid(PolyhedralBoundedSolid solid,
        Vector3D point)
    {
        int i, j;
        _PolyhedralBoundedSolidFace face;
        double eps = numericContext.bigEpsilon();
        int insideVotes = 0;
        int outsideVotes = 0;

        if ( solid == null || solid.polygonsList.size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            face = solid.polygonsList.get(i);
            if ( face.containingPlane == null ) {
                continue;
            }
            if ( Math.abs(face.containingPlane.pointDistance(point)) <= eps ) {
                if ( face.testPointInside(point, eps) != Geometry.OUTSIDE ) {
                    return Geometry.LIMIT;
                }
            }
        }

        Vector3D[] dirs = {
            new Vector3D(1.0, 0.371, 0.137),
            new Vector3D(0.193, 1.0, 0.417),
            new Vector3D(0.217, 0.173, 1.0)
        };

        for ( j = 0; j < dirs.length; j++ ) {
            int hits = 0;
            boolean ambiguous = false;
            ArrayList<Double> distances = new ArrayList<Double>();
            Ray ray = new Ray(point, dirs[j]);

            for ( i = 0; i < solid.polygonsList.size(); i++ ) {
                face = solid.polygonsList.get(i);
                if ( face.containingPlane == null ) {
                    ambiguous = true;
                    break;
                }
                Ray rayHit = new Ray(ray);
                if ( !face.containingPlane.doIntersection(rayHit) ) {
                    continue;
                }
                if ( rayHit.t <= eps ) {
                    continue;
                }

                Vector3D pi = rayHit.origin.add(
                    rayHit.direction.multiply(rayHit.t));
                int status = face.testPointInside(pi, eps);
                if ( status == Geometry.LIMIT ) {
                    ambiguous = true;
                    break;
                }
                if ( status == Geometry.INSIDE ) {
                    boolean duplicated = false;
                    int k;
                    for ( k = 0; k < distances.size(); k++ ) {
                        if ( Math.abs(distances.get(k).doubleValue() - rayHit.t)
                             <= eps ) {
                            duplicated = true;
                            break;
                        }
                    }
                    if ( !duplicated ) {
                        distances.add(Double.valueOf(rayHit.t));
                        hits++;
                    }
                }
            }

            if ( !ambiguous ) {
                if ( (hits % 2) == 1 ) {
                    insideVotes++;
                }
                else {
                    outsideVotes++;
                }
            }
        }

        if ( insideVotes > outsideVotes ) {
            return Geometry.INSIDE;
        }
        if ( outsideVotes > insideVotes ) {
            return Geometry.OUTSIDE;
        }
        return Geometry.LIMIT;
    }

    private static int classifySolidAgainstSolid(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        int i;
        boolean sawLimit = false;
        boolean sawOutside = false;

        if ( solidA == null || solidA.verticesList.size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solidA.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solidA.verticesList.get(i);
            int status = classifyPointAgainstSolid(solidB, v.position);
            if ( status == Geometry.INSIDE ) {
                return Geometry.INSIDE;
            }
            if ( status == Geometry.LIMIT ) {
                sawLimit = true;
            }
            else {
                sawOutside = true;
            }
        }

        if ( sawLimit ) {
            return Geometry.LIMIT;
        }
        if ( sawOutside ) {
            return Geometry.OUTSIDE;
        }
        return Geometry.OUTSIDE;
    }

    private static int classifyNoIntersectionRelation(int aInB, int bInA)
    {
        if ( aInB == Geometry.INSIDE ) {
            return NO_INT_RELATION_A_IN_B;
        }
        if ( bInA == Geometry.INSIDE ) {
            return NO_INT_RELATION_B_IN_A;
        }
        if ( aInB == Geometry.LIMIT || bInA == Geometry.LIMIT ) {
            return NO_INT_RELATION_TOUCHING;
        }
        return NO_INT_RELATION_DISJOINT;
    }

    private static boolean hasProperEdgeFaceIntersection(
        PolyhedralBoundedSolid current,
        PolyhedralBoundedSolid other)
    {
        int i, j;
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidFace face;
        _PolyhedralBoundedSolidVertex v1, v2;
        double d1, d2, d3, t;
        int s1, s2;
        Vector3D p;

        if ( current == null || other == null ) {
            return false;
        }

        for ( i = 0; i < current.edgesList.size(); i++ ) {
            edge = current.edgesList.get(i);
            if ( edge == null || edge.rightHalf == null ||
                 edge.leftHalf == null ) {
                continue;
            }
            v1 = edge.rightHalf.startingVertex;
            v2 = edge.leftHalf.startingVertex;
            if ( v1 == null || v2 == null ) {
                continue;
            }

            for ( j = 0; j < other.polygonsList.size(); j++ ) {
                face = other.polygonsList.get(j);
                if ( face == null || face.containingPlane == null ) {
                    continue;
                }

                d1 = face.containingPlane.pointDistance(v1.position);
                d2 = face.containingPlane.pointDistance(v2.position);
                s1 = compareToZero(d1);
                s2 = compareToZero(d2);

                if ( !((s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1)) ) {
                    continue;
                }

                t = d1 / (d1 - d2);
                p = v1.position.add(
                    v2.position.substract(v1.position).multiply(t));
                d3 = face.containingPlane.pointDistance(p);
                if ( compareToZero(d3) != 0 ) {
                    continue;
                }

                if ( pointInFace(face, p) == Geometry.INSIDE ) {
                    return true;
                }
            }
        }
        return false;
    }
}
