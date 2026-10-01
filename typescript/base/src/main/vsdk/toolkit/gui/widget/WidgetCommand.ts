import { VSDK } from "../../common/VSDK.js";
import type { RGBAImageUncompressed } from "../../media/RGBAImageUncompressed.js";
import type { RGBAPixel } from "../../media/RGBAPixel.js";
import type { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import type { RGBPixel } from "../../media/RGBPixel.js";
import { WidgetElement } from "./WidgetElement.js";

/**
This class plays a role of leaf on an n-ary tree in the composite design
pattern.
*/
export class WidgetCommand extends WidgetElement {
    private id: string | null;
    private name: string | null;
    private briefDescription: string | null;
    private help: string | null; // Could contain HTML tags
    private icon: RGBAImageUncompressed | null;
    private iconTransparency: RGBImageUncompressed | null;
    private secondaryIcon: RGBAImageUncompressed | null;
    private secondaryIconTransparency: RGBImageUncompressed | null;

    public constructor() {
        super();
        this.id = null;
        this.name = null;
        this.briefDescription = null;
        this.help = null;
        this.icon = null;
        this.iconTransparency = null;
        this.secondaryIcon = null;
        this.secondaryIconTransparency = null;
    }

    public getId(): string | null {
        return this.id;
    }

    public getName(): string | null {
        return this.name;
    }

    public getBriefDescription(): string | null {
        return this.briefDescription;
    }

    public getHelp(): string | null {
        return this.help;
    }

    public getIcon(): RGBAImageUncompressed | null {
        return this.icon;
    }

    public getIconTransparency(): RGBImageUncompressed | null {
        return this.iconTransparency;
    }

    public applyTransparency(): void {
        if (this.icon === null || this.iconTransparency === null) {
            return;
        }
        WidgetCommand.applyMask(this.icon, this.iconTransparency);
    }

    public applySecondTransparency(): void {
        if (this.secondaryIcon === null || this.secondaryIconTransparency === null) {
            return;
        }
        WidgetCommand.applyMask(this.secondaryIcon, this.secondaryIconTransparency);
    }

    /**
    Java repeats this loop in `applyTransparency` and
    `applySecondTransparency`; it is shared here, unchanged.
    */
    private static applyMask(icon: RGBAImageUncompressed, mask: RGBImageUncompressed): void {
        let x: number;
        let y: number;

        const xlimit: number = Math.min(icon.getXSize(), mask.getXSize());
        const ylimit: number = Math.min(icon.getYSize(), mask.getYSize());

        let pixelIn: RGBPixel;
        let pixelOut: RGBAPixel;
        let r: number;
        let g: number;
        let b: number;
        let a: number;

        for (y = 0; y < ylimit; y++) {
            for (x = 0; x < xlimit; x++) {
                pixelIn = mask.getPixel(x, y);
                r = VSDK.signedByte2unsignedInteger(pixelIn.r);
                g = VSDK.signedByte2unsignedInteger(pixelIn.g);
                b = VSDK.signedByte2unsignedInteger(pixelIn.b);
                a = Math.trunc((r + g + b) / 3);
                pixelOut = icon.getPixel(x, y);
                pixelOut.a = VSDK.unsigned8BitInteger2signedByte(a);
                icon.putPixel(x, y, pixelOut);
            }
        }
    }

    public setId(i: string | null): void {
        this.id = i;
    }

    public setName(n: string | null): void {
        this.name = n;
    }

    public setBrief(b: string | null): void {
        this.briefDescription = b;
    }

    public setHelp(h: string | null): void {
        this.help = h;
    }

    public appendToHelp(h: string): void {
        if (this.help !== null) {
            this.help = this.help + h;
        } else {
            this.help = h;
        }
    }

    public setIcon(i: RGBAImageUncompressed | null): void {
        this.icon = i;
    }

    public setIconTransparency(i: RGBImageUncompressed | null): void {
        this.iconTransparency = i;
    }

    public override toString(): string {
        let msg = "  - Command [" + this.id + "]:\n";
        msg = msg + "    . Name: " + this.name + "\n";
        msg = msg + "    . Brief description: " + this.briefDescription + "\n";
        if (this.icon === null) {
            msg = msg + "    . No icon image\n";
        } else {
            msg = msg + "    . Icon image of size (" + this.icon.getXSize() + ", " + this.icon.getYSize() + ")\n";
        }
        return msg;
    }

    /**
    @return the secondaryIcon
    */
    public getSecondaryIcon(): RGBAImageUncompressed | null {
        return this.secondaryIcon;
    }

    /**
    @param secondaryIcon the secondaryIcon to set
    */
    public setSecondaryIcon(secondaryIcon: RGBAImageUncompressed | null): void {
        this.secondaryIcon = secondaryIcon;
    }

    /**
    @return the secondaryIconTransparency
    */
    public getSecondaryIconTransparency(): RGBImageUncompressed | null {
        return this.secondaryIconTransparency;
    }

    /**
    @param secondaryIconTransparency the secondaryIconTransparency to set
    */
    public setSecondaryIconTransparency(secondaryIconTransparency: RGBImageUncompressed | null): void {
        this.secondaryIconTransparency = secondaryIconTransparency;
    }
}
