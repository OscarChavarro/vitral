package vsdk.toolkit.render.raytracing;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.camera.CameraSnapshot;

/**
Converts the distances of the primary rays of a raytracer into the values of
a depth buffer (see `DepthBufferMode`). The OpenGL conversion follows the
same projection matrices used by `Camera.calculateViewVolumeMatrix`
(`Matrix4x4d.frustumProjection` and `Matrix4x4d.orthogonalProjection`, as
`glFrustum` and `glOrtho`), so a depth encoded here compares correctly, with
the usual `GL_LESS` / `GL_LEQUAL` depth functions, against the fragments of
geometry rasterized with that camera.

An instance is immutable and can be shared among rendering threads.
*/
public final class DepthBufferEncoder
{
    private final DepthBufferMode mode;
    private final int projectionMode;
    private final double nearPlaneDistance;
    private final double farPlaneDistance;
    private final double depthRangeNear;
    private final double depthRangeFar;
    private final Vector3Dd eyePosition;
    private final Vector3Dd front;

    /**
    Encoder with the default OpenGL depth range [0, 1].
    @param mode kind of depth buffer to produce
    @param camera camera that generates the primary rays
    */
    public DepthBufferEncoder(DepthBufferMode mode, CameraSnapshot camera)
    {
        this(mode, camera, 0.0, 1.0);
    }

    /**
    @param mode kind of depth buffer to produce
    @param camera camera that generates the primary rays
    @param depthRangeNear near value of `glDepthRange`
    @param depthRangeFar far value of `glDepthRange`
    */
    public DepthBufferEncoder(DepthBufferMode mode, CameraSnapshot camera,
                              double depthRangeNear, double depthRangeFar)
    {
        if ( mode == null || camera == null ) {
            throw new IllegalArgumentException("mode and camera can not be null");
        }
        if ( mode == DepthBufferMode.OPENGL_DEPTH &&
             !(camera.getNearPlaneDistance() < camera.getFarPlaneDistance()) ) {
            throw new IllegalArgumentException(
                "OPENGL_DEPTH needs a camera with near plane distance < far plane distance");
        }
        this.mode = mode;
        this.projectionMode = camera.getProjectionMode();
        this.nearPlaneDistance = camera.getNearPlaneDistance();
        this.farPlaneDistance = camera.getFarPlaneDistance();
        this.depthRangeNear = depthRangeNear;
        this.depthRangeFar = depthRangeFar;
        this.eyePosition = camera.getEyePosition();
        this.front = camera.getFront();
    }

    /**
    @return kind of depth buffer produced
    */
    public DepthBufferMode getMode()
    {
        return mode;
    }

    /**
    Encodes the depth of a primary ray.
    @param origin origin of the primary ray
    @param unitDirection unit length direction of the primary ray
    @param distance distance from the origin to the nearest hit, or
    `Double.POSITIVE_INFINITY` if nothing was hit
    @return depth value for the buffer
    */
    public float encode(Vector3Dd origin, Vector3Dd unitDirection, double distance)
    {
        if ( mode != DepthBufferMode.OPENGL_DEPTH ) {
            return (float)distance;
        }
        if ( Double.isInfinite(distance) || Double.isNaN(distance) ) {
            return (float)depthRangeFar;
        }
        double eyeDepth =
            (origin.x() - eyePosition.x()) * front.x() +
            (origin.y() - eyePosition.y()) * front.y() +
            (origin.z() - eyePosition.z()) * front.z() +
            distance * unitDirection.dotProduct(front);
        return (float)eyeDepthToWindowDepth(eyeDepth);
    }

    /**
    Converts a depth along the viewing direction of the camera (positive in
    front of the eye) into an OpenGL window depth, clamped to the depth range.
    @param eyeDepth distance from the eye to the point, measured along the
    front vector of the camera
    @return window space depth
    */
    public double eyeDepthToWindowDepth(double eyeDepth)
    {
        double n = nearPlaneDistance;
        double f = farPlaneDistance;
        double ndcZ;

        if ( projectionMode == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            // Row 3 of glOrtho applied to eye space z = -eyeDepth, w = 1
            ndcZ = (2.0 * eyeDepth - (f + n)) / (f - n);
        }
        else {
            // Row 3 of glFrustum over w = eyeDepth
            if ( eyeDepth <= 0.0 ) {
                return depthRangeNear;
            }
            ndcZ = ((f + n) / (f - n)) - (2.0 * f * n) / ((f - n) * eyeDepth);
        }
        if ( ndcZ < -1.0 ) {
            ndcZ = -1.0;
        }
        if ( ndcZ > 1.0 ) {
            ndcZ = 1.0;
        }
        return depthRangeNear + (depthRangeFar - depthRangeNear) * (ndcZ + 1.0) / 2.0;
    }
}
