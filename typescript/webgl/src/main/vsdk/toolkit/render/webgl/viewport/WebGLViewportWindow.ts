//=   previous "JoglView" class at SceneEditorApplication example).         =

import {
    Arrow,
    Camera,
    ColorRgb,
    InputGizmo,
    JavaMath,
    Matrix4x4d,
    ReferenceFrameGizmo,
    RotateGizmo,
    Vector3Dd,
    Viewport,
    VSDK,
    type Geometry,
    type RendererConfiguration,
    type RGBAImageUncompressed,
    type SimpleBody,
    type TranslateGizmo,
    type ViewportElementScaler,
    type ViewportSet,
} from "@vitral/base";
import { WebGLImageRenderer } from "../WebGLImageRenderer.js";
import { WebGLInputGizmoRenderer } from "../WebGLInputGizmoRenderer.js";
import { WebGLLineRenderer } from "../WebGLLineRenderer.js";
import { WebGLReferenceFrameGizmoRenderer } from "../WebGLReferenceFrameGizmoRenderer.js";
import { WEBGL_LABEL_DEFAULT_FONT_SIZE, type WebGLLabelImageProvider } from "./WebGLLabelImageProvider.js";

/**
Port of `vsdk.toolkit.render.jogl.viewport.Jogl4ViewportWindow`.

WebGL presentation of one `Viewport` of a `ViewportSet`: grid, reference base,
title and gizmo labels. It holds the WebGL/image resources needed for that
drawing, and reads everything else (cameras, areas, selection) from the
injected model. It does not depend on the DOM: user interaction is processed
by the application, and label images are provided by the injected
`WebGLLabelImageProvider`. Drawing is asynchronous, as every WebGL renderer
of the toolkit is.
*/
export class WebGLViewportWindow {
    // Sizes, in pixels, designed for legacy resolutions; they are enlarged
    // for bigger screens by the element scaler of the viewport set
    private static readonly BASE_TITLE_FONT_SIZE: number = 14;
    private static readonly BASE_TITLE_BORDER_X: number = 4;
    private static readonly BASE_TITLE_BORDER_Y: number = 1;
    // A bit smaller than the titles
    private static readonly BASE_REFERENCE_FRAME_LABEL_FONT_SIZE: number = 12;
    private static readonly BASE_TRANSLATE_GIZMO_LABEL_FONT_SIZE: number = WEBGL_LABEL_DEFAULT_FONT_SIZE;
    // Position of the labels of the translate gizmo, with respect to the tip
    // of its arrows
    private static readonly BASE_TRANSLATE_GIZMO_LABEL_OFFSET_X: number = -3;
    private static readonly BASE_TRANSLATE_GIZMO_LABEL_OFFSET_Y: number = 12;
    // Text of the angle of the arc of the rotate gizmo
    private static readonly BASE_ROTATE_GIZMO_ARC_LABEL_FONT_SIZE: number = 16;

    // Each WebGLViewportWindow can call a different visualization algorithm
    public static readonly RENDER_MODE_ZBUFFER: number = Viewport.RENDER_MODE_Z_BUFFER;
    public static readonly RENDER_MODE_RAYTRACING: number = Viewport.RENDER_MODE_RAYTRACING;

    private readonly viewportSet: ViewportSet;
    private readonly viewport: Viewport;
    private readonly labelImageProvider: WebGLLabelImageProvider;

    private title: string | null = null;
    private titleColor: ColorRgb | null = null;
    private titleFontSize: number = 0;
    private titleImage: RGBAImageUncompressed | null = null;
    // Label images no longer used, whose textures must be released
    private readonly discardedLabelImages: RGBAImageUncompressed[] = [];
    private readonly referenceFrameGizmo: ReferenceFrameGizmo = new ReferenceFrameGizmo();
    private referenceFrameLabelImages: RGBAImageUncompressed[] | null = null;
    private referenceFrameLabelFontSize: number = 0;
    // Labels of the translate gizmo, by axis, normal and selected (yellow)
    private translateGizmoLabelImages: RGBAImageUncompressed[] | null = null;
    private translateGizmoSelectedLabelImages: RGBAImageUncompressed[] | null = null;
    private translateGizmoLabelFontSize: number = 0;
    // Label with the angle of the rotation arc of the rotate gizmo, that
    // changes as the arc does: it is created again when its text changes
    private rotateGizmoArcLabelImage: RGBAImageUncompressed | null = null;
    private rotateGizmoArcLabelText: string | null = null;
    private rotateGizmoArcLabelColor: ColorRgb | null = null;
    private rotateGizmoArcLabelFontSize: number = 0;
    private readonly inputGizmoRenderer: WebGLInputGizmoRenderer;

