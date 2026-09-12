import type {
    ProgressMonitor,
    RGBImageUncompressed,
    RendererConfiguration,
    SimpleRaytracer,
    SimpleSceneSnapshot,
} from "@vitral/base";

/**
Java declares `run` as `void`. The parallel implementation needs worker
threads, which Node only exposes asynchronously, so `run` may also answer a
promise here and `RaytracerSimple` awaits it. The serial implementation still
returns nothing, exactly as in Java.
*/
export interface RaytracerExecutor {
    run(
        visualizationEngine: SimpleRaytracer,
        resultingImage: RGBImageUncompressed,
        rendererConfiguration: RendererConfiguration,
        sceneSnapshot: SimpleSceneSnapshot,
        reporter: ProgressMonitor,
    ): void | Promise<void>;
}
