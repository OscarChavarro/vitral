import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  signal,
} from '@angular/core';
import { WebGLShaderPreprocessor, type WebGLShaderKind } from '@vitral/webgl';

const SHADER_BASE_URL = '/etc/glslShaders';

@Component({
  selector: 'app-webgl-hello-world',
  templateUrl: './webgl-hello-world.html',
  styleUrl: './webgl-hello-world.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WebGLHelloWorld implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  protected readonly statusMessage = signal<string | null>(null);

  private gl: WebGL2RenderingContext | null = null;
  private shaderProgram: WebGLProgram | null = null;
  private vertexArray: WebGLVertexArrayObject | null = null;
  private vertexBuffer: WebGLBuffer | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private disposed = false;

  async ngAfterViewInit(): Promise<void> {
    try {
      await this.initialize();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.disposed = true;
    this.resizeObserver?.disconnect();
    this.releaseResources();
  }

  private async initialize(): Promise<void> {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true });
    if (!gl) {
      throw new Error('This example requires WebGL2 support.');
    }

    this.gl = gl;
    this.shaderProgram = await this.createShaderProgram(gl);
    if (this.disposed) {
      return;
    }

    this.createGeometry(gl);
    gl.useProgram(this.shaderProgram);
    this.setShaderUniforms(gl, this.shaderProgram);
    gl.useProgram(null);

    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(canvas);
    this.draw();
  }

  private async createShaderProgram(gl: WebGL2RenderingContext): Promise<WebGLProgram> {
    const [vertexSource, fragmentSource] = await Promise.all([
      this.readShaderSource('constantVertexShader.glsl', 'vertex'),
      this.readShaderSource('constantPixelShader.glsl', 'fragment'),
    ]);

    const vertexShader = this.compileShader(gl, gl.VERTEX_SHADER, vertexSource);
    const fragmentShader = this.compileShader(gl, gl.FRAGMENT_SHADER, fragmentSource);
    const program = gl.createProgram();
    if (!program) {
      throw new Error('Failed to create WebGL shader program.');
    }

    gl.attachShader(program, vertexShader);
    gl.attachShader(program, fragmentShader);
    gl.linkProgram(program);

    gl.deleteShader(vertexShader);
    gl.deleteShader(fragmentShader);

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      const log = gl.getProgramInfoLog(program) ?? '(no log)';
      gl.deleteProgram(program);
      throw new Error(`Program link error: ${log}`);
    }

    return program;
  }

  private compileShader(
    gl: WebGL2RenderingContext,
    shaderType: number,
    source: string,
  ): WebGLShader {
    const shader = gl.createShader(shaderType);
    if (!shader) {
      throw new Error('Failed to create WebGL shader.');
    }

    gl.shaderSource(shader, source);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      const log = gl.getShaderInfoLog(shader) ?? '(no log)';
      gl.deleteShader(shader);
      throw new Error(`Shader compile error: ${log}`);
    }

    return shader;
  }

  private createGeometry(gl: WebGL2RenderingContext): void {
    const vertexData = new Float32Array([-0.8, -0.8, 0.0, 0.8, 0.8, 0.0]);

    this.vertexArray = gl.createVertexArray();
    this.vertexBuffer = gl.createBuffer();
    if (!this.vertexArray || !this.vertexBuffer) {
      throw new Error('Failed to create WebGL geometry buffers.');
    }

    gl.bindVertexArray(this.vertexArray);
    gl.bindBuffer(gl.ARRAY_BUFFER, this.vertexBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, vertexData, gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 3 * Float32Array.BYTES_PER_ELEMENT, 0);
    gl.bindBuffer(gl.ARRAY_BUFFER, null);
    gl.bindVertexArray(null);
  }

  private setShaderUniforms(gl: WebGL2RenderingContext, program: WebGLProgram): void {
    const identity = new Float32Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]);

    const modelViewProjectionLocalLoc = gl.getUniformLocation(program, 'modelViewProjectionLocal');
    const withTextureLoc = gl.getUniformLocation(program, 'withTexture');
    const withVertexColorsLoc = gl.getUniformLocation(program, 'withVertexColors');
    const diffuseColorLoc = gl.getUniformLocation(program, 'diffuseColor');

    if (modelViewProjectionLocalLoc) {
      gl.uniformMatrix4fv(modelViewProjectionLocalLoc, false, identity);
    }
    if (withTextureLoc) {
      gl.uniform1i(withTextureLoc, 0);
    }
    if (withVertexColorsLoc) {
      gl.uniform1i(withVertexColorsLoc, 0);
    }
    if (diffuseColorLoc) {
      gl.uniform3f(diffuseColorLoc, 1.0, 1.0, 1.0);
    }
  }

  private async readShaderSource(
    shaderFileName: string,
    shaderKind: WebGLShaderKind,
  ): Promise<string> {
    const response = await fetch(`${SHADER_BASE_URL}/${shaderFileName}`);
    if (!response.ok) {
      throw new Error(`Shader not found: ${shaderFileName}`);
    }
    return WebGLShaderPreprocessor.preprocess(await response.text(), shaderKind);
  }

  private draw(): void {
    if (!this.gl || !this.shaderProgram || !this.vertexArray) {
      return;
    }

    const canvas = this.canvasRef.nativeElement;
    const width = Math.max(1, Math.floor(canvas.clientWidth * window.devicePixelRatio));
    const height = Math.max(1, Math.floor(canvas.clientHeight * window.devicePixelRatio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }

    const gl = this.gl;
    gl.viewport(0, 0, width, height);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT);
    gl.useProgram(this.shaderProgram);
    gl.bindVertexArray(this.vertexArray);
    gl.lineWidth(1.0);
    gl.drawArrays(gl.LINES, 0, 2);
    gl.bindVertexArray(null);
    gl.useProgram(null);
  }

  private releaseResources(): void {
    if (!this.gl) {
      return;
    }

    if (this.vertexBuffer) {
      this.gl.deleteBuffer(this.vertexBuffer);
      this.vertexBuffer = null;
    }
    if (this.vertexArray) {
      this.gl.deleteVertexArray(this.vertexArray);
      this.vertexArray = null;
    }
    if (this.shaderProgram) {
      this.gl.deleteProgram(this.shaderProgram);
      this.shaderProgram = null;
    }
    this.gl = null;
  }
}
