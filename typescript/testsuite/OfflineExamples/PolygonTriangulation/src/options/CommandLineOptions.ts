// Basic Node classes
import { basename } from "node:path";

// VSDK classes
import { IllegalArgumentException, Integer, Polygon2D } from "@vitral/base";

// Application classes
import { PolygonModel } from "../model/PolygonModel.js";

export class CommandLineOptions {
    private static readonly INPUT_FILE_PATTERN: RegExp = /^example(\d+)\.polygon$/u;
    private static readonly DEFAULT_INPUT_FILE: string = "../../../../etc/polygons/example01.polygon";
    private static readonly DEFAULT_ZONE_WIDTH: number = 512;
    private static readonly DEFAULT_ZONE_HEIGHT: number = 512;

    private inputFileName: string;
    private outputFileName: string | null;
    private zoneWidth: number;
    private zoneHeight: number;
    private showHelp: boolean;

    public constructor() {
        this.inputFileName = CommandLineOptions.DEFAULT_INPUT_FILE;
        this.outputFileName = null;
        this.zoneWidth = CommandLineOptions.DEFAULT_ZONE_WIDTH;
        this.zoneHeight = CommandLineOptions.DEFAULT_ZONE_HEIGHT;
        this.showHelp = false;
    }

    public static parse(args: string[]): CommandLineOptions {
        const options: CommandLineOptions = new CommandLineOptions();
        let positionalIndex = 0;

        let i: number;
        for (i = 0; args !== null && i < args.length; i++) {
            const argument: string = args[i]!;

            if (argument === "--help" || argument === "-h") {
                options.showHelp = true;
                continue;
            }

            if (argument === "--input" || argument === "-i") {
                options.inputFileName = CommandLineOptions.requireValue(args, ++i, argument);
                continue;
            }

            if (argument === "--output" || argument === "-o") {
                options.outputFileName = CommandLineOptions.requireValue(args, ++i, argument);
                continue;
            }

            if (argument === "--zone-width") {
                options.zoneWidth = CommandLineOptions.parsePositiveInt(
                    CommandLineOptions.requireValue(args, ++i, argument),
                    argument,
                );
                continue;
            }

            if (argument === "--zone-height") {
                options.zoneHeight = CommandLineOptions.parsePositiveInt(
                    CommandLineOptions.requireValue(args, ++i, argument),
                    argument,
                );
                continue;
            }

            if (argument.startsWith("-")) {
                throw new IllegalArgumentException("Unknown option: " + argument);
            }

            if (positionalIndex === 0) {
                options.inputFileName = argument;
            } else if (positionalIndex === 1) {
                options.outputFileName = argument;
            } else {
                throw new IllegalArgumentException("Unexpected positional argument: " + argument);
            }
            positionalIndex++;
        }

        if (options.outputFileName === null || options.outputFileName.trim().length === 0) {
            options.outputFileName = CommandLineOptions.deriveOutputFileName(options.inputFileName);
        }

        return options;
    }

    public static printUsage(): void {
        console.log("Usage: PolygonTriangulation [options] [input_file] [output_file]");
        console.log("Options:");
        console.log("  --input, -i <file>       Polygon input file (.polygon)");
        console.log("  --output, -o <file>      PNG output file");
        console.log("  --zone-width <pixels>    Width of each image zone (default 512)");
        console.log("  --zone-height <pixels>   Height of each image zone (default 512)");
        console.log("  --help, -h               Show this help");
    }

    public toPolygonModel(polygon2D: Polygon2D): PolygonModel {
        return new PolygonModel(polygon2D, this.inputFileName, this.outputFileName!, this.zoneWidth, this.zoneHeight);
    }

    public getInputFileName(): string {
        return this.inputFileName;
    }

    public getOutputFileName(): string | null {
        return this.outputFileName;
    }

    public getZoneWidth(): number {
        return this.zoneWidth;
    }

    public getZoneHeight(): number {
        return this.zoneHeight;
    }

    public shouldShowHelp(): boolean {
        return this.showHelp;
    }

    private static requireValue(args: string[], index: number, option: string): string {
        if (args === null || index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index]!;
    }

    private static parsePositiveInt(value: string, option: string): number {
        const parsedValue: number = Integer.parseInt(value);
        if (parsedValue <= 0) {
            throw new IllegalArgumentException(option + " must be a positive integer: " + value);
        }
        return parsedValue;
    }

    private static deriveOutputFileName(inputFileName: string): string {
        const inputBaseName: string = basename(inputFileName);
        const matcher: RegExpMatchArray | null = CommandLineOptions.INPUT_FILE_PATTERN.exec(inputBaseName);
        if (matcher !== null) {
            return "output" + matcher[1]! + ".png";
        }
        return "output.png";
    }
}
