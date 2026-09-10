export enum ShadingType {
    NOLIGHT = 0,
    FLAT = 1,
    GOURAUD = 2,
    PHONG = 3,
    COOK_TERRANCE = 4,
}
export namespace ShadingType {
    export const fromCode = (x: number): ShadingType => (x >= 0 && x <= 4 ? (x as ShadingType) : ShadingType.GOURAUD);
    export const next = (x: ShadingType): ShadingType => ((x + 1) % 5) as ShadingType;
}
