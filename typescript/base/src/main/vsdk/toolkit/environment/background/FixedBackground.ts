import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { RGBAImageUncompressed } from "../../media/RGBAImageUncompressed.js";
import { Camera } from "../camera/Camera.js";
import { Background } from "./Background.js";

/**
 * A background image anchored to a camera.
 *
 * The Java implementation documents its directional projection as unfinished
 * and returns null; that compatibility behaviour is retained here.
 */
export class FixedBackground extends Background {
    public constructor(
        private camera: Camera,
        private backgroundImage: RGBAImageUncompressed,
    ) {
        super();
    }

    public setImage(image: RGBAImageUncompressed): void {
        this.backgroundImage = image;
    }

    public getImage(): RGBAImageUncompressed {
        return this.backgroundImage;
    }

    /** BUG compatibility: the Java near-plane projection is not implemented. */
    public override colorInDireccion(_d: Vector3Dd): null {
        return null;
    }

    public getCamera(): Camera {
        return this.camera;
    }
}
