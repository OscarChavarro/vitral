# Plan etapa 2 — Endurecimiento del kernel CSG hacia production grade

Fecha: 2026-05-13
Autor: Análisis asistido (Opus 4.7)

Este documento extiende `doc/plan-csg-boolean-fix.md`. La etapa 1 dejó el
sweep de 40 motifs Kurlander en `ok=15, empty=11, invalid=2,
blackFaces=12` (sesión 2026-05-12) gracias a la triangulación post-finish
y al agrupamiento por anillos topológicos. La etapa 2 ataca las causas
estructurales remanentes recorriendo el pipeline tal como lo define
Mäntylä 1988 capítulo 15, fase por fase, eliminando las heurísticas
acumuladas y verificando invariantes en cada borde.

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

### 1.1 Capas y archivos clave

Topología y geometría base:

- [PolyhedralBoundedSolid.java](base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.java)
- [PolyhedralBoundedSolidEulerOperators.java](base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.java)
- [PolyhedralBoundedSolidTopologyEditing.java](base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.java)
- [PolyhedralBoundedSolidGeometricValidator.java](base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.java)
- [PolyhedralBoundedSolidValidationEngine.java](base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.java)
- [PolyhedralBoundedSolidNumericPolicy.java](base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.java)
- nodes: `_PolyhedralBoundedSolidFace/Loop/HalfEdge/Edge/Vertex`
- Estrategias geometricas: `_GeometricPlanarityStrategy`,
  `_GeometricStrictFaceIntersectionsStrategy`,
  `_GeometricStrictLoopsStrategy`,
  `_TopologicalIntegrityStrategy`,
  `_GeometricFaceOrientationStrategy` (no encadenada en intermediate).

Pipeline de booleanas:

- [PolyhedralBoundedSolidSetOperator.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSetOperator.java)
  — orquestador `setOp(...)`. 4232 LOC.
- [_PolyhedralBoundedSolidSetIntersector.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetIntersector.java)
  — `setOpGenerate` (Program 15.2). 518 LOC.
- [_PolyhedralBoundedSolidSetGeometricPredicateProcessor.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.java)
  — `contfv`, `contfp`, `sctrwithin`, `sectoroverlap`. 866 LOC.
- [_PolyhedralBoundedSolidSetClassifier.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetClassifier.java)
  — orquestador de classify (Program 15.5).
- [_PolyhedralBoundedSolidSetVertexFaceClassifier.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetVertexFaceClassifier.java)
  — Program 15.5 V/F. 751 LOC.
- [_PolyhedralBoundedSolidSetVertexVertexClassifier.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetVertexVertexClassifier.java)
  — Program 15.6 V/V. 665 LOC.
- `_PolyhedralBoundedSolidSetOperatorSectorClassificationOn{Vertex,Face,Sector}.java`
  — Programs 15.7-15.10.
- [_PolyhedralBoundedSolidSetNullEdgesConnector.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetNullEdgesConnector.java)
  — `setOpConnect` (Programs 15.13-15.14). **2790 LOC**, hotspot
  principal.
- [_PolyhedralBoundedSolidSetFinisher.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetFinisher.java)
  — `setOpFinish` (Program 15.15). 464 LOC.
- [_PolyhedralBoundedSolidSetNonIntersectingClassifier.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSetNonIntersectingClassifier.java)
  — disjoint/touching/containment. 1053 LOC.
- [CsgKurlanderBowlFixture.java](base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/CsgKurlanderBowlFixture.java)
  — fixture star+moon (la luna se construye con boolean previa
  cilindro − cilindro).

Tests directos:

- Falla persistente: `CsgKurlanderBowlFirstStarRegressionTest`
  - `given_kurlanderBowlAndFifthStar_..._twoDoubleBoundaryContoursAreClosed`
  - `given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_connectStageClosesAllStarEdges`
- Sweep `ok=15/40` después de la etapa 1.
- Disabled: `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`,
  `BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_*`,
  dos casos en `PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest`.

### 1.2 Inventario de heurísticas y deuda técnica

Heurísticas en el conector (núcleo de la deuda):

1. `vsdk.setop.connect.forceARingMove`
2. `vsdk.setop.connect.allowCrossLooseMatch`
3. `vsdk.setop.connect.keepInsertionOrder` (default `true`)
4. `vsdk.setop.connect.flexibleEndpointChains`
5. `vsdk.setop.connect.flexibleSkipCuts`
6. `vsdk.setop.connect.flexibleAllowSamePointSelfClosure`
7. `vsdk.setop.connect.flexibleSkipLegacyPairFinalCuts`
8. `vsdk.setop.connect.flexibleKeepOnlyPairedCutFaces`
9. `vsdk.setop.connect.flexibleDisableBRingMoveForSubtract`
10. `vsdk.setop.connect.flexibleAllowCrossChainMerge`
11. `vsdk.setop.connect.flexibleRejectOneSidedMatches`

Mecanismos de recovery que ocultan fallos:

- `PolyhedralBoundedSolidSetOperator.setOp()` líneas 3879-3911:
  retry con `forceARingMove=true`.
