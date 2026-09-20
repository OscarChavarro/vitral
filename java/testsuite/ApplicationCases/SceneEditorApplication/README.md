# SceneEditorApplication MCP Notes

This application can expose a small MCP-like JSON-RPC service over TCP so an AI
agent can inspect and manipulate the scene without using the Swing UI directly.

## Start The MCP Server

Run the application with the `-s` argument:

```bash
./gradlew --quiet :testsuite:ApplicationCases:SceneEditorApplication:runMain \
  -PrunMainClass=application.SceneEditorApplication \
  -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms300m|-Xmx300m' \
  -PrunArgs='-s'
```

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
- `render.raytrace_png`: raytraces the current scene and exports a PNG. Arguments: `path`, `width`, `height`.
- `viewport.export_png`: exports the current JOGL viewport color buffer as a PNG. Arguments: `path`.

## Example Agent Session

```bash
printf '%s\n' \
'{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"scene.clear","arguments":{}}}' \
'{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"scene.add_point_light","arguments":{"x":-10,"y":-9,"z":8,"r":1,"g":1,"b":1}}}' \
'{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"scene.add_sphere","arguments":{"radius":1,"x":0,"y":0,"z":0}}}' \
'{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"scene.describe","arguments":{}}}' \
'{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"render.raytrace_png","arguments":{"path":"/tmp/vitral-raytrace.png","width":640,"height":480}}}' \
'{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"viewport.export_png","arguments":{"path":"/tmp/vitral-viewport.png"}}}' \
| nc 127.0.0.1 1234
```

This sequence clears the scene, adds a light and a sphere, inspects the scene,
exports a raytraced PNG, and exports the current JOGL viewport PNG.

## Notes For Agents

- Use `scene.describe` before and after mutating the scene to verify state.
- Use `render.raytrace_png` and `viewport.export_png` together when debugging
  mismatches between the raytracer and the JOGL renderer.
- The TCP service is intentionally line-oriented; keep each JSON-RPC request on
  a single line.
- If the local shell blocks TCP access from a sandbox, run the client command
  with the required local-network permission.
