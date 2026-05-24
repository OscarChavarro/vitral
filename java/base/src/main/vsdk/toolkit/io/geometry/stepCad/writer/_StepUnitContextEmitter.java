package vsdk.toolkit.io.geometry.stepCad.writer;

import static vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer.fmt;

import vsdk.toolkit.io.geometry.stepCad.StepLengthUnit;

/**
Emits the AP242 unit + tolerance + geometric representation context
cluster: SI_UNIT for the requested length unit, radian/steradian, the
UNCERTAINTY_MEASURE_WITH_UNIT carrying the kernel's BIG_EPSILON (scaled to
the requested unit), and the composite GEOMETRIC_REPRESENTATION_CONTEXT.

This is an internal collaborator of `StepWriter`.
*/
public class _StepUnitContextEmitter {

    private final _StepEntityBuffer buffer;
    private final StepLengthUnit lengthUnit;

    private int lengthUnitId;
    private int planeAngleUnitId;
    private int solidAngleUnitId;
    private int uncertaintyId;
    private int contextId;

    public _StepUnitContextEmitter(_StepEntityBuffer buffer,
                                   StepLengthUnit lengthUnit)
    {
        this.buffer = buffer;
        this.lengthUnit = lengthUnit;
    }

    /**
    Emits the full unit/context cluster and returns the id of the
    GEOMETRIC_REPRESENTATION_CONTEXT entity, which other entities
    reference.
    @param toleranceMeters the distance tolerance (BIG_EPSILON), in metres.
        The value is automatically scaled to the configured length unit.
    @return the geometric representation context entity id.
    */
    public int emit(double toleranceMeters)
    {
        lengthUnitId = emitLengthUnit();
        planeAngleUnitId = emitPlaneAngleUnit();
        solidAngleUnitId = emitSolidAngleUnit();
        uncertaintyId = emitUncertainty(toleranceMeters * lengthUnit.metreScale);
        contextId = emitGeometricRepresentationContext();
        return contextId;
    }

    public int getContextId()
    {
        return contextId;
    }

    //=================================================================

    private int emitLengthUnit()
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "( LENGTH_UNIT() NAMED_UNIT(*) SI_UNIT("
            + lengthUnit.siPrefix + ",.METRE.) )");
        return id;
    }

    private int emitPlaneAngleUnit()
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "( NAMED_UNIT(*) PLANE_ANGLE_UNIT() SI_UNIT($,.RADIAN.) )");
        return id;
    }

    private int emitSolidAngleUnit()
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "( NAMED_UNIT(*) SI_UNIT($,.STERADIAN.) SOLID_ANGLE_UNIT() )");
        return id;
    }

    private int emitUncertainty(double toleranceMeters)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "UNCERTAINTY_MEASURE_WITH_UNIT(LENGTH_MEASURE(" + fmt(toleranceMeters)
            + "),#" + lengthUnitId
            + ",'DISTANCE_ACCURACY_VALUE',"
            + "'Tolerance derived from PolyhedralBoundedSolid BIG_EPSILON')");
        return id;
    }

    private int emitGeometricRepresentationContext()
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "( GEOMETRIC_REPRESENTATION_CONTEXT(3) "
            + "GLOBAL_UNCERTAINTY_ASSIGNED_CONTEXT((#" + uncertaintyId + ")) "
            + "GLOBAL_UNIT_ASSIGNED_CONTEXT((#" + lengthUnitId + ",#"
            + planeAngleUnitId + ",#" + solidAngleUnitId + ")) "
            + "REPRESENTATION_CONTEXT('Vitral','3D') )");
        return id;
    }
}