- `_PolyhedralBoundedSolidSetFinisher.sanitizePairedFaces()` líneas
  220-224: fallback a "legacy ordering".
- `_PolyhedralBoundedSolidSetVertexVertexClassifier.recoverMissingCoplanarEndpoints()`:
  16-iter guard sobre ha1/ha2/hb1/hb2.
- `_PolyhedralBoundedSolidSetNullEdgesConnector.closeLegacyCoincidentLooseEnds()`:
  bucle sin prueba de terminación.
- `_PolyhedralBoundedSolidSetFinisher.triangulateNonPlanarFaces()`:
  fan triangulation con cota `50*(initialCount+1)` (etapa 1).

### 1.3 Falla raíz observada en MOON 23 (representativa de los 11 EMPTY)

Trace `tracePipelineSummary=true`:

```
[connect post-pass] sonfa=1 sonfb=1 looseA=12 looseB=12
[finish sanitize skip A face=218 ringSize=10 usable=false connected=false]
[finish end] outRes faces=0 edges=1 vertices=2
```

Los 12 half-edges loose vienen en pares (loose[5]/[6] en
`<-0.66,-0.66,1.05>`, loose[9]/[10] en `<-0.64,-0.64,0.80>`). El conector
los rechaza por estar en caras distintas. El "ring de integración"
queda con 10 vértices en lugar de 40, el finisher salta la cara y
`outRes` queda vacío.

### 1.4 Diagnóstico estructural

La falla tiene tres orígenes simultáneos que se refuerzan:

- **Datos de entrada con vértices casi-coincidentes** sin _weld_ previo,
  que multiplican los "loose" en Intersect/Connect.
- **Connect basado en heurísticas de geometría local** en lugar de un
  recorrido topológico ordenado por la curva de intersección
  ([MANT1988] §15.7 supone null-edges ordenados a lo largo de la curva).
- **Finish que oculta connect roto** vía recoveries, dejando casos
  legítimos (anillo de 40 vértices) sin distinguirse de casos rotos
  (anillo de 10).

---

## 2. Estrategia global

La etapa 2 sigue el ordenamiento natural del pipeline para que cada
fase reciba datos garantizados por la fase anterior. Cada fase se
trabaja en cuatro pasos: **medir → corregir → validar → instrumentar
test de regresión**.

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
   pero alineado palabra-por-palabra con Mäntylä antes que mantener
   una variante exótica.

### 2.1 Ejecución por nivel (objetivo del usuario)

| Nivel | Fase Mäntylä | Archivos primarios | Sección de este documento |
|------:|--------------|--------------------|---------------------------|
| 1 | Modelado y preprocesamiento (faceeq, getmaxnames, updmaxnames, esfera/cono/cilindro válidos) | `PolyhedralBoundedSolidGeometricValidator`, `Sphere`, `Cylinder`, `Cone`, fixture loaders | §3 |
| 2 | `setopgenerate` — reducción del problema | `_PolyhedralBoundedSolidSetIntersector` | §4 |
| 3 | `setopclassify` — clasificación de frontera | `_PolyhedralBoundedSolidSetClassifier` y V/F, V/V | §5 |
| 4 | `setopconnect` — conexión de aristas | `_PolyhedralBoundedSolidSetNullEdgesConnector` | §6 |
| 5 | `setopfinish` — generación del resultado | `_PolyhedralBoundedSolidSetFinisher`, `postProcessResult` | §7 |

Métrica final esperada: sweep `ok=40, empty=0, invalid=0, blackFaces=0`,
todos los tests deshabilitados en §1.1 reactivados sin regresiones.

---

## 3. Nivel 1 — Modelado y preprocesamiento

Objetivo: garantizar que la entrada a la fase Intersect cumple las
invariantes de [MANT1988] §10.2 y §13.1 — caras planas dentro del
epsilon del modelo, sin vértices casi-coincidentes residuales, ID
globalmente únicos.

**Estado de ejecución (sesión 2026-05-13):**

| Sub-tarea | Estado | Resultado |
|-----------|--------|-----------|
| §3.1 weldCoincidentVertices | ✅ | `CsgKurlanderBowlAllMotifsRegressionTest.given_kurlanderBowlAndThirdMoon` pasa |
| §3.1 validateNoCoincidentVertices | ✅ | O(n²) scan en `PolyhedralBoundedSolidGeometricValidator` |
| §3.1 validateUniqueFaceAndVertexIds | ✅ | Detecta IDs > max y duplicados en faces/vertices |
| §3.1 validateBooleanInputs | ✅ | IllegalArgumentException si persiste inválido post-weld |
| §3.1 wired into setOp | ✅ | Reemplaza los dos `validateIntermediate` ad-hoc previos |
| §3.2 Newell normal con centroide | ✅ | Fallback a corner para loops wire (strut-topology) |
| §3.3 IdNamespace | ✅ | `_PolyhedralBoundedSolidIdNamespace`; usado en SetOperator, Intersector, Operator.join, Finisher |
| §3.4 Snap en addArcToExistingFace | ✅ | Snap 1e-10 en coordenadas trig del generador circular (afecta cilindro, cono, bowl) |
| §3.4 JavaDoc Sphere planarity | ✅ | Garantía analítica documentada en `buildPolyhedralBoundedSolid` |
| §3.4 Moon directo sin boolean | ⬜ Diferido | Invasivo; weld ya mitiga el problema |
| §3.5 PolyhedralBoundedSolidPreflightTest | ✅ | 4 tests pasando (bowl+star, shell+moon, sphere16x8, cylinder sin coincidentes) |

