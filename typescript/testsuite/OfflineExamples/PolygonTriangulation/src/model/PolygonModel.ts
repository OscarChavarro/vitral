// VSDK classes
import { Polygon2D } from "@vitral/base";

export class PolygonModel {
    private readonly polygon2D: Polygon2D;
    private readonly inputFileName: string;
    private readonly outputFileName: string;
    private readonly zoneWidth: number;
    private readonly zoneHeight: number;

    public constructor(
        polygon2D: Polygon2D,
        inputFileName: string,
        outputFileName: string,
        zoneWidth: number,
        zoneHeight: number,
    ) {
        if (polygon2D === null || polygon2D === undefined) {
            throw new Error("polygon2D");
        }
        if (inputFileName === null || inputFileName === undefined) {
            throw new Error("inputFileName");
        }
        if (outputFileName === null || outputFileName === undefined) {
            throw new Error("outputFileName");
        }
        this.polygon2D = polygon2D;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.zoneWidth = zoneWidth;
        this.zoneHeight = zoneHeight;
    }

    public getPolygon2D(): Polygon2D {
        return this.polygon2D;
    }

    public getInputFileName(): string {
        return this.inputFileName;
    }

    public getOutputFileName(): string {
        return this.outputFileName;
    }

    public getZoneWidth(): number {
        return this.zoneWidth;
    }

    public getZoneHeight(): number {
        return this.zoneHeight;
    }

    public getImageWidth(): number {
        return this.zoneWidth * 2;
    }

    public getImageHeight(): number {
        return this.zoneHeight;
    }
}
