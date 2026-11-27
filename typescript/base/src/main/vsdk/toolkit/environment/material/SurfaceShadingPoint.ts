import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
What the directional functions of PhongMaterial read about the surface point
being shaded: the shading normal and the texture coordinate, each with a flag
telling whether the caller could determine it. The caller finds the point,
its normal and its texture coordinate; the material only reads them.

Immutable value.
*/
export class SurfaceShadingPoint {
    private readonly shadingNormal: Vector3Dd;
    private readonly textureCoordinate: Vector3Dd;
    private readonly shadingNormalKnown: boolean;
    private readonly textureCoordinateKnown: boolean;

    public constructor(
        inShadingNormal: Vector3Dd,
        inShadingNormalKnown: boolean,
        inTextureCoordinate: Vector3Dd,
        inTextureCoordinateKnown: boolean,
    ) {
        this.shadingNormal = inShadingNormal;
        this.textureCoordinate = inTextureCoordinate;
        this.shadingNormalKnown = inShadingNormalKnown;
        this.textureCoordinateKnown = inTextureCoordinateKnown;
    }

    /** Unit normal leaving the surface on the side the scattering is evaluated from */
    public getShadingNormal(): Vector3Dd {
        return this.shadingNormal;
    }

    public hasShadingNormal(): boolean {
        return this.shadingNormalKnown;
    }

    /** Texture coordinate (x and y used) */
    public getTextureCoordinate(): Vector3Dd {
        return this.textureCoordinate;
    }

    public hasTextureCoordinate(): boolean {
        return this.textureCoordinateKnown;
    }
}
