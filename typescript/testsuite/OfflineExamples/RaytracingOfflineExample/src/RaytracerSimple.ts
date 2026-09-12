// Java classes
import { platformPrintln } from "@vitral/base";
import type { InputStream } from "@vitral/base";
import type { ArrayList } from "@vitral/base";
import { FileInputStream } from "@vitral/fs";

// Vitral classes
import { RendererConfiguration } from "@vitral/base";
import { StopWatch } from "@vitral/base";
import { VSDK } from "@vitral/base";
import { RaytraceStatistics } from "@vitral/base";
import { RGBImageUncompressed } from "@vitral/base";
import type { Camera } from "@vitral/base";
import type { CameraSnapshot } from "@vitral/base";
import type { SimpleBody } from "@vitral/base";
import { SimpleScene } from "@vitral/base";
import type { SimpleSceneSnapshot } from "@vitral/base";
import { ProgressMonitorConsole } from "@vitral/base";
import { SimpleRaytracer } from "@vitral/base";
import { ReaderMitScene } from "@vitral/fs";

import { CommandOptionsProcessor } from "./CommandOptionsProcessor.js";
import { ImageExporter } from "./ImageExporter.js";
import type { RaytracerExecutor } from "./RaytracerExecutor.js";
import { RaytracerParallelExecutor } from "./RaytracerParallelExecutor.js";
import { RaytracerSerialExecutor } from "./RaytracerSerialExecutor.js";

export class RaytracerSimple {
    private static readonly SCENE_SAMPLES_PATH = "../../../../etc/geometry/mitscenes/";
    private static readonly ELAPSED_TIME_DECIMALS = 3;
    private static readonly EXIT_CODE_READ_ERROR = -1;
    private static readonly EXIT_CODE_IMAGE_ERROR = 1;
    private static readonly EXIT_CODE_ARGUMENT_ERROR = 2;

    private readonly scene: SimpleScene;

    public constructor() {
        this.scene = new SimpleScene();
    }

    private optimizeRendererConfigurationForScene(rendererConfiguration: RendererConfiguration): void {
        let hasTextures = false;
        let hasNormalMaps = false;
        const bodies: ArrayList<SimpleBody> = this.scene.getSimpleBodies();

        for (let i = 0; i < bodies.size(); i++) {
            const body: SimpleBody = bodies.get(i);
            if (body.getTexture() !== null) {
                hasTextures = true;
            }
            if (body.getNormalMap() !== null) {
                hasNormalMaps = true;
            }
            if (hasTextures && hasNormalMaps) {
                break;
            }
        }

        rendererConfiguration.setTexture(hasTextures);
        rendererConfiguration.setBumpMap(hasNormalMaps);
    }

    private async offlineExecution(
        fileName: string,
        save: boolean,
        outputFileName: string,
        parallel: boolean,
    ): Promise<void> {
        let resultingImage: RGBImageUncompressed;

        //- 1. Import the scene from scene description file to RAM -----
        console.log("Loading scene from " + fileName + ": ");
        let is: InputStream | null = null;
        try {
            is = new FileInputStream(fileName);
            const readerMitScene: ReaderMitScene = new ReaderMitScene();
            readerMitScene.importEnvironment(is, this.scene);
        } catch {
            console.error("Error reading " + fileName);
            console.error("There are scene samples on " + RaytracerSimple.SCENE_SAMPLES_PATH);
            process.exit(RaytracerSimple.EXIT_CODE_READ_ERROR);
        } finally {
            // `try (InputStream is = ...)`: the reader already closes the
            // stream on the success path, and this closes it on the error one.
            is?.close();
        }
        console.log("Scene loaded OK!");

        //- 2. Create an empty image --------------------------------------
        resultingImage = new RGBImageUncompressed();
        const activeCamera: Camera = this.scene.getActiveCamera();
        if (
            !resultingImage.initNoFill(
                Math.trunc(activeCamera.getViewportXSize()),
                Math.trunc(activeCamera.getViewportYSize()),
            )
        ) {
            console.error("Error creating image!");
            process.exit(RaytracerSimple.EXIT_CODE_IMAGE_ERROR);
        }

        //- 3. Process the image from the scene data structure -----------
        const reporter: ProgressMonitorConsole = new ProgressMonitorConsole();
        const rendererConfiguration: RendererConfiguration = new RendererConfiguration();
        this.optimizeRendererConfigurationForScene(rendererConfiguration);

        const visualizationEngine: SimpleRaytracer = new SimpleRaytracer();
        const cameraSnapshot: CameraSnapshot = activeCamera.exportToCameraSnapshot(
            resultingImage.getXSize(),
            resultingImage.getYSize(),
        );
        const sceneSnapshot: SimpleSceneSnapshot = this.scene.exportToSimpleSceneSnapshot(
            cameraSnapshot,
            this.scene.getActiveBackground(),
        );

        const clock: StopWatch = new StopWatch();
        const raytracerExecutor: RaytracerExecutor = parallel
            ? new RaytracerParallelExecutor(fileName, new URL("./RaytracerTileWorker.js", import.meta.url))
            : new RaytracerSerialExecutor();

        clock.start();
        await raytracerExecutor.run(
            visualizationEngine,
            resultingImage,
            rendererConfiguration,
            sceneSnapshot,
            reporter,
        );
        clock.stop();

        platformPrintln(
            "Image generated in " +
                VSDK.formatDouble(clock.getElapsedRealTime(), RaytracerSimple.ELAPSED_TIME_DECIMALS) +
                " seconds.",
        );
        RaytraceStatistics.printSummary();

        //- 4. Export resulting image to an image file --------------------
        if (save) {
            const imageExporter: ImageExporter = new ImageExporter();
            if (!imageExporter.export(outputFileName, resultingImage)) {
                console.error("Error saving output image!");
                process.exit(RaytracerSimple.EXIT_CODE_IMAGE_ERROR);
            }
        }
    }

    public static async main(args: string[]): Promise<void> {
        const instance: RaytracerSimple = new RaytracerSimple();
        const options: CommandOptionsProcessor = CommandOptionsProcessor.process(args);
        if (options.shouldShowHelp()) {
            CommandOptionsProcessor.printUsage();
            if (args.length > 0) {
                process.exit(RaytracerSimple.EXIT_CODE_ARGUMENT_ERROR);
            }
        }
        await instance.offlineExecution(
            options.getSceneFile(),
            options.shouldSave(),
            options.getOutputFile(),
            options.shouldUseParallelExecutor(),
        );
    }
}

await RaytracerSimple.main(process.argv.slice(2));