    public constructor(viewportSet: ViewportSet,
                       viewport: Viewport,
                       labelImageProvider: WebGLLabelImageProvider) {
        this.viewportSet = viewportSet;
        this.viewport = viewport;
        this.labelImageProvider = labelImageProvider;
        this.inputGizmoRenderer = new WebGLInputGizmoRenderer({
            createLabelImage: (text: string, color: ColorRgb, fontSize: number): RGBAImageUncompressed =>
                this.labelImageProvider.createLabelImage(text, color, fontSize),
            drawLabelImage: (gl: WebGL2RenderingContext, image: RGBAImageUncompressed, x: number, y: number): Promise<void> =>
                this.drawLabelImageInViewport(gl, image, x, y),
            discardLabelImage: (image: RGBAImageUncompressed): void => {
                this.discardedLabelImages.push(image);
            },
        }, viewportSet, viewport);

        this.updateTitleImage();
    }

    public getViewport(): Viewport {
        return this.viewport;
    }

    public getRenderMode(): number {
        return this.viewport.getRenderMode();
    }

    public getViewportStartX(): number {
        return this.viewport.getPixelStartX();
    }

    public getViewportStartY(): number {
        return this.viewport.getPixelStartY();
    }

    public getViewportSizeX(): number {
        return this.viewport.getPixelSizeX();
    }

    public getViewportSizeY(): number {
        return this.viewport.getPixelSizeY();
    }

    public isSelected(): boolean {
        return this.viewportSet.isSelected(this.viewport);
    }

    public isActive(): boolean {
        return this.viewport.isActive();
    }

    public getCamera(): Camera {
        return this.viewport.getActiveCamera();
    }

    public getRendererConfiguration(): RendererConfiguration {
        return this.viewport.getRendererConfiguration();
    }

    /**
    Draws the reference frame gizmo at the lower left corner of the viewport.
    Its size, line width and labels follow the screen resolution (see
    `ViewportElementScaler`), so it keeps a similar apparent size.
    */
    public async drawReferenceBase(gl: WebGL2RenderingContext): Promise<void> {
        const elementScaler: ViewportElementScaler = this.viewportSet.getElementScaler();

        this.referenceFrameGizmo.applyScale(elementScaler);
        this.updateReferenceFrameLabelImages(elementScaler);
        const labels: RGBAImageUncompressed[] = this.referenceFrameLabelImages!;

        await WebGLReferenceFrameGizmoRenderer.draw(gl, this.referenceFrameGizmo,
            this.viewport.getActiveCamera().getRotation(),
            this.viewport.getPixelStartX(), this.viewport.getPixelStartY(),
            {
                drawLabel: (glContext: WebGL2RenderingContext, axis: number, _label: string,
                            windowX: number, windowY: number): Promise<void> =>
                    this.drawLabelImage(glContext, labels[axis]!, windowX, windowY),
            });
    }

    /**
    The label images of the reference frame depend on the screen resolution,
    so they are regenerated when the font size they need changes.
    */
    private updateReferenceFrameLabelImages(elementScaler: ViewportElementScaler): void {
        const currentFontSize: number =
            elementScaler.scaleSize(WebGLViewportWindow.BASE_REFERENCE_FRAME_LABEL_FONT_SIZE);

        if (this.referenceFrameLabelImages !== null &&
            currentFontSize === this.referenceFrameLabelFontSize) {
            return;
        }
        if (this.referenceFrameLabelImages !== null) {
            for (const image of this.referenceFrameLabelImages) {
                this.discardedLabelImages.push(image);
            }
        }
        this.referenceFrameLabelFontSize = currentFontSize;
        this.referenceFrameLabelImages = [];
        for (let axis: number = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++) {
            this.referenceFrameLabelImages.push(this.labelImageProvider.createLabelImage(
                this.referenceFrameGizmo.getAxisLabel(axis),
                this.referenceFrameGizmo.getAxisColor(axis),
                currentFontSize));
        }
    }

