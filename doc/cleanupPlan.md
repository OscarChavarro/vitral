# Cleanup plan for the findings of doc/BUGS.txt

Plan to solve the defects, rule deviations and technical debt listed in
[BUGS.txt](BUGS.txt) (review of 2026-11-22). Each stage names the entries
it closes; when a stage lands, its entries move to section 7 ("Closed") of
that file.

## 1. Rules for every stage

- **Java first, then the ports.** The change is made and verified in
  `java/`, and then ported literally to `cpp/`, `turboc/` and `typescript/`
  (same classes, same method names; see
  `doc/attributesGettersAndSettersNames.md`). A stage is done when the four
  ports have it, or when the plan says a port is excluded.
- **Behavior does not change unless the stage says so.** Stages A and C
  change what happens on errors and with degenerate input; the others are
  refactors, and the reference outputs of the offline examples must stay
  byte for byte the same.
- **Verification of each stage**: the test suite of each port
  (`./gradlew :base:test`, the gtest programs of `cpp/build`,
  `make check` in `turboc/`, `npm test` in `typescript/`), the offline
  examples compared with their reference outputs, and
  `scripts/checkAttributeNames.py --all`. Stages touching `turboc/` also
  run `turboc/scripts/checkTurboCpp.py`; a deployment to the DOS and
  Debian 1.1 nodes is asked for at the end of stages A, C and D, not after
  each change.
- **One commit per stage and port**, made by the maintainer. Agents do not
  commit.
- No new dependencies, no `friend`, no project namespaces, no smart
  pointers (project style), English only.

## 2. Order and size

| Stage | Subject | Closes (BUGS.txt) | Size | Risk |
|---|---|---|---|---|
| A | Error handling of readers and fatal errors | 1: readers exit, Logger exit, swallowed errors | small | low |
| B | Quick rule fixes and dead code | 2: friend, English; 3: dead code, cpp warnings | small | low |
| C | Verified clipping inside the polygon clipper | 1: clipper results, hidden surface fallbacks; 3: large classes (in part) | medium | medium |
| D | Reentrancy: triangulation, readers, global state | 1: triangulation, readers; 3: global state | medium | medium |
| E | Console output through Logger | 2: Logger | medium | low |
| F | One tolerance and predicate layer | 3: tolerances | large | high |
| G | `Geometry` API consolidation | 3: overlapping APIs | medium | medium |
| H | Convergence of `cpp/` and `turboc/` | 2: platform forks | large | medium |
| I | Automated checks (CI, parity matrix) | 3: no CI; 4: differences between ports | medium | low |
| J | Port gaps | 1: web editor; 4: readers, animation | medium | low |
| K | Documentation | 3: documentation | small | low |

A and B can start at once and in parallel. C and D are independent of each
other. E needs A (the Logger policy). F should follow C (the clipper is its
first client) and precede the radiosity work. H and I pay for themselves
before the rpkCpp merge, which multiplies the code that lives in two C++
trees. G, J and K have no deadline.

## 3. Stages

### Stage A. Error handling of readers and fatal errors

1. `ReaderAse` (3 places) and `ReaderBinNeedForSpeed` (7 places): replace
   `System.out.println(...); System.exit(0);` by a new
   `GeometryReadException` (an `IOException`, in `io/geometry`) carrying
   the file name and the position. Most import methods of the readers
   declare a bare `throws Exception` today (about 34 of them, against 5
   with `throws IOException`): narrow those declarations to the new
   exception in the same pass. The callers in `java/testsuite` report it
   and go on. Delete the commented `System.exit` lines of `ReaderAse` and
   `ReaderVrml`.
2. Empty `catch` blocks: `ReaderObj` (3), `ReaderAse` (1),
   `ImagePersistence` (4). Each one either rethrows as the reader
   exception, or reports with `Logger.reportMessageWithException` at
   `WARNING` level when going on is correct (i.e. an optional image helper
   that is not available). `RendererConfiguration.clone` keeps its empty
   block, with a comment.
