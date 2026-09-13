import { TestBed } from '@angular/core/testing';
import { WebSystem } from '@vitral/webgl';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);

    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the explorer folders and WebGL API test file', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const labels = Array.from(
      host.querySelectorAll('.folder-item, .file-item'),
      (element: Element) => element.textContent?.trim(),
    );

    expect(labels).toEqual([
      '_APITests',
      '_WebGLHelloWorld',
      'WebGLExamples',
      'CameraExample',
      'ImageExample',
      'MeshExample',
      'SolidTextureExample',
      'MD2Example',
      'ShadersExample',
      'WebGPUExamples',
      'Tools',
      'ApplicationCases',
    ]);
  });

  it('should select a folder without adding content to the workspace', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const folder = Array.from(host.querySelectorAll<HTMLButtonElement>('.folder-item')).find(
      (element) => element.textContent?.includes('WebGLExamples') ?? false,
    );
    expect(folder).toBeTruthy();
    if (!folder) {
      throw new Error('WebGLExamples folder was not rendered');
    }

    folder.click();
    fixture.detectChanges();

    const workspace = host.querySelector('#workspace') as HTMLDivElement;
    expect(folder.getAttribute('aria-selected')).toBe('true');
    expect(workspace.dataset['selection']).toBe('WebGLExamples');
    expect(workspace.childElementCount).toBe(0);
  });

  it('should mount the WebGL hello world example from the API tests file', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const file = Array.from(host.querySelectorAll<HTMLButtonElement>('.file-item')).find(
      (element) => element.textContent?.includes('_WebGLHelloWorld') ?? false,
    );
    expect(file).toBeTruthy();
    if (!file) {
      throw new Error('_WebGLHelloWorld file was not rendered');
    }

    file.click();
    fixture.detectChanges();

    const workspace = host.querySelector('#workspace') as HTMLDivElement;
    expect(file.getAttribute('aria-selected')).toBe('true');
    expect(workspace.dataset['selection']).toBe('_WebGLHelloWorld');
    expect(workspace.querySelector('app-webgl-hello-world')).toBeTruthy();
  });

  it('should mount the WebGL camera example from the WebGLExamples folder', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const file = Array.from(host.querySelectorAll<HTMLButtonElement>('.file-item')).find(
      (element) => element.textContent?.includes('CameraExample') ?? false,
    );
    expect(file).toBeTruthy();
    if (!file) {
      throw new Error('CameraExample file was not rendered');
    }

    file.click();
    fixture.detectChanges();

    const workspace = host.querySelector('#workspace') as HTMLDivElement;
    expect(file.getAttribute('aria-selected')).toBe('true');
    expect(workspace.dataset['selection']).toBe('CameraExample');
    expect(workspace.querySelector('app-camera-example')).toBeTruthy();
  });

  it('should deactivate the mounted camera example with Escape', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const file = Array.from(host.querySelectorAll<HTMLButtonElement>('.file-item')).find(
      (element) => element.textContent?.includes('CameraExample') ?? false,
    );
    expect(file).toBeTruthy();
    if (!file) {
      throw new Error('CameraExample file was not rendered');
    }

    file.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const canvas = host.querySelector<HTMLCanvasElement>('app-camera-example canvas');
    expect(canvas).toBeTruthy();
    if (!canvas) {
      throw new Error('CameraExample canvas was not rendered');
    }

    canvas.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape' }));
    fixture.detectChanges();

    const workspace = host.querySelector('#workspace') as HTMLDivElement;
    expect(workspace.dataset['selection']).toBeUndefined();
    expect(workspace.querySelector('app-camera-example')).toBeFalsy();
  });

  it('should map shifted and unshifted camera-controller keys distinctly', () => {
    expect(
      WebSystem.web2vsdkKeyEvent(new KeyboardEvent('keydown', { key: 'x', code: 'KeyX' })).keycode,
    ).toBe('KEY_x');
    expect(
      WebSystem.web2vsdkKeyEvent(
        new KeyboardEvent('keydown', { key: 'X', code: 'KeyX', shiftKey: true }),
      ).keycode,
    ).toBe('KEY_X');
    expect(
      WebSystem.web2vsdkKeyEvent(new KeyboardEvent('keydown', { key: 's', code: 'KeyS' })).keycode,
    ).toBe('KEY_s');
    expect(
      WebSystem.web2vsdkKeyEvent(
        new KeyboardEvent('keydown', { key: 'S', code: 'KeyS', shiftKey: true }),
      ).keycode,
    ).toBe('KEY_S');
  });
});
