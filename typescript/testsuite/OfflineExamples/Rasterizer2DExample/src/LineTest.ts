// Basic JDK classes
import { File } from "@vitral/fs";

// VSDK classes
import { RGBImageUncompressed, RGBPixel, JavaMath } from "@vitral/base";
import { ImagePersistence } from "@vitral/fs";
import { Rasterizer2D } from "@vitral/base";

export class LineTest {
    public static main(args: string[]): void {
        const outputFileName: string =
            args !== null && args.length > 0 && args[0] !== undefined && args[0].trim().length > 0
                ? args[0]
                : "output1.png";
        //-----------------------------------------------------------------
        let img: RGBImageUncompressed | null = null;
        const color: RGBPixel = new RGBPixel();

        color.r = -1;
        color.g = 0;
        color.b = 0;

        img = new RGBImageUncompressed();
        img.init(640, 480);
        //img.createTestPattern();

        //-----------------------------------------------------------------
        let x: number;
        let y: number;
        let a: number;

        for (a = 0; a < 360.0; a += 15.0) {
            x = 320 + Math.trunc(200.0 * Math.cos(JavaMath.toRadians(a)));
            y = 240 + Math.trunc(200.0 * Math.sin(JavaMath.toRadians(a)));
            Rasterizer2D.drawLine(img, 320, 240, x, y, color);
        }

        //-----------------------------------------------------------------
        ImagePersistence.exportPNG(new File(outputFileName), img);
        console.log('Resulting image has been written to "' + outputFileName + '"');
    }
}

LineTest.main(process.argv.slice(2));
