# fontFixPlan.md — Eliminar la cara degenerada del glyph "A" (FONT_BLOCK)

Plan de implementación detallado
Lenguaje del documento: español. **Todo el código/identificadores en inglés**,
siguiendo las "Coding Rules for Agents" de `CLAUDE.md` (tipos explícitos, una
declaración por línea, `camelCase`, imports en vez de nombres totalmente
cualificados, null-checks defensivos con `VSDK.reportMessage(...)`).

---

## 1. Objetivo

El flujo por defecto del visor
`testsuite/Jogl4Examples/PolyhedralBoundedSolidExample` muestra el glyph "A"
importado de `etc/fonts/cyrvetic.ttf` (modelo `SolidModelNames.FONT_BLOCK`).
El glyph se ve bien geométricamente, pero tiene un **error topológico**: dos
parejas de vértices coincidentes — `(9, 12)` y `(21, 24)` — separadas por una
distancia ≈ 0, que generan una **cara degenerada de área 0** (un "sliver") en
el ápice del hueco triangular interior de la letra.

La meta es **añadir un paso de simplificación** en la construcción de los loops
del glyph, **antes de la extrusión**, que evite agregar parejas de puntos cuya
distancia sea menor que una tolerancia de soldadura ("weld"). Hay que tratar con
cuidado el **caso especial de polígonos cerrados** cuyo último punto vuelve a la
posición del primero: la forma debe quedar cerrada **sin** vértices repetidos.

---

## 2. Causa raíz (verificada con datos reales, no especulación)

Se extrajo el contorno crudo del glyph "A" desde `cyrvetic.ttf` (mismas
transformaciones que usa el lector: tamaño 10pt, dividir por `factor = 10`,
`y` negada). Resultado:

**Contorno externo (8 esquinas reales):**
```
MOVE (0.268750, 0.728125)   <- inicio
LINE (0.385938, 0.728125)
LINE (0.651563, 0.000000)
LINE (0.542188, 0.000000)
LINE (0.467188, 0.212500)
LINE (0.182813, 0.212500)
LINE (0.114063, 0.000000)
LINE (0.004688, 0.000000)
LINE (0.268750, 0.728125)   <- IGUAL al inicio (distancia 0) -> cierre
CLOSE
```

**Contorno interno = hueco triangular (3 esquinas reales + ruido):**
```
MOVE (0.326563, 0.640625)   <- inicio (S)
LINE (0.214063, 0.301563)
LINE (0.443750, 0.301563)
LINE (0.329688, 0.640625)   <- a SOLO 0.003125 de S (casi-duplicado)
LINE (0.326563, 0.640625)   <- IGUAL a S (distancia 0) -> cierre
CLOSE
```

Mapa a vértices del B-Rep (antes de extrudir), `nextVertexId` consecutivo:

- Loop externo -> vértices `1..8` (el punto de cierre, distancia 0 con el
  inicio, **ya** se rechaza correctamente). Sin problema.
- Loop interno -> vértices `9, 10, 11, 12`:
  - `9`  = `S`               (0.326563, 0.640625)
  - `10` = (0.214063, 0.301563)
  - `11` = (0.443750, 0.301563)
  - `12` = (0.329688, 0.640625) ← casi-duplicado de `9` (**0.003125**)
  - el punto de cierre exacto (distancia 0 con `9`) **sí** se rechaza.

El triángulo interior REAL son las esquinas `9, 10, 11`. El vértice `12` es
ruido de la fuente (un ápice ligeramente "achatado", dos puntos a ~3 unidades de
em). La extrusión duplica el loop, por lo que el par `(9,12)` reaparece como
`(21,24)` en la copia superior. Ese es exactamente el sliver que se ve en
`screenshot.png`.

### Por qué el filtro actual NO lo atrapa

En `PolyhedralBoundedSolidModeler.shouldAcceptPolyLinePoint(...)` el criterio de
aceptación compara contra `lastAcceptedPoint` y `firstPointInLoop` usando
`VSDK.EPSILON = 1e-6`:

- El **cierre exacto** (distancia 0) cae bajo `1e-6` → se rechaza. Bien.
- El **casi-duplicado** `12` está a `0.003125`, que es `> 1e-6` → se acepta. Mal.

### Dato crítico sobre la tolerancia

