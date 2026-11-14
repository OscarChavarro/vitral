# Ray tracing / CSG migration plan: cpp → turboc, java, typescript

Plan for a Claude Code session (the *orchestrator*) that launches three agents
in parallel, one per port, to bring `turboc/`, `java/` and `typescript/` to
the state the C++ port reached while its geometry and scene model was
redesigned to support CSG ray operations.

- **Baseline** (all four ports were in sync here): `7276c54ac72d0f2474bbffbc5e08ecc59648fa5b`
- **Target** (cpp state to reproduce): `4412e55a22ff23e2f4592452585d3b61e3e96435`
  ("Cpp port: geometry and scene migration to support CSG, part 7")
- **Source of truth**: `cpp/` at the target. This overrides the usual "Java is
  the source of truth for names" rule of `doc/attributesGettersAndSettersNames.md`
  for everything introduced in this range.

If `cpp/` has moved past the target when this plan is executed, the
orchestrator re-pins the target to `git rev-parse HEAD`, re-runs the inventory
commands of section 2, and gives the new SHA to the agents.

## 1. History decision: keep the seven commits, port from the cumulative diff

Do not squash. The agents never replay the commits; they port the **final
state**, read through the cumulative diff `git diff <baseline> <target>`.

- The seven commits ("... to support CSG", parts 1–7) are already on
  `origin/main`; squashing means rewriting published history for no gain.
- The cumulative diff is what a squash would produce, and it is available
  without one.
- Replaying commit by commit would port code that later parts restructured:
  part 7 extracts `SceneSolid` out of `SimpleBody` and rewrites about 600
  lines added by parts 1–6. Only 66 of 280 files are touched by more than one
  commit, and nothing is renamed or deleted in between, so the per-commit
  history stays useful for `git log -p <file>` and `git bisect` on the cpp side.
- The commits are daily checkpoints, not self-contained features; section 3
  gives the dependency order to port in instead.

Each port lands as its own commit after verification (section 7).

## 2. Inventory of the change

280 files, +23,285 / −316 lines, all under `cpp/` and `etc/`. No port other
than cpp was touched after the baseline.

| Area (under `cpp/base/src/main/vsdk/toolkit` unless noted) | Files | Lines added |
|---|---|---|
| `environment/scene/csg` (all new) | 83 | 3,771 |
| `environment/geometry/volume` | 37 | 4,897 |
| `environment/geometry/surface` | 10 | 1,675 |
| `environment/geometry/element` | 28 | 673 |
| `environment/scene` | 13 | 1,183 |
| `io/geometry` | 8 | 941 |
| `processing` | 6 | 583 |
| `common/numerics` (`FloatingInterval`) | 2 | 151 |
| `cpp/base/src/main/java` (Java-like layer) | 8 | 174 |
| `cpp/base/src/test` (GoogleTest, fixtures, reference data, generators) | 46 | 7,977 |
| `cpp/testsuite` + `cpp/opengl4` | 17 | 543 |
| `etc/geometry/mitscenes` (17 `.ray` scenes + `terrain.alt`) | 18 | 502 |

Commands every agent uses to read the change:

```bash
BASE=7276c54ac72d0f2474bbffbc5e08ecc59648fa5b
TARGET=4412e55a22ff23e2f4592452585d3b61e3e96435
git diff --name-status $BASE $TARGET -- cpp etc      # A = new file, M = modified
git diff $BASE $TARGET -- cpp/<path>                 # how a pre-existing file changed
git show $TARGET:cpp/<path>                          # final text of any file
git diff $BASE $TARGET -- cpp/base/CMakeLists.txt    # authoritative list of new sources and tests
```

### Design summary (what the ports must reproduce)

- **Typed geometry contract** on `Geometry`: `copyGeometry`, `classifyRay`
  (full solid membership over an interval), `queryFirstHit`,
  `querySurfaceDetail`, `getBounds`, `classifyPoint`,
  `classifyPointCertified`, `classifyBoxCertified`. "Unsupported",
  "InvalidInput" and "Indeterminate" are explicit results, never silent
  defaults. The legacy `doIntersectionFirstHit`, `doContainmentTest` and
  `getMinMax` remain.
- **Element types**: `RayBoundary` (now the base of `RayCrossing`),
  `RaySolidBoundary`, `RaySolidClassification`, `RayInstancePath` /
  `RayInstanceStep`, `GeometryBounds`, `GeometryQueryWorkspace` (caller-owned
  scratch, one per worker), status enums. `Ray::fromAffine` preserves the
  direction magnitude; `RayHit` tracks which detail is actually available.
