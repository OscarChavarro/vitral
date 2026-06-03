package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.WeilerAthertonPolygonClipper;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._DoubleLinkedListNode;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._Polygon2DContourWA;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._Polygon2DWA;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._VertexNode2D;

public class PolygonClippingModelingTools
{
    private static final double CLIP_Y_OFFSET = -1.0;
    private static final String POLYGONS_PATH = "../../../../etc/polygons/";

    public static void rebuildScene(PolygonClippingDebuggerModel model)
    {
        PolygonClippingTestCase testCase = model.getCurrentTestCase();
        WeilerAthertonPolygonClipper clipper = new WeilerAthertonPolygonClipper();
        Polygon2D operationResult = new Polygon2D();
        Polygon2D secondaryResult = new Polygon2D();
        Polygon2D scratch = new Polygon2D();

        try {
            model.setClipPolygon(buildPolygon(testCase.clipFile(), CLIP_Y_OFFSET));
            model.setSubjectPolygon(buildPolygon(testCase.subjectFile(), 0.0));
        }
        catch ( IOException e ) {
            model.setErrorState("Failed to load polygon file: " + e.getMessage());
            return;
        }
        model.setInnerPolygon(operationResult);
        model.setOuterPolygon(secondaryResult);

        switch ( model.getOperation() ) {
          case INTERSECTION:
            clipper.clipPolygons(model.getClipPolygon(), model.getSubjectPolygon(),
                operationResult, secondaryResult);
            break;
          case UNION:
            clipper.unionPolygons(model.getClipPolygon(), model.getSubjectPolygon(),
                operationResult);
            resetPolygonToEmpty(secondaryResult);
            break;
          case A_MINUS_B:
            clipper.clipPolygons(model.getSubjectPolygon(), model.getClipPolygon(),
                scratch, operationResult);
            resetPolygonToEmpty(secondaryResult);
            break;
          case B_MINUS_A:
            clipper.clipPolygons(model.getClipPolygon(), model.getSubjectPolygon(),
                scratch, operationResult);
            resetPolygonToEmpty(secondaryResult);
            break;
          default:
            clipper.clipPolygons(model.getClipPolygon(), model.getSubjectPolygon(),
                operationResult, secondaryResult);
            break;
        }

        model.setClipPolygonWA(clipper.getClipPolyWA());
        model.setSubjectPolygonWA(clipper.getSubjectPolyWA());
    }

    public static Vector3Dd calculateSceneCenter(PolygonClippingDebuggerModel model)
    {
        Bounds2D bounds = new Bounds2D();

        expandBounds(bounds, model.getClipPolygon());
        expandBounds(bounds, model.getSubjectPolygon());
        expandBounds(bounds, model.getInnerPolygon());
        expandBounds(bounds, model.getOuterPolygon());

        if ( !bounds.initialized ) {
            return new Vector3Dd(0, 0, 0);
        }

        double panelWidth = Math.max(1.0, bounds.maxX - bounds.minX);
        double panelDepth = Math.max(1.0, bounds.maxY - bounds.minY);
        double centerX = (bounds.minX + bounds.maxX) / 2.0;
        double centerZ = (bounds.minY + bounds.maxY) / 2.0;

        // The renderer shows the outer result on a panel translated in +X and
        // the inner result on a panel translated in -Z.
        centerX += panelWidth * 0.4;
        centerZ -= panelDepth * 0.2;

        return new Vector3Dd(centerX, 0, centerZ);
    }

    public static int countPairedVertices(_Polygon2DWA polygon)
    {
        int paired = 0;
        int i;
        int j;

        if ( polygon == null || polygon.loops == null ) {
            return 0;
        }

        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContourWA loop = polygon.loops.get(i);
            if ( loop.vertices == null || loop.vertices.getHead() == null ) {
                continue;
            }
            _DoubleLinkedListNode<_VertexNode2D> head = loop.vertices.getHead();
            _DoubleLinkedListNode<_VertexNode2D> cursor = head;
            j = 0;
            do {
                if ( cursor.data != null && cursor.data.pairNode != null ) {
                    paired++;
                }
                cursor = cursor.next;
                j++;
            } while ( cursor != head && j <= loop.vertices.size() + 1 );
        }

        return paired / 2;
    }

    private static Polygon2D buildPolygon(String filename, double yOffset) throws IOException
    {
        List<String> lines = Files.readAllLines(Path.of(POLYGONS_PATH + filename));
        List<String> tokens = new ArrayList<>();
        for ( String line : lines ) {
            String trimmed = line.trim();
            if ( trimmed.isEmpty() ) continue;
            for ( String t : trimmed.split("\\s+") ) {
                if ( !t.isEmpty() ) tokens.add(t);
            }
        }

        int idx = 0;
        int numberOfContours = Integer.parseInt(tokens.get(idx++));
        Polygon2D polygon = null;

        for ( int c = 0; c < numberOfContours; c++ ) {
            if ( polygon == null ) {
                polygon = new Polygon2D();
            }
            else {
                polygon.nextLoop();
            }
            int numberOfPoints = Integer.parseInt(tokens.get(idx++));
            for ( int i = 0; i < numberOfPoints; i++ ) {
                double x = Double.parseDouble(tokens.get(idx++));
                double y = Double.parseDouble(tokens.get(idx++));
                polygon.addVertex(x, y + yOffset);
            }
        }
        return polygon;
    }

    private static void expandBounds(Bounds2D bounds, Polygon2D polygon)
    {
        int i;
        int j;

        if ( polygon == null || polygon.loops == null ) {
            return;
        }

        for ( i = 0; i < polygon.loops.size(); i++ ) {
            _Polygon2DContour loop = polygon.loops.get(i);
            for ( j = 0; j < loop.vertices.size(); j++ ) {
                bounds.include(loop.vertices.get(j).x, loop.vertices.get(j).y);
            }
        }
    }

    private static void resetPolygonToEmpty(Polygon2D polygon)
    {
        polygon.loops.clear();
        polygon.nextLoop();
    }

    private static final class Bounds2D
    {
        private boolean initialized;
        private double minX;
        private double maxX;
        private double minY;
        private double maxY;

        private Bounds2D()
        {
            initialized = false;
            minX = 0.0;
            maxX = 0.0;
            minY = 0.0;
            maxY = 0.0;
        }

        private void include(double x, double y)
        {
            if ( !initialized ) {
                initialized = true;
                minX = maxX = x;
                minY = maxY = y;
                return;
            }
            if ( x < minX ) {
                minX = x;
            }
            if ( x > maxX ) {
                maxX = x;
            }
            if ( y < minY ) {
                minY = y;
            }
            if ( y > maxY ) {
                maxY = y;
            }
        }
    }
}
