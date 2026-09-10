import { RendererConfiguration } from "./RendererConfiguration.js";

declare module "./RendererConfiguration.js" {
    interface RendererConfiguration {
        compareTo(other: RendererConfiguration): number;
        getUseVertexColors(): boolean;
        setUseVertexColors(value: boolean): void;
        toString(): string;
    }
}

RendererConfiguration.prototype.compareTo = function (other: RendererConfiguration): number {
    const packed = (value: RendererConfiguration): number =>
        (value.isSurfacesSet() ? 1 : 0) +
        (value.isWiresSet() ? 2 : 0) +
        (value.isBoundingVolumeSet() ? 4 : 0) +
        (value.isSelectionCornersSet() ? 8 : 0) +
        (value.isTextureSet() ? 16 : 0) +
        (value.isBumpMapSet() ? 32 : 0) +
        (value.isPointsSet() ? 64 : 0) +
        (value.isNormalsSet() ? 128 : 0) +
        (value.isTrianglesNormalsSet() ? 256 : 0) +
        value.getShadingType() * 0x1000 +
        Math.round(value.getVertexNormalSmoothingThresholdDegrees()) * 0x100000;
    return Math.sign(packed(this) - packed(other));
};
RendererConfiguration.prototype.getUseVertexColors = function (): boolean {
    return (this as unknown as { useVertexColors: boolean }).useVertexColors;
};
RendererConfiguration.prototype.setUseVertexColors = function (value: boolean): void {
    (this as unknown as { useVertexColors: boolean }).useVertexColors = value;
};
RendererConfiguration.prototype.toString = function (): string {
    const shading =
        ["LIGHTING DISABLED (ONLY AMBIENT COLOR)", "FLAT", "GOURAUD", "PHONG", "COOK-TERRANCE"][
            this.getShadingType()
        ] ?? "INVALID!";
    const state = (value: boolean): string => (value ? "ON" : "OFF");
    return `<RendererConfiguration>:\n  - Shading type: ${shading}\n  - Draw points: ${state(this.isPointsSet())}\n  - Draw wires: ${state(this.isWiresSet())}\n  - Draw surfaces: ${state(this.isSurfacesSet())}\n  - Draw bounding volume: ${state(this.isBoundingVolumeSet())}\n  - Draw selection corners: ${state(this.isSelectionCornersSet())}\n  - Draw normals: ${state(this.isNormalsSet())}\n  - Draw triangles normals: ${state(this.isTrianglesNormalsSet())}\n  - With texture: ${state(this.isTextureSet())}\n  - With bump map: ${state(this.isBumpMapSet())}\n  - Vertex normal smoothing threshold: ${this.getVertexNormalSmoothingThresholdDegrees()} deg\n`;
};
