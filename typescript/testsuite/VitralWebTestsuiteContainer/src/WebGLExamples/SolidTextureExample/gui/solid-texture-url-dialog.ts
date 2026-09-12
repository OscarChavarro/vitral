import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';

/**
 * What the user chooses before `SolidTextureExample` starts: the geometry the
 * solid texture will be carved into, and the tangible-interface service to
 * listen to.
 */
export interface SolidTextureSelection {
  readonly meshUrl: string;
  readonly tangibleServiceUrl: string;
  readonly tangibleEnabled: boolean;
}

/**
 * Browser counterpart of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/awt/FileSelectorDialog.java`,
 * carrying as well what the Java program takes from its command line.
 *
 * The Java file is `MeshExample`'s own, duplicated into this project with one
 * string changed ("Ply Files" against "PLY Files"), and this dialog is
 * duplicated from `MeshExample`'s for the same reason: the container maps one
 * Angular module onto one Java project, and each project owns its chooser.
 *
 * Java opens a `JFileChooser` on `etc/geometry` and narrows it with one
 * `awt.ObjectFilter` per geometry format. A browser has no file system to
 * browse, so the resource is named by URL and the filter list becomes the
 * suffix hint below: the same six formats, in the same order, with the same
 * descriptions. Only `.obj` has a reader in the TypeScript edition to date,
 * which the hint says.
 *
 * `selectGeometryFile` answers null when the user dismisses the chooser, and
 * the caller then reports "File not specified"; the {@link cancel} output is
 * that same null.
 *
 * The second section is the browser's place for what
 * `options.CommandLineOptions` reads from `-tangibleServer`: a page has no
 * command line, and this dialog is where the Java program's startup arguments
 * can be given. It adds one control Java has no counterpart for, the connect
 * checkbox. Java's `main` always builds a `TangibleInterfaceNetworkClient` and
 * calls `run()`, which costs nothing when no server answers because the
 * connection attempt happens on its own thread; a browser `WebSocket` to an
 * absent server instead logs a failed-connection error on the page's own
 * console every time the module starts. The checkbox therefore starts
 * **unchecked**, so the connection is made only when the user has a
 * `TangibleInterfaceMarkersDetectorServer` running and says so.
 */
@Component({
  selector: 'app-solid-texture-url-dialog',
  templateUrl: './solid-texture-url-dialog.html',
  styleUrl: './solid-texture-url-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SolidTextureUrlDialog {
  protected static readonly DEFAULT_URL = '/etc/geometry/cow.obj';

  /**
   * The same default `model.SolidTextureModel` gives its `tangibleServiceUrl`
   * field.
   */
  protected static readonly DEFAULT_TANGIBLE_SERVICE_URL = 'ws://localhost:8090/v1/values';

  protected readonly url = signal<string>(SolidTextureUrlDialog.DEFAULT_URL);
  protected readonly tangibleServiceUrl = signal<string>(
    SolidTextureUrlDialog.DEFAULT_TANGIBLE_SERVICE_URL,
  );
  protected readonly tangibleEnabled = signal<boolean>(false);

  @Input()
  set initialUrl(value: string | null) {
    this.url.set(value ?? SolidTextureUrlDialog.DEFAULT_URL);
  }

  @Input()
  set initialTangibleServiceUrl(value: string | null) {
    this.tangibleServiceUrl.set(value ?? SolidTextureUrlDialog.DEFAULT_TANGIBLE_SERVICE_URL);
  }

  @Input()
  set initialTangibleEnabled(value: boolean) {
    this.tangibleEnabled.set(value);
  }

  @Output()
  readonly accept = new EventEmitter<SolidTextureSelection>();

  @Output()
  readonly cancel = new EventEmitter<void>();

  protected readonly suffixHints: readonly string[] = [
    'obj — Obj Files (ported)',
    '3ds — 3ds Files',
    'ply — Ply Files',
    'wrl — VRML Files (exported from Renderpark only)',
    'ase — 3ds Files (Ascii Scene Export)',
    'vtk — kitware’s VTK legacy binary file',
  ];

  protected onUrlInput(event: Event): void {
    this.url.set((event.target as HTMLInputElement).value);
  }

  protected onTangibleServiceUrlInput(event: Event): void {
    this.tangibleServiceUrl.set((event.target as HTMLInputElement).value);
  }

  protected onTangibleEnabledChange(event: Event): void {
    this.tangibleEnabled.set((event.target as HTMLInputElement).checked);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    const meshUrl = this.url().trim();
    if (meshUrl.length === 0) {
      return;
    }
    this.accept.emit({
      meshUrl,
      // `SolidTextureModel.setTangibleServiceUrl` ignores a blank value and
      // keeps its own default, exactly as Java does, so a cleared field is not
      // an error.
      tangibleServiceUrl: this.tangibleServiceUrl().trim(),
      tangibleEnabled: this.tangibleEnabled(),
    });
  }

  protected onCancel(): void {
    this.cancel.emit();
  }

  protected onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      this.cancel.emit();
    }
  }
}
