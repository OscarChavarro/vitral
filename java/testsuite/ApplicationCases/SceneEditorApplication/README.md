# SceneEditorApplication MCP Notes

This application can expose a small MCP-like JSON-RPC service over TCP so an AI
agent can inspect and manipulate the scene without using the Swing UI directly.

## Start The MCP Server

Run the application with the `-s` argument:

```bash
./gradlew --quiet :testsuite:ApplicationCases:SceneEditorApplication:runMain \
  -PrunMainClass=application.AwtJogl4SceneEditorApplication \
  -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms300m|-Xmx300m' \
  -PrunArgs='-s'
```

`runMain` already adds the JVM option `--add-exports=java.desktop/sun.awt=ALL-UNNAMED`
(see the root `build.gradle`). JOGL needs it on JDK 16+, otherwise it prints
`Caught AppContextInfo(Bug 1004) InaccessibleObjectException...` with a stack
trace each time the OpenGL canvas is created. If you launch the application
some other way (i.e. from an IDE), add that option to the JVM arguments.

The server listens on TCP port `1234` on localhost. Each request is one JSON-RPC
message per line, and each response is returned as one JSON line.

## Protocol

Supported JSON-RPC methods:

- `initialize`
- `tools/list`
- `tools/call`

For tool calls, send:

```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"scene.describe","arguments":{}}}
```

## Tools

- `scene.describe`: returns bodies, lights, positions, scale, geometry type and sphere radius as JSON.
- `scene.clear`: removes all bodies, lights and debug groups.
- `scene.add_point_light`: creates a point light. Arguments: `x`, `y`, `z`, `r`, `g`, `b`.
- `scene.add_sphere`: creates a sphere. Arguments: `radius`, `x`, `y`, `z`.
- `scene.add_cone`: creates a cone or truncated cone. Arguments: `baseRadius`, `topRadius`, `height`, `x`, `y`, `z`.
- `scene.add_cylinder`: creates a cylinder. Arguments: `radius`, `height`, `x`, `y`, `z`.
- `scene.move_body`: sets the position of a body (default: the last one); missing coordinates are kept. Arguments: `index`, `x`, `y`, `z`.
- `scene.select_body`: selects one body (a negative `index` clears the selection). Arguments: `index`.
- `gui.set_mode`: sets the interaction mode. Arguments: `mode` (`camera`, `select`, `translate`, `rotate` or `scale`).
- `gui.key`: injects a key press into the canvas. Arguments: `key` (a single character, or `tab`, `enter`, `backspace`, `escape`, `left`, `right`, `up`, `down`, `pageup`, `pagedown`) and `shift` (default false).
- `render.get_configuration`: returns the `RendererConfiguration` flags of the viewports. Arguments: `viewport` (index; default all).
- `render.set_configuration`: sets the `RendererConfiguration` flags bit by bit. Arguments: `viewport` (index; default all) and any of the booleans `points`, `wires`, `surfaces`, `texture`, `bumpMap`, `boundingVolume`, `normals`, `trianglesNormals`, `selectionCorners`, and `shading` (`nolight`, `flat`, `gouraud`, `phong`, `cook_terrance`).
- `render.raytrace_png`: raytraces the current scene and exports a PNG. Arguments: `path`, `width`, `height`.
- `viewport.export_jpg`: exports the selected JOGL4 viewport as a JPG. Arguments: `path`.
- `workspace.export_jpg`: exports the complete JOGL4 workspace area, including all viewports, as a JPG. Arguments: `path`.
- `gui.list_languages`: lists the languages available for the GUI (the I18N JSON files in `etc/gui`) and marks the current one.
- `gui.set_language`: changes the GUI language, rebuilding the GUI. Arguments: `language` (an id returned by `gui.list_languages`, i.e. `spanish`).
- `app.exit`: closes the application (the response is sent before it ends).

## Example Agent Session

```bash
printf '%s\n' \
'{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"scene.clear","arguments":{}}}' \
'{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scene.add_point_light","arguments":{"x":-10,"y":-9,"z":8,"r":1,"g":1,"b":1}}}' \
'{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"scene.add_sphere","arguments":{"radius":1,"x":0,"y":0,"z":0}}}' \
'{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scene.describe","arguments":{}}}' \
'{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"render.raytrace_png","arguments":{"path":"/tmp/vitral-raytrace.png","width":640,"height":480}}}' \
'{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"viewport.export_jpg","arguments":{"path":"/tmp/vitral-selected-viewport.jpg"}}}' \
'{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"workspace.export_jpg","arguments":{"path":"./outputViewport.jpg"}}}' \
| nc 127.0.0.1 1234
```

This sequence clears the scene, adds a light and a sphere, inspects the scene,
exports a raytraced PNG, exports the selected JOGL4 viewport as a JPG, and exports
the complete JOGL4 workspace as `outputViewport.jpg`.

## Notes For Agents

- Use `scene.describe` before and after mutating the scene to verify state.
- Use `render.raytrace_png` and `viewport.export_jpg` together when debugging
  mismatches between the raytracer and the JOGL renderer.
- The TCP service is intentionally line-oriented; keep each JSON-RPC request on
  a single line.
- If the local shell blocks TCP access from a sandbox, run the client command
  with the required local-network permission.

## I18N And Standard Viewport Set Popups

GUI texts come from the JSON files in `etc/gui` (one per language). Besides the
application `IDC_` commands, the framework defines standard commands starting
with `IDV_` and popup menus that are not part of the main menubar. The first one
is the `VIEWPORT_SET_PROJECTION_LOCATION` popup (see
`vsdk.toolkit.gui.viewport.ViewportSetCommands`), with the commands
`IDV_VIEWPORT_SET_PROJECTION_LOCATION_{PERSPECTIVE,TOP,BOTTOM,LEFT,FRONT}`. The
`ViewportSet` uses the item texts of that popup as the names of its viewports,
and the application injects the GUI definition (`ApplicationModel.setI18nContext`)
each time it is loaded, so names follow the language selected by the user.
