import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.ProgressMonitor;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.Raytracer;

interface RaytracerExecutor {
    void run(Raytracer visualizationEngine,
             RGBImage resultingImage,
             RendererConfiguration rendererConfiguration,
             SimpleSceneSnapshot sceneSnapshot,
             ProgressMonitor reporter);
}
