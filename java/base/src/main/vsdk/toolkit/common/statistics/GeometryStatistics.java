package vsdk.toolkit.common.statistics;

import java.util.List;

public final class GeometryStatistics
{
    private long raySphereTests;
    private long raySphereTestsSucceeded;
    private long rayBoxTests;
    private long rayBoxTestsSucceeded;
    private long rayBlobTests;
    private long rayBlobTestsSucceeded;
    private long rayPlaneTests;
    private long rayPlaneTestsSucceeded;
    private long rayTriangleTests;
    private long rayTriangleTestsSucceeded;
    private long rayQuadricTests;
    private long rayQuadricTestsSucceeded;
    private long rayPolyTests;
    private long rayPolyTestsSucceeded;
    private long rayBicubicTests;
    private long rayBicubicTestsSucceeded;
    private long rayHtFieldTests;
    private long rayHtFieldTestsSucceeded;
    private long boundingRegionTests;
    private long boundingRegionTestsSucceeded;
    private long clippingRegionTests;
    private long clippingRegionTestsSucceeded;

    public GeometryStatistics()
    {
        reset();
    }

    // Mirrors Statistics(ArrayList<Statistics*>*): supports multi-thread ray
    // tracing, where GeometryStatistics parts are totaled after all worker
    // threads have been joined.
    public GeometryStatistics(List<GeometryStatistics> partsPerThread)
    {
        reset();
        if ( partsPerThread == null ) {
            return;
        }
        for ( GeometryStatistics part : partsPerThread ) {
            if ( part != null ) {
                raySphereTests += part.raySphereTests;
                raySphereTestsSucceeded += part.raySphereTestsSucceeded;
                rayBoxTests += part.rayBoxTests;
                rayBoxTestsSucceeded += part.rayBoxTestsSucceeded;
                rayBlobTests += part.rayBlobTests;
                rayBlobTestsSucceeded += part.rayBlobTestsSucceeded;
                rayPlaneTests += part.rayPlaneTests;
                rayPlaneTestsSucceeded += part.rayPlaneTestsSucceeded;
                rayTriangleTests += part.rayTriangleTests;
                rayTriangleTestsSucceeded += part.rayTriangleTestsSucceeded;
                rayQuadricTests += part.rayQuadricTests;
                rayQuadricTestsSucceeded += part.rayQuadricTestsSucceeded;
                rayPolyTests += part.rayPolyTests;
                rayPolyTestsSucceeded += part.rayPolyTestsSucceeded;
                rayBicubicTests += part.rayBicubicTests;
                rayBicubicTestsSucceeded += part.rayBicubicTestsSucceeded;
                rayHtFieldTests += part.rayHtFieldTests;
                rayHtFieldTestsSucceeded += part.rayHtFieldTestsSucceeded;
                boundingRegionTests += part.boundingRegionTests;
                boundingRegionTestsSucceeded += part.boundingRegionTestsSucceeded;
                clippingRegionTests += part.clippingRegionTests;
                clippingRegionTestsSucceeded += part.clippingRegionTestsSucceeded;
            }
        }
    }

