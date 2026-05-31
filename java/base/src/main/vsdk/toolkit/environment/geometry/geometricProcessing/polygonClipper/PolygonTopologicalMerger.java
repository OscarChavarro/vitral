package vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        canonicalContours = weldInternalEdges(canonicalContours, epsilon);

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

    private static List<List<Vertex2D>> weldInternalEdges(
        List<List<Vertex2D>> contours, double epsilon)
    {
        int i;
        int j;
        List<Segment2D> segments;
        Map<EdgeKey, Integer> signedUsage;
        List<DirectedEdge> boundaryEdges;

        segments = new ArrayList<Segment2D>();
        for ( i = 0; i < contours.size(); ++i ) {
            List<Vertex2D> contour = contours.get(i);
            for ( j = 0; j < contour.size(); ++j ) {
                Vertex2D a = contour.get(j);
                Vertex2D b = contour.get((j + 1) % contour.size());
                if ( samePoint(a, b, epsilon) ) {
                    continue;
                }
                segments.add(new Segment2D(copyVertex(a), copyVertex(b)));
            }
        }
        if ( segments.isEmpty() ) {
            return contours;
        }

        signedUsage = new HashMap<EdgeKey, Integer>();
        splitAndAccumulateSegments(segments, epsilon, signedUsage);

        boundaryEdges = new ArrayList<DirectedEdge>();
        for ( Map.Entry<EdgeKey, Integer> e : signedUsage.entrySet() ) {
            int balance = e.getValue().intValue();
            if ( balance == 0 ) {
                continue;
            }
            if ( balance > 0 ) {
                boundaryEdges.add(new DirectedEdge(e.getKey().a, e.getKey().b));
            } else {
                boundaryEdges.add(new DirectedEdge(e.getKey().b, e.getKey().a));
            }
        }
        if ( boundaryEdges.isEmpty() ) {
            return new ArrayList<List<Vertex2D>>();
        }

        return extractLoopsFromBoundaryEdges(boundaryEdges, epsilon);
    }

    private static void splitAndAccumulateSegments(List<Segment2D> segments,
        double epsilon, Map<EdgeKey, Integer> signedUsage)
    {
        int i;
        int j;
        for ( i = 0; i < segments.size(); ++i ) {
            Segment2D seg = segments.get(i);
            List<SplitPoint> splitPoints = new ArrayList<SplitPoint>();

            splitPoints.add(new SplitPoint(0.0, seg.a));
            splitPoints.add(new SplitPoint(1.0, seg.b));
            for ( j = 0; j < segments.size(); ++j ) {
                Segment2D other = segments.get(j);
                maybeAddPointOnSegment(seg, other.a, epsilon, splitPoints);
                maybeAddPointOnSegment(seg, other.b, epsilon, splitPoints);
            }

            Collections.sort(splitPoints);
            List<SplitPoint> dedup = dedupSplitPoints(splitPoints, epsilon);
            for ( j = 0; j + 1 < dedup.size(); ++j ) {
                Vertex2D p0 = dedup.get(j).p;
                Vertex2D p1 = dedup.get(j + 1).p;
                if ( samePoint(p0, p1, epsilon) ) {
                    continue;
                }
                EdgeKey key = new EdgeKey(p0, p1, epsilon);
                int delta = key.isForward(p0, epsilon) ? 1 : -1;
                Integer current = signedUsage.get(key);
                signedUsage.put(key, Integer.valueOf((current == null ? 0 : current.intValue()) + delta));
            }
        }
    }

    private static void maybeAddPointOnSegment(Segment2D seg, Vertex2D p,
        double epsilon, List<SplitPoint> out)
    {
        double t = parameterOnSegment(seg, p, epsilon);
        if ( t < -0.5 ) {
            return;
        }
        out.add(new SplitPoint(t, copyVertex(p)));
    }

    private static double parameterOnSegment(Segment2D seg, Vertex2D p,
        double epsilon)
    {
        double dx = seg.b.x - seg.a.x;
        double dy = seg.b.y - seg.a.y;
        double len2 = dx * dx + dy * dy;
        double t;
        double projx;
        double projy;
        double dist2;

        if ( len2 <= epsilon * epsilon ) {
            return -1.0;
        }
        t = ((p.x - seg.a.x) * dx + (p.y - seg.a.y) * dy) / len2;
        if ( t < -epsilon || t > 1.0 + epsilon ) {
            return -1.0;
        }
        if ( t < 0.0 ) {
            t = 0.0;
        } else if ( t > 1.0 ) {
            t = 1.0;
        }
        projx = seg.a.x + t * dx;
        projy = seg.a.y + t * dy;
        dist2 = (p.x - projx) * (p.x - projx) + (p.y - projy) * (p.y - projy);
        if ( dist2 > epsilon * epsilon ) {
            return -1.0;
        }
        return t;
    }

    private static List<SplitPoint> dedupSplitPoints(List<SplitPoint> in,
        double epsilon)
    {
        int i;
        List<SplitPoint> out = new ArrayList<SplitPoint>();
        for ( i = 0; i < in.size(); ++i ) {
            SplitPoint cur = in.get(i);
            if ( out.isEmpty() ||
                 Math.abs(out.get(out.size() - 1).t - cur.t) > epsilon ||
                 !samePoint(out.get(out.size() - 1).p, cur.p, epsilon) ) {
                out.add(cur);
            }
        }
        return out;
    }

    private static List<List<Vertex2D>> extractLoopsFromBoundaryEdges(
        List<DirectedEdge> edges, double epsilon)
    {
        int i;
        Map<PointKey, List<Integer>> outgoing = new HashMap<PointKey, List<Integer>>();
        Set<Integer> used = new HashSet<Integer>();
        List<List<Vertex2D>> loops = new ArrayList<List<Vertex2D>>();

        for ( i = 0; i < edges.size(); ++i ) {
            PointKey key = new PointKey(edges.get(i).start, epsilon);
            if ( !outgoing.containsKey(key) ) {
                outgoing.put(key, new ArrayList<Integer>());
            }
            outgoing.get(key).add(Integer.valueOf(i));
        }

        for ( i = 0; i < edges.size(); ++i ) {
            if ( used.contains(Integer.valueOf(i)) ) {
                continue;
            }
            List<Vertex2D> loop = traceLoop(edges, i, outgoing, used, epsilon);
            if ( loop.size() >= 3 ) {
                loops.add(loop);
            }
        }
        return loops;
    }

    private static List<Vertex2D> traceLoop(List<DirectedEdge> edges,
        int startEdgeIndex, Map<PointKey, List<Integer>> outgoing,
        Set<Integer> used, double epsilon)
    {
        List<Vertex2D> loop = new ArrayList<Vertex2D>();
        DirectedEdge startEdge = edges.get(startEdgeIndex);
        Vertex2D start = startEdge.start;
        Vertex2D current = startEdge.end;
        double prevDx = startEdge.end.x - startEdge.start.x;
        double prevDy = startEdge.end.y - startEdge.start.y;
        int guard = edges.size() * 2 + 4;
        int currentEdge = startEdgeIndex;

        used.add(Integer.valueOf(startEdgeIndex));
        loop.add(copyVertex(start));
        loop.add(copyVertex(current));

        while ( !samePoint(current, start, epsilon) && guard > 0 ) {
            PointKey key = new PointKey(current, epsilon);
            List<Integer> candidates = outgoing.get(key);
            int nextEdge = -1;
            if ( candidates != null ) {
                nextEdge = chooseNextEdge(edges, candidates, used, prevDx, prevDy);
            }
            if ( nextEdge < 0 ) {
                break;
            }
            used.add(Integer.valueOf(nextEdge));
            currentEdge = nextEdge;
            DirectedEdge e = edges.get(currentEdge);
            prevDx = e.end.x - e.start.x;
            prevDy = e.end.y - e.start.y;
            current = e.end;
            if ( !samePoint(current, start, epsilon) ) {
                loop.add(copyVertex(current));
            }
            guard--;
        }
        if ( loop.size() > 1 &&
             samePoint(loop.get(0), loop.get(loop.size() - 1), epsilon) ) {
            loop.remove(loop.size() - 1);
        }
        return loop;
    }

    private static int chooseNextEdge(List<DirectedEdge> edges,
        List<Integer> candidates, Set<Integer> used, double prevDx,
        double prevDy)
    {
        int i;
        int selected = -1;
        double bestAngle = Double.MAX_VALUE;

        for ( i = 0; i < candidates.size(); ++i ) {
            int idx = candidates.get(i).intValue();
            if ( used.contains(Integer.valueOf(idx)) ) {
                continue;
            }
            DirectedEdge e = edges.get(idx);
            double dx = e.end.x - e.start.x;
            double dy = e.end.y - e.start.y;
            double cross = prevDx * dy - prevDy * dx;
            double dot = prevDx * dx + prevDy * dy;
            double angle = Math.atan2(cross, dot);
            if ( angle <= 0.0 ) {
                angle += Math.PI * 2.0;
            }
            if ( angle < bestAngle ) {
                bestAngle = angle;
                selected = idx;
            }
        }
        return selected;
    }

    private static class Segment2D
    {
        final Vertex2D a;
        final Vertex2D b;

        Segment2D(Vertex2D a, Vertex2D b)
        {
            this.a = a;
            this.b = b;
        }
    }

    private static class SplitPoint implements Comparable<SplitPoint>
    {
        final double t;
        final Vertex2D p;

        SplitPoint(double t, Vertex2D p)
        {
            this.t = t;
            this.p = p;
        }

        @Override
        public int compareTo(SplitPoint other)
        {
            return Double.compare(t, other.t);
        }
    }

    private static class DirectedEdge
    {
        final Vertex2D start;
        final Vertex2D end;

        DirectedEdge(Vertex2D start, Vertex2D end)
        {
            this.start = start;
            this.end = end;
        }
    }

    private static class PointKey
    {
        final long qx;
        final long qy;

        PointKey(Vertex2D p, double epsilon)
        {
            qx = Math.round(p.x / epsilon);
            qy = Math.round(p.y / epsilon);
        }

        @Override
        public int hashCode()
        {
            int h = 17;
            h = 31 * h + Long.hashCode(qx);
            h = 31 * h + Long.hashCode(qy);
            return h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if ( this == obj ) {
                return true;
            }
            if ( !(obj instanceof PointKey) ) {
                return false;
            }
            PointKey other = (PointKey) obj;
            return qx == other.qx && qy == other.qy;
        }
    }

    private static class EdgeKey
    {
        final Vertex2D a;
        final Vertex2D b;
        final PointKey ka;
        final PointKey kb;

        EdgeKey(Vertex2D p0, Vertex2D p1, double epsilon)
        {
            PointKey k0 = new PointKey(p0, epsilon);
            PointKey k1 = new PointKey(p1, epsilon);
            if ( compareKeys(k0, k1) <= 0 ) {
                a = copyVertex(p0);
                b = copyVertex(p1);
                ka = k0;
                kb = k1;
            } else {
                a = copyVertex(p1);
                b = copyVertex(p0);
                ka = k1;
                kb = k0;
            }
        }

        boolean isForward(Vertex2D start, double epsilon)
        {
            return samePoint(a, start, epsilon);
        }

        @Override
        public int hashCode()
        {
            int h = 17;
            h = 31 * h + ka.hashCode();
            h = 31 * h + kb.hashCode();
            return h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if ( this == obj ) {
                return true;
            }
            if ( !(obj instanceof EdgeKey) ) {
                return false;
            }
            EdgeKey other = (EdgeKey) obj;
            return ka.equals(other.ka) && kb.equals(other.kb);
        }

        private static int compareKeys(PointKey a, PointKey b)
        {
            if ( a.qx < b.qx ) {
                return -1;
            }
            if ( a.qx > b.qx ) {
                return 1;
            }
            if ( a.qy < b.qy ) {
                return -1;
            }
            if ( a.qy > b.qy ) {
                return 1;
            }
            return 0;
        }
    }
}
