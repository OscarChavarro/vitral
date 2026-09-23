# Nombres de atributos, getters y setters en todos los ports

Plan ejecutado el 2026-09-23 sobre `java/`, `cpp/` y `typescript/` (versión
1.4 de Vitral). Guarda automática: `scripts/checkAttributeNames.py`.

## Objetivo

Que todos los atributos de clase tengan nombres **semánticos, en camelCase y
sin `_`** (ni prefijo ni sufijo), y que cada atributo con accessors tenga
`getX()` / `setX(value)` cuyo nombre coincida con el atributo. Es requisito del
editor genérico: la especificación `"double;bottomRadius;(0, INFINITE)"`
implica `getBottomRadius()` y `setBottomRadius(double)`.

## Fase 0: reglas y excepciones (decididas)

1. Atributo en camelCase, sin `_` inicial/final, sin snake_case, sin nombres
   de 1–2 letras salvo las excepciones de abajo.
2. Nombre semántico (`height`, no `h`; `bottomRadius`, no `r1`).
3. Accessors `getX()` / `setX(value)`; el parámetro del setter se llama
   `value`. Los predicados booleanos pueden ser `isX()` / `hasX()`.
4. Mismo nombre en Java, C++ y TS; Java es la fuente de verdad.
5. Excepciones, todas codificadas en `scripts/checkAttributeNames.py`:
   - **E1 tipos valor**: vectores, colores, píxeles, `Complex`, cuaterniones,
     matrices. Sus accessors llevan el nombre del componente (`x()`, `r()`),
     así que C++ guarda `x_` y TS `xv`.
   - **E2 nombres cortos con significado**: `x y z w u v` (coordenadas), `id`,
     `up`, `gl` (contexto OpenGL), `to` (par de `from`).
   - **E3 emulación de APIs de terceros**: `src/main/java`, `jackson`, `org`,
     `com` en C++ y TS.
   - **E4 notación establecida**: registros de [MANT1988] cap. 15 (`va`, `vb`,
     `he`, `wa`, `wb`, `cl`, `e`…), plano `a b c d`, parámetro de rayo `t`
     (`Ray`, `Intersection`), pesos `kd`/`ks` (columnas de archivo y
     uniforms de shader), registros del formato MD2 (`s`, `t`, `st`), estado
     SHA-1 `h`.
   - **E5 registros inmutables** con accessors estilo record de Java
     (`epsilon()`, `status()`): `PolyhedralBoundedSolidNumericPolicy`,
     `_PolyhedralBoundedSolidFace.PointInsideResult` (C++ `epsilon_`).
   - **Accessors de vista** (problema M exento): `get*Reference`, `getRaw*`,
     `get*Enum`, alias (`WidgetDialog.getChildren`), estado actual de gizmos
     (`RayGizmo`, `InfinitePlaneGizmo`: `current*` frente al valor
     configurado), `ZBuffer.getZBuffer`, valores derivados.
6. Serialización: renombrar atributos privados de clases `Serializable`
   rompe objetos Java serializados antes; se aceptó (mismo criterio que
   Sphere/Cone), sin `readObject` de compatibilidad.
7. Formatos externos intactos: argumentos MCP (`scene.add_cone` sigue con
   `baseRadius`), claves JSON escritas a mano (`"t"` del volcado de Appel),
   claves compactas del objeto de cambios de `SimpleMaterial.ts` (`ior`).

**Decisión pendiente (dueño):** `Jacobian.ts` (solo TS, sin uso ni origen
Java): el significado de `A`, `B`, `C` es desconocido. Borrar la clase o
documentarla. El script lo reporta como *warning*, no como violación.

## Fase 1: inventario

`scripts/checkAttributeNames.py [--all] [--csv]` recorre clases de los tres
ports (Java, C/C++ `.h/.hpp/.cpp`, TS) con un escáner de llaves/paréntesis
que distingue cuerpos de clase de cuerpos de método, detecta declaraciones
múltiples y *parameter properties* de TS. Problemas: `P` prefijo, `S` sufijo,
`N` snake_case, `C` 1–2 letras, `M` getter trivial `getX()` que devuelve un
atributo que no se llama `x`.

