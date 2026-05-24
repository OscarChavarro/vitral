//= References:                                                             =
//= [ISO10303-21] ISO 10303-21:2016. Industrial automation systems and      =
//=     integration -- Product data representation and exchange -- Part 21: =
//=     Implementation methods: Clear text encoding of the exchange         =
//=     structure.                                                          =
//= [ISO10303-242] ISO 10303-242:2020. Application protocol: Managed        =
//=     model-based 3D engineering (AP242).                                 =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.io.geometry.stepCad.writer;

import java.io.OutputStream;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.io.PersistenceElement;

/**
Exports a `PolyhedralBoundedSolid` to an ISO 10303-21 ASCII STEP file
targeting application protocol AP242 (managed model-based 3D engineering,
MIM long form).

This class orchestrates the export pipeline. The actual work is delegated
to internal collaborators in this package:

  - `_StepSolidValidator`           - pre-export manifold / volume checks.
  - `_StepEntityBuffer`             - entity id counter and DATA buffer.
  - `_StepGeometryEmitter`          - low-level geometric primitives.
  - `_StepUnitContextEmitter`       - units + tolerance + context.
  - `_StepTopologyEmitter`          - Mantyla B-Rep to STEP mapping.
  - `_StepProductStructureEmitter`  - AP242 product structure chain.
  - `_StepHeaderWriter`             - ISO 10303-21 file wrapper.

The unit of length is metre, with no SI prefix. The geometric tolerance
declared in the representation context is `BIG_EPSILON` of the active
numeric policy.
*/
public class StepWriter extends PersistenceElement {

    private StepWriter()
    {
    }

    /**
    Exports the given solid as an AP242 ASCII STEP file written to the
    provided output stream.

    @param solid solid to export; must satisfy `validateIntermediate`
        and have non-zero volume.
    @param outputStream destination stream; not closed by this method.
    @param productName product name to encode in the FILE_NAME and
        PRODUCT entities (use a stable identifier, not a file path).
    @throws Exception when validation fails or write fails.
    */
    public static void exportSolid(PolyhedralBoundedSolid solid,
                                   OutputStream outputStream,
                                   String productName)
        throws Exception
    {
        if ( solid == null ) {
            throw new IllegalArgumentException("solid is null");
        }
        if ( outputStream == null ) {
            throw new IllegalArgumentException("outputStream is null");
        }
        String safeName = (productName == null || productName.isBlank())
            ? "VitralSolid" : productName;

        _StepSolidValidator.validate(solid);

        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        double tolerance = numericContext.bigEpsilon();

        _StepEntityBuffer buffer = new _StepEntityBuffer();
        _StepGeometryEmitter geometry = new _StepGeometryEmitter(buffer);

        //- 1. Units, tolerance and geometric representation context -----
        _StepUnitContextEmitter unitContext =
            new _StepUnitContextEmitter(buffer);
        int contextId = unitContext.emit(tolerance);

        //- 2. Global axis placement -------------------------------------
        int axisOriginCpId = geometry.emitCartesianPoint(0.0, 0.0, 0.0);
        int axisZDirId = geometry.emitDirection(0.0, 0.0, 1.0);
        int axisXDirId = geometry.emitDirection(1.0, 0.0, 0.0);
        int globalAxisPlacementId = geometry.emitAxis2Placement3D(
            axisOriginCpId, axisZDirId, axisXDirId);

        //- 3. Mantyla B-Rep -> STEP topology + geometry -----------------
        _StepTopologyEmitter topology =
            new _StepTopologyEmitter(buffer, geometry);
        int manifoldSolidId = topology.emit(solid, safeName);

        //- 4. Shape representation linking topology to context ----------
        int shapeRepId = buffer.nextId();
        buffer.appendEntity(shapeRepId,
            "ADVANCED_BREP_SHAPE_REPRESENTATION('"
            + _StepEntityBuffer.escape(safeName)
            + "',(#" + manifoldSolidId + ",#" + globalAxisPlacementId
            + "),#" + contextId + ")");

        //- 5. AP242 product structure -----------------------------------
        _StepProductStructureEmitter product =
            new _StepProductStructureEmitter(buffer);
        product.emit(safeName, shapeRepId);

        //- 6. Wrap and write --------------------------------------------
        _StepHeaderWriter.writeHeader(outputStream, safeName);
        _StepHeaderWriter.writeDataSectionOpen(outputStream);
        buffer.writeTo(outputStream);
        _StepHeaderWriter.writeDataSectionClose(outputStream);
    }
}
