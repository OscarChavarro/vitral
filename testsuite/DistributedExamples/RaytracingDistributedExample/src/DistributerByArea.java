import vsdk.toolkit.gui.ProgressMonitor;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.render.Raytracer;

public class DistributerByArea
{
    private Raytracer visualizationEngine;

    public DistributerByArea()
    {
        visualizationEngine = new Raytracer();
    }

    public void
    distributedControl(RGBImage theResultingImage,
                       RendererConfiguration rendererConfiguration,
                       SimpleScene theScene,
                       ProgressMonitor reporter)
    {
        visualizationEngine.execute(theResultingImage,
                                    rendererConfiguration,
                                    theScene.getSimpleBodies(),
                                    theScene.getLights(),
                                    theScene.getActiveBackground(),
                                    theScene.getActiveCamera(),
                                    reporter, null,
                                    0, 0, 100, 100);
    }
}
