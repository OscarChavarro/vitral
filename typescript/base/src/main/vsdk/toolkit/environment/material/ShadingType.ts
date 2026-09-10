export enum ShadingType {
    NOLIGHT = 0,
    FLAT = 1,
    GOURAUD = 2,
    PHONG = 3,
    COOK_TERRANCE = 4,
}
export namespace ShadingType {
    export const values = (): ShadingType[] => [
        ShadingType.NOLIGHT,
        ShadingType.FLAT,
        ShadingType.GOURAUD,
        ShadingType.PHONG,
        ShadingType.COOK_TERRANCE,
    ];
    export const valueOf = (name: string): ShadingType => {
        const value = ShadingType[name as keyof typeof ShadingType];
        if (typeof value !== "number") throw new RangeError(`No enum constant ShadingType.${name}`);
        return value;
    };
    export const getCode = (value: ShadingType): number => value;
    export const fromCode = (x: number): ShadingType => (x >= 0 && x <= 4 ? (x as ShadingType) : ShadingType.GOURAUD);
    export const next = (x: ShadingType): ShadingType => ((x + 1) % 5) as ShadingType;
}
