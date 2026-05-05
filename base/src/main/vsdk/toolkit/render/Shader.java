package vsdk.toolkit.render;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

abstract class Shader {
    abstract Vector3D shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        Material material,
        TraceWorkspace workspace,
        ColorRgb outColor);
}
