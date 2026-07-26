package gui;

import java.util.ArrayList;
import java.util.Locale;

import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.gui.KeyEvent;

public class DebuggerKeyboardInteractionTechniques
{
    public interface Actions
    {
        void requestExit();
        void rebuildSolid();
        void exportCurrentModelStl();
        void toggleFullscreen();
        void requestScreenshot();
        void requestStlExport();
    }

    public boolean processPressed(
        TangibleInterfaceGizmosModel model,
        KeyEvent event,
        Actions actions)
    {
        boolean repaint = false;
        boolean handled = false;

        if ( event.keycode == KeyEvent.KEY_ESC ) {
            actions.requestExit();
            return false;
        }

        if ( model.getCameraController().processKeyPressedEvent(event) ) {
            repaint = true;
        }
        if ( model.getQualityController().processKeyPressedEvent(event) ) {
            System.out.println(model.getQuality());
            repaint = true;
        }

        switch ( event.keycode ) {
          case KeyEvent.KEY_v:
            model.setDebugVertices(model.notDebugVertices());
            handled = true;
            break;
          case KeyEvent.KEY_g:
            actions.toggleFullscreen();
            handled = true;
            break;
          case KeyEvent.KEY_SPACE:
            model.setShowCoordinateSystem(!model.isShowCoordinateSystem());
            handled = true;
            break;
          case KeyEvent.KEY_I:
            System.out.println(model.getSolid());
            if ( PolyhedralBoundedSolidValidationEngine
                     .validateIntermediate(model.getSolid()) ) {
                System.out.println("SOLID MODEL IS VALID!");
            }
            else {
                System.out.println("SOLID MODEL IS INVALID!");
            }
            handled = true;
            break;
          case KeyEvent.KEY_PERIOD:
            actions.requestScreenshot();
            handled = true;
            break;
          case KeyEvent.KEY_m:
            actions.requestStlExport();
            handled = true;
            break;
          case KeyEvent.KEY_1:
            model.setFaceIndex(model.getFaceIndex() - 1);
            model.clampFaceIndex();
            printSelectedFaceArea(model);
            handled = true;
            break;
          case KeyEvent.KEY_2:
            model.setFaceIndex(model.getFaceIndex() + 1);
            model.clampFaceIndex();
            printSelectedFaceArea(model);
            handled = true;
            break;
          case KeyEvent.KEY_3:
            model.setSolidModelName(model.getSolidModelName().previousClamped());
            actions.rebuildSolid();
            actions.exportCurrentModelStl();
            handled = true;
            break;
          case KeyEvent.KEY_4:
            model.setSolidModelName(model.getSolidModelName().nextClamped());
            actions.rebuildSolid();
            actions.exportCurrentModelStl();
            handled = true;
            break;
          case KeyEvent.KEY_5:
            model.decreaseInnerRadius();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_6:
            model.increaseInnerRadius();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_7:
            model.decreaseOutterRadius();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_8:
            model.increaseOutterRadius();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_9:
            model.decreaseBaseHeight();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_0:
            model.increaseBaseHeight();
            actions.rebuildSolid();
            handled = true;
            break;
          default:
            break;
        }

        model.clampFaceIndex();
        return repaint || handled;
    }

    private static void printSelectedFaceArea(TangibleInterfaceGizmosModel model)
    {
        if ( model.getSolid() == null || model.getSolid().getPolygonsList() == null ) {
            System.out.println("No solid available for face area calculation");
            return;
        }

        int faceIndex = model.getFaceIndex();
        if ( faceIndex < 0 || faceIndex >= model.getSolid().getPolygonsList().size() ) {
            System.out.println("No face selected for area calculation");
            return;
        }

        _PolyhedralBoundedSolidFace face =
            model.getSolid().getPolygonsList().get(faceIndex);
        double area = calculateFaceArea(face);
        System.out.println("Face " + face.id + " area: "
            + String.format(Locale.US, "%.8f", area));
    }

    private static double calculateFaceArea(_PolyhedralBoundedSolidFace face)
    {
        InfinitePlane plane = face.getContainingPlane();
        if ( plane == null ) {
            return 0.0;
        }

        FaceBasis basis = buildBasis(face, plane);
        Polygon2D polygon = new Polygon2D();
        polygon.loops.clear();
        ArrayList<ProjectedVertex> vertices = new ArrayList<ProjectedVertex>();

        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }

            polygon.nextLoop();
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            do {
                Vector3Dd position = he.startingVertex.position;
                double x = projectToAxis(position.subtract(basis.origin), basis.u);
                double y = projectToAxis(position.subtract(basis.origin), basis.v);
                polygon.addVertex(x, y);
                vertices.add(new ProjectedVertex(x, y));
                he = he.next();
            } while ( he != start );
        }

        if ( !polygon.loops.isEmpty() && polygon.loops.get(0).vertices.isEmpty() ) {
            polygon.eraseLastLoop();
        }
        if ( vertices.size() < 3 ) {
            return 0.0;
        }

        ArrayList<MonotoneDecompositionTriangulator.Triangle> triangles =
            new ArrayList<MonotoneDecompositionTriangulator.Triangle>();
        MonotoneDecompositionTriangulator triangulator =
            new MonotoneDecompositionTriangulator();
        triangulator.triangulate(polygon, triangles);

        double area = 0.0;
        for ( int i = 0; i < triangles.size(); i++ ) {
            MonotoneDecompositionTriangulator.Triangle triangle = triangles.get(i);
            ProjectedVertex a = vertices.get(triangle.a);
            ProjectedVertex b = vertices.get(triangle.b);
            ProjectedVertex c = vertices.get(triangle.c);
            area += Math.abs(
                (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)) * 0.5;
        }
        return area;
    }

    private static FaceBasis buildBasis(
        _PolyhedralBoundedSolidFace face,
        InfinitePlane plane)
    {
        Vector3Dd normal = plane.getNormal().normalized();
        Vector3Dd origin = chooseFaceAnchor(face);
        Vector3Dd referenceAxis = chooseReferenceAxis(normal);
        Vector3Dd u = referenceAxis.crossProduct(normal).normalized();
        Vector3Dd v = normal.crossProduct(u).normalized();
        return new FaceBasis(origin, u, v);
    }

    private static Vector3Dd chooseFaceAnchor(_PolyhedralBoundedSolidFace face)
    {
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop != null && loop.boundaryStartHalfEdge != null ) {
                return loop.boundaryStartHalfEdge.startingVertex.position;
            }
        }
        return new Vector3Dd();
    }

    private static Vector3Dd chooseReferenceAxis(Vector3Dd normal)
    {
        Vector3Dd axis = new Vector3Dd(0, 0, 1);
        if ( Math.abs(normal.dotProduct(axis)) > 0.9 ) {
            axis = new Vector3Dd(0, 1, 0);
        }
        return axis;
    }

    private static double projectToAxis(Vector3Dd vector, Vector3Dd axis)
    {
        return vector.dotProduct(axis);
    }

    private static final class FaceBasis
    {
        private final Vector3Dd origin;
        private final Vector3Dd u;
        private final Vector3Dd v;

        private FaceBasis(Vector3Dd origin, Vector3Dd u, Vector3Dd v)
        {
            this.origin = origin;
            this.u = u;
            this.v = v;
        }
    }

    private static final class ProjectedVertex
    {
        private final double x;
        private final double y;

        private ProjectedVertex(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
    }
}
