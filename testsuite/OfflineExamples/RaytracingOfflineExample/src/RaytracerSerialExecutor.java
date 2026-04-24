import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.ProgressMonitor;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.SimpleRaytracer;

final class RaytracerSerialExecutor implements RaytracerExecutor {
    @Override
    public void run(SimpleRaytracer visualizationEngine,
                    RGBImage resultingImage,
                    RendererConfiguration rendererConfiguration,
                    SimpleSceneSnapshot sceneSnapshot,
                    ProgressMonitor reporter)
    {
        visualizationEngine.execute(
            resultingImage,
            rendererConfiguration,
            sceneSnapshot,
            reporter,
            null);
    }
}
