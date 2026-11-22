import type { Vector3Df } from "../../common/linealAlgebra/Vector3Df.js";

/**
What the directional functions of PhongMaterial read about the surface point
being shaded: the shading normal and the texture coordinate, each with a flag
telling whether the caller could determine it. The caller finds the point,
its normal and its texture coordinate; the material only reads them.

Immutable value.
*/
export class SurfaceShadingPoint {
    private readonly shadingNormal: Vector3Df;
    private readonly textureCoordinate: Vector3Df;
    private readonly shadingNormalKnown: boolean;
    private readonly textureCoordinateKnown: boolean;

    public constructor(
        inShadingNormal: Vector3Df,
        inShadingNormalKnown: boolean,
        inTextureCoordinate: Vector3Df,
        inTextureCoordinateKnown: boolean,
    ) {
        this.shadingNormal = inShadingNormal;
        this.textureCoordinate = inTextureCoordinate;
        this.shadingNormalKnown = inShadingNormalKnown;
        this.textureCoordinateKnown = inTextureCoordinateKnown;
    }

    /** Unit normal leaving the surface on the side the scattering is evaluated from */
    public getShadingNormal(): Vector3Df {
        return this.shadingNormal;
    }

    public hasShadingNormal(): boolean {
        return this.shadingNormalKnown;
    }

    /** Texture coordinate (x and y used) */
    public getTextureCoordinate(): Vector3Df {
        return this.textureCoordinate;
    }

    public hasTextureCoordinate(): boolean {
        return this.textureCoordinateKnown;
    }
}
