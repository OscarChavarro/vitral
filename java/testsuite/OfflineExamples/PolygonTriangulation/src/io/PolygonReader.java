package io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;

public class PolygonReader
{
    public Polygon2D read(String fileName) throws IOException
    {
        List<String> lines = Files.readAllLines(Path.of(fileName));
        List<String> tokens = new ArrayList<>();

        for ( String line : lines ) {
            String trimmedLine = line.trim();
            if ( trimmedLine.isEmpty() ) {
                continue;
            }
            for ( String token : trimmedLine.split("\\s+") ) {
                if ( !token.isEmpty() ) {
                    tokens.add(token);
                }
            }
        }

        int tokenIndex = 0;
        int contourCount = Integer.parseInt(tokens.get(tokenIndex++));
        Polygon2D polygon = new Polygon2D();
        polygon.loops.clear();

        for ( int contourIndex = 0; contourIndex < contourCount; contourIndex++ ) {
            polygon.nextLoop();
            int pointCount = Integer.parseInt(tokens.get(tokenIndex++));
            for ( int pointIndex = 0; pointIndex < pointCount; pointIndex++ ) {
                double x = Double.parseDouble(tokens.get(tokenIndex++));
                double y = Double.parseDouble(tokens.get(tokenIndex++));
                polygon.addVertex(x, y);
            }
        }

        if ( !polygon.loops.isEmpty() && polygon.loops.get(0).vertices.isEmpty() ) {
            polygon.loops.remove(0);
        }

        return polygon;
    }
}
