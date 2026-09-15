import { FontReader, ParametricCurve, VSDK, Vector3Dd } from "@vitral/base";

/** One segment of a glyph outline, as `java.awt.geom.PathIterator` reports it. */
interface _OutlineSegment {
    /** `SEG_MOVETO`, `SEG_LINETO`, `SEG_QUADTO` or `SEG_CLOSE`. */
    readonly type: number;
    /** The segment's coordinates, in the `PathIterator` layout. */
    readonly coords: readonly number[];
}

/** A TrueType font the reader has parsed: the tables the outline needs. */
interface _TrueTypeFont {
    readonly data: DataView;
    readonly unitsPerEm: number;
    readonly indexToLocFormat: number;
    readonly numGlyphs: number;
    readonly locaOffset: number;
    readonly glyfOffset: number;
    readonly cmapOffset: number;
}

/** One contour point of a loaded glyph, already scaled to 26.6 pixels. */
interface _GlyphPoint {
    x: number;
    y: number;
    onCurve: boolean;
}

/**
The browser's concrete `FontReader`: the counterpart of
`vsdk.toolkit.render.awt.AwtFontReader`.

Java does not read the font file itself. It hands the file to AWT
(`Font.createFont`, `deriveFont(10)`), asks a `GlyphVector` for the outline of
the glyph, and walks that outline with a `PathIterator`, turning each segment
into a `ParametricCurve` point; on the host this port is verified on, AWT gets
the outline from FreeType. A browser offers no API that answers a glyph
outline — a canvas can only paint one — so this class reads the TrueType
tables and produces the segments AWT would have handed Java, and from there on
the conversion is `AwtFontReader.extractGlyph` statement by statement: the
`BREAK` before each contour, `CORNER` for a line, `QUAD` with the endpoint
before the control point ("the inverse order of awt with respect to VSDK"),
the `BEZIER` control attachment, the duplicate-endpoint filter, the ten-point
size divided back out, and the Y flip.

What AWT's outline is, and therefore what the TrueType half reproduces, is
FreeType's unhinted load of the glyph at ten pixels per em — AWT's
`FontRenderContext(identity, antialiased, fractional metrics)` loads outlines
without hinting — and FreeType's `FT_Outline_Decompose` of it:

  - every font-unit coordinate is scaled to 26.6 fixed point with
    `FT_MulFix(value, FT_DivFix(10 << 6, unitsPerEm))`, the scale
    `FT_Request_Size` computes for a ten-point request at 72 dpi, and AWT
    divides the result by 64 into a `float`, negating Y;
  - a contour starts at its first on-curve point or, when it opens off-curve,
    at the last point if that one is on-curve and otherwise at the integer
    midpoint of the first and the last; two consecutive off-curve points imply
    an on-curve one at their integer midpoint; and every contour ends with an
    explicit line back to its start and a close.

A composite glyph is assembled as FreeType assembles an unhinted one: each
component loaded and scaled, transformed by its 2x2 matrix when it has one,
and moved by its offset, scaled unless the component asks otherwise; point
matching and grid rounding, which only a hinted load uses, are not.

A font file is named by URL, and fetching is asynchronous while
`extractGlyph` is synchronous in Java, so a font is fetched once with `load`
before the glyph is asked for; a font that was never loaded, or failed to
parse, answers the null Java answers for a font AWT could not create.
*/
export class WebFontReader extends FontReader {
    private static readonly SEG_MOVETO = 0;
    private static readonly SEG_LINETO = 1;
    private static readonly SEG_QUADTO = 2;
    private static readonly SEG_CUBICTO = 3;
    private static readonly SEG_CLOSE = 4;

    private readonly fontData = new Map<string, ArrayBuffer>();
    private currentFont: _TrueTypeFont | null;
    private fileName: string | null;

    public constructor() {
        super();
        this.currentFont = null;
        this.fileName = null;
    }

