import { Matrix4x4d, type Viewport, type ViewportSet } from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "../WebGLColoredPrimitiveRenderer.js";
import type { WebGLLabelImageProvider } from "./WebGLLabelImageProvider.js";
import { WebGLViewportWindow } from "./WebGLViewportWindow.js";

/**
What the application draws inside each viewport (Java's nested interface
`Jogl4ViewportSetRenderer.ViewRenderer`).
*/
export interface WebGLViewRenderer {
    configureView(view: WebGLViewportWindow): void;
    drawView(gl: WebGL2RenderingContext, view: WebGLViewportWindow): Promise<void>;
}

/**
Port of `vsdk.toolkit.render.jogl.viewport.Jogl4ViewportSetRenderer`.

WebGL presentation of a `ViewportSet`: it activates the GL viewport of each
`Viewport`, draws the lines bordering them (highlighting the selected one)
and delegates the drawing of the scene inside each viewport to a
`WebGLViewRenderer`.

There is one renderer for each `ViewportSet`, so an application working with
several displays uses one renderer per display. The model is injected in the
constructor, and it is not modified by the rendering, except for the pixel
areas of the viewports, which depend on the GL surface size. Java's
`IdentityHashMap` is a `Map` (identity keys); drawing is asynchronous, as
every WebGL renderer of the toolkit is.
*/
export class WebGLViewportSetRenderer {
    private readonly viewportSet: ViewportSet;
    private readonly viewRenderer: WebGLViewRenderer;
    private readonly labelImageProvider: WebGLLabelImageProvider;
    private readonly windows: Map<Viewport, WebGLViewportWindow>;

    public constructor(viewportSet: ViewportSet,
                       labelImageProvider: WebGLLabelImageProvider,
                       viewRenderer: WebGLViewRenderer) {
        this.viewportSet = viewportSet;
        this.labelImageProvider = labelImageProvider;
        this.viewRenderer = viewRenderer;
        this.windows = new Map<Viewport, WebGLViewportWindow>();
    }

    public getViewportSet(): ViewportSet {
        return this.viewportSet;
    }

    /**
    @param viewport a viewport of the rendered set
    @return the WebGL window holding the drawing resources of the viewport
    */
    public getWindow(viewport: Viewport): WebGLViewportWindow {
        let window: WebGLViewportWindow | undefined = this.windows.get(viewport);

        if (window === undefined) {
            window = new WebGLViewportWindow(this.viewportSet, viewport, this.labelImageProvider);
            this.windows.set(viewport, window);
        }
        return window;
    }

    /**
    @return the window of the selected viewport, or null if the set is empty
    */
    public getSelectedWindow(): WebGLViewportWindow | null {
        const viewport: Viewport | null = this.viewportSet.getSelectedViewport();

        if (viewport === null) {
            return null;
        }
        return this.getWindow(viewport);
    }

    /**
    PRE: the GL viewport is set to the full set area.
    @param gl WebGL context
    @param fullScreenGuiMode true if the GUI is in full screen mode
    */
    public async draw(gl: WebGL2RenderingContext, fullScreenGuiMode: boolean): Promise<void> {
        this.forgetRemovedViewports();

        if (this.viewportSet.countActiveViewports() === 1 && fullScreenGuiMode) {
            await this.drawSelectedViewFullScreen(gl);
        }
        else {
            await this.drawMultipleViews(gl);
        }
    }

    /**
    Releases the WebGL resources of all the windows.
    @param gl WebGL context about to be discarded
    */
    public disposeGlResources(gl: WebGL2RenderingContext): void {
        for (const window of this.windows.values()) {
            window.disposeGlResources(gl);
        }
    }

    /**
    Forgets the WebGL resources of all the windows, because the context that
    owned them no longer exists (i.e. a new canvas). They are created again
    when needed.
    */
    public invalidateGlResources(): void {
        for (const window of this.windows.values()) {
            window.invalidateGlResources();
        }
    }

    private forgetRemovedViewports(): void {
        if (this.windows.size > this.viewportSet.getViewportCount()) {
            const kept: Set<Viewport> = new Set<Viewport>(this.viewportSet.getViewports());
            for (const viewport of [...this.windows.keys()]) {
                if (!kept.has(viewport)) {
                    this.windows.delete(viewport);
                }
            }
        }
    }

