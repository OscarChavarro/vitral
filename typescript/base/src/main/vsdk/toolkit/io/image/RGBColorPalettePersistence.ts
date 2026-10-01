import type { DataInputStream } from "../../../../java/io/DataInputStream.js";
import type { Reader } from "../../../../java/io/Reader.js";
import { StreamTokenizer } from "../../../../java/io/StreamTokenizer.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { RGBColorPalette } from "../../media/RGBColorPalette.js";
import { PersistenceElement } from "../PersistenceElement.js";

/**
This class contains the persistence operations to load and save
RGBColorPalettes
*/
export class RGBColorPalettePersistence extends PersistenceElement {
    /**
    This method reads a text formated stream in the style of GIMP
    palettes and from its data generates a RGBColorPalette
    @return the RGBColorPalette builded from the source
    */
    public static importGimpPalette(source: Reader): RGBColorPalette {
        const p = new RGBColorPalette();
        p.init(0);

        const parser = new StreamTokenizer(source);
        const code = (c: string): number => c.charCodeAt(0);

        parser.resetSyntax();
        parser.eolIsSignificant(true);
        parser.slashSlashComments(false);
        parser.slashStarComments(false);
        parser.commentChar(code("#"));
        parser.whitespaceChars(code(" "), code(" "));
        parser.whitespaceChars(code(","), code(","));
        parser.whitespaceChars(code("\t"), code("\t"));
        parser.wordChars(code("A"), code("Z"));
        parser.wordChars(code("a"), code("z"));
        parser.wordChars(code("0"), code("9"));
        parser.wordChars(code("_"), code("_"));
        parser.parseNumbers();

        let tokenType: number;
        let startline = 0;
        let col: ColorRgb | null = null;
        let r = 0.0;
        let g = 0.0;

        do {
            try {
                tokenType = parser.nextToken();
            } catch {
                break;
            }
            switch (tokenType) {
                case StreamTokenizer.TT_EOL:
                    startline = 0;
                    break;
                case StreamTokenizer.TT_EOF:
                    break;
                case StreamTokenizer.TT_NUMBER:
                    switch (startline) {
                        case 0:
                            r = parser.nval / 255.0;
                            break;
                        case 1:
                            g = parser.nval / 255.0;
                            break;
                        case 2:
                            col = new ColorRgb(r, g, parser.nval / 255.0);
                            p.addColor(col);
                            break;
                    }
                    startline++;
                    break;
                case StreamTokenizer.TT_WORD:
                    break;
                default:
                    break;
            }
        } while (tokenType !== StreamTokenizer.TT_EOF);

        return p;
    }

    /** This method reads a binary raw stream of consecutive RGB color values
     */
    public static importRawPalette(dis: DataInputStream): RGBColorPalette {
        const p = new RGBColorPalette();
        p.init(0);

        while (dis.available() > 0) {
            let nr: number = dis.readByte();
            if (nr < 0) {
                nr = 256 + nr;
            }
            let ng: number = dis.readByte();
            if (ng < 0) {
                ng = 256 + ng;
            }

            let nb: number = dis.readByte();
            if (nb < 0) {
                nb = 256 + nb;
            }

            const color = new ColorRgb(nr, ng, nb);
            p.addColor(color);
        }

        dis.close();
        return p;
    }
}
