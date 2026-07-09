package vsdk.toolkit.io.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;

public class ReaderPlyResult {
    public TriangleMesh triangleMesh;
    public ArrayList<Vector3Dd> pointCloud;

    public ReaderPlyResult() {
        triangleMesh = null;
        pointCloud = new ArrayList<Vector3Dd>();
    }
}
