/** Per-worker solid-texture counters, represented exactly as Java longs. */
export class SolidTextureStatistics {
    public callsToNoise = 0n;
    public callsToDNoise = 0n;
    public constructor(partsPerThread: readonly SolidTextureStatistics[] | null = null) {
        this.reset();
        if (partsPerThread !== null)
            for (const part of partsPerThread) {
                this.callsToNoise += part.callsToNoise;
                this.callsToDNoise += part.callsToDNoise;
            }
    }
    public reset(): void {
        this.callsToNoise = 0n;
        this.callsToDNoise = 0n;
    }
}
