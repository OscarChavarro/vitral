package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.environment.geometry.element.RayCrossing;
import vsdk.toolkit.environment.geometry.element.RayCrossingKind;
import vsdk.toolkit.environment.geometry.element.RayCrossingSink;

/** Port of the cpp test helper `RayCrossingTestCollector`. */
public class RayCrossingTestCollector implements RayCrossingSink {
    public final ArrayList<Double> roots = new ArrayList<>();
    public final ArrayList<RayCrossingKind> kinds = new ArrayList<>();
    public final ArrayList<Geometry> geometries = new ArrayList<>();
    public final ArrayList<Integer> surfaceIndices = new ArrayList<>();
    public final ArrayList<Integer> instanceCounts = new ArrayList<>();

    public RayCrossingTestCollector() {}

    @Override
    public void crossing(RayCrossing event)
    {
        roots.add(event.t);
        kinds.add(event.kind);
        geometries.add(event.geometry);
        surfaceIndices.add((int)event.surfaceIndex);
        instanceCounts.add((int)event.getInstanceCount());
    }
}
