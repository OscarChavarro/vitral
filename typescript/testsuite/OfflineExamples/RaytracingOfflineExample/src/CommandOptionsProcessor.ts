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
    private showHelp = false;

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

    public shouldShowHelp(): boolean {
        return this.showHelp;
    }
}
