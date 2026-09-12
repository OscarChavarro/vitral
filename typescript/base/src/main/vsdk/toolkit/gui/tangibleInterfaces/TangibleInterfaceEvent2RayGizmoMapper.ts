import { VSDK } from "../../common/VSDK.js";
import { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { RayGizmo } from "../gizmo/RayGizmo.js";
import type { TangibleInterfaceEvent } from "./TangibleInterfaceEvent.js";

/**
Converts a {@link TangibleInterfaceEvent} pose into a {@link RayGizmo} position
and direction.

Coordinate-space mapping:
  The TangibleInterfaceMarkersDetectorServer delivers poses in the reference
  frame of the physical (real) camera.  We want to achieve a mirror-like
  correspondence: a marker moved in front of the real camera should move the
  gizmo in the same direction as seen from the virtual camera.

  Mapping (tangible -> virtual-world):
    tangible +X  ->  virtual camera right  (-camera.getLeft())
    tangible +Y  ->  virtual camera up     ( camera.getUp())
    tangible +Z  ->  virtual camera front  ( camera.getFront())

  world_position = camera.getPosition()
                 + right  * (-net_position.x)        [X mirrored]
                 + up     * (-net_position.y)        [Y mirrored]
                 + front  * (netZRef^2/net_position.z) [Z inverted: close->far]

  Depth inversion: when the physical marker is CLOSE to the real camera the
  gizmo moves AWAY from the virtual camera (and vice versa), using a reciprocal
  mapping anchored at netZRef so that at z=netZRef/2 the gizmo sits at double
  the mid-frustum depth.

  MARKER_INTO_SCENE in group frame:
    The rayCube group frame has its +Y axis pointing toward the camera when
    the front/back faces (marker ids 11/14) are visible.  Rotating group -Y
    by the camera-group quaternion gives a direction pointing INTO the virtual
    scene.  Using -Y also preserves direction stability under cube spin:
    spinning the cube around its pointing axis (group Y) leaves -Y invariant.
*/
export class TangibleInterfaceEvent2RayGizmoMapper {
    private static readonly MARKER_INTO_SCENE = new Vector3Dd(0, -1, 0);
    private static readonly DISTANCE_FACTOR = 4;
    private static readonly MAX_GIZMO_DEPTH_FACTOR = 2.5;

    public constructor(private readonly camera: Camera) {}

    public map(event: TangibleInterfaceEvent, gizmo: RayGizmo): void {
        this.camera.updateVectors();

        const netPosition: Vector3Dd = event.getPosition();
        const netDirection: Vector3Dd = event
            .getRotation()
            .rotate(TangibleInterfaceEvent2RayGizmoMapper.MARKER_INTO_SCENE);

        const camPos: Vector3Dd = this.camera.getPosition();
        const nearPlane: number = this.camera.getNearPlaneDistance();
        const farPlane: number = this.camera.getFarPlaneDistance();

        const camRight: Vector3Dd = this.camera.getLeft().multiply(-1.0);
        const camUp: Vector3Dd = this.camera.getUp();
        const camFront: Vector3Dd = this.camera.getFront();

        const midDepth: number = (nearPlane + farPlane) * 0.5;
        const netZRef = 0.5;
        const depthScale: number = midDepth / netZRef;

        const safeNetZ: number = Math.max(netPosition.z(), 0.05);
        const gizmoZ: number =
            Math.min(
                depthScale * TangibleInterfaceEvent2RayGizmoMapper.MAX_GIZMO_DEPTH_FACTOR,
                Math.max(nearPlane * 1.5, (depthScale * netZRef * netZRef) / safeNetZ),
            ) - 14;

        const worldPosition: Vector3Dd = camPos
            .add(
                camRight.multiply(
                    -netPosition.x() * depthScale * TangibleInterfaceEvent2RayGizmoMapper.DISTANCE_FACTOR,
                ),
            )
            .add(camUp.multiply(-netPosition.y() * depthScale * TangibleInterfaceEvent2RayGizmoMapper.DISTANCE_FACTOR))
            .add(camFront.multiply(gizmoZ));

        const worldDirection: Vector3Dd = camRight
            .multiply(netDirection.x())
            .add(camUp.multiply(netDirection.y()))
            .add(camFront.multiply(netDirection.z()));

        const rollAngle: number = TangibleInterfaceEvent2RayGizmoMapper.computeRollAngle(
            event.getRotation(),
            worldDirection,
            camRight,
            camUp,
            camFront,
        );

        gizmo.setRay(new Ray(worldPosition, worldDirection), rollAngle);
    }

    private static computeRollAngle(
        rotation: Quaterniond,
        worldDirection: Vector3Dd,
        camRight: Vector3Dd,
        camUp: Vector3Dd,
        camFront: Vector3Dd,
    ): number {
        const netCubeUp: Vector3Dd = rotation.rotate(new Vector3Dd(0, 0, 1));
        const worldCubeUp: Vector3Dd = camRight
            .multiply(netCubeUp.x())
            .add(camUp.multiply(netCubeUp.y()))
            .add(camFront.multiply(netCubeUp.z()));

        const wdNorm: Vector3Dd = worldDirection.normalized();
        const projCubeUp: Vector3Dd = worldCubeUp.subtract(wdNorm.multiply(wdNorm.dotProduct(worldCubeUp)));
        let projRef: Vector3Dd = camUp.subtract(wdNorm.multiply(wdNorm.dotProduct(camUp)));

        if (projRef.length() < VSDK.EPSILON) {
            projRef = camRight.subtract(wdNorm.multiply(wdNorm.dotProduct(camRight)));
        }
        if (projCubeUp.length() < VSDK.EPSILON || projRef.length() < VSDK.EPSILON) {
            return 0.0;
        }

        const cosAngle: number = projRef.dotProduct(projCubeUp);
        const sinAngle: number = projRef.crossProduct(projCubeUp).dotProduct(wdNorm);
        return Math.atan2(sinAngle, cosAngle);
    }
}