- **Scene**: new abstract `SceneSolid` (geometry reference, authoritative
  affine placement with inverse, typed placed-solid queries). `SimpleBody`
  now derives from it and adds decomposed or explicit affine placement,
  appearance, and shared texture / normal map references owned by
  `SimpleScene` (`addImage`, `addNormalMap`).
- **CSG** (`environment/scene/csg`): expression model (`CsgOperation` with
  the POV identifiers 9/10/11, `CsgExpression`, `CsgOperandView`, `CsgTree`,
  `CsgBodyOperand`, `CsgPlacedExpression`, `CsgVersionStamp`); exact dyadic
  arithmetic and solid equivalence (`CsgExactArithmetic`, `CsgDyadic*`,
  `CsgExact*`, `CsgSolidEquivalence`, `CsgExpressionEquivalence`); certified
  point classification (`CsgPoint*`); ray events and two evaluators
  (`CsgRothRayClassifier`, `CsgCrossingMembershipFilter`, plus the
  uncertified template `CsgSegmentClassification`); and `CsgBody`, the scene
  body that renders an expression.
- **New solids and surfaces**: `PlanarHalfSpace`, `QuadricSurface`,
  `ImplicitSolid`, `PolynomialSurface`, `BlobSolid`, `HeightFieldSolid`,
  `SubdividedBezierPatch`, and the certified ellipsoid distance family
  (`AffineEllipsoid*`, `OrthogonalEllipsoidDistance`, `QuadraticInteriorBound`).
- **IO and processing**: `ReaderMitScene` grew from a few primitives to the
  extended MIT format (`csg … end` blocks with `roth` / `pov` evaluators,
  `transform`, `rotation`, `quadric`, `halfspace`, `poly`/`term`, `blob`/
  `component`, `heightfield`, `patch`, `triangles`, `quads`, `obj`,
  `texture`, `bumpmap`, `backgroundcubemap`, `viewport`, …), with
  `ReaderMitTriangleGroup`, `ReaderMitQuadGroup`, `ReaderAltimetry`;
  `BezierPatchTessellator` and `SolidBoundaryTessellator` (+ report).
- **Applications**: `RaytracingOfflineExample` (`runAll.sh` renders every
  scene in `etc/geometry/mitscenes`), `ShadersExample` (selectable geometry
  through `ShadersGeometryFixture`), `OpenGL4GeometryRenderer`.

## 3. Porting order (same layers in every port)

Port bottom-up and build after every layer. Port each layer's tests together
with the layer, not at the end. The order follows the include graph of the
target; if an include says otherwise, follow the include.

| Layer | Content |
|---|---|
| L0 Support | Java-like layer additions (`Double::NaN/isNaN/isFinite`, `Math::nextAfter`; reference-counted `Future` / regex `Pattern` state replacing `SharedPointer`), `FloatingInterval`, the `MicroFacetedMaterial` fix |
| L1 Element contract | New `geometry/element` types and enums, `Ray`, `RayHit`, `RayCrossing*`, `Triangle`, new `Geometry` virtuals with their defaults |
| L2 Existing primitives | `Sphere`, `Box`, `Cone`, `InfinitePlane`, `TriangleMesh` + `TriangleMeshIntersectionIndex`, ellipsoid distance family |
| L3 Scene | `AffinePlacement`, `AffineToleranceMap`, `InstanceRayCrossingBuffer`, `SceneSolid`, `SimpleBody`, `SimpleScene` resources |
| L4 New solids | `PlanarHalfSpace`, `QuadricSurface` (+`QuadricRayExpansion`), `ImplicitSolid`, `PolynomialSurface`, `BlobSolid`, `HeightFieldSolid`, `SubdividedBezierPatch` |
| L5 CSG | Expression model → exact arithmetic and equivalence → point classification → ray events → evaluators → `CsgBody` |
| L6 IO / processing | `ReaderMitScene` and helpers, `ReaderAltimetry`, tessellators |
| L7 Applications | `RaytracingOfflineExample`, `ShadersExample`, renderer change |

Test files per layer are listed in the `vitral_base_tests` block of
`cpp/base/CMakeLists.txt`; the reference tables `csg_*_reference.inc` come
from the `generate_*_reference.py` scripts next to them.

## 4. Rules for 1:1 parity (all ports)

