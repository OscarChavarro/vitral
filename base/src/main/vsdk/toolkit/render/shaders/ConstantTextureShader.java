package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

// GLSL analogue: constantTexturePixelShader.glsl
public final class ConstantTextureShader extends Shader {
    @Override
    public Vector3D shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        Material material,
        TraceWorkspace workspace,
        ColorRgb outColor)
    {
        ColorRgb diffuse = material.getDiffuseReference();
        double r = diffuse.r;
        double g = diffuse.g;
        double b = diffuse.b;

        if ( info.texture != null ) {
            ColorRgb textureColor =
                CpuTextureSamplingConfig.sample(info.texture, info.u, 1 - info.v);
            r *= textureColor.r;
            g *= textureColor.g;
            b *= textureColor.b;
        }

        outColor.r += r;
        outColor.g += g;
        outColor.b += b;
        return info.n;
    }
}
