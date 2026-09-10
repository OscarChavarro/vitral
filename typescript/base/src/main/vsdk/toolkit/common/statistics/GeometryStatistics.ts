/** Per-worker geometry-intersection counters, represented exactly as Java longs. */
export class GeometryStatistics {
    private raySphereTests = 0n;
    private raySphereTestsSucceeded = 0n;
    private rayBoxTests = 0n;
    private rayBoxTestsSucceeded = 0n;
    private rayBlobTests = 0n;
    private rayBlobTestsSucceeded = 0n;
    private rayPlaneTests = 0n;
    private rayPlaneTestsSucceeded = 0n;
    private rayTriangleTests = 0n;
    private rayTriangleTestsSucceeded = 0n;
    private rayQuadricTests = 0n;
    private rayQuadricTestsSucceeded = 0n;
    private rayPolyTests = 0n;
    private rayPolyTestsSucceeded = 0n;
    private rayBicubicTests = 0n;
    private rayBicubicTestsSucceeded = 0n;
    private rayHtFieldTests = 0n;
    private rayHtFieldTestsSucceeded = 0n;
    private boundingRegionTests = 0n;
    private boundingRegionTestsSucceeded = 0n;
    private clippingRegionTests = 0n;
    private clippingRegionTestsSucceeded = 0n;