**Resultado neto sobre `:base:test` (sesión 2026-05-13):**
- Inicio sesión: 4 fallos — ThirdMoon, FirstMoon-EulerWire×2, twoDoubleBoundaryContoursAreClosed
- Fin sesión: **1 fallo** (pre-existente `twoDoubleBoundaryContoursAreClosed`)
- Tests añadidos: 4 preflight tests nuevos (214 total)
- Regresiones introducidas: **0**

### 3.1 Validador previo a `setOp`

**Problema medido**: los moons (cilindro − cilindro) llegan a `setOp`
con caras casi planares y a veces con vértices coincidentes producto de
la primera boolean. El operando A (bowl) puede tener faces con
desviaciones cercanas a `bigEpsilon`.

**Acciones**:

1. ✅ Implementar `validateBooleanInputs(a, b, msg)` como predicado
   público en `PolyhedralBoundedSolidValidationEngine`. Corre:
   - `validateIntermediate` (planaridad + integridad topológica).
   - `validateNoCoincidentVertices` (nuevo, scan O(n²) sobre
     `verticesList` con `ToleranceContext.bigEpsilon()`).
   - Si hay vértices coincidentes: intenta `weldCoincidentVertices`
     (Euler `lkev`) y re-valida.
   - ✅ `validateUniqueFaceAndVertexIds` — detecta IDs > max y duplicados
     en `polygonsList` y `verticesList`.
2. ✅ `PolyhedralBoundedSolidSetOperator.setOp` invoca
   `validateBooleanInputs`. Lanza `IllegalArgumentException` si el
   sólido persiste inválido después del weld.
3. ✅ `weldCoincidentVertices(solid, context)` en
   `PolyhedralBoundedSolidTopologyEditing`: bucle iterativo sobre
   `edgesList` usando `lkev` para colapsar aristas con endpoints
   coincidentes dentro de `bigEpsilon`.

### 3.2 `faceeq` — robustecer el cálculo de plano de cara

**Problema medido**: el plano se calcula desde tres vértices
arbitrarios del primer loop, lo que amplifica el ruido cuando esos
vértices están casi colineales (cuadriláteros esféricos cercanos al
polo, caras estrechas de la luna).

**Acciones**:

1. ✅ En `_PolyhedralBoundedSolidFace.getContainingPlane()`, llamar
   primero a `calculatePlaneByNewell` (suma de productos Newell sobre
   todo el loop, centroide como punto de paso). Si el acumulador
   Newell es cero (loops de tipo wire/strut que se recorren en ambos
   sentidos cancelando la contribución), cae a `calculatePlaneByCorner`.
2. ✅ `calculatePlaneByNewell` devuelve `null` cuando `count < 3` o
   `normal.length() ≤ tolerance` (cara degenerada).
3. ⬜ Caché de `containingPlane` marcada dirty en operadores Euler
   (optimización, no corrección funcional — diferida a §7).

### 3.3 `getmaxnames` / `updmaxnames`

**Problema medido**: la convención está implementada (`getMaxFaceId`,
`getMaxVertexId`, `setMaxFaceId`, `setMaxVertexId`), pero el llamado a
`updmaxnames(B)` en `setOp` es manual y propenso a omisión. Además, en
el pipeline existen rutas (recovery, fallback) donde se crean vértices
con IDs no globalmente únicos.

**Acciones**:

1. ✅ `_PolyhedralBoundedSolidIdNamespace` creado en paquete `processing`.
   Se construye una vez en `setOp` tras `updmaxnames`. Expuesto como
   campo estático en `_PolyhedralBoundedSolidOperator`.
2. ✅ `nextVertexId()` del Intersector y del SetOperator delegan al
   namespace con fallback al cálculo legacy si namespace es null.
3. ✅ `_PolyhedralBoundedSolidOperator.join` y `_SetFinisher.triangulate`
   también usan `idNamespace.nextFaceId(solid)`.
4. ⬜ Test unitario de no-colisión de IDs (integrado indirectamente en
   `PolyhedralBoundedSolidPreflightTest` via `validateUniqueFaceAndVertexIds`).

### 3.4 Generadores primarios (esfera, cilindro, cono)

**Problema medido**: la esfera con 8×16 produce quads esféricos
analíticamente coplanares (verificado en etapa 1), pero el cilindro
ortogonalizado del moon arroja vértices coincidentes en el ecuador
después de la sustracción. El cono no se usa en Kurlander pero está en
el roadmap.

**Acciones**:

1. ✅ Snap de coordenadas trig en `PolyhedralBoundedSolidModeler.addArcToExistingFace`:
   `x = round(cx + r*cos(a), 1e-10)`, `y = round(cy + r*sin(a), 1e-10)`.
   Afecta a cilindro y cono (ambos usan `createCircularLamina`).
