import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.feedback.ProgressMonitor;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.SimpleRaytracer;

interface RaytracerExecutor {
    void run(SimpleRaytracer visualizationEngine,
             RGBImageUncompressed resultingImage,
             RendererConfiguration rendererConfiguration,
             SimpleSceneSnapshot sceneSnapshot,
             ProgressMonitor reporter);
}