    /**
    The label images of the translate gizmo depend on the screen resolution,
    so they are regenerated when the font size they need changes.
    */
    private updateTranslateGizmoLabelImages(elementScaler: ViewportElementScaler): void {
        const currentFontSize: number =
            elementScaler.scaleSize(WebGLViewportWindow.BASE_TRANSLATE_GIZMO_LABEL_FONT_SIZE);

        if (this.translateGizmoLabelImages !== null &&
            currentFontSize === this.translateGizmoLabelFontSize) {
            return;
        }
        if (this.translateGizmoLabelImages !== null && this.translateGizmoSelectedLabelImages !== null) {
            for (let axis: number = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++) {
                this.discardedLabelImages.push(this.translateGizmoLabelImages[axis]!);
                this.discardedLabelImages.push(this.translateGizmoSelectedLabelImages[axis]!);
            }
        }
        this.translateGizmoLabelFontSize = currentFontSize;
        this.translateGizmoLabelImages = [];
        this.translateGizmoSelectedLabelImages = [];
        for (let axis: number = 0; axis < ReferenceFrameGizmo.NUMBER_OF_AXES; axis++) {
            this.translateGizmoLabelImages.push(this.labelImageProvider.createLabelImage(
                this.referenceFrameGizmo.getAxisLabel(axis),
                this.referenceFrameGizmo.getAxisColor(axis),
                currentFontSize));
            this.translateGizmoSelectedLabelImages.push(this.labelImageProvider.createLabelImage(
                this.referenceFrameGizmo.getAxisLabel(axis),
                new ColorRgb(1, 1, 0),
                currentFontSize));
        }
    }

    private async drawGridRectangle(gl: WebGL2RenderingContext): Promise<void> {
        let gridTransform: Matrix4x4d = new Matrix4x4d();

        const R: Matrix4x4d = this.viewport.getActiveCamera().getRotation();
        const yaw: number = JavaMath.toDegrees(R.obtainEulerYawAngle());
        const pitch: number = JavaMath.toDegrees(R.obtainEulerPitchAngle());

        if (this.viewport.getActiveCamera().getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL &&
            (pitch > -45 && pitch < 45)) {
            if ((yaw > 45 && yaw < 135) ||
                (yaw < -45 && yaw > -135)) {
                gridTransform = new Matrix4x4d().axisRotation(JavaMath.toRadians(90), 1, 0, 0);
            }
            else {
                gridTransform = new Matrix4x4d().axisRotation(JavaMath.toRadians(90), 0, 1, 0);
            }
        }

        //-----------------------------------------------------------------
        const nx: number = 14; // Must be an even number
        const ny: number = 14; // Must be an even number
        const dx: number = 1.0;
        const dy: number = 1.0;
        const minx: number = -(nx / 2) * dx;
        const maxx: number = (nx / 2) * dx;
        const miny: number = -(ny / 2) * dy;
        const maxy: number = (ny / 2) * dy;

        const p: number[] = [];
        const c: number[] = [];

        for (let x: number = 0; x <= nx; x++) {
            if (x === nx / 2) continue;
            WebGLViewportWindow.addGridLine(p, c, minx + x * dx, miny, minx + x * dx, maxy, 0.37);
        }
        for (let y: number = 0; y <= ny; y++) {
            if (y === ny / 2) continue;
            WebGLViewportWindow.addGridLine(p, c, minx, minx + y * dy, maxx, minx + y * dy, 0.37);
        }
        WebGLViewportWindow.addGridLine(p, c, minx + Math.trunc(nx / 2) * dx, miny,
            minx + Math.trunc(nx / 2) * dx, maxy, 0.0);
        WebGLViewportWindow.addGridLine(p, c, minx, minx + Math.trunc(ny / 2) * dy,
            maxx, minx + Math.trunc(ny / 2) * dy, 0.0);

        gl.enable(gl.DEPTH_TEST);
        await WebGLLineRenderer.drawLines(gl,
            this.viewport.getActiveCamera().calculateProjectionMatrix().multiply(gridTransform),
            new Float32Array(p), new Float32Array(c), 1.0);
    }

    private static addGridLine(p: number[], c: number[],
                               x0: number, y0: number, x1: number, y1: number, gray: number): void {
        p.push(x0, y0, 0.0);
        p.push(x1, y1, 0.0);
        for (let i: number = 0; i < 2; i++) {
            c.push(gray, gray, gray);
        }
    }