1. **One cpp class, one port class**, with the same name, the same package
   path under `vsdk/toolkit/...`, the same public method names, the same
   parameter order and the same documented behavior, including which inputs
   yield Unsupported / InvalidInput / Indeterminate. No extra public API, no
   merged or split classes, no "improved" algorithms.
2. **Same arithmetic**. Keep the order of floating-point operations of the
   cpp source; do not reassociate, do not introduce fused multiply-add, do
   not replace a certified bound by a looser or tighter one. The files that
   cpp compiles with `-fno-fast-math -ffp-contract=off` (listed in
   `cpp/base/CMakeLists.txt`) depend on IEEE-754 binary64 round-to-nearest.
3. **Comments and bibliographic references** (`[ROTH1982]`, …) are carried
   over, translated only where the language forces it.
4. **Follow the precedent of the port**. Commit `35970a77` ported the first
   step of this redesign (`RayCrossing*`) to all four ports; diff each port's
   version of those files against `git show 7276c54a:cpp/...` to see how the
   port expresses enums, borrowed pointers, output arguments and constants,
   and keep doing the same.
5. **Shared conventions for Java and TypeScript** where C++ has no direct
   equivalent (fixed here so two independent agents converge):
   - `enum class` → Java `enum` / the TS form already used by
     `RayCrossingKind.ts`; `CsgOperation` keeps the values 9, 10, 11.
   - Output argument of class type (`RayHit*`, `GeometryBounds&`,
     `RaySolidClassification&`, `Matrix4x4d&`) → the caller passes the mutable
     object; same parameter position.
   - Output argument of primitive type (`double&`, `bool&`, `long long&`,
     `GeometryContainment&`) → a one-element array in the same parameter
     position, unless the port already has an established holder for that
     case. "Outputs unchanged on failure" contracts must hold.
   - Multiple inheritance (`CsgTree : CsgExpression, CsgOperandView`) →
     interfaces; Java uses default methods for the base defaults, TypeScript
     implementers restate them.
   - Templates (`CsgSegmentClassification<Payload>`) → generics.
   - Ownership (`setX` owns, `setXReference` borrows; `= delete` copy) has no
     runtime meaning with garbage collection: keep both methods so call sites
     stay identical, and document them as equivalent.
   - `nullptr` results stay `null`; exceptions stay exceptions of the
     corresponding Java-like type.
6. **Tests are part of parity**. Every cpp test case gets a port counterpart
   with the same name and the same numeric expectations. A case that cannot be
   expressed is listed in the agent's report with the reason; it is never
   silently dropped or weakened.
7. **Stay inside your port**. Agents write only under their own port
   directory. `cpp/`, `etc/`, `doc/`, `scripts/` and the other ports are
   read-only. A needed change outside the port (for instance an exception in
   `scripts/checkAttributeNames.py`) goes in the report for the orchestrator.
8. **No git state changes**. The three agents share one working tree: no
   `commit`, `stash`, `checkout`, `reset`, `clean` or branch operations.
   Read-only git (`diff`, `show`, `log`) is fine.
9. A defect found in cpp is reported, not fixed and not reproduced blindly:
   port the cpp behavior, mark the spot with a comment, list it in the report.
10. **turboc exception**. Rules 1, 2 and 6 apply to turboc at the level of
    class names, file paths, public interfaces and documented behavior. Where
    the 32-bit limits of Turbo C++ make the cpp implementation impossible,
    turboc may reimplement the inside of a module and may produce different
    numeric results, under the decisions of section 6.3.1.

## 5. Orchestrator procedure

1. Confirm the working tree is clean and the target SHA (top of this document).
2. Produce the cpp reference renders (outputs are git-ignored):
   `cpp/testsuite/OfflineExamples/RaytracingOfflineExample/runAll.sh`, and run
   `vitral_base_tests` so the cpp side is known green at the target.
3. Run include guard step G1 of section 6.3.2 on `cpp/` and `turboc/`
   (the agents treat `cpp/` as read-only, so this happens before they start).
   Build both ports and re-run `vitral_base_tests`. This is a mechanical
   change with its own commit, if the user asked for commits:
   `Include guards without VSDK_ prefix and _H suffix (cpp and turboc)`.
   The baseline and target SHAs of this plan do not change; the guard commit
   sits on top of the target.
4. Launch the three agents **in one message**, in the background, with the
   prompts of section 8. They work in the shared tree, in disjoint
   directories; no worktree isolation is needed.
