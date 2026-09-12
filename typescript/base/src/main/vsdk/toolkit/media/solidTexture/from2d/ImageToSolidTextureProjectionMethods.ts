/**
Port of
`vsdk.toolkit.media.solidTexture.from2d.ImageToSolidTextureProjectionMethods`.

A string enum rather than a numeric one, as the rest of the TypeScript edition
does for a Java enum that can cross a structured clone; the Java ordinal-like
`value()` codes the enum actually carries are preserved by {@link value} and
{@link fromInt}, which are the persisted form.
*/
export enum ImageToSolidTextureProjectionMethods {
    PLANAR_MAP = "PLANAR_MAP",
    SPHERICAL_MAP = "SPHERICAL_MAP",
    CYLINDRICAL_MAP = "CYLINDRICAL_MAP",
    TORUS_MAP = "TORUS_MAP",
}

const VALUES: ReadonlyMap<ImageToSolidTextureProjectionMethods, number> = new Map([
    [ImageToSolidTextureProjectionMethods.PLANAR_MAP, 0],
    [ImageToSolidTextureProjectionMethods.SPHERICAL_MAP, 1],
    [ImageToSolidTextureProjectionMethods.CYLINDRICAL_MAP, 2],
    [ImageToSolidTextureProjectionMethods.TORUS_MAP, 5],
]);

export function imageToSolidTextureProjectionMethodValue(v: ImageToSolidTextureProjectionMethods): number {
    return VALUES.get(v)!;
}

export function imageToSolidTextureProjectionMethodFromInt(value: number): ImageToSolidTextureProjectionMethods {
    switch (value) {
        case 1:
            return ImageToSolidTextureProjectionMethods.SPHERICAL_MAP;
        case 2:
            return ImageToSolidTextureProjectionMethods.CYLINDRICAL_MAP;
        case 5:
            return ImageToSolidTextureProjectionMethods.TORUS_MAP;
        default:
            return ImageToSolidTextureProjectionMethods.PLANAR_MAP;
    }
}