3. `Logger`: make exceptions the default (`withSystemExit = false`), keep
   `-Dvsdk.withSystemExit=true` and `Logger.setWithSystemExit(true)` for
   the command line programs that want the old behavior, and set it in
   those programs (`main` methods of the offline examples) where an exit
   code matters. Same in `cpp/` and `turboc/` (`std::exit` in
   `Logger.cpp`); TypeScript has no exit there.
4. Ports: the C++ readers that exist (`ReaderObj`, `ReaderPly`, the MIT
   ones) are reviewed for the same two patterns; `ReaderAse` and
   `ReaderBinNeedForSpeed` exist only in Java.

Done when: a truncated `.ase` and a truncated NFS file give an exception in
a new test each, and no `System.exit` is left in `java/base` outside
`Logger` and `java.lang` wrappers.

### Stage B. Quick rule fixes and dead code

1. `friend`: in `cpp/base/.../element/PlanarFace.h`, give the nested
   `RayHit` a constructor (or package-style setters) that `PlanarFace`
   uses, and remove `friend class PlanarFace`. Apply the same edit in
   rpkCpp, so the file stays identical in both projects.
2. English: translate the precondition comment of `Camera.updateVectors`
   (all ports that carry it).
3. Dead code: delete the commented-out quartic solver of `Torus.java`
   (1126 lines) and the 186-line block of `ReaderAse`; check the same
   files in the other ports. Then remove the single commented-out code
   lines (about 350 in `java/base`) package by package; comments that
   explain are kept.
4. C++ warnings: add `-Wall -Wextra` to the CMake build of `cpp/` for GCC
   and Clang, fix what it reports in `base/` first, then in the backends.
   Do not add `-Werror` until stage I.
5. `Camera.setFocusedPositionMaintainingOrthogonality`: when `up` is
   parallel to the new front, choose the world axis least aligned with
   the front as the hint (as `HiddenSurfaceRenderer.lightCamera` does),
   and remove the `\todo`. Add a test with a camera looking straight down.

Done when: `grep -rn "friend " cpp/base/src/main` is empty, `cpp/` builds
without warnings in `base/`, and the Torus tests and raytracing references
are unchanged.

### Stage C. Verified clipping inside the polygon clipper

Today `WeilerAthertonPolygonClipper.clipPolygons` may return wrong areas
with degenerate contacts, and only `HiddenSurfaceRenderer` protects itself
(`verifiedClip`, `isConsistentClip`, `clipByConvexPolygon`, `trapezoids`,
`_WeilerAthertonEdgeGrid`).

1. Move those methods, with the 2D utilities they need (`signedArea`,
   `enclosedArea`, `perimeter`, `contains`, `boundingBox`,
   `withoutDegenerateContours`), from `HiddenSurfaceRenderer` to the
   `polygonClipper` package: a `VerifiedPolygonClipper` class (the retry
   policy and the half-plane fallbacks) and a `Polygon2DMeasures` helper.
   `_WeilerAthertonArea` moves with them, renamed without the renderer in
   its name.
2. `VerifiedPolygonClipper` takes the scale of the coordinates as a
   parameter (the raw clipper has absolute 1e-4 tolerances), so callers do
   not need to know them; `HiddenSurfaceRenderer.SCALE` stays as the value
   it passes.
3. `HiddenSurfaceRenderer` keeps the algorithm of the paper, the shadows
   and the screen subdivision, and should drop under about 1300 lines.
4. Other callers of the raw clipper (the polygon clipping example of
   `java/testsuite`, and whatever the other ports have) either move to the
   verified one or state in a comment that they show the raw algorithm.
5. Document in the raw clipper which contacts it does not solve, with one
   small test per case kept as a known failure, so a future fix of the
   clipper itself can be measured.

Done when: the seven tests of `HiddenSurfaceRendererTest` pass unchanged
in the four ports, their timings do not get worse, and the new class has
its own tests (areas add up and sample points fall on the right side for
polygons with vertices on edges, coincident contours and holes).

### Stage D. Reentrancy

