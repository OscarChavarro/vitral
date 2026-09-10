import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { RGBAImageUncompressed } from "../../media/RGBAImageUncompressed.js";
import { Ray } from "../geometry/element/Ray.js";
import { RayHit } from "../geometry/element/RayHit.js";
import { Box } from "../geometry/volume/Box.js";
import { Camera } from "../camera/Camera.js";
import { Background } from "./Background.js";

/** A six-image environment map sampled by casting a ray through a unit cube. */
export class CubemapBackground extends Background {
    private readonly backgroundImages: RGBAImageUncompressed[];
    private readonly boundingCube = new Box(1, 1, 1);

    public constructor(
        private camera: Camera,
        front: RGBAImageUncompressed,
        right: RGBAImageUncompressed,
        back: RGBAImageUncompressed,
        left: RGBAImageUncompressed,
        down: RGBAImageUncompressed,
        up: RGBAImageUncompressed,
    ) {
        super();
        this.backgroundImages = [front, right, back, left, down, up];
    }

    public override colorInDireccion(d: Vector3Dd): ColorRgb {
        const ray = new Ray(new Vector3Dd(), d.normalized());
        const hit = new RayHit();
        if (!this.boundingCube.doIntersectionFirstHit(ray, hit)) return new ColorRgb();

        const plane = this.classifyPlane(hit.n);
        let u = 1 - hit.u;
        let v = 1 - hit.v;
        let image: RGBAImageUncompressed;
        switch (plane) {
            case 1:
                image = this.backgroundImages[5]!;
                u = 1 - hit.v;
                v = hit.u;
                break;
            case 2:
                image = this.backgroundImages[4]!;
                u = hit.v;
                v = 1 - hit.u;
                break;
            case 3:
                image = this.backgroundImages[0]!;
                break;
            case 4:
                image = this.backgroundImages[2]!;
                break;
            case 5:
                image = this.backgroundImages[1]!;
                break;
            default:
                image = this.backgroundImages[3]!;
                break;
        }
        return image.getColorRgbBiLinear(u, v);
    }

    public getImages(): RGBAImageUncompressed[] {
        return this.backgroundImages;
    }

    public getCamera(): Camera {
        return this.camera;
    }

    public setCamera(camera: Camera): void {
        this.camera = camera;
    }

    private classifyPlane(normal: Vector3Dd): number {
        const x = Math.abs(normal.x());
        const y = Math.abs(normal.y());
        const z = Math.abs(normal.z());
        if (z >= x && z >= y) return normal.z() >= 0 ? 1 : 2;
        if (y >= x) return normal.y() >= 0 ? 3 : 4;
        return normal.x() >= 0 ? 5 : 6;
    }
}