5. When an agent stops before its completion criteria are met, continue it
   with `SendMessage` rather than starting a new one.
6. When all three report, run the cross-port checks of section 7, resolve
   divergences between the Java and TypeScript APIs (cpp decides), and report
   to the user. Commit only if the user asked for it, one commit per port:
   - `Java port: geometry and scene migration to support CSG (synced to cpp <short sha>)`
   - `Typescript port: geometry and scene migration to support CSG (synced to cpp <short sha>)`
   - `TurboC port: geometry and scene migration to support CSG (synced to cpp <short sha>)`

## 6. Port briefs

### 6.1 Java (`java/`)

- Sources in `java/base/src/main/vsdk/toolkit/...`, one public class per file.
  Tests are JUnit 5 + AssertJ in `java/base/src/test/vsdk/toolkit/...`, named
  `<Subject>Test.java` (existing example:
  `environment/geometry/RayCrossingTest.java`). Test-only helpers of cpp
  (`CsgTestOperand`, `HeightFieldTestFactory`, …) go next to the tests.
- L0: the Java-like layer is the JDK itself; nothing to port except checking
  that `Math.nextAfter`, `Double.isNaN`, `Double.isFinite` have the semantics
  the cpp code relies on. The `Future` / `Pattern` reference counting does not
  apply. Do not use `Math.fma`; Java 17 arithmetic is already strict.
- Exact arithmetic: cpp uses `unsigned long long` words and `unsigned int`
  limbs. Use `long` / `int` with explicit unsigned handling
  (`Long.compareUnsigned`, `& 0xFFFFFFFFL`, `>>>`); do not substitute
  `BigInteger`, the fixed-capacity overflow failures are part of the contract.
- Reference tables: translate `csg_*_reference.inc` into test data generated
  by the same Python generators (add a Java output mode in a copy under the
  Java test tree, or parse the `.inc` at test time); never retype values.
- Applications: `java/testsuite/OfflineExamples/RaytracingOfflineExample`
  (including `runAll.sh` over every scene) and
  `java/testsuite/Jogl4Examples/ShadersExample`; apply the
  `OpenGL4GeometryRenderer` change to its JOGL 4 counterpart.
- Build and verify: `cd java && ./gradlew -q classes`, then
  `./gradlew :base:test`. Run `python3 scripts/checkAttributeNames.py --all`
  from the repository root.

### 6.2 TypeScript (`typescript/`)

- Node.js 22+, `npm ci`. Sources in
  `typescript/base/src/main/vsdk/toolkit/...`; tests are Vitest specs in
  `typescript/base/src/test/vsdk/toolkit/...` named `<Subject>.spec.ts`
  (existing example: `environment/geometry/RayCrossing.spec.ts`). `dist/` is
  build output: never edit it. `@vitral/base` stays platform-neutral: file
  access for `ReaderMitScene` resources, `ReaderAltimetry` and the tests goes
  through the existing reader abstractions and `@vitral/fs`, not `node:fs`
  imports in `base`. Export new classes the way the package already does.
- L0: add to the Java-like layer (`typescript/base/src/main/java`) what the
  cpp layer gained and the port lacks (`Double`, `Math.nextAfter`). The
  `Future` / `Pattern` reference counting does not apply.
- Numerics: `number` is binary64, which is what the certified code needs.
  `nextAfter`, `ilogb`, `ldexp`, `signbit` and the decomposition of a double
  into significand and exponent have no built-in: implement them exactly
  (typed-array views), with tests against the cpp reference values. 64-bit
  integer words do not fit in `number`: use `Uint32Array` limbs with
  carry handled in 16-bit halves or `BigInt` confined to the exact-arithmetic
  classes, keeping the fixed capacities and overflow failures of cpp.
- `GeometryQueryWorkspace` is per worker; it must not cross the Web Worker
  protocol. Check that scene snapshots transferred to workers
  (`SimpleSceneSnapshotTransfer.spec.ts`) carry the new bodies, placements
  and solids.
- Applications: `typescript/testsuite/OfflineExamples/RaytracingOfflineExample`
  and `typescript/testsuite/VitralWebTestsuiteContainer/src/WebGLExamples/ShadersExample`;
  apply the geometry renderer change to the WebGL counterpart.
- Build and verify from `typescript/`: `npm run build`, `npm run test`,
  `npm run lint` (includes the attribute name check), `./scripts/compile.sh`.

