package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Low-level geometric primitives shared by the structural-shape boolean fallback
builders (profile-difference, axis-aligned cell, orthogonal-profile and
offset-cylinder families). Extracted from
{@link PolyhedralBoundedSolidSetOperator} in Stage 7 R2 so that more than one
fallback family can reuse the same coordinate/profile helpers without
duplicating them. Pure code motion; no behavior change.

<p>Extends {@link _PolyhedralBoundedSolidOperator} solely to inherit the shared
{@code numericContext} tolerance state used by the epsilon comparisons.</p>
 */
final class _PolyhedralBoundedSolidFallbackGeometry
    extends _PolyhedralBoundedSolidOperator
{
    static double coordinate(Vector3Dd p, int axis)
    {
        if ( axis == 0 ) {
            return p.x();
        }
        if ( axis == 1 ) {
            return p.y();
        }
        return p.z();
    }

    static boolean sameCoordinate(double a, double b)
    {
        return Math.abs(a - b) <= numericContext.bigEpsilon();
    }

    static boolean boundsMatch(double[] a, double[] b)
    {
        int i;

        if ( a == null || b == null || a.length < 6 || b.length < 6 ) {
            return false;
        }
        for ( i = 0; i < 6; i++ ) {
            if ( !sameCoordinate(a[i], b[i]) ) {
                return false;
            }
        }
        return true;
    }

    static void addUniqueCoordinate(ArrayList<Double> values,
                                            double value)
    {
        int i;

        for ( i = 0; i < values.size(); i++ ) {
            if ( sameCoordinate(values.get(i), value) ) {
                return;
            }
        }
        values.add(value);
        Collections.sort(values);
    }

    static ArrayList<Double> uniqueVertexCoordinates(
        PolyhedralBoundedSolid solid,
        int axis)
    {
        ArrayList<Double> values;
        int i;

        values = new ArrayList<Double>();
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            addUniqueCoordinate(values,
                coordinate(solid.getVerticesList().get(i).position, axis));
        }
        return values;
    }

    static double signedAreaOnYZ(ArrayList<Vector3Dd> profile)
    {
        double area;
        int i;

        area = 0.0;
        for ( i = 0; i < profile.size(); i++ ) {
            Vector3Dd a;
            Vector3Dd b;

            a = profile.get(i);
            b = profile.get((i + 1) % profile.size());
            area += a.y() * b.z() - b.y() * a.z();
        }
        return area * 0.5;
    }

    static ArrayList<Vector3Dd> extractProfileAtX(
        PolyhedralBoundedSolid solid,
        double x)
    {
        ArrayList<Vector3Dd> best;
        double bestArea;
        int i;

        best = null;
        bestArea = 0.0;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face;
            int j;

            face = solid.getPolygonsList().get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                ArrayList<Vector3Dd> profile;
                double area;
                int k;
                boolean onPlane;

                loop = face.boundariesList.get(j);
                if ( loop.halfEdgesList.size() < 3 ) {
                    continue;
                }
                profile = new ArrayList<Vector3Dd>();
                onPlane = true;
                for ( k = 0; k < loop.halfEdgesList.size(); k++ ) {
                    Vector3Dd p;

                    p = loop.halfEdgesList.get(k).startingVertex.position;
                    if ( !sameCoordinate(p.x(), x) ) {
                        onPlane = false;
                        break;
                    }
                    profile.add(new Vector3Dd(p));
                }
                if ( !onPlane ) {
                    continue;
                }
                area = Math.abs(signedAreaOnYZ(profile));
                if ( area > bestArea ) {
                    bestArea = area;
                    best = profile;
                }
            }
        }
        return best;
    }

    static boolean sameProfilePoint(Vector3Dd a, Vector3Dd b)
    {
        return sameCoordinate(a.x(), b.x()) &&
               sameCoordinate(a.y(), b.y()) &&
               sameCoordinate(a.z(), b.z());
    }

    static void appendProfilePoint(ArrayList<Vector3Dd> profile,
                                           Vector3Dd point)
    {
        if ( !profile.isEmpty() &&
             sameProfilePoint(profile.get(profile.size() - 1), point) ) {
            return;
        }
        profile.add(point);
    }

    static Vector3Dd projectProfilePoint(Vector3Dd point,
                                                double x,
                                                double zCut)
    {
        double z;

        z = point.z();
        if ( sameCoordinate(z, zCut) ) {
            z = zCut;
        }
        return new Vector3Dd(x, point.y(), z);
    }

    static Vector3Dd intersectProfileSegmentAtZ(Vector3Dd a,
                                                       Vector3Dd b,
                                                       double x,
                                                       double zCut)
    {
        double t;
        double y;

        if ( sameCoordinate(a.z(), b.z()) ) {
            return new Vector3Dd(x, a.y(), zCut);
        }
        t = (zCut - a.z()) / (b.z() - a.z());
        y = a.y() + (b.y() - a.y()) * t;
        return new Vector3Dd(x, y, zCut);
    }

    static ArrayList<Vector3Dd> clipProfileAboveZ(
        ArrayList<Vector3Dd> profile,
        double x,
        double zCut)
    {
        ArrayList<Vector3Dd> clipped;
        Vector3Dd previous;
        boolean previousInside;
        int i;

        clipped = new ArrayList<Vector3Dd>();
        if ( profile == null || profile.size() < 3 ) {
            return clipped;
        }

        previous = profile.get(profile.size() - 1);
        previousInside = previous.z() + numericContext.bigEpsilon() >= zCut;
        for ( i = 0; i < profile.size(); i++ ) {
            Vector3Dd current;
            boolean currentInside;

            current = profile.get(i);
            currentInside = current.z() + numericContext.bigEpsilon() >= zCut;

            if ( currentInside ) {
                if ( !previousInside ) {
                    appendProfilePoint(clipped,
                        intersectProfileSegmentAtZ(previous, current, x, zCut));
                }
                appendProfilePoint(clipped,
                    projectProfilePoint(current, x, zCut));
            }
            else if ( previousInside ) {
                appendProfilePoint(clipped,
                    intersectProfileSegmentAtZ(previous, current, x, zCut));
            }

            previous = current;
            previousInside = currentInside;
        }

        if ( clipped.size() > 1 &&
             sameProfilePoint(clipped.get(0),
                 clipped.get(clipped.size() - 1)) ) {
            clipped.remove(clipped.size() - 1);
        }
        return clipped;
    }
}
