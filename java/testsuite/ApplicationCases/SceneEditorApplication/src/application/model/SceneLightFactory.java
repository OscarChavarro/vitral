package application.model;

import java.util.List;
import java.util.Random;

import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;

/**
Creates the point lights of the scene editor, following these rules:
  - A scene starts without lights.
  - Every new light is placed inside the view volume (frustum) of the camera
    of one of the visible viewports, so its gizmo can be seen when created.
  - The first light of the scene is white and is placed at a fixed screen
    position of the selected viewport.
  - Every other light gets a random light color and a random position, inside
    the frustum of a randomly chosen visible viewport.
This is a plain model class: it knows nothing about AWT or JOGL.
*/
public class SceneLightFactory
{
    /// Random positions keep this fraction of the viewport as margin, so the
    /// light gizmo is not cut by the viewport borders: |u|, |v| <= 0.4
    private static final double MAX_SCREEN_OFFSET = 0.4;
    /// The first light is placed at the upper left quadrant of the viewport
    private static final double FIRST_LIGHT_U = -0.25;
    private static final double FIRST_LIGHT_V = 0.25;
    /// Depth range for random lights, as a factor of the camera focal
    /// distance, so lights are created near the region being looked at
    private static final double MIN_DEPTH_FACTOR = 0.5;
    private static final double MAX_DEPTH_FACTOR = 1.5;
    /// Light colors have all their channels in [MIN_LIGHT_CHANNEL, 1]
    private static final double MIN_LIGHT_CHANNEL = 0.6;

    private final Random random;

    public SceneLightFactory()
    {
        this(new Random());
    }

    /**
    @param random source of randomness, injectable for reproducible results
    */
    public SceneLightFactory(Random random)
    {
        this.random = random;
    }

    /**
    Builds the next point light for a scene. The light is not added to the
    scene: that is up to the caller.
    @param existingLights lights currently in the scene, used to know if the
    new one is the first
    @param viewportSet viewports whose cameras define where a light can be
    created
    @return a new light, or null if there is no camera to place it in view
    */
    public PointLight createLight(List<Light> existingLights,
                                  ViewportSet viewportSet)
    {
        boolean first = existingLights.isEmpty();
        Viewport viewport = chooseViewport(viewportSet, first);

        if ( viewport == null ) {
            return null;
        }
        Camera camera = viewport.getActiveCamera();

        if ( first ) {
            return new PointLight(
                pointInFrustum(camera, FIRST_LIGHT_U, FIRST_LIGHT_V,
                    calculateFocalDistance(camera)),
                new ColorRgb(1, 1, 1));
        }
        double u = randomInRange(-MAX_SCREEN_OFFSET, MAX_SCREEN_OFFSET);
        double v = randomInRange(-MAX_SCREEN_OFFSET, MAX_SCREEN_OFFSET);
        double focal = calculateFocalDistance(camera);
        double depth = randomInRange(
            clampDepth(camera, focal * MIN_DEPTH_FACTOR),
            clampDepth(camera, focal * MAX_DEPTH_FACTOR));

        return new PointLight(pointInFrustum(camera, u, v, depth),
            createRandomLightColor());
    }

    /**
    @return a pastel color, with every channel in [MIN_LIGHT_CHANNEL, 1]
    */
    private ColorRgb createRandomLightColor()
    {
        return new ColorRgb(
            randomInRange(MIN_LIGHT_CHANNEL, 1.0),
            randomInRange(MIN_LIGHT_CHANNEL, 1.0),
            randomInRange(MIN_LIGHT_CHANNEL, 1.0));
    }

    /**
    The first light uses the selected viewport. Any other light uses a random
    one. Only visible (active) viewports are considered.
    @return the chosen viewport, or null if there are no visible viewports
    */
    private Viewport chooseViewport(ViewportSet viewportSet, boolean first)
    {
        Viewport selected = viewportSet.getSelectedViewport();

        if ( first && selected != null && selected.isActive() ) {
            return selected;
        }
        int activeCount = viewportSet.countActiveViewports();

        if ( activeCount < 1 ) {
            return null;
        }
        int chosen = first ? 0 : random.nextInt(activeCount);

        for ( Viewport viewport : viewportSet.getViewports() ) {
            if ( !viewport.isActive() ) continue;
            if ( chosen == 0 ) {
                return viewport;
            }
            chosen--;
        }
        return null;
    }

    private double calculateFocalDistance(Camera camera)
    {
        return camera.getFocusedPosition().subtract(camera.getPosition()).length();
    }

    private double clampDepth(Camera camera, double depth)
    {
        double near = camera.getNearPlaneDistance();
        double far = camera.getFarPlaneDistance();

        return Math.max(near, Math.min(far, depth));
    }

    /**
    Calculates the world point that projects at a given place of the viewport
    of a camera, at a given depth along the camera viewing direction. This is
    valid for perspective and orthogonal cameras, as it uses the camera's own
    ray generation, and a point with a depth between the near and far planes
    is inside the frustum by construction.
    @param camera camera whose view volume must contain the point
    @param u horizontal screen offset from the viewport center, in
    [-0.5, 0.5] (positive to the right)
    @param v vertical screen offset from the viewport center, in
    [-0.5, 0.5] (positive up)
    @param depth distance from the eye plane, along the viewing direction
    @return a point in world coordinates
    */
    private Vector3Dd pointInFrustum(Camera camera, double u, double v,
                                     double depth)
    {
        camera.updateVectors();

        double width = camera.getViewportXSize();
        double height = camera.getViewportYSize();
        int x = (int)Math.round(width * (0.5 + u));
        int y = (int)Math.round(height * (0.5 - v) - 1);
        Ray ray = camera.generateRay(x, y);
        Vector3Dd front = camera.getFront().normalized();
        double alongFront = ray.getDirection().dotProduct(front);

        return ray.getOrigin().add(ray.getDirection().multiply(depth / alongFront));
    }

    private double randomInRange(double min, double max)
    {
        return min + random.nextDouble() * (max - min);
    }
}
