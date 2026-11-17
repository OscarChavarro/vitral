# SceneEditorApplication Automation Service (MCP)

The scene editor can expose a small MCP-like JSON-RPC service over TCP, so an
AI agent (or any script) can inspect and manipulate the editor without using
its graphical user interface. Every port of the application is expected to
offer the same service, with the same tools and the same results.

## Starting The Service

The service listens on TCP port `1234` on localhost and prints `Waiting for
MCP connections on TCP port 1234` when it is ready (a few seconds after the
start). If the port is already taken (i.e. by another instance of the
editor), the error is reported in the console and the editor keeps running
without the service.

In the Java port the service is always started, with no arguments needed (the
former `-s` argument is still accepted, and ignored):

```bash
./run.sh
```

`run.sh` runs the `runMain` Gradle task of this module, which already adds the
JVM options the Java port needs (see the root `build.gradle`). When launching
it some other way (i.e. from an IDE), copy those options too.

In the C++ port (Xt GUI and OpenGL 4 drawing area, in
`cpp/testsuite/ApplicationCases/SceneEditorApplication`):

```bash
./run.sh -s
```

It is served by `XtOpenGL4VitralEditorMCP` and
`XtOpenGL4VitralEditorMCPProtocol`, ports of the Java classes, with the same
tools, arguments, results and error messages.

## Protocol

- Each request is one JSON-RPC 2.0 message on a single line; each response is
  one JSON line. Several requests can be sent over the same connection, and
  several connections can be open at the same time.
- Supported methods: `initialize`, `tools/list` and `tools/call`.
- Tools are executed one at a time, in the thread of the user interface, so
  they never run in the middle of a user action.

A tool call:

```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"scene.describe","arguments":{}}}
```

Its result carries the JSON of the tool as text:

```json
{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\"bodies\":[],\"lights\":[]}"}],"isError":false}}
```

Errors:

- Unknown methods get a JSON-RPC error (`-32601`), and failures outside the
  tools a `-32000` one.
- Failures inside a tool (unknown tool, bad argument, index out of range...)
  are returned as a normal result whose text is `{"error":"<message>"}`.

Arguments are read leniently: numbers must be plain decimals (`-1.5`, not
`1e-3`), booleans are `true` / `false`, and missing arguments take the default
values documented below.

## Tools

### Scene

- `scene.describe`: returns the bodies and the lights (`index`, `type`,
  `position`, `emission` color). It does not report the selection. Each
  body reports:
  - `index`, `name`, `geometry` (class of the geometry, i.e. `Cone` for
    cones and cylinders) and `hidden`.
  - Its placement: `position`, `scale` and `rotation`, given as its 3x3
    `matrix` (rows) and as the `yaw`, `pitch` and `roll` angles in degrees
    (`null` if they can not be obtained).
  - The `parameters` of its geometry, named as in its constructor:
    `radius` (`Sphere`); `bottomRadius`, `topRadius`, `height` (`Cone`);
    `size`, `minimum`, `maximum` (`Box`); `majorRadius`, `minorRadius`
    (`Torus`); `baseLength`, `headLength`, `baseRadius`, `headRadius`
    (`Arrow`); `a`, `b`, `c`, `d` of a*x + b*y + c*z + d = 0
    (`InfinitePlane`); `functionExpression`, `minimum`, `maximum`,
    `tesselationHintX`, `tesselationHintY` (`FunctionalExplicitSurface`);
    `xSize`, `ySize`, `zSize`, `threshold` (`VoxelVolume`, without its
    voxels); `threshold`, `sturm` and `components` (`center`, `radius`,
    `strength`) (`BlobSolid`); `order`, `sturm` and `coefficients`, in the
    order of `PolynomialSurface.getTermIndex` (`PolynomialSurface`);
    `square`, `mixed`, `linear`, `constant` (`QuadricSurface`); `a`, `b`,
    `c`, `d` of the solid a*x + b*y + c*z + d <= 0 (`PlanarHalfSpace`);
    `columns`, `rows`, `minimum`, `maximum` (`HeightFieldSolid`, whose
    `heights` are serialized, at index x + columns*z). Geometries defined by data report only a summary: the number
    of vertices, edges and faces of a `PolyhedralBoundedSolid`, of vertices
    and triangles of each mesh, of points of a `ParametricCurve`, and the
    `type` of a `ParametricBiCubicPatch`. Other geometries report `null`.
  - `radius` for spheres, also at the level of the body, as in the first
    version of this tool.
