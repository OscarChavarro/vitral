import type { PolygonClippingTestCase } from './polygon-clipping-test-case';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/model/PolygonClippingFixtures.java`.
 *
 * The forty-three cases the `[1]` and `[2]` keys walk through, in the Java
 * order, naming the same files of `etc/polygons`. Java's private constructor,
 * which only stops the class being instantiated, has no counterpart in a
 * module that exports one constant.
 */
export const POLYGON_CLIPPING_CASES: readonly PolygonClippingTestCase[] = [
  { name: 'TRIANGLE_VS_QUAD', clipFile: 'example03.polygon', subjectFile: 'example04.polygon' },
  {
    name: 'TRIANGLE_VS_QUAD_WITH_HOLE',
    clipFile: 'example03.polygon',
    subjectFile: 'example05.polygon',
  },
  {
    name: 'DIAMOND_VS_STAR_CHAIN',
    clipFile: 'example06.polygon',
    subjectFile: 'example07.polygon',
  },
  {
    name: 'DIAMOND_VS_STAR_CHAIN_HOLE',
    clipFile: 'example06.polygon',
    subjectFile: 'example08.polygon',
  },
  { name: 'VERTICAL_STRIPPER', clipFile: 'example09.polygon', subjectFile: 'example10.polygon' },
  { name: 'MULTI_LOOP_CLIP', clipFile: 'example11.polygon', subjectFile: 'example12.polygon' },
  {
    name: 'MULTI_LOOP_CLIP_OUTER_ONLY',
    clipFile: 'example11.polygon',
    subjectFile: 'example13.polygon',
  },
  {
    name: 'SUBJECT_AS_OUTER_FRAME',
    clipFile: 'example12.polygon',
    subjectFile: 'example14.polygon',
  },
  {
    name: 'FRAME_WITH_INNER_SUBJECTS',
    clipFile: 'example15.polygon',
    subjectFile: 'example16.polygon',
  },
  { name: 'SUBJECT_WITH_BANDS', clipFile: 'example12.polygon', subjectFile: 'example17.polygon' },
  { name: 'NESTED_SQUARES', clipFile: 'example18.polygon', subjectFile: 'example19.polygon' },
  { name: 'WINDOW_WITH_ISLANDS', clipFile: 'example20.polygon', subjectFile: 'example18.polygon' },
  {
    name: 'RECT_WITH_MATCHING_HOLE',
    clipFile: 'example21.polygon',
    subjectFile: 'example22.polygon',
  },
  { name: 'OUTER_ONLY_BAND', clipFile: 'example15.polygon', subjectFile: 'example13.polygon' },
  { name: 'TRAPEZOID_VS_WINDOW', clipFile: 'example23.polygon', subjectFile: 'example24.polygon' },
  { name: 'RECT_VS_WINDOW', clipFile: 'example25.polygon', subjectFile: 'example24.polygon' },
  { name: 'SLANTED_RECT', clipFile: 'example26.polygon', subjectFile: 'example27.polygon' },
  { name: 'ARROW_A', clipFile: 'example28.polygon', subjectFile: 'example29.polygon' },
  { name: 'ARROW_B', clipFile: 'example29.polygon', subjectFile: 'example28.polygon' },
  { name: 'SQUARE_VS_STAR', clipFile: 'example30.polygon', subjectFile: 'example31.polygon' },
  { name: 'QUAD_VS_TRIANGLE', clipFile: 'example32.polygon', subjectFile: 'example30.polygon' },
  { name: 'HEX_VS_TRIANGLE', clipFile: 'example33.polygon', subjectFile: 'example34.polygon' },
  { name: 'HEX_VS_TRIANGLE_ALT', clipFile: 'example33.polygon', subjectFile: 'example35.polygon' },
  { name: 'DIAMOND_VS_DIAMOND', clipFile: 'example36.polygon', subjectFile: 'example32.polygon' },
  { name: 'KITE_VS_COMPLEX', clipFile: 'example37.polygon', subjectFile: 'example38.polygon' },
  { name: 'DIAMOND_VS_RECT', clipFile: 'example39.polygon', subjectFile: 'example40.polygon' },
  { name: 'RECT_VS_HALF_RECT', clipFile: 'example40.polygon', subjectFile: 'example41.polygon' },
  { name: 'NOTCH_SELF_SIMILAR', clipFile: 'example42.polygon', subjectFile: 'example42.polygon' },
  { name: 'RECT_DISJOINT_RIGHT', clipFile: 'example40.polygon', subjectFile: 'example43.polygon' },
  { name: 'RECT_DISJOINT_TOP', clipFile: 'example44.polygon', subjectFile: 'example45.polygon' },
  { name: 'SKEW_RECT_SELF', clipFile: 'example46.polygon', subjectFile: 'example46.polygon' },
  { name: 'PENTAGON_SELF', clipFile: 'example47.polygon', subjectFile: 'example47.polygon' },
  { name: 'PENTAGON_VS_RECT', clipFile: 'example44.polygon', subjectFile: 'example48.polygon' },
  {
    name: 'CONCAVE_WINDOW_VS_BAR',
    clipFile: 'example49.polygon',
    subjectFile: 'example50.polygon',
  },
  {
    name: 'BAR_VS_CONCAVE_WINDOW',
    clipFile: 'example50.polygon',
    subjectFile: 'example49.polygon',
  },
  {
    name: 'CONCAVE_FRAME_VS_ROOF',
    clipFile: 'example51.polygon',
    subjectFile: 'example52.polygon',
  },
  { name: 'LOWER_BAR_VS_FRAME', clipFile: 'example53.polygon', subjectFile: 'example51.polygon' },
  { name: 'INNER_BAR_OFFSET', clipFile: 'example51.polygon', subjectFile: 'example54.polygon' },
  { name: 'INNER_BAR_SHARP', clipFile: 'example51.polygon', subjectFile: 'example55.polygon' },
  {
    name: 'INNER_BAR_TOUCHING_RIGHT',
    clipFile: 'example51.polygon',
    subjectFile: 'example56.polygon',
  },
  {
    name: 'FRAME_WITH_SHIFTED_BAR',
    clipFile: 'example51.polygon',
    subjectFile: 'example57.polygon',
  },
  {
    name: 'UNION_SQUARES_SHARED_EDGE',
    clipFile: 'example58.polygon',
    subjectFile: 'example59.polygon',
  },
  {
    name: 'UNION_SQUARES_SHARED_CORNER',
    clipFile: 'example58.polygon',
    subjectFile: 'example60.polygon',
  },
];
