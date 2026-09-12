import type { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Image } from "../../media/Image.js";
import type { NormalMap } from "../../media/NormalMap.js";

export class CpuTextureSamplingConfig {
    private static textureOffsetUTexels = -0.5;
    private static textureOffsetVTexels = -0.5;
    private static readonly NORMAL_OFFSET_U_TEXELS = -0.5;
    private static readonly NORMAL_OFFSET_V_TEXELS = -0.5;

    private constructor() {}

    public static setTextureOffsetTexels(uTexels: number, vTexels: number): void {
        CpuTextureSamplingConfig.textureOffsetUTexels = uTexels;
        CpuTextureSamplingConfig.textureOffsetVTexels = vTexels;
    }

    public static sample(texture: Image | null, u: number, v: number): ColorRgb | null {
        if (texture === null) {
            return null;
        }
        let du = 0.0;
        let dv = 0.0;
        const width: number = texture.getXSize();
        const height: number = texture.getYSize();
        if (width > 0) {
            du = CpuTextureSamplingConfig.textureOffsetUTexels / width;
        }
        if (height > 0) {
            dv = CpuTextureSamplingConfig.textureOffsetVTexels / height;
        }
        return texture.getColorRgbBiLinear(u + du, v + dv);
    }

    public static sampleNormal(normalMap: NormalMap | null, u: number, v: number): Vector3Dd | null {
        if (normalMap === null) {
            return null;
        }
        let du = 0.0;
        let dv = 0.0;
        const width: number = normalMap.getXSize();
        const height: number = normalMap.getYSize();
        if (width > 0) {
            du = CpuTextureSamplingConfig.NORMAL_OFFSET_U_TEXELS / width;
        }
        if (height > 0) {
            dv = CpuTextureSamplingConfig.NORMAL_OFFSET_V_TEXELS / height;
        }
        return normalMap.getNormal(u + du, v + dv);
    }
}
