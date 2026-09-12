import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { IndexedColorImageHDRUncompressed } from "../../IndexedColorImageHDRUncompressed.js";
import { RGBAImageHDRUncompressed } from "../../RGBAImageHDRUncompressed.js";
import {
    ImageToSolidTextureInterpolationTypes,
    imageToSolidTextureInterpolationTypeFromInt,
    imageToSolidTextureInterpolationTypeValue,
} from "./ImageToSolidTextureInterpolationTypes.js";
import {
    ImageToSolidTextureProjectionMethods,
    imageToSolidTextureProjectionMethodFromInt,
    imageToSolidTextureProjectionMethodValue,
} from "./ImageToSolidTextureProjectionMethods.js";

/**
Port of
`vsdk.toolkit.media.solidTexture.from2d.ControlledRGBAImageHDRUncompressed`.

An HDR image plus the controls a solid-texture mapping needs: which projection
carries the 3D point onto it, how a sample between texels is interpolated,
whether the mapping tiles or applies once, whether the color or the palette
index is the payload, along which axes the image gradient runs, and an optional
palette-indexed twin of the same pixels.

Java overloads `setMapType` and `setInterpolationType` on the enum and on its
integer code; TypeScript takes either in one setter, since a string enum and a
number cannot be confused.
*/
export class ControlledRGBAImageHDRUncompressed extends RGBAImageHDRUncompressed {
    private mapType: ImageToSolidTextureProjectionMethods = ImageToSolidTextureProjectionMethods.PLANAR_MAP;
    private interpolationType: ImageToSolidTextureInterpolationTypes =
        ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION;
    private onceFlag = false;
    private useColorFlag = true;
    private imageGradient: Vector3Dd = new Vector3Dd();
    private indexedData: IndexedColorImageHDRUncompressed | null = null;

    public getMapType(): number {
        return imageToSolidTextureProjectionMethodValue(this.mapType);
    }

    public getMapTypeEnum(): ImageToSolidTextureProjectionMethods {
        return this.mapType;
    }

    public setMapType(v: ImageToSolidTextureProjectionMethods | number): void {
        this.mapType = typeof v === "number" ? imageToSolidTextureProjectionMethodFromInt(v) : v;
    }

    public getInterpolationType(): number {
        return imageToSolidTextureInterpolationTypeValue(this.interpolationType);
    }

    public getInterpolationTypeEnum(): ImageToSolidTextureInterpolationTypes {
        return this.interpolationType;
    }

    public setInterpolationType(v: ImageToSolidTextureInterpolationTypes | number): void {
        this.interpolationType = typeof v === "number" ? imageToSolidTextureInterpolationTypeFromInt(v) : v;
    }

    public getOnceFlag(): boolean {
        return this.onceFlag;
    }

    public setOnceFlag(v: boolean): void {
        this.onceFlag = v;
    }

    public getUseColorFlag(): boolean {
        return this.useColorFlag;
    }

    public setUseColorFlag(v: boolean): void {
        this.useColorFlag = v;
    }

    public getImageGradient(): Vector3Dd {
        return this.imageGradient;
    }

    public setImageGradient(v: Vector3Dd): void {
        this.imageGradient = v;
    }

    public getIndexedData(): IndexedColorImageHDRUncompressed | null {
        return this.indexedData;
    }

    public setIndexedData(v: IndexedColorImageHDRUncompressed | null): void {
        this.indexedData = v;
    }
}
