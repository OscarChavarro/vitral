package vsdk.toolkit.environment.geometry.volume;

/**
Per-ray stepping state of the height field first-hit traversal: direction
signs, the dominant axis and the slopes between ray coordinates.
*/
public class HeightFieldTraversal {
    private final int isdx;
    private final int isdz;
    private final boolean xDom;
    private final double gdx;
    private final double gdy;
    private final double gdz;
    private final double myx;
    private final double mxz;
    private final double mzx;
    private final double myz;
    private double depth;

    public HeightFieldTraversal(int isdxValue, int isdzValue, boolean xDomValue, double gdxValue,
        double gdyValue, double gdzValue, double myxValue, double mxzValue,
        double mzxValue, double myzValue)
    {
        isdx = isdxValue; isdz = isdzValue; xDom = xDomValue; gdx = gdxValue; gdy = gdyValue;
        gdz = gdzValue; myx = myxValue; mxz = mxzValue; mzx = mzxValue; myz = myzValue; depth = 0;
    }

    public int getIsdx() { return isdx; }
    public int getIsdz() { return isdz; }
    public boolean getXDom() { return xDom; }
    public double getGdx() { return gdx; }
    public double getGdy() { return gdy; }
    public double getGdz() { return gdz; }
    public double getMyx() { return myx; }
    public double getMxz() { return mxz; }
    public double getMzx() { return mzx; }
    public double getMyz() { return myz; }
    // Ray parameter of the hit found by the traversal.
    public double getDepth() { return depth; }
    public void setDepth(double value) { depth = value; }
}
