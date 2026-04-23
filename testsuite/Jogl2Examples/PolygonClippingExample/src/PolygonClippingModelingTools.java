import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;
import vsdk.toolkit.processing.polygonClipper.WeilerAthertonPolygonClipper;
import vsdk.toolkit.processing.polygonClipper._DoubleLinkedListNode;
import vsdk.toolkit.processing.polygonClipper._Polygon2DContourWA;
import vsdk.toolkit.processing.polygonClipper._Polygon2DWA;
import vsdk.toolkit.processing.polygonClipper._VertexNode2D;

public class PolygonClippingModelingTools
{
    private static final double CLIP_Y_OFFSET = -1.0;

    public static void rebuildScene(PolygonClippingDebuggerModel model)
    {
        PolygonClippingTestCase testCase = model.getCurrentTestCase();
        WeilerAthertonPolygonClipper clipper = new WeilerAthertonPolygonClipper();

        model.setClipPolygon(buildPolygon(testCase.clipLoops(), CLIP_Y_OFFSET));
        model.setSubjectPolygon(buildPolygon(testCase.subjectLoops(), 0.0));
        model.setInnerPolygon(new Polygon2D());
        model.setOuterPolygon(new Polygon2D());

        clipper.clipPolygons(model.getClipPolygon(), model.getSubjectPolygon(),
            model.getInnerPolygon(), model.getOuterPolygon());

        model.setClipPolygonWA(clipper.getClipPolyWA());
        model.setSubjectPolygonWA(clipper.getSubjectPolyWA());
    }

    public static Vector3D calculateSceneCenter(PolygonClippingDebuggerModel model)
    {
        Bounds2D bounds = new Bounds2D();

        expandBounds(bounds, model.getClipPolygon());
        expandBounds(bounds, model.getSubjectPolygon());
        expandBounds(bounds, model.getInnerPolygon());
        expandBounds(bounds, model.getOuterPolygon());

        if ( !bounds.initialized ) {
            return new Vector3D(0, 0, 0);
        }

        double panelWidth = Math.max(1.0, bounds.maxX - bounds.minX);
        double panelDepth = Math.max(1.0, bounds.maxY - bounds.minY);
        double centerX = (bounds.minX + bounds.maxX) / 2.0;
        double centerZ = (bounds.minY + bounds.maxY) / 2.0;

        // The renderer shows the outer result on a panel translated in +X and
        // the inner result on a panel translated in -Z.
        centerX += panelWidth * 0.4;
        centerZ -= panelDepth * 0.2;

        return new Vector3D(centerX, 0, centerZ);
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

    private static Polygon2D buildPolygon(double[][] loops, double yOffset)
    {
        Polygon2D polygon = null;
        int i;
        int j;

        for ( i = 0; i < loops.length; i++ ) {
            if ( polygon == null ) {
                polygon = new Polygon2D();
            }
            else {
                polygon.nextLoop();
            }
            for ( j = 0; j < loops[i].length / 2; j++ ) {
                polygon.addVertex(loops[i][j * 2], loops[i][j * 2 + 1] + yOffset);
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
