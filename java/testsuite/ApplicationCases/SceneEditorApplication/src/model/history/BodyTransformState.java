package model.history;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.scene.SimpleBody;

/**
Position, orientation and scale of a body. Vectors and matrices of vitral are
immutable, so they are kept without copying them.
*/
public final class BodyTransformState implements EntityTransformState
{
    private final SimpleBody body;
    private final Vector3Dd position;
    private final Matrix4x4d rotation;
    private final Vector3Dd scale;

    private BodyTransformState(SimpleBody body)
    {
        this.body = body;
        this.position = body.getPosition();
        this.rotation = body.getRotation();
        this.scale = body.getScale();
    }

    /**
    @param body body whose placement is captured
    @return the current placement of the body
    */
    public static BodyTransformState capture(SimpleBody body)
    {
        return new BodyTransformState(body);
    }

    @Override
    public Entity getEntity()
    {
        return body;
    }

    @Override
    public void restore()
    {
        body.setScale(scale);
        if ( rotation != null ) {
            // The inverse rotation is derived from this one
            body.setRotation(rotation);
        }
        body.setPosition(position);
    }

    @Override
    public boolean isSameState(EntityTransformState other)
    {
        if ( !(other instanceof BodyTransformState state) ) {
            return false;
        }
        return state.body == body &&
            equal(state.position, position) &&
            equal(state.rotation, rotation) &&
            equal(state.scale, scale);
    }

    private static boolean equal(Object a, Object b)
    {
        return a == null ? b == null : a.equals(b);
    }
}
