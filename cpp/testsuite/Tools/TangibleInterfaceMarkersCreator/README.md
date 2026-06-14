# TangibleInterfaceMarkersCreator

A command-line tool that generates **printable PDF sheets of AprilTag fiducial
markers** (family `tag36h11`). Each marker is drawn inside a red square frame and
the markers are tiled, centered, over an A4 page.

This program is part of the **Vitral tangible interfaces** implementation. Its
purpose is to produce the *physical* markers that are then attached to tangible
gizmos (cubes, handles, props). Those gizmos let users manipulate **rays,
objects, cameras and lights** inside other programs of the Vitral architecture by
moving real-world objects instead of using a mouse or keyboard. The companion
program [`TangibleInterfaceMarkersDetectorServer`](../TangibleInterfaceMarkersDetectorServer)
detects these printed markers with a camera and streams their pose to those
programs.

## What it produces

- An A4 PDF (210 × 297 mm) with as many markers as fit, given the requested size.
- Each marker is a red square frame ("cut/assembly outline") with the AprilTag
  printed inside it. The tag occupies `35/40` of the frame, matching the
  reference design.
- Page margins are honored so the sheet is never printed edge to edge:
  **top 40 mm, left/right/bottom 30 mm**. The grid is centered inside that
  printable area.

When the marker size changes, spacing between markers, the inner tag and the
frame stroke all scale proportionally, so the layout looks the same at any scale.

## Usage

```sh
tangibleInterfaceMarkersCreator [-size SIZE] [-start ID] [OUTPUT.pdf]
```

### Options

| Option        | Description                                                                                 | Default            |
|---------------|---------------------------------------------------------------------------------------------|--------------------|
| `-size SIZE`  | Side length of the red marker frame. Accepts `40mm` or plain `40` (interpreted as mm).      | `40mm`             |
| `-start ID`   | First AprilTag id to emit. Subsequent markers use consecutive ids (valid range `0`–`586`).  | `0`                |
| `OUTPUT.pdf`  | Positional output path. If omitted, a name like `markers_<start>_<end>_a4.pdf` is used.     | auto-generated     |

### Behavior

- **Layout is computed to fill the printable area maximally.** For example,
  `-size 40mm` yields a 3 × 5 grid (15 markers) and `-size 20mm` yields a
  6 × 10 grid (60 markers).
- If the requested size does not fit even a single marker within the margins
  (e.g. `-size 160mm`), the program prints an English error message and
  generates nothing.
- If `-start` is close to the end of the family, the page is filled only up to
  the last valid id (`586`).

### Examples

```sh
# Default 40mm markers starting at id 0
tangibleInterfaceMarkersCreator cube_faces.pdf

# 20mm markers starting at id 100
tangibleInterfaceMarkersCreator -size 20mm -start 100 small_markers.pdf

# One large marker per page
tangibleInterfaceMarkersCreator -size 150mm big_marker.pdf
```

## Building and running

The whole project is built with the helper scripts at the repository root:

```sh
./scripts/compile.sh
```

This produces `build/tangibleInterfaceMarkersCreator`. You can run it directly or
through the local wrapper:

```sh
./run.sh -size 40mm out.pdf
```

### Dependencies

- [Cairo](https://www.cairographics.org/) for PDF rendering
- The `apriltag` library (for the `tag36h11` family codes)
- `pkg-config`

On macOS: `brew install pkg-config cairo apriltag`.
On Debian/Ubuntu: `sudo apt-get install -y pkg-config libcairo2-dev libapriltag-dev`.

## Relationship with the detector

The physical side length you print here must match the `physicalSideLength`
declared for the corresponding marker group in the detector's configuration
(`etc/markerGroups/*.json`). That value is what lets the detector recover correct
metric (meters) coordinates from the camera image.
