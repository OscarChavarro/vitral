# SceneEditorApplication

C++ port of the Vitral **scene editor**: an Xt (X11) GUI with an OpenGL
drawing area, ported from the Java `SceneEditorApplication`.

## Variants

Each executable is built for one **widget set** and one **OpenGL version**,
chosen with `SCENE_EDITOR_VARIANT`:

| Variant       | Widget set    | OpenGL                          |
|---------------|---------------|---------------------------------|
| `xm_opengl4`  | Motif         | 4.1 core (default, Linux only)  |
| `xaw_opengl4` | Athena (Xaw)  | 4.1 core (Linux only)           |
| `xm_opengl1`  | Motif         | 1.2 fixed function              |
| `xaw_opengl1` | Athena (Xaw)  | 1.2 fixed function              |

Motif and Athena never share a process. On macOS only the `opengl1` variants
are supported: XQuartz exposes only legacy OpenGL through GLX.

Each variant builds in `build-cmake-<variant>/` and places its executable
**and its Vitral libraries** in `build/<variant>/`, so variants linked with
different X11/OpenGL stacks never overwrite each other.

## Build and run

`run.sh` configures, builds and runs one variant:

```sh
./run.sh -s                                   # xm_opengl4
SCENE_EDITOR_VARIANT=xaw_opengl1 ./run.sh -s
SCENE_EDITOR_VARIANT=xm_opengl1 ./run.sh -s
```

The `-s` option starts the MCP automation service on TCP port 1234.

To build only:

```sh
mkdir -p build-cmake-xaw_opengl1 && cd build-cmake-xaw_opengl1
cmake -DSCENE_EDITOR_VARIANT=xaw_opengl1 ..
cmake --build .
```

## macOS (XQuartz)

On macOS the application runs as an X11 client of
[XQuartz](https://www.xquartz.org). The whole X11 stack of an executable
(libX11, libXt, widget set, GLX `libGL` and `libGLU`) must come from **one**
place, as mixing them loads two libXt/libX11 in the process (errors such as
`BadMatch` or `Couldn't find per display information`). `CMakeLists.txt`
chooses it by widget set:

- **Athena (`xaw_*`)**: XQuartz, in `/opt/X11` (Homebrew has no libXaw).
  OpenGL is rendered by the Apple GPU through the GLX of XQuartz.
- **Motif (`xm_*`)**: Homebrew, in `/opt/homebrew` (XQuartz has no Motif):

  ```sh
  brew install openmotif libx11 libxt mesa mesa-glu
  ```

### Running the Motif variants: indirect GLX

The Mesa of Homebrew renders in software over MIT-SHM, which fails with
XQuartz (`BadShmSeg` on `X_ShmPutImage`). The Motif variants must instead
send their OpenGL commands to the XQuartz server (**indirect GLX**), which
renders them with the Apple GPU:

1. Enable indirect GLX in XQuartz, once, and **restart XQuartz** (quit it
   with ⌘Q and open it again):

   ```sh
   defaults write org.xquartz.X11 enable_iglx -bool true
   defaults read org.xquartz.X11 enable_iglx    # must print 1
   ```

   Without it, creating the context fails with `BadValue` on
   `X_GLXCreateNewContext`.

2. Run the application with `LIBGL_ALWAYS_INDIRECT=1`:

   ```sh
   DISPLAY=:0 LIBGL_ALWAYS_INDIRECT=1 ./build/xm_opengl1/SceneEditorApplication -s
   ```

   `run.sh` sets it automatically for the `xm_*` variants on macOS.

The editor then reports `OpenGL version string: 1.4 (2.1 Metal ...)`, enough
for its OpenGL 1.2 pipeline.

### Homebrew libX11 locale modules

The libX11 of Homebrew (checked with 1.8.13) looks for its locale modules as
`*.so.2`, but installs them as `*.2.so`. It then supports no locale, and the
editor stops with `Could not create the UTF-8 menu font set`. Create the
links with the expected names (again after each `brew upgrade libx11`):

```sh
cd "$(brew --prefix libx11)/lib/X11/locale/common"
for f in *.2.so; do ln -sf "$f" "${f%.2.so}.so.2"; done
```
