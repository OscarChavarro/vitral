package vsdk.toolkit.gui.tangibleInterfaces;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.gui.gizmo.RayGizmo;

/**
Converts a {@link TangibleInterfaceEvent} pose into a {@link RayGizmo} position and direction.

Coordinate-space mapping:
  The TangibleInterfaceMarkersDetectorServer delivers poses in the reference
  frame of the physical (real) camera.  We want to achieve a mirror-like
  correspondence: a marker moved in front of the real camera should move the
  gizmo in the same direction as seen from the virtual camera.

  Mapping (tangible → virtual-world):
    tangible +X  →  virtual camera right  (-camera.getLeft())
    tangible +Y  →  virtual camera up     ( camera.getUp())
    tangible +Z  →  virtual camera front  ( camera.getFront())

  world_position = camera.getPosition()
                 + right  * (-net_position.x)        [X mirrored]
                 + up     * (-net_position.y)        [Y mirrored]
                 + front  * (netZRef²/net_position.z) [Z inverted: close→far]

  Depth inversion: when the physical marker is CLOSE to the real camera the
  gizmo moves AWAY from the virtual camera (and vice versa), using a reciprocal
  mapping anchored at netZRef so that at z=netZRef/2 the gizmo sits at double
  the mid-frustum depth.

  MARKER_INTO_SCENE in group frame:
    The rayCube group frame has its +Y axis pointing toward the camera when
    the front/back faces (marker ids 11/14) are visible.  Rotating group -Y
    by the camera-group quaternion gives a direction pointing INTO the virtual
    scene.  Using -Y also preserves direction stability under cube spin: spinning
    the cube around its pointing axis (group Y) leaves -Y invariant.
*/
public class TangibleInterfaceEvent2RayGizmoMapper {
    private static final Vector3Dd MARKER_INTO_SCENE = new Vector3Dd(0, -1, 0);
    private static final double DISTANCE_FACTOR = 4;
    private static final double MAX_GIZMO_DEPTH_FACTOR = 2.5;

    private final Camera camera;

    public TangibleInterfaceEvent2RayGizmoMapper(Camera camera) {
        this.camera = camera;
    }

    public void map(TangibleInterfaceEvent event, RayGizmo gizmo) {
        camera.updateVectors();

        Vector3Dd netPosition  = event.getPosition();
        Vector3Dd netDirection = event.getRotation().rotate(MARKER_INTO_SCENE);

        Vector3Dd camPos   = camera.getPosition();
        double nearPlane   = camera.getNearPlaneDistance();
        double farPlane    = camera.getFarPlaneDistance();

        Vector3Dd camRight = camera.getLeft().multiply(-1.0);
        Vector3Dd camUp    = camera.getUp();
        Vector3Dd camFront = camera.getFront();

        double midDepth   = (nearPlane + farPlane) * 0.5;
        double netZRef    = 0.5;
        double depthScale = midDepth / netZRef;

        double safeNetZ = Math.max(netPosition.z(), 0.05);
        double gizmoZ   = Math.min(depthScale * MAX_GIZMO_DEPTH_FACTOR,
                          Math.max(nearPlane * 1.5, depthScale * netZRef * netZRef / safeNetZ)) - 14;

        Vector3Dd worldPosition =
            camPos
            .add(camRight.multiply(-netPosition.x() * depthScale * DISTANCE_FACTOR))
            .add(camUp   .multiply(-netPosition.y() * depthScale * DISTANCE_FACTOR))
            .add(camFront.multiply(gizmoZ));

        Vector3Dd worldDirection =
            camRight.multiply(netDirection.x())
            .add(camUp   .multiply(netDirection.y()))
            .add(camFront.multiply(netDirection.z()));

        double rollAngle = computeRollAngle(
            event.getRotation(), worldDirection, camRight, camUp, camFront);

        gizmo.setRay(new Ray(worldPosition, worldDirection), rollAngle);
    }

    private static double computeRollAngle(
        Quaterniond rotation,
        Vector3Dd worldDirection,
        Vector3Dd camRight,
        Vector3Dd camUp,
        Vector3Dd camFront)
    {
        Vector3Dd netCubeUp = rotation.rotate(new Vector3Dd(0, 0, 1));
        Vector3Dd worldCubeUp = camRight.multiply(netCubeUp.x())
            .add(camUp.multiply(netCubeUp.y()))
            .add(camFront.multiply(netCubeUp.z()));

        Vector3Dd wdNorm = worldDirection.normalized();
        Vector3Dd projCubeUp = worldCubeUp.subtract(wdNorm.multiply(wdNorm.dotProduct(worldCubeUp)));
        Vector3Dd projRef    = camUp.subtract(wdNorm.multiply(wdNorm.dotProduct(camUp)));

        if ( projRef.length() < VSDK.EPSILON ) {
            projRef = camRight.subtract(wdNorm.multiply(wdNorm.dotProduct(camRight)));
        }
        if ( projCubeUp.length() < VSDK.EPSILON || projRef.length() < VSDK.EPSILON ) {
            return 0.0;
        }

        double cosAngle = projRef.dotProduct(projCubeUp);
        double sinAngle = projRef.crossProduct(projCubeUp).dotProduct(wdNorm);
        return Math.atan2(sinAngle, cosAngle);
    }
}
