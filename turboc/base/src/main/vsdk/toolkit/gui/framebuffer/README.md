# Framebuffer GUI (turboc port)

This is the turboc port of the framebuffer GUI package of the cpp port (`cpp/base/src/main/vsdk/toolkit/gui/framebuffer`, see its README for the design). It keeps the same classes, methods and behavior, written in the language subset of the turboc port (no `bool`, `nullptr`, `enum class`, range `for`, `<stdint.h>` nor `std::nothrow`; colors and native pixel values are `unsigned long`).

Only the part that the menu-only Scene Editor needs has been ported so far:

- `Framebuffer`, `FramebufferEvent` and `FramebufferInput`: the contracts of the backends.
- `FramebufferTheme`, `FramebufferPainter`: the DECwindows and monochrome looks and the Motif primitives.
- `FramebufferSnapshot`: the pixels saved under the pull-down menus. In this port they are kept in as many bytes as the depth needs and one row at a time, as a real mode DOS program has 640 KB and blocks of 64 KB at most; with `TURBO_C_8086_REAL_MODE_ENABLED` the rows go to an XMS block (`XmsMemory` of the `dos` module) when there is one.
- `FramebufferMenuBar` and `FramebufferMenuReader`: the menu bar of a GUI JSON definition.

The toolbars, side panels, window frames, dialogs and controls of the cpp port are still to be ported.

## Backends

The BGI backend for real mode DOS is the separate `bgi` library (`turboc/bgi/src/main/vsdk/toolkit/gui/framebuffer/bgi`), the counterpart of the cpp port's `svgalib` and `xlib` libraries. `testsuite/BGIExamples/SceneEditorApplication` uses it.
