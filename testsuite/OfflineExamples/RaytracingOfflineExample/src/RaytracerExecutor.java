import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.feedback.ProgressMonitor;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.SimpleRaytracer;

interface RaytracerExecutor {
    void run(SimpleRaytracer visualizationEngine,
             RGBImage resultingImage,
             RendererConfiguration rendererConfiguration,
             SimpleSceneSnapshot sceneSnapshot,
             ProgressMonitor reporter);
}
