import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.feedback.ProgressMonitor;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.raytracing.SimpleRaytracer;

final class RaytracerSerialExecutor implements RaytracerExecutor {
    @Override
    public void run(SimpleRaytracer visualizationEngine,
                    RGBImageUncompressed resultingImage,
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