    public constructor(partsPerThread: readonly GeometryStatistics[] | null = null) {
        this.reset();
        if (partsPerThread !== null) for (const part of partsPerThread) this.add(part);
    }
    public getRaySphereTests(): bigint {
        return this.raySphereTests;
    }
    public incrementRaySphereTests(): void {
        this.raySphereTests++;
    }
    public getRaySphereTestsSucceeded(): bigint {
        return this.raySphereTestsSucceeded;
    }
    public incrementRaySphereTestsSucceeded(): void {
        this.raySphereTestsSucceeded++;
    }
    public getRayBoxTests(): bigint {
        return this.rayBoxTests;
    }
    public incrementRayBoxTests(): void {
        this.rayBoxTests++;
    }
    public getRayBoxTestsSucceeded(): bigint {
        return this.rayBoxTestsSucceeded;
    }
    public incrementRayBoxTestsSucceeded(): void {
        this.rayBoxTestsSucceeded++;
    }
    public getRayBlobTests(): bigint {
        return this.rayBlobTests;
    }
    public incrementRayBlobTests(): void {
        this.rayBlobTests++;
    }
    public getRayBlobTestsSucceeded(): bigint {
        return this.rayBlobTestsSucceeded;
    }
    public incrementRayBlobTestsSucceeded(): void {
        this.rayBlobTestsSucceeded++;
    }
    public getRayPlaneTests(): bigint {
        return this.rayPlaneTests;
    }
    public incrementRayPlaneTests(): void {
        this.rayPlaneTests++;
    }
    public getRayPlaneTestsSucceeded(): bigint {
        return this.rayPlaneTestsSucceeded;
    }
    public incrementRayPlaneTestsSucceeded(): void {
        this.rayPlaneTestsSucceeded++;
    }
    public getRayTriangleTests(): bigint {
        return this.rayTriangleTests;
    }
    public incrementRayTriangleTests(): void {
        this.rayTriangleTests++;
    }
    public getRayTriangleTestsSucceeded(): bigint {
        return this.rayTriangleTestsSucceeded;
    }
    public incrementRayTriangleTestsSucceeded(): void {
        this.rayTriangleTestsSucceeded++;
    }
    public getRayQuadricTests(): bigint {
        return this.rayQuadricTests;
    }
    public incrementRayQuadricTests(): void {
        this.rayQuadricTests++;
    }
    public getRayQuadricTestsSucceeded(): bigint {
        return this.rayQuadricTestsSucceeded;
    }
    public incrementRayQuadricTestsSucceeded(): void {
        this.rayQuadricTestsSucceeded++;
    }
    public getRayPolyTests(): bigint {
        return this.rayPolyTests;
    }
    public incrementRayPolyTests(): void {
        this.rayPolyTests++;
    }
    public getRayPolyTestsSucceeded(): bigint {
        return this.rayPolyTestsSucceeded;
    }
    public incrementRayPolyTestsSucceeded(): void {
        this.rayPolyTestsSucceeded++;
    }
    public getRayBicubicTests(): bigint {
        return this.rayBicubicTests;
    }
    public incrementRayBicubicTests(): void {
        this.rayBicubicTests++;
    }
    public getRayBicubicTestsSucceeded(): bigint {
        return this.rayBicubicTestsSucceeded;
    }
    public incrementRayBicubicTestsSucceeded(): void {
        this.rayBicubicTestsSucceeded++;
    }
    public getRayHtFieldTests(): bigint {
        return this.rayHtFieldTests;
    }
    public incrementRayHtFieldTests(): void {
        this.rayHtFieldTests++;
    }
    public getRayHtFieldTestsSucceeded(): bigint {
        return this.rayHtFieldTestsSucceeded;
    }
    public incrementRayHtFieldTestsSucceeded(): void {
        this.rayHtFieldTestsSucceeded++;
    }
    public getBoundingRegionTests(): bigint {
        return this.boundingRegionTests;
    }
    public incrementBoundingRegionTests(): void {
        this.boundingRegionTests++;
    }
    public getBoundingRegionTestsSucceeded(): bigint {
        return this.boundingRegionTestsSucceeded;
    }
    public incrementBoundingRegionTestsSucceeded(): void {
        this.boundingRegionTestsSucceeded++;
    }
    public getClippingRegionTests(): bigint {
        return this.clippingRegionTests;
    }
    public incrementClippingRegionTests(): void {
        this.clippingRegionTests++;
    }
    public getClippingRegionTestsSucceeded(): bigint {
        return this.clippingRegionTestsSucceeded;
    }
    public incrementClippingRegionTestsSucceeded(): void {
        this.clippingRegionTestsSucceeded++;
    }
    public reset(): void {
        this.raySphereTests =
            this.raySphereTestsSucceeded =
            this.rayBoxTests =
            this.rayBoxTestsSucceeded =
            this.rayBlobTests =
            this.rayBlobTestsSucceeded =
            this.rayPlaneTests =
            this.rayPlaneTestsSucceeded =
            this.rayTriangleTests =
            this.rayTriangleTestsSucceeded =
            this.rayQuadricTests =
            this.rayQuadricTestsSucceeded =
            this.rayPolyTests =
            this.rayPolyTestsSucceeded =
            this.rayBicubicTests =
            this.rayBicubicTestsSucceeded =
            this.rayHtFieldTests =
            this.rayHtFieldTestsSucceeded =
            this.boundingRegionTests =
            this.boundingRegionTestsSucceeded =
            this.clippingRegionTests =
            this.clippingRegionTestsSucceeded =
                0n;
    }
    private add(part: GeometryStatistics): void {
        this.raySphereTests += part.raySphereTests;
        this.raySphereTestsSucceeded += part.raySphereTestsSucceeded;
        this.rayBoxTests += part.rayBoxTests;
        this.rayBoxTestsSucceeded += part.rayBoxTestsSucceeded;
        this.rayBlobTests += part.rayBlobTests;
        this.rayBlobTestsSucceeded += part.rayBlobTestsSucceeded;
        this.rayPlaneTests += part.rayPlaneTests;
        this.rayPlaneTestsSucceeded += part.rayPlaneTestsSucceeded;
        this.rayTriangleTests += part.rayTriangleTests;
        this.rayTriangleTestsSucceeded += part.rayTriangleTestsSucceeded;
        this.rayQuadricTests += part.rayQuadricTests;
        this.rayQuadricTestsSucceeded += part.rayQuadricTestsSucceeded;
        this.rayPolyTests += part.rayPolyTests;
        this.rayPolyTestsSucceeded += part.rayPolyTestsSucceeded;
        this.rayBicubicTests += part.rayBicubicTests;
        this.rayBicubicTestsSucceeded += part.rayBicubicTestsSucceeded;
        this.rayHtFieldTests += part.rayHtFieldTests;
        this.rayHtFieldTestsSucceeded += part.rayHtFieldTestsSucceeded;
        this.boundingRegionTests += part.boundingRegionTests;
        this.boundingRegionTestsSucceeded += part.boundingRegionTestsSucceeded;
        this.clippingRegionTests += part.clippingRegionTests;
        this.clippingRegionTestsSucceeded += part.clippingRegionTestsSucceeded;
    }
}
