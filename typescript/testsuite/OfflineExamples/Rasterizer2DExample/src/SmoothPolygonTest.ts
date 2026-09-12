//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

// Basic JDK classes
import { File } from "@vitral/fs";

// VSDK classes
import { RGBImageUncompressed, RGBPixel } from "@vitral/base";
import { Polygon2D } from "@vitral/base";
import { Rasterizer2D } from "@vitral/base";
import { ImagePersistence } from "@vitral/fs";

export class SmoothPolygonTest {
    public static main(args: string[]): void {
        const outputFileName: string =
            args !== null && args.length > 0 && args[0] !== undefined && args[0].trim().length > 0
                ? args[0]
                : "output3.png";
        //-----------------------------------------------------------------
        let img: RGBImageUncompressed | null = null;
        const fillcolor: RGBPixel = new RGBPixel();
        const bordercolor: RGBPixel = new RGBPixel();

        fillcolor.r = -1;
        fillcolor.g = 0;
        fillcolor.b = 0;

        bordercolor.r = -1;
        bordercolor.g = -1;
        bordercolor.b = 0;

        img = new RGBImageUncompressed();
        img.init(640, 480);
        img.createTestPattern();

        //-----------------------------------------------------------------
        const pol: Polygon2D = new Polygon2D();

        pol.addVertex(70, 50, 1.0, 0.0, 0.0);
        pol.addVertex(400, 200, 0.0, 1.0, 0.0);
        pol.addVertex(100, 300, 0.0, 0.0, 1.0);
        pol.nextLoop();
        pol.addVertex(120, 150, 1.0, 1.0, 0.0);
        pol.addVertex(250, 150, 0.0, 1.0, 1.0);
        pol.addVertex(230, 220, 1.0, 0.0, 1.0);

        /*
        pol.addVertex(320, 50, 1.0, 0.0, 0.0);
        pol.addVertex(20, 300, 0.0, 1.0, 0.0);
        pol.addVertex(620, 300, 0.0, 0.0, 1.0);
        */

        //-----------------------------------------------------------------
        Rasterizer2D.fillSmoothPolygon(img, pol);
        Rasterizer2D.drawPolygon(img, pol, bordercolor);

        ImagePersistence.exportPNG(new File(outputFileName), img);
        console.log('Resulting image has been written to "' + outputFileName + '"');
    }
}

SmoothPolygonTest.main(process.argv.slice(2));
