import { FundamentalEntity } from "../../common/FundamentalEntity.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { ShadingType } from "./ShadingType.js";
export class RendererConfiguration extends FundamentalEntity {
    public static readonly SHADING_TYPE_NOLIGHT = 0;
    public static readonly SHADING_TYPE_FLAT = 1;
    public static readonly SHADING_TYPE_GOURAUD = 2;
    public static readonly SHADING_TYPE_PHONG = 3;
    public static readonly SHADING_TYPE_COOK_TERRANCE = 4;
    private shading = 2;
    private surfaces = true;
    private wires = false;
    private boundingVolume = false;
    private selectionCorners = false;
    private texture = true;
    private bumpMap = false;
    private points = false;
    private normals = false;
    private trianglesNormals = false;
    private useVertexColors = false;
    private threshold = 15;
    private wireColor = new ColorRgb(1, 1, 1);
    private boundingColor = new ColorRgb(1, 1, 0);
    private lodHint = 0;
    public compareTo(other: RendererConfiguration): number {
        const value = (configuration: RendererConfiguration): number =>
            (configuration.surfaces ? 0x0001 : 0) +
            (configuration.wires ? 0x0002 : 0) +
            (configuration.boundingVolume ? 0x0004 : 0) +
            (configuration.selectionCorners ? 0x0008 : 0) +
            (configuration.texture ? 0x0010 : 0) +
            (configuration.bumpMap ? 0x0020 : 0) +
            (configuration.points ? 0x0040 : 0) +
            (configuration.normals ? 0x0080 : 0) +
            (configuration.trianglesNormals ? 0x0100 : 0) +
            configuration.shading * 0x1000 +
            Math.round(configuration.threshold) * 0x100000;
        const thisValue = value(this),
            otherValue = value(other);
        return thisValue > otherValue ? 1 : thisValue < otherValue ? -1 : 0;
    }
    public override clone(o?: RendererConfiguration): RendererConfiguration | void {
        if (o === undefined) {
            const c = new RendererConfiguration();
            c.clone(this);
            return c;
        }
        Object.assign(this, o);
        this.wireColor = new ColorRgb(o.wireColor);
        this.boundingColor = new ColorRgb(o.boundingColor);
    }
    public setLodHint(x: number) {
        this.lodHint = x;
    }
    public getLodHint() {
        return this.lodHint;
    }
    public setWireColor(c: ColorRgb): void;
    public setWireColor(r: number, g: number, b: number): void;
    public setWireColor(a: ColorRgb | number, b?: number, c?: number) {
        this.wireColor = a instanceof ColorRgb ? new ColorRgb(a) : new ColorRgb(a, b!, c!);
    }
    public getWireColor() {
        return this.wireColor;
    }
    public setBoundingVolumeColor(c: ColorRgb) {
        this.boundingColor = new ColorRgb(c);
    }
    public getBoundingVolumeColor() {
        return this.boundingColor;
    }
    public setShadingType(s: number | ShadingType | null) {
        this.shading = s === null ? RendererConfiguration.SHADING_TYPE_GOURAUD : s;
    }
    public getShadingType() {
        return this.shading;
    }
    public getShadingTypeEnum() {
        return ShadingType.fromCode(this.shading);
    }
    public setVertexNormalSmoothingThresholdDegrees(x: number) {
        this.threshold = Number.isNaN(x) ? 15 : Math.max(0, Math.min(180, x));
    }
    public getVertexNormalSmoothingThresholdDegrees() {
        return this.threshold;
    }
    public setSurfaces(x: boolean) {
        this.surfaces = x;
    }
    public isSurfacesSet() {
        return this.surfaces;
    }
    public changeSurfaces() {
        this.surfaces = !this.surfaces;
    }
    public setWires(x: boolean) {
        this.wires = x;
    }
    public isWiresSet() {
        return this.wires;
    }
    public changeWires() {
        this.wires = !this.wires;
    }
    public setBoundingVolume(x: boolean) {
        this.boundingVolume = x;
    }
    public isBoundingVolumeSet() {
        return this.boundingVolume;
    }
    public changeBoundingVolume() {
        this.boundingVolume = !this.boundingVolume;
    }
    public setSelectionCorners(x: boolean) {
        this.selectionCorners = x;
    }
    public isSelectionCornersSet() {
        return this.selectionCorners;
    }
    public changeSelectionCorners() {
        this.selectionCorners = !this.selectionCorners;
    }
    public setTexture(x: boolean) {
        this.texture = x;
    }
    public isTextureSet() {
        return this.texture;
    }
    public changeTexture() {
        this.texture = !this.texture;
    }
    public setBumpMap(x: boolean) {
        this.bumpMap = x;
    }
    public isBumpMapSet() {
        return this.bumpMap;
    }
    public changeBumpMap() {
        this.bumpMap = !this.bumpMap;
    }
    public setPoints(x: boolean) {
        this.points = x;
    }
    public isPointsSet() {
        return this.points;
    }
    public changePoints() {
        this.points = !this.points;
    }
    public setNormals(x: boolean) {
        this.normals = x;
    }
    public isNormalsSet() {
        return this.normals;
    }
    public changeNormals() {
        this.normals = !this.normals;
    }
    public setTrianglesNormals(x: boolean) {
        this.trianglesNormals = x;
    }
    public isTrianglesNormalsSet() {
        return this.trianglesNormals;
    }
    public changeTrianglesNormals() {
        this.trianglesNormals = !this.trianglesNormals;
    }
    public changeShadingType() {
        this.shading = ShadingType.next(this.getShadingTypeEnum());
    }
    public getUseVertexColors(): boolean {
        return this.useVertexColors;
    }
    public setUseVertexColors(useVertexColors: boolean): void {
        this.useVertexColors = useVertexColors;
    }
    public override toString(): string {
        const names = ["LIGHTING DISABLED (ONLY AMBIENT COLOR)", "FLAT", "GOURAUD", "PHONG", "COOK-TERRANCE"];
        let message = `<RendererConfiguration>:\n  - Shading type: ${names[this.shading] ?? "INVALID!"}\n`;
        message += `  - Draw points: ${this.points ? "ON" : "OFF"}\n`;
        message += `  - Draw wires: ${this.wires ? "ON" : "OFF"}\n`;
        message += `  - Draw surfaces: ${this.surfaces ? "ON" : "OFF"}\n`;
        message += `  - Draw bounding volume: ${this.boundingVolume ? "ON" : "OFF"}\n`;
        message += `  - Draw selection corners: ${this.selectionCorners ? "ON" : "OFF"}\n`;
        message += `  - Draw normals: ${this.normals ? "ON" : "OFF"}\n`;
        message += `  - Draw triangles normals: ${this.trianglesNormals ? "ON" : "OFF"}\n`;
        message += `  - With texture: ${this.texture ? "ON" : "OFF"}\n`;
        message += `  - With bump map: ${this.bumpMap ? "ON" : "OFF"}\n`;
        message += `  - Vertex normal smoothing threshold: ${this.threshold} deg\n`;
        return message;
    }
}
