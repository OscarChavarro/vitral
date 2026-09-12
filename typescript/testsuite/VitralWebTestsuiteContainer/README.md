# Vitral Web Testsuite Container

Frontend Angular para alojar progresivamente los ejemplos gráficos de Vitral.
El panel izquierdo contiene el árbol de navegación y el panel derecho reserva el
`DIV` en el que se crearán los futuros contextos Canvas, WebGL o WebGPU.

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
