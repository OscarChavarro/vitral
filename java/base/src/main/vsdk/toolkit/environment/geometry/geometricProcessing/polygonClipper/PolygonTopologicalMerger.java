package vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper;

import java.util.ArrayList;
import java.util.List;
import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;

/**
 * Post-processes polygon boolean outputs to reduce topological redundancy.
 *
 * This merger performs lightweight canonicalization:
 * - drops empty contours
 * - removes consecutive duplicate vertices
 * - removes collinear vertices
 * - removes duplicated contours (cyclic, either orientation)
 */
public class PolygonTopologicalMerger
{
    private static final double DEFAULT_EPSILON = 1E-9;

    public void mergeInPlace(Polygon2D polygon)
    {
        mergeInPlace(polygon, DEFAULT_EPSILON);
    }

    public void mergeInPlace(Polygon2D polygon, double epsilon)
    {
        int i;
        List<List<Vertex2D>> canonicalContours;
        Polygon2D merged;

        if ( polygon == null ) {
            return;
        }

        canonicalContours = new ArrayList<List<Vertex2D>>();
        for ( i = 0; i < polygon.loops.size(); ++i ) {
            _Polygon2DContour contour = polygon.loops.get(i);
            List<Vertex2D> normalized = normalizeContour(contour, epsilon);
            if ( normalized.size() < 3 ) {
                continue;
            }
            if ( containsEquivalentContour(canonicalContours, normalized, epsilon) ) {
                continue;
            }
            canonicalContours.add(normalized);
        }

        merged = new Polygon2D();
        merged.loops.clear();
        for ( i = 0; i < canonicalContours.size(); ++i ) {
            List<Vertex2D> contour = canonicalContours.get(i);
            merged.nextLoop();
            for ( Vertex2D v : contour ) {
                merged.addVertex(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
            }
        }

        if ( merged.loops.isEmpty() ) {
            merged.nextLoop();
        }

        polygon.loops = merged.loops;
    }

    private static List<Vertex2D> normalizeContour(_Polygon2DContour contour,
        double epsilon)
    {
        int i;
        List<Vertex2D> input;
        List<Vertex2D> withoutDuplicates;
        List<Vertex2D> simplified;

        input = contour.vertices;
        withoutDuplicates = new ArrayList<Vertex2D>();
        for ( i = 0; i < input.size(); ++i ) {
            Vertex2D v = input.get(i);
            if ( withoutDuplicates.isEmpty() ||
                 !samePoint(withoutDuplicates.get(withoutDuplicates.size() - 1), v, epsilon) ) {
                withoutDuplicates.add(copyVertex(v));
            }
        }

        if ( withoutDuplicates.size() > 1 &&
             samePoint(withoutDuplicates.get(0),
                 withoutDuplicates.get(withoutDuplicates.size() - 1), epsilon) ) {
            withoutDuplicates.remove(withoutDuplicates.size() - 1);
        }

        simplified = new ArrayList<Vertex2D>();
        for ( i = 0; i < withoutDuplicates.size(); ++i ) {
            Vertex2D prev = withoutDuplicates.get((i - 1 + withoutDuplicates.size()) %
                withoutDuplicates.size());
            Vertex2D cur = withoutDuplicates.get(i);
            Vertex2D next = withoutDuplicates.get((i + 1) % withoutDuplicates.size());
            if ( withoutDuplicates.size() >= 3 && areCollinear(prev, cur, next, epsilon) ) {
                continue;
            }
            simplified.add(copyVertex(cur));
        }

        return simplified;
    }

    private static boolean containsEquivalentContour(
        List<List<Vertex2D>> contours, List<Vertex2D> candidate, double epsilon)
    {
        for ( List<Vertex2D> existing : contours ) {
            if ( areEquivalentContours(existing, candidate, epsilon) ) {
                return true;
            }
        }
        return false;
    }

    private static boolean areEquivalentContours(List<Vertex2D> a,
        List<Vertex2D> b, double epsilon)
    {
        int n;
        int start;
        int i;

        if ( a.size() != b.size() ) {
            return false;
        }
        n = a.size();
        if ( n == 0 ) {
            return true;
        }

        for ( start = 0; start < n; ++start ) {
            if ( !samePoint(a.get(0), b.get(start), epsilon) ) {
                continue;
            }

            boolean forward = true;
            for ( i = 0; i < n; ++i ) {
                if ( !samePoint(a.get(i), b.get((start + i) % n), epsilon) ) {
                    forward = false;
                    break;
                }
            }
            if ( forward ) {
                return true;
            }

            boolean backward = true;
            for ( i = 0; i < n; ++i ) {
                int index = start - i;
                while ( index < 0 ) {
                    index += n;
                }
                if ( !samePoint(a.get(i), b.get(index), epsilon) ) {
                    backward = false;
                    break;
                }
            }
            if ( backward ) {
                return true;
            }
        }
        return false;
    }

    private static Vertex2D copyVertex(Vertex2D v)
    {
        return new Vertex2D(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
    }

    private static boolean samePoint(Vertex2D a, Vertex2D b, double epsilon)
    {
        return Math.abs(a.x - b.x) <= epsilon && Math.abs(a.y - b.y) <= epsilon;
    }

    private static boolean areCollinear(Vertex2D a, Vertex2D b, Vertex2D c,
        double epsilon)
    {
        double abx = b.x - a.x;
        double aby = b.y - a.y;
        double bcx = c.x - b.x;
        double bcy = c.y - b.y;
        double cross = abx * bcy - aby * bcx;
        return Math.abs(cross) <= epsilon;
    }
}