1. Polygon triangulation: turn the static work arrays and counters of
   `_Construct`, `_Monotone` and `_RandomSegmentOrder` into fields of one
   context object created by each `triangulate` call and passed down.
   Java, C++, turboc and TypeScript carry the same statics. Test: two
   triangulations interleaved from two threads (Java, C++ with threads)
   give the results of the serial runs.
2. Readers: `Reader3ds` (ten "current" fields), `ReaderBinNeedForSpeed`,
   `ReaderVtk`. Each import call creates an instance (or a private
   context object) that holds that state; the public static entry points
   stay as thin wrappers.
3. Identifier counters of `_PolyhedralBoundedSolidEdge` and
   `_PolyhedralBoundedSolidHalfEdge`: make them owned by the solid they
   number (or atomic, if identifiers must be unique across solids; decide
   from their uses before changing).
4. `CsgCrossingMembershipFilter.membershipTest`: pass the test as a
   parameter of the query (or keep it in the CSG tree), with the current
   value as the default.
5. `HiddenLineRenderer.debugEdgeIndex`: make it an instance option or
   remove it with its debug branch.
6. Boolean set operator: list its static and `ThreadLocal` state
   (`_PolyhedralBoundedSolidSetOperator`, the classifiers, the null edges
   connector, the finisher) and move it to the operation context object
   that already exists for part of it (`_SetOperationContext`). This is the largest step; it ends
   when the Java tests can run in parallel inside one JVM
   (`junit.jupiter.execution.parallel.enabled=true`) and the comment of
   `java/base/build.gradle` about serial tests is removed.

Done when: no non-final static field remains in the packages above except
constants and caches documented as thread safe.

### Stage E. Console output through Logger

1. Classify the about 320 active `System.out` / `System.err` calls of
   `java/base`: debug traces of the boolean classifiers (remove, or move
   behind the trace switches of `_SetOperationTrace`), messages of the readers
   (to `Logger` at `WARNING` or `ERROR`), statistics reports (keep, as a
   method that writes to a given `PrintStream`).
2. Same pass for `printf` in the C++ cores (about 240 each) and `console`
   in `@vitral/base` (about 140).
3. Add a host check (next to `scripts/checkAttributeNames.py`) that fails
   when a new direct console call appears in a core outside an allow
   list.

Done when: the allow list holds only the statistics and `Logger` classes.

### Stage F. One tolerance and predicate layer

1. Inventory: for each literal tolerance of the cores (some 13 different
   values besides `VSDK.EPSILON`), record what it compares (lengths,
   areas, parameters, angles, normalized or world units).
2. Introduce a small `GeometricTolerance` family in `common` (or extend
   `PolyhedralBoundedSolidNumericPolicy` upwards): named tolerances
   relative to a scale given by the caller, instead of absolute literals.
3. Introduce robust 2D predicates (orientation of three points, point on
   segment, segment intersection) with one implementation, used by the
   polygon clipper, the triangulation and the hidden surface renderer.
   Start with the clipper: its absolute 1e-4 comparisons become relative
   to the bounding box of its input, which removes the need for callers
   to scale coordinates (stage C step 2 then becomes a no-op parameter).
4. Move the boundary representation kernel and the raytracer to the
   named tolerances last, one package per commit, with the fuzz suites
   (`slowTest`) run before and after.

This stage changes numeric results in the last digits. Reference outputs
are regenerated once per package, and the regeneration is reviewed, not
assumed.

### Stage G. `Geometry` API consolidation

1. Containment: keep `classifyPoint` and `classifyPointCertified`
   (`GeometryContainment`); reimplement `doContainmentTest` (int
   constants) as a final wrapper, move its callers, then deprecate and
   remove it.
2. Rays: decide which of `doIntersectionFirstHit` and `queryFirstHit` is
   the entry point for new code (the CSG work points to `queryFirstHit`),
   document the other as the implementation hook of the subclasses, and
   make that visible in names or visibility.
3. `RayHit`, `Vertex2D` and the half-edge nodes: public fields to
   accessors where an invariant exists (a unit normal, consistent
   topology); plain value holders may stay as they are, with that stated
   in the class documentation.