- `scene.serialize`: like `scene.describe`, with all the data needed to
  create the bodies again: each body also reports its `material` (colors,
  coefficients, `opacity`, `doubleSided`) and the full data of the
  geometries defined by data: `vertices` (`id`, `position`) and `faces`
  (`id` and `loops`, each one the ids of its vertices in order, the outer
  loop first) of a `PolyhedralBoundedSolid`; `vertexPositions` (x, y, z of
  each vertex) and `triangleIndexes` (three per triangle) of meshes;
  `points` (`type` and `vectors`: position and tangent control points) of a
  `ParametricCurve`; `contourCurve` (Ferguson) or 4x4 `controlMeshPoints`
  (Bezier) of a `ParametricBiCubicPatch`. Spot lights also report
  `pointsAt`, `coefficient`, `radius` and `falloff`. Arguments: `index`
  (optional: only that body, and no lights).
- `scene.clear`: removes all bodies, lights and debug groups. Returns
  `{"ok":true}`.
- `scene.add_point_light`: creates a point light. Without arguments, the light
  is placed automatically inside the view volume of a visible viewport (the
  first light is white; the next ones get random light colors and positions).
  Optional arguments `x`, `y`, `z` (position) and `r`, `g`, `b` (color, default
  white) override the automatic values; missing coordinates follow the
  automatic policy of the first light. Returns the scene description.
- `scene.add_sphere`: creates a sphere. Arguments: `radius` (default 1), `x`,
  `y`, `z` (default 0). Returns the scene description.
- `scene.add_cone`: creates a cone or truncated cone. Arguments: `baseRadius`
  (default 1), `topRadius` (default 0), `height` (default 2), `x`, `y`, `z`
  (default 0). Returns the scene description.
- `scene.add_cylinder`: creates a cylinder. Arguments: `radius` (default 1),
  `height` (default 2), `x`, `y`, `z` (default 0). Returns the scene
  description.
- `scene.add_tube`: creates a tube (a washer, a circular lamina with a hole,
  if `height` is 0). Arguments: `internalRadius` (default 0.5),
  `externalRadius` (default 1), `height` (default 2), `x`, `y`, `z` (default
  0). Returns the scene description.
- `scene.move_body`: sets the position of a body. Arguments: `index` (default:
  the last body), `x`, `y`, `z` (missing coordinates are kept). Returns the
  scene description.
- `scene.select_body`: selects one body, unselecting the others. Arguments:
  `index` (a negative index, or none, clears the selection). Returns the scene
  description.

The changes done by `scene.clear`, `scene.add_*` and `scene.move_body` are
recorded in the scene history, so they can be undone as the ones of the user
(see `edit.history`).

- `gui.command`: executes a command of the user interface that works only
  over the model, as its menu item or button does: the `IDC_` commands of the
  I18N files in `etc/gui` that need no dialog, i.e. `IDC_CREATE_SPHERE`,
  `IDC_CREATE_FUNCTIONALEXPLICITSURFACE`, `IDC_CREATE_PARAMETRICBICUBICPATCH`,
  `IDC_CREATE_OMNILIGHT`, `IDC_TOOLS_RAY` or `IDC_OTHERS_CYCLE_BACKGROUND`.
  The samples of each kind of volume are created by `IDC_CREATE_SPHERE`,
  `IDC_CREATE_CONE`, `IDC_CREATE_CYLINDER`, `IDC_CREATE_CUBE`,
  `IDC_CREATE_BOX`, `IDC_CREATE_TUBE`, `IDC_CREATE_WASHER`, `IDC_CREATE_TORUS`, `IDC_CREATE_ARROW`,
  `IDC_CREATE_BLOB` (three components in a triangle with a hole, as a
  molecule), `IDC_CREATE_QUADRIC` (an ellipsoid), `IDC_CREATE_POLYNOMIAL`
  (the quartic "tangle cube", of genus 5), `IDC_CREATE_HEIGHTFIELD` (a
  terrain of 17 x 17 heights), `IDC_CREATE_HALFSPACE` (z <= 0),
  `IDC_CREATE_BREP` and `IDC_CREATE_VOLUME` (voxels of the selected body). Arguments:
  `command`. Returns `result` (`DONE`, `FAILED`, or `NOT_HANDLED` for commands
  that need the user interface, i.e. file dialogs) and the status `message`.
  Scene changes are recorded in the scene history.
- `gui.import_scene_dialog`: opens the native Swing selector used by **File →
  Import**. Files `.ray` replace the scene; `.md2` models add their first
  key frame as a triangle mesh; other mesh formats add their objects. The MCP request remains pending while the dialog is open and
  returns after the user selects a file or cancels it.
- `gui.dialog.choose_file`: on a second MCP connection, selects `path` in the
  open import chooser. An empty path cancels it. This completes the pending
  `gui.import_scene_dialog` request.
- `gui.selector.open`: opens or refreshes the modeless scene hierarchy dialog.
- `gui.selector.select_body`: selects a body through that dialog by `index`;
  `-1` clears the tree selection. The normal selection history is recorded.
