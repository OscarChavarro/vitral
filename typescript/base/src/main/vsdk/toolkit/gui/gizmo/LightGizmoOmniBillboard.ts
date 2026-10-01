import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import { Calligraphic2DBuffer } from "../../media/Calligraphic2DBuffer.js";

export class LightGizmoOmniBillboard {
    private static readonly NUMBER_OF_SIDES = 32;
    private static readonly NUMBER_OF_RAYS = 8;
    private static readonly CIRCLE_RADIUS = 0.2;
    private static readonly RAY_INNER_RADIUS = 0.3;
    private static readonly RAY_OUTER_RADIUS = 0.5;

    /// Apparent size of the gizmo (its full width), as a fraction of the
    /// smaller dimension of the viewport
    private static readonly VIEWPORT_FRACTION = 0.05;

    private constructor() {}

    /**
    Calculates half the size of the gizmo in world units, which is also the
    world radius of its outermost rays. The gizmo keeps a constant apparent
    size in the viewport, so this depends on the distance to the camera.
    This value is shared by the gizmo drawing and by the picking of lights.
    @param camera camera that views the gizmo, or null
    @param position position of the light, in world coordinates
    @param viewportWidth width of the viewport in pixels
    @param viewportHeight height of the viewport in pixels
    @return half the size of the gizmo, in world units
    */
    public static calculateWorldHalfSize(
        camera: Camera | null,
        position: Vector3Dd,
        viewportWidth: number,
        viewportHeight: number,
    ): number {
        const targetPixels: number = LightGizmoOmniBillboard.VIEWPORT_FRACTION
            * Math.min(viewportWidth, viewportHeight);

        if (camera === null) {
            return Math.max(0.05, targetPixels / Math.max(viewportHeight, 1));
        }

        if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
            const worldViewHeight: number = 2.0 / camera.getOrthogonalZoom();
            const worldPerPixel: number = worldViewHeight / Math.max(viewportHeight, 1);
            return Math.max(1e-5, 0.5 * targetPixels * worldPerPixel);
        }

        const toLight: Vector3Dd = position.subtract(camera.getPosition());
        let depth: number = Math.abs(toLight.dotProduct(camera.getFront()));
        depth = Math.max(depth, camera.getNearPlaneDistance());

        const fovRadians: number = JavaMath.toRadians(camera.getFov());
        const worldViewHeightAtDepth: number = 2.0 * depth * Math.tan(fovRadians / 2.0);
        const worldPerPixel: number = worldViewHeightAtDepth / Math.max(viewportHeight, 1);
        return Math.max(1e-5, 0.5 * targetPixels * worldPerPixel);
    }

    public static createLinePattern(): Calligraphic2DBuffer {
        const lines = new Calligraphic2DBuffer();

        const cx = 0.5;
        const cy = 0.5;

        for (let i = 0; i < LightGizmoOmniBillboard.NUMBER_OF_SIDES; i++) {
            const a0: number = (2.0 * Math.PI * i) / LightGizmoOmniBillboard.NUMBER_OF_SIDES;
            const a1: number = (2.0 * Math.PI * (i + 1)) / LightGizmoOmniBillboard.NUMBER_OF_SIDES;

            const x0: number = cx + LightGizmoOmniBillboard.CIRCLE_RADIUS * Math.cos(a0);
            const y0: number = cy + LightGizmoOmniBillboard.CIRCLE_RADIUS * Math.sin(a0);
            const x1: number = cx + LightGizmoOmniBillboard.CIRCLE_RADIUS * Math.cos(a1);
            const y1: number = cy + LightGizmoOmniBillboard.CIRCLE_RADIUS * Math.sin(a1);

            lines.add2DLine(x0, y0, x1, y1);
        }

        for (let i = 0; i < LightGizmoOmniBillboard.NUMBER_OF_RAYS; i++) {
            const a: number = (2.0 * Math.PI * i) / LightGizmoOmniBillboard.NUMBER_OF_RAYS;

            const x0: number = cx + LightGizmoOmniBillboard.RAY_INNER_RADIUS * Math.cos(a);
            const y0: number = cy + LightGizmoOmniBillboard.RAY_INNER_RADIUS * Math.sin(a);
            const x1: number = cx + LightGizmoOmniBillboard.RAY_OUTER_RADIUS * Math.cos(a);
            const y1: number = cy + LightGizmoOmniBillboard.RAY_OUTER_RADIUS * Math.sin(a);

            lines.add2DLine(new Vector3Dd(x0, y0, 0.0), new Vector3Dd(x1, y1, 0.0));
        }

        return lines;
    }
}
