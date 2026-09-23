# SceneEditorApplication Automation Service (MCP)

The scene editor can expose a small MCP-like JSON-RPC service over TCP, so an
AI agent (or any script) can inspect and manipulate the editor without using
its graphical user interface. Every port of the application is expected to
offer the same service, with the same tools and the same results.

## Starting The Service

Start the application with the `-s` argument. The service listens on TCP port
`1234` on localhost and prints `Waiting for MCP connections on TCP port 1234`
when it is ready (a few seconds after the start).

In the Java port:

```bash
./run.sh -s
```

`run.sh` passes `-s` to the `runMain` Gradle task of this module, which already
adds the JVM options the Java port needs (see the root `build.gradle`). When
launching it some other way (i.e. from an IDE), copy those options too.

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

- `scene.describe`: returns the bodies (`index`, `name`, `geometry` type,
  `position`, `scale`, and `radius` for spheres) and the lights (`index`,
  `type`, `position`, `emission` color). It does not report the selection.
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
- `scene.move_body`: sets the position of a body. Arguments: `index` (default:
  the last body), `x`, `y`, `z` (missing coordinates are kept). Returns the
  scene description.
- `scene.select_body`: selects one body, unselecting the others. Arguments:
  `index` (a negative index, or none, clears the selection). Returns the scene
  description.

The changes done by `scene.clear`, `scene.add_*` and `scene.move_body` are
recorded in the scene history, so they can be undone as the ones of the user
(see `edit.history`).

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
  `backspace`, `escape`, `left`, `right`, `up`, `down`, `pageup`,
  `pagedown`), `shift` and `ctrl` (default false). `shift` does not change the
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
  for raytracing). Arguments: `viewport` (index; default all).
- `render.set_configuration`: changes the rendering configuration of the
  viewports, only in the given values. Arguments: `viewport` (index; default
  all), any of the booleans reported by `render.get_configuration`, `shading`
  (`nolight`, `flat`, `gouraud`, `phong` or `cook_terrance`, in any case) and
  `renderMode` (`gpu` or `cpu`). Returns the new configuration. These changes are not
  recorded in the view histories.
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