Inventario inicial (2026-09-23, tras exenciones): librerías 177 Java / 154
C++ / 109 TS; testsuites 93 / 92 / 13; más ~150 accessors `M`.

## Fases 2 y 3: renombres aplicados (en los tres ports donde existen)

| Clase | Antes | Después |
|---|---|---|
| Sphere | `_radius`, `_radius_squared` / `radius_` | `radius`, `radiusSquared` |
| Cone | `r1`, `r2`, `h`; `get/setBaseRadius` | `bottomRadius`, `topRadius`, `height`; `get/setBottomRadius` |
| Camera | `_dir`/`dir`; `dx`, `dy` (sin uso); `eyePosition` | `frontWithScale`; eliminados; `position` |
| SimpleBackground | `_color` / `color_` | `color` |
| RayHit | `p`, `n`, `t`; `ray()`, `hitDistance()`, `requiredDetailMask()`; privados con `_` (C++) / `hitRay`, `distance`, `hasDistance`, `requiredMask` (TS) | `point`, `normal`, `tangent`; `getRay()`, `getHitDistance()`, `getRequiredDetailMask()`; `ray`, `hasRay`, `hitDistance`, `hitDistanceKnown`, `storeRay`, `requiredDetailMask` |
| Triangle | `p0..p2` | `point0..point2` (índices, como `getPoint0()`) |
| MonotoneDecompositionTriangulator.Triangle | `a b c` | `point0..point2` |
| FunctionalExplicitSurface | `nx ny`; `minx…maxz`; `internalGeometry` | `tesselationHintX/Y`; `minXBound…maxZBound`; `internalTriangleMesh` |
| ParametricBiCubicPatch | `Gx_MATRIX…`, `S_MATRIX`, `Tt_MATRIX`, `M_MATRIX`, `Mt_MATRIX`, `M_Gx_Mt_MATRIX…` | `geometryMatrixX/Y/Z`, `sParameterMatrix`, `tParameterMatrix`, `s/tDerivativeParameterMatrix`, `basisMatrix`, `transposedBasisMatrix`, `coefficientMatrixX/Y/Z` |
| SimpleBodyGroup | `rotation_i` | `rotationInverse` |
| SimpleBody | `globalMaterial`, `globalTextureMap`, `globalNormalMap(Rgb)` | `material`, `texture`, `normalMap(Rgb)` |
| SimpleScene | `*Array` | `simpleBodies`, `lights`, `backgrounds`, `cameras` |
| Fixed/CubemapBackground | `backgroundImage(s)` | `image(s)` |
| TriangleMesh | `triangleIndices` | `triangleIndexes` (como `getTriangleIndexes()`) |
| PolygonTopologicalMerger | `Segment2D.a/b`, `SplitPoint.t/p`, `PointKey.qx/qy`, `EdgeKey.a/b/ka/kb` | `start/end`, `segmentParameter/point`, `quantizedX/Y`, `start/end/startKey/endKey` |
| Fallbacks del sólido | `xs ys zs` | `x/y/zCoordinates` |
| AnimationEvent | `t`, `get/setT` | `time`, `get/setTime` |
| StopWatch | `t0 t1` / `running_ start_ elapsedSeconds_` | `startTime stopTime` / `running startTime elapsedSeconds` |
| ProceduralNoise.Lattice | `ix jx sx tx…` | `lowerCellX upperCellX upperWeightX lowerWeightX…` |
| LuCpuStrategy.LuDecomposition | `lu piv` | `factors pivots` |
| HiddenLineRenderer (Appel) | `t`, `d` | `lineParameter`, `direction` (clave JSON `"t"` intacta) |
| RasterTileArea / Generator, mensaje del worker TS | `x0 y0 dx dy`; `getX0/Y0/Dx/Dy/X1/Y1` | `startX startY width height`; `getStartX/StartY/Width/Height/EndX/EndY` |
| ParallelRaytracer | `depthRangeNear/Far` | `openGlDepthRangeNear/Far` |
| ProgressMonitorConsoleLongFormat | `n` | `updateCount` |
| IndexedColorImageUncompressed | `_static_color` | `staticColor` |
| KeyEvent / WebSystem | `unicode_id` | `unicodeId` |
| CollapsablePanel | `text_` | `text` |
| Rotate/Scale/TranslateGizmo | `T` | `transformationMatrix` |
| Scale/TranslateGizmo | `elementInstances` | `elements` |
| CameraControllerOrbiter/Aquynza | `deltaMov` | `deltaMovement` |
| ViewportWindow, WidgetDialog, WidgetButtonGroup | `viewportBorder`, `children`, `commandReferenceList` | `border`, `widgetElementList`, `commands` |
| GeometryMetadata | `objectFilename`, `descriptorsList` | `filename`, `descriptors` |
| TS alineados a Java | `SimpleMaterial.phong/reflection/refraction/ior`, `RendererConfiguration.boundingColor/shading/threshold`, `RGBAImageCompressed.format/compressedSize`, `_Polygon2DContour.exterior`, `CoordinateSystem.X/Y/Z`, `ClippedLine2DResult.ok/a/b` | nombres Java (`phongExponent`…, `x/y/z`, `acceptedValue`…) |
| Lectores/escritores | `_ReaderAseTriangleCache.p0..`, `ReaderPly` `a b c`, `_WriterGtsTriangle.p0..`, `_StlFacetEmitter.Facet.a b c` | `point0..point2`; facetas `vertex0..vertex2` |
| C++ utilidades y OpenGL4 | `strategy_`, `withSystemExit_`, `message_`, `vao_`, `program_`…, `VBO_positions` | sin `_`; `positionsVbo`, `colorsVbo` |
| C++ `AlgebraicExpression` Parser | `s`, `p` | `text`, `position` |
| SimpleCorridorSample (3 ports + testsuite) | `a na b nb c nc` | `width widthTiles length lengthTiles height heightTiles` |
| Jogl2 | anaglifo `lr…rb`; temporales `p n` | `leftRed…rightBlue`; variables locales |
| Testsuites | applets `snake_case` y `get_x()`, herramientas tangibles C++ con `_`, `fx fy cx cy`, torus `N n`, iluminación `N H`, ejemplo distribuido `x0…ip`, Wii `LS t`, etc. | camelCase semántico (`focalLengthX`, `majorDivisions`, `reflection`, `cellSize`, `plotColumn`…) |

