// Java classes
import { Double, JavaMath } from "@vitral/base";
import { File } from "@vitral/fs";

// VSDK classes
import { Matrix4x4d, Vector3Dd } from "@vitral/base"; // Model elements
import { Camera } from "@vitral/base";
import { Arrow } from "@vitral/base";
import { SimpleBody } from "@vitral/base";
import { SimpleScene } from "@vitral/base";
import { Calligraphic2DBuffer } from "@vitral/base"; // I/O artifacts
import { RGBImageUncompressed } from "@vitral/base";
import { EnvironmentPersistence } from "@vitral/fs"; // Persistence elements
import { ImagePersistence } from "@vitral/fs";
import { WireframeRenderer } from "@vitral/base"; // Processing elements

/**
This example program is the most fundamental computer graphics example in
VitralSDK that does not depend on any external libraries to generate an image
from a 3D scene. Note that it is based on:
  - A wireframe model imported from an external .obj file (requires a
    running platform with support to file systems, networked or "inline in
    code" reader).
  - Simple camera model and calligraphic renderer (100% java / Vitral SDK
    implementation)
  - Raster output using Vitral SDK image and Bresenham's line algorithm
  - This particular program exports the resulting image in a file (requires
    a platform supporting I/O to files).
*/
export class WireframeOfflineExample {
    private camera!: Camera;
    private scene!: SimpleScene;

    /**
    Note the starting simplicity of this constructor. Compare it against
    the Swing+JOGL version of this same example (VSDKExamples/WireframeExample)
    and recall that the only difference is that the offline example must
    update the virtual viewport space to the camera (in the interactive
    version this is done on a Swing/JOGL controlled callback function).
    */
    public constructor() {
        this.createModel();
        this.camera.updateViewportResize(640, 480);
    }

    private createModel(): void {
        //-----------------------------------------------------------------
        this.camera = new Camera();
        let R: Matrix4x4d = new Matrix4x4d();

        this.camera.setPosition(new Vector3Dd(7, -4, 4));
        R = R.eulerAnglesRotation(JavaMath.toRadians(140), JavaMath.toRadians(-30), 0);
        this.camera.setNearPlaneDistance(0.001);
        this.camera.setFarPlaneDistance(100);
        this.camera.setRotation(R);

        //-----------------------------------------------------------------
        const sceneFile = "../../../../etc/geometry/cow.obj";
        this.scene = new SimpleScene();

        try {
            EnvironmentPersistence.importEnvironment(new File(sceneFile), this.scene);
        } catch (ex) {
            console.error("Failed to read file");
            console.error(ex);
        }

        //-----------------------------------------------------------------
        let b: SimpleBody;
        let arrow: Arrow;

        b = new SimpleBody();
        arrow = new Arrow(1.0, 0.5, 0.15, 0.3);
        b.setGeometry(arrow);
        b.setPosition(new Vector3Dd(1, 2, 3));
        this.scene.addBody(b);
    }

    public rasterOutput(lineSet: Calligraphic2DBuffer): void {
        let outputImageRasterViewport: RGBImageUncompressed;

        //- (1/2) line rasterization in to output image -------------------
        const xt: number = this.camera.getViewportXSize();
        const yt: number = this.camera.getViewportYSize();

        outputImageRasterViewport = new RGBImageUncompressed();
        outputImageRasterViewport.init(Math.trunc(xt), Math.trunc(yt));

        lineSet.exportRgbImage(outputImageRasterViewport);

        lineSet.init(); // leaves buffer ready for next frame

        //- (2/2) Image result transfer to output file --------------------
        ImagePersistence.exportPNG(new File("output.png"), outputImageRasterViewport);
    }

    private static reportLineSetStats(lineSet: Calligraphic2DBuffer): void {
        let inside = 0;
        let minx: number = Double.POSITIVE_INFINITY;
        let miny: number = Double.POSITIVE_INFINITY;
        let maxx: number = Double.NEGATIVE_INFINITY;
        let maxy: number = Double.NEGATIVE_INFINITY;
        for (let i = 0; i < lineSet.getNumLines(); i++) {
            const seg: [Vector3Dd, Vector3Dd] = lineSet.get2DLine(i);
            const p0: Vector3Dd = seg[0];
            const p1: Vector3Dd = seg[1];
            minx = Math.min(minx, Math.min(p0.x(), p1.x()));
            miny = Math.min(miny, Math.min(p0.y(), p1.y()));
            maxx = Math.max(maxx, Math.max(p0.x(), p1.x()));
            maxy = Math.max(maxy, Math.max(p0.y(), p1.y()));
            if (
                p0.x() >= -1 &&
                p0.x() <= 1 &&
                p0.y() >= -1 &&
                p0.y() <= 1 &&
                p1.x() >= -1 &&
                p1.x() <= 1 &&
                p1.y() >= -1 &&
                p1.y() <= 1
            ) {
                inside++;
            }
        }
        console.log(
            "[WireframeOfflineExample] minx=" +
                Double.toString(minx) +
                " maxx=" +
                Double.toString(maxx) +
                " miny=" +
                Double.toString(miny) +
                " maxy=" +
                Double.toString(maxy) +
                " insideSegments=" +
                inside,
        );
    }

    public static main(_args: string[]): void {
        const instance: WireframeOfflineExample = new WireframeOfflineExample();
        let lineSet: Calligraphic2DBuffer;

        lineSet = new Calligraphic2DBuffer();
        WireframeRenderer.execute(lineSet, instance.scene.getSimpleBodies(), instance.camera);
        WireframeOfflineExample.reportLineSetStats(lineSet);
        instance.rasterOutput(lineSet);
    }
}

WireframeOfflineExample.main(process.argv.slice(2));
