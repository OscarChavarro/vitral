import type { ColorRgb, RGBAImageUncompressed } from "@vitral/base";

/**
Port of `vsdk.toolkit.render.jogl.viewport.Jogl4LabelImageProvider`.

Creates the images used by WebGL renderers to draw text labels (viewport
titles, axis names). It isolates the rendering classes from the technology
used to rasterize text (i.e. a 2D canvas), which is provided by the caller.

Java's `default` method (a label with the default font size) is the optional
`fontSize` of `createLabelImage`; the default size is
`WEBGL_LABEL_DEFAULT_FONT_SIZE`.
*/
export interface WebGLLabelImageProvider {
    /**
    @param text the text to draw
    @param color the color of the text
    @param fontSize the size of the font, in pixels
    @return an image with the text on transparent background
    */
    createLabelImage(text: string, color: ColorRgb, fontSize: number): RGBAImageUncompressed;
}

/** Font size, in pixels, of the labels created without giving one. */
export const WEBGL_LABEL_DEFAULT_FONT_SIZE: number = 14;
