import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import vsdk.toolkit.io.PersistenceElement;

public class StlComparisonTool
{
    private static final double EXACT_EPSILON = 0.0;
    private static final double[] EDGE_THRESHOLDS = {
        0.0, 1.0e-9, 1.0e-8, 1.0e-7, 1.0e-6, 1.0e-5
    };
    private static final double[] AREA_THRESHOLDS = {
        0.0, 1.0e-12, 1.0e-11, 1.0e-10, 1.0e-9, 1.0e-8
    };
    private static final double[] WELD_EPSILONS = {
        1.0e-9, 1.0e-8, 1.0e-7, 1.0e-6
    };

    public static void main(String[] args)
        throws Exception
    {
        if ( args.length != 2 ) {
            System.err.println("Usage: StlComparisonTool <fileA.stl> <fileB.stl>");
            System.exit(1);
        }

        ParsedStl a = read(Path.of(args[0]));
        ParsedStl b = read(Path.of(args[1]));

        System.out.println(report("A", a));
        System.out.println(report("B", b));
        System.out.println(compare(a, b));
    }

    private static ParsedStl read(Path path)
        throws Exception
    {
        try (InputStream input = Files.newInputStream(path)) {
            byte[] header = new byte[80];
            PersistenceElement.readBytes(input, header);
            long triangleCount = PersistenceElement.readLongLE(input);
            ArrayList<Facet> facets = new ArrayList<>((int)triangleCount);

            long i;
            for ( i = 0; i < triangleCount; i++ ) {
                Vec3 normal = readVector(input);
                Vec3 a = readVector(input);
                Vec3 b = readVector(input);
                Vec3 c = readVector(input);
                int attributeByteCount = PersistenceElement.readSignedShortLE(input);
                facets.add(new Facet(normal, a, b, c, attributeByteCount));
            }

            return new ParsedStl(path,
                new String(header, StandardCharsets.US_ASCII).trim(),
                triangleCount, facets);
        }
    }

    private static Vec3 readVector(InputStream input)
        throws Exception
    {
        return new Vec3(
            PersistenceElement.readFloatLE(input),
            PersistenceElement.readFloatLE(input),
            PersistenceElement.readFloatLE(input));
    }

    private static String report(String label, ParsedStl stl)
    {
        Analysis analysis = analyze(stl);
        StringBuilder out = new StringBuilder();
        out.append(label).append(": ").append(stl.path).append('\n');
        out.append("  header: ").append(stl.header).append('\n');
        out.append("  triangles: ").append(stl.triangleCount).append('\n');
        out.append("  exact unique vertices: ").append(analysis.uniqueVertices).append('\n');
        out.append("  exact unique undirected edges: ").append(analysis.uniqueEdges).append('\n');
        out.append("  boundary edges: ").append(analysis.boundaryEdges).append('\n');
        out.append("  boundary loops: ").append(analysis.boundaryLoops.size()).append('\n');
        out.append("  non-manifold edges (>2 incident triangles): ")
            .append(analysis.nonManifoldEdges).append('\n');
        appendWeldedTopology(out, stl.facets);
        out.append("  duplicate triangles (same 3 vertices): ")
            .append(analysis.duplicateTriangles).append('\n');
        out.append("  inverted duplicates (same triangle opposite winding): ")
            .append(analysis.invertedDuplicateTriangles).append('\n');
        out.append("  degenerate normals (|n| == 0): ")
            .append(analysis.zeroNormals).append('\n');
        appendThresholds(out, "min edge length", analysis.edgeBelowThresholds,
            EDGE_THRESHOLDS);
        appendThresholds(out, "double area", analysis.areaBelowThresholds,
            AREA_THRESHOLDS);
        out.append(String.format(Locale.US, "  min edge length: %.12g%n",
            analysis.minEdgeLength));
        out.append(String.format(Locale.US, "  min double area: %.12g%n",
            analysis.minDoubleArea));
        out.append(String.format(Locale.US, "  bbox min: (%.9g, %.9g, %.9g)%n",
            analysis.min.x, analysis.min.y, analysis.min.z));
        out.append(String.format(Locale.US, "  bbox max: (%.9g, %.9g, %.9g)%n",
            analysis.max.x, analysis.max.y, analysis.max.z));
        appendBoundaryLoopSamples(out, analysis.boundaryLoops);
        return out.toString();
    }

