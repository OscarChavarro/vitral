import { Entity } from "../../common/Entity.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
 * A model-side source of colour for a viewing direction.
 *
 * Backgrounds deliberately do not depend on a rendering backend.  A concrete
 * background can therefore be sampled by either the software or GPU renderer.
 */
export abstract class Background extends Entity {
    public abstract colorInDireccion(d: Vector3Dd): ColorRgb | null;
}
