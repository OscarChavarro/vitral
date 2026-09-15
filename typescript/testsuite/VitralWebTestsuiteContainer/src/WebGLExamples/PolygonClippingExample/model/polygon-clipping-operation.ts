/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/model/PolygonClippingOperation.java`.
 *
 * A Java enum with a display name and a `next()` that wraps around its
 * declaration order. The constants become a string union whose members are the
 * Java constant names, the display names become the lookup below, and `next()`
 * becomes the free function, so the wrap-around order is the declaration order
 * of the Java enum.
 */
export type PolygonClippingOperation = 'INTERSECTION' | 'UNION' | 'A_MINUS_B' | 'B_MINUS_A';

const VALUES: readonly PolygonClippingOperation[] = [
  'INTERSECTION',
  'UNION',
  'A_MINUS_B',
  'B_MINUS_A',
];

const DISPLAY_NAMES: Readonly<Record<PolygonClippingOperation, string>> = {
  INTERSECTION: 'INTERSECTION',
  UNION: 'UNION',
  A_MINUS_B: 'A_MINUS_B',
  B_MINUS_A: 'B_MINUS_A',
};

export function polygonClippingOperationDisplayName(operation: PolygonClippingOperation): string {
  return DISPLAY_NAMES[operation];
}

export function nextPolygonClippingOperation(
  operation: PolygonClippingOperation,
): PolygonClippingOperation {
  const nextIndex = (VALUES.indexOf(operation) + 1) % VALUES.length;
  return VALUES[nextIndex]!;
}
