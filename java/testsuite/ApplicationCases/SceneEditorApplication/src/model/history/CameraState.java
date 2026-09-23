package model.history;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;

/**
Placement and projection of a camera: eye position, reference frame (up,
front, left), focused point, projection mode, field of view, orthogonal zoom
and clipping planes. It is restored through the setters of `Camera`, so
whoever caches data derived from the camera sees it modified. The size of the
viewport the camera projects to is not part of the state: it belongs to the
viewport.

Vectors are compared with a tolerance: `Camera.updateVectors`, called each
time a camera is drawn, normalizes its frame again, which changes the last
bits of vectors nobody moved.
*/
public final class CameraState implements EntityTransformState
{
    /// Relative tolerance for vectors to be the same
    private static final double VECTOR_TOLERANCE = 1.0e-9;

    private final Camera camera;
    private final Vector3Dd position;
    private final Vector3Dd up;
    private final Vector3Dd front;
    private final Vector3Dd left;
    private final Vector3Dd focusedPosition;
    private final int projectionMode;
    private final double fov;
    private final double orthogonalZoom;
    private final double nearPlaneDistance;
    private final double farPlaneDistance;

    private CameraState(Camera camera)
    {
        this.camera = camera;
        this.position = camera.getPosition();
        this.up = camera.getUp();
        this.front = camera.getFront();
        this.left = camera.getLeft();
        this.focusedPosition = camera.getFocusedPosition();
        this.projectionMode = camera.getProjectionMode();
        this.fov = camera.getFov();
        this.orthogonalZoom = camera.getOrthogonalZoom();
        this.nearPlaneDistance = camera.getNearPlaneDistance();
        this.farPlaneDistance = camera.getFarPlaneDistance();
    }

    /**
    @param camera camera whose state is captured
    @return the current state of the camera
    */
    public static CameraState capture(Camera camera)
    {
        return new CameraState(camera);
    }

    @Override
    public Entity getEntity()
    {
        return camera;
    }

    @Override
    public void restore()
    {
        camera.setPosition(position);
        // Gives back the front direction and the focal distance...
        camera.setFocusedPositionDirect(focusedPosition);
        // ... and the exact frame, which is not recomputed from the former
        camera.setUpDirect(up);
        camera.setLeftDirect(left);
        camera.setProjectionMode(projectionMode);
        camera.setFov(fov);
        camera.setOrthogonalZoom(orthogonalZoom);
        camera.setNearPlaneDistance(nearPlaneDistance);
        camera.setFarPlaneDistance(farPlaneDistance);
        camera.updateVectors();
    }

    @Override
    public boolean isSameState(EntityTransformState other)
    {
        if ( !(other instanceof CameraState state) ) {
            return false;
        }
        return state.camera == camera &&
            sameVector(state.position, position) &&
            sameVector(state.up, up) &&
            sameVector(state.front, front) &&
            sameVector(state.left, left) &&
            sameVector(state.focusedPosition, focusedPosition) &&
            state.projectionMode == projectionMode &&
            Double.compare(state.fov, fov) == 0 &&
            Double.compare(state.orthogonalZoom, orthogonalZoom) == 0 &&
            Double.compare(state.nearPlaneDistance, nearPlaneDistance) == 0 &&
            Double.compare(state.farPlaneDistance, farPlaneDistance) == 0;
    }

    private static boolean sameVector(Vector3Dd a, Vector3Dd b)
    {
        double scale = Math.max(1.0, Math.max(a.length(), b.length()));

        return a.subtract(b).length() <= VECTOR_TOLERANCE * scale;
    }
}