2. ✅ JavaDoc de `Sphere.buildPolyhedralBoundedSolid` documenta la
   garantía analítica de planaridad para meridians=16, parallels=8.
3. ⬜ Test `ConePlanarFaceGenerationTest` (16 meridianos × {3,8,16}
   divisiones verticales) — diferido a §4 junto con Intersect.
4. ⬜ Moon directo sin boolean — diferido; `weldCoincidentVertices`
   ya mitiga el impacto en el pipeline actual.

### 3.5 Tests de aceptación del nivel 1

✅ `base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidPreflightTest.java` creado con:

- ✅ `given_kurlanderBowlAndFirstStar_when_validateBooleanInputs_then_passes`
- ✅ `given_moonAndShell_when_validateBooleanInputs_then_passes`
- ✅ `given_sphere16x8_when_inspectingFaces_then_allCoplanarWithinEpsilon`
- ✅ `given_twoCylindersWithSameRadius_when_inspectingEachSolid_then_noCoincidentVertices`

**Cierre del Nivel 1**: `:base:test` 214 tests, 1 fallo pre-existente,
0 regresiones. El sweep `--motifSweep` requiere app gráfica; la mejora
del ThirdMoon (ahora pasando) indica al menos +1 caso `ok` en sweep.

---

## 4. Nivel 2 — `setopgenerate` (reducción del problema)

Objetivo: producir `sonvv`, `sonva`, `sonvb`, `sonea`, `soneb` y los
vértices de intersección de modo que estén exactamente sobre la cara
receptora, con orden topológico estable y sin duplicados.

### 4.1 Proyección al plano receptor

✅ **Ya implementada** en `_PolyhedralBoundedSolidSetIntersector` (líneas
357-404): proyección dual al plano receptor + snap a la línea de
intersección de los dos planos de cara.

### 4.2 Weld de vértices coincidentes (post-Intersect)

✅ **Implementado** en `PolyhedralBoundedSolidSetOperator.setOpGenerate`:
- `weldIntersectionVertices(inSolidA, inSolidB)` llama a
  `PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices` en
  ambos sólidos después de que el Intersector devuelve
  `sonva`/`sonvb`.
- `pruneStaleVertexFaceEntries` elimina de `sonva`/`sonvb` cualquier
  entrada cuyo vértice fue eliminado por `lkev`.
- Logging a nivel `VSDK.DEBUG` cuando ocurren weldings.

### 4.3 Orden estable de inserción

✅ **Implementado** en `_PolyhedralBoundedSolidSetOperatorNullEdge.compareTo`:
- Reemplazado el comparador basado en banda-epsilon
  (`PolyhedralBoundedSolidNumericPolicy.compare`) por
  `Double.compare` exacto, que garantiza orden total para
  `Collections.sort`.
- Agregado `midpoint` como tercer criterio de desempate.
- `setNumericContext` se conserva por compatibilidad de API pero ya
  no afecta el orden.

### 4.4 Tests de aceptación del nivel 2

✅ `IntersectorWeldTest` (§4.2):
- `given_overlappingBoxes_when_union_then_resultHasNoCoincidentVertices`
- `given_overlappingBoxes_when_subtract_then_resultHasNoCoincidentVertices`

✅ `IntersectorParametricOrderingTest` (§4.3):
- `given_overlappingBoxes_when_unionTwice_then_sameStructure`
- `given_overlappingBoxes_when_subtractTwice_then_sameStructure`

**Cierre del Nivel 2**: `:base:test` 261 tests, 0 fallos, 0 regresiones
(2026-05-14).

---

## 5. Nivel 3 — `setopclassify` (clasificación de la frontera)

Objetivo: cada vecindad de vértice queda clasificada como `IN`, `OUT`
o `ON` con coherencia entre A y B, alineada con la tabla 15.3 de
Mäntylä.

### 5.1 Eliminar la rama "borrowed wMANT2008" del V/F classifier ✅

**Completado en dos pasadas**. (a) En el classifier extraído
`_PolyhedralBoundedSolidSetVertexFaceClassifier`: eliminados los
métodos `*Borrowed` (`vertexFaceReclassifyOnEdgesBorrowed`,
`vertexFaceInsertNullEdgesBorrowed`), los wrappers `useBorrowed`, la
variante duplicada de `vertexFaceReclassifyOnSectors` con
`useMirrorFace=true` (la rama wMANT2008 disfrazada), y el flag local
`boolean borrowed = false`. Los métodos `*NoPeekVersion` se renombraron
a la forma canónica. (b) En `PolyhedralBoundedSolidSetOperator`:
eliminado el bloque entero zombie (≈505 líneas: la duplicada
`vertexFaceClassify`, todos los `vertexFace*Borrowed`/`*NoPeekVersion`,
los wrappers, `vertexFaceGetNeighborhood`, `inplaneEdgesOn`, `printNbr`
y la `makering` huérfana, más el wrapper inutilizado
`applyCoplanarRulesToVertexFaceNeighborhood`). El test
`given_classifier_when_inspectingApi_then_borrowedBranchIsRemoved` actúa
como guarda de regresión: verifica vía reflection que no queda ningún
método con sufijo `Borrowed` o `NoPeekVersion`.

