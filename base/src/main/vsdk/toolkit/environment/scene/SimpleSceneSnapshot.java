package vsdk.toolkit.environment.scene;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.environment.Light;

/**
Immutable scene container used by the raytracer for a consistent render pass.
*/
public final class SimpleSceneSnapshot extends Entity
{
    @Serial private static final long serialVersionUID = 20260421L;

    private final List<SimpleBody> simpleBodies;
    private final List<Light> lights;
    private final Background background;
    private final CameraSnapshot cameraSnapshot;

    public SimpleSceneSnapshot(
        List<SimpleBody> simpleBodies,
        List<Light> lights,
        Background background,
        CameraSnapshot cameraSnapshot)
    {
        this.simpleBodies = Collections.unmodifiableList(
            new ArrayList<SimpleBody>(
                Objects.requireNonNull(
                    simpleBodies,
                    "simpleBodies cannot be null")));
        this.lights = Collections.unmodifiableList(
            new ArrayList<Light>(
                Objects.requireNonNull(
                    lights,
                    "lights cannot be null")));
        this.background =
            Objects.requireNonNull(background, "background cannot be null");
        this.cameraSnapshot =
            Objects.requireNonNull(cameraSnapshot, "cameraSnapshot cannot be null");
    }

    public List<SimpleBody> getSimpleBodies()
    {
        return simpleBodies;
    }

    public List<Light> getLights()
    {
        return lights;
    }

    public Background getBackground()
    {
        return background;
    }

    public CameraSnapshot getCameraSnapshot()
    {
        return cameraSnapshot;
    }
}
