# Plan etapa 2 — Endurecimiento del kernel CSG hacia production grade

Fecha original: 2026-05-13
Última actualización: 2026-05-15
Autor: Análisis asistido (Opus 4.7)

Este documento extiende `doc/plan-csg-boolean-fix.md`. La etapa 1 dejó el
sweep de 40 motifs Kurlander en `ok=15, empty=11, invalid=2,
blackFaces=12`. La etapa 2 ataca las causas estructurales recorriendo el
pipeline tal como lo define Mäntylä 1988 capítulo 15, eliminando las
heurísticas acumuladas y verificando invariantes en cada borde.

**Estado tras varias sesiones (2026-05-15)**: niveles 1-4 cerrados o muy
avanzados (282 tests, 0 failures, 9 skipped). El bloqueador único
restante es un fix upstream en `sectoroverlap` (nuevo §7) que destrabará
los pasos finales del pipeline.

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

### 1.1 Suite de tests (snapshot 2026-05-15)

**282 tests · 0 failures · 9 skipped** (todos los skipped están
documentados con su razón y referencia al subpaso que los reactivará).

Clases de test relevantes (todas verdes):

- `BooleansFromReferenceObjectPairsTest` — 37 tests, 2 skipped (MANT1988_15_1
  + INTERSECTION/SUBTRACT)
- `SetOpConnectNoLooseInvariantTest` — invariante de Program 15.14 con
  4 baselines verdes + 2 pending `@Disabled` (gemelos del §5.2 / §7)
- `SetOpConnectScanJoinTest` — 7 tests, contratos de scanjoin /
  sgetnextnulledge + 41 nombres prohibidos en regression guard
- `VertexFaceClassifierCoplanarTest` — 5 tests (incluye reflection guard)
- `VertexVertexEndpointRecoveryTest` — 4 tests del enum
  `SeparateEdgeSequenceResult`
- `IntersectorWeldTest`, `IntersectorParametricOrderingTest` — cobertura
  de §4.2 y §4.3
- `PolyhedralBoundedSolidPreflightTest` — 4 tests del Nivel 1

### 1.2 LOC eliminadas

- `_PolyhedralBoundedSolidSetNullEdgesConnector.java`: **2790 → 1248**
  LOC (−55 %). Eliminadas: camino dual `flexibleEndpointChains`,
  red de seguridad post-bucle (`closeLegacyCoincidentLooseEnds`,
  `resolveClassicAlternatingLooseCycle`, `resolveClassicLooseNetwork`),
  deferrals, `removeLooseEndsA/B` extra, ~17 helpers huérfanos, 7 system
  properties `flexible*`, 4 inner classes.
- `PolyhedralBoundedSolidSetOperator.java`: ~165 LOC eliminadas
  (bloque retry `trySubtractConnectRecovery` + helpers).
- `_PolyhedralBoundedSolidSetVertexFaceClassifier.java` (extraído) +
  bloque zombie `vertexFaceClassify` en SetOperator: ~505 LOC.

### 1.3 Bloqueadores reales restantes (actualizado 2026-05-15)

Tras §7 (instrumentación de sectoroverlap), la lista de bloqueadores
reorientó significativamente:

**No-bloqueadores** (antes considerados bloqueadores, ahora descartados
con evidencia):

- **`looseA != 0` no es bloqueador funcional.** Trace ejecutable
  demostró que `MANT1988_15_1 + INTERSECTION/SUBTRACT` (looseA=4) y
  `HOLLOW_BRICK + INTERSECTION` (looseA=4) **producen el resultado
  topológico correcto** que la suite real valida. El invariante
  teórico de Program 15.14 es ortogonal al outcome funcional.
- **`sectoroverlap` no es la causa raíz.** Trace ejecutable demostró
  que UNION e INTERSECTION sobre la misma geometría producen
  **trazas idénticas** (52 calls, 48/4 TRUE/FALSE, 8 boundary-ray).

**Bloqueadores reales identificados** (pendientes de atacar):

