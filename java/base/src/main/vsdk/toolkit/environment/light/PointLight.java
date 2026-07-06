package vsdk.toolkit.environment.light;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Ray;

public final class PointLight extends Light
{
    public PointLight(Vector3Dd position, ColorRgb emission)
    {
        super(position, emission);
    }

    @Override
    public LightDirection getDirectionAndDistance(Vector3Dd surfacePoint)
    {
        Vector3Dd toLight = getPosition().subtract(surfacePoint);
        double distance = toLight.length();
        if ( distance <= VSDK.EPSILON ) {
            return new LightDirection(new Vector3Dd(0, 0, 0), 0.0);
        }
        double invDistance = 1.0 / distance;
        return new LightDirection(
            new Vector3Dd(toLight.x() * invDistance, toLight.y() * invDistance, toLight.z() * invDistance),
            distance - VSDK.EPSILON);
    }

    @Override
    public double evaluateLightResponseFactor(Ray lightSourceRay)
    {
        return 1.0;
    }

    @Override
    public Light copy()
    {
        PointLight copy = new PointLight(getPosition(), getEmission());
        copy.setId(getId());
        copy.setName(getName());
        return copy;
    }
}
