# Vitral TypeScript packages

Build and test tooling requires Node.js 22 or newer (the version enforced by
`package.json`). Install the pinned dependencies with `npm ci` and run
`./scripts/compile.sh` from this directory or any other directory.

`@vitral/base` is platform-neutral and contains no Node filesystem imports.
Browser parallel work uses the Web Worker protocol exported by that package.
`@vitral/fs` is a separate Node/server-only adapter package for local files;
do not install or bundle it in browser applications.
