package vsdk.toolkit.gui;

import java.util.concurrent.atomic.AtomicReference;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;

/**
Gizmo that represents a 3D ray (origin + direction) as a volumetric arrow.

The Arrow geometry is defined in object space along the +Z axis.  The
internal {@link SimpleBody} is positioned and rotated so that the arrow
points from {@code position} toward {@code position + direction}.

Thread-safety contract:
  - The network (or any non-render) thread calls {@link #setRay} to post a
    new (position, direction) pair.  The call is non-blocking and always
    stores the <em>latest</em> update; intermediate updates are discarded if
    the render thread has not consumed the previous one yet.
  - The render thread calls {@link #acquireSnapshot} once per frame.  If a
    new snapshot is pending it is applied to the {@link SimpleBody} and
    returned; otherwise {@code null} is returned and the body is unchanged.
  - During a single frame the body is never modified: it is only updated at
    the moment {@link #acquireSnapshot} is called, before any drawing begins.
*/
public class RayGizmo extends Gizmo {

    private static final double ARROW_BASE_LENGTH = 3.0;
    private static final double ARROW_HEAD_LENGTH = 1.0;
    private static final double ARROW_BASE_RADIUS = 0.15;
    private static final double ARROW_HEAD_RADIUS = 0.40;

    /** Immutable snapshot shared between network and render threads. */
    public static final class RaySnapshot {
        public final Vector3Dd position;
        public final Vector3Dd direction;

        public RaySnapshot(Vector3Dd position, Vector3Dd direction) {
            this.position = position;
            this.direction = direction;
        }
    }

    private final Arrow arrow;
    private final SimpleBody body;

    /** Latest update written by the network thread, consumed by the render thread. */
    private final AtomicReference<RaySnapshot> pendingSnapshot = new AtomicReference<>(null);

    /** Last snapshot applied to the body (render-thread only). */
    private Vector3Dd currentPosition;
    private Vector3Dd currentDirection;

    public RayGizmo() {
        arrow = new Arrow(ARROW_BASE_LENGTH, ARROW_HEAD_LENGTH,
                ARROW_BASE_RADIUS, ARROW_HEAD_RADIUS);

        body = new SimpleBody();
        body.setGeometry(arrow);

        SimpleMaterial mat = new SimpleMaterial();
        mat = mat.withAmbient(new ColorRgb(0.1, 0.0, 0.0));
        mat = mat.withDiffuse(new ColorRgb(0.9, 0.2, 0.1));
        mat = mat.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        mat = mat.withPhongExponent(32.0);
        body.setMaterial(mat);

        currentPosition = new Vector3Dd(0, 0, 0);
        currentDirection = new Vector3Dd(0, 0, 1);
        applyTransform(currentPosition, currentDirection);
    }

    /**
     * Returns the current ray origin (last applied value).
     * @return position
     */
    public Vector3Dd getPosition() {
        return currentPosition;
    }

    /**
     * Returns the current ray direction (last applied value).
     * @return direction
     */
    public Vector3Dd getDirection() {
        return currentDirection;
    }

    /**
     * Posts a new ray state to be consumed by the render thread on the next
     * frame.  May be called from any thread.  If called multiple times before
     * the render thread consumes the update, only the latest call is kept.
     *
     * @param position  ray origin in world space
     * @param direction ray direction in world space (need not be normalized)
     */
    public void setRay(Vector3Dd position, Vector3Dd direction) {
        if ( position == null || direction == null ) {
            return;
        }
        pendingSnapshot.set(new RaySnapshot(position, direction));
    }

    /**
     * Called once per frame by the render thread before drawing.  If a new
     * snapshot is pending it is consumed, the internal {@link SimpleBody} is
     * updated, and the snapshot is returned.  Returns {@code null} when
     * nothing changed.
     *
     * @return the applied snapshot, or {@code null} if the body was not changed
     */
    public RaySnapshot acquireSnapshot() {
        RaySnapshot snap = pendingSnapshot.getAndSet(null);
        if ( snap == null ) {
            return null;
        }
        applyTransform(snap.position, snap.direction);
        return snap;
    }

    /**
     * Returns the {@link SimpleBody} whose geometry and transform represent
     * the ray.  Only read this from the render thread (after
     * {@link #acquireSnapshot} has been called for the current frame).
     *
     * @return the body
     */
    public SimpleBody getBody() {
        return body;
    }

    // -----------------------------------------------------------------------

    private void applyTransform(Vector3Dd position, Vector3Dd direction) {
        currentPosition = position;
        currentDirection = direction;

        Matrix4x4d rotation = rotationFromZToDirection(direction);
        Matrix4x4d rotationInverse = rotation.invert();

        body.setPosition(position);
        body.setRotation(rotation);
        body.setRotationInverse(rotationInverse);
    }

    /**
     * Computes a rotation matrix that maps the +Z axis onto the given target
     * direction.  Handles the degenerate anti-parallel case by rotating 180°
     * around the X axis.
     */
    private static Matrix4x4d rotationFromZToDirection(Vector3Dd direction) {
        double len = direction.length();
        if ( len < VSDK.EPSILON ) {
            return new Matrix4x4d();
        }

        Vector3Dd d = direction.multiply(1.0 / len);
        Vector3Dd z = new Vector3Dd(0, 0, 1);

        double dot = z.dotProduct(d);

        if ( dot > 1.0 - VSDK.EPSILON ) {
            return new Matrix4x4d();
        }

        if ( dot < -1.0 + VSDK.EPSILON ) {
            // Anti-parallel: rotate 180° around X
            return new Matrix4x4d().axisRotation(Math.PI, 1, 0, 0);
        }

        Vector3Dd axis = z.crossProduct(d).normalized();
        double angle = Math.acos(dot);
        return new Matrix4x4d().axisRotation(angle, axis.x(), axis.y(), axis.z());
    }
}