    private static void appendThresholds(StringBuilder out,
                                         String label,
                                         long[] counts,
                                         double[] thresholds)
    {
        int i;
        out.append("  ").append(label).append(" counts:");
        for ( i = 0; i < thresholds.length; i++ ) {
            out.append(' ')
                .append("<=")
                .append(formatNumber(thresholds[i]))
                .append(':')
                .append(counts[i]);
        }
        out.append('\n');
    }

    private static String compare(ParsedStl a, ParsedStl b)
    {
        Set<String> aTriangles = buildCanonicalTriangleSet(a.facets);
        Set<String> bTriangles = buildCanonicalTriangleSet(b.facets);

        int shared = 0;
        for ( String triangle : aTriangles ) {
            if ( bTriangles.contains(triangle) ) {
                shared++;
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("Comparison\n");
        out.append("  shared canonical triangles: ").append(shared).append('\n');
        out.append("  triangles only in A: ").append(aTriangles.size() - shared)
            .append('\n');
        out.append("  triangles only in B: ").append(bTriangles.size() - shared)
            .append('\n');
        return out.toString();
    }

    private static void appendBoundaryLoopSamples(StringBuilder out,
                                                  List<BoundaryLoop> boundaryLoops)
    {
        int limit = Math.min(3, boundaryLoops.size());
        int i;
        for ( i = 0; i < limit; i++ ) {
            BoundaryLoop loop = boundaryLoops.get(i);
            out.append("  boundary loop ").append(i + 1)
                .append(": edges=").append(loop.edgeCount)
                .append(" approx perimeter=")
                .append(String.format(Locale.US, "%.9g", loop.perimeter))
                .append(" start=")
                .append(loop.start.key())
                .append('\n');
        }
    }

    private static void appendWeldedTopology(StringBuilder out,
                                             List<Facet> facets)
    {
        int i;
        for ( i = 0; i < WELD_EPSILONS.length; i++ ) {
            double epsilon = WELD_EPSILONS[i];
            WeldedTopology topology = analyzeWeldedTopology(facets, epsilon);
            out.append("  welded@")
                .append(formatNumber(epsilon))
                .append(": uniqueVertices=")
                .append(topology.uniqueVertices)
                .append(" boundaryEdges=")
                .append(topology.boundaryEdges)
                .append(" nonManifoldEdges=")
                .append(topology.nonManifoldEdges)
                .append('\n');
        }
    }

    private static Set<String> buildCanonicalTriangleSet(ArrayList<Facet> facets)
    {
        HashSet<String> out = new HashSet<>();
        int i;
        for ( i = 0; i < facets.size(); i++ ) {
            out.add(facets.get(i).canonicalTriangleKey());
        }
        return out;
    }

    private static Analysis analyze(ParsedStl stl)
    {
        Analysis analysis = new Analysis();
        analysis.min = new Vec3(Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        analysis.max = new Vec3(Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        Map<String, Integer> edgeUseCount = new HashMap<>();
        Map<String, EdgeEndpoints> edgeEndpoints = new HashMap<>();
        Map<String, Integer> triangleUseCount = new HashMap<>();
        Map<String, Integer> orientedTriangleUseCount = new HashMap<>();
        Set<String> uniqueVertices = new HashSet<>();

        analysis.minEdgeLength = Double.POSITIVE_INFINITY;
        analysis.minDoubleArea = Double.POSITIVE_INFINITY;

        int i;
        for ( i = 0; i < stl.facets.size(); i++ ) {
            Facet facet = stl.facets.get(i);
            updateBounds(analysis, facet.a);
            updateBounds(analysis, facet.b);
            updateBounds(analysis, facet.c);

            uniqueVertices.add(facet.a.key());
            uniqueVertices.add(facet.b.key());
            uniqueVertices.add(facet.c.key());

            if ( facet.normal.length() <= EXACT_EPSILON ) {
                analysis.zeroNormals++;
            }

            double ab = facet.a.distance(facet.b);
            double bc = facet.b.distance(facet.c);
            double ca = facet.c.distance(facet.a);
            double minEdge = Math.min(ab, Math.min(bc, ca));
            analysis.minEdgeLength = Math.min(analysis.minEdgeLength, minEdge);
            incrementThresholds(analysis.edgeBelowThresholds, EDGE_THRESHOLDS,
                minEdge);

            Vec3 cross = facet.b.subtract(facet.a).cross(facet.c.subtract(facet.a));
            double doubleArea = cross.length();
            analysis.minDoubleArea = Math.min(analysis.minDoubleArea, doubleArea);
            incrementThresholds(analysis.areaBelowThresholds, AREA_THRESHOLDS,
                doubleArea);

            registerEdge(edgeUseCount, edgeEndpoints, facet.a, facet.b);
            registerEdge(edgeUseCount, edgeEndpoints, facet.b, facet.c);
            registerEdge(edgeUseCount, edgeEndpoints, facet.c, facet.a);
            increment(triangleUseCount, facet.canonicalTriangleKey());
            increment(orientedTriangleUseCount, facet.orientedTriangleKey());
        }

        for ( int uses : edgeUseCount.values() ) {
            if ( uses == 1 ) {
                analysis.boundaryEdges++;
            }
            else if ( uses > 2 ) {
                analysis.nonManifoldEdges++;
            }
        }

        analysis.boundaryLoops = buildBoundaryLoops(edgeUseCount, edgeEndpoints);
        Collections.sort(analysis.boundaryLoops,
            (left, right) -> Integer.compare(right.edgeCount, left.edgeCount));

        for ( Map.Entry<String, Integer> entry : triangleUseCount.entrySet() ) {
            if ( entry.getValue() > 1 ) {
                analysis.duplicateTriangles += entry.getValue() - 1;
            }
        }

        for ( Map.Entry<String, Integer> entry : triangleUseCount.entrySet() ) {
            int uses = entry.getValue();
            if ( uses < 2 ) {
                continue;
            }
            String canonical = entry.getKey();
            String[] variants = Facet.orientedKeysFromCanonical(canonical);
            int first = orientedTriangleUseCount.getOrDefault(variants[0], 0);
            int second = orientedTriangleUseCount.getOrDefault(variants[1], 0);
            if ( first > 0 && second > 0 ) {
                analysis.invertedDuplicateTriangles += Math.min(first, second);
            }
        }

        analysis.uniqueVertices = uniqueVertices.size();
        analysis.uniqueEdges = edgeUseCount.size();
        return analysis;
    }

    private static WeldedTopology analyzeWeldedTopology(List<Facet> facets,
                                                        double epsilon)
    {
        ArrayList<Vec3> representatives = new ArrayList<>();
        HashMap<String, Integer> edgeUseCount = new HashMap<>();

        int i;
        for ( i = 0; i < facets.size(); i++ ) {
            Facet facet = facets.get(i);
            int a = weldVertex(representatives, facet.a, epsilon);
            int b = weldVertex(representatives, facet.b, epsilon);
            int c = weldVertex(representatives, facet.c, epsilon);

            increment(edgeUseCount, weldedEdgeKey(a, b));
            increment(edgeUseCount, weldedEdgeKey(b, c));
            increment(edgeUseCount, weldedEdgeKey(c, a));
        }

        WeldedTopology topology = new WeldedTopology();
        topology.uniqueVertices = representatives.size();
        for ( int uses : edgeUseCount.values() ) {
            if ( uses == 1 ) {
                topology.boundaryEdges++;
            }
            else if ( uses > 2 ) {
                topology.nonManifoldEdges++;
            }
        }
        return topology;
    }

    private static int weldVertex(ArrayList<Vec3> representatives,
                                  Vec3 candidate,
                                  double epsilon)
    {
        int i;
        for ( i = 0; i < representatives.size(); i++ ) {
            if ( representatives.get(i).distance(candidate) <= epsilon ) {
                return i;
            }
        }
        representatives.add(candidate);
        return representatives.size() - 1;
    }

    private static String weldedEdgeKey(int a, int b)
    {
        if ( a <= b ) {
            return a + "|" + b;
        }
        return b + "|" + a;
    }

    private static void updateBounds(Analysis analysis, Vec3 p)
    {
        analysis.min = new Vec3(
            Math.min(analysis.min.x, p.x),
            Math.min(analysis.min.y, p.y),
            Math.min(analysis.min.z, p.z));
        analysis.max = new Vec3(
            Math.max(analysis.max.x, p.x),
            Math.max(analysis.max.y, p.y),
            Math.max(analysis.max.z, p.z));
    }

    private static void incrementThresholds(long[] counts, double[] thresholds,
                                            double value)
    {
        int i;
        for ( i = 0; i < thresholds.length; i++ ) {
            if ( value <= thresholds[i] ) {
                counts[i]++;
            }
        }
    }

    private static void increment(Map<String, Integer> counter, String key)
    {
        counter.put(key, counter.getOrDefault(key, 0) + 1);
    }

    private static void registerEdge(Map<String, Integer> edgeUseCount,
                                     Map<String, EdgeEndpoints> edgeEndpoints,
                                     Vec3 a,
                                     Vec3 b)
    {
        String key = Facet.edgeKey(a, b);
        increment(edgeUseCount, key);
        edgeEndpoints.putIfAbsent(key, new EdgeEndpoints(a, b));
    }

    private static List<BoundaryLoop> buildBoundaryLoops(
        Map<String, Integer> edgeUseCount,
        Map<String, EdgeEndpoints> edgeEndpoints)
    {
        HashMap<String, ArrayList<String>> adjacency = new HashMap<>();
        Set<String> boundaryEdgeKeys = new HashSet<>();

        for ( Map.Entry<String, Integer> entry : edgeUseCount.entrySet() ) {
            if ( entry.getValue() != 1 ) {
                continue;
            }
            String edgeKey = entry.getKey();
            EdgeEndpoints endpoints = edgeEndpoints.get(edgeKey);
            boundaryEdgeKeys.add(edgeKey);
            addAdjacency(adjacency, endpoints.a.key(), edgeKey);
            addAdjacency(adjacency, endpoints.b.key(), edgeKey);
        }

        ArrayList<BoundaryLoop> loops = new ArrayList<>();
        Set<String> visitedEdges = new HashSet<>();
        for ( String startEdge : boundaryEdgeKeys ) {
            if ( visitedEdges.contains(startEdge) ) {
                continue;
            }
            BoundaryLoop loop = traceBoundaryComponent(startEdge, edgeEndpoints,
                adjacency, visitedEdges);
            loops.add(loop);
        }
        return loops;
    }

    private static void addAdjacency(Map<String, ArrayList<String>> adjacency,
                                     String vertexKey,
                                     String edgeKey)
    {
        adjacency.computeIfAbsent(vertexKey, unused -> new ArrayList<>()).add(edgeKey);
    }

    private static BoundaryLoop traceBoundaryComponent(
        String startEdge,
        Map<String, EdgeEndpoints> edgeEndpoints,
        Map<String, ArrayList<String>> adjacency,
        Set<String> visitedEdges)
    {
        ArrayList<String> queue = new ArrayList<>();
        queue.add(startEdge);
        visitedEdges.add(startEdge);

        int edgeCount = 0;
        double perimeter = 0.0;
        Vec3 start = edgeEndpoints.get(startEdge).a;

        while ( !queue.isEmpty() ) {
            String edgeKey = queue.remove(queue.size() - 1);
            EdgeEndpoints endpoints = edgeEndpoints.get(edgeKey);
            edgeCount++;
            perimeter += endpoints.a.distance(endpoints.b);

            enqueueAdjacentEdges(endpoints.a.key(), adjacency, visitedEdges, queue);
            enqueueAdjacentEdges(endpoints.b.key(), adjacency, visitedEdges, queue);
        }

        return new BoundaryLoop(edgeCount, perimeter, start);
    }

    private static void enqueueAdjacentEdges(String vertexKey,
                                             Map<String, ArrayList<String>> adjacency,
                                             Set<String> visitedEdges,
                                             ArrayList<String> queue)
    {
        ArrayList<String> incident = adjacency.get(vertexKey);
        if ( incident == null ) {
            return;
        }
        int i;
        for ( i = 0; i < incident.size(); i++ ) {
            String edgeKey = incident.get(i);
            if ( visitedEdges.add(edgeKey) ) {
                queue.add(edgeKey);
            }
        }
    }

    private static String formatNumber(double value)
    {
        if ( value == 0.0 ) {
            return "0";
        }
        return String.format(Locale.US, "%.0e", value);
    }

    private static final class Analysis {
        int uniqueVertices;
        int uniqueEdges;
        int boundaryEdges;
        int nonManifoldEdges;
        int duplicateTriangles;
        int invertedDuplicateTriangles;
        int zeroNormals;
        double minEdgeLength;
        double minDoubleArea;
        Vec3 min;
        Vec3 max;
        List<BoundaryLoop> boundaryLoops = List.of();
        final long[] edgeBelowThresholds = new long[EDGE_THRESHOLDS.length];
        final long[] areaBelowThresholds = new long[AREA_THRESHOLDS.length];
    }

    private static final class BoundaryLoop {
        final int edgeCount;
        final double perimeter;
        final Vec3 start;

        BoundaryLoop(int edgeCount, double perimeter, Vec3 start)
        {
            this.edgeCount = edgeCount;
            this.perimeter = perimeter;
            this.start = start;
        }
    }

    private static final class WeldedTopology {
        int uniqueVertices;
        int boundaryEdges;
        int nonManifoldEdges;
    }

    private static final class EdgeEndpoints {
        final Vec3 a;
        final Vec3 b;

        EdgeEndpoints(Vec3 a, Vec3 b)
        {
            this.a = a;
            this.b = b;
        }
    }

    private static final class ParsedStl {
        final Path path;
        final String header;
        final long triangleCount;
        final ArrayList<Facet> facets;

        ParsedStl(Path path, String header, long triangleCount,
                  ArrayList<Facet> facets)
        {
            this.path = path;
            this.header = header;
            this.triangleCount = triangleCount;
            this.facets = facets;
        }
    }

    private static final class Facet {
        final Vec3 normal;
        final Vec3 a;
        final Vec3 b;
        final Vec3 c;
        final int attributeByteCount;

        Facet(Vec3 normal, Vec3 a, Vec3 b, Vec3 c, int attributeByteCount)
        {
            this.normal = normal;
            this.a = a;
            this.b = b;
            this.c = c;
            this.attributeByteCount = attributeByteCount;
        }

        String canonicalTriangleKey()
        {
            return sortedKey(a.key(), b.key(), c.key());
        }

        String orientedTriangleKey()
        {
            return a.key() + ">" + b.key() + ">" + c.key();
        }

        static String edgeKey(Vec3 p, Vec3 q)
        {
            if ( p.key().compareTo(q.key()) <= 0 ) {
                return p.key() + "|" + q.key();
            }
            return q.key() + "|" + p.key();
        }

        static String[] orientedKeysFromCanonical(String canonical)
        {
            String[] parts = canonical.split("\\|");
            return new String[] {
                parts[0] + ">" + parts[1] + ">" + parts[2],
                parts[0] + ">" + parts[2] + ">" + parts[1]
            };
        }

        private static String sortedKey(String x, String y, String z)
        {
            String[] values = {x, y, z};
            int i;
            for ( i = 0; i < values.length - 1; i++ ) {
                int j;
                for ( j = i + 1; j < values.length; j++ ) {
                    if ( values[j].compareTo(values[i]) < 0 ) {
                        String tmp = values[i];
                        values[i] = values[j];
                        values[j] = tmp;
                    }
                }
            }
            return values[0] + "|" + values[1] + "|" + values[2];
        }
    }

    private static final class Vec3 {
        final double x;
        final double y;
        final double z;

        Vec3(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Vec3 subtract(Vec3 other)
        {
            return new Vec3(x - other.x, y - other.y, z - other.z);
        }

        Vec3 cross(Vec3 other)
        {
            return new Vec3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
        }

        double length()
        {
            return Math.sqrt(x * x + y * y + z * z);
        }

        double distance(Vec3 other)
        {
            return subtract(other).length();
        }

        String key()
        {
            return String.format(Locale.US, "%.9g,%.9g,%.9g", x, y, z);
        }
    }
}
