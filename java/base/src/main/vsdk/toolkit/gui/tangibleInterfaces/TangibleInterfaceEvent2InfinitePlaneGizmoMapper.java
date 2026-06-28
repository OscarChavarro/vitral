package vsdk.toolkit.gui.tangibleInterfaces;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.InfinitePlaneGizmo;

public class TangibleInterfaceEvent2InfinitePlaneGizmoMapper {
    private static final Vector3Dd MARKER_INTO_SCENE = new Vector3Dd(0, -1, 0);
    private static final Vector3Dd MARKER_PLANE_NORMAL = new Vector3Dd(0, 0, 1);
    private static final double DISTANCE_FACTOR = 4;
    private static final double MAX_GIZMO_DEPTH_FACTOR = 2.5;

    private final Camera camera;

    public TangibleInterfaceEvent2InfinitePlaneGizmoMapper(Camera camera) {
        this.camera = camera;
    }

    public void map(TangibleInterfaceEvent event, InfinitePlaneGizmo gizmo) {
        if ( event == null || gizmo == null ) {
            return;
        }

        camera.updateVectors();

        Vector3Dd netPosition = event.getPosition();
        Vector3Dd netNormal = event.getRotation().rotate(MARKER_PLANE_NORMAL);

        Vector3Dd camPos = camera.getPosition();
        double nearPlane = camera.getNearPlaneDistance();
        double farPlane = camera.getFarPlaneDistance();

        Vector3Dd camRight = camera.getLeft().multiply(-1.0);
        Vector3Dd camUp = camera.getUp();
        Vector3Dd camFront = camera.getFront();

        double midDepth = (nearPlane + farPlane) * 0.5;
        double netZRef = 0.5;
        double depthScale = midDepth / netZRef;

        double safeNetZ = Math.max(netPosition.z(), 0.05);
        double gizmoZ = Math.min(depthScale * MAX_GIZMO_DEPTH_FACTOR,
            Math.max(nearPlane * 1.5, depthScale * netZRef * netZRef / safeNetZ)) - 14;

        Vector3Dd worldPosition =
            camPos
            .add(camRight.multiply(-netPosition.x() * depthScale * DISTANCE_FACTOR))
            .add(camUp.multiply(-netPosition.y() * depthScale * DISTANCE_FACTOR))
            .add(camFront.multiply(gizmoZ));

        Vector3Dd worldNormal =
            camRight.multiply(netNormal.x())
            .add(camUp.multiply(netNormal.y()))
            .add(camFront.multiply(netNormal.z()));

        if ( worldNormal.length() < VSDK.EPSILON ) {
            worldNormal = camUp;
        }

        Vector3Dd worldRayDirection =
            mapDirection(event.getRotation().rotate(MARKER_INTO_SCENE),
                camRight, camUp, camFront);
        worldNormal = removeRayComponent(worldNormal, worldRayDirection, camUp);

        gizmo.setPlane(worldPosition, worldNormal);
    }

    private static Vector3Dd mapDirection(
        Vector3Dd netDirection,
        Vector3Dd camRight,
        Vector3Dd camUp,
        Vector3Dd camFront)
    {
        return camRight.multiply(netDirection.x())
            .add(camUp.multiply(netDirection.y()))
            .add(camFront.multiply(netDirection.z()));
    }

    private static Vector3Dd removeRayComponent(
        Vector3Dd normal,
        Vector3Dd rayDirection,
        Vector3Dd fallback)
    {
        if ( rayDirection.length() < VSDK.EPSILON ) {
            return normal.normalized();
        }
        Vector3Dd ray = rayDirection.normalized();
        Vector3Dd projected = normal.subtract(ray.multiply(normal.dotProduct(ray)));
        if ( projected.length() >= VSDK.EPSILON ) {
            return projected.normalized();
        }
        projected = fallback.subtract(ray.multiply(fallback.dotProduct(ray)));
        if ( projected.length() >= VSDK.EPSILON ) {
            return projected.normalized();
        }
        return normal.normalized();
    }
}
