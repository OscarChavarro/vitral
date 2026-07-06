package vsdk.toolkit.environment.light;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Ray;

public final class SpotLight extends Light
{
    private final Vector3Dd pointsAt;
    private final double coefficient;
    private final double radius;
    private final double falloff;

    public SpotLight(
        Vector3Dd position,
        Vector3Dd pointsAt,
        ColorRgb emission,
        double coefficient,
        double radius,
        double falloff)
    {
        super(position, emission);
        this.pointsAt = pointsAt;
        this.coefficient = coefficient;
        this.radius = radius;
        this.falloff = falloff;
    }

    public Vector3Dd getPointsAt()
    {
        return pointsAt;
    }

    public double getCoefficient()
    {
        return coefficient;
    }

    public double getRadius()
    {
        return radius;
    }

    public double getFalloff()
    {
        return falloff;
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

    private static double cubicSpline(double low, double high, double pos)
    {
        if ( pos < low ) {
            return 0.0;
        }
        if ( pos > high ) {
            return 1.0;
        }
        if ( high == low ) {
            return 0.0;
        }

        double t = (pos - low) / (high - low);
        return (3 - 2 * t) * t * t;
    }

    @Override
    public double evaluateLightResponseFactor(Ray lightSourceRay)
    {
        Vector3Dd spotDirection = getPointsAt().subtract(getPosition());
        double len = spotDirection.length();
        if ( len <= 0.0 ) {
            return 0.0;
        }
        spotDirection = new Vector3Dd(
            spotDirection.x() / len, spotDirection.y() / len, spotDirection.z() / len);
        double cosTheta = -lightSourceRay.getDirection().dotProduct(spotDirection);
        if ( cosTheta <= 0.0 ) {
            return 0.0;
        }
        double attenuation = Math.pow(cosTheta, getCoefficient());
        if ( getRadius() > 0.0 ) {
            attenuation *= cubicSpline(getFalloff(), getRadius(), cosTheta);
        }
        return attenuation;
    }

    @Override
    public Light copy()
    {
        SpotLight copy = new SpotLight(
            getPosition(), pointsAt, getEmission(), coefficient, radius, falloff);
        copy.setId(getId());
        copy.setName(getName());
        return copy;
    }
}