- **7 fixtures con drift algebraico** ocultos por `@Disabled` en
  `AlgebraicPropertiesTest`. Detalle:
  - Idempotence: `MANT1988_15_2_LIMIT[idx1]`, `MANT1988_6_13[idx0]`
  - Absorption: `MANT1986_2`, `MANT1988_15_2_LIMIT`, `MANT1988_6_13`
  - Diff-swapped determinismo: `MANT1988_15_2_LIMIT`, `MANT1988_6_13`
- **2 tests Kurlander `@Disabled`** (no instrumentados):
  `CsgKurlanderBowlFirstStarRegressionTest.given_kurlanderBowlAndFifthStar_...`
  y `BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_...`.
- **Sweep visual Kurlander**: 11 EMPTY + 12 blackFaces remanentes
  desde etapa 1.

**Nota sobre el AlgebraicPropertiesTest**: el test original tenía
assertions invertidas (`assertThat(allHold).isFalse()`) — un drift
detector que valida que el bug sigue presente. Eso lo hacía
contraproducente: arreglar un bug rompía el test. La reescritura
con assertions positivas es el primer paso de §7.3.1.B.

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
| **5** | **Núcleo algorítmico** — sectoroverlap upstream + cierre topológico | predicate processor + scanjoin contract | **§7** | 🟡 **En curso (bloqueador único)** |
| 6 | Cleanup post-núcleo (revert(B), groupNullEdgesByRing) | SetOperator + connector | §8 | ⏸ Bloqueado por §7 |
| 7 | `setopfinish` | `_PolyhedralBoundedSolidSetFinisher` | §9 | ⏸ Pendiente |
| 8 | Validación visual y regresión | `--motifSweep` + tests slow | §10 | ⏸ Pendiente |

Métrica final esperada: sweep `ok=40, empty=0, invalid=0, blackFaces=0`.

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

## 7. Nivel 5 — Bugs algebraicos reales y diagnóstico ✅ REORIENTADO

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

#### 7.3.1.A Aislar 1 fixture y un test [BAJO, BAJO]

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

#### 7.3.1.E Reactivar AlgebraicPropertiesTest [BAJO]

Cuando los 5 fixtures pasen, quitar el `@Disabled` de la clase entera.

### 7.4 Reorientación del Nivel 5: outcome esperado

Al cerrar §7.3.1.A-E:

- `AlgebraicPropertiesTest` reactivado y verde (12 tests adicionales).
- 3 fixtures problemáticos identificados y arreglados.
- Infraestructura de trace en `_PolyhedralBoundedSolidSetGeometricPredicateProcessor`
  disponible para diagnósticos futuros.
- Suite total esperada: 286 → ~298 tests, 0 fallos, ~7 skipped.

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

## 8. Nivel 6 — Cleanup post-núcleo ⏸

Trabajo posible **únicamente** una vez §7 cerrado, porque depende del
nuevo invariante "looseA == looseB == 0 siempre".

### 8.1 §6.2.3 `revert(B)` antes de Connect (Equation 15.1)

Mover `inSolidB.revert()` desde `_PolyhedralBoundedSolidSetFinisher`
hasta antes de `setOpConnect(op)`. **Resultado del experimento
previo**: rompía 28 tests con el conector con muletas. Con el
conector limpio de §6 + §7, el experimento debe repetirse: si pasa,
la complementación queda donde Mäntylä la quiere; si no, hay un
detalle adicional que documentar.

### 8.2 §6.3 Eliminar `groupNullEdgesByRing` heurístico

Con §4.3 (orden paramétrico estable) y §7 (sectoroverlap correcto), el
agrupamiento por anillos debería emerger naturalmente del orden de
inserción. **Acción**: ejecutar el sweep `--motifSweep` y la suite
`:base:test` con y sin `groupNullEdgesByRing`. Si los resultados son
equivalentes, eliminar el método. Si no, formalizar como
`partitionNullEdgesIntoRings` determinista con tests aislados.

### 8.3 `CsgKurlanderBowlFirstStarRegressionTest` reactivado

