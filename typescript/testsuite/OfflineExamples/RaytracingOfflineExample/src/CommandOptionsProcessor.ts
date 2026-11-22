import { TriangleMeshAccelerationMethod, TriangleMeshAccelerationMethodUtil } from "@vitral/base";

/**
Package-private `final class` in Java; TypeScript has no package visibility, so
the class is exported and simply not part of any published package surface.
*/
export class CommandOptionsProcessor {
    private static readonly DEFAULT_SCENE_FILE = "../../../../etc/geometry/mitscenes/balls.ray";
    private static readonly DEFAULT_OUTPUT_FILE_NAME = "./output.ppm";

    private sceneFile: string = CommandOptionsProcessor.DEFAULT_SCENE_FILE;
    private outputFile: string = CommandOptionsProcessor.DEFAULT_OUTPUT_FILE_NAME;
    private save = true;
    private parallel = false;
    private statistics = false;
    private showHelp = false;
    private meshAccelerationMethod: TriangleMeshAccelerationMethod = TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH;

    private constructor() {}

    public static process(args: string[]): CommandOptionsProcessor {
        const options: CommandOptionsProcessor = new CommandOptionsProcessor();
        let positionalCount = 0;

        for (let i = 0; i < args.length; i++) {
            const arg: string = args[i]!;
            if ("nosave" === arg || "--nosave" === arg || "-n" === arg) {
                options.save = false;
                continue;
            }
            if ("-parallel" === arg || "--parallel" === arg) {
                options.parallel = true;
                continue;
            }
            if ("--stats" === arg) {
                options.statistics = true;
                continue;
            }
            if ("--help" === arg || "-h" === arg) {
                options.showHelp = true;
                continue;
            }
            if ("--scene" === arg || "-s" === arg) {
                if (i + 1 >= args.length) {
                    console.error("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                options.sceneFile = args[++i]!;
                continue;
            }
            if ("--mesh-acceleration" === arg || "-m" === arg) {
                if (i + 1 >= args.length) {
                    console.error("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                const value: string = args[++i]!;
                const method: TriangleMeshAccelerationMethod[] = [options.meshAccelerationMethod];
                if (!TriangleMeshAccelerationMethodUtil.fromName(value, method)) {
                    console.error("Unknown mesh acceleration method: " + value);
                    options.showHelp = true;
                    return options;
                }
                options.meshAccelerationMethod = method[0]!;
                continue;
            }
            if ("--output" === arg || "-o" === arg) {
                if (i + 1 >= args.length) {
                    console.error("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                options.outputFile = args[++i]!;
                continue;
            }
            if (arg.startsWith("-")) {
                console.error("Unknown option: " + arg);
                options.showHelp = true;
                return options;
            }

            if (positionalCount === 0) {
                options.sceneFile = arg;
            } else if (positionalCount === 1) {
                options.outputFile = arg;
            } else {
                console.error("Unexpected argument: " + arg);
                options.showHelp = true;
                return options;
            }
            positionalCount++;
        }

        return options;
    }

    public static printUsage(): void {
        console.log("Usage: RaytracerSimple [options] [scene_file]");
        console.log("Options:");
        console.log("  --scene, -s <file>     MIT scene file (.ray)");
        console.log("  --output, -o <file>    Output image file (.ppm/.png/.jpg)");
        console.log("  --nosave, -n           Render only, no image file");
        console.log("  -parallel, --parallel  Render tiles in parallel");
        console.log("  --mesh-acceleration, -m <method>");
        console.log("                         Triangle mesh ray acceleration:");
        console.log("                         bruteforce (none), wald2007 (binned SAH");
        console.log("                         BVH, default) or voxel (uniform grid,");
        console.log("                         RenderPark's voxel grid)");
        console.log("  --stats                Count rays and acceleration structure work");
        console.log("                         (RaytraceStatistics) and print a summary");
        console.log("  --help, -h             Show this help");
        console.log();
        console.log("Legacy compatibility:");
        console.log("  - `nosave` (without dashes) is still accepted.");
    }

    public getSceneFile(): string {
        return this.sceneFile;
    }

    public getOutputFile(): string {
        return this.outputFile;
    }

    public shouldSave(): boolean {
        return this.save;
    }

    public shouldUseParallelExecutor(): boolean {
        return this.parallel;
    }

    public shouldPrintStatistics(): boolean {
        return this.statistics;
    }

    public shouldShowHelp(): boolean {
        return this.showHelp;
    }

    public getMeshAccelerationMethod(): TriangleMeshAccelerationMethod {
        return this.meshAccelerationMethod;
    }
}
