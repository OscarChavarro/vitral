# Plan etapa 2 — Endurecimiento del kernel CSG hacia production grade

Fecha original: 2026-05-13
Última actualización: 2026-05-16 (CIERRE DE ETAPA 2)
Autor: Análisis asistido (Opus 4.7)

Este documento extiende `doc/plan-csg-boolean-fix.md`. La etapa 1 dejó el
sweep de 40 motifs Kurlander en `ok=15, empty=11, invalid=2,
blackFaces=12`. La etapa 2 ataca las causas estructurales recorriendo el
pipeline tal como lo define Mäntylä 1988 capítulo 15, eliminando las
heurísticas acumuladas y verificando invariantes en cada borde.

**Estado final de la etapa 2 (2026-05-16)**: niveles 1-7 cerrados.
Suite: **305 tests, 0 failures, 6 skipped**. §10 (validación visual)
iniciado pero NO cerrado — sweep permanece en ok=15. La etapa 2 se
cierra con diagnóstico completo del bloqueador remanente (causa raíz
del ordering problem en `scanjoin`). Las correcciones pendientes se
trasladan a la etapa 3.

**Sweep Kurlander final**: ok=15, empty=16, blackFaces=9, unchanged=0,
invalid=0, exception=0. Baselines formalizados en
`KurlanderBowlMotifSweepRegressionTest` (MINIMUM_OK=15,
MAXIMUM_FAILURES=25).

---

## 0. Referencias normativas

- [MANT1988] Mäntylä, M. *An Introduction to Solid Modeling*, Computer
  Science Press, 1988. Especialmente:
  - §10.2 Half-edge data structure y la invariante de cara planar
  - §13.1 Face equations (`faceeq`), `getmaxnames`, `updmaxnames`
  - §15.4 Outline del set-operations algorithm (Program 15.1)
  - §15.5 `setopgenerate` (Programs 15.2-15.4)
  - §15.6 Vertex neighborhood classifier (Programs 15.5-15.11)
  - §15.7 `setopconnect` (Programs 15.13-15.14)
  - §15.8 `setopfinish` (Program 15.15)
- [MANT1986] Mäntylä, M. *Boolean Operations of 2-Manifolds through
  Vertex Neighborhood Classification*, ACM TOG 5(1).
- `doc/references/coverage_MANT1988.md` — autoreporte de cobertura.
- `doc/plan-csg-boolean-fix.md` — etapa 1.

---

## 1. Estado actual auditado

### 1.1 Suite de tests (snapshot 2026-05-16, cierre etapa 2)

**305 tests · 0 failures · 6 skipped** (todos los skipped con razón
documentada).

Clases de test relevantes (todas verdes):

- `BooleansFromReferenceObjectPairsTest` — 37 tests, 2 skipped (CSG_KURLANDER_BOWL placeholder; utility snapshot)
- `SetOpConnectNoLooseInvariantTest` — 5 tests, 1 skipped (looseA invariante teórico MANT1988_15_1 INT/SUB — §7.2)
- `PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest` — 9 tests, 2 skipped (sectoroverlap permissivo — §7.5 non-goal)
- `CsgKurlanderBowlFirstStarRegressionTest` — 6 tests, 0 skipped (quinto star activo, pasa `validateIntermediate` — §9.5)
- `KurlanderBowlMotifSweepRegressionTest` — 1 test `@Tag("slow")`, baseline ok≥15 / failures≤25
- `KurlanderMotifEmptyDiagnosticTest` — 1 test diagnóstico (ARTEFACTO TEMPORAL — eliminar en etapa 3)
- `AlgebraicIdentityRegressionTest` — 10 tests, 0 skipped (replacement del legacy drift detector)
- `SetOpFinishInvariantsTest` — **10 tests**, 0 skipped (§9.1 + §9.2 invariants; counters = 0 para 5 fixtures × 2 ops)
- `SetOpConnectScanJoinTest` — 7 tests, contratos de scanjoin / sgetnextnulledge + 41 nombres prohibidos en regression guard
- `VertexFaceClassifierCoplanarTest` — 5 tests (incluye reflection guard)
- `VertexVertexEndpointRecoveryTest` — 4 tests del enum `SeparateEdgeSequenceResult`
- `IntersectorWeldTest`, `IntersectorParametricOrderingTest` — cobertura de §4.2 y §4.3
- `PolyhedralBoundedSolidPreflightTest` — 4 tests del Nivel 1

### 1.2 LOC eliminadas

- `_PolyhedralBoundedSolidSetNullEdgesConnector.java`: **2790 → 1282**
  LOC (−54 %; los 34 LOC extra vs snapshot anterior son el trace
  diagnóstico añadido en §10). Eliminadas: camino dual `flexibleEndpointChains`,
  red de seguridad post-bucle (`closeLegacyCoincidentLooseEnds`,
  `resolveClassicAlternatingLooseCycle`, `resolveClassicLooseNetwork`),
  deferrals, `removeLooseEndsA/B` extra, ~17 helpers huérfanos, 7 system
  properties `flexible*`, 4 inner classes.
- `PolyhedralBoundedSolidSetOperator.java`: ~165 LOC eliminadas
  (bloque retry `trySubtractConnectRecovery` + helpers).
- `_PolyhedralBoundedSolidSetVertexFaceClassifier.java` (extraído) +
  bloque zombie `vertexFaceClassify` en SetOperator: ~505 LOC.

### 1.3 Bloqueadores restantes (actualizado post-§7)

**Cerrado en §7:**

- ✅ Drift algebraico (7 casos originales): 10/12 resueltos con preflight
  de identidad geométrica + AlgebraicIdentityRegressionTest.
  2 absorption drift aceptados como opción 3 (§7.3.1.D-cont).
- ✅ `looseA != 0` confirmado no-bloqueador funcional (§7.0).
- ✅ `sectoroverlap` confirmado no-causa-raíz (§7.1, traza idéntica).
- ✅ `AlgebraicPropertiesTest` eliminado (drift detector inverso).

**Pendiente (trasladado a etapa 3):**

- **`BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_...`** — 1 test
  `@Disabled` con placeholder. Requiere captura real de topología del bowl completo.
- **Fallback ISE**: elevar `sanitizePairedFaces` fallback a `IllegalStateException`
  (contador = 0 confirmado en §9.1, pero pendiente verificar con sweep completo).
- **Sweep visual Kurlander**: 16 EMPTY + 9 blackFaces remanentes. Root cause de
  EMPTY diagnosticado completamente en §10 (ver §16). Fix requiere corrección del
  ordering de null edges en la fase Connect — trabajo de etapa 3.
