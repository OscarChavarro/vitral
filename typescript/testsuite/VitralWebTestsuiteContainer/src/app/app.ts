import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { Vector3Dd } from '@vitral/base';
import { WebGLHelloWorld } from '../_APITests/_WebGLHelloWorld/webgl-hello-world';
import { CameraExample } from '../WebGLExamples/CameraExample/camera-example';
import { ImageExample } from '../WebGLExamples/ImageExample/image-example';
import { MeshExample } from '../WebGLExamples/MeshExample/mesh-example';
import {
  MeshUrlDialog,
  type MeshSelection,
} from '../WebGLExamples/MeshExample/gui/mesh-url-dialog';
import { SolidTextureExample } from '../WebGLExamples/SolidTextureExample/solid-texture-example';
import {
  SolidTextureUrlDialog,
  type SolidTextureSelection,
} from '../WebGLExamples/SolidTextureExample/gui/solid-texture-url-dialog';

type ExplorerItem =
  | {
      kind: 'folder';
      label: string;
      children?: readonly ExplorerItem[];
    }
  | {
      kind: 'file';
      label: string;
      exampleId:
        | '_WebGLHelloWorld'
        | 'CameraExample'
        | 'ImageExample'
        | 'MeshExample'
        | 'SolidTextureExample';
    };

@Component({
  selector: 'app-root',
  imports: [
    WebGLHelloWorld,
    CameraExample,
    ImageExample,
    MeshExample,
    MeshUrlDialog,
    SolidTextureExample,
    SolidTextureUrlDialog,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly treeItems: readonly ExplorerItem[] = [
    {
      kind: 'folder',
      label: '_APITests',
      children: [
        {
          kind: 'file',
          label: '_WebGLHelloWorld',
          exampleId: '_WebGLHelloWorld',
        },
      ],
    },
    {
      kind: 'folder',
      label: 'WebGLExamples',
      children: [
        {
          kind: 'file',
          label: 'CameraExample',
          exampleId: 'CameraExample',
        },
        {
          kind: 'file',
          label: 'ImageExample',
          exampleId: 'ImageExample',
        },
        {
          kind: 'file',
          label: 'MeshExample',
          exampleId: 'MeshExample',
        },
        {
          kind: 'file',
          label: 'SolidTextureExample',
          exampleId: 'SolidTextureExample',
        },
      ],
    },
    { kind: 'folder', label: 'WebGPUExamples' },
    { kind: 'folder', label: 'Tools' },
    { kind: 'folder', label: 'ApplicationCases' },
  ];

  protected readonly selectedItem = signal<string | null>(null);
  protected readonly workspaceOrigin = new Vector3Dd();

  /**
   * `MeshExample` names its geometry by URL, because a browser has no file
   * system to browse. The URL is asked for here, in the explorer, so that the
   * example module receives it the way the Java program receives a file name:
   * already chosen, before anything is drawn.
   */
  protected readonly meshUrl = signal<string | null>(null);
  protected readonly meshUrlDialogOpen = signal<boolean>(false);

  /**
   * The dialog also carries what `options.CommandLineOptions` reads from
   * `-tangibleServer`, plus whether to connect at all; see the dialog for why
   * that second control exists and why it starts off.
   */
  protected readonly tangibleServiceUrl = signal<string | null>(null);
  protected readonly tangibleEnabled = signal<boolean>(false);

  /**
   * `SolidTextureExample` names its geometry the same way and for the same
   * reason, and carries its own copy of the chooser, as the two Java projects
   * each carry their own `awt.FileSelectorDialog`. Its selection is kept apart
   * from `MeshExample`'s so that pointing one module at another mesh leaves the
   * other module where it was.
   */
  protected readonly solidTextureMeshUrl = signal<string | null>(null);
  protected readonly solidTextureUrlDialogOpen = signal<boolean>(false);
  protected readonly solidTextureTangibleServiceUrl = signal<string | null>(null);
  protected readonly solidTextureTangibleEnabled = signal<boolean>(false);

  protected selectItem(item: ExplorerItem): void {
    if (item.kind === 'file' && item.exampleId === 'MeshExample' && this.meshUrl() === null) {
      // Java runs `FileSelectorDialog` when the command line named no file.
      this.openMeshUrlDialog();
      return;
    }
    if (
      item.kind === 'file' &&
      item.exampleId === 'SolidTextureExample' &&
      this.solidTextureMeshUrl() === null
    ) {
      this.openSolidTextureUrlDialog();
      return;
    }
    this.selectedItem.set(item.kind === 'file' ? item.exampleId : item.label);
  }

  /**
   * Right-clicking `MeshExample` or `SolidTextureExample` reopens that
   * module's own chooser, which is how a running module is pointed at another
   * mesh; every other tree item keeps the browser's own context menu.
   */
  protected onItemContextMenu(event: MouseEvent, item: ExplorerItem): void {
    if (item.kind !== 'file') {
      return;
    }
    if (item.exampleId === 'MeshExample') {
      event.preventDefault();
      this.openMeshUrlDialog();
      return;
    }
    if (item.exampleId === 'SolidTextureExample') {
      event.preventDefault();
      this.openSolidTextureUrlDialog();
    }
  }

  protected acceptMeshUrl(selection: MeshSelection): void {
    this.meshUrlDialogOpen.set(false);
    this.tangibleServiceUrl.set(selection.tangibleServiceUrl);
    this.tangibleEnabled.set(selection.tangibleEnabled);
    this.meshUrl.set(selection.meshUrl);
    this.selectedItem.set('MeshExample');
  }

  protected cancelMeshUrl(): void {
    this.meshUrlDialogOpen.set(false);
    if (this.meshUrl() === null) {
      // Java answers a null file and the program reports "File not specified".
      console.error('File not specified');
    }
  }

  private openMeshUrlDialog(): void {
    this.meshUrlDialogOpen.set(true);
  }

  protected acceptSolidTextureUrl(selection: SolidTextureSelection): void {
    this.solidTextureUrlDialogOpen.set(false);
    this.solidTextureTangibleServiceUrl.set(selection.tangibleServiceUrl);
    this.solidTextureTangibleEnabled.set(selection.tangibleEnabled);
    this.solidTextureMeshUrl.set(selection.meshUrl);
    this.selectedItem.set('SolidTextureExample');
  }

  protected cancelSolidTextureUrl(): void {
    this.solidTextureUrlDialogOpen.set(false);
    if (this.solidTextureMeshUrl() === null) {
      // Java answers a null file and the program reports "File not specified".
      console.error('File not specified');
    }
  }

  private openSolidTextureUrlDialog(): void {
    this.solidTextureUrlDialogOpen.set(true);
  }

  protected clearSelection(): void {
    this.selectedItem.set(null);
  }

  protected isSelected(item: ExplorerItem): boolean {
    return this.selectedItem() === (item.kind === 'file' ? item.exampleId : item.label);
  }
}
