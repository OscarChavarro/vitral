package vsdk.toolkit.environment.light;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Ray;

public final class AmbientLight extends Light
{
    public AmbientLight(ColorRgb emission)
    {
        super(new Vector3Dd(0, 0, 0), emission);
    }

    @Override
    public boolean isAmbient()
    {
        return true;
    }

    @Override
    public LightDirection getDirectionAndDistance(Vector3Dd surfacePoint)
    {
        // Never sampled: shaders resolve the ambient contribution from
        // getEmission() directly and skip the shadow ray entirely.
        return new LightDirection(new Vector3Dd(0, 0, 0), 0.0);
    }

    @Override
    public double evaluateLightResponseFactor(Ray lightSourceRay)
    {
        return 0.0;
    }

    @Override
    public Light copy()
    {
        AmbientLight copy = new AmbientLight(getEmission());
        copy.setId(getId());
        copy.setName(getName());
        return copy;
    }
}
