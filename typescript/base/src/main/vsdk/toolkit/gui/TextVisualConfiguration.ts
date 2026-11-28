import { HashMap } from "../../../java/util/HashMap.js";
import { ColorRgb } from "../common/color/ColorRgb.js";
import type { RGBAImageUncompressed } from "../media/RGBAImageUncompressed.js";
import { PresentationElement } from "./PresentationElement.js";

/**
This class is used to set visual characteristics for text rendering.
*/
export class TextVisualConfiguration extends PresentationElement {
    private fontName: string | null = null;
    private fontSize: number;
    private foregroundColor: ColorRgb;
    private backgroundColor: ColorRgb;
    private borderSize: number;
    private readonly characterSprites: HashMap<string, RGBAImageUncompressed>;

    public constructor() {
        super();
        this.characterSprites = new HashMap<string, RGBAImageUncompressed>();
        this.fontSize = 16;
        this.borderSize = 1;
        this.foregroundColor = new ColorRgb(1, 1, 1);
        this.backgroundColor = new ColorRgb(0, 0, 0);
    }

    /**
    @return the fontName
    */
    public getFontName(): string | null {
        return this.fontName;
    }

    /**
    @param fontName the fontName to set
    */
    public setFontName(fontName: string | null): void {
        this.fontName = fontName;
    }

    /**
    @return the fontSize
    */
    public getFontSize(): number {
        return this.fontSize;
    }

    /**
    @param fontSize the fontSize to set
    */
    public setFontSize(fontSize: number): void {
        this.fontSize = fontSize;
    }

    /**
    @return the foregroundColor
    */
    public getForegroundColor(): ColorRgb {
        return this.foregroundColor;
    }

    /**
    @param foregroundColor the foregroundColor to set
    */
    public setForegroundColor(foregroundColor: ColorRgb): void {
        this.foregroundColor = foregroundColor;
    }

    /**
    @return the backgroundColor
    */
    public getBackgroundColor(): ColorRgb {
        return this.backgroundColor;
    }

    /**
    @param backgroundColor the backgroundColor to set
    */
    public setBackgroundColor(backgroundColor: ColorRgb): void {
        this.backgroundColor = backgroundColor;
    }

    /**
    @return the characterSprites
    */
    public getCharacterSprites(): HashMap<string, RGBAImageUncompressed> {
        return this.characterSprites;
    }

    /**
    @return the borderSize
    */
    public getBorderSize(): number {
        return this.borderSize;
    }

    /**
    @param borderSize the borderSize to set
    */
    public setBorderSize(borderSize: number): void {
        this.borderSize = borderSize;
    }
}
