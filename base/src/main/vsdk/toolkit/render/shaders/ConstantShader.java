package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

// GLSL analogue: constantPixelShader.glsl
public final class ConstantShader extends Shader {
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
        return new LocalShadingResult(info.n, new ColorRgb(diffuse));
    }
}
