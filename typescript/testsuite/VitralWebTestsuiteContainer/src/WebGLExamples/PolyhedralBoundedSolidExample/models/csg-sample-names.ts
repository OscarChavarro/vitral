import type { CsgOperationNames } from './csg-operation-names';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/CsgSampleNames.java`.
 *
 * The twelve constants with their display ids, the wrapping `nextCircular()`,
 * and the operation each sample forces. Java's commented-out
 * `KURLANDER_BOWL_ALL_MOTIFS(13)` stays out, as it is out there. The samples
 * are built by `GeneralModelsBuilder.createCsgOperands` for the CSG models.
 */
export type CsgSampleNames =
  | 'MANT1986_2'
  | 'STACKED_BLOCKS'
  | 'MOON_BLOCK'
  | 'CROSS_PAIR'
  | 'HOLLOW_BRICK'
  | 'MANT1988_6_13'
  | 'MANT1988_15_1'
  | 'MANT1988_3'
  | 'MANT1988_15_2_HOLED'
  | 'MANT1988_15_2_LIMIT_DIFFERENCE'
  | 'MANT1988_15_2_OPEN_DIFFERENCE'
  | 'KURLANDER_BOWL_SINGLE_MOTIF';

const VALUES: readonly CsgSampleNames[] = [
  'MANT1986_2',
  'STACKED_BLOCKS',
  'MOON_BLOCK',
  'CROSS_PAIR',
  'HOLLOW_BRICK',
  'MANT1988_6_13',
  'MANT1988_15_1',
  'MANT1988_3',
  'MANT1988_15_2_HOLED',
  'MANT1988_15_2_LIMIT_DIFFERENCE',
  'MANT1988_15_2_OPEN_DIFFERENCE',
  'KURLANDER_BOWL_SINGLE_MOTIF',
];

export function csgSampleNextCircular(sample: CsgSampleNames): CsgSampleNames {
  const nextIndex: number = (VALUES.indexOf(sample) + 1) % VALUES.length;
  return VALUES[nextIndex]!;
}

/** Java's `getLabel()`, which is `name()`. */
export function csgSampleLabel(sample: CsgSampleNames): string {
  return sample;
}

export function csgSamplePreferredOperation(
  sample: CsgSampleNames,
  currentOperation: CsgOperationNames,
): CsgOperationNames {
  if (
    sample === 'MANT1988_15_2_LIMIT_DIFFERENCE' ||
    sample === 'MANT1988_15_2_OPEN_DIFFERENCE' ||
    sample === 'KURLANDER_BOWL_SINGLE_MOTIF' /*|| sample === 'KURLANDER_BOWL_ALL_MOTIFS'*/
  ) {
    return 'DIFFERENCE_A_MINUS_B';
  }
  return currentOperation;
}

/** Java's `getDisplayIndex()`: the constructor id, one past the ordinal. */
export function csgSampleDisplayIndex(sample: CsgSampleNames): number {
  return VALUES.indexOf(sample) + 1;
}

export function csgSampleTotalSamples(): number {
  return VALUES.length;
}
