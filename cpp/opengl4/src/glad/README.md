# glad OpenGL loader

Loader of the OpenGL functions used by `vitral_opengl4` and the applications
built over it (it replaces GLEW): OpenGL 4.1 core profile, the version every
port of the toolkit requests (and the highest one on macOS), plus the
`GL_EXT_texture_compression_s3tc` extension.

Generated with [glad 2](https://github.com/Dav1dde/glad) 2.0.8, from the
specifications bundled with it:

```bash
pip install --target /tmp/glad glad2==2.0.8
PYTHONPATH=/tmp/glad python3 -m glad --reproducible --api gl:core=4.1 \
    --extensions GL_EXT_texture_compression_s3tc --out-path . c
```

Applications load it once their OpenGL context is current, with the
function lookup of their windowing system (see `OpenGL4Loader`).

License: the generated files are under (WTFPL OR CC0-1.0) AND Apache-2.0
(the latter, for `KHR/khrplatform.h` and the Khronos specifications).
