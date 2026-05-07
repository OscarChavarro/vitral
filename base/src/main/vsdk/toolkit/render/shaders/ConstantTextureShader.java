package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

// GLSL analogue: constantTexturePixelShader.glsl
public final class ConstantTextureShader extends Shader {
    @Override
    public LocalShadingResult shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        SimpleMaterial material,
        TraceWorkspace workspace)
    {
        ColorRgb diffuse = material.getDiffuseReference();
        double r = diffuse.r();
        double g = diffuse.g();
        double b = diffuse.b();

        if ( info.texture != null ) {
            ColorRgb textureColor =
                CpuTextureSamplingConfig.sample(info.texture, info.u, 1 - info.v);
            r *= textureColor.r();
            g *= textureColor.g();
            b *= textureColor.b();
        }

        return new LocalShadingResult(info.n, new ColorRgb(r, g, b));
    }
}