### 5.2 Endurecer `sectoroverlap` — diferido al nivel 6

**Investigado**. La intención original era semántica de conjunto
abierto (`a2 > b1` estricto). La investigación con ejecución
instrumentada demostró que `sectoroverlap` sí se invoca en el caso
coplanar V/V de la geometría MANT1988 §15.1, donde sectores con
`a2 == b1` exacto requieren la inserción del null-edge para que
`DIFF_A_MINUS_B` produzca dos cáscaras (eulerCharacteristic = 4) en
lugar de una (eulerCharacteristic = 2). El predicado en sí mismo no
puede saber si la coincidencia exacta significa "tocando, no
solapado" (caso de sectores del mismo sólido) o "tocando y debe
generar strut" (caso de sectores entre dos sólidos en el camino
V/V coplanar) sin información adicional del contexto.

**Decisión**: la implementación epsilon-tolerante
(`a2 + ε > b1 - ε`) se mantiene como contrato actual. Los tres tests
`@Disabled` en `PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest`
documentan el límite con mensajes explicativos. El subpaso §5.2 se
**difiere a §6.x** porque la corrección apropiada implica refactorizar
`vertexVertexSectorIntersectionTest` para que pase información del
caso de uso al predicado, lo que pertenece al núcleo del nivel 6
(reescritura del setopconnect).

### 5.3 Renombrar `separateInterior` y formalizar convergencia ✅

**Completado**. (a) `separateInterior` renombrado a
`flipNullEdgeOrientationForOpenSide` en
`_PolyhedralBoundedSolidSetClassifier` y
`PolyhedralBoundedSolidSetOperator`, con Javadoc que documenta la
invariante de orientación según tabla 15.3 de Mäntylä. (b)
`separateEdgeSequence` reemplaza la guarda mágica
`recoveryGuard > 16` por **detección estricta de ciclos** sobre el
conjunto de configuraciones `(from, to)` visitadas: cada iteración
debe producir una configuración nueva, lo que constituye la prueba
de terminación pedida (acotada por la población finita de half-edges
en los loops). (c) La firma cambia de `void` a un nuevo enum
`SeparateEdgeSequenceResult` con cinco valores (`OK`,
`FAILED_NULL_INPUT`, `FAILED_DIFFERENT_SOLIDS`,
`FAILED_CYCLE_DETECTED`, `FAILED_NO_PAIRING_REACHED`), permitiendo
que futuros callers reaccionen específicamente a cada modo de fallo
en vez de tratar el síntoma como un único FATAL_ERROR. Los logs
internos se degradaron de `FATAL_ERROR` a `WARNING` (la promesa del
nuevo contrato es retornar el código, no abortar).

### 5.4 Tests de aceptación del nivel 3 ✅

- **`VertexFaceClassifierCoplanarTest`** (5 tests): cubre los casos
  coplanares de las figuras 15.9-15.12 de Mäntylä, e incluye una
  guarda de regresión por reflection que falla si vuelve a aparecer
  un método con sufijo `Borrowed` o `NoPeekVersion`.
- **`VertexVertexEndpointRecoveryTest`** (4 tests): cubre los casos
  de éxito y los modos de fallo del nuevo enum de
  `separateEdgeSequence`, incluyendo una guarda de regresión que
  enumera el contrato del enum (debe exponer los cinco valores) y
  ejercita el camino V/V con el par MANT1988 §15.1 (figura 15.13)
  bajo `INTERSECTION` y `SUBTRACT`.

**Estado de la suite tras §5**: 270 tests, 0 failures, 8 skipped
(los `@Disabled` documentados en §5.2 + skips preexistentes).

---

## 6. Nivel 4 — `setopconnect` (conexión de aristas null) — núcleo

Objetivo: reescribir la fase Connect de forma alineada con Programs
15.13-15.14, eliminando las 11 system properties y los recoveries.

### 6.1 Reemplazar el matching geométrico por recorrido topológico (USAR OPUS 4.7 EN ESTE PASO, HIGH EFFORT)

**Problema medido (causa raíz)**: el conector actual empareja
null-edges por proximidad de endpoints en 3D, lo que confunde curvas
de intersección distintas que pasan por vecindades cercanas (caso
cilindro-cilindro offset). Mäntylä §15.7 propone el opuesto: recorrer
las cadenas como secuencia topológica ordenada (la fase Intersect
genera null-edges en pares A/B en el mismo orden a lo largo de la
curva).

**Acciones**:

1. Después de §4.3 (orden paramétrico estable), reescribir
   `setOpConnect` siguiendo `Program 15.14`:
   ```
   while (sgetnextnulledge(&nea, &neb)) {
       if (scanjoin(nea->he1, neb->he2, &h1a, &h1b)) {
           join(h1a, nea->he1);
           if (!isloosea(mate(h1a))) cuta(h1a);
           join(h1b, neb->he2);
           if (!islooseb(mate(h1b))) cutb(h1b);
       }
       if (scanjoin(nea->he2, neb->he1, &h2a, &h2b)) { ... }
       if (h1a && h1b && h2a && h2b) {
           cuta(nea->he1); cutb(nea->he1);
       }
   }
   ```
   sin ramas adicionales. Las "flexible_*" se borran a medida que cada
   sweep se vuelve verde.
