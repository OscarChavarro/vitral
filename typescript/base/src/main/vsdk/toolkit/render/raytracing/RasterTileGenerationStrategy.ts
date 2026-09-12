/**
Port of the Java enum `RasterTileGenerationStrategy`. Members are string
constants so that a strategy survives a structured-clone hop to a worker.
*/
export enum RasterTileGenerationStrategy {
    LINEAR = "LINEAR",
    SERIAL = "SERIAL",
}