### 6.3 Turbo C++ (`turboc/`)

Read `turboc/README.md` in full, and the "Turbo C++ 3.0 / DOS maintenance
notes" of the root `README.md`, before writing any code. The port is one code
base for g++ 4.1.2, gcc 2.7.2.1 and Borland Turbo C++ 3.0 on DOS, written in a
pre-C++98 subset. The cpp sources cannot be copied: every file is rewritten in
that subset.

Language translation of what the cpp change uses (counts are occurrences in
the changed cpp files, as a guide to the amount of rewriting):

| cpp construct | turboc form |
|---|---|
| `nullptr` (189), `override` (158), `= delete` (12), `constexpr` (32), `auto` (7), lambdas (7) | `NULL` (cast where the type matters), plain `virtual`, private undefined copy constructor and assignment, enumerators or constants defined in a `.cpp`, explicit types, named functions or small classes |
| `bool` / `true` / `false` | `BOOLEAN` / `TRUE` / `FALSE` from `vsdk/toolkit/common/VSDK.h` |
| `enum class X { … }` and `X::Value` | the form already used by `RayCrossingKind.h` / `RayCrossingSupport.h` in turboc |
| `std::abs`, `std::sqrt`, `std::isnan`, `std::nextafter`, `std::ilogb`, `std::ldexp`, `std::signbit`, `std::fma`, `std::strtod`, … | `<math.h>` / `<stdlib.h>` C forms; where DOS lacks one, a `Cpp98Support` / Java-like layer function with an `#ifdef __TURBOC__` alternative |
| `dynamic_cast` (11), `throw` (44) | `java/lang/Class` runtime type descriptions; the Java-like exception hierarchy |
| `static_cast` (38), `for (int i…)`, declarations in conditions, `?:` with class temporaries | conventional casts, indices declared before the loop, `if` / `else` with a local |
| `template <class Payload> CsgSegmentClassification` + `.txx` | allowed (no member templates, no specializations, no static data members, complete element types for containers) |
| `long long` / `unsigned long long` (63) | `VSDK_LL` / `VSDK_ULL` literals; **32 bits on Turbo C++** — see below |

#### 6.3.1 Decisions for 32-bit precision

Turbo C++ 3.0 has no 64-bit integer: `long long` is a 32-bit `long`, `int` is
16 bits, and the x87 evaluates `double` expressions in extended precision.
The cpp change assumes 64-bit integers and strict binary64 in several
modules. These decisions are taken for the turboc port:

- **D1. One implementation for every turboc target.** The 32-bit design is
  the turboc design, used by g++ 4.1.2, gcc 2.7.2.1 and Turbo C++ alike, so
  the host build exercises the same code that runs on DOS. `#ifdef __TURBOC__`
  stays confined to C library and platform services, as the README requires.
- **D2. Modules may be reimplemented.** When a cpp module depends on 64-bit
  integers or on strict binary64, turboc keeps its class name, file path,
  public interface and documented contract, and may replace its internals.
  Known candidates:
  - `CsgExactArithmetic` (53-bit significands in `unsigned long long`, 64-bit
    word accumulation) and `CsgDyadic` / `CsgDyadicSolid` /
    `CsgDyadicTransform` (`unsigned int limbs[1100]`): rewrite on 16-bit limbs
    with 32-bit `unsigned long` intermediates, or on a smaller capacity that
    fits DOS memory.
  - `CsgExactPlane`, `CsgExactQuadric`, `CsgExactTransform`,
    `CsgSolidEquivalence`, `CsgExpressionEquivalence`: follow whatever the
    exact layer above can still prove.
  - `FloatingInterval` and the certified bounds built on it
    (`AffineEllipsoid*`, `QuadraticInteriorBound`, `AffineToleranceMap`,
    `classify*Certified`): widen the outward rounding to cover extended
    precision evaluation, or report the environment as unsupported.
  - Anything else the agent finds: it reports the module and the reason.
- **D3. Degrade to "not proven", never to a wrong answer.** The contracts
  already allow it: exact comparisons return false on unsupported
  environments or overflow, intervals become invalid, certified queries
  answer Indeterminate. A 32-bit reimplementation may prove less than cpp
  (two equal solids not recognized as equal, a point left Indeterminate) and
  the evaluators then fall back to their uncertified paths. It must not claim
  a proof that cpp would not give.
