package vsdk.toolkit.io.geometry.stepCad.writer;

import static vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer.escape;

/**
Emits the AP242 product structure chain that wraps the
ADVANCED_BREP_SHAPE_REPRESENTATION: APPLICATION_CONTEXT,
APPLICATION_PROTOCOL_DEFINITION, PRODUCT_CONTEXT, PRODUCT,
PRODUCT_RELATED_PRODUCT_CATEGORY, PRODUCT_DEFINITION_FORMATION,
PRODUCT_DEFINITION_CONTEXT, PRODUCT_DEFINITION,
PRODUCT_DEFINITION_SHAPE, SHAPE_DEFINITION_REPRESENTATION.

This is an internal collaborator of `StepWriter`.
*/
public class _StepProductStructureEmitter {

    private final _StepEntityBuffer buffer;

    public _StepProductStructureEmitter(_StepEntityBuffer buffer)
    {
        this.buffer = buffer;
    }

    public void emit(String productName, int shapeRepId)
    {
        int appContextId = buffer.nextId();
        buffer.appendEntity(appContextId,
            "APPLICATION_CONTEXT("
            + "'managed model-based 3D engineering')");

        int appProtocolId = buffer.nextId();
        buffer.appendEntity(appProtocolId,
            "APPLICATION_PROTOCOL_DEFINITION("
            + "'international standard',"
            + "'ap242_managed_model_based_3d_engineering_mim_lf',"
            + "2020,#" + appContextId + ")");

        int productContextId = buffer.nextId();
        buffer.appendEntity(productContextId,
            "PRODUCT_CONTEXT('',#" + appContextId + ",'mechanical')");

        int productId = buffer.nextId();
        buffer.appendEntity(productId,
            "PRODUCT('" + escape(productName) + "','"
            + escape(productName) + "','',(#" + productContextId + "))");

        int productCategoryId = buffer.nextId();
        buffer.appendEntity(productCategoryId,
            "PRODUCT_RELATED_PRODUCT_CATEGORY('part',$,(#"
            + productId + "))");

        int formationId = buffer.nextId();
        buffer.appendEntity(formationId,
            "PRODUCT_DEFINITION_FORMATION('','',#" + productId + ")");

        int productDefContextId = buffer.nextId();
        buffer.appendEntity(productDefContextId,
            "PRODUCT_DEFINITION_CONTEXT('part definition',#"
            + appContextId + ",'design')");

        int productDefId = buffer.nextId();
        buffer.appendEntity(productDefId,
            "PRODUCT_DEFINITION('','',#" + formationId + ",#"
            + productDefContextId + ")");

        int productDefShapeId = buffer.nextId();
        buffer.appendEntity(productDefShapeId,
            "PRODUCT_DEFINITION_SHAPE('','',#" + productDefId + ")");

        int shapeDefRepId = buffer.nextId();
        buffer.appendEntity(shapeDefRepId,
            "SHAPE_DEFINITION_REPRESENTATION(#" + productDefShapeId
            + ",#" + shapeRepId + ")");
    }
}
