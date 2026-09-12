# Vitral Web Testsuite Container

Frontend Angular para alojar progresivamente los ejemplos gráficos de Vitral.
El panel izquierdo contiene el árbol de navegación y el panel derecho reserva el
`DIV` en el que se crearán los futuros contextos Canvas, WebGL o WebGPU.

## Estructura de los ejemplos

Cada programa de `java/testsuite/Jogl4Examples` se migra como un módulo Angular
dentro de esta aplicación, en `src/WebGLExamples/<NombreDelProyectoJava>/`, y se
abre desde el árbol del explorador. El módulo posee un único `<canvas>`: `init`
corresponde a `ngAfterViewInit`, `reshape` a un `ResizeObserver`, `display` al
método de cuadro y `dispose` a `ngOnDestroy`; `KEY_ESC` y el cierre de la
ventana Java corresponden a la salida `deactivate`, que regresa al explorador.

Los recursos de `etc/` (shaders GLSL, imágenes, texturas) se publican en
`public/etc` mediante `scripts/sync-etc.sh`, de modo que donde Java usa una ruta
de archivo relativa el módulo usa una URL. Todo el dibujo pasa por las clases
`vsdk.toolkit.render.webgl` de `@vitral/webgl`; un cuadro nunca debe esperar una
lectura de red, por lo que los recursos de GPU se preparan antes con
`prepare(gl)`. La estrategia completa está documentada en
`doc/typescriptPortPlan.md`, sección «WebGL Example Programs».

Módulos migrados:

- `CameraExample`
- `ImageExample`

## Requisitos

- Node.js 22 o posterior
- npm 11 o posterior
- El paquete local `../../base` compilado

## Ejecutar

```bash
./run.sh
```

El script instala las dependencias cuando es necesario y levanta el servidor de
desarrollo en `http://localhost:4200`. Se pueden pasar opciones adicionales a
Angular, por ejemplo:

```bash
./run.sh --port 4300
```

## Verificar

```bash
npm run build
npm test -- --watch=false
```

`@vitral/base` se declara como dependencia local mediante `file:../../base`.
