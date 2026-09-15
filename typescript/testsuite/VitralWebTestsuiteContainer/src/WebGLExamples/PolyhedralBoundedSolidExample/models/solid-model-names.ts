/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/SolidModelNames.java`.
 *
 * Every Java constant is kept, with its id, so that `fromId` answers what the
 * Java one answers, and the declaration order is the array below. Java's enum
 * methods become the free functions after it, taking the constant as their
 * first argument.
 *
 * `MAIN_SEQUENCE` — the walk `[3]` and `[4]` step through, and whose length and
 * positions the HUD prints — is Java's without seven entries, and that is the
 * one deliberate difference of this first stage of the port: `HOLLOW_BOX`,
 * `CSG_LAMP_SHELL`, `CSG_DIRECT`, `CSG_OPERAND1_PARTIAL` and
 * `CSG_OPERAND2_PARTIAL` are built by the set operations of
 * `PolyhedralBoundedSolidModeler.setOp`, and `SPLIT_TEST_PART_1` to `_3` by the
 * splitter, `PolyhedralBoundedSolidModeler.split` — the first of the three
 * shows the solid before the split, but belongs to the same test. Both
 * families are left for the second stage of this migration, where they will
 * be verified against the Java kernel on their own, and they return to the
 * sequence then, in their Java places. `CSG_MOON_BLOCK` is not in Java's
 * sequence either.
 */
export type SolidModelNames =
  | 'MVFS_SMEV_SAMPLE'
  | 'BOX'
  | 'HOLED_BOX'
  | 'HOLLOW_BOX'
  | 'ARC_SAMPLE'
  | 'CIRCULAR_LAMINA'
  | 'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC'
  | 'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR'
  | 'SPHERE'
  | 'CONE'
  | 'CYLINDER'
  | 'CSG_MOON_BLOCK'
  | 'CSG_LAMP_SHELL'
  | 'ARROW'
  | 'LAMINA_WITH_TWO_SHELLS'
  | 'LAMINA_WITH_HOLE'
  | 'FONT_BLOCK'
  | 'GLUED_CYLINDERS'
  | 'EULER_OPERATORS_TEST'
  | 'ROTATIONAL_SWEEP'
  | 'SPLIT_TEST_PART_1'
  | 'SPLIT_TEST_PART_2'
  | 'SPLIT_TEST_PART_3'
  | 'CSG_DIRECT'
  | 'CSG_OPERAND1_PARTIAL'
  | 'CSG_OPERAND2_PARTIAL'
  | 'FEATURED_OBJECT'
  | 'IMPORT_OR_FEATURED_OBJECT'
  | 'STEP_IMPORT';

const VALUES: readonly SolidModelNames[] = [
  'MVFS_SMEV_SAMPLE',
  'BOX',
  'HOLED_BOX',
  'HOLLOW_BOX',
  'ARC_SAMPLE',
  'CIRCULAR_LAMINA',
  'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC',
  'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR',
  'SPHERE',
  'CONE',
  'CYLINDER',
  'CSG_MOON_BLOCK',
  'CSG_LAMP_SHELL',
  'ARROW',
  'LAMINA_WITH_TWO_SHELLS',
  'LAMINA_WITH_HOLE',
  'FONT_BLOCK',
  'GLUED_CYLINDERS',
  'EULER_OPERATORS_TEST',
  'ROTATIONAL_SWEEP',
  'SPLIT_TEST_PART_1',
  'SPLIT_TEST_PART_2',
  'SPLIT_TEST_PART_3',
  'CSG_DIRECT',
  'CSG_OPERAND1_PARTIAL',
  'CSG_OPERAND2_PARTIAL',
  'FEATURED_OBJECT',
  'IMPORT_OR_FEATURED_OBJECT',
  'STEP_IMPORT',
];

/** Java's constructor argument: every constant's id is its declaration ordinal. */
const IDS: Readonly<Record<SolidModelNames, number>> = Object.fromEntries(
  VALUES.map((name, index) => [name, index]),
) as Record<SolidModelNames, number>;

const MAIN_SEQUENCE: readonly SolidModelNames[] = [
  'MVFS_SMEV_SAMPLE',
  'BOX',
  'HOLED_BOX',
  'ARC_SAMPLE',
  'CIRCULAR_LAMINA',
  'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC',
  'TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR',
  'SPHERE',
  'CONE',
  'CYLINDER',
  'ARROW',
  'LAMINA_WITH_TWO_SHELLS',
  'LAMINA_WITH_HOLE',
  'FONT_BLOCK',
  'GLUED_CYLINDERS',
  'EULER_OPERATORS_TEST',
  'ROTATIONAL_SWEEP',
  'FEATURED_OBJECT',
  'IMPORT_OR_FEATURED_OBJECT',
  'STEP_IMPORT',
];

export function solidModelNamesValues(): SolidModelNames[] {
  return [...VALUES];
}

export function solidModelNamesFromId(id: number): SolidModelNames {
  for (const value of VALUES) {
    if (IDS[value] === id) {
      return value;
    }
  }
  return 'HOLED_BOX';
}

export function solidModelNextClamped(name: SolidModelNames): SolidModelNames {
  const currentIndex: number = getMainSequenceIndex(name);
  if (currentIndex < 0) {
    return MAIN_SEQUENCE[0]!;
  }

  let nextIndex: number = currentIndex + 1;
  if (nextIndex >= MAIN_SEQUENCE.length) {
    nextIndex = MAIN_SEQUENCE.length - 1;
  }
  return MAIN_SEQUENCE[nextIndex]!;
}

export function solidModelPreviousClamped(name: SolidModelNames): SolidModelNames {
  const currentIndex: number = getMainSequenceIndex(name);
  if (currentIndex < 0) {
    return MAIN_SEQUENCE[0]!;
  }

  let previousIndex: number = currentIndex - 1;
  if (previousIndex < 0) {
    previousIndex = 0;
  }
  return MAIN_SEQUENCE[previousIndex]!;
}

export function solidModelDisplayIndex(name: SolidModelNames): number {
  const index: number = getMainSequenceIndex(name);
  if (index < 0) {
    return 1;
  }
  return index + 1;
}

export function solidModelTotalModels(): number {
  return MAIN_SEQUENCE.length;
}

export function solidModelUsesCsgDebugControls(name: SolidModelNames): boolean {
  return (
    name === 'CSG_MOON_BLOCK' ||
    name === 'CSG_DIRECT' ||
    name === 'CSG_OPERAND1_PARTIAL' ||
    name === 'CSG_OPERAND2_PARTIAL'
  );
}

function getMainSequenceIndex(name: SolidModelNames): number {
  let i: number;

  for (i = 0; i < MAIN_SEQUENCE.length; i++) {
    if (MAIN_SEQUENCE[i] === name) {
      return i;
    }
  }
  return -1;
}