- **D4. Counters and indices.** Modification and structure version counters
  (`long long` in cpp) are 32-bit. Any count that can exceed 32,767 (height
  field cells, triangle and vertex indices, limb and event counts, image
  offsets) is a `long`, never an `int`.
- **D5. Memory before fidelity.** Fixed-capacity buffers sized for 64-bit
  hosts (a `CsgDyadic` is over 2 KB in cpp, and several live in one
  expression) may be reduced or moved off the stack so the offline ray tracer
  fits the DOS 286 build. A scene that exceeds a reduced capacity fails with
  an explicit error or a "not proven" result, not with corruption.
- **D6. Images may differ from the other ports.** Because of D2–D5, turboc
  renders are not required to match cpp, Java or TypeScript pixel by pixel.
  They are required to make sense (section 7). Every module reimplemented
  under D2 gets a comment at the top of its header stating that its internals
  differ from cpp and why, and is listed in the agent's report with the
  observable differences.
- **D7. Tests follow the module.** Tests of a reimplemented module keep the
  cpp case names. An expectation that depends on 64-bit exactness (for
  example the `csg_*_reference.inc` tables) may be adapted to what the 32-bit
  design guarantees: "proven equal" may become "equal or not proven", never
  "proven different". Each adapted expectation is listed in the report.

#### 6.3.2 Include guard review

Turbo C++ distinguishes identifiers by their first 32 characters, macros and
include guards included. Today the guards carry redundant `VSDK_` prefixes
and `_H` suffixes that push them over the limit: at the target, cpp has 210
`#ifndef` names longer than 32 characters and turboc has 122, and the new
`VSDK_CSG_SEGMENT_CLASSIFICATION_H` / `VSDK_CSG_SEGMENT_CLASSIFICATION_TXX`
pair already collides in its first 32 characters. Guards are fixed from the
start instead of being patched as collisions appear.

**Step G1 — cpp and turboc (orchestrator, before the agents start).**
Normalize every project include guard to the prevailing `__NAME__` style,
without `VSDK_` prefix and without `_H` postfix:

- `VSDK_RAY_CROSSING_H` → `__RAY_CROSSING__`; `VSDK_CSG_BODY_H` →
  `__CSG_BODY__`. A `.txx` file keeps `_TXX` so it differs from its header:
  `VSDK_CSG_SEGMENT_CLASSIFICATION_TXX` → `__CSG_SEGMENT_CLASSIFICATION_TXX__`.
- Scope: guards of project headers (`.h`, `.txx`) in both ports. About 98
  guards with `VSDK_` and 130 with `_H` in cpp; 10 and 27 in turboc. Not in
  scope: third-party headers (glad, GL), and macros that are not guards
  (`VSDK_LL`, `VSDK_ULL`, `VSDK_HAS_LONG_LONG`, `VSDK_CHECK_BOOLEAN`, …).
- Change the `#ifndef`, the `#define`, any `#endif` comment that repeats the
  name, and any other place that tests the guard macro (grep for each old
  name before replacing it).
- After the change the guards of each port are pairwise distinct. cpp guards
  may stay longer than 32 characters; cpp has no such limit.
- New headers written during the migration use this style in every port that
  has guards.

**Step G2 — turboc only (turboc agent, first task, before L0).** Shorten the
guards so the limit is never approached:

- Every turboc guard is at most 31 characters long, underscores included.
  This covers the headers that already exist and every header added by this
  migration.
- A guard that fits keeps its G1 name, identical to cpp. A guard that does
  not fit is abbreviated word by word, keeping each word's first letter and
  dropping its other vowels, as in `__CSG_SEGMENT_CLASSIFICATION__` →
  `__CSG_SGMNT_CLSSFCTN__`. Apply the same abbreviation to a word wherever it
  appears, so related headers stay recognizable
  (`__PLYHDRL_BNDD_SLD_…__`). If the result is still too long, abbreviate
  further by hand.
- No two guards are equal, and no two are equal in their first 32
  characters. Abbreviation can create collisions (`_TXX` variants, words that
  lose their only difference), so this is checked over the whole port, not
  per file.
- Make the check permanent: extend `turboc/scripts/validateDos83.py`, or add
  a script next to it, to fail when a guard is longer than 31 characters or
  when two guards coincide, and run it in the verification steps below.
- Guards are not class identifiers: `scripts/class_renames_32.csv` and
  `scripts/map_to_83.py` are unaffected unless they match guard text; verify
  by running the mapping on a staging copy.