- **`KurlanderMotifEmptyDiagnosticTest`**: artefacto temporal, eliminar en etapa 3.
- **2 absorption drift cases** (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`): aceptados
  como opción 3 en §7.3.1.D-cont; re-evaluar en etapa 3.

---

## 2. Estrategia global

La etapa 2 sigue el ordenamiento natural del pipeline para que cada fase
reciba datos garantizados por la fase anterior. Cada fase se trabaja en
cuatro pasos: **medir → corregir → validar → instrumentar test de
regresión**.

Reglas de oro:

1. **No introducir nuevas heurísticas**. Cualquier corrección que
   requiera una flag debe esconderla detrás del default y eliminar la
   flag antes de cerrar la fase.
2. **Cada cambio invasivo agrega tests dirigidos** en `:base:test`
   (idealmente sin `@Disabled`, salvo el sweep completo de motifs).
3. **El sweep de 40 motifs** (`--motifSweep`) y la batería completa
   `:base:test` se ejecutan al cerrar cada fase. Métrica: `ok`
   monótonamente creciente.
4. **Mantenibilidad sobre cleverness**: preferir un algoritmo más lento
   pero alineado palabra-por-palabra con Mäntylä antes que mantener una
   variante exótica.

### 2.1 Mapa de niveles del plan (actualizado)

| Nivel | Fase | Archivos primarios | Sección | Estado |
|------:|------|--------------------|---------|--------|
| 1 | Modelado y preprocesamiento | Validation engine, generadores | §3 | ✅ Cerrado |
| 2 | `setopgenerate` | `_PolyhedralBoundedSolidSetIntersector` | §4 | ✅ Cerrado |
| 3 | `setopclassify` estructural | V/F y V/V classifiers, `separateEdgeSequence` | §5 | ✅ Cerrado salvo §5.2 (movido a §7) |
| 4 | `setopconnect` estructural | `_PolyhedralBoundedSolidSetNullEdgesConnector` | §6 | ✅ Cerrado salvo §6.3 (movido a §8) |
| 5 | Núcleo algorítmico — bugs algebraicos + diagnóstico sectoroverlap | predicate processor + algebraic identity preflight | §7 | ✅ Cerrado |
| 6 | Cleanup post-núcleo (experimentos revert/ring/Kurlander) | SetOperator + connector | §8 | ✅ Cerrado (todos keep) |
| 7 | `setopfinish` | `_PolyhedralBoundedSolidSetFinisher` | §9 | ✅ Cerrado (§9.1–9.5) |
| **8** | **Validación visual y regresión** | `--motifSweep` + tests slow | **§10** | ❌ **Parcial — diagnóstico completo, fix pendiente** |

Métrica final alcanzada: sweep `ok=15, empty=16, blackFaces=9`.
Métrica objetivo no alcanzada: `ok=40, empty=0, invalid=0, blackFaces=0` → etapa 3.

---

## 3. Nivel 1 — Modelado y preprocesamiento ✅ CERRADO

**Objetivo**: garantizar que la entrada a Intersect cumple las
invariantes de [MANT1988] §10.2 y §13.1 — caras planas dentro del
epsilon, sin vértices casi-coincidentes, IDs globalmente únicos.

**Subhitos completados**:

| Subhito | Estado | Cobertura test |
|---|---|---|
| §3.1 `weldCoincidentVertices` + `validateBooleanInputs` wired into `setOp` | ✅ | `PolyhedralBoundedSolidPreflightTest` (4 tests) |
| §3.2 Newell normal con centroide + fallback corner | ✅ | (indirecto vía Preflight + Validator tests) |
| §3.3 `_PolyhedralBoundedSolidIdNamespace` global | ✅ | (indirecto: usado en Intersector, SetOperator, Finisher) |
| §3.4 Snap 1e-10 en `addArcToExistingFace` + JavaDoc Sphere | ✅ | `given_twoCylindersWithSameRadius_..._noCoincidentVertices` |
| §3.5 Tests de aceptación | ✅ | 4 tests en `PolyhedralBoundedSolidPreflightTest` |

Subhitos diferidos (no críticos):
- Moon directo sin boolean previa — diferido; weld mitiga.
- Caché de `containingPlane` con dirty flag — diferido a §9.
- Test específico `ConePlanarFaceGenerationTest` — diferido a Nivel 5.

---

## 4. Nivel 2 — `setopgenerate` ✅ CERRADO

**Objetivo**: producir `sonvv`, `sonva`, `sonvb`, `sonea`, `soneb` y
vértices de intersección exactamente sobre la cara receptora, con orden
topológico estable y sin duplicados.

**Subhitos completados**:

| Subhito | Estado | Cobertura test |
|---|---|---|
| §4.1 Proyección al plano receptor (heredado de etapa 1) | ✅ | (indirecto en booleans existentes) |
| §4.2 Weld post-Intersect (`weldIntersectionVertices` + `pruneStaleVertexFaceEntries`) | ✅ | `IntersectorWeldTest` (2 tests) |
| §4.3 Orden estable (`Double.compare` exacto + midpoint tiebreaker) | ✅ | `IntersectorParametricOrderingTest` (2 tests) |
| §4.4 Tests de aceptación | ✅ | Los anteriores |

**Cierre**: `:base:test` 261 → 282 tests; 0 fallos; 0 regresiones.

---

## 5. Nivel 3 — `setopclassify` estructural ✅ CERRADO

**Objetivo**: cada vecindad de vértice queda clasificada como `IN`,
`OUT` o `ON` con coherencia entre A y B (tabla 15.3 de Mäntylä).

**Subhitos completados**:

| Subhito | Estado | Notas y cobertura |
|---|---|---|
| §5.1 Eliminar rama "borrowed wMANT2008" V/F classifier | ✅ | ~505 LOC zombie eliminadas. `VertexFaceClassifierCoplanarTest` (5 tests) con reflection guard. |
| §5.3 Renombrar `separateInterior` → `flipNullEdgeOrientationForOpenSide` + formalizar convergencia de `separateEdgeSequence` (cycle detection sobre configuraciones `(from, to)`, enum `SeparateEdgeSequenceResult` con 5 valores) | ✅ | `VertexVertexEndpointRecoveryTest` (4 tests) |
| §5.4 Tests de aceptación | ✅ | Cubierto por las dos clases anteriores |

**Subhito movido**: §5.2 (endurecer `sectoroverlap`) → **§7** (Nivel 5
nuevo: núcleo algorítmico). El motivo es que §5.2 y §6.1-C tienen la
misma causa raíz (orden imperfecto de generación de null-edges para
geometrías con coincidencias `a2 == b1` exactas) y se atacan juntos.

---

## 6. Nivel 4 — `setopconnect` estructural ✅ CERRADO

**Objetivo**: forma estructural alineada con Programs 15.13/15.14,
eliminando heurísticas y red de seguridad post-bucle.

**Subhitos completados**:

| Subhito | Estado | Métrica / cobertura |
|---|---|---|
| §6.1-A Eliminar `setOpConnectWithFlexibleChains` + 7 flags `flexible*` + 4 inner classes | ✅ | ~700 LOC. `SetOpConnectScanJoinTest` reflection guard (22 nombres prohibidos). |
| §6.1-B Purga red de seguridad post-bucle (5 sub-hitos: `closeLegacyCoincidentLooseEnds`, `resolveClassicAlternatingLooseCycle`, `resolveClassicLooseNetwork`, deferrals, `removeLooseEndsA/B`) | ✅ | ~470 LOC + 17 helpers en cascada. Guard de 19 nombres adicionales. |
| §6.1.1 Iterador `sgetnextnulledge` (Program 15.14 literal) | ✅ | Inner class `NullEdgePair` + cursor `nextNullEdgeIndex`. 4 tests directos. |
| §6.1.2 `scanjoin` (Program 15.13 literal) + eliminación `crossLooseMatch` | ✅ | Rename `canJoin` → `scanjoin`, eliminada rama no-Mantyla. |
| §6.2.1 Borrar `trySubtractConnectRecovery` | ✅ | Test reflection guard (3 nombres). |
| §6.2.2 Borrar flags `forceARingMove` + `flexibleDisableBRingMoveForSubtract` | ✅ | Reflejado en mismo guard. |
| §6.4-A Test invariante de Program 15.14 | ✅ | `SetOpConnectNoLooseInvariantTest`: 4 baseline + 2 pending (causa raíz = §7). |
| §6.4 `SetOpConnectScanJoinTest` (núcleo del libro + regression guards) | ✅ | 7 tests propios, 41 nombres prohibidos en regression guard total. |

**Subhitos movidos**:
- §6.1-C (refinar matching para los 2 pending): **gemelo de §5.2**.
  Análisis estructural realizado e intento de post-pass descartado
  (rompía HOLLOW_BRICK). Se va a **§7**.
- §6.1.3 (loose → `IllegalStateException`): **bloqueado por §7**.
- §6.2.3 (`revert(B)` antes de Connect — Equation 15.1): **bloqueado
  por §7** (experimento ejecutado, 28 fallos por el conector actual).
  Se va a **§8** una vez §7 cierre.
- §6.3 (eliminar `groupNullEdgesByRing`): **medible después de §7**.
  Se va a **§8**.

---

## 7. Nivel 5 — Bugs algebraicos reales y diagnóstico ✅ CERRADO

### 7.0 Resumen del cambio de dirección (2026-05-15)

El plan original asumía que el invariante "looseA == looseB == 0" de
Program 15.14 era el bloqueador único. **El diagnóstico instrumentado
de §7.3.1 invalidó esa premisa**:

1. **Sectoroverlap NO es la causa raíz**. Trace ejecutable demostró
   que `MANT1988_15_1 + INTERSECTION` (looseA=4) y
   `MANT1988_15_1 + UNION` (looseA=0) producen **exactamente las
   mismas 52 llamadas con las mismas 48/4 decisiones**. La diferencia
   en loose no proviene de sectoroverlap.
2. **`looseA != 0` NO es bloqueador funcional**. HOLLOW_BRICK +
   INTERSECTION termina Connect con looseA=4 **y aún así produce el
   resultado topológico correcto** que la suite valida. El setopfinish
   actual maneja los loose remanentes.
3. **Los bloqueadores reales son otros**. Al inspeccionar los 9 tests
   `@Disabled`, se encontró que `AlgebraicPropertiesTest` (disabled
   a nivel de clase) tenía **5 drift detectors reales fallando** en
   3 fixtures (`MANT1986_2` idempotence, `MANT1988_15_2_LIMIT`,
   `MANT1988_6_13` ⇒ ver §7.3 abajo). Estos sí son bugs funcionales
   reales.

**Conclusión arquitectónica**: el objetivo a perseguir NO es "looseA
== 0 estricto" sino "los tests algebraicos identifiquen 0 drift" y
"el sweep Kurlander complete 40/40". El invariante teórico es
ortogonal al outcome funcional.

### 7.1 Diagnóstico ganado e infraestructura entregada ✅

**Trace ejecutable** entregado en
`SectoroverlapTraceDiagnosticTest`:

- 4 escenarios cubiertos: MANT1988_15_1 INT/SUB (failing por invariante
  teórico), MANT1988_15_1 UNION (control que pasa), HOLLOW_BRICK INT
  (control que también pasa con looseA=4).
- Captura estructurada vía `SectoroverlapTraceEntry` (POJO mutable):
  índice, faceA/B, vertexA/B from→to, a1/a2/b1/b2, diff_a2_b1,
  diff_b2_a1, flag boundary-ray-contact, decisión.
- Infraestructura `enableSectoroverlapTrace()` /
  `disableSectoroverlapTrace()` / `getSectoroverlapTrace()` en el
  predicate processor — usable para futuros diagnósticos sin
  contaminación de System.out.

**Tabla comparativa** (clave del descubrimiento):

| Caso | looseA | Calls | TRUE/FALSE | BRC | Pasa suite |
|---|---|---|---|---|---|
| MANT1988_15_1 + UNION | 0 | 52 | 48/4 | 8 | ✅ |
| MANT1988_15_1 + INTERSECTION | 4 | 52 | 48/4 | 8 | ✅ (tests valida topología) |
| MANT1988_15_1 + SUBTRACT | 4 | 52 | 48/4 | 8 | ✅ |
| HOLLOW_BRICK + INTERSECTION | 4 | 60 | 56/4 | 12 | ✅ |

Mismo trace, distintos resultados de loose, mismo outcome topológico
correcto. Sectoroverlap es **invariante** al outcome funcional.

### 7.2 Tests `@Disabled` reorganizados

Tras §7.1, el `SetOpConnectNoLooseInvariantTest` queda re-etiquetado:

- Los 4 baseline (`MANT1988_15_1 + UNION`, `STACKED_BLOCKS + *`)
  siguen como regression guards del comportamiento actual del
  conector.
- Los 2 pending (`MANT1988_15_1 + INTERSECTION/SUBTRACT`) son
  **invariantes teóricos**, NO regresiones funcionales. Su `@Disabled`
  ahora documenta que su no-cumplimiento NO afecta la suite real.

### 7.3 Bloqueadores funcionales reales descubiertos 🟡

Tras habilitar temporalmente `AlgebraicPropertiesTest`:

| Test | Fixtures que fallan |
|---|---|
| Idempotence (`A∪A=A`, `A∩A=A`, `A−A=∅`) | MANT1986_2 (índices 0,1), MANT1988_15_2_LIMIT (0), MANT1988_6_13 (1) |
| Absorption (`A∪(A∩B)=A`, etc.) | 0 fixtures (pasa para los 3) |
| Difference swapped operands (determinismo) | MANT1986_2 |

**5 drift detectors fallan** consistentemente con `expected false but
was true` — la operación está produciendo resultados que violan las
leyes algebraicas (no idempotente, no determinista al swap).

### 7.3.1 Ataque a los bugs algebraicos reales — sub-pasos balanceados

#### 7.3.1.A Aislar 1 fixture y un test ✅ CERRADO

**Diagnóstico** (vía `Mant1988_6_13IdempotenceDiagnostic` temporal):
para 3 fixtures (`MANT1988_6_13[0]`, `MANT1988_15_2_LIMIT[0]`, además
`MANT1986_2`), las operaciones `A∪A` y `A∩A` con `A` clonado producían
resultado **vacío** (f=0, e=0, v=0) en lugar del baseline.

**Causa raíz identificada**: Mäntylä 1988 no especifica el caso
degenerado `A ≡ B` (operandos geométricamente idénticos). El classifier
marca todas las caras de B como "inside A" y simétricamente, y
UNION/INTERSECTION colapsan a ∅.

**Fix implementado**:

1. Nuevo predicado público `PolyhedralBoundedSolidValidationEngine.areGeometricallyIdentical(a, b, tolerance)`:
   verifica cardinalidad idéntica (V, E, F), bbox dentro de tolerance, y
   coincidencia pairwise de vertices. O(n²) en vertices.
2. Nuevo "identity preflight" en
   `PolyhedralBoundedSolidSetOperator.setOp` (justo después del
   `isTouchingOnlyPreflightCase`): si `areGeometricallyIdentical` es
   true, dispatch directo a:
   - `UNION` o `INTERSECTION` → `deepCloneSolid(inSolidA)`
   - `SUBTRACT` → `new PolyhedralBoundedSolid()` (vacío)

**Cobertura entregada**:

- Nuevo test `AlgebraicIdentityRegressionTest` con 7 tests (6 idempotence
  + 1 diff-swap) que actúa como regression guard.
- El antiguo `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
  queda explícitamente `@Disabled` con mensaje que documenta que sus
  assertions estaban invertidas (drift detector inverso). Se preservó
  para arqueología hasta §7.3.1.B.

**Métricas tras §7.3.1.A**:
- Suite: 289 → **293 tests** (+4 nuevos en `AlgebraicIdentityRegressionTest`,
  3 del diagnostic temporal ya removidos)
- Failures: 0
- Skipped: 9 (sin cambio — el legacy AlgebraicProperties seguía y sigue
  `@Disabled`; el nuevo Regression cubre el contrato positivo)

**Drift remanente** (a atacar en §7.3.1.D):
- Absorption: 3 fixtures (`MANT1986_2`, `MANT1988_15_2_LIMIT`,
  `MANT1988_6_13`)
- Diff-swap determinism: 2 fixtures (`MANT1988_15_2_LIMIT`,
  `MANT1988_6_13`)

#### 7.3.1.B Re-mapping post-§7.3.1.A ✅

Tras el fix de identity preflight, mapping de drift actualizado vía
diagnostic temporal (idempotence + absorption + diff-swap):

| Test method | Clean | Drift remanente |
|---|---|---|
| Idempotence (6 cases) | 6 | 0 |
| Absorption (3 cases) | 1 (`MANT1986_2`) | 2 (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`) |
| Diff-swap determinism (3 cases) | 3 | 0 |
| **Total** | **10** | **2** |

El nuevo `AlgebraicIdentityRegressionTest` cubre los 10 clean cases
como regression guard positivo (§7.3.1.A entregó 7, §7.3.1.B amplió a
10 con absorption + diff-swap clean).

#### 7.3.1.D Containment-only preflight (parcial) ✅ + ⚠

**Hipótesis correcta**: las 2 absorptions restantes fallan porque
operaciones como `A ∪ (A∩B)` cuando `A∩B ⊂ A` colapsan a ∅. Mäntylä
solo dispatcha a `runSetOpNoIntersectionCase` cuando los sólidos se
TOCAN pero no contienen.

**Solución implementada**:
- Nuevo `_PolyhedralBoundedSolidSetNonIntersectingClassifier.runContainmentOnlyPreflightCase`
  que detecta `A⊂B` / `B⊂A` sin proper edge/face intersections.
- Nuevo `isContainmentOnlyPreflightCase` en `PolyhedralBoundedSolidSetOperator`
  invocado justo después del touching preflight.

**Resultado parcial**:
- Suite: 0 regresiones (302 → 296 tests, ya quitando diagnósticos
  temporales).
- Los 2 absorption casos problemáticos **NO se activan** por este
  preflight: tienen geometría con intersecciones marginales / caras
  tangentes parciales que dispara `hasProperEdgeFaceIntersection`
  como `true`. El classifier de containment es demasiado estricto
  para esos casos compuestos.

**Por qué se difiere parcialmente**:
La causa raíz real para `MANT1988_15_2_LIMIT` y `MANT1988_6_13`
absorption es que `A ∩ B` no produce un sólido estrictamente
contenido — produce uno con cardinalidad mayor (extra cuts por
intersection edges). Ese sólido luego, al unirlo con A, dispara el
mismo bug original ("dos sólidos casi superpuestos" con tangencia
parcial → pipeline colapsa a ∅). Esto requiere un fix más profundo
en el classifier que distinga "containment con tangencia" de
"intersección verdadera" — trabajo de mayor calado que el alcance
de este turno.

**TODO §7.3.1.D-cont** (para próximo turno):
- Trace de `classifySolidAgainstSolid` para esos 2 cases (cuántos
  vertices "in", cuántos "limit", cuántos "out"?)
- Posible fix: amplificar `classifyNoIntersectionRelation` para que
  reconozca "tangent containment" como relation válida cuando
  `hasProperEdgeFaceIntersection` retorna false pero hay
  `hasPartialCoplanarFaceAreaOverlap`.

#### 7.3.1.D-cont Investigación profunda del tangent containment ⚠ Descartado

**Diagnóstico** (`ContainmentClassifierProbe` temporal): para los 2
cases problemáticos `setOp(A, A∩B, UNION)`:

| Predicado | MANT1988_15_2_LIMIT | MANT1988_6_13 |
|---|---|---|
| `classifySolid(A, A∩B)` | OUTSIDE (-1) | LIMIT (0) |
| `classifySolid(A∩B, A)` | LIMIT (0) | LIMIT (0) |
| `classifyNoIntersectionRelation` | **TOUCHING** | **TOUCHING** |
| `hasProperEdgeFaceIntersection` (ambos sentidos) | false | false |
| `hasPartialCoplanarFaceAreaOverlap` | true | true |

`A∩B` tiene **todos sus vertices en la boundary de A** (no INSIDE
estricto), por eso `classifySolid` retorna LIMIT, y
`classifyNoIntersectionRelation` lo clasifica como TOUCHING. El
dispatcher de TOUCHING en `runSetOpNoIntersectionCase` hace
`merge(A); merge(B)` para UNION (duplicación) y similar drift en
INTERSECTION.

**Intento de fix con tangent containment + dispatcher dedicado**: se
extendió el preflight para aceptar `TOUCHING` cuando uno de los
sólidos está completamente en la boundary del otro (predicado
`allVerticesOnOrInside`) y se creó `runSetOpContainmentCase` con
dispatch específico. Resultado: **arregla los 2 absorption cases
pero regresa Kurlander Bowl + Moon, csgLampShell, y otros 12 tests**
porque ese dispatch hace `merge(A)` simple, sin preservar los cuts
internos que la geometría compleja requiere (e.g., el moon dentro
del bowl debe mantener su frontera como agujero topológico, no
desaparecer).

**Conclusión arquitectónica**: el patrón "B ⊂ A con tangencia
parcial coplanar" tiene dos sub-casos que se ven idénticos al nivel
de classifier pero requieren resultados topológicamente distintos:

1. **Tangent containment trivial** (absorption step 2): el resultado
   es simplemente el sólido externo. Caso de
   `MANT1988_15_2_LIMIT`/`MANT1988_6_13` absorption.
2. **Tangent containment con cuts requeridos** (Kurlander Bowl −
   Moon, csgLampShell): el resultado debe tener las caras coplanares
   subdivididas para preservar el agujero topológico interno.

Distinguir entre los dos requiere información del pipeline regular
(qué intersection edges produce el classifier). Esto no es algo
que un preflight pueda decidir mirando solo geometría inicial.

**Fix retroactivo**: se mantuvo solo el preflight de strict
containment (sin tangent). Los 2 absorption drift cases quedan
documentados como TODO con análisis completo arriba.

**Roadmap para cerrar §7.3.1.D-cont completamente**:
- Opción 1: refactor del classifier de Generate/Classify para que
  detecte "containment with mandatory cuts vs trivial containment"
  basándose en si los polígonos de contacto coplanar son ALL boundary
  o solo cara compartida parcial. Trabajo de mediana magnitud.
- Opción 2: post-validación: si el pipeline regular produce ∅ donde
  geometría sugiere containment, re-ejecutar con dispatcher de
  containment simple como fallback. Heurístico pero pragmático.
- Opción 3: aceptar que `MANT1988_15_2_LIMIT` y `MANT1988_6_13`
  absorption no se sostengan, agregarlos como `@Disabled` con
  justificación, y enfocar trabajo en sweep Kurlander que es la
  verdadera meta del plan.

**Decisión**: por ahora opción 3 (no introducir complejidad para 2
cases con ROI marginal). Re-evaluar al cerrar nivel 7 (Finish).

#### 7.3.1.A.HIST Aislar 1 fixture y un test (descripción original) [BAJO, BAJO]

Empezar por el más simple: `MANT1986_2 + idempotence` (`A∪A=A`).
Producir un test temporal de diagnóstico que ejecute:

```java
PolyhedralBoundedSolid a = createFixture(MANT1986_2)[0];
PolyhedralBoundedSolid b = createFixture(MANT1986_2)[0]; // clone idéntico
PolyhedralBoundedSolid result = setOp(a, b, UNION);
// Verificar: result tiene mismas faces/edges/vertices que a
```

Identificar **dónde difiere** el resultado de `a`. ¿Más vértices?
¿Más caras? ¿Faces con orientación distinta? Eso da pista del bug.

#### 7.3.1.B Caracterizar la fuente del drift [MEDIO, BAJO]

Con el caso aislado, agregar trace adicional para responder:
- ¿El intersector duplica vértices coincidentes con sí mismo?
- ¿El classifier marca alguna face como ON cuando debería ser IN?
- ¿El finisher hace un loopGlue que rompe la cara?

Esto reutiliza la infraestructura de trace de §7.1 + tracking de
sonea/soneb/sonfa/sonfb.

#### 7.3.1.C Aplicar fix y verificar [MEDIO/ALTO, MEDIO]

Implementar el fix puntual basado en §7.3.1.B. Verificar:
- El test idempotence pasa para el fixture afectado.
- Los demás drift detectors no empeoran.
- La suite completa sigue verde.
- Especialmente: HOLLOW_BRICK + INTERSECTION sigue produciendo
  topología correcta.

#### 7.3.1.D Repetir para los otros fixtures fallantes [REPETITIVO]

Una vez resuelto MANT1986_2 idempotence, atacar los otros 4 fallos.
Cada uno es un caso pequeño aislado.

#### 7.3.1.E Reactivar AlgebraicPropertiesTest ✅ CERRADO (por eliminación)

**Decisión**: el test legacy `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
fue **eliminado del repositorio**. Razón: sus assertions estaban
diseñadas como drift detectors invertidos
(`assertThat(allHold).isFalse()`), lo que significaba que pasaban
cuando un bug existía y fallaban cuando el bug se arreglaba — el
gradiente contrario al deseable para una suite de regresión.

Cobertura efectiva: el reemplazo `AlgebraicIdentityRegressionTest`
(creado en §7.3.1.A-C) cubre los **10 cases clean** con assertions
positivas:

- 6 idempotence (todos los fixtures × 2 indices)
- 3 diff-swap determinism (los 3 fixtures)
- 1 absorption (MANT1986_2)

Los 2 cases drift remanente (`MANT1988_15_2_LIMIT` y `MANT1988_6_13`
absorption) están documentados como TODO arriba (§7.3.1.D-cont).
No se agregaron como `@Disabled` específico al regression guard
porque su análisis ya está completo en el plan y agregarlos sería
duplicación.

**Resultado**: 3 skipped del legacy desaparecieron (293 tests vs
296), sin pérdida de cobertura. Estructura más limpia: una sola
suite de algebraic identities con semántica correcta.

### 7.4 Outcome real del Nivel 5 ✅

- `AlgebraicPropertiesTest` legacy **eliminado** (drift detector inverso).
  Reemplazado por `AlgebraicIdentityRegressionTest` con 10 tests positivos:
  6 idempotence + 3 diff-swap + 1 absorption (MANT1986_2). Sin regresiones.
- Preflight de identidad geométrica (`areGeometricallyIdentical` +
  `isContainmentOnlyPreflightCase`) integrado en `setOp`.
- Infraestructura de trace en `_PolyhedralBoundedSolidSetGeometricPredicateProcessor`
  disponible para diagnósticos futuros.
- 2 absorption drift cases (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`)
  aceptados como opción 3 (ROI marginal; re-evaluar al cerrar §9 Finish).
- **Suite final: 293 tests, 0 failures, 6 skipped** (todos los skipped con
  razón documentada: 3 invariantes teóricos/non-goals, 2 Kurlander → §8,
  1 helper de mantenimiento).

### 7.5 Lo que NO se hará en §7 (decisión documentada)

- **No se ataca `sectoroverlap`**: el trace probó que no es el problema.
  Mantener la implementación epsilon-tolerante actual.
- **No se persigue looseA==0 estricto**: el setopfinish maneja los
  loose y produce topología correcta. El invariante teórico permanece
  documentado pero no es objetivo.
- **No se reescribe el matching scanjoin**: lo mismo, no es necesario
  para que la suite pase.

Estos puntos pueden retomarse en una **etapa 3** futura si se quiere
elevar el rigor teórico, pero la etapa 2 prioriza outcomes.

---

## 8. Nivel 6 — Cleanup post-núcleo ✅ CERRADO (experimentos completados)

Los tres sub-pasos de §8 fueron experimentos deliberados. Ninguno produjo
una limpieza efectiva; todos resultaron en "mantener el estado actual con
decisión documentada".

### 8.1 §6.2.3 `revert(B)` antes de Connect ✅ DECIDIDO: mantener en Finisher

**Experimento (2026-05-15)**: Se movió `inSolidB.revert()` de
`_PolyhedralBoundedSolidSetFinisher:443` a antes de `setOpConnect(op)` en
`PolyhedralBoundedSolidSetOperator`. Resultado: **29 fallos** (mismo que
con el conector anterior). El conector limpio de §6/§7 no cambia la
dependencia.

**Causa raíz**: La secuencia `lmfkrh(inSolidB, ...) → revert(B) →
movefac → loopGlue` del Finisher require que B esté sin revertir durante
los `lmfkrh` (que crean las nuevas caras espejo con la orientación
original) y revertido durante `movefac/loopGlue` (para que las caras de B
queden con normales complementadas en el resultado SUBTRACT). Mover
`revert` antes de Connect pasa B ya revertido al `lmfkrh`, invirtiendo
las orientaciones que luego usa `loopGlue`.

**Decisión**: `revert(B)` permanece en su posición actual, entre `lmfkrh`
y `movefac`, dentro del Finisher. Esta posición, aunque diferente al
Program 15.1 de Mäntylä, es correcta para la implementación actual.

### 8.2 §6.3 `groupNullEdgesByRing` ✅ DECIDIDO: mantener

**Experimento (2026-05-15)**: Se deshabilitó `groupNullEdgesByRing()` en
`sortNullEdges()`. Resultado: **3 fallos** en
`CsgKurlanderBowlAllMotifsRegressionTest` — precisamente los motifs con
múltiples curvas de intersección (e.g., bowl con inner + outer boundary).

**Conclusión**: El método es necesario para multi-ring cases. La
implementación actual ya usa `partitionNullEdgesIntoRings` (determinista)
+ `sortRingsBySignature` para alineación, lo que satisface el objetivo
del plan de "formalizar como `partitionNullEdgesIntoRings` determinista".
No hay nada adicional que eliminar.

### 8.3 `CsgKurlanderBowlFirstStarRegressionTest` quinto star ✅ DIAGNOSTICADO

**Hallazgo (2026-05-15)**: El test `given_..._then_connectStageClosesAllStarEdges`
(primer star) ya estaba activo y en verde **antes de §8** — no había
`@Disabled` sobre él.

El `@Disabled` existente era sobre `given_kurlanderBowlAndFifthStar_when_...`.
Se eliminó el `@Disabled` y se probó: el quinto star (índice 4) produce
**Face [232] y Face [144] no coplanares** y retorna 0 contornos en lugar
de 2. Causa: el pipeline Finisher produce caras no coplanares para la
geometría del quinto star — un issue de §9 (setopfinish).

**Acción**: `@Disabled` restituido con mensaje diagnóstico completo.
Reactivar tras §9.

**Métricas post-§8**: 293 tests, 0 failures, 6 skipped (sin cambio).

---

## 9. Nivel 7 — `setopfinish` ✅ CERRADO (§9.1–9.5)

**Objetivo**: implementar Program 15.15 sin recoveries ni triangulación
post-hoc, manteniendo la invariante de cara planar por construcción.

### 9.1 Instrumentar `sanitizePairedFaces` — fallback legacy ✅ HECHO

**Resultado medido**: contador `lastLegacyFallbackCount` = **0** en todos
los fixtures de referencia (MANT1986_2 × 3 ops, MANT1988_15_2 UNION,
MANT1988_6_13 SUBTRACT). El emparejamiento por `pairIndex` funciona
correctamente y el fallback de orden-por-índice nunca se activa.

**Implementado**:
- Campo `lastLegacyFallbackCount` + getter `getLastLegacyFallbackCount()`
  en `_PolyhedralBoundedSolidSetFinisher`.
- Fallback incrementa contador y emite `Logger.reportMessage(WARNING)`.
- `SetOpFinishInvariantsTest` §9.1: 5 tests, todos verdes — counter = 0.

**Siguiente**: con el contador confirmado en 0 para todo el baseline,
se puede elevar el fallback a `IllegalStateException` sin riesgo para
los fixtures conocidos. Pendiente: verificar con fixtures Kurlander
antes de eliminar el fallback (§9.4).

### 9.2 Instrumentar `triangulateNonPlanarFaces` ✅ HECHO

**Resultado medido**: contador `lastTriangulatedFaceCount` = **0** en
todos los fixtures de referencia. `loopGlue` no produce caras no planares
en las geometrías baseline.

**Implementado**:
- Campo `lastTriangulatedFaceCount` + getter en `_PolyhedralBoundedSolidSetFinisher`.
- Reset al inicio de `triangulateNonPlanarFaces`; incremento cuando `lmef` triangula.
- `SetOpFinishInvariantsTest` §9.2: 5 tests, todos verdes — counter = 0.

**Siguiente**: la triangulación ya opera como "assertion mode" implícito
(cuenta pero no impide). Con Kurlander reactivado (§9.4), si el contador
sube, buscar la causa en `loopGlue`.

### 9.3 Guarda de planaridad en `maximizeFaces` ✅ HECHO

**Implementado**:
- `wouldMergedFaceBeCoplanar(rightHalf, leftHalf, numericContext)` en
  `PolyhedralBoundedSolidTopologyEditing`: recorre ambos loops, colecta
  posiciones de vértices, llama a `validateFacePointsAreCoplanar`.
- Guard `if (!wouldMergedFaceBeCoplanar(...)) continue;` antes de `lkef`
  en la sección de fusión coplanar de `maximizeFaces`.
- Suite permanece en 303/0/6 — sin regresiones.

### 9.4 Tests de aceptación del Nivel 7 🟡 Diagnóstico completado

**Hallazgos (2026-05-16)**:

Test renombrado `given_kurlanderBowlAndFifthStar_..._resultIsValidAndPairIndexMatchingSucceeds`
con `@Disabled` y nota diagnóstica completa. Resultado del probe:

- **§9.1 counter = 0** ✅ — `sanitizePairedFaces` empareja por `pairIndex` sin fallback.
- **§9.2 counter = 7** — esperado para superficie curva tessellated. `loopGlue`
  fusiona faces adyacentes con normales distintas; `triangulateNonPlanarFaces` resuelve 7.
- **2 faces no resolubles**: face[275] (loopSize=3, triángulo colineal)
  y face[145] (loopSize=1, self-loop: `h.mirrorHalfEdge().parentFace == face`).

**Causa raíz confirmada** (`loopGlue` + `lmekr`):
- `lmekr` recibe un ring de tamaño 1 (`migratedHalfEdges.size()==1`).
- Crea bridge self-loop (v→v). El `lkev` posterior usa `h2.previous()`
  inválido (loop ya destruido). El `lkef` final deja face[145] con self-loop.
- **Referencia**: `lmekr` línea 936-940 tiene comentario "rare condition" que
  documenta este caso sin corregirlo. → **§9.5**.

**Mejoras implementadas** (en producción):
- `findNonDegenerateEar`: tracking de `bestCandidate` con fallback a `epsilon`.
- `triangulateNonPlanarFaces`: guard `loopSize==1 → lkef` (distinto-face funciona;
  self-loop cae en skip seguro con `i++`).

Suite: 303/0/6 (el skip de quinto star se mantiene, renombrado).

### 9.5 Fix `lmekr`/`loopGlue` para ring de tamaño < 3 ✅ CERRADO

**Objetivo**: eliminar faces degeneradas (self-loop, triángulo colineal) que
`loopGlue` produce cuando `lmekr` recibe un ring de tamaño insuficiente, y
resolver cascadas de topología inválida en `triangulateNonPlanarFaces` y `lkef`.

**Tres causas raíz encontradas y corregidas**:

**A) `loopGlue` (`PolyhedralBoundedSolidTopologyEditing.java`)**:
Guard §9.5 antes de `lmekr`: si cualquier loop tiene `halfEdgesList.size() < 3`
se descarta el loop degenado con `removeLoop` + return. Se cubre tanto el caso
`h1` como `h2` de forma simétrica, y el caso en que ambos son degenerados.

**B) `findNonDegenerateEar` (`_PolyhedralBoundedSolidSetFinisher.java`)**:
Reemplazado chequeo de cross-product sin normalizar (`|a||b|sinθ > bigEpsilon`)
por chequeo normalizado (`|cos(θ)| < 1 − unitVectorTolerance`), alineado con
`validateFacePointsAreCoplanar`. Esto previene que `triangulateNonPlanarFaces`
produzca triángulos colineales que fallan planarity después de lmef.

**C) `triangulateNonPlanarFaces` — handler `loopSize ≤ 3`**:
- Para faces size-1 auto-referenciales (h.next()==h, mirror.parentLoop==null):
  se remueven directamente con `remove(i)` en polygonsList + búsqueda y remoción
  del edge huérfano en edgesList. Esto elimina el artefacto `face[145]`.
- Para faces size ≤ 3 con mirror en cara distinta: `lkef` absorbe en cara
  adyacente; `i = 0` para re-examinar caras que absorbieron vértices.

**D) `lkef` (`PolyhedralBoundedSolidEulerOperators.java`) — loop orfanado**:
`maximizeFaces` llama `lkef` sobre faces con múltiples loops (inner rings).
`lkef` sólo migraba la `loopToBeKilled`; los otros loops del killed face
quedaban con `parentFace` inválido → topological integrity falla (count=1).
Ahora, tras la migración principal, los loops restantes del killed face se
reasignan a `he1.parentLoop.parentFace` (surviving face).

**Resultado**: `CsgKurlanderBowlFirstStarRegressionTest` (6 tests, 0 skipped),
incluyendo `given_kurlanderBowlAndFifthStar_..._resultIsValidAndPairIndexMatchingSucceeds`.
Suite: 303/0/5 (sin regresiones).

---

## 10. Nivel 8 — Validación visual y regresión ❌ Parcial

### 10.1 Sweep automatizado ✅ HECHO (baseline formalizado)

`KurlanderBowlMotifSweepRegressionTest` creado con `@Tag("slow")`.
Thresholds conservadores que formalizan el estado actual:

- `MINIMUM_OK_COUNT = 15` (observado: 14 stars + 1 moon)
- `MAXIMUM_FAILURE_COUNT = 25` (observado: empty=16, blackFaces=9)

El sweep se ejecuta con `gradle :base:test --tests "*KurlanderBowl*MotifSweep*"`.
El test de regresión protege las mejoras ya alcanzadas y alertará si
un cambio futuro empeora el score.

**`KurlanderMotifEmptyDiagnosticTest`**: creado como herramienta de
diagnóstico para motif 24 (EMPTY) vs motif 21 (OK). Artefacto temporal
— debe eliminarse en etapa 3 una vez incorporado el fix.

### 10.2 Visual diagnostics ✅ EXISTENTE (ampliado por usuario)

`PolyhedralBoundedSolidExample` mantiene el highlighting y las opciones
de depuración visual. El usuario ha añadido más opciones de depuración
visual controlada durante esta etapa.

Modos disponibles: `--motifSweep`, `--motifIndex N`, highlighting de
vértices numerados, visualización de aristas y caras, overlays CSG.
Baselines en `doc/baselines/kurlander/motif_NN.png` — no creados aún
(traslado a etapa 3).

### 10.3 Diagnóstico EMPTY motifs — completado sin fix ❌

**Síntoma**: 16 de 40 motifs producen resultado vacío (sonfa=0 tras Connect).

**Análisis realizado** (sesiones 2026-05-16):

El pipeline trace (`vsdk.setop.tracePipelineSummary=true`) fue instrumentado
con un dump compacto de los 76 pares de null edges para motif 24 (EMPTY) vs
motif 21 (OK). Los dos motifs tienen estructura idéntica:
`A:sameLoop=64 diffLoop=12 B:sameLoop=12 diffLoop=64`, pero:

- Motif 21 (OK): `connect end sonfa=2 looseA=0`
- Motif 24 (EMPTY): `connect end sonfa=0 looseA=20`

**Causa raíz identificada**: el algoritmo `scanjoin` (Program 15.13 Mäntylä)
requiere que, para un par de null edges `(hea, heb)`, exista un índice `j`
en las listas `(endsa[j], endsb[j])` donde **simultáneamente**:
- `neighbor(hea, endsa[j])` = misma cara A, roles opuestos (rightHalf/leftHalf)
- `neighbor(heb, endsb[j])` = misma cara B, roles opuestos

Las listas `endsa`/`endsb` son **pareadas**: el índice `j` preserva la
correspondencia establecida cuando un par falló anteriormente.

Los pares STRUT_B (A-diffLoop, B-sameLoop) fallan cuando la cara B del
null edge B-sameLoop no aparece en ningún `endsb[j]` previo. Esto ocurre
cuando el par STRUT_B llega ANTES de cualquier par B-diffLoop que comparta
la misma cara B.

**Detalle para motif 24** (par[12]: A-diffLoop f=140/f=139, B-sameLoop f=229):
- Pares[0-11]: B-faces cubren f=215–f=225 únicamente; f=229 jamás aparece
- Par[13] (B-diffLoop f=229/f=230) y par[15] (B-diffLoop f=228/f=229) llegan
  DESPUÉS del par[12] → cuando par[12] intenta scanjoin, f=229 no está en endsb
- Si par[13] precediera a par[12], par[13] fallaría scanjoin y añadiría
  (A:f=140, B:f=229) a endsa/endsb; par[12] matchearía en ese índice

**Clasificación de los 12 pares B-sameLoop de motif 24**:

| Par | B-face | ¿Aparece en B-diffLoop? | Posición | Fixable con reordering |
|-----|--------|------------------------|----------|------------------------|
| 8 | f=224 | sí (par 6, 11, 30, 36) | ANTES que pares difloop | ✅ ya funciona |
| 10 | f=215 | sí (par 9, 19) | ANTES | ✅ ya funciona |
| 12 | f=229 | sí (pares 13, 15) | DESPUÉS ← problema | ✅ fixable |
| 21 | f=234 | sí (pares 20, 33, 53, 57) | ANTES | ✅ ya funciona |
| 34 | f=269 | NO aparece en ningún B-diffLoop | — | ❌ no fixable por reordering |
| 35 | f=263 | sí (pares 32, 40) | DESPUÉS | ✅ fixable |
| 37 | f=229 | sí | DESPUÉS | ✅ fixable |
| 46 | f=292 | NO | — | ❌ no fixable |
| 49 | f=236 | sí (pares 44, 54, 63) | ANTES | ✅ ya funciona |
| 59 | f=209 | sí (pares 58, 61, 70) | ANTES pero A-face f=360 única | ❌ A-face bloqueante |
| 65 | f=373 | NO | — | ❌ no fixable |
| 71 | f=209 | sí | ANTES | ✅ parcialmente (segundo scanjoin) |

**Pares irresolubles** (f=269, f=292, f=373, f=360): son puntos de
intersección "tangencial" donde la curva roza pero no atraviesa la cara B
(o la cara A en el caso f=360). El classifier genera un null edge STRUT
para ese contacto degenerado que nunca puede tener un par complementario.

**Implicación para el fix**:

Un reordering tipo "B-diffLoop antes de B-sameLoop para la misma cara B"
solo resolvería los casos fixables (máximo 5-6 pares del motif 24).
Los pares con caras únicas (f=269, f=292, f=373) requieren una corrección
upstream en el **clasificador** para que no genere null edges para
contactos tangenciales que no crean topología nueva, o bien un manejo
especial en el conector para null edges sin complementario.

La corrección completa (looseA=0 para todos los motifs) requiere trabajo
de etapa 3 en la fase Generate/Classify.

### 10.4 Diagnóstico BLACK_FACES — pendiente

9 motifs clasificados como BLACK_FACES (orientación inconsistente de caras).
No investigado en esta etapa. Root cause probable: el finisher invierte la
orientación de algunas caras de B durante `revert(B)`/`movefac`. Traslado
a etapa 3.

---

## 11. Orden de ejecución, dependencias y riesgo (actualizado)

| Paso | Sección | Estado | Nota |
|------|---------|--------|------|
| 1 | §3 Nivel 1 — preprocesamiento | ✅ Cerrado | — |
| 2 | §4 Nivel 2 — setopgenerate | ✅ Cerrado | — |
| 3 | §5 Nivel 3 — classify estructural | ✅ Cerrado | §5.2 → §7 |
| 4 | §6 Nivel 4 — connect estructural | ✅ Cerrado | §6.3 → §8 |
| 5 | §7 Nivel 5 — bugs algebraicos + diagnóstico sectoroverlap | ✅ Cerrado | 2 absorption drift → etapa 3 |
| 6 | §8 Nivel 6 — cleanup post-núcleo | ✅ Cerrado | todos "keep" documentados |
| 7 | §9 Nivel 7 — setopfinish | ✅ Cerrado (§9.1–9.5) | 303→305 tests |
| 8 | §10 Nivel 8 — validación visual | ❌ Parcial | diagnóstico completo; fix → etapa 3 |

---

## 12. Definition of Done — Estado final etapa 2

La etapa 2 se cierra con la siguiente evaluación de cada criterio:

| # | Criterio | Estado | Detalle |
|---|----------|--------|---------|
| 1 | Sweep ok=40, empty=0, invalid=0, blackFaces=0 | ❌ | ok=15; fix pendiente etapa 3 |
| 2 | `gradle :base:test` sin fallos; `@Disabled` documentados | ✅ | 305/0/6; todos los skips tienen justificación |
| 3 | Properties `flexible*` y `forceARingMove` eliminadas | ✅ | Eliminadas en §6 |
| 4 | Connector < 1000 LOC | ❌ | 1282 LOC; traza diagnóstico añade ~34 LOC extra |
| 5 | Retry `subtractConnectRecovery` eliminado | ✅ | Eliminado en §6.2.1 |
| 6 | JaCoCo connector > 85 % | ⚠ | No medido explícitamente |
| 7 | Documentación actualizada | ✅ | Este cierre documenta estado real |

**La etapa 2 se cierra PARCIALMENTE**: los criterios de infraestructura
(2, 3, 5, 7) están cumplidos; el criterio de resultado del sweep (1)
no. Esto es aceptable dado que el diagnóstico está completo y el fix
requiere trabajo de mayor calado (etapa 3).

---

## 13. Referencias rápidas para etapa 3

Puntos de entrada recomendados para el plan de etapa 3:

**Bloqueador principal — EMPTY motifs (16 casos)**:
- Root cause documentado en §10.3 y §16 de este plan.
- Archivo clave: `_PolyhedralBoundedSolidSetNullEdgesConnector.java`,
  método `setOpConnect()` (~línea 1048) y `scanjoin()` (~línea 886).
- Fix requerido: corrección del ordering de null edges en el conector
  (reordering de pares STRUT_B) o eliminación de null edges tangenciales
  upstream en el clasificador.
- Test de regresión ya creado: `KurlanderBowlMotifSweepRegressionTest`.
- Diagnóstico artefacto: `KurlanderMotifEmptyDiagnosticTest` (eliminar tras fix).

**Bloqueador secundario — BLACK_FACES (9 casos)**:
- Probable causa: `revert(B)` / `movefac` en Finisher invierte orientación.
- Punto de entrada: `_PolyhedralBoundedSolidSetFinisher.java`.

**Cleanup pendiente**:
- `KurlanderMotifEmptyDiagnosticTest` eliminar.
- Traza diagnóstica en `_PolyhedralBoundedSolidSetNullEdgesConnector`
  (bloque `isPipelineSummaryTraceEnabled()` post-sort, ~línea 1064) —
  puede quedar si es útil o eliminarse.
- `BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_...`:
  capturar topología real y reemplazar placeholder.
- 2 absorption drift cases (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`):
  ver §7.3.1.D-cont para análisis completo.

**Herramientas de diagnóstico disponibles**:
- `vsdk.setop.tracePipelineSummary=true` → `[SetOpPipelineTrace]` en stdout.
- `KurlanderMotifEmptyDiagnosticTest` — traza de motifs individuales.
- `PolyhedralBoundedSolidExample` — depuración visual con controles ampliados.

---

## 14. Log de cambios del plan

- **2026-05-13**: Versión inicial del plan stage-2.
- **2026-05-14**: Niveles 1-2 cerrados. Niveles 3-4 en progreso.
- **2026-05-15 (este turno)**: Reestructuración completa.
  - Niveles 1-4 marcados como cerrados con tablas de subhitos y
    cobertura.
  - **Nuevo §7 (Nivel 5)**: núcleo algorítmico que unifica §5.2 y
    §6.1-C con plan de ataque en 4 sub-pasos balanceados (§7.3.1 a
    §7.3.4).
  - §6.2.3, §6.3 movidos a §8 (Nivel 6) — bloqueados por §7.
  - Antes §7 (Finish) y §8 (Visual) renumerados a §9 y §10.
  - §11 (orden de ejecución) y §13 (referencias rápidas)
    actualizados.
  - Métricas finales: 282/0/9, conector −55 % LOC.
- **2026-05-15 (sesión de cierre §7)**:
  - §7.3.1.E: `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
    eliminado del repo (`git rm`). Javadoc de `AlgebraicIdentityRegressionTest`
    actualizado para reflejar eliminación (no "arqueología").
  - §7 (Nivel 5) marcado ✅ CERRADO. Métricas reales: 293/0/6.
  - §8 (Nivel 6) desbloqueado → siguiente bloque activo.
- **2026-05-15 (sesión §8)**:
  - §8.1: experimento revert(B) antes de setOpConnect → 29 fallos, revertido.
    Causa documentada: lmfkrh necesita B sin revertir antes de movefac.
  - §8.2: experimento sin groupNullEdgesByRing → 3 fallos Kurlander multi-ring,
    revertido. El método ya es determinista (usa partitionNullEdgesIntoRings).
  - §8.3: quinto star @Disabled retirado, falla con faces no coplanares [232,144]
    → issue de §9 Finisher; @Disabled restituido con mensaje diagnóstico.
  - §8 (Nivel 6) marcado ✅ CERRADO. Métricas: 293/0/6 (sin cambio).
  - §9 (Nivel 7 — setopfinish) → siguiente bloque activo.
- **2026-05-16 (sesión §9)**:
  - §9.1: contador `lastLegacyFallbackCount` añadido a `_PolyhedralBoundedSolidSetFinisher`.
    Reset al inicio de cada llamada a `sanitizePairedFaces` (misma semántica que
    `lastTriangulatedFaceCount`). Medido = 0 para todos los fixtures baseline. Fallback no se activa post-§7.
  - §9.2: contador `lastTriangulatedFaceCount` añadido. Medido = 0; `loopGlue` no
    produce caras no planares en geometrías baseline.
  - §9.3: `wouldMergedFaceBeCoplanar()` guard añadido en `maximizeFaces` antes de `lkef`.
  - `SetOpFinishInvariantsTest` creado: 10 tests (5 fixtures × §9.1 + §9.2), todos verdes.
  - Suite: 303/0/6 (sin regresiones). §9.4 (Kurlander reactivation) → pendiente.
- **2026-05-16 (sesión §9.5)**:
  - §9.5: quinto star de Kurlander pasa `validateIntermediate`. Cuatro fixes:
    1. `loopGlue`: guard simétrico `isDegenerateLoop (size < 3)` antes de `lmekr`.
    2. `findNonDegenerateEar`: check normalizado (unitVectorTolerance) alineado con planarityValidator.
    3. `triangulateNonPlanarFaces`: prune directo para faces size-1 auto-referenciales;
       `i = 0` restart tras lkef para re-examinar faces absorbidas.
    4. `lkef`: migrar loops extra del killed face a surviving face (previene count=1
       tras maximizeFaces en faces con inner rings).
  - `@Disabled` retirado de `given_kurlanderBowlAndFifthStar_...`.
  - Suite: 303/0/5 (sin regresiones). Nivel 7 (§9) ✅ CERRADO.
- **2026-05-16 (CIERRE ETAPA 2)**:
  - §10: sweep automatizado formalizado en `KurlanderBowlMotifSweepRegressionTest`
    (ok≥15, failures≤25). `KurlanderMotifEmptyDiagnosticTest` creado.
  - §10.3: diagnóstico completo de EMPTY motifs. Root cause: ordering problem en
    `scanjoin` — pares STRUT_B llegan antes de que sus B-faces aparezcan en `endsb`.
    Fix requiere corrección upstream (clasificador o pre-sort topology-aware) → etapa 3.
  - §10.4: BLACK_FACES (9 motifs) — sin investigar → etapa 3.
  - Suite final: 305/0/6. Sweep: ok=15, empty=16, blackFaces=9.
  - Plan cerrado parcialmente: criterios de infraestructura ✅; criterio de sweep ❌.
  - Usuario añadió más opciones de depuración visual en `PolyhedralBoundedSolidExample`.

## 15. Estado de las pruebas

Para los motifs seleccionados individualmente dentro del Bowl Kurlander:

| Índice motif | Estado |
|------|---------|
| 0 | ✅ |
| 1 | ✅ |
| 2 | ❌ Fallo en contorno interno |
| 3 | ✅ |
| 4 | ❌ Fallo en ambos contornos |
| 5 | ❌ Fallo en ambos contornos |
| 6 | ❌ Fallo en contorno interno |
| 7 | ⚠️ B-A pierde un shell |
| 8 | ❌ Fallo en contorno externo |
| 9 | ❌ Objeto A eliminado |
| 10 | ✅ |
| 11 | ❌ Fallo en contorno externo |
| 12 | ✅ |
| 13 | ❌ Fallo en contorno externo |
| 14 | ✅ |
| 15 | ✅ |
| 16 | ❌ Fallo en contorno externo |
| 17 | ❌ Fallo en contorno externo |
| 18 | ❌ Fallo en contorno externo |
| 19 | ❌ Objeto A eliminado |
| 20 | ❌ Fallo en contorno externo |
| 21 | ✅ |
| 22 | ❌ Cara resultante no planar |
| 23 | ❌ Cara resultante no planar y cara vecina eliminada |
| 24 | ❌ Objeto A eliminado |
| 25 | ❌ Objeto A eliminado |
| 26 | ❌ Fallo en ambos contornos |
| 27 | ❌ Objeto A eliminado |
| 28 | ❌ Objeto A eliminado |
| 29 | ❌ Objeto A eliminado |
| 30 | ❌ Objeto A eliminado |
| 31 | ❌ Objeto A eliminado |
| 32 | ❌ Objeto A eliminado |
| 33 | ❌ Objeto A eliminado |
| 34 | ❌ Objeto A eliminado |
| 35 | ❌ Objeto A eliminado |
| 36 | ❌ Objeto A eliminado |
| 37 | ❌ Fallo en contorno externo |
| 38 | ❌ Objeto A eliminado |
| 39 | ❌ Objeto A eliminado |

---

## 16. Root Cause técnico — EMPTY motifs (referencia para etapa 3)

Esta sección preserva el análisis técnico detallado obtenido en §10.3
para que la etapa 3 pueda retomarlo sin repetir el diagnóstico.

### 16.1 Estructura de los 76 pares de null edges

Para la operación bowl SUBTRACT motif (moon), el clasificador genera
76 pares de null edges con estructura:
- A:sameLoop=64, A:diffLoop=12 → 12 pares STRUT_A (flipNullEdgeOrientationForOpenSide)
- B:sameLoop=12, B:diffLoop=64 → 12 pares STRUT_B (separateEdgeSequence con
  B-sameLoop cuando hb1==hb2 en el clasificador V/V)

`partitionNullEdgesIntoRings` produce **76 anillos de tamaño 1** (cada
null edge STRUT forma un anillo aislado por tener ambos vértices en el
mismo punto geométrico). `groupNullEdgesByRing` es no-op → el orden en
sonea/soneb es el orden de inserción del clasificador.

### 16.2 Condición de éxito de scanjoin para pares STRUT_B

Para que `scanjoin(rightHalf_A, leftHalf_B)` tenga éxito para un par
STRUT_B (A-diffLoop f_A1/f_A2, B-sameLoop f_B), se requiere que en la
lista pareada `(endsa[j], endsb[j])` exista un índice `j` donde:

```
endsa[j].parentLoop.parentFace == f_A1 o f_A2   (misma cara A)
endsa[j] == endsa[j].parentEdge.leftHalf         (rol opuesto a rightHalf_A)
endsb[j].parentLoop.parentFace == f_B             (misma cara B)
endsb[j] == endsb[j].parentEdge.rightHalf         (rol opuesto a leftHalf_B)
```

Este `j` solo existe si un par anterior falló scanjoin y añadió
`(endsa[j]=A-half-en-f_A, endsb[j]=B-half-en-f_B)` al mismo índice.

El par predecesor necesario es de tipo STRUT_A: A-sameLoop f_A, B-diffLoop
f_B/fX. Si ese par falla su primer scanjoin, añade `(rightHalf_A(f_A),
leftHalf_B(f_B o fX))` — si `leftHalf_B` está en f_B, la condición se cumple.

### 16.3 Pares problemáticos de motif 24 y sus predecesores faltantes

| Par STRUT_B | B-face | Par predecesor necesario | Posición | Tipo de problema |
|-------------|--------|--------------------------|----------|-----------------|
| 12 | f=229 | par 13 (B-DL f=229/230) | DESPUÉS ← | Reordering fix |
| 35 | f=263 | par 32 (B-DL f=258/263) | DESPUÉS | Reordering fix |
| 37 | f=229 | par 13/15 ya existentes | DESPUÉS | Reordering fix |
| 34 | f=269 | ninguno | N/A | Null edge tangencial — fix en clasificador |
| 46 | f=292 | ninguno | N/A | Null edge tangencial — fix en clasificador |
| 65 | f=373 | ninguno | N/A | Null edge tangencial — fix en clasificador |
| 59 | A-face=f=360 | ninguno (A-face única) | N/A | A-diffLoop tangencial — fix en clasificador |

### 16.4 Opciones de fix para etapa 3

**Opción A — Reordering en `setOpConnect`**: antes del bucle principal,
reordenar sonea/soneb de forma que cada par STRUT_B (B-sameLoop) tenga
al menos un par B-diffLoop con la misma B-face precediendo en el índice.
- Implementable como: topological sort por face-adjacency graph.
- Cubre casos fixables (tabla 10.3), no los tangenciales.
- Riesgo: podría alterar el pairing A-B necesario para otros casos.

**Opción B — Eliminación de null edges tangenciales en `setOpClassify`**:
en `vertexVertexInsertNullEdges`, detectar cuando el par STRUT crea un
null edge que no tiene complementario (la curva solo roza la cara).
No insertar el null edge o marcarlo para eliminación pre-Connect.
- Más limpio semánticamente (no generar topología innecesaria).
- Requiere análisis del contexto topológico en el clasificador.

**Opción C — Manejo especial de null edges sin complementario en Connect**:
en `setOpConnect`, si scanjoin falla completamente (both null), verificar
si la cara B nunca aparecerá en endsb (pre-scan). Si es así, eliminar
ese null edge con `lkef` directamente.
- Pragmático, sin modificar el clasificador.
- Riesgo: `lkef` sobre un null edge ya integrado en el B-rep puede
  dejar topología inconsistente si otros null edges lo referenciaban.

**Recomendación**: Opción A + B en combinación. A para los casos reordenables,
B para los tangenciales. Medir impacto con `KurlanderBowlMotifSweepRegressionTest`.

### 16.5 Conexión con BLACK_FACES

Los 9 BLACK_FACES tienen caras con orientación inconsistente en el resultado.
La causa probable está en `_PolyhedralBoundedSolidSetFinisher.java`:
`revert(B)` invierte todas las normales de B, luego `movefac` mueve esas
caras al resultado. Si alguna cara queda con orientación invertida en el
join, `validateConsistentFaceOrientations` la detecta como BLACK_FACE.
Punto de entrada: comparar orientaciones antes/después de `movefac` para
los motifs problemáticos.