    public toggleGrid(): void {
        this.viewport.toggleGrid();
    }

    public async drawGrid(gl: WebGL2RenderingContext): Promise<void> {
        //- Draw reference grid plane -------------------------------------
        if (this.viewport.isShowGrid()) await this.drawGridRectangle(gl);
    }

    /**
    Draws a label image with its upper left corner at a position of the
    viewport, measured in pixels from its upper left corner.
    */
    public async drawTextureString2D(gl: WebGL2RenderingContext, x: number, y: number,
                                     i: RGBAImageUncompressed): Promise<void> {
        await this.drawLabelImage(gl, i,
            this.viewport.getPixelStartX() + x,
            this.viewport.getPixelStartY() + (this.viewport.getPixelSizeY() - y));
    }

    /**
    Draws a label image with its lower left corner at a position of the
    surface, in window coordinates.

    The image is uploaded to a texture the first time it is used and drawn as a
    textured quad in window coordinates, over the whole surface of the
    viewport set, with an explicitly configured state (alpha blending, no
    depth test). Both the color and the transparency of the label come only
    from the image. The GL viewport does not clip 2D quads drawn over the whole
    surface, so the label is clipped to the area of this viewport with the
    scissor test, and the parts that fall outside are not drawn over its
    neighbors. The state changed is restored.

    @param gl WebGL context
    @param image label image
    @param windowX horizontal position of the lower left corner
    @param windowY vertical position of the lower left corner, from the bottom
    */
    private async drawLabelImage(gl: WebGL2RenderingContext, image: RGBAImageUncompressed,
                                 windowX: number, windowY: number): Promise<void> {
        const currentViewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;

        let surfaceWidth: number = this.viewportSet.getSizeXInPixels();
        let surfaceHeight: number = this.viewportSet.getSizeYInPixels();
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            surfaceWidth = currentViewport[0]! + currentViewport[2]!;
            surfaceHeight = currentViewport[1]! + currentViewport[3]!;
        }

        this.releaseDiscardedLabelTextures(gl);
        const texture: WebGLTexture | null = WebGLImageRenderer.activate(gl, image);
        const x0: number = JavaMath.round(windowX);
        const y0: number = JavaMath.round(windowY);
        const x1: number = x0 + image.getXSize();
        const y1: number = y0 + image.getYSize();

        // Window coordinates to clip space
        const toClip: Matrix4x4d = Matrix4x4d.identityMatrix()
            .withVal(0, 0, 2.0 / surfaceWidth).withVal(0, 3, -1.0)
            .withVal(1, 1, 2.0 / surfaceHeight).withVal(1, 3, -1.0);
        const positions: Float32Array = new Float32Array([
            x0, y0, 0,
            x1, y0, 0,
            x1, y1, 0,
            x0, y0, 0,
            x1, y1, 0,
            x0, y1, 0,
        ]);
        const uvs: Float32Array = new Float32Array([
            0, 0,  1, 0,  1, 1,
            0, 0,  1, 1,  0, 1,
        ]);

        const scissorWasEnabled: boolean = gl.isEnabled(gl.SCISSOR_TEST);
        const previousScissor: Int32Array = gl.getParameter(gl.SCISSOR_BOX) as Int32Array;

        gl.viewport(0, 0, surfaceWidth, surfaceHeight);
        gl.enable(gl.SCISSOR_TEST);
        gl.scissor(this.viewport.getPixelStartX(), this.viewport.getPixelStartY(),
            this.viewport.getPixelSizeX(), this.viewport.getPixelSizeY());
        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

        await WebGLImageRenderer.drawTexturedQuad(gl, texture, toClip, positions, uvs, 1.0, 1.0, 1.0);