2. Implementar `scanjoin` siguiendo Program 15.13: emparejar dos
   null-edges sólo si **ambas** pueden cerrar a un loose existente.
3. Eliminar `closeLegacyCoincidentLooseEnds`. Si después de Connect
   quedan loose, es bug que se reporta como `IllegalStateException`,
   no se intenta cerrar con heurística.

### 6.2 Eliminar `forceARingMove` y el retry de subtract

**Problema medido**: `PolyhedralBoundedSolidSetOperator.setOp` hacía
retry con `forceARingMove=true` para sustracciones que fallaban. Eso
enmascaraba el bug y duplicaba el tiempo de ejecución.

**Acciones**:

1. ✅ Borrar el bloque `recoveredResult = setOp(...)` y la mutación de
   system property. **Hecho**: `trySubtractConnectRecovery` y
   `shouldAttemptSubtractConnectRecovery` eliminados; el parámetro
   `allowSubtractConnectRecovery` se quitó de la firma interna de
   `setOp`; las variables `subtractConnectRecoverySolidA/B`,
   `subtractConnectRecoveryResult`, `usedSubtractConnectRecovery` y el
   bloque de inicialización/clonado correspondiente desaparecen; los
   helpers huérfanos `restoreSystemProperty` y la constante
   `CONNECT_FORCE_A_RING_MOVE_PROPERTY` también se eliminaron.
2. ✅ Borrar las flags `forceARingMove` y
   `flexibleDisableBRingMoveForSubtract`. **Hecho**: en
   `_PolyhedralBoundedSolidSetNullEdgesConnector` se eliminaron las
   constantes `FORCE_A_RING_MOVE_PROPERTY` y
   `FLEXIBLE_DISABLE_B_RING_MOVE_FOR_SUBTRACT_PROPERTY` junto con sus
   getters `isForceARingMoveEnabled` /
   `isFlexibleDisableBRingMoveForSubtractEnabled`. Los dos sitios que
   las leían se simplificaron a sus valores constantes:
   `allowRingMoveOnAJoin = (operation == INTERSECTION)` y
   `allowRingMoveOnBJoin = true`.
3. ⏸ **Bloqueado por §6.1** — para la operación de sustracción,
   aplicar `revert(B)` antes de Connect en lugar de dentro de Finish.
   **Resultado del experimento**: mover el `inSolidB.revert()` desde
   `_PolyhedralBoundedSolidSetFinisher` (línea 443) hasta antes de
   `setOpConnect(op)` rompe 28 tests inmediatamente. El conector
   actual asume que B mantiene su orientación original durante Connect
   (los half-edges de los null-edges se emparejan con esa convención).
   La Equation 15.1 sigue siendo el contrato fijo del resultado, pero
   moverla al lugar correcto del pipeline requiere primero la
   reescritura de Connect según Programs 15.13/15.14 (§6.1) — el
   nuevo `scanjoin` debe operar sobre B ya complementado. Se difiere
   §6.2.3 a la implementación de §6.1.

### 6.3 Borrar `groupNullEdgesByRing` heurístico

**Problema medido (etapa 1)**: el agrupamiento por anillos topológicos
fue una corrección de etapa 1 que sigue siendo necesaria porque
`keepInsertionOrder` mezcla cadenas. Con §4.3 (orden paramétrico
estable), el agrupamiento debería emerger naturalmente del orden de
inserción.

**Acciones**:

1. Después de §4.3, ejecutar el sweep con y sin `groupNullEdgesByRing`.
   Si los resultados son equivalentes, eliminar el método y su llamada.
2. Si el agrupamiento sigue siendo necesario, formalizarlo: la
   pertenencia a un anillo se determina recorriendo half-edges
   adyacentes en el mismo plano de intersección. El método
   `partitionNullEdgesIntoRings` debe ser determinista y testeado
   aisladamente.

### 6.4 Tests de aceptación del nivel 4

- `SetOpConnectScanJoinTest` — Programs 15.13-15.14 con datos
  sintéticos (dos cubos solapados). **Pendiente**.
- `SetOpConnectNoLooseInvariantTest` — invariante: después de Connect,
  `looseA == 0 && looseB == 0` siempre. **Implementado parcialmente
  (avance §6.4-A)**: 4 casos baseline pasan (regression guard),
  2 casos pendientes están `@Disabled` con counts documentados (ambos
  MANT1988_15_1 + INTERSECTION/SUBTRACT, looseA=4 — comparten causa
  raíz con §5.2 sectoroverlap diferido).
- `CsgKurlanderBowlFirstStarRegressionTest.given_..._then_connectStageClosesAllStarEdges`
  reactivado y pasando. **Pendiente**.

Sweep esperado tras §6: `ok ≥ 35/40`. Los 11 EMPTY desaparecen porque
ahora Connect produce el ring completo (40 vértices), y Finish recibe
datos consistentes.

