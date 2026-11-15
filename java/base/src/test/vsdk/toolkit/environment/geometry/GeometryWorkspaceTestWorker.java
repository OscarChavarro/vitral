package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryQueryWorkspace;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.volume.Sphere;

/**
Port of the cpp test helper `GeometryWorkspaceTestWorker`. The cpp
`boundaryStorage` returns the address of the boundary storage; the Java form
returns the boundary list itself, whose identity plays that role.
*/
public class GeometryWorkspaceTestWorker implements Runnable {
    private final Sphere sphere;
    private final GeometryQueryWorkspace workspace;
    private boolean success;

    public GeometryWorkspaceTestWorker(Sphere value)
    {
        sphere = value;
        workspace = new GeometryQueryWorkspace();
        success = true;
    }

    @Override
    public void run()
    {
        final Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
        for ( int i = 0; i < 500; i++ ) {
            workspace.clear();
            workspace.hitResult().resetForDistanceOnly();
            if ( sphere.queryFirstHit(ray, new RayCrossingQuery(0,4), workspace.hitResult()) != RayFirstHitStatus.Hit ||
                workspace.hitResult().getHitDistance() != 1 ||
                sphere.classifyRay(ray, new RayCrossingQuery(0,4), workspace.solidResult()) != RaySolidQueryStatus.Supported ||
                workspace.solidResult().boundaries.size() != 2 ||
                workspace.solidResult().boundaries.get(1).t != 2 ) {
                success = false;
                return;
            }
        }
    }

    public boolean passed() { return success; }

    public ArrayList<RayBoundary> boundaryStorage()
    {
        return workspace.solidResult().boundaries;
    }
}