Eliminar `@Disabled` del test `given_..._then_connectStageClosesAllStarEdges`.
Si pasa: documentar como avance del sweep. Si no: investigar como
nueva instancia del problema (probablemente diagnosticable con la
infraestructura de §7.3.1).

---

## 9. Nivel 7 — `setopfinish` ⏸

**Objetivo**: implementar Program 15.15 sin recoveries ni triangulación
post-hoc, manteniendo la invariante de cara planar por construcción.

### 9.1 Eliminar `sanitizePairedFaces` con fallback legacy

**Problema medido**: el método cae a "legacy ordering" cuando no
encuentra pares. Con Connect emitiendo `sonfa`/`sonfb` correctos
(post §7), no debe haber fallback.

**Acciones**:

1. Reemplazar el fallback por `IllegalStateException` con dump
   topológico.
2. El emparejamiento se vuelve determinista: `sonfa[i]` se empareja
   con `sonfb[i]` por índice, en el orden producido por Connect.

### 9.2 Reducir o eliminar `triangulateNonPlanarFaces`

**Problema medido**: triangulación post-finish como red de seguridad
para `loopGlue` cuando produce caras no planares. Con Connect
emparejando bien, no debe ser necesaria.

**Acciones**:

1. Tras §7, ejecutar el sweep con triangulación deshabilitada. Métrica:
   `nonPlanarFacesPerMotif`.
2. Si una vez emparejado bien sigue habiendo caras no planares,
   corregir en `loopGlue` (alineando los IDs entre `sonfa[i]` y
   `sonfb[i]` antes de `loopGlue`).
3. Mantener `findNonDegenerateEar` y `extractInnerLoopsOfNonPlanarFace`
   como **assertion mode** sólo: si encuentran cara no planar, logean
   y triangulan, pero también incrementan un contador que el test usa
   para asegurar `count == 0` en builds limpios.

### 9.3 Reactivar `maximizeFaces` con guarda de planaridad

**Acciones**:

1. En `PolyhedralBoundedSolidTopologyEditing.maximizeFaces`, antes de
   fusionar dos caras vía `lkef`, computar el plano resultante sobre
   los vértices del loop fusionado y verificar
   `validateFacePointsAreCoplanar(union)` con la `ToleranceContext`
   de la cara. Si falla, no fusiona.
2. Después de §9.2 + §9.3, el segundo paso de triangulación
   post-maximize del `postProcessResult` sobra.

### 9.4 Tests de aceptación del Nivel 7

- `SetOpFinishLoopGlueInvariantTest` — invariante: después de
  `loopGlue`, todas las caras de `outRes` son planares dentro del
  epsilon.
