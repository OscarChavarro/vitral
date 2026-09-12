import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Calligraphic2DBuffer } from "../../media/Calligraphic2DBuffer.js";

export class LightGizmoOmniBillboard {
    private static readonly NUMBER_OF_SIDES = 32;
    private static readonly NUMBER_OF_RAYS = 8;
    private static readonly CIRCLE_RADIUS = 0.2;
    private static readonly RAY_INNER_RADIUS = 0.3;
    private static readonly RAY_OUTER_RADIUS = 0.5;

    private constructor() {}

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