## Fase 4: verificación (2026-09-23)

- Java: `gradle compileJava compileTestJava` (todos los módulos), `gradle
  :base:test` verde; applets legados compilados con `javac` aparte; módulo
  `android` (fuera del build) revisado con grep.
- C++: árbol completo (base, opengl4, glfw, glut, testsuite incl.
  herramientas tangibles con OpenCV) sin errores; `vitral_base_tests` 24/24.
- TS: `tsc --build` de base/fs/webgl, `tsc --noEmit` de todos los proyectos
  del testsuite, specs revisados con `tsc`, `npm test`, `eslint`, `prettier`
  en los archivos tocados.
- Paridad: `RaytracingOfflineExample -parallel` byte a byte igual en los tres
  ports.
- SceneEditorApplication por MCP: raytrace idéntico al de HEAD.

## Fase 5: guardas

- `scripts/checkAttributeNames.py --all` (0 violaciones):
  - Gradle: tarea `verifyAttributeNames`, dependencia de `check`.
  - npm: `npm run check:names`, incluido en `npm run lint`.
  - CMake/CTest: test `attribute_names`.
- Tests de accessors de especificaciones de control:
  `EntityControlSpecificationsTest` (Java) y `Entity.spec.ts` (TS).

## Estado

- [x] Fases 0–5 ejecutadas en librerías y testsuites de los tres ports.
- [ ] Decisión pendiente: `Jacobian.ts`.
- [ ] Commits por lote: no se hicieron (el trabajo quedó sin commitear).
