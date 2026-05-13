package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.elements.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

public abstract class Shader {
    public record LocalShadingResult(Vector3D normal, ColorRgb color)
    {
    }

    public abstract LocalShadingResult shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        SimpleMaterial material,
        TraceWorkspace workspace);
}
