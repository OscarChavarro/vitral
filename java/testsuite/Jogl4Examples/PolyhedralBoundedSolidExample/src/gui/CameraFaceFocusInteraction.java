package gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import models.DebuggerModel;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.gui.CameraControllerOrbiter;

public class CameraFaceFocusInteraction
{
    private static final double SAFETY_MARGIN = 1.05;

    public boolean focusSelectedFace(DebuggerModel model)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        int faceIndex = model.getFaceIndex();
        Camera camera = model.getCamera();

        if ( solid == null || solid.getPolygonsList() == null || camera == null ) {
            return false;
        }
        if ( faceIndex < 0 || faceIndex >= solid.getPolygonsList().size() ) {
            return false;
        }

        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(faceIndex);
        ArrayList<_PolyhedralBoundedSolidVertex> faceVertices = collectFaceVertices(face);
        if ( faceVertices.isEmpty() ) {
            return false;
        }

        Vector3Dd center = computeCentroid(faceVertices);
        InfinitePlane plane = face.getContainingPlane();
        Vector3Dd planeNormal = plane != null ? plane.getNormal() : null;
        if ( planeNormal == null || planeNormal.length() < VSDK.EPSILON ) {
            return false;
        }

        Vector3Dd front = chooseFacingFront(planeNormal, center, camera.getPosition());
        Vector3Dd up = chooseUpVector(camera.getUp(), front);
        Vector3Dd left = up.crossProduct(front).normalized();

        double distance = computeFramingDistance(camera, center, front, left, up,
            faceVertices) * SAFETY_MARGIN;
        if ( distance < VSDK.EPSILON ) {
            distance = Math.max(camera.getNearPlaneDistance() * 2.0, 1.0);
        }

        Vector3Dd eye = center.subtract(front.multiply(distance));
        camera.setPosition(eye);
        camera.setUpDirect(up);
        camera.setLeftDirect(left);
        camera.setFocusedPositionDirect(center);

        if ( model.getCameraController() instanceof CameraControllerOrbiter ) {
            ((CameraControllerOrbiter)model.getCameraController())
                .setPointOfInterest(center);
        }
        return true;
    }

    private static ArrayList<_PolyhedralBoundedSolidVertex> collectFaceVertices(
        _PolyhedralBoundedSolidFace face)
    {
        ArrayList<_PolyhedralBoundedSolidVertex> vertices =
            new ArrayList<_PolyhedralBoundedSolidVertex>();
        Set<Integer> visitedIds = new LinkedHashSet<Integer>();

        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            do {
                _PolyhedralBoundedSolidVertex v = he.startingVertex;
                if ( v != null && v.position != null && visitedIds.add(v.id) ) {
                    vertices.add(v);
                }
                he = he.next();
            } while ( he != start );
        }
        return vertices;
    }

    private static Vector3Dd computeCentroid(
        ArrayList<_PolyhedralBoundedSolidVertex> vertices)
    {
        double sx = 0.0;
        double sy = 0.0;
        double sz = 0.0;

        for ( int i = 0; i < vertices.size(); i++ ) {
            Vector3Dd p = vertices.get(i).position;
            sx += p.x();
            sy += p.y();
            sz += p.z();
        }
        double invN = 1.0 / vertices.size();
        return new Vector3Dd(sx * invN, sy * invN, sz * invN);
    }

    private static Vector3Dd chooseFacingFront(Vector3Dd normal,
        Vector3Dd center,
        Vector3Dd cameraPosition)
    {
        Vector3Dd toCamera = cameraPosition.subtract(center);
        if ( normal.dotProduct(toCamera) >= 0.0 ) {
            return normal.multiply(-1).normalized();
        }
        return normal.normalized();
    }

    private static Vector3Dd chooseUpVector(Vector3Dd upHint, Vector3Dd front)
    {
        Vector3Dd projected = upHint.subtract(front.multiply(upHint.dotProduct(front)));
        if ( projected.length() > VSDK.EPSILON ) {
            return projected.normalized();
        }

        Vector3Dd alt1 = new Vector3Dd(0, 0, 1);
        projected = alt1.subtract(front.multiply(alt1.dotProduct(front)));
        if ( projected.length() > VSDK.EPSILON ) {
            return projected.normalized();
        }

        Vector3Dd alt2 = new Vector3Dd(0, 1, 0);
        projected = alt2.subtract(front.multiply(alt2.dotProduct(front)));
        return projected.normalized();
    }

    private static double computeFramingDistance(Camera camera,
        Vector3Dd center,
        Vector3Dd front,
        Vector3Dd left,
        Vector3Dd up,
        ArrayList<_PolyhedralBoundedSolidVertex> vertices)
    {
        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            return Math.max(camera.getPosition().subtract(center).length(),
                camera.getNearPlaneDistance() * 2.0);
        }

        double viewportY = Math.max(camera.getViewportYSize(), 1e-9);
        double aspect = camera.getViewportXSize() / viewportY;
        double halfVerticalFov = Math.toRadians(camera.getFov() * 0.5);
        double tanVertical = Math.tan(halfVerticalFov);
        double halfHorizontalFov = Math.atan(Math.max(aspect, 1e-9) * tanVertical);
        double tanHorizontal = Math.tan(halfHorizontalFov);

        Vector3Dd right = left.multiply(-1);
        double requiredDistance = 0.0;

        for ( int i = 0; i < vertices.size(); i++ ) {
            Vector3Dd rel = vertices.get(i).position.subtract(center);
            double x = rel.dotProduct(right);
            double y = rel.dotProduct(up);
            double z = rel.dotProduct(front);

            double byHorizontal = Math.abs(x) / Math.max(tanHorizontal, 1e-9) - z;
            double byVertical = Math.abs(y) / Math.max(tanVertical, 1e-9) - z;
            double byNear = camera.getNearPlaneDistance() - z + 1e-6;

            requiredDistance = Math.max(requiredDistance, byHorizontal);
            requiredDistance = Math.max(requiredDistance, byVertical);
            requiredDistance = Math.max(requiredDistance, byNear);
        }

        return Math.max(requiredDistance, camera.getNearPlaneDistance() * 2.0);
    }
}