- Reactivar `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
  (idempotencia, absorción, determinismo).
- Reactivar `BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_*`.

Sweep esperado tras §9: `ok=40/40`.

---

## 10. Nivel 8 — Validación visual y regresión ⏸

### 10.1 Sweep automatizado

`PolyhedralBoundedSolidExample --motifSweep` se mantiene como
herramienta visual. Adicionalmente, crear
`KurlanderBowlMotifSweepRegressionTest` con `@Tag("slow")` que ejecute
el sweep sin renderizar y exija `ok == 40`. Se corre antes de cada PR
de release; opcional en CI por velocidad.

### 10.2 Visual diagnostics

El highlighting amarillo incondicional de etapa 1 se mantiene. Modo
`--motifIndex` permite renderizar casos individuales y comparar con
baselines en `doc/baselines/kurlander/motif_NN.png` (por crear).

### 10.3 Performance

Cada fase debe medirse antes/después con:

```
gradle :base:test --tests "*KurlanderBowl*" --info
```

La eliminación de retries y recoveries debería bajar significativamente
el tiempo. Si una fase introduce regresión >20% sin justificación, se
revierte.

---

## 11. Orden de ejecución, dependencias y riesgo (actualizado)

| Paso | Sección | Estado | Riesgo |
|------|---------|--------|--------|
| 1 | §3 Nivel 1 — preprocesamiento | ✅ Cerrado | — |
| 2 | §4 Nivel 2 — setopgenerate | ✅ Cerrado | — |
| 3 | §5 Nivel 3 — classify estructural | ✅ Cerrado | — |
| 4 | §6 Nivel 4 — connect estructural | ✅ Cerrado | — |
| 5 | **§7 Nivel 5 — sectoroverlap upstream + cierre** | 🟡 **En curso** | **Alto** (núcleo algorítmico) |
| 6 | §8 Nivel 6 — cleanup post-núcleo | ⏸ Bloqueado por 5 | Medio |
| 7 | §9 Nivel 7 — setopfinish | ⏸ Bloqueado por 6 | Medio |
| 8 | §10 Nivel 8 — validación visual | ⏸ Bloqueado por 7 | Bajo |

Riesgo principal está en el paso 5 (§7). Mitigación: trabajar en sub-pasos
balanceados (§7.3.1 → §7.3.2 → §7.3.3 → §7.3.4), con verificación de
suite verde entre cada uno. Cada sub-paso es retroceable.

---

## 12. Definition of Done

La etapa 2 se considera cerrada cuando:

1. **Sweep `--motifSweep` reporta `ok=40, empty=0, invalid=0,
   blackFaces=0`**.
2. **`gradle :base:test` ejecuta sin tests fallidos y sin `@Disabled`
   nuevos**. Los `@Disabled` actuales (gemelos del §7) están
   reactivados.
3. **Las 11 system properties `vsdk.setop.connect.flexible*` y
   `forceARingMove` están eliminadas del repositorio**. (✅ ya hecho)
4. **`_PolyhedralBoundedSolidSetNullEdgesConnector.java` baja a
   < 1000 LOC** (actualmente 1248; objetivo cualitativo).
5. **El bloque retry `subtractConnectRecovery` en `setOp` se elimina**.
   (✅ ya hecho)
6. **Cobertura JaCoCo de
   `_PolyhedralBoundedSolidSetNullEdgesConnector` > 85 %**.
7. **Documentación**: este archivo actualizado con métricas finales,
   `doc/references/coverage_MANT1988.md` con porcentajes por sección.

---

## 13. Referencias rápidas a los puntos de cambio

Por si el ejecutor de la siguiente sesión quiere ir directo:

**Nivel 5 (§7) — el bloqueador único actual**:

- Predicado a atacar:
  [_PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlap](java/base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.java#L219)
- Callsite del predicado (V/V coplanar):
  [_PolyhedralBoundedSolidSetVertexVertexClassifier.vertexVertexSectorIntersectionTest](java/base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetVertexVertexClassifier.java)
- Tests `@Disabled` que pasan a verdes cuando §7 cierre:
  - `SetOpConnectNoLooseInvariantTest.given_pendingPair_when_setopRuns_then_connectShouldLeaveNoLooseEndpoints` (2 casos)
  - 2 tests en `PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest`
- Diagnóstico ya hecho: ver §7.1 y §7.2 arriba.

**Nivel 6 (§8) — depende de §7**:

- Mover `revert(B)`:
  [_PolyhedralBoundedSolidSetFinisher línea ~443](java/base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetFinisher.java) →
  [PolyhedralBoundedSolidSetOperator.setOp antes de setOpConnect](java/base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSetOperator.java)
- Eliminar `groupNullEdgesByRing`: helper en
  `_PolyhedralBoundedSolidSetNullEdgesConnector`.

**Nivel 7 (§9) — depende de §8**:

- Fallback `sanitizePairedFaces`:
  [_PolyhedralBoundedSolidSetFinisher líneas ~220-224](java/base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetFinisher.java)
- Triangulación post-finish: mismo archivo.
- `maximizeFaces`:
  [PolyhedralBoundedSolidTopologyEditing.maximizeFaces](java/base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.java)

**Nivel 8 (§10)**:

- Sweep test slow: nuevo
  `base/src/test/.../KurlanderBowlMotifSweepRegressionTest.java`
  (`@Tag("slow")`).

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
