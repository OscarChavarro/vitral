/**
A frame buffer object with a color attachment and a depth texture, of the
size of a canvas, that a WebGL application draws into and then presents on
the canvas with `presentToCanvas`.

It has no Java counterpart: it exists because of a runtime boundary. A JOGL
application reads the depth buffer of its window with `glReadPixels`
(`Jogl4FrameBufferReader.readDepth`), which WebGL does not allow for any
frame buffer. Drawing into a depth *texture* instead lets
`WebGLFrameBufferReader.readDepth` sample it. It also keeps the drawn frame
readable after the browser composites the canvas (the default frame buffer is
cleared then, unless the context was created with `preserveDrawingBuffer`).
*/
export class WebGLOffscreenFrameBuffer {
    private readonly gl: WebGL2RenderingContext;
    private frameBuffer: WebGLFramebuffer | null = null;
    private colorBuffer: WebGLRenderbuffer | null = null;
    private depthTexture: WebGLTexture | null = null;
    private width: number = 0;
    private height: number = 0;

    /**
    @param gl context the frame buffer belongs to
    */
    public constructor(gl: WebGL2RenderingContext) {
        this.gl = gl;
    }

    /**
    Makes the frame buffer of the given size (recreating its attachments when
    the size changes), and binds it for drawing and reading.

    @param width width in pixels
    @param height height in pixels
    */
    public bind(width: number, height: number): void {
        const gl: WebGL2RenderingContext = this.gl;
        const w: number = Math.max(1, Math.floor(width));
        const h: number = Math.max(1, Math.floor(height));

        if (this.frameBuffer === null || w !== this.width || h !== this.height) {
            this.release();
            this.create(w, h);
        }
        gl.bindFramebuffer(gl.FRAMEBUFFER, this.frameBuffer);
    }

    /**
    Copies the color of the frame buffer to the canvas (the default frame
    buffer), and leaves the default frame buffer bound.
    */
    public presentToCanvas(): void {
        const gl: WebGL2RenderingContext = this.gl;

        if (this.frameBuffer === null) {
            return;
        }
        gl.bindFramebuffer(gl.READ_FRAMEBUFFER, this.frameBuffer);
        gl.bindFramebuffer(gl.DRAW_FRAMEBUFFER, null);
        gl.blitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height,
            gl.COLOR_BUFFER_BIT, gl.NEAREST);
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    }

    /**
    @return the frame buffer object, or null before the first `bind`
    */
    public getFrameBuffer(): WebGLFramebuffer | null {
        return this.frameBuffer;
    }

    /**
    @return the depth texture (`DEPTH_COMPONENT24`), or null before the first
    `bind`
    */
    public getDepthTexture(): WebGLTexture | null {
        return this.depthTexture;
    }

    /**
    @return width in pixels
    */
    public getWidth(): number {
        return this.width;
    }

    /**
    @return height in pixels
    */
    public getHeight(): number {
        return this.height;
    }

    /**
    Releases the WebGL objects of the frame buffer.
    */
    public release(): void {
        const gl: WebGL2RenderingContext = this.gl;

        if (this.frameBuffer !== null) {
            gl.deleteFramebuffer(this.frameBuffer);
        }
        if (this.colorBuffer !== null) {
            gl.deleteRenderbuffer(this.colorBuffer);
        }
        if (this.depthTexture !== null) {
            gl.deleteTexture(this.depthTexture);
        }
        this.frameBuffer = null;
        this.colorBuffer = null;
        this.depthTexture = null;
        this.width = 0;
        this.height = 0;
    }

    private create(width: number, height: number): void {
        const gl: WebGL2RenderingContext = this.gl;

        this.colorBuffer = gl.createRenderbuffer();
        gl.bindRenderbuffer(gl.RENDERBUFFER, this.colorBuffer);
        gl.renderbufferStorage(gl.RENDERBUFFER, gl.RGBA8, width, height);
        gl.bindRenderbuffer(gl.RENDERBUFFER, null);

        this.depthTexture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, this.depthTexture);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.DEPTH_COMPONENT24, width, height, 0,
            gl.DEPTH_COMPONENT, gl.UNSIGNED_INT, null);
        gl.bindTexture(gl.TEXTURE_2D, null);

        this.frameBuffer = gl.createFramebuffer();
        gl.bindFramebuffer(gl.FRAMEBUFFER, this.frameBuffer);
        gl.framebufferRenderbuffer(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.RENDERBUFFER, this.colorBuffer);
        gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.TEXTURE_2D, this.depthTexture, 0);
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);

        this.width = width;
        this.height = height;
    }
}
