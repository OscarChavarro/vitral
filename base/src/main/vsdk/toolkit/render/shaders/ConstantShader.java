package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

// GLSL analogue: constantPixelShader.glsl
public final class ConstantShader extends Shader {
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
        outColor.r += diffuse.r;
        outColor.g += diffuse.g;
        outColor.b += diffuse.b;
        return info.n;
    }
}
