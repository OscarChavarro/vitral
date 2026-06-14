# TangibleInterfaceMarkersDetectorServer

A server that **detects AprilTag fiducial markers from a camera**, estimates the
6-DoF pose (position + orientation) of the **marker groups** it knows about, and
**streams those poses to clients over WebSocket**.

This program is part of the **Vitral tangible interfaces** implementation. The
markers it detects are printed by the companion tool
[`TangibleInterfaceMarkersCreator`](../TangibleInterfaceMarkersCreator) and
attached to tangible gizmos (for example, a small cube). By tracking those
physical gizmos, this server lets other programs of the Vitral architecture
manipulate **rays, objects, cameras and lights** through real-world manipulation
instead of mouse/keyboard input: moving or rotating a gizmo moves or rotates the
associated entity in the scene.

## How it works

1. The camera stream is processed with the `tag36h11` AprilTag detector.
2. Each detected tag's pose in meters is estimated using the **physical side
   length** of the marker (see `physicalSideLength` below).
3. Markers are grouped into rigid **marker groups** (e.g. the six faces of a
   cube). The poses of all visible markers of a group are fused into a single
   stable group pose.
4. The resulting group poses are broadcast to connected WebSocket clients.

### Marker groups

Marker groups are loaded from `etc/markerGroups/*.json` (relative to the binary,
at `../../../etc/markerGroups`). Each file describes one rigid group:

```json
{
  "label": "rayCube",
  "color": [1.0, 0.0, 0.0],
  "physicalSideLength": 0.035,
  "markers": [
    { "id": 0, "position": [0.0, 0.02, 0.0], "yawPitchRoll": [270.0, 180.0, 0.0] },
    { "id": 1, "position": [0.0, 0.0, 0.02], "yawPitchRoll": [270.0,  90.0, 0.0] }
  ]
}
```

- `label` — group name, sent to clients.
- `color` — RGB color (0..1) used for preview overlays only.
- `physicalSideLength` — the printed side length of the (square) markers, in
  **meters**. This must match what was printed with the creator tool; it is what
  makes the recovered coordinates metrically correct. It is **internal** to the
  marker model and is **not** sent to clients.
- `markers` — each marker's `id`, its `position` (meters) in the group frame and
  its orientation as `yawPitchRoll` (degrees).

## Usage

```sh
tangibleInterfaceServer [options]
```

### Options

| Option                  | Description                                            | Default      |
|-------------------------|--------------------------------------------------------|--------------|
| `-p`, `--port PORT`     | WebSocket server port.                                 | `8090`       |
| `-cam`, `--camera INDEX`| Camera index.                                          | `0`          |
| `--marker-size SIZE`    | Fallback AprilTag side length (m) for markers that do not belong to any group; groups use their own `physicalSideLength`. | `0.035` |
| `--calib FILE`          | Camera calibration file (OpenCV `FileStorage` or plain `fx fy cx cy k1 k2 p1 p2 k3`). | — |
| `--margin THRESHOLD`    | Minimum AprilTag decision margin to accept a detection.| `30.0`       |
| `--view-cos THRESHOLD`  | Minimum view-angle cosine to accept a detection.       | `0.5`        |
| `--hz RATE`             | WebSocket stream rate, in Hz.                          | `30`         |
| `--debug`               | Enable debug mode.                                     | off          |
| `--debug-dir DIR`       | Debug output directory.                                | `debug`      |
| `-preview`              | Open a window showing the camera with detected markers and group gizmos. | off |
| `-list`                 | List available cameras and exit.                       | —            |
| `-h`, `--help`          | Show help and exit.                                    | —            |

### WebSocket output

Clients connect to `ws://<host>:<port>/v1/values`. On each update the server
sends a JSON array of the currently visible groups:

```json
[
  {
    "label": "rayCube",
    "position": [0.1234, -0.0456, 0.5678],
    "quaternion": [0.9999, 0.0012, -0.0034, 0.0056]
  }
]
```

- `position` — group origin in camera coordinates, in **meters**.
- `quaternion` — group orientation as `[w, x, y, z]`.

You can test the stream with [`websocat`](https://github.com/vi/websocat):

```sh
websocat ws://localhost:8090/v1/values
```

### Preview mode controls

When started with `-preview`, an OpenCV window shows the live camera feed:

- `space` — toggle between *single marker* and *marker group* overlay modes.
- `q` or `ESC` — quit.
- `1` / `2` / `3` / `4` — orientation/marker test helpers used while authoring
  marker-group geometry.

## Building and running

Build the whole project from the repository root:

```sh
./scripts/compile.sh
```

This produces `build/tangibleInterfaceServer`. Run it directly or via the local
wrapper:

```sh
./run.sh -cam 3 -preview
```

### Dependencies

- [OpenCV](https://opencv.org/) for camera capture and preview
- The `apriltag` library (`tag36h11` family)
- POSIX threads

On macOS: `brew install opencv apriltag`.

## Relationship with the creator

Print your markers with
[`TangibleInterfaceMarkersCreator`](../TangibleInterfaceMarkersCreator) and make
sure the printed size matches the `physicalSideLength` of the corresponding
marker group here, so that the poses streamed to Vitral clients are in correct
real-world units.
