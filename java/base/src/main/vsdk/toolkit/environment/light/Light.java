package vsdk.toolkit.environment.light;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Ray;

public abstract class Light extends Entity
{
    private Vector3Dd position;
    private ColorRgb emission;
    private int id;

    /// This string should be used for specific application defined
    /// functionality. Can be null.
    private String name;

    protected Light(Vector3Dd position, ColorRgb emission)
    {
        this.position = position;
        this.emission = emission;
        this.id = 0;
        this.name = "";
    }

    public String getName()
    {
        return name;
    }

    public void setName(String n)
    {
        name = n;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int i)
    {
        id = i;
    }

    public Vector3Dd getPosition()
    {
        return position;
    }

    public void setPosition(Vector3Dd position)
    {
        this.position = position;
    }

    public ColorRgb getEmission()
    {
        return emission;
    }

    public void setEmission(ColorRgb emission)
    {
        this.emission = emission;
    }

    /**
    True only for AmbientLight: shaders must add its contribution directly
    from getEmission() and skip shadow sampling entirely.
    @return whether this light is an ambient light
    */
    public boolean isAmbient()
    {
        return false;
    }

    /**
    Surface point -> light direction (normalized) and the shadow ray
    distance limit past which occluders no longer count (a large sentinel
    for lights with no finite position, e.g. DirectionalLight).
    @param surfacePoint point being shaded
    @return direction toward the light and the shadow ray distance limit
    */
    public abstract LightDirection getDirectionAndDistance(Vector3Dd surfacePoint);

    /**
    Attenuation contract carried over from the povCpp light hierarchy:
    lightSourceRay direction points from the surface toward the light,
    normalized.
    @param lightSourceRay surface-to-light ray
    @return attenuation factor in [0, 1] to multiply into the light's
    contribution
    */
    public abstract double evaluateLightResponseFactor(Ray lightSourceRay);

    public abstract Light copy();

    public record LightDirection(Vector3Dd direction, double maxShadowDistance)
    {
    }
}