No new class name of this migration exceeds 32 characters (longest:
`SolidBoundaryTessellationReport`, 31), so `scripts/class_renames_32.csv`
should not need rows; verify method names and other macros too.

#### 6.3.3 Other hazards in this change
- **8.3 mapping**. Every new source and header needs a row in
  `turboc/scripts/mappings_83.csv` with a unique 8.3 name (83 new files land
  in one `csg` directory: choose names that stay unique and recognizable).
  Add the new sources to `base/Makefile` and `base/CMakeLists.txt`.
- **No threads on DOS**. Keep the `VITRAL_WITHOUT_PTHREADS` synchronous path;
  port the `Future` / `Pattern` reference counting only where the turboc
  layer has the same `SharedPointer` usage.
- **Out of scope**: `ShadersExample` and `OpenGL4GeometryRenderer` (no
  shaders, no OpenGL 4 in this port). Report whether `opengl1` needs a
  counterpart for drawing the new geometries. Scene features that need
  resources DOS cannot load (JPEG cubemaps, large OBJ) must fail the same way
  the port already fails for missing optional decoders.
- **Tests**. The port has no GoogleTest. Follow the precedent of
  `turboc/base/src/test/vsdk/toolkit/render/dithering/DitheringTest.cpp`:
  plain programs with the same case names and expectations as the cpp tests,
  returning non-zero on failure. They must build on the host; run on DOS the
  subset that fits (exact arithmetic, interval, CSG evaluator). Expectations
  may be adapted under D7.

#### 6.3.4 Verification, in this order

1. **Host build** with the Makefiles (the path the legacy compilers use):
   `./scripts/compileWithMake.sh clean && ./scripts/compileWithMake.sh`, plus
   `EXTRA_CXXFLAGS=-DVSDK_CHECK_BOOLEAN` once to catch `BOOLEAN` misuse. Run
   the include guard check of step G2, the test programs and
   `testsuite/OfflineExamples/RaytracingOfflineExample` over every scene.
   The host build runs the 32-bit design (D1), so its images are the turboc
   reference, and they are the ones inspected for plausibility (section 7).
2. **DOS VM** (VirtualBox VM named `DOS`), following "Validated DOS VM
   transfer procedure" in `turboc/README.md`: stage with
   `sh scripts/stageDos286.sh` (extend it to copy the new scenes into `ETC`
   under 8.3 names, and `TERRAIN.ALT`), validate with
   `scripts/validateDos83.py`, attach the ISO, extract with `DJTAR`, then
   `BASE`, `BASELINK`, `DOS`, `DOSLINK`, and the example's `COMPILE`. Never
   copy an unmapped source into the staging tree; batch files are CRLF. Render
   at 320x240 at least `csg_lens`, `csg_sculpture`, `quadric_pair`,
   `halfspace_sloped` and `box_offsets` (add `RUNn.BAT` files in the style of
   `run1.bat`) and bring the `.PPM` files back. Compare them with the turboc
   host build at the same resolution; differences are expected to be small
   and are explained, not required to be zero.
3. If the DOS VM cannot be driven from the session, stop at step 1, say so
   plainly, and hand the user the exact DOS commands and the staged ISO path.
   Do not claim DOS verification that did not happen.

The gcc 4.1.2 (Ubuntu 7.04) and gcc 2.7.2.1 (Debian 1.1) VMs are not required
by this plan; write code that respects their rules in `turboc/README.md` and
list them as untested.

## 7. Acceptance and cross-port checks

A port is done when:

1. Every class added or modified in `cpp/base/src/main` in the range has its
   counterpart at the same path (turboc: plus its 8.3 mapping row), and the
   port builds with its normal build.
2. Every cpp test case of the range has a passing counterpart, or is listed
   as not portable with a reason. Pre-existing port tests still pass.
3. `RaytracingOfflineExample` renders all scenes of `etc/geometry/mitscenes`
   (turboc on DOS: the subset of 6.3).
4. **Java and TypeScript**: rendered images match the cpp references of step
   5.2 at the same resolution: identical pixel coverage (same hit / miss per
   pixel) and a maximum channel difference of 2. Larger differences are
   investigated, not tolerated: they usually mean a reordered floating-point
   expression.
