# SceneEditorApplication

C++ port of the Vitral **scene editor**: an Xt (X11) GUI with an OpenGL
drawing area, ported from the Java `SceneEditorApplication`.

The File → Import dialog accepts 3ds, vtk, gts, obj and ply meshes, plus MIT
`.ray` scenes.  A `.ray` import replaces the complete editor scene (camera,
lights, materials and background); its perspective camera is copied into the
viewports so CPU raytracing and the interactive view use the same aspect.

## Variants

Each executable is built for one **widget set** and one **OpenGL version**:

| Variant       | Widget set    | OpenGL                          |
|---------------|---------------|---------------------------------|
| `xm_opengl4`  | Motif         | 4.1 core (default, Linux only)  |
| `xaw_opengl4` | Athena (Xaw)  | 4.1 core (Linux only)           |
| `xm_opengl1`  | Motif         | 1.2 fixed function              |
| `xaw_opengl1` | Athena (Xaw)  | 1.2 fixed function              |

| `x11_framebuffer` | Framebuffer GUI over Xlib | none (CPU rendering) |

Motif and Athena never share a process. On macOS only the `opengl1` variants
are supported: XQuartz exposes only legacy OpenGL through GLX.

Each variant builds in `build-cmake-<variant>/` and places its executable
**and its Vitral libraries** in `build/<variant>/`, so variants linked with
different X11/OpenGL stacks never overwrite each other.

## Build and run

Each launcher configures, builds and runs its fixed variant:

```sh
./runXawOpenGL1.sh -s
./runXawOpenGL4.sh -s
./runXmOpenGL1.sh -s
./runXmOpenGL4.sh -s
```

The `-s` option starts the MCP automation service on TCP port 1234.

The last button of the command bar (**Tools → Tangible interface client**)
opens the dialog to connect with a tangible interface websocket service (see
`cpp/testsuite/Tools/TangibleInterfaceMarkersDetectorServer`); the poses of
its `rayCube1` marker group move the ray gizmo, as in the Java port. The
option `-tangibleServer <url>` changes the URL the dialog proposes (by
default `ws://127.0.0.1:8090/v1/values`), in every variant, GTK4 included.

The dialogs are built from the definitions shared by every port
(`etc/gui/dialogs.json`, read by `DialogPersistence`): the tangible
interface connection form and the scene selector (`h` key in the drawing
area), whose tree selects lights and geometry objects (undoable), deletes
them with the Delete key and hides or shows them from its context menu.

## X11 framebuffer variant

```sh
./runX11framebuffer.sh [-display name] [-geometry WxH+X+Y] [-fn font] [english|spanish]
```

The `x11_framebuffer` variant runs the editor on the framebuffer GUI of the
toolkit (`cpp/base/src/main/vsdk/toolkit/gui/framebuffer`), the same one
the `sceneEditor` program of the minimal kernel draws with svgalib: menus,
tool bar, dialogs and viewports are drawn with pixels, lines, rectangles,
polygons and a core font on one X11 window, using Xlib only (the backend is
in `cpp/xlib`). Without `-geometry` the window is maximized (the largest
window the desktop allows, not full screen). The font is `10x20` unless
`-fn` names another one, and the language comes from the argument or the
`SCENE_EDITOR_GUI_LANGUAGE` variable.

The viewports are rendered in CPU (hidden lines, or raytracing chosen from
the menu of the viewport title); there is no OpenGL. At the right, the
tabbed side panel has the button groups of the GUI definition, as in the Xt
variants: creation of geometries, lights, import and export (in two
collapsible sections), GUI, scene components and render; its modify tab
shows the selected body and the transformation tools. Dialogs are modal and
drawn inside the same window: file import (also from File → Import) and
export, palettes, the raytraced image, messages and the scene selector
(`h`). The application
class (`src/application/framebuffer/FramebufferSceneEditor`) only uses the
framebuffer interfaces; only its `main` knows Xlib, so the same class can
run on the svgalib and future BGI backends. The `sceneEditor` program of
the minimal kernel (`cpp/x86_64_smp_kernel`) compiles these sources (all
but the Xlib `main`) with its own svgalib `main` and runs the whole editor,
including the parallel raytracer. It runs from
`/media/ramdisk/cpp/testsuite/ApplicationCases/SceneEditorApplication` in
its root image, so `../../../../etc` reaches the `etc` copied into the
image. Not available yet in this variant: the tangible interface client,
the MCP service and the editors of the modify panel of the other variants.

To configure and build every variant without running it:

```sh
./compileAllVariants.sh
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

2. Run the application through its launcher, which sets
   `LIBGL_ALWAYS_INDIRECT=1` on macOS:

   ```sh
   DISPLAY=:0 ./runXmOpenGL1.sh -s
   ```

The editor then reports `OpenGL version string: 1.4 (2.1 Metal ...)`, enough
for its OpenGL 1.2 pipeline.

### Homebrew libX11 locale modules

Homebrew libX11 1.8.13 looks for locale modules as `*.so.2`, but installs
them as `*.2.so`. Without them, the editor stops with `Could not create the
UTF-8 menu font set`. The `runXmOpenGL1.sh` launcher creates the missing
links in Homebrew's libX11 locale module directory. Run through the launcher
on macOS; it recreates the links after Homebrew upgrades. The same setup is
in `runXmOpenGL4.sh` for systems where that variant is supported.