    public long getRaySphereTests() { return raySphereTests; }
    public void incrementRaySphereTests() { ++raySphereTests; }
    public long getRaySphereTestsSucceeded() { return raySphereTestsSucceeded; }
    public void incrementRaySphereTestsSucceeded() { ++raySphereTestsSucceeded; }
    public long getRayBoxTests() { return rayBoxTests; }
    public void incrementRayBoxTests() { ++rayBoxTests; }
    public long getRayBoxTestsSucceeded() { return rayBoxTestsSucceeded; }
    public void incrementRayBoxTestsSucceeded() { ++rayBoxTestsSucceeded; }
    public long getRayBlobTests() { return rayBlobTests; }
    public void incrementRayBlobTests() { ++rayBlobTests; }
    public long getRayBlobTestsSucceeded() { return rayBlobTestsSucceeded; }
    public void incrementRayBlobTestsSucceeded() { ++rayBlobTestsSucceeded; }
    public long getRayPlaneTests() { return rayPlaneTests; }
    public void incrementRayPlaneTests() { ++rayPlaneTests; }
    public long getRayPlaneTestsSucceeded() { return rayPlaneTestsSucceeded; }
    public void incrementRayPlaneTestsSucceeded() { ++rayPlaneTestsSucceeded; }
    public long getRayTriangleTests() { return rayTriangleTests; }
    public void incrementRayTriangleTests() { ++rayTriangleTests; }
    public long getRayTriangleTestsSucceeded() { return rayTriangleTestsSucceeded; }
    public void incrementRayTriangleTestsSucceeded() { ++rayTriangleTestsSucceeded; }
    public long getRayQuadricTests() { return rayQuadricTests; }
    public void incrementRayQuadricTests() { ++rayQuadricTests; }
    public long getRayQuadricTestsSucceeded() { return rayQuadricTestsSucceeded; }
    public void incrementRayQuadricTestsSucceeded() { ++rayQuadricTestsSucceeded; }
    public long getRayPolyTests() { return rayPolyTests; }
    public void incrementRayPolyTests() { ++rayPolyTests; }
    public long getRayPolyTestsSucceeded() { return rayPolyTestsSucceeded; }
    public void incrementRayPolyTestsSucceeded() { ++rayPolyTestsSucceeded; }
    public long getRayBicubicTests() { return rayBicubicTests; }
    public void incrementRayBicubicTests() { ++rayBicubicTests; }
    public long getRayBicubicTestsSucceeded() { return rayBicubicTestsSucceeded; }
    public void incrementRayBicubicTestsSucceeded() { ++rayBicubicTestsSucceeded; }
    public long getRayHtFieldTests() { return rayHtFieldTests; }
    public void incrementRayHtFieldTests() { ++rayHtFieldTests; }
    public long getRayHtFieldTestsSucceeded() { return rayHtFieldTestsSucceeded; }
    public void incrementRayHtFieldTestsSucceeded() { ++rayHtFieldTestsSucceeded; }
    public long getBoundingRegionTests() { return boundingRegionTests; }
    public void incrementBoundingRegionTests() { ++boundingRegionTests; }
    public long getBoundingRegionTestsSucceeded() { return boundingRegionTestsSucceeded; }
    public void incrementBoundingRegionTestsSucceeded() { ++boundingRegionTestsSucceeded; }
    public long getClippingRegionTests() { return clippingRegionTests; }
    public void incrementClippingRegionTests() { ++clippingRegionTests; }
    public long getClippingRegionTestsSucceeded() { return clippingRegionTestsSucceeded; }
    public void incrementClippingRegionTestsSucceeded() { ++clippingRegionTestsSucceeded; }

    public void reset()
    {
        raySphereTests = 0L;
        raySphereTestsSucceeded = 0L;
        rayBoxTests = 0L;
        rayBoxTestsSucceeded = 0L;
        rayBlobTests = 0L;
        rayBlobTestsSucceeded = 0L;
        rayPlaneTests = 0L;
        rayPlaneTestsSucceeded = 0L;
        rayTriangleTests = 0L;
        rayTriangleTestsSucceeded = 0L;
        rayQuadricTests = 0L;
        rayQuadricTestsSucceeded = 0L;
        rayPolyTests = 0L;
        rayPolyTestsSucceeded = 0L;
        rayBicubicTests = 0L;
        rayBicubicTestsSucceeded = 0L;
        rayHtFieldTests = 0L;
        rayHtFieldTestsSucceeded = 0L;
        boundingRegionTests = 0L;
        boundingRegionTestsSucceeded = 0L;
        clippingRegionTests = 0L;
        clippingRegionTestsSucceeded = 0L;
    }
}