### Stage H. Convergence of `cpp/` and `turboc/`

Of 954 files with the same path, 64 are identical.

1. Measure: a script (`scripts/checkPortDivergence.py`) that reports, per
   file, the size of the diff between `cpp/base` and `turboc/base` after
   normalizing what the legacy subset forces (`bool` / `BOOLEAN`, loop
   index declarations, `nullptr`, `override`). The report shows which
   differences are mechanical and which are real.
2. Remove the real ones: where `turboc` has a fix `cpp` lacks, or the
   reverse, port it.
3. Decide, with the report in hand, between the two ways to stop writing
   every change twice:
   - generate `turboc/base` from `cpp/base` with a transformation script
     (as `map_to_83.py` already does for DOS names), keeping hand-written
     only the files that can not be generated; or
   - write `cpp/base` itself in the common subset wherever it costs
     nothing, so most files become identical and are shared.
4. Whatever is chosen, the check of step 1 goes into stage I so the trees
   do not drift again.

### Stage I. Automated checks

1. A single entry script per port that builds and runs its fast tests and
   exits non zero on failure (most exist: collect them under one name).
2. A parity matrix generated from the sources: for each class of
   `java/base`, whether it exists in each port, and for each public method
   name, whether it is present. It replaces the hand-written feature table
   of the README, and it is what would have shown that the web editor did
   not get the hidden surfaces mode.
3. Run 1, 2, the name checks, the console check of stage E and the
   divergence check of stage H on every push, on a modern Linux runner
   (Java, C++, turboc built with gcc, TypeScript). The legacy and DOS
   nodes stay on request, through the procedure of `doc/portability.md`.
4. Then turn `-Werror` on for `cpp/base`.

### Stage J. Port gaps

1. TypeScript web scene editor: hidden surfaces render mode. Port
   `ProjectedPolygon`, `ProjectedSurfacesViewBuilder` and a canvas 2D
   renderer equivalent to `AwtProjectedPolygonRenderer`, add the mode to
   the render mode popup and to the MCP protocol (`hidden_surfaces`,
   `withShadows`). Check `F11` in the browsers in use; if it can not be
   taken, move the raytraced image to a menu item and document it.
2. Readers: decide per format whether it is wanted in every port (PLY and
   STL are cheap and useful) or stays Java only (NFS, ASE, 3DS), and write
   that decision in the README table instead of leaving it as a gap.
3. Animation in turboc: port or mark as excluded.

### Stage K. Documentation

1. `doc/TODO.txt`: check each of its about 30 items against the code, as
   was done for BUGS.txt; close, rewrite or move to this plan.
2. `doc/readme.txt` (coding standard of 2008): merge what still applies
   into `AGENTS.md` and the port READMEs, delete the rest.
3. `doc/ArchitectureAndDesignSpecification.doc`: export what is still
   true to Markdown, keep the binary only as history.
4. The other READMEs of the repository (17 not reviewed in this pass):
   check links and claims with the same method.

## 4. Before the rpkCpp merge

The radiosity and global illumination code of rpkCpp (about 560 files
still outside `cpp/base`) should arrive after stages A, B, D and H, and
ideally F:

- It will run the triangulation and the geometry queries from several
  threads (stage D).
- Every file that enters `cpp/base` today must be written again for
  `turboc/base` (stage H).
- Galerkin form factors and Monte Carlo estimators are sensitive to the
  tolerances of the intersection code (stage F).
- rpkCpp is validated with reference images only; unit tests for its
  estimators should be written as each package is moved, following the
  verification rules of section 1.

## 5. Tracking

| Stage | java | cpp | turboc | typescript | Notes |
|---|---|---|---|---|---|
| A | | | | | |
| B | | | | | |
| C | | | | | |
| D | | | | | |
| E | | | | | |
| F | | | | | |
| G | | | | | |
| H | n/a | | | n/a | |
| I | | | | | |
| J | n/a | | | | |
| K | | | | | |
