# SceneEditorApplication

Legacy C++98 (turboc) port of the Vitral **scene editor**: an Xt (X11) GUI
with an OpenGL 1.2 drawing area, ported from the Java
`SceneEditorApplication` through the cpp port. It builds with old toolchains
(i.e. Ubuntu 7.04: CMake 2.4.6, g++ 4.1.2, Mesa 6.5.2, Motif 2.2.3, libXaw7
1.0.3).

The File → Import dialog accepts 3ds, vtk, gts, obj and ply meshes, plus MIT
`.ray` scenes.  A `.ray` import replaces the complete editor scene (camera,
lights, materials and background); its perspective camera is copied into the
viewports so CPU raytracing and the interactive view use the same aspect.

## Variants

Unlike the cpp port, the turboc port has no OpenGL 4.1 nor GTK variants:
each executable is built for one **widget set**, always with the OpenGL 1.2
fixed function pipeline (`vitral_opengl1`):

| Variant       | Widget set    | OpenGL                          |
|---------------|---------------|---------------------------------|
| `xm_opengl1`  | Motif         | 1.2 fixed function (default)    |
| `xaw_opengl1` | Athena (Xaw)  | 1.2 fixed function              |

Motif and Athena never share a process. Each variant builds in
`buildtc/<variant>/` and places its executable in `build/<variant>/`; it
links the libraries of the turboc modules (`base`, `opengl1`, `xt` and
`xaw` or `xm`, built by `scripts/compile.sh`) from their `build/` folders.

## Build and run

Each launcher builds the libraries if they are missing, configures and
builds its fixed variant, and runs it from this folder (the resources are
read from `etc/` of the Vitral repository, `../../../../etc`):

```sh
./runXawOpenGL1.sh -s
./runXmOpenGL1.sh -s
./run.sh -s          # SCENE_EDITOR_VARIANT=xm_opengl1 (default) or xaw_opengl1
```

The `-s` option starts the MCP automation service on TCP port 1234.

The last button of the command bar (**Tools → Tangible interface client**)
opens the dialog to connect with a tangible interface websocket service (see
`cpp/testsuite/Tools/TangibleInterfaceMarkersDetectorServer`); the poses of
its `rayCube1` marker group move the ray gizmo, as in the Java port. The
option `-tangibleServer <url>` changes the URL the dialog proposes (by
default `ws://127.0.0.1:8090/v1/values`), in every variant.

The dialogs are built from the definitions shared by every port
(`etc/gui/dialogs.json`, read by `DialogPersistence`): the tangible
interface connection form and the scene selector (`h` key in the drawing
area), whose tree selects lights and geometry objects (undoable), deletes
them with the Delete key and hides or shows them from its context menu.

To configure and build every variant without running it:

```sh
./compileAllVariants.sh
```

`scripts/clean.sh` of the turboc port removes the `build/` and `buildtc/`
folders of the application too.

## C++98 port notes

- Callbacks are objects
  implementing Java style interfaces (`Runnable`, `Consumer`, `Function`,
  `Supplier` of `java/util/function`, `XtDialogHost::MessageSource` and
  `ActionListener`, `XtTreeView::ContextMenuListener`): small named classes
  next to their users. Callbacks run from other threads (tangible interface
  client, MCP service) post `Runnable` tasks to `XtEventQueue`.
- `SharedPointer` owns `RenderPrimitive` instances; enumerations use
  wrapper classes keeping the `Type::VALUE` syntax.
- The `String` and `Object` types of Xt are renamed `XtString`/`XtObject`
  around the includes of X toolkit headers (see `XtIntrinsics.h` of the
  `xt` module), as the vitral `String` and `Object` classes are global.
- macOS (XQuartz) notes of the cpp port do not apply: this port targets
  legacy X11 systems.
