import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;

public class PolygonTriangulation {
    private static Polygon2D readDataFile(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filename));
        List<String> tokens = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            for (String t : trimmed.split("\\s+")) {
                if (!t.isEmpty()) tokens.add(t);
            }
        }

        int idx = 0;
        int numberOfContours = Integer.parseInt(tokens.get(idx++));
        Polygon2D input = new Polygon2D();
        input.loops.clear();

        for (int c = 0; c < numberOfContours; ++c) {
            input.nextLoop();
            int numberOfPoints = Integer.parseInt(tokens.get(idx++));
            for (int i = 0; i < numberOfPoints; ++i) {
                double x = Double.parseDouble(tokens.get(idx++));
                double y = Double.parseDouble(tokens.get(idx++));
                input.addVertex(x, y);
            }
        }

        if (!input.loops.isEmpty() && input.loops.get(0).vertices.isEmpty()) {
            input.loops.remove(0);
        }
        return input;
    }

    public static void main(String[] args) throws Exception {
        String filename = args.length > 0 ? args[0] : "example1.polygon";
        Polygon2D input = readDataFile(filename);

        MonotoneDecompositionTriangulator pipeline = new MonotoneDecompositionTriangulator();
        List<MonotoneDecompositionTriangulator.Triangle> triangles = new ArrayList<>();
        int triangleCount = pipeline.triangulate(input, triangles);

        for (int i = 0; i < triangleCount; ++i) {
            MonotoneDecompositionTriangulator.Triangle triangle = triangles.get(i);
            System.out.printf("triangle #%d: %d %d %d%n", i, triangle.a, triangle.b, triangle.c);
        }
    }
}
