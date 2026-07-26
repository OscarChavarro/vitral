# Stage 10 strict-validation performance log

**Date:** 2026-07-26  
**Host:** macOS 26.5.1, arm64 (Darwin 25.5.0)  
**JVM:** Azul Zulu OpenJDK 17.0.19+10-LTS  
**Gradle:** 9.3.1  
**Test policy:** one fork; JUnit parallel execution disabled

## Full-suite baseline

Command, run from `java/`:

```bash
/usr/bin/time -p ./gradlew :base:test --no-build-cache --rerun-tasks
```

| Revision state | Configuration cache | real | user | sys | Result |
|---|---:|---:|---:|---:|---|
| Before Stage 10 changes | populated by run | 91.66 s | 1.13 s | 0.19 s | pass |
| Initial Stage 10 implementation, default overloads | reused | 84.92 s | 0.84 s | 0.12 s | pass |

The initial post-change sample is 7.35% faster, but this is **not** evidence of
an improvement: the baseline populated Gradle's configuration cache while the
second run reused it. It does show no obvious default-path regression. The
S10.6A alternating multi-fork run is still required before drawing a
performance conclusion.

The explicit `doStrictValidation=false` path has a unit assertion proving that
it makes zero strict-validation invocations. Consequently it does not enter
the strict face-pair scan or allocate the strict topology summary. Following
the A/B result, the shorter overloads were changed to default to `true`;
explicit `false` is now the opt-out path.

The final default-path acceptance run used:

```bash
./gradlew :base:test --no-build-cache --rerun-tasks
```

It executed 420 tests with zero failures or errors (35 skipped) and reported
`BUILD SUCCESSFUL in 1m 28s`. This is about 4% below the frozen 91.66 s
baseline. The timers are not identical—the baseline used external
`/usr/bin/time`, while this result is Gradle's elapsed display—so the
appropriate conclusion is only that no regression was visible in the
initial opt-out-default implementation. The final strict-by-default policy is
represented by the A/B `true` measurements below.

## Full-suite impact of the strict-by-default policy

After changing every shorter overload to delegate with
`doStrictValidation=true`, the same no-build-cache/rerun-tasks command
completed successfully with 420 tests, zero failures/errors and 35 skipped:

| Default policy | Gradle elapsed | Delta |
|---|---:|---:|
| Explicit validation defaulted to `false` | 1 min 28 s | baseline |
| Strict validation defaulted to `true` | 2 min 44 s | +1 min 16 s (+86.4%) |

This broader result supersedes the small-corpus figure for estimating the
whole-suite cost. It includes tests that perform many or much larger booleans,
and some tests also invoke `validateStrict` explicitly after `setOp`, causing a
second validation that normal application callers would not perform. The
strict-by-default policy remains enabled for robustness, but its overall cost
is workload-dependent and can be substantially greater than 2.4%.

## Strict-validation A/B matrix

The comparison reuses the 20 pre-existing
`CsgMoonCylinderDifferenceDegeneracyTest` cases and their assertions.  The
Gradle property changes only the final argument of the six-argument `setOp`
call.  Each mode received one unmeasured warm-up, followed by five alternating
forks. Compilation was already up-to-date and JaCoCo report generation was
excluded:

```bash
./gradlew --quiet :base:test \
  --tests 'vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CsgMoonCylinderDifferenceDegeneracyTest' \
  -PstrictValidationBenchmark=<false|true> \
  -x :base:jacocoTestReport
```

| Mode | Raw real-time samples | Median | p95 | Delta |
|---|---|---:|---:|---:|
| `doStrictValidation=false` | 3.7251, 3.6368, 3.7247, 3.7694, 3.6753 s | 3.7247 s | 3.7694 s | baseline |
| `doStrictValidation=true` | 3.9337, 3.8147, 3.7989, 3.9252, 3.8011 s | 3.8147 s | 3.9337 s | +0.0900 s median (+2.42%); +0.1643 s p95 (+4.36%) |

With five observations, p95 is reported as the nearest-rank maximum. These
figures are wall-clock times for a complete isolated Gradle invocation, so
test-worker and Gradle overhead dilute the cost of strict validation itself.
They nevertheless provide the requested end-to-end comparison: on this
strict-valid CSG corpus, the now-default validation adds about 2.4% to the
median relative to explicit opt-out.

Compilation, Gradle configuration, and JaCoCo report generation must be kept
outside the timed flag-comparison interval.