`0.003125` es **mucho mayor** que el `BREP_BIG_EPSILON` actual del kernel
(`= 10 * 1e-6 = 1e-5`, en
`PolyhedralBoundedSolidNumericPolicy`). Por lo tanto, **usar `BREP_BIG_EPSILON`
tal cual NO elimina el sliver** (1e-5 < 0.003125). La tolerancia de soldadura
debe ser del orden de la escala del glyph, no del epsilon de predicados
geométricos.

Decisión recomendada: **tolerancia de soldadura relativa al tamaño del glyph**,
con `BREP_BIG_EPSILON` como piso (floor). Esto es robusto ante distintos
tamaños/fuentes y respeta la idea de "BIG_EPSILON" como tolerancia gruesa.

```
weldEpsilon = max(BREP_BIG_EPSILON, GLYPH_WELD_RELATIVE_FACTOR * bboxDiagonal)
```

Para "A": `bboxDiagonal ≈ 0.974`, `GLYPH_WELD_RELATIVE_FACTOR = 1.0e-2` →
`weldEpsilon ≈ 0.00974`. Entonces `12` (gap 0.003125 < 0.00974) se rechaza,
y las aristas reales del triángulo (longitud ≥ 0.23) se preservan con holgura.

---

## 3. Mapa del flujo (archivos y métodos exactos)

1. `testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/GeneralModelsBuilder.java`
   - `case FONT_BLOCK` (línea ~155) → `createFontBlock(".../cyrvetic.ttf", "A")`
     (línea ~603) → luego `translationalSweepExtrudeFacePlanar(...)` (extrusión).
   - `createFontBlock` llama
     `AwtFontReader.extractGlyph(...)` y luego
     `PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve)`.

2. `awt/src/vsdk/toolkit/render/awt/AwtFontReader.java`
   - `extractGlyph(...)`: convierte el `GlyphVector` de AWT en un
     `ParametricCurve`. Inserta `BREAK` en cada `MOVETO` y `CORNER`/`QUAD`/
     `BEZIER` para los demás segmentos. Filtra solo duplicados **consecutivos
     exactos** vía `shouldAddEndpoint(...)` (umbral `VSDK.EPSILON`).
   - **NO modificar** en este plan (fuera de alcance; ver §7).

3. `base/.../environment/geometry/curve/ParametricCurve.java`
   - Almacena puntos/tipos; `calculatePoints(i, false)` muestrea un segmento como
     polilínea. `getMinMax()` devuelve la caja envolvente (se usará para la
     tolerancia relativa). **NO modificar.**

4. `base/.../geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.java`
   — **AQUÍ se hace el arreglo.** Métodos relevantes:
   - `createBrepFromParametricCurve(ParametricCurve)` (línea ~441): recorre los
     segmentos, cierra loops en cada `BREAK` con `closeLoopWithMef`.
   - `processSampledSegment(...)` (línea ~409): por cada punto muestreado decide
     sembrar/aceptar.
   - `startLoopWithSeedPoint(...)` (línea ~367): primer punto del loop
     (`mvfs` para el primer loop; `smev`+`kemr` para loops internos).
   - `shouldAcceptPolyLinePoint(...)` (línea ~392): **criterio de aceptación**
     (compara contra `lastAcceptedPoint` y `firstPointInLoop` con `VSDK.EPSILON`).
   - `appendPointToCurrentLoop(...)` (línea ~400): `smev` del punto aceptado.
   - `closeLoopWithMef(...)` (línea ~424): cierra el loop con `mef`
     (y `kfmrh` para contornos internos).

5. `base/.../polyhedralBoundedSolidOperators/_BoundaryRepresentationFromCurveBuildState.java`
   - Estado mutable del build (ids, `firstPointInLoop`, `lastAcceptedPoint`,
     etc.). Aquí se añaden campos nuevos.

---

## 4. El arreglo (quirúrgico, mínimo, verificable)

La idea: el chequeo `firstPointInLoop` es **exactamente** el manejo del "caso
especial de polígono cerrado que vuelve a su primer vértice". Hoy usa
`VSDK.EPSILON` (solo atrapa el cierre exacto). Lo subimos a una **tolerancia de
soldadura `weldEpsilon`** calculada por glyph. Con eso, tanto el cierre exacto
(`seg14`, dist 0) como el casi-duplicado (`seg13`, dist 0.003) se rechazan, y el
loop interno queda como el triángulo limpio `9,10,11`, cerrado por `mef` sin
vértice repetido.

### EDIT 1 — `_BoundaryRepresentationFromCurveBuildState.java`

