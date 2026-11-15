package vsdk.toolkit.environment.geometry.volume;

/**
Height range of one square block of height field cells, used to skip
blocks a ray passes above or below.
*/
public class HeightFieldBlock {
    private double minimumHeight;
    private double maximumHeight;

    public HeightFieldBlock() { minimumHeight = 65536.0; maximumHeight = 0.0; }

    /** cpp copy constructor. */
    public HeightFieldBlock(HeightFieldBlock other) {
        minimumHeight = other.minimumHeight; maximumHeight = other.maximumHeight;
    }

    public double getMinimumHeight() { return minimumHeight; }
    public double getMaximumHeight() { return maximumHeight; }
    public void include(double height)
    {
        if (height < minimumHeight) minimumHeight = height;
        if (height > maximumHeight) maximumHeight = height;
    }
}
