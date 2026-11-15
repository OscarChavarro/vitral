package vsdk.toolkit.environment.geometry.volume;

/**
Start or end, along a ray, of the influence sphere of one blob component.
*/
public class BlobInfluenceInterval {
    private boolean starting;

    private long component;

    private double bound;

    public BlobInfluenceInterval() { starting = true; component = 0; bound = 0; }

    public BlobInfluenceInterval(boolean isStart, long componentIndex, double distance) {
        starting = isStart; component = componentIndex; bound = distance;
    }

    public boolean isStart() { return starting; }

    public long getComponent() { return component; }

    public double getBound() { return bound; }
}
