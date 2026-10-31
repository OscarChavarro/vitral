# Framebuffer GUI

This package defines the technology-independent foundation for drawing and interacting with Vitral graphical interfaces on a framebuffer. It connects the existing GUI descriptions and widget model to simple graphics devices, in the spirit of the original DOS jedimenu program, while keeping rendering and input independent of any particular graphics library.

GUI descriptions are read into Vitral's in-memory widget structures: `FramebufferMenuReader` builds the `WidgetMenu` tree of a GUI JSON document (such as `etc/gui/english.json`) with the Jackson-compatible `ObjectMapper`. The `Widget` owns that tree. Applications remain responsible for responding to commands; rendering a menu and choosing an item never runs a command.

## Contracts

- `Framebuffer` is a drawing surface: pixels, lines, filled and outlined rectangles and UTF-8 text in a fixed width bitmap font. Coordinates are pixels from the top-left corner and rectangles have a width and height. Drawing is clipped to a clip rectangle, which is the whole surface unless `setClip()` narrows it. Colors are packed `0xRRGGBB` values; backends map them exactly in 16 and 24 bits per pixel, through palette entries in 4 and 8 bits per pixel, and to black or white in 1 bit per pixel. Native pixel values, used to save and restore areas, preserve palette indexes and ignore the clip. `beginDrawing()` and `endDrawing()` bracket drawing so that a backend can remove overlays such as the pointer.
- `FramebufferInput` delivers `FramebufferEvent`s that carry Vitral's `KeyEvent` and `MouseEvent`, already translated from the native devices.
- `FramebufferTheme` holds the colors and shadow thickness. Surfaces with more than one bit per pixel get the DECwindows look of OSF/1 (tan Motif faces with three-dimensional shadows on a slate blue desktop); monochrome surfaces get a white background with black lines and text, and show the armed menu entry in inverse video. `colors()` lists the theme's colors so that palette backends can reserve their entries first.
- `FramebufferPainter` draws the Motif primitives: bevels, panels, etched separators, labels with underlined mnemonics and etched disabled text, cascade arrows and check boxes.
- `FramebufferWindowFrame` draws the Motif window manager decoration (resize border, window menu, title, minimize and maximize buttons) and reports the client area. The buttons have no behavior.
- `FramebufferMenuBar` lays out, draws and drives a menu bar from a `WidgetMenu` tree: wrapping rows, the last menu at the right end as Motif's help cascade, nested pull-down menus that continue in more columns when taller than the screen, separators, accelerators (shown, not bound), check marks and grayed items. It takes F10, arrows, Enter, Space, mnemonic letters and Escape, and mouse press, drag and release. The chosen item is returned by `takeChosen()`.
- `FramebufferSnapshot` saves and restores the native pixels under a pull-down menu.

## Lifecycle and ownership

The application owns the backend, the widget and the controls. After every graphics mode change it attaches the backend to the new mode, picks the theme with `FramebufferTheme::forSurface()`, lays the controls out again (which forgets posted menus) and redraws. The backend owns its device state and must leave the display and the terminal as it found them; the application restores the text mode on every exit path.

## Backends

The initial backend, in the separate `svgalib` library (`svgalib/src/main/vsdk/toolkit/gui/framebuffer/svgalib`), uses the svgalib-style API of the minimal kernel and of Linux. It draws with `vga_setcolor()`, `vga_drawpixel()` and `vga_drawscansegment()`, uses the VGA text font of `vga_gettextfont()` (code page 437, with Latin-1 letters mapped to it and anything else shown as `?`; bold in modes 640 pixels wide or more), and supports 1, 4, 8, 16 and 24 bits per pixel. In 16 and 256 colour modes it programs exact colors into palette entries on first use: entries 1 to 14 in 16 colour modes, keeping black and white, and entries 16 to 255 in 256 colour modes, keeping the standard 16; when entries run out the nearest known color is used. Keyboard input comes from the terminal bytes, including ANSI escape sequences, and the pointer is drawn with the kernel's `MouseCursor` and hidden while drawing. Text modes are outside its scope.

Future implementations can target the Borland Graphics Interface and the Microsoft mouse driver in real-mode DOS, protected-mode svgalib ports, and a lightweight Xlib layer. Experimental Java and HTML5/CSS implementations can explore the same concepts. These are prospective backends, not dependencies of the framebuffer interfaces.
