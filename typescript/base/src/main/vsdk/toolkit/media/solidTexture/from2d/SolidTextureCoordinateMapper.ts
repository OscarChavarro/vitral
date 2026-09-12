import type { ControlledRGBAImageHDRUncompressed } from "./ControlledRGBAImageHDRUncompressed.js";
import { ImageToSolidTextureProjectionMethods } from "./ImageToSolidTextureProjectionMethods.js";

/**
The pair of image coordinates Java returns through two one-element `double[]`
out parameters. A mutable holder is the TypeScript spelling of that idiom, and
it keeps the read-modify-write sequences of the mapper and of
`ImageTexture.bumpMap` — which decrements and increments these very values
between samples — exactly as they are in Java.
*/
export class SolidTextureCoordinate {
    public constructor(
        public x = 0.0,
        public y = 0.0,
    ) {}
}

/**
Port of
`vsdk.toolkit.media.solidTexture.from2d.SolidTextureCoordinateMapper`.

Carries a point in solid-texture space onto an image, by one of the four
projections {@link ImageToSolidTextureProjectionMethods} names, and then wraps
the result into the image's own extent.

Note the return convention, which is Java's and is easy to read backwards:
{@link map} answers **true** when the point could not be mapped and the caller
should fall back, and **false** when the coordinates were written.
*/
export class SolidTextureCoordinateMapper {
    public map(
        x: number,
        y: number,
        z: number,
        image: ControlledRGBAImageHDRUncompressed,
        coordinate: SolidTextureCoordinate,
        smallTolerance: number,
    ): boolean {
        let ok: boolean;
        switch (image.getMapTypeEnum()) {
            case ImageToSolidTextureProjectionMethods.PLANAR_MAP:
                ok = SolidTextureCoordinateMapper.planarImageMap(x, y, z, image, coordinate);
                break;
            case ImageToSolidTextureProjectionMethods.SPHERICAL_MAP:
                ok = SolidTextureCoordinateMapper.sphericalImageMap(x, y, z, image, coordinate);
                break;
            case ImageToSolidTextureProjectionMethods.CYLINDRICAL_MAP:
                ok = SolidTextureCoordinateMapper.cylindricalImageMap(x, y, z, image, coordinate);
                break;
            case ImageToSolidTextureProjectionMethods.TORUS_MAP:
                ok = SolidTextureCoordinateMapper.torusImageMap(x, y, z, image, coordinate);
                break;
        }
        if (!ok) {
            return true;
        }

        coordinate.y += smallTolerance;
        coordinate.x += smallTolerance;
        coordinate.y = image.getYSize() - coordinate.y;

        if (coordinate.x < 0.0) {
            coordinate.x += image.getXSize();
        } else if (coordinate.x >= image.getXSize()) {
            coordinate.x -= image.getXSize();
        }
        if (coordinate.y < 0.0) {
            coordinate.y += image.getYSize();
        } else if (coordinate.y >= image.getYSize()) {
            coordinate.y -= image.getYSize();
        }

        if (
            coordinate.x >= image.getXSize() ||
            coordinate.y >= image.getYSize() ||
            coordinate.x < 0.0 ||
            coordinate.y < 0.0
        ) {
            throw new Error("Picture index out of range");
        }
        return false;
    }

    private static cylindricalImageMap(
        x: number,
        y: number,
        z: number,
        image: ControlledRGBAImageHDRUncompressed,
        coordinate: SolidTextureCoordinate,
    ): boolean {
        if (image.getOnceFlag() && (y < 0.0 || y > 1.0)) {
            return false;
        }
        coordinate.y = (y * image.getYSize()) % image.getYSize();
        let len: number = Math.sqrt(x * x + y * y + z * z);
        if (len === 0.0) {
            return false;
        }
        x /= len;
        z /= len;

        len = Math.sqrt(x * x + z * z);
        if (len === 0.0) {
            return false;
        }
        let theta: number;
        if (z === 0.0) {
            theta = x > 0.0 ? 0.0 : Math.PI;
        } else {
            theta = Math.acos(x / len);
            if (z < 0.0) {
                theta = 2.0 * Math.PI - theta;
            }
        }
        theta /= 2.0 * Math.PI;
        coordinate.x = theta * image.getXSize();
        return true;
    }

    private static torusImageMap(
        x: number,
        y: number,
        z: number,
        image: ControlledRGBAImageHDRUncompressed,
        coordinate: SolidTextureCoordinate,
    ): boolean {
        const r0: number = image.getImageGradient().x();
        let len: number = Math.sqrt(x * x + z * z);
        if (len === 0.0) {
            return false;
        }
        let theta: number;
        if (z === 0.0) {
            theta = x > 0.0 ? 0.0 : Math.PI;
        } else {
            theta = Math.acos(x / len);
            if (z < 0.0) {
                theta = 2.0 * Math.PI - theta;
            }
        }
        theta = 0.0 - theta;

        x = len - r0;
        len = Math.sqrt(x * x + y * y);
        let phi: number = Math.acos(-x / len);
        if (y > 0.0) {
            phi = 2.0 * Math.PI - phi;
        }
        theta /= 2.0 * Math.PI;
        phi /= 2.0 * Math.PI;
        coordinate.x = -theta * image.getXSize();
        coordinate.y = phi * image.getYSize();
        return true;
    }

    private static sphericalImageMap(
        x: number,
        y: number,
        z: number,
        image: ControlledRGBAImageHDRUncompressed,
        coordinate: SolidTextureCoordinate,
    ): boolean {
        let len: number = Math.sqrt(x * x + y * y + z * z);
        if (len === 0.0) {
            return false;
        }
        x /= len;
        y /= len;
        z /= len;

        const phi: number = 0.5 + Math.asin(y) / Math.PI;
        len = Math.sqrt(x * x + z * z);
        let theta: number;
        if (len === 0.0) {
            theta = 0.0;
        } else if (z === 0.0) {
            theta = x > 0.0 ? 0.0 : Math.PI;
        } else {
            theta = Math.acos(x / len);
            if (z < 0.0) {
                theta = 2.0 * Math.PI - theta;
            }
            theta /= 2.0 * Math.PI;
        }
        coordinate.x = theta * image.getXSize();
        coordinate.y = phi * image.getYSize();
        return true;
    }

    private static planarImageMap(
        x: number,
        y: number,
        z: number,
        image: ControlledRGBAImageHDRUncompressed,
        coordinate: SolidTextureCoordinate,
    ): boolean {
        if (image.getImageGradient().x() !== 0.0) {
            if (image.getOnceFlag() && (x < 0.0 || x > 1.0)) return false;
            if (image.getImageGradient().x() > 0.0) coordinate.x = (x * image.getXSize()) % image.getXSize();
            else coordinate.y = (x * image.getYSize()) % image.getYSize();
        }
        if (image.getImageGradient().y() !== 0.0) {
            if (image.getOnceFlag() && (y < 0.0 || y > 1.0)) return false;
            if (image.getImageGradient().y() > 0.0) coordinate.x = (y * image.getXSize()) % image.getXSize();
            else coordinate.y = (y * image.getYSize()) % image.getYSize();
        }
        if (image.getImageGradient().z() !== 0.0) {
            if (image.getOnceFlag() && (z < 0.0 || z > 1.0)) return false;
            if (image.getImageGradient().z() > 0.0) coordinate.x = (z * image.getXSize()) % image.getXSize();
            else coordinate.y = (z * image.getYSize()) % image.getYSize();
        }
        return true;
    }
}
