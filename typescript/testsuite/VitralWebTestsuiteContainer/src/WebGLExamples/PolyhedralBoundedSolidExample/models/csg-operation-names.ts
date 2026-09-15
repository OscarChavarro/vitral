/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/CsgOperationNames.java`.
 *
 * The four constants in declaration order, their labels, and the wrapping
 * `nextCircular()`, as a string union with free functions, the way the
 * container's other modules carry a Java enum.
 */
export type CsgOperationNames =
  'UNION' | 'INTERSECTION' | 'DIFFERENCE_A_MINUS_B' | 'DIFFERENCE_B_MINUS_A';

const VALUES: readonly CsgOperationNames[] = [
  'UNION',
  'INTERSECTION',
  'DIFFERENCE_A_MINUS_B',
  'DIFFERENCE_B_MINUS_A',
];

const LABELS: Readonly<Record<CsgOperationNames, string>> = {
  UNION: 'UNION',
  INTERSECTION: 'INTERSECTION',
  DIFFERENCE_A_MINUS_B: 'DIFFERENCE A-B',
  DIFFERENCE_B_MINUS_A: 'DIFFERENCE B-A',
};

export function csgOperationNextCircular(operation: CsgOperationNames): CsgOperationNames {
  const nextIndex: number = (VALUES.indexOf(operation) + 1) % VALUES.length;
  return VALUES[nextIndex]!;
}

export function csgOperationLabel(operation: CsgOperationNames): string {
  return LABELS[operation];
}
