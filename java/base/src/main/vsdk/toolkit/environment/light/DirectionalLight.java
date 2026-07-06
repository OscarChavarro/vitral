package vsdk.toolkit.environment.light;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Ray;

public final class DirectionalLight extends Light
{
    public DirectionalLight(Vector3Dd direction, ColorRgb emission)
    {
        super(direction.normalized(), emission);
    }

    @Override
    public LightDirection getDirectionAndDistance(Vector3Dd surfacePoint)
    {
        Vector3Dd direction = getPosition();
        return new LightDirection(
            new Vector3Dd(-direction.x(), -direction.y(), -direction.z()),
            Double.POSITIVE_INFINITY);
    }

    @Override
    public double evaluateLightResponseFactor(Ray lightSourceRay)
    {
        return 1.0;
    }

    @Override
    public Light copy()
    {
        DirectionalLight copy = new DirectionalLight(getPosition(), getEmission());
        copy.setId(getId());
        copy.setName(getName());
        return copy;
    }
}
