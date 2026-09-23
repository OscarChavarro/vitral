# SceneEditorApplication

Interactive 3D scene editor built on the vitral toolkit. It is meant to be
ported to other languages and technologies, so its features are described
here independently of any implementation.

## Features

### Viewports

- Several viewports over the same scene, in a standard arrangement of four
  (Top, Front, Left and Perspective); viewports can be added and removed.
- Selection of the viewport to work with (`;` cycles them), several layout
  styles (`,`) and maximization of the selected viewport (`Alt+w`).
- Projection of each viewport chosen from the menu of its title or with keys:
  perspective (`p`) or parallel Top (`t`), Bottom (`b`), Left (`l`) and Front
  (`f`).
- Per viewport display settings: reference grid (`g`), points, wires,
  surfaces, bounding volumes, normals, textures and bump maps (`F1`..`F9`),
  and shading model (none, flat, Gouraud, Phong, Cook-Torrance).
- Per viewport render mode: GPU rasterization or CPU raytracing (`.`), with
  the grid, gizmos and selection marks correctly occluded by the raytraced
  objects.
- Camera control with the mouse (rotate, pan, advance) and the keyboard.

### Selection and transformation

- Interaction modes: camera (`c`), selection (`q`), translation (`w`),
  rotation (`e`) and scale (`r`).
- Selection by clicking objects and lights (with `Ctrl` to add to the
  selection), sequential selection with the arrow keys, and an object
  selector dialog (`h`).
- Translation, rotation and scale gizmos, with numeric input boxes to type
  exact coordinates, angles and scale factors. Groups of selected things
  are moved together.
- Deletion of the selected things (`Delete`).

### Undo and redo

- Global history of the scene: creation, deletion and transformation of
  objects, lights and cameras (`Ctrl+Z` undo, `Ctrl+Y` redo).
- One history per viewport for its view: camera movements, projection
  changes and display settings (`Ctrl+Shift+Z` undo, `Ctrl+Shift+Y` redo, over
  the selected viewport).
- A whole mouse gesture (i.e. a drag of a gizmo) is a single step.

### Scene content

- Primitive objects: sphere, cone, cylinder, cube, box, torus, arrow and
  infinite plane.
- Advanced geometry: boundary representation solid, parametric cubic curve,
  bicubic patch, explicit functional surface (editable formula) and voxel
  volume from the selected object.
- Point lights, placed automatically where the camera sees them.
- Sample texture and bump map on the selected object (`T`, `B`).
- Backgrounds: plain color, fixed image and cube map.

### Files and images

- Import of objects from 3ds, vtk, gts, obj and ply files; export to obj, gts
  and vtk.
- Raytraced image of the scene (`F10`).
- Color and depth images of the rasterized view, with selectable palettes,
  and contour images.

### Debugging tools

- Visual debug ray, showing the path of a ray traced through the scene.
- Projected views debugging, spherical harmonics debug spheres over voxel
  volumes, a test corridor and a scene report on the console.

### User interface

- Languages: English and Spanish, switchable at run time.
- Full screen mode (`Ctrl+Shift+F`).
- Automation service for AI agents and scripts: see [MCP.md](MCP.md).