- `gui.selector.action`: runs `deleteSelected` or `toggleVisibility` on the
  current tree selection.
- `gui.tangible.open`: opens the connection form without waiting for it to
  close. `gui.tangible.action` invokes `connect` (with `serviceUrl`) or
  `cancel`. `gui.dialog.state` reports the open dialog IDs and connection form
  contents, including the retry label after a failed attempt.

### Edition history

- `edit.history`: returns the undo/redo state of the global scene history
  (`scene`) and of the view history of each viewport of the active viewport
  set (`viewports`: `index`, `title`, `selected` and `history`). Each history
  reports `undo` and `redo` (number of operations) and `nextUndo` and
  `nextRedo` (names of the next operations, or `null`).

Undo and redo are done as the user does them, with `gui.key`: `z` / `y` with
`ctrl` over the scene history, and with `ctrl` and `shift` over the view
history of the selected viewport.

### User interaction

These tools act as the user: their events go through the same interaction
techniques as the real ones, so they exercise the editor end to end. The
window of the editor must exist (otherwise they fail with `The drawing area
has not been created`).

- `gui.set_mode`: sets the interaction mode. Arguments: `mode` (`camera`,
  `select`, `translate`, `rotate` or `scale`). Returns
  `{"ok":true,"mode":...}`.
- `gui.mouse`: sends a mouse event to the drawing area. Arguments: `type`
  (`move`, `press`, `drag` or `release`; default `move`), `x`, `y` (position in
  the logical pixels of the drawing area, as reported by `viewport.project`;
  default 0) and `button` (1 = left, 2 = middle, 3 = right; default 1). A
  gesture is a `press`, some `drag`s and a `release`. Returns the scene
  description.
- `gui.key`: sends a key press to the drawing area (whatever has the keyboard
  focus). Arguments: `key` (a single character, or `tab`, `enter`,
  `backspace`, `delete`, `escape`, `left`, `right`, `up`, `down`,
  `pageup`, `pagedown`, `num0`..`num9`, `num/`, `num*`, `num-`, `num+`,
  `num.`, `numenter`), `shift` and `ctrl` (default false). `shift` does not change the
  character: send `T` for an uppercase letter. No key release is sent.
  Returns the scene description.
- `viewport.project`: reports where the first selected body is seen in a
  viewport: the drawing area pixels (same coordinates as `gui.mouse`) of its
  position (`origin`) and of the tips of the unit vectors `x`, `y`, `z` from
  it, or `null` for points behind the camera. Arguments: `viewport` (index,
  default 0). Fails if no body is selected.

### Rendering

- `render.get_configuration`: returns the rendering configuration of the
  viewports: `index`, `title`, the booleans `points`, `wires`, `surfaces`,
  `texture`, `bumpMap`, `boundingVolume`, `normals`, `trianglesNormals`,
  `selectionCorners`, `grid` (reference grid of the viewport), `shading`
  (uppercase, i.e. `PHONG`) and `renderMode` (`gpu` for rasterization, `cpu`
  for raytracing, `hidden_lines` for lines computed in the processor), plus
  the `technology` presenting the viewport set (`opengl4` or `awt`).
  Arguments: `viewport` (index; default all).
- `render.set_configuration`: changes the rendering configuration of the
  viewports, only in the given values. Arguments: `viewport` (index; default
  all), any of the booleans reported by `render.get_configuration`, `shading`
  (`nolight`, `flat`, `gouraud`, `phong` or `cook_terrance`, in any case) and
  `renderMode` (`gpu`, `cpu` or `hidden_lines`; only the modes available in
  the current technology: `gpu`/`cpu` with `opengl4`, `hidden_lines`/`cpu`
  with `awt`). Returns the new configuration. These changes are not
  recorded in the view histories.
- `viewport.set_rendering`: atomically requests a canvas technology and an
  algorithm for the selected viewport (or the `viewport` argument). Arguments
  are `technology` (`opengl4` or `awt`) and `algorithm` (`gpu`,
  `hidden_lines` or `raytracing`). A request for `opengl4` rebuilds an AWT
  drawing canvas as JOGL before applying the algorithm. GPU is available only
  with OpenGL; hidden lines only with AWT; raytracing works with both.
- `render.set_technology`: presents the viewport set with other technology,
  rebuilding the GUI (wait about a second before the next call). Arguments:
  `technology` (`opengl4` or `awt`). With `awt` no OpenGL is used: viewports
  are drawn with AWT 2D operations, as lines (Appel hidden line removal for
  the solids, i.e. every volume, as all of them export a boundary
  representation, when `surfaces` is on, with hidden lines dashed when `wires` is
  also on; a plain wireframe of every body when `surfaces` is off; curves,
  the edges of the meshes the surfaces are exported to, and the reference
  grid are lines hidden by the solids, but hide nothing; an infinite plane is
  a square of 10 x 10 cells of 2 units, centered at the point of the plane
  viewed by the camera) or as
  raytraced images. The translation, rotation and
  scale gizmos and the input boxes are drawn there as 2D primitives (painter's
  algorithm, see `vsdk.toolkit.render.gizmo`); the visual debug ray is not.