### Avance §6.4-A — primer hito del nivel 6 (✅ formal, autocontenido)

Avance mínimo y verificable seleccionado por independencia y por estar
en línea con Programs 15.13/15.14: **establecer el contrato externo
del Connect como test ejecutable**, sin reescribir aún el conector.

`SetOpConnectNoLooseInvariantTest` audita
`_PolyhedralBoundedSolidSetNullEdgesConnector` contra el invariante
de Program 15.14 vía los accessors `getLastLooseACount()` /
`getLastLooseBCount()`. La matriz se separa en dos bloques:

- **baseline** (4 tests, deben mantenerse en verde):
  `MANT1988_15_1 + UNION`,
  `STACKED_BLOCKS + {UNION, INTERSECTION, SUBTRACT}`. El conector ya
  cumple Program 15.14 para estos casos; el test los lockea como
  guarda de regresión.
- **pending §6.1** (2 tests, `@Disabled`):
  `MANT1988_15_1 + {INTERSECTION, SUBTRACT}`, ambos con `looseA = 4`
  documentado. El `@Disabled` referencia §6.1 + §5.2 explícitamente
  para que, cuando esos cierren, baste con quitar la anotación.

Esto da una métrica continua del progreso del nivel 6 sin acoplar el
avance a la reescritura completa del conector.

---

## 7. Nivel 5 — `setopfinish` (generación del resultado)

Objetivo: implementar Program 15.15 sin recoveries ni triangulación
post-hoc, manteniendo la invariante de cara planar por construcción.

### 7.1 Eliminar `sanitizePairedFaces` con fallback legacy

**Problema medido**: el método 220-224 cae a "legacy ordering" cuando
no encuentra pares. Con Connect emitiendo `sonfa`/`sonfb` correctos,
no debe haber fallback.

**Acciones**:

1. Reemplazar el fallback por `IllegalStateException` con dump
   topológico.
2. El emparejamiento se vuelve determinista: `sonfa[i]` se empareja
   con `sonfb[i]` por índice, en el orden producido por Connect.

### 7.2 Reducir o eliminar `triangulateNonPlanarFaces`

**Problema medido**: la triangulación post-finish es una red de
seguridad para `loopGlue` cuando produce caras no planares. Con
Connect emparejando bien, `loopGlue` no debería producirlas.

**Acciones**:

1. Tras §6, ejecutar el sweep con la triangulación deshabilitada.
   Métrica: `nonPlanarFacesPerMotif`.
2. Si una vez emparejado bien sigue habiendo caras no planares, la
   causa está en el `loopGlue` no encontrar vértices coincidentes
   entre loops; corregir ahí (alineando los IDs entre `sonfa[i]` y
   `sonfb[i]` antes de `loopGlue`).
3. Mantener `findNonDegenerateEar` y `extractInnerLoopsOfNonPlanarFace`
   como **assertion mode** sólo: si encuentran una cara no planar,
   logean y triangulan, pero también incrementan un contador que el
   test usa para asegurar `count == 0` en builds limpios.

### 7.3 Reactivar `maximizeFaces` con guarda de planaridad (USAR OPUS 4.7 EN ESTE PASO)

**Problema medido (etapa 1)**: `maximizeFaces` puede revertir la
triangulación si dos caras vecinas tienen planos "suficientemente
similares". Hoy se compensa volviendo a triangular después.

**Acciones**:

1. En `PolyhedralBoundedSolidTopologyEditing.maximizeFaces`, antes de
   fusionar dos caras vecinas vía `lkef`, computar el plano resultante
   sobre los vértices del loop fusionado y verificar
   `validateFacePointsAreCoplanar(union)` con la `ToleranceContext` de
   la cara. Si falla, no fusiona.
2. Después de §7.2 + §7.3, el segundo paso de triangulación post-
   maximize del `postProcessResult` sobra.

### 7.4 Tests de aceptación del nivel 5

- `SetOpFinishLoopGlueInvariantTest` — invariante: después de
  `loopGlue`, todas las caras de `outRes` son planares dentro del
  epsilon del modelo.