        gl.disable(gl.BLEND);
        gl.enable(gl.DEPTH_TEST);
        gl.scissor(previousScissor[0]!, previousScissor[1]!, previousScissor[2]!, previousScissor[3]!);
        if (!scissorWasEnabled) {
            gl.disable(gl.SCISSOR_TEST);
        }
        gl.viewport(currentViewport[0]!, currentViewport[1]!, currentViewport[2]!, currentViewport[3]!);
    }

    /**
    Draws a label image with its lower left corner at a position of the
    viewport, measured in pixels from its lower left corner.
    */
    private async drawLabelImageInViewport(gl: WebGL2RenderingContext, image: RGBAImageUncompressed,
                                           x: number, y: number): Promise<void> {
        await this.drawLabelImage(gl, image, this.viewport.getPixelStartX() + x, this.viewport.getPixelStartY() + y);
    }

    /**
    Releases the textures of the label images that are no longer used.
    */
    private releaseDiscardedLabelTextures(gl: WebGL2RenderingContext): void {
        for (const discarded of this.discardedLabelImages) {
            WebGLImageRenderer.unload(gl, discarded);
        }
        this.discardedLabelImages.length = 0;
    }

    /**
    Deletes the WebGL resources (label textures) of this window.
    @param gl WebGL context about to be discarded
    */
    public disposeGlResources(gl: WebGL2RenderingContext): void {
        this.releaseDiscardedLabelTextures(gl);
        this.inputGizmoRenderer.disposeGlResources(gl);
        if (this.titleImage !== null) {
            WebGLImageRenderer.unload(gl, this.titleImage);
        }
        if (this.referenceFrameLabelImages !== null) {
            for (const image of this.referenceFrameLabelImages) {
                WebGLImageRenderer.unload(gl, image);
            }
        }
        if (this.translateGizmoLabelImages !== null && this.translateGizmoSelectedLabelImages !== null) {
            for (let axis: number = 0; axis < this.translateGizmoLabelImages.length; axis++) {
                WebGLImageRenderer.unload(gl, this.translateGizmoLabelImages[axis]!);
                WebGLImageRenderer.unload(gl, this.translateGizmoSelectedLabelImages[axis]!);
            }
        }
        if (this.rotateGizmoArcLabelImage !== null) {
            WebGLImageRenderer.unload(gl, this.rotateGizmoArcLabelImage);
        }
    }

    /**
    Forgets the pending release of label textures, because the context that
    owned them is gone. The label images themselves are kept, and their
    textures are created again when needed.
    */
    public invalidateGlResources(): void {
        this.discardedLabelImages.length = 0;
    }

    public async drawTitle(gl: WebGL2RenderingContext): Promise<void> {
        this.updateTitleImage();

        const elementScaler: ViewportElementScaler = this.viewportSet.getElementScaler();
        const borderx: number = elementScaler.scaleSize(WebGLViewportWindow.BASE_TITLE_BORDER_X);
        const bordery: number = elementScaler.scaleSize(WebGLViewportWindow.BASE_TITLE_BORDER_Y);
        const titleImage: RGBAImageUncompressed = this.titleImage!;

        // The area of the title (its border included, so it can be easily
        // pointed) is informed to the model, for interaction
        this.viewport.setTitleArea(0, 0,
            titleImage.getXSize() + 2 * borderx, titleImage.getYSize() + 2 * bordery);

        await this.drawTextureString2D(gl, borderx, titleImage.getYSize() + bordery, titleImage);
    }

    /**
    The title is given by the viewport set (it follows the active camera and
    the language selected by the user), its color is configured in the viewport
    set (it depends on whether the viewport is selected) and its size depends
    on the screen resolution (see `ViewportElementScaler`), so the image is
    regenerated whenever any of them changes.
    */
    private updateTitleImage(): void {
        const currentTitle: string = this.viewportSet.getTitleFor(this.viewport);
        const currentColor: ColorRgb = this.viewportSet.getTitleColorFor(this.viewport);
        const currentFontSize: number =
            this.viewportSet.getElementScaler().scaleSize(WebGLViewportWindow.BASE_TITLE_FONT_SIZE);

        if (this.titleImage === null || currentTitle !== this.title ||
            !currentColor.equals(this.titleColor) ||
            currentFontSize !== this.titleFontSize) {
            this.title = currentTitle;
            this.titleColor = currentColor;
            this.titleFontSize = currentFontSize;
            if (this.titleImage !== null) {
                this.discardedLabelImages.push(this.titleImage);
            }
            this.titleImage = this.labelImageProvider.createLabelImage(this.title, this.titleColor, this.titleFontSize);
        }
    }

    public async drawLabelsForTranslateGizmo(gl: WebGL2RenderingContext, gizmo: TranslateGizmo): Promise<void> {
        const elementScaler: ViewportElementScaler = this.viewportSet.getElementScaler();
        const offsetX: number =
            JavaMath.round(elementScaler.scaleLength(WebGLViewportWindow.BASE_TRANSLATE_GIZMO_LABEL_OFFSET_X));
        const offsetY: number =
            JavaMath.round(elementScaler.scaleLength(WebGLViewportWindow.BASE_TRANSLATE_GIZMO_LABEL_OFFSET_Y));

        this.updateTranslateGizmoLabelImages(elementScaler);

        const things: SimpleBody[] = gizmo.getElements();
        let lv: Vector3Dd = new Vector3Dd();
        const c: ColorRgb = new ColorRgb(1, 1, 0);

        for (let i: number = 0; i < things.length && i < 3; i++) {
            const r: SimpleBody = things[i]!;
            const g: Geometry | null = r.getGeometry();

            if (g !== null) {
                lv = new Vector3Dd(0, 0, lv.z());
                if (g instanceof Arrow) {
                    lv = lv.withZ((g.getHeadLength() + g.getBaseLength()) * 1.1);
                }
                else {
                    lv = lv.withZ(1);
                }

                let R: Matrix4x4d = new Matrix4x4d();
                R = R.translation(r.getPosition());
                R = R.multiply(r.getRotation());
                const p: Vector3Dd = R.multiply(lv);
                const tp: Vector3Dd | null = this.viewport.getActiveCamera().projectPointUsingRayMethodResult(p);

                if (tp !== null) {
                    const yellow: boolean = ColorRgb.distance(c, r.getMaterial()!.getDiffuse()) < VSDK.EPSILON;

                    await this.drawTextureString2D(gl,
                        Math.trunc(tp.x()) + offsetX,
                        Math.trunc(tp.y()) + offsetY,
                        yellow ? this.translateGizmoSelectedLabelImages![i]! : this.translateGizmoLabelImages![i]!);
                }
            }
        }
    }

    /**
    Draws the label with the angle, in degrees, of the rotation arc of a
    rotate gizmo (if it is showing one), next to the middle of the arc and
    with its color.
    @param gl WebGL context
    @param gizmo the gizmo
    */
    public async drawLabelForRotateGizmoArc(gl: WebGL2RenderingContext, gizmo: RotateGizmo): Promise<void> {
        const anchor: Vector3Dd | null = gizmo.getArcLabelPosition();

        if (anchor === null) {
            if (this.rotateGizmoArcLabelImage !== null) {
                this.discardedLabelImages.push(this.rotateGizmoArcLabelImage);
                this.rotateGizmoArcLabelImage = null;
                this.rotateGizmoArcLabelText = null;
            }
            return;
        }

        const projected: Vector3Dd | null = this.viewport.getActiveCamera().projectPointUsingRayMethodResult(anchor);
        const fontSize: number =
            this.viewportSet.getElementScaler().scaleSize(WebGLViewportWindow.BASE_ROTATE_GIZMO_ARC_LABEL_FONT_SIZE);
        const text: string = InputGizmo.format(gizmo.getArcSweepInDegrees(), RotateGizmo.ANGLE_DECIMALS) + "°";
        const color: ColorRgb = gizmo.getArcColor();

        if (this.rotateGizmoArcLabelImage === null || fontSize !== this.rotateGizmoArcLabelFontSize ||
            text !== this.rotateGizmoArcLabelText || !color.equals(this.rotateGizmoArcLabelColor)) {
            if (this.rotateGizmoArcLabelImage !== null) {
                this.discardedLabelImages.push(this.rotateGizmoArcLabelImage);
            }
            this.rotateGizmoArcLabelImage = this.labelImageProvider.createLabelImage(text, color, fontSize);
            this.rotateGizmoArcLabelText = text;
            this.rotateGizmoArcLabelColor = color;
            this.rotateGizmoArcLabelFontSize = fontSize;
        }
        if (projected !== null) {
            const label: RGBAImageUncompressed = this.rotateGizmoArcLabelImage;
            // Centered at the projected point
            await this.drawTextureString2D(gl,
                Math.trunc(projected.x()) - Math.trunc(label.getXSize() / 2),
                Math.trunc(projected.y()) + Math.trunc(label.getYSize() / 2),
                label);
        }
    }

    /**
    Draws an input gizmo (numeric boxes the user can edit) at the lower right
    corner of the viewport.
    @param gl WebGL context
    @param gizmo the gizmo
    */
    public async drawInputGizmo(gl: WebGL2RenderingContext, gizmo: InputGizmo): Promise<void> {
        await this.inputGizmoRenderer.draw(gl, gizmo);
    }
}
