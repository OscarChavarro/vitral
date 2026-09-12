/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/model/OperationMode.java`.
 *
 * A string enum, as the rest of the TypeScript edition spells a Java enum, with
 * the declaration order kept because `SolidTextureModel.rotateOperationMode`
 * advances through `values()` by ordinal.
 */
export enum OperationMode {
  MESH_MODEL = 'MESH_MODEL',
  TEXTURE_2D_STACK = 'TEXTURE_2D_STACK',
}

/** Java's `OperationMode.values()`, in declaration order. */
export const OPERATION_MODE_VALUES: readonly OperationMode[] = [
  OperationMode.MESH_MODEL,
  OperationMode.TEXTURE_2D_STACK,
];
