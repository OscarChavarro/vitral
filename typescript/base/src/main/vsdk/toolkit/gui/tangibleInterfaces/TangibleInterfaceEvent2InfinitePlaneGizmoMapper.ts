import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Camera } from "../../environment/camera/Camera.js";
import type { InfinitePlaneGizmo } from "../gizmo/InfinitePlaneGizmo.js";
import type { TangibleInterfaceEvent } from "./TangibleInterfaceEvent.js";

/**
Port of
`vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2InfinitePlaneGizmoMapper`.

The plane counterpart of {@link TangibleInterfaceEvent2RayGizmoMapper}: it
carries a marker pose into a cutting plane in the virtual world, using the same
tangible-to-camera axis mapping and the same reciprocal depth mapping recorded
there. The plane's normal has the ray direction projected out of it, so that a
cube spun about its pointing axis leaves the plane where it was.
*/
export class TangibleInterfaceEvent2InfinitePlaneGizmoMapper {
    private static readonly MARKER_INTO_SCENE = new Vector3Dd(0, -1, 0);
    private static readonly MARKER_PLANE_NORMAL = new Vector3Dd(0, 0, 1);
    private static readonly DISTANCE_FACTOR = 4;
    private static readonly MAX_GIZMO_DEPTH_FACTOR = 2.5;

    public constructor(private readonly camera: Camera) {}

    public map(event: TangibleInterfaceEvent | null, gizmo: InfinitePlaneGizmo | null): void {
        if (event === null || gizmo === null) {
            return;
        }

        this.camera.updateVectors();

        const netPosition: Vector3Dd = event.getPosition();
        const netNormal: Vector3Dd = event
            .getRotation()
            .rotate(TangibleInterfaceEvent2InfinitePlaneGizmoMapper.MARKER_PLANE_NORMAL);

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
                depthScale * TangibleInterfaceEvent2InfinitePlaneGizmoMapper.MAX_GIZMO_DEPTH_FACTOR,
                Math.max(nearPlane * 1.5, (depthScale * netZRef * netZRef) / safeNetZ),
            ) - 14;

        const worldPosition: Vector3Dd = camPos
            .add(
                camRight.multiply(
                    -netPosition.x() * depthScale * TangibleInterfaceEvent2InfinitePlaneGizmoMapper.DISTANCE_FACTOR,
                ),
            )
            .add(
                camUp.multiply(
                    -netPosition.y() * depthScale * TangibleInterfaceEvent2InfinitePlaneGizmoMapper.DISTANCE_FACTOR,
                ),
            )
            .add(camFront.multiply(gizmoZ));

        let worldNormal: Vector3Dd = camRight
            .multiply(netNormal.x())
            .add(camUp.multiply(netNormal.y()))
            .add(camFront.multiply(netNormal.z()));

        if (worldNormal.length() < VSDK.EPSILON) {
            worldNormal = camUp;
        }

        const worldRayDirection: Vector3Dd = TangibleInterfaceEvent2InfinitePlaneGizmoMapper.mapDirection(
            event.getRotation().rotate(TangibleInterfaceEvent2InfinitePlaneGizmoMapper.MARKER_INTO_SCENE),
            camRight,
            camUp,
            camFront,
        );
        worldNormal = TangibleInterfaceEvent2InfinitePlaneGizmoMapper.removeRayComponent(
            worldNormal,
            worldRayDirection,
            camUp,
        );

        gizmo.setPlane(worldPosition, worldNormal);
    }

    private static mapDirection(
        netDirection: Vector3Dd,
        camRight: Vector3Dd,
        camUp: Vector3Dd,
        camFront: Vector3Dd,
    ): Vector3Dd {
        return camRight
            .multiply(netDirection.x())
            .add(camUp.multiply(netDirection.y()))
            .add(camFront.multiply(netDirection.z()));
    }

    private static removeRayComponent(normal: Vector3Dd, rayDirection: Vector3Dd, fallback: Vector3Dd): Vector3Dd {
        if (rayDirection.length() < VSDK.EPSILON) {
            return normal.normalized();
        }
        const ray: Vector3Dd = rayDirection.normalized();
        let projected: Vector3Dd = normal.subtract(ray.multiply(normal.dotProduct(ray)));
        if (projected.length() >= VSDK.EPSILON) {
            return projected.normalized();
        }
        projected = fallback.subtract(ray.multiply(fallback.dotProduct(ray)));
        if (projected.length() >= VSDK.EPSILON) {
            return projected.normalized();
        }
        return normal.normalized();
    }
}