    /**
    Fetches the font a later `extractGlyph(fontFile, ...)` names. This is the
    read AWT does inside `Font.createFont`, moved ahead of the synchronous call
    that needs it. A program that names its font by a file path, as the Java
    testsuite programs do, fetches it from a URL and registers it under that
    path; by default the URL is the name.
    */
    public async load(fontUrl: string, fontFile: string = fontUrl): Promise<void> {
        if (this.fontData.has(fontFile)) {
            return;
        }
        const response = await fetch(fontUrl);
        if (!response.ok) {
            throw new Error("Font file not found: " + fontUrl);
        }
        this.fontData.set(fontFile, await response.arrayBuffer());
    }

    /**
    Given a font file and a character, this method return a parametric
    curve representing a glyph. If something goes wrong, this method
    returns null. The character string is usually a lone character,
    but under some circumstantes this chatacter is acommpanied with
    a context (i.e. in arabic languages where glyph selection depends
    on a bounding form).

    Note that glyph vectorization in Java's AWT relys to much (perhaps
    incorrectly) on current font size. When using a size of 1 point,
    some glyphs composed of multiple curves get incorrectly placed components.
    So this method works with fonts `factor` points in size, to later scale the
    glyphs down by a factor of 1/`factor`.
    */
    public override extractGlyph(fontFile: string, characterAndItsContext: string): ParametricCurve | null {
        //-----------------------------------------------------------------
        const factor: number = Math.fround(10.0);
        if (this.currentFont === null || this.fileName === null || fontFile !== this.fileName) {
            this.fileName = fontFile;
            try {
                const bytes: ArrayBuffer | undefined = this.fontData.get(fontFile);
                if (bytes === undefined) {
                    throw new Error("Font file was not loaded: " + fontFile);
                }
                this.currentFont = WebFontReader.parseTrueType(bytes);
            } catch {
                console.error("Error loading font file " + fontFile);
                this.currentFont = null;
                return null;
            }
        }

        let curve: ParametricCurve;

        //-----------------------------------------------------------------
        let pointParameters: Vector3Dd[];

        curve = new ParametricCurve();

        //- Glyph analisys -------------------------------------------
        const p: _OutlineSegment[] | null = WebFontReader.glyphOutline(
            this.currentFont,
            characterAndItsContext,
            factor,
        );
        if (p === null) {
            console.error("Glyph outline is null for [" + characterAndItsContext + "] in font " + fontFile);
            return null;
        }

        const endIt = false;
        let code: number;
        let lastAddedEndpoint: Vector3Dd | null = null;

        for (const segment of p) {
            const coords: readonly number[] = segment.coords;
            const type: number = segment.type;

            code = 0;
            switch (type) {
                case WebFontReader.SEG_CUBICTO:
                    code = 4;
                    break;
                case WebFontReader.SEG_LINETO:
                    code = 1;
                    break;
                case WebFontReader.SEG_MOVETO:
                    code = 0;
                    break;
                case WebFontReader.SEG_QUADTO:
                    code = 2;
                    break;
                case WebFontReader.SEG_CLOSE:
                    code = 3;
                    break;
                default:
                    console.log("AwtFontReader.extractGlyph: UNKNOWN");
                    break;
            }

            if (!endIt) {
                switch (code) {
                    case 0:
                        curve.addPoint(null, ParametricCurve.BREAK);
                        lastAddedEndpoint = null;

                        pointParameters = [new Vector3Dd(coords[0]! / factor, -coords[1]! / factor, 0)];
                        if (WebFontReader.shouldAddEndpoint(lastAddedEndpoint, pointParameters[0]!)) {
                            curve.addPoint(pointParameters, ParametricCurve.CORNER);
                            lastAddedEndpoint = pointParameters[0]!;
                        }
                        break;
                    case 1:
                        pointParameters = [new Vector3Dd(coords[0]! / factor, -coords[1]! / factor, 0)];
                        if (WebFontReader.shouldAddEndpoint(lastAddedEndpoint, pointParameters[0]!)) {
                            curve.addPoint(pointParameters, ParametricCurve.CORNER);
                            lastAddedEndpoint = pointParameters[0]!;
                        }
                        break;
                    case 2:
                        // Note the inverse order of awt with respect to VSDK!
                        pointParameters = [
                            new Vector3Dd(coords[2]! / factor, -coords[3]! / factor, 0),
                            new Vector3Dd(coords[0]! / factor, -coords[1]! / factor, 0),
                        ];
                        if (WebFontReader.shouldAddEndpoint(lastAddedEndpoint, pointParameters[0]!)) {
                            curve.addPoint(pointParameters, ParametricCurve.QUAD);
                            lastAddedEndpoint = pointParameters[0]!;
                        }
                        break;
                    case 3:
                        //endIt = true;
                        break;
                    case 4:
                        pointParameters = [
                            new Vector3Dd(coords[4]! / factor, -coords[5]! / factor, 0),
                            new Vector3Dd(coords[2]! / factor, -coords[3]! / factor, 0),
                        ];
                        if (WebFontReader.shouldAddEndpoint(lastAddedEndpoint, pointParameters[0]!)) {
                            WebFontReader.attachBezierControlToPreviousPoint(
                                curve,
                                new Vector3Dd(coords[0]! / factor, -coords[1]! / factor, 0),
                            );
                            curve.addPoint(pointParameters, ParametricCurve.BEZIER);
                            lastAddedEndpoint = pointParameters[0]!;
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (curve.types.length < 2) {
            console.error(
                "Glyph [" + characterAndItsContext + "] in font " + fontFile + " produced too few curve segments",
            );
            return null;
        }

        return curve;
    }

    private static attachBezierControlToPreviousPoint(curve: ParametricCurve, controlPoint: Vector3Dd): void {
        if (curve.points.length === 0) {
            return;
        }

        const lastIndex: number = curve.points.length - 1;
        const previous: Vector3Dd[] | null | undefined = curve.points[lastIndex];
        if (previous === null || previous === undefined || previous.length === 0 || previous[0] === undefined) {
            return;
        }

        if (previous.length >= 3) {
            previous[2] = controlPoint;
            return;
        }

        const expanded: Vector3Dd[] = new Array<Vector3Dd>(3);
        expanded[0] = previous[0];
        if (previous.length > 1) {
            expanded[1] = previous[1]!;
        }
        expanded[2] = controlPoint;
        curve.setPointAt(expanded, lastIndex);
    }

    private static shouldAddEndpoint(lastAddedEndpoint: Vector3Dd | null, candidateEndpoint: Vector3Dd): boolean {
        return lastAddedEndpoint === null || Vector3Dd.distance(lastAddedEndpoint, candidateEndpoint) >= VSDK.EPSILON;
    }

    //=================================================================
    //= What AWT answers: the outline FreeType decomposes =============

    /**
    The segments of the outline of the first glyph of `text`, with AWT's
    coordinates: pixels at `size` pixels per em, as `float`, Y pointing down.
    Null when the font has no glyph for the character, which AWT reports as
    `.notdef`'s outline; `.notdef` is glyph zero and is loaded like any other.
    */
    private static glyphOutline(font: _TrueTypeFont, text: string, size: number): _OutlineSegment[] | null {
        const codePoint: number | undefined = text.codePointAt(0);
        if (codePoint === undefined) {
            return null;
        }
        const glyphIndex: number = WebFontReader.glyphIndexFor(font, codePoint);
        const scale: number = WebFontReader.ftDivFix(Math.round(size * 64), font.unitsPerEm);

        const contours: _GlyphPoint[][] = [];
        WebFontReader.loadGlyph(font, glyphIndex, scale, contours, 0);

        const segments: _OutlineSegment[] = [];
        for (const contour of contours) {
            WebFontReader.decomposeContour(contour, segments);
        }
        return segments;
    }

    /** `FT_Outline_Decompose` of one contour, with AWT's `outlineToShape` callbacks. */
    private static decomposeContour(points: _GlyphPoint[], out: _OutlineSegment[]): void {
        if (points.length === 0) {
            return;
        }
        const first: _GlyphPoint = points[0]!;
        const last: _GlyphPoint = points[points.length - 1]!;
        let vStart: { x: number; y: number } = { x: first.x, y: first.y };
        let limit: number = points.length - 1;
        let index = 0;

        if (!first.onCurve) {
            if (last.onCurve) {
                vStart = { x: last.x, y: last.y };
                limit--;
            } else {
                vStart = {
                    x: Math.trunc((first.x + last.x) / 2),
                    y: Math.trunc((first.y + last.y) / 2),
                };
            }
            index = -1;
        }

        WebFontReader.emit(out, WebFontReader.SEG_MOVETO, [vStart.x, vStart.y]);

        let closedByConic = false;
        while (index < limit) {
            index++;
            const point: _GlyphPoint = points[index]!;
            if (point.onCurve) {
                WebFontReader.emit(out, WebFontReader.SEG_LINETO, [point.x, point.y]);
                continue;
            }

            let control: { x: number; y: number } = { x: point.x, y: point.y };
            for (;;) {
                if (index < limit) {
                    index++;
                    const next: _GlyphPoint = points[index]!;
                    if (next.onCurve) {
                        WebFontReader.emit(out, WebFontReader.SEG_QUADTO, [control.x, control.y, next.x, next.y]);
                        break;
                    }
                    const middle = {
                        x: Math.trunc((control.x + next.x) / 2),
                        y: Math.trunc((control.y + next.y) / 2),
                    };
                    WebFontReader.emit(out, WebFontReader.SEG_QUADTO, [control.x, control.y, middle.x, middle.y]);
                    control = { x: next.x, y: next.y };
                    continue;
                }
                WebFontReader.emit(out, WebFontReader.SEG_QUADTO, [control.x, control.y, vStart.x, vStart.y]);
                closedByConic = true;
                break;
            }
            if (closedByConic) {
                break;
            }
        }

        if (!closedByConic) {
            WebFontReader.emit(out, WebFontReader.SEG_LINETO, [vStart.x, vStart.y]);
        }
        out.push({ type: WebFontReader.SEG_CLOSE, coords: [] });
    }

    /** AWT's `F26Dot6ToFloat`, with its Y negation, into a `GeneralPath` segment. */
    private static emit(out: _OutlineSegment[], type: number, f26dot6: number[]): void {
        const coords: number[] = [];
        for (let i = 0; i < f26dot6.length; i += 2) {
            coords.push(Math.fround(f26dot6[i]! / 64.0));
            coords.push(Math.fround(-f26dot6[i + 1]! / 64.0));
        }
        out.push({ type, coords });
    }

    /** `FT_MulFix`: rounds half away from zero. */
    private static ftMulFix(a: number, b: number): number {
        const sign: number = a < 0 !== b < 0 ? -1 : 1;
        const magnitude: bigint = (BigInt(Math.abs(a)) * BigInt(Math.abs(b)) + 0x8000n) >> 16n;
        return sign * Number(magnitude);
    }

    /** `FT_DivFix`: 16.16 quotient, rounded half up on magnitudes. */
    private static ftDivFix(a: number, b: number): number {
        const sign: number = a < 0 !== b < 0 ? -1 : 1;
        const ua = BigInt(Math.abs(a));
        const ub = BigInt(Math.abs(b));
        if (ub === 0n) {
            return 0x7fffffff;
        }
        return sign * Number(((ua << 16n) + (ub >> 1n)) / ub);
    }

    private static parseTrueType(bytes: ArrayBuffer): _TrueTypeFont {
        const data = new DataView(bytes);
        const numTables: number = data.getUint16(4);
        const tables = new Map<string, number>();
        for (let i = 0; i < numTables; i++) {
            const record: number = 12 + i * 16;
            const tag: string = String.fromCharCode(
                data.getUint8(record),
                data.getUint8(record + 1),
                data.getUint8(record + 2),
                data.getUint8(record + 3),
            );
            tables.set(tag, data.getUint32(record + 8));
        }
        const head = tables.get("head");
        const maxp = tables.get("maxp");
        const loca = tables.get("loca");
        const glyf = tables.get("glyf");
        const cmap = tables.get("cmap");
        if (
            head === undefined ||
            maxp === undefined ||
            loca === undefined ||
            glyf === undefined ||
            cmap === undefined
        ) {
            throw new Error("Not a TrueType-outline font");
        }
        return {
            data,
            unitsPerEm: data.getUint16(head + 18),
            indexToLocFormat: data.getInt16(head + 50),
            numGlyphs: data.getUint16(maxp + 4),
            locaOffset: loca,
            glyfOffset: glyf,
            cmapOffset: cmap,
        };
    }

    /** The `cmap` lookup: format 12 or format 4 of the Unicode subtables. */
    private static glyphIndexFor(font: _TrueTypeFont, codePoint: number): number {
        const data: DataView = font.data;
        const numberSubtables: number = data.getUint16(font.cmapOffset + 2);
        let format4: number | null = null;
        let format12: number | null = null;
        for (let i = 0; i < numberSubtables; i++) {
            const record: number = font.cmapOffset + 4 + i * 8;
            const platformId: number = data.getUint16(record);
            const encodingId: number = data.getUint16(record + 2);
            const offset: number = font.cmapOffset + data.getUint32(record + 4);
            const unicode: boolean = platformId === 0 || (platformId === 3 && (encodingId === 1 || encodingId === 10));
            if (!unicode) {
                continue;
            }
            const format: number = data.getUint16(offset);
            if (format === 12 && format12 === null) {
                format12 = offset;
            } else if (format === 4 && format4 === null) {
                format4 = offset;
            }
        }

        if (format12 !== null) {
            const groups: number = data.getUint32(format12 + 12);
            for (let i = 0; i < groups; i++) {
                const group: number = format12 + 16 + i * 12;
                const startCode: number = data.getUint32(group);
                const endCode: number = data.getUint32(group + 4);
                if (codePoint >= startCode && codePoint <= endCode) {
                    return data.getUint32(group + 8) + (codePoint - startCode);
                }
            }
            return 0;
        }
        if (format4 !== null && codePoint <= 0xffff) {
            const segX2: number = data.getUint16(format4 + 6);
            const endCodes: number = format4 + 14;
            const startCodes: number = endCodes + segX2 + 2;
            const idDeltas: number = startCodes + segX2;
            const idRangeOffsets: number = idDeltas + segX2;
            for (let i = 0; i < segX2 / 2; i++) {
                const endCode: number = data.getUint16(endCodes + i * 2);
                if (codePoint > endCode) {
                    continue;
                }
                const startCode: number = data.getUint16(startCodes + i * 2);
                if (codePoint < startCode) {
                    return 0;
                }
                const idDelta: number = data.getInt16(idDeltas + i * 2);
                const rangeOffsetPosition: number = idRangeOffsets + i * 2;
                const idRangeOffset: number = data.getUint16(rangeOffsetPosition);
                if (idRangeOffset === 0) {
                    return (codePoint + idDelta) & 0xffff;
                }
                const glyph: number = data.getUint16(rangeOffsetPosition + idRangeOffset + (codePoint - startCode) * 2);
                return glyph === 0 ? 0 : (glyph + idDelta) & 0xffff;
            }
        }
        return 0;
    }

    private static glyphDataRange(font: _TrueTypeFont, glyphIndex: number): [number, number] {
        if (glyphIndex < 0 || glyphIndex >= font.numGlyphs) {
            return [0, 0];
        }
        const data: DataView = font.data;
        if (font.indexToLocFormat === 0) {
            return [
                data.getUint16(font.locaOffset + glyphIndex * 2) * 2,
                data.getUint16(font.locaOffset + (glyphIndex + 1) * 2) * 2,
            ];
        }
        return [
            data.getUint32(font.locaOffset + glyphIndex * 4),
            data.getUint32(font.locaOffset + (glyphIndex + 1) * 4),
        ];
    }

    /**
    Appends the scaled contours of a glyph. Simple glyphs are scaled point by
    point with `FT_MulFix`; composite glyphs recurse into their components.
    */
    private static loadGlyph(
        font: _TrueTypeFont,
        glyphIndex: number,
        scale: number,
        contours: _GlyphPoint[][],
        depth: number,
    ): void {
        if (depth > 16) {
            return;
        }
        const [start, end] = WebFontReader.glyphDataRange(font, glyphIndex);
        if (end <= start) {
            return;
        }
        const data: DataView = font.data;
        const glyph: number = font.glyfOffset + start;
        const numberOfContours: number = data.getInt16(glyph);

        if (numberOfContours >= 0) {
            WebFontReader.loadSimpleGlyph(data, glyph, numberOfContours, scale, contours);
            return;
        }
        WebFontReader.loadCompositeGlyph(font, glyph, scale, contours, depth);
    }

    private static loadSimpleGlyph(
        data: DataView,
        glyph: number,
        numberOfContours: number,
        scale: number,
        contours: _GlyphPoint[][],
    ): void {
        const endPoints: number[] = [];
        for (let i = 0; i < numberOfContours; i++) {
            endPoints.push(data.getUint16(glyph + 10 + i * 2));
        }
        const pointCount: number = numberOfContours === 0 ? 0 : endPoints[numberOfContours - 1]! + 1;
        const instructionLength: number = data.getUint16(glyph + 10 + numberOfContours * 2);
        let cursor: number = glyph + 12 + numberOfContours * 2 + instructionLength;

        const flags: number[] = [];
        while (flags.length < pointCount) {
            const flag: number = data.getUint8(cursor++);
            flags.push(flag);
            if ((flag & 0x08) !== 0) {
                let repeat: number = data.getUint8(cursor++);
                while (repeat-- > 0 && flags.length < pointCount) {
                    flags.push(flag);
                }
            }
        }

        const xs: number[] = [];
        let x = 0;
        for (let i = 0; i < pointCount; i++) {
            const flag: number = flags[i]!;
            if ((flag & 0x02) !== 0) {
                const delta: number = data.getUint8(cursor++);
                x += (flag & 0x10) !== 0 ? delta : -delta;
            } else if ((flag & 0x10) === 0) {
                x += data.getInt16(cursor);
                cursor += 2;
            }
            xs.push(x);
        }
        const ys: number[] = [];
        let y = 0;
        for (let i = 0; i < pointCount; i++) {
            const flag: number = flags[i]!;
            if ((flag & 0x04) !== 0) {
                const delta: number = data.getUint8(cursor++);
                y += (flag & 0x20) !== 0 ? delta : -delta;
            } else if ((flag & 0x20) === 0) {
                y += data.getInt16(cursor);
                cursor += 2;
            }
            ys.push(y);
        }

        let first = 0;
        for (let c = 0; c < numberOfContours; c++) {
            const last: number = endPoints[c]!;
            const contour: _GlyphPoint[] = [];
            for (let i = first; i <= last; i++) {
                contour.push({
                    x: WebFontReader.ftMulFix(xs[i]!, scale),
                    y: WebFontReader.ftMulFix(ys[i]!, scale),
                    onCurve: (flags[i]! & 0x01) !== 0,
                });
            }
            contours.push(contour);
            first = last + 1;
        }
    }

    private static loadCompositeGlyph(
        font: _TrueTypeFont,
        glyph: number,
        scale: number,
        contours: _GlyphPoint[][],
        depth: number,
    ): void {
        const ARG_1_AND_2_ARE_WORDS = 0x0001;
        const ARGS_ARE_XY_VALUES = 0x0002;
        const WE_HAVE_A_SCALE = 0x0008;
        const MORE_COMPONENTS = 0x0020;
        const WE_HAVE_AN_X_AND_Y_SCALE = 0x0040;
        const WE_HAVE_A_TWO_BY_TWO = 0x0080;
        const SCALED_COMPONENT_OFFSET = 0x0800;
        const UNSCALED_COMPONENT_OFFSET = 0x1000;

        const data: DataView = font.data;
        let cursor: number = glyph + 10;
        let flags: number;
        do {
            flags = data.getUint16(cursor);
            const glyphIndex: number = data.getUint16(cursor + 2);
            cursor += 4;

            let arg1: number;
            let arg2: number;
            if ((flags & ARG_1_AND_2_ARE_WORDS) !== 0) {
                arg1 = data.getInt16(cursor);
                arg2 = data.getInt16(cursor + 2);
                cursor += 4;
            } else {
                arg1 = data.getInt8(cursor);
                arg2 = data.getInt8(cursor + 1);
                cursor += 2;
            }

            // The 2x2 matrix in 16.16, from its F2Dot14 fields.
            let xx = 0x10000;
            let xy = 0;
            let yx = 0;
            let yy = 0x10000;
            if ((flags & WE_HAVE_A_SCALE) !== 0) {
                xx = data.getInt16(cursor) * 4;
                yy = xx;
                cursor += 2;
            } else if ((flags & WE_HAVE_AN_X_AND_Y_SCALE) !== 0) {
                xx = data.getInt16(cursor) * 4;
                yy = data.getInt16(cursor + 2) * 4;
                cursor += 4;
            } else if ((flags & WE_HAVE_A_TWO_BY_TWO) !== 0) {
                xx = data.getInt16(cursor) * 4;
                yx = data.getInt16(cursor + 2) * 4;
                xy = data.getInt16(cursor + 4) * 4;
                yy = data.getInt16(cursor + 6) * 4;
                cursor += 8;
            }
            const transformed: boolean =
                (flags & (WE_HAVE_A_SCALE | WE_HAVE_AN_X_AND_Y_SCALE | WE_HAVE_A_TWO_BY_TWO)) !== 0;

            const component: _GlyphPoint[][] = [];
            WebFontReader.loadGlyph(font, glyphIndex, scale, component, depth + 1);

            if (transformed) {
                for (const contour of component) {
                    for (const point of contour) {
                        const px: number = point.x;
                        const py: number = point.y;
                        point.x = WebFontReader.ftMulFix(px, xx) + WebFontReader.ftMulFix(py, xy);
                        point.y = WebFontReader.ftMulFix(px, yx) + WebFontReader.ftMulFix(py, yy);
                    }
                }
            }

            if ((flags & ARGS_ARE_XY_VALUES) !== 0) {
                let dx: number = arg1;
                let dy: number = arg2;
                if (
                    transformed &&
                    (flags & UNSCALED_COMPONENT_OFFSET) === 0 &&
                    (flags & SCALED_COMPONENT_OFFSET) !== 0
                ) {
                    const tx: number = WebFontReader.ftMulFix(dx, xx) + WebFontReader.ftMulFix(dy, xy);
                    const ty: number = WebFontReader.ftMulFix(dx, yx) + WebFontReader.ftMulFix(dy, yy);
                    dx = tx;
                    dy = ty;
                }
                dx = WebFontReader.ftMulFix(dx, scale);
                dy = WebFontReader.ftMulFix(dy, scale);
                for (const contour of component) {
                    for (const point of contour) {
                        point.x += dx;
                        point.y += dy;
                    }
                }
            }

            contours.push(...component);
        } while ((flags & MORE_COMPONENTS) !== 0);
    }
}