Añadir dos campos y su inicialización.

- Añadir import:
  ```java
  import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
  ```
- Añadir campos (una declaración por línea):
  ```java
  double weldEpsilon;
  int verticesInCurrentLoop;
  ```
- En el constructor, inicializar:
  ```java
  weldEpsilon = PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON;
  verticesInCurrentLoop = 0;
  ```
  (El valor real se sobrescribe en `createBrepFromParametricCurve`; el piso
  `BREP_BIG_EPSILON` queda como respaldo si el glyph fuera degenerado.)

### EDIT 2 — `PolyhedralBoundedSolidModeler.java`

**2.1** Añadir import (junto a los demás `...polyhedralBoundedSolid.*`):
```java
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
```

**2.2** Añadir una constante de clase (cerca de otras constantes privadas):
```java
// Weld tolerance for glyph/poly-line simplification, expressed as a fraction
// of the contour bounding-box diagonal. The cyrvetic 'A' inner contour carries
// a font artifact ~3.1e-3 away from the apex; a relative weld removes it while
// preserving real edges (>= 0.23 long here). See fontFixPlan.md / [MANT1988].12.
private static final double GLYPH_WELD_RELATIVE_FACTOR = 1.0e-2;
```

**2.3** En `createBrepFromParametricCurve(ParametricCurve curve)`, **antes** del
bucle, calcular la tolerancia relativa al tamaño del glyph y guardarla en el
estado:
```java
double[] minMax = curve.getMinMax();
double dx = minMax[3] - minMax[0];
double dy = minMax[4] - minMax[1];
double dz = minMax[5] - minMax[2];
double bboxDiagonal = Math.sqrt(dx * dx + dy * dy + dz * dz);
state.weldEpsilon = Math.max(
    PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON,
    GLYPH_WELD_RELATIVE_FACTOR * bboxDiagonal);
```
Null-check defensivo: si `minMax == null` o `bboxDiagonal` no es finito o es 0,
dejar `state.weldEpsilon` en su valor por defecto y emitir
`VSDK.reportMessage(...)` con texto accionable (p. ej. "glyph bbox degenerate,
falling back to BREP_BIG_EPSILON weld").

**2.4** En `shouldAcceptPolyLinePoint(...)`, reemplazar **ambos** `VSDK.EPSILON`
por `state.weldEpsilon`:
```java
private static boolean shouldAcceptPolyLinePoint(
    _BoundaryRepresentationFromCurveBuildState state, Vector3Dd point)
{
    return Vector3Dd.distance(point, state.lastAcceptedPoint) >
        state.weldEpsilon &&
        Vector3Dd.distance(point, state.firstPointInLoop) >
        state.weldEpsilon;
}
```
- El término `lastAcceptedPoint` suelda casi-duplicados **consecutivos**.
- El término `firstPointInLoop` es el **caso especial del polígono cerrado**:
  cualquier punto que regrese a la posición del primer vértice del loop (exacto o
  casi) NO se agrega; el cierre lo provee la topología (`mef`), no un vértice
  repetido. **Mantener este término es obligatorio.**

**2.5** Llevar la cuenta de vértices del loop actual (para la guarda defensiva):
- En `startLoopWithSeedPoint(...)`, al final, poner:
  ```java
  state.verticesInCurrentLoop = 1;
  ```
- En `appendPointToCurrentLoop(...)`, al final, incrementar:
  ```java
  state.verticesInCurrentLoop++;
  ```
- En `closeLoopWithMef(...)`, **al inicio**, guarda defensiva (no estructural):
  ```java
  if ( state.verticesInCurrentLoop < 3 ) {
      VSDK.reportMessage(/* origen */ null,
          VSDK.WARNING /* o nivel equivalente usado en el archivo */,
          "closeLoopWithMef",
          "Degenerate glyph loop with " + state.verticesInCurrentLoop +
          " distinct vertices after welding; result may be invalid.");
  }
  ```
  Verificar la firma real de `VSDK.reportMessage(...)` en
  `base/.../common/VSDK.java` y ajustarla (no inventar parámetros). Esta guarda
  **solo registra**; con el arreglo, los loops normales tienen >= 3 vértices, así
  que no debe dispararse para "A". (El manejo estructural de loops < 3 queda
  fuera de alcance.)
- En `closeLoopWithMef(...)`, al final (donde ya se resetea `beginningOfLoop` y
  `lastAcceptedPoint`), añadir:
  ```java
  state.verticesInCurrentLoop = 0;
  ```

> No tocar la lógica de índices de `mef`/`kfmrh`/`smev`/`kemr`: como `12` ya no se
> agrega, `nextVertexId` y los ids que usa `closeLoopWithMef` quedan consistentes
> automáticamente (el loop interno cierra con 3 vértices, igual que el externo
> cierra con 8). El conteo variable de vértices por loop ya está soportado.

---

## 5. Caso especial de polígono cerrado (tratamiento explícito)

Requisito del usuario: *"polígonos cerrados que empiezan en un vértice y terminan
en otro vértice en su misma posición... darle manejo para que quede la forma
cerrada, sin vértices repetidos."*

Cómo lo cumple este plan:

- El último segmento del contorno de fuente vuelve a la posición inicial
  (`seg8` en el externo, `seg14` en el interno). El término `firstPointInLoop`
  de `shouldAcceptPolyLinePoint` **rechaza** ese punto de retorno (exacto o casi,
  bajo `weldEpsilon`), por lo que **no** se crea un vértice duplicado.
- El cierre lo realiza `closeLoopWithMef` mediante `mef` (Make-Edge-Face), que
  conecta el último vértice con el primero topológicamente. La forma queda
  **cerrada** y **sin** vértice repetido.
- Esto es coherente con [MANT1988].12.2 (construcción de contornos con
  operadores de Euler).

Invariante a respetar: nunca eliminar el **primer** vértice del loop
(`firstPointInLoop` / la semilla `mvfs`/`smev`+`kemr`); solo se descartan puntos
**posteriores** que coincidan con él dentro de `weldEpsilon`.

---

## 6. Pruebas

### 6.1 Test unitario nuevo (base)

Crear
`base/src/test/vsdk/toolkit/processing/CurveModelerGlyphWeldTest.java`
(estilo JUnit 5 + AssertJ, igual que `CurveModelerRotationalSweepTest.java`).

El test **no debe depender de AWT** (módulo `awt`). En su lugar, construir un
`ParametricCurve` a mano que reproduzca el patrón del contorno interno del "A":
un triángulo cerrado con un casi-duplicado del primer vértice cerca del cierre.

Esqueleto:
```java
package vsdk.toolkit.processing;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

import static org.assertj.core.api.Assertions.assertThat;

class CurveModelerGlyphWeldTest
{
    @Test
    void given_contourWithNearDuplicateClosingVertex_when_buildBrep_then_noZeroAreaSliver()
    {
        // Arrange: single closed triangle (CORNER segments) whose last real
        // corner sits ~3.1e-3 from the start, mirroring cyrvetic 'A' inner loop.
        ParametricCurve curve = new ParametricCurve();
        curve.setApproximationSteps(8);
        addCorner(curve, 0.326563, 0.640625); // S (start)
        addCorner(curve, 0.214063, 0.301563);
        addCorner(curve, 0.443750, 0.301563);
        addCorner(curve, 0.329688, 0.640625); // near-duplicate of S (~0.0031)
        addCorner(curve, 0.326563, 0.640625); // exact return to S

        // Action
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);

        // Assert: no two distinct vertices coincide within the weld tolerance.
        assertThat(hasCoincidentVertexPair(solid, 1.0e-3)).isFalse();
        assertThat(solid.getVerticesList().size()).isEqualTo(3);
    }

    private static void addCorner(ParametricCurve curve, double x, double y)
    {
        Vector3Dd[] p = new Vector3Dd[1];
        p[0] = new Vector3Dd(x, y, 0.0);
        curve.addPoint(p, ParametricCurve.CORNER);
    }

    private static boolean hasCoincidentVertexPair(
        PolyhedralBoundedSolid solid, double tolerance)
    {
        java.util.List<_PolyhedralBoundedSolidVertex> vs = solid.getVerticesList();
        int i;
        int j;
        for ( i = 0; i < vs.size(); i++ ) {
            for ( j = i + 1; j < vs.size(); j++ ) {
                double d = Vector3Dd.distance(
                    vs.get(i).position, vs.get(j).position);
                if ( d < tolerance ) {
                    return true;
                }
            }
        }
        return false;
    }
}
```
Notas:
- Verificar que `ParametricCurve.addPoint(...)` y la firma de
  `createBrepFromParametricCurve(...)` coinciden con el código real antes de
  compilar (las he listado en §3).
- `_PolyhedralBoundedSolidVertex.position` es público (`Vector3Dd`).
- Confirmar el tipo de retorno/colección de `getVerticesList()`; si hace falta,
  ajustar el import del tipo del nodo.
- Si `createBrepFromParametricCurve` requiere un primer segmento "real" (el bucle
  empieza en `i = 1`), el `MOVE`/semilla lo aporta el primer `CORNER` (índice 0)
  como en el flujo real; mantener ese patrón. Ajustar si la validación interna
  exige un `BREAK`/segundo loop.

### 6.2 Test de no-regresión (caso degenerado ya existente)

Confirmar que un contorno **sin** casi-duplicados produce el mismo número de
vértices que antes (no se sobre-suelda). P. ej. un cuadrado de lado 1 debe dar 4
vértices.

---

## 7. Fuera de alcance (anotar, no implementar)

- `AwtFontReader.shouldAddEndpoint` y `ParametricCurve.shouldSkipConsecutiveEndpoint`
  usan `VSDK.EPSILON` y solo filtran duplicados **consecutivos exactos**. No son
  la causa del sliver (que es un casi-duplicado **no** consecutivo respecto al
  punto previo, pero **sí** cercano al inicio del loop). **No** modificarlos en
  este cambio para mantenerlo localizado en el constructor de B-Rep.
- No cambiar el valor de `BREP_BIG_EPSILON` (1e-5): otros predicados del kernel
  dependen de él. La tolerancia de soldadura del glyph es un concepto separado
  (relativo al tamaño), por eso se introduce `GLYPH_WELD_RELATIVE_FACTOR`.
- Manejo estructural de loops con < 3 vértices tras soldar (solo se registra).

---

## 8. Validación (comandos)

Desde la raíz del repo (donde está `gradle`):

```bash
# Compilar core + tests
gradle :base:compileJava :base:compileTestJava

# Test enfocado del arreglo
gradle :base:test --tests "vsdk.toolkit.processing.CurveModelerGlyphWeldTest"

# Suite de regresión relevante (no debe romperse nada)
gradle :base:test --tests "vsdk.toolkit.processing.CurveModelerRotationalSweepTest"
gradle :base:test
```

Verificación visual (opcional, diagnóstica) desde
`testsuite/Jogl4Examples/PolyhedralBoundedSolidExample`:
```bash
./run.sh    # o el modo offline:
gradle --quiet :testsuite:Jogl4Examples:PolyhedralBoundedSolidExample:runMain \
  -PrunMainClass=PolyhedralBoundedSolidExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED'
```
Con el modelo por defecto `FONT_BLOCK`, los pares `(9,12)` y `(21,24)` deben
**desaparecer** del HUD; el hueco interior queda como un triángulo limpio.

---

## 9. Definition of Done

- [ ] `:base:compileJava` y `:base:compileTestJava` compilan.
- [ ] `CurveModelerGlyphWeldTest` pasa: el B-Rep del contorno casi-duplicado
      tiene **3** vértices y **ningún** par coincidente bajo tolerancia.
- [ ] `gradle :base:test` sigue verde (sin regresiones).
- [ ] En el visor, el glyph "A" ya no muestra los pares `(9,12)` / `(21,24)`
      ni la cara de área 0.
- [ ] El primer vértice del loop nunca se elimina; los contornos cerrados quedan
      cerrados por `mef` sin vértices repetidos ([MANT1988].12.2).
- [ ] Comentarios/Javadoc concisos en inglés; sin acceso directo a campos cuando
      exista getter; `VSDK.reportMessage(...)` con texto accionable en las
      ramas defensivas.

---

## 10. Resumen del cambio

1. `_BoundaryRepresentationFromCurveBuildState.java`: + import NumericPolicy,
   + campos `weldEpsilon`, `verticesInCurrentLoop`, inicializarlos.
2. `PolyhedralBoundedSolidModeler.java`: + import NumericPolicy, + constante
   `GLYPH_WELD_RELATIVE_FACTOR`, calcular `state.weldEpsilon` desde
   `curve.getMinMax()`, usar `state.weldEpsilon` en `shouldAcceptPolyLinePoint`,
   contar vértices por loop + guarda defensiva en `closeLoopWithMef`.
3. Nuevo test `CurveModelerGlyphWeldTest.java`.
4. Compilar, correr tests, validar visualmente.
</content>
</invoke>
