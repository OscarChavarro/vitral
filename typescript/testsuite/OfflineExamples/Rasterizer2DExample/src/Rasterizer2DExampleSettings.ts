/** Shared image-size parsing for the Rasterizer2D offline examples. */
export const DEFAULT_OUTPUT_WIDTH = 640;
export const DEFAULT_OUTPUT_HEIGHT = 480;

export function positiveArgument(args: string[], index: number, fallback: number): number {
    const value = Number.parseInt(args[index] ?? "", 10);
    return Number.isFinite(value) && value > 0 ? value : fallback;
}
