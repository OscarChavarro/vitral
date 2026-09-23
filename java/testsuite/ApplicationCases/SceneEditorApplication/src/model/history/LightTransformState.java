package model.history;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.light.Light;

/**
Position of a light (the placement the editor can change).
*/
public final class LightTransformState implements EntityTransformState
{
    private final Light light;
    private final Vector3Dd position;

    private LightTransformState(Light light)
    {
        this.light = light;
        this.position = light.getPosition();
    }

    /**
    @param light light whose placement is captured
    @return the current placement of the light
    */
    public static LightTransformState capture(Light light)
    {
        return new LightTransformState(light);
    }

    @Override
    public Entity getEntity()
    {
        return light;
    }

    @Override
    public void restore()
    {
        light.setPosition(position);
    }

    @Override
    public boolean isSameState(EntityTransformState other)
    {
        if ( !(other instanceof LightTransformState state) ) {
            return false;
        }
        return state.light == light &&
            (position == null ? state.position == null : position.equals(state.position));
    }
}