    private async drawMultipleViews(gl: WebGL2RenderingContext): Promise<void> {
        for (const viewport of this.viewportSet.getViewports()) {
            await this.drawBorder(gl, viewport);
        }

        for (const viewport of this.viewportSet.getViewports()) {
            if (!viewport.isActive()) {
                continue;
            }

            const view: WebGLViewportWindow = this.getWindow(viewport);
            this.activateViewport(gl, viewport);
            if (view.isSelected()) {
                this.viewRenderer.configureView(view);
            }
            await this.viewRenderer.drawView(gl, view);
            await view.drawTitle(gl);
        }
    }

    private async drawSelectedViewFullScreen(gl: WebGL2RenderingContext): Promise<void> {
        for (const viewport of this.viewportSet.getViewports()) {
            if (!viewport.isActive() || !this.viewportSet.isSelected(viewport)) {
                continue;
            }

            const view: WebGLViewportWindow = this.getWindow(viewport);
            this.activateViewport(gl, viewport);
            gl.viewport(0, 0, this.viewportSet.getSizeXInPixels(), this.viewportSet.getSizeYInPixels());
            this.viewRenderer.configureView(view);
            await this.viewRenderer.drawView(gl, view);
            await view.drawTitle(gl);
        }
    }

    private activateViewport(gl: WebGL2RenderingContext, viewport: Viewport): void {
        viewport.updatePixelArea(this.viewportSet.getSizeXInPixels(), this.viewportSet.getSizeYInPixels());
        gl.viewport(viewport.getPixelStartX(), viewport.getPixelStartY(),
            viewport.getPixelSizeX(), viewport.getPixelSizeY());
    }

    /**
    Draws the line bordering a viewport, in a highlight color if the viewport
    is the selected one.
    PRE: the GL viewport is set to the full set area.
    */
    private async drawBorder(gl: WebGL2RenderingContext, viewport: Viewport): Promise<void> {
        if (!viewport.isActive() || viewport.getBorder() <= 0) {
            return;
        }
        const epsilonx: number = 2.0 / this.viewportSet.getSizeXInPixels();
        const epsilony: number = 2.0 / this.viewportSet.getSizeYInPixels();
        const dx: number = viewport.getBorder() * epsilonx;
        const dy: number = viewport.getBorder() * epsilony;
        const x1: number = viewport.getStartXPercent() * 2 - 1;
        const y1: number = viewport.getStartYPercent() * 2 - 1;
        const x2: number = x1 + viewport.getSizeXPercent() * 2;
        const y2: number = y1 + viewport.getSizeYPercent() * 2;
        const outer: number[] = this.viewportSet.isSelected(viewport)
            ? [1.0, 0.96, 0.0]
            : [0.21, 0.25, 0.29];
        const positions: Float32Array = new Float32Array([
            x1, y1, 0,
            x2, y1, 0,
            x2, y2, 0,
            x1, y1, 0,
            x2, y2, 0,
            x1, y2, 0,
            x1 + 2 * dx, y1 + 2 * dy, 0,
            x2 - 2 * dx, y1 + 2 * dy, 0,
            x2 - 2 * dx, y2 - 2 * dy, 0,
            x1 + 2 * dx, y1 + 2 * dy, 0,
            x2 - 2 * dx, y2 - 2 * dy, 0,
            x1 + 2 * dx, y2 - 2 * dy, 0,
        ]);
        const colors: Float32Array = new Float32Array(12 * 4);

        for (let i: number = 0; i < 12; i++) {
            const c: number[] = i < 6 ? outer : [0, 0, 0];
            colors[4 * i] = c[0]!;
            colors[4 * i + 1] = c[1]!;
            colors[4 * i + 2] = c[2]!;
            colors[4 * i + 3] = 1.0;
        }
        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);
        await WebGLColoredPrimitiveRenderer.draw(gl, Matrix4x4d.identityMatrix(),
            gl.TRIANGLES, positions, colors);
        gl.enable(gl.DEPTH_TEST);
    }
}
