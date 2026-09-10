# TypeScript literal-parity audit

Date: 2026-09-11

Java source of truth: `java/base/src/main` at `deb03bf1`.

## Verdict

The TypeScript tree at the end of the recorded Phase 25.6 gate is **not** a
1:1 port of the Java source. A green TypeScript build and the then-current 106
tests did not constitute a parity gate: production classes and complete
private algorithm graphs were absent, and several existing implementations
used replacement algorithms.

Phase 25 must not be described as complete. The plan itself records Phase
25.7 as unstarted. The current Java CSG/operator source under
`environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
contains 32 files and 15,608 lines; none had a TypeScript counterpart when
this audit began.

## Audit method

- Compared the Phase 1--25 plan inventory with the current Java and TypeScript
  production trees.
- Compiled `java/base` and compared compiled Java public members (`javap`) to
  TypeScript AST members for every same-path class.
- Compared private method inventories for every same-path class.
- Reviewed low source-size ratios and every public-member mismatch against the
  Java method bodies.
- Searched production and tests for placeholders, dummy returns, unsupported
  branches, skipped tests, and explicit alternative/simplified algorithms.
- Compared the 58 Java base test sources with the TypeScript test tree.

## Confirmed missing production blocks

### Phase 15

The recorded status of 6/22 was real, despite later phases having been marked
complete. This audit added the complete Java algorithm graph for:

- `SolverPolynomialQuarticBairstow`
- `DeterminantStrategy`, `InverseStrategy`, and `LinearAlgebraBackend`
- `MatrixAlgorithmsSupport`
- `GaussCpuStrategy`, `LuCpuStrategy`, and `NaiveCofactorCpuStrategy`
- `StrategySelector` and `LinearAlgebraEngine`, including their nested public
  contracts

The native `LzwWrapper` and `SpharmonicKitWrapper` still require explicit Web
runtime designs. Their actual implementations live in `pkgs/LempelZivWelch`
and `pkgs/SpharmonicKit27`; adding empty TypeScript native facades would violate
the port rules.

### Phase 25.7

All current-Java classes below
`environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
remain absent, including the set operator, intersector, intersection-curve
builder, classifiers, null-edge connector, finisher, splitter, construction
fallbacks, validators, modeler, fixtures, context, and trace objects. The
corresponding CSG regression/degeneracy tests are also absent.

## Confirmed incomplete or replacement implementations

The following are source-reviewed findings, not line-count guesses:

- `Matrix4x4f` lacks projection, cofactor, quaternion, Euler-angle, array
  export, equality, hash, and diagnostic behavior present in Java.
- `ParametricBiCubicPatch` lacks `doExtraInformation` and
  `printGeometryMatrices`; its private geometry-matrix construction was
  reorganized and still needs a statement-by-statement parity proof.
- `TriangleMesh.slice` and its complete triangle-cut dependency graph are
  absent.
- `Sphere`, `Box`, `Cone`, and `Arrow` generate B-reps through the
  TypeScript-only `PolyhedralBoundedSolidBuilder`, rather than the Java Euler
  operation sequences. Their remaining intersection/detail branches still
  require statement-level comparison even though the missing public
  `doExtraInformation` wrappers and `Sphere` frame methods were restored by
  this audit.
- `PolyhedralBoundedSolidTopologyEditing` replaced the 1,212-line Java
  implementation (coincident-loop reduction, dangling-edge repair,
  coplanarity safeguards, and restart logic) with a 154-line conservative
  cleanup.
- `PolyhedralBoundedSolidGeometricValidator`, `PolyhedralBoundedSolid`,
  `PolyhedralBoundedSolidPredicates`, and `PolyhedralBoundedSolidEulerOperators`
  omit sizeable private decision graphs or replace their flow. Existing small
  tests do not exercise the Java CSG degeneracies that consume those paths.
- `RGBAImageCompressed` used a different fatal exception path and did not
  reset all state after invalid compressed input.
- The source tree contains legacy TypeScript seed classes with no current Java
  counterpart. They are not public exports, but they must be removed or
  explicitly classified before an exhaustive source-tree parity claim.

## Corrections made during this audit

- Restored the literal Bairstow solver and made `Torus` use Java's normalized
  ray, root arrays, positive-root selection, and hit mutation order. Removed
  the replacement derivative/bisection root solver.
- Added all three Java matrix strategies, backend selection, engine behavior,
  and the translated Java strategy-consistency test.
- Restored Java's distinct direct and indexed distance-field algorithms and
  the separate enlarge/shrink branches in `ImageProcessing.resize`.
- Restored `Matrix4x4d` projections, Euler composition/extraction, cofactors,
  Java inversion path, exports, exact equality/hash, and diagnostics.
- Restored exact equality/hash behavior for double vectors, `MatrixNxM`,
  `ColorRgb`, and `Ray`; restored Ray's unit-direction tolerance.
- Restored missing renderer-configuration comparison, vertex-color access,
  null shading default, and diagnostic output.
- Restored missing compressed/RGB image clone/access contracts and Java's
  invalid-input state reset.
- Restored `Sphere`'s public position/normal/tangent/binormal frame methods and
  the missing `Cone`/`Arrow` detail-intersection entry points.

## Gate status

The repository may pass build/tests after the corrections above, but the
literal-parity gate remains **open** until every confirmed item in this report
and every Phase 25.7 class/test is ported and compared. No test-count-only gate
may override the source/API audit.