- `render.raytrace_png`: raytraces the scene and exports it as a PNG. It uses
  the active camera and rendering configuration of the scene, which the
  editor sets to the ones of each viewport while drawing it: in practice, the
  last viewport drawn, which is not necessarily the selected one. Arguments:
  `path` (default `./mcp-raytrace.png`), `width` (default 640), `height`
  (default 480). It also writes
  `./output.jpg` and reports the progress in the console. Returns the absolute
  path.
- `viewport.export_jpg`: exports the selected viewport, as drawn, to a JPG.
  Arguments: `path` (default `./outputSelectedViewport.jpg`). Returns the
  absolute path.
- `workspace.export_jpg`: exports the whole drawing area, with all its
  viewports, to a JPG. Arguments: `path` (default `./outputViewport.jpg`).
  Returns the absolute path.

Exported images have the physical pixels of the display: on high density
displays they are larger than the logical pixels used by `gui.mouse` (i.e.
twice as large).

### Application

- `gui.list_languages`: lists the languages available for the user interface
  (the I18N JSON files in `etc/gui`), marking the current one.
- `gui.set_language`: changes the language of the user interface, rebuilding
  it. Arguments: `language` (an id returned by `gui.list_languages`, i.e.
  `spanish`).
- `app.exit`: closes the application. The response is sent before it ends.

## Example Session

```bash
printf '%s\n' \
'{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"scene.clear","arguments":{}}}' \
'{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scene.add_point_light","arguments":{"x":-10,"y":-9,"z":8,"r":1,"g":1,"b":1}}}' \
'{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"scene.add_sphere","arguments":{"radius":1,"x":0,"y":0,"z":0}}}' \
'{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scene.describe","arguments":{}}}' \
'{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"render.raytrace_png","arguments":{"path":"/tmp/vitral-raytrace.png","width":640,"height":480}}}' \
'{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"viewport.export_jpg","arguments":{"path":"/tmp/vitral-selected-viewport.jpg"}}}' \
'{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"workspace.export_jpg","arguments":{"path":"./outputViewport.jpg"}}}' \
'{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"gui.key","arguments":{"key":"z","ctrl":true}}}' \
'{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"edit.history","arguments":{}}}' \
| nc 127.0.0.1 1234
```

This sequence clears the scene, adds a light and a sphere, inspects the scene,
exports a raytraced PNG, the selected viewport and the whole workspace, undoes
the creation of the sphere and reports the edition history.

## Notes For Agents

- Use `scene.describe` before and after changing the scene to verify its
  state, and `edit.history` to verify what was recorded.
- The editor window appears on the real desktop and takes the keyboard focus:
  a person using the machine at the same time can move things or close the
  application (`escape`). Sample the state twice while idle before suspecting
  a bug.
- Prefer explicit positions and colors (i.e. in `scene.add_point_light`) for
  reproducible scenes.
- To compare both render modes of the same view, export the workspace after
  `render.set_configuration` with `renderMode` `gpu` and then `cpu` (wait a
  few seconds after switching to `cpu`). In CPU mode the raytracer also
  produces the depth of each pixel as the rasterizer would, so the grid, the
  gizmos and the selection corners are depth tested against the raytraced
  bodies.
- Use `render.raytrace_png` and `viewport.export_jpg` together to find
  mismatches between the raytracer and the rasterizer.
- Keep each request on a single line. If a sandbox blocks TCP access, run the
  client with local network permission.

## I18N And Standard Viewport Set Popups

Texts of the user interface come from the JSON files in `etc/gui` (one per
language). Besides the application `IDC_` commands, the framework defines
standard commands starting with `IDV_` and popup menus that are not part of
the main menubar:

- `VIEWPORT_SET_PROJECTION_LOCATION` (see
  `vsdk.toolkit.gui.viewport.ViewportSetCommands`), with the commands
  `IDV_VIEWPORT_SET_PROJECTION_LOCATION_{PERSPECTIVE,TOP,BOTTOM,LEFT,FRONT}`.
  The viewport set uses the item texts of this popup as the names of its
  viewports, and the application gives it the GUI definition each time it is
  loaded, so names follow the language selected by the user.
- `VIEWPORT_SET_RENDER_MODE`, with the commands
  `IDV_VIEWPORT_SET_RENDER_MODE_{GPU,CPU}`, shown after the former in the menu
  of the title of a viewport.