5. **turboc (relaxed)**: images are not required to match the other ports
   (decision D6). Each rendered scene must make sense, which the agent
   verifies by looking at the image next to the cpp reference (convert the
   `.ppm` to PNG and read it) and checking that:
   - the image is complete: no crash, no truncated file, not blank or
     uniformly one color;
   - camera framing, background and lighting direction agree with the cpp
     reference;
   - every object of the scene is present, with the expected shape, position
     and material color;
   - CSG scenes show the operation: the subtracted volume is removed, the
     intersection keeps only the common part, cut faces carry the material
     of the leaf that produced them;
   - there are no holes, speckle noise, inverted normals (black faces lit
     from the wrong side) or missing shadows that the reference does not
     have.

   A numeric comparison with the cpp reference is still run and reported per
   scene (pixels that differ in hit / miss, maximum and mean channel
   difference), as information: it locates differences, it does not gate
   acceptance. Each visible difference is attributed to a decision of section
   6.3.1 or treated as a bug. A scene that cannot be rendered because of a
   reduced capacity (D5) is reported as such.

The orchestrator then checks across ports:

- Class lists: the set of class names under `environment/geometry`,
  `environment/scene`, `io/geometry`, `processing`, `common/numerics` is the
  same in the four ports (modulo the documented out-of-scope items).
- Public method names of each new class match between Java and TypeScript
  and match cpp.
- Test case names and counts per test file match across ports (turboc
  expectations may differ where the report lists them under D7).
- Include guards: no `VSDK_…_H` guard remains in `cpp/` or `turboc/`; the
  turboc guard check of step G2 passes.
- `python3 scripts/checkAttributeNames.py --all` passes.

## 8. Agent prompts

Launch all three in a single message. Common body, followed by the port line:

> You are porting the cpp changes between commits
> `7276c54ac72d0f2474bbffbc5e08ecc59648fa5b` (baseline) and
> `4412e55a22ff23e2f4592452585d3b61e3e96435` (target) of this repository to
> the **<PORT>** port under `<PORT_DIR>/`. Read
> `doc/raytracingMigrationsPlan.md` first and follow it exactly: sections 2–4
> for what to port and the parity rules, section 6.<N> for your port, section
> 7 for when you are done. Write only under `<PORT_DIR>/`; do not run git
> commands that change repository state; two other agents are porting the
> same change to the other ports in the same working tree at the same time.
> Work layer by layer (L0–L7), building and running the tests after each
> layer, and keep going until the acceptance criteria are met; do not stop to
> ask for confirmation between layers.
>
> Finish with a report containing: (1) layers completed; (2) table of cpp
> test file → port test file, cases ported / total, pass / fail; (3) render
> comparison per scene; (4) every deviation from 1:1 parity with its reason;
> (5) cpp defects or ambiguities found; (6) changes needed outside your port;
> (7) what was verified by running it and what was not.

- Java: `<PORT>` = Java, `<PORT_DIR>` = `java`, `<N>` = 1.
- TypeScript: `<PORT>` = TypeScript, `<PORT_DIR>` = `typescript`, `<N>` = 2.
- Turbo C++: `<PORT>` = Turbo C++ legacy, `<PORT_DIR>` = `turboc`, `<N>` = 3,
  plus: "The code must stay in the pre-C++98 subset of `turboc/README.md` and
  be verified on the DOS VirtualBox VM as described in section 6.3. Start
  with include guard step G2 (section 6.3.2); step G1 is already applied.
  Section 6.3.1 lets you reimplement modules for 32-bit precision, and your
  images only have to make sense (section 7, item 5). Add to your report:
  the modules you reimplemented and how they differ from cpp, the test
  expectations you adapted, and the guards you abbreviated."

## 9. Risks to watch

- **Size**: about 15,000 lines of library code and 8,000 of tests per port.
  Expect each agent to need continuation; the layer order makes partial
  progress buildable and reportable.
- **Divergent Java / TypeScript choices** where C++ has no equivalent. Section
  4.5 fixes the main ones; anything else an agent has to invent goes in its
  report so the orchestrator can align the two ports.
- **Certified arithmetic in turboc** (integer widths, x87 precision) is
  settled by section 6.3.1: reimplement, prove less, never prove wrong. The
  remaining risk is a reimplementation that silently claims too much; D3 and
  D7 are what the orchestrator reviews first in the turboc report.
- **Guard renaming touches about 230 cpp headers** on top of the target. It
  is mechanical, but it must be built and tested before the agents start, so
  a guard typo is not mistaken for a porting error.
- **cpp still moving**: this plan syncs to one SHA. Later cpp parts are
  ported as a follow-up diff from that SHA, recorded in the port commit
  messages.
