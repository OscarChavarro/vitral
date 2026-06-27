package gui;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceListener;
import model.MeshModel;

/**
Connects the tangible interface marker tracking service to {@link MeshModel}.

Coordinate-space mapping:
  The TangibleInterfaceMarkersDetectorServer delivers poses in the reference
  frame of the physical (real) camera.  We want to achieve a mirror-like
  correspondence: a marker moved in front of the real camera should move the
  gizmo in the same direction as seen from the virtual camera.

  Mapping (tangible → virtual-world):
    tangible +X  →  virtual camera right  (-camera.getLeft())
    tangible +Y  →  virtual camera up     ( camera.getUp())
    tangible +Z  →  virtual camera front  ( camera.getFront())

  world_position = camera.position
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
public class TangibleInterfaceInteractionTechniques implements TangibleInterfaceListener {

    private static final String RAY_CUBE_ID = "rayCube";
    // rayCube group frame: +Y points toward real camera when front/back faces
    // are visible.  Group -Y maps to "into scene" in camera space, and is
    // invariant under spin around the cube's pointing axis (group Y).
    private static final Vector3Dd MARKER_INTO_SCENE = new Vector3Dd(0, -1, 0);
    // Scales lateral (X/Y) movement: < 1 reduces sensitivity so the marker
    // does not need to travel as far to cover the viewport.
    private static final double DISTANCE_FACTOR = 4;
    // Maximum gizmo depth as a multiple of depthScale.  The un-clamped formula
    // reaches depthScale*5.0 at minimum safeNetZ; this constant halves that.
    private static final double MAX_GIZMO_DEPTH_FACTOR = 2.5;

    private final MeshModel model;
    private final Runnable repaintCallback;

    public TangibleInterfaceInteractionTechniques(MeshModel model, Runnable repaintCallback) {
        this.model = model;
        this.repaintCallback = repaintCallback;
    }

    @Override
    public void tangibleInterfaceEventReceived(TangibleInterfaceEvent event) {
        if ( event == null ) {
            return;
        }

        if ( !RAY_CUBE_ID.equals(event.getId()) ) {
            return;
        }

        Vector3Dd netPosition = event.getPosition();
        Vector3Dd netDirection = event.getRotation().rotate(MARKER_INTO_SCENE);
        Camera camera = model.getCamera();
        camera.updateVectors();

        Vector3Dd camPos   = camera.getPosition();
        double nearPlane   = camera.getNearPlaneDistance();
        double farPlane    = camera.getFarPlaneDistance();

        Vector3Dd camRight = camera.getLeft().multiply(-1.0);
        Vector3Dd camUp    = camera.getUp();
        Vector3Dd camFront = camera.getFront();

        // net Z is typically ~0.5 m from real camera; scale so the gizmo sits
        // roughly mid-frustum in the virtual world.
        double midDepth   = (nearPlane + farPlane) * 0.5;
        double netZRef    = 0.5;
        double depthScale = midDepth / netZRef;

        // Reciprocal depth: close marker → gizmo far from virtual camera.
        // At net_z = netZRef     → gizmo at midDepth (same as before)
        // At net_z = netZRef/2   → gizmo at 2*midDepth (double depth)
        // At net_z = 2*netZRef   → gizmo at midDepth/2 (half depth)
        double safeNetZ = Math.max(netPosition.z(), 0.05);
        double gizmoZ   = Math.min(depthScale * MAX_GIZMO_DEPTH_FACTOR,
                          Math.max(nearPlane * 1.5, depthScale * netZRef * netZRef / safeNetZ)) - 14;

        // Negate X and Y: tangible interface viewport axes are mirrored
        // relative to the virtual camera axes.
        Vector3Dd worldPosition =
            camPos
            .add(camRight.multiply(-netPosition.x() * depthScale * DISTANCE_FACTOR))
            .add(camUp   .multiply(-netPosition.y() * depthScale * DISTANCE_FACTOR))
            .add(camFront.multiply(gizmoZ));

        Vector3Dd worldDirection =
            camRight.multiply(netDirection.x())
            .add(camUp   .multiply(netDirection.y()))
            .add(camFront.multiply(netDirection.z()));

        model.getRayGizmo().setRay(worldPosition, worldDirection);
        repaintCallback.run();
    }
}
