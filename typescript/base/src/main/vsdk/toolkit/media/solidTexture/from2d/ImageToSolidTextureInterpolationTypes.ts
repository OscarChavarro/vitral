/**
Port of
`vsdk.toolkit.media.solidTexture.from2d.ImageToSolidTextureInterpolationTypes`.

A string enum for the same reason as
`ImageToSolidTextureProjectionMethods`; the Java codes survive in
{@link imageToSolidTextureInterpolationTypeValue} and
{@link imageToSolidTextureInterpolationTypeFromInt}.
*/
export enum ImageToSolidTextureInterpolationTypes {
    NO_INTERPOLATION = "NO_INTERPOLATION",
    NEAREST_NEIGHBOR = "NEAREST_NEIGHBOR",
    BI_LINEAR = "BI_LINEAR",
    CUBIC_SPLINE = "CUBIC_SPLINE",
    NORMALIZED_DIST = "NORMALIZED_DIST",
}

const VALUES: ReadonlyMap<ImageToSolidTextureInterpolationTypes, number> = new Map([
    [ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION, 0],
    [ImageToSolidTextureInterpolationTypes.NEAREST_NEIGHBOR, 1],
    [ImageToSolidTextureInterpolationTypes.BI_LINEAR, 2],
    [ImageToSolidTextureInterpolationTypes.CUBIC_SPLINE, 3],
    [ImageToSolidTextureInterpolationTypes.NORMALIZED_DIST, 4],
]);

export function imageToSolidTextureInterpolationTypeValue(v: ImageToSolidTextureInterpolationTypes): number {
    return VALUES.get(v)!;
}

export function imageToSolidTextureInterpolationTypeFromInt(value: number): ImageToSolidTextureInterpolationTypes {
    switch (value) {
        case 1:
            return ImageToSolidTextureInterpolationTypes.NEAREST_NEIGHBOR;
        case 2:
            return ImageToSolidTextureInterpolationTypes.BI_LINEAR;
        case 3:
            return ImageToSolidTextureInterpolationTypes.CUBIC_SPLINE;
        case 4:
            return ImageToSolidTextureInterpolationTypes.NORMALIZED_DIST;
        default:
            return ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION;
    }
}
