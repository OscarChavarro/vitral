package render;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.material.SimpleMaterial;

/**
One geometry to draw, with its transformation and material. Lists of
primitives are the technology independent way for the application to
describe feedback geometry (debug entities, editor overlays): each rendering
technology draws them with its own renderer (i.e.
`Jogl4RenderPrimitiveRenderer`), so they are portable to new technologies by
writing only that renderer.
*/
public class RenderPrimitive
{
    private final Geometry geometry;
    private final Matrix4x4d transform;
    private final SimpleMaterial material;

    /**
    @param geometry geometry to draw
    @param transform transformation from geometry to world coordinates
    @param material material of the geometry
    */
    public RenderPrimitive(Geometry geometry, Matrix4x4d transform, SimpleMaterial material)
    {
        this.geometry = geometry;
        this.transform = transform;
        this.material = material;
    }

    public Geometry getGeometry()
    {
        return geometry;
    }

    public Matrix4x4d getTransform()
    {
        return transform;
    }

    public SimpleMaterial getMaterial()
    {
        return material;
    }
}
