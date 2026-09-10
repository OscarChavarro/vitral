import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Background } from "./Background.js";

/** A uniform, direction-independent background colour. */
export class SimpleBackground extends Background {
    private color = new ColorRgb(0, 0, 0);

    public override colorInDireccion(_d: Vector3Dd): ColorRgb {
        return new ColorRgb(this.color);
    }

    public setColor(r: number, g: number, b: number): void {
        this.color = new ColorRgb(r, g, b);
    }
}
