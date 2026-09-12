import type {
    ProgressMonitor,
    RGBImageUncompressed,
    RendererConfiguration,
    SimpleRaytracer,
    SimpleSceneSnapshot,
} from "@vitral/base";
import type { RaytracerExecutor } from "./RaytracerExecutor.js";

export class RaytracerSerialExecutor implements RaytracerExecutor {
    public run(
        visualizationEngine: SimpleRaytracer,
        resultingImage: RGBImageUncompressed,
        rendererConfiguration: RendererConfiguration,
        sceneSnapshot: SimpleSceneSnapshot,
        reporter: ProgressMonitor,
    ): void {
        visualizationEngine.execute(resultingImage, rendererConfiguration, sceneSnapshot, reporter, null);
    }
}
