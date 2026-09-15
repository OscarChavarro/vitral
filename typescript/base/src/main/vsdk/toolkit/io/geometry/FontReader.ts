import type { ParametricCurve } from "../../environment/geometry/curve/ParametricCurve.js";
import { PersistenceElement } from "../PersistenceElement.js";

/**
This is an abstract class to serve as a base in an abstract factory design
pattern that serves to load font file data from TrueType, Type1 and
other common font encoding schema.

The basic functionality that this must provide, is to extract a glyph
from a specified character from a font file.

Port of `vsdk.toolkit.io.geometry.FontReader`. Java's only concrete factory is
`vsdk.toolkit.render.awt.AwtFontReader`, in the AWT module; the browser's is
`WebFontReader` in `@vitral/webgl`.
*/
export abstract class FontReader extends PersistenceElement {
    public abstract extractGlyph(fontFile: string, characterAndItsContext: string): ParametricCurve | null;
}