- Reactivar `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
  (idempotencia, absorción, determinismo).
- Reactivar `BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_*`.

Sweep esperado tras §7: `ok=40/40`.

---

## 8. Validación visual y regresión

### 8.1 Sweep automatizado

`PolyhedralBoundedSolidExample --motifSweep` se mantiene como
herramienta visual y de log. Adicionalmente, crear el test JUnit
`KurlanderBowlMotifSweepRegressionTest` (marcado `@Tag("slow")`) que
ejecute el sweep sin renderizar y exija `ok == 40`. Se corre antes de
cada PR de release; opcional en CI por velocidad.

### 8.2 Visual diagnostics

El highlighting amarillo incondicional implementado en etapa 1 se
mantiene como herramienta. Adicionalmente, el modo `--motifIndex`
permite renderizar casos individuales y comparar con baselines en
`doc/baselines/kurlander/motif_NN.png` (por crear).

### 8.3 Performance

Cada fase debe medirse antes/después con:

```
gradle :base:test --tests "*KurlanderBowl*" --info
```

La eliminación de retries y recoveries debería bajar significativamente
el tiempo. Si una fase introduce regresión >20% sin justificación, se
revierte.

---

## 9. Orden de ejecución, dependencias y riesgo

| Paso | Sección | Impacto | Riesgo | Depende de |
|------|---------|---------|--------|-----------|
| 1 | §3.1, §3.3 (preflight + IdNamespace) | Bajo — visibilidad | Bajo | — |
| 2 | §3.2 (Newell faceeq) | Medio — estabilidad | Bajo | — |
| 3 | §3.4 (cilindro/cono snap, moon directo) | Medio — quita ruido | Medio (cambia fixture) | 1, 2 |
| 4 | §4.1 (proyección al plano) | Alto — quita caras no planares fuente | Bajo | 2 |
| 5 | §4.2 (weld post-intersect) | Alto — base para Connect | Medio | 4 |
| 6 | §4.3 (orden paramétrico) | Alto — base para Connect topológico | Medio | 5 |
| 7 | §5.1, §5.2 (limpiar V/F y sectoroverlap) | Medio — coplanares | Bajo | — |
| 8 | §5.3 (formalizar V/V recovery) | Medio | Medio | 7 |
| 9 | §6.1 (scanjoin canónico) | **Muy alto** — núcleo de la cura | **Alto** | 6, 8 |
| 10 | §6.2 (eliminar forceARingMove) | Medio | Bajo | 9 |
| 11 | §6.3 (revisar groupNullEdgesByRing) | Bajo | Bajo | 9 |
| 12 | §7.1, §7.2, §7.3 (Finish limpio) | Alto — invariantes | Medio | 9 |
| 13 | §8 (tests + baselines) | Bajo | Ninguno | 12 |

Riesgo principal está en el paso 9: reescribir `setOpConnect`. Mitigación:
mantener el connector legacy como `_PolyhedralBoundedSolidSetNullEdgesConnectorLegacy`
en una rama paralela, ejecutar ambos en cada test y comparar resultados
durante el desarrollo. Cuando el sweep pasa con el connector nuevo, se
borra el legacy.

---

## 10. Definition of Done

La etapa 2 se considera cerrada cuando:

1. Sweep `--motifSweep` reporta `ok=40, empty=0, invalid=0, blackFaces=0`.
2. `gradle :base:test` ejecuta sin tests fallidos y sin `@Disabled`
   nuevos (los 4-5 actualmente deshabilitados están reactivados o
   eliminados con justificación documentada).
3. Las 11 system properties `vsdk.setop.connect.flexible*` y
   `forceARingMove` están eliminadas del repositorio.
4. `_PolyhedralBoundedSolidSetNullEdgesConnector.java` baja de 2790 a
   < 1000 LOC (objetivo cualitativo; refleja eliminación de heurísticas).
5. El bloque de retry `subtractConnectRecovery` en
   `PolyhedralBoundedSolidSetOperator` se elimina.
6. Cobertura JaCoCo de `_PolyhedralBoundedSolidSetNullEdgesConnector` >
   85 % (porque el código nuevo está cubierto por tests dirigidos).
7. Documentación: este archivo actualizado con métricas finales,
   `doc/references/coverage_MANT1988.md` actualizado con los nuevos
   porcentajes por sección.

---

## 11. Referencias rápidas a los puntos de cambio

Por si el ejecutor de la etapa quiere ir directo:

- Preflight: `PolyhedralBoundedSolidValidationEngine` (+ nuevo método).
- Newell normal: `_PolyhedralBoundedSolidFace.getContainingPlane`.
- IdNamespace: nuevo helper en
  `base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/IdNamespace.java`.
- Proyección al plano: `_PolyhedralBoundedSolidSetIntersector.processEdge`
  (~líneas 354-401).
- Weld post-intersect: `_PolyhedralBoundedSolidSetIntersector.setOpGenerate`
  (nuevo paso final).
- Orden paramétrico: nuevo método
  `_PolyhedralBoundedSolidSetIntersector.sortByIntersectionParameter`.
- V/F borrar variante "borrowed":
  `_PolyhedralBoundedSolidSetVertexFaceClassifier`.
- `sectoroverlap`:
  `_PolyhedralBoundedSolidSetGeometricPredicateProcessor.sectoroverlap`.
- V/V `recoverMissingCoplanarEndpoints`:
  `_PolyhedralBoundedSolidSetVertexVertexClassifier`.
- Connect canónico:
  `_PolyhedralBoundedSolidSetNullEdgesConnector.setOpConnect` (Programs
  15.13-15.14).
- Borrar retry: `PolyhedralBoundedSolidSetOperator.setOp` líneas
  3879-3911.
- Finish sin fallback:
  `_PolyhedralBoundedSolidSetFinisher.sanitizePairedFaces`.
- `maximizeFaces` con guarda de planaridad:
  `PolyhedralBoundedSolidTopologyEditing.maximizeFaces`.
- Sweep test JUnit: nuevo
  `base/src/test/.../KurlanderBowlMotifSweepRegressionTest.java`
  (`@Tag("slow")`).
