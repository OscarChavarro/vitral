package vsdk.toolkit.environment.scene;
import java.io.Serial;

// Java basic classes
import java.util.ArrayList;

// VSDK Classes
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.environment.background.Background;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.camera.CameraSnapshot;
import vsdk.toolkit.environment.light.Light;

public class SimpleScene extends Entity
{
    @Serial private static final long serialVersionUID = 20100901L;

    private ArrayList<SimpleBody> simpleBodies;
    private ArrayList<Light> lights;
    private ArrayList<Background> backgrounds;
    private ArrayList<Camera> cameras;
    private int activeCameraIndex;
    private int activeBackgroundIndex;

    public SimpleScene()
    {
        simpleBodies = new ArrayList<SimpleBody>();
        lights = new ArrayList<Light>();
        backgrounds = new ArrayList<Background>();
        cameras = new ArrayList<Camera>();
    }

    public int getActiveCameraIndex()
    {
        return activeCameraIndex;
    }

    public int getActiveBackgroundIndex()
    {
        return activeBackgroundIndex;
    }

    public void setActiveCameraIndex(int i)
    {
        activeCameraIndex = i;
    }

    public void setActiveBackgroundIndex(int i)
    {
        activeBackgroundIndex = i;
    }

    public void addBody(SimpleBody b)
    {
        simpleBodies.add(b);
    }

    public void addCamera(Camera c)
    {
        cameras.add(c);
    }

    public void addBackground(Background b)
    {
        backgrounds.add(b);
    }

    public void addLight(Light l)
    {
        l.setId(lights.size());
        lights.add(l);
    }

    public ArrayList<SimpleBody> getSimpleBodies()
    {
        return simpleBodies;
    }

    public ArrayList<Light> getLights()
    {
        return lights;
    }

    public ArrayList<Background> getBackgrounds()
    {
        return backgrounds;
    }

    public ArrayList<Camera> getCameras()
    {
        return cameras;
    }

    public void setSimpleBodies(ArrayList<SimpleBody> simpleBodies)
    {
        this.simpleBodies = simpleBodies;
    }

    public void setLights(ArrayList<Light> lights)
    {
        this.lights = lights;
        for ( int i = 0; i < this.lights.size(); i++ ) {
            this.lights.get(i).setId(i);
        }
    }

    public void setBackgrounds(ArrayList<Background> backgrounds)
    {
        this.backgrounds = backgrounds;
    }

    public Background getActiveBackground()
    {
        return backgrounds.get(activeBackgroundIndex);
    }

    public Camera getActiveCamera()
    {
        return cameras.get(activeCameraIndex);
    }

    public void setCameras(ArrayList<Camera> cameras)
    {
        this.cameras = cameras;
    }

    public SimpleSceneSnapshot exportToSimpleSceneSnapshot()
    {
        return exportToSimpleSceneSnapshot(
            getActiveCamera().exportToCameraSnapshot(),
            getActiveBackground());
    }

    public SimpleSceneSnapshot exportToSimpleSceneSnapshot(
        int viewportXSize,
        int viewportYSize)
    {
        return exportToSimpleSceneSnapshot(
            getActiveCamera().exportToCameraSnapshot(viewportXSize, viewportYSize),
            getActiveBackground());
    }

    public SimpleSceneSnapshot exportToSimpleSceneSnapshot(
        CameraSnapshot cameraSnapshot,
        Background background)
    {
        return new SimpleSceneSnapshot(
            simpleBodies,
            lights,
            background,
            cameraSnapshot);
    }
}
