package vsdk.toolkit.io.geometry.stepCad.writer;

import static vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer.fmt;

/**
Emits the low-level ISO 10303-21 geometric primitives shared by the
representation context, the face surfaces and the edge curves:
CARTESIAN_POINT, DIRECTION, VECTOR, LINE, VERTEX_POINT,
AXIS2_PLACEMENT_3D, PLANE.

This is an internal collaborator of `StepWriter`.
*/
public class _StepGeometryEmitter {

    private final _StepEntityBuffer buffer;

    public _StepGeometryEmitter(_StepEntityBuffer buffer)
    {
        this.buffer = buffer;
    }

    public int emitCartesianPoint(double x, double y, double z)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "CARTESIAN_POINT('',(" + fmt(x) + "," + fmt(y) + ","
            + fmt(z) + "))");
        return id;
    }

    public int emitDirection(double x, double y, double z)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "DIRECTION('',(" + fmt(x) + "," + fmt(y) + "," + fmt(z) + "))");
        return id;
    }

    public int emitVector(int directionId, double magnitude)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "VECTOR('',#" + directionId + "," + fmt(magnitude) + ")");
        return id;
    }

    public int emitLine(int pointId, int vectorId)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "LINE('',#" + pointId + ",#" + vectorId + ")");
        return id;
    }

    public int emitVertexPoint(int cartesianPointId)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "VERTEX_POINT('',#" + cartesianPointId + ")");
        return id;
    }

    public int emitAxis2Placement3D(int originCpId, int zDirId, int xDirId)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "AXIS2_PLACEMENT_3D('',#" + originCpId + ",#"
            + zDirId + ",#" + xDirId + ")");
        return id;
    }

    public int emitPlane(int axisPlacementId)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "PLANE('',#" + axisPlacementId + ")");
        return id;
    }
}
