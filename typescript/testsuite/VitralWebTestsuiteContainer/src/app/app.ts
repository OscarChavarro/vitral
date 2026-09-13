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
import { MD2Example } from '../WebGLExamples/MD2Example/md2-example';
import { Md2UrlDialog, type Md2Selection } from '../WebGLExamples/MD2Example/gui/md2-url-dialog';
import { ShadersExample } from '../WebGLExamples/ShadersExample/shaders-example';
import {
  ShadersUrlDialog,
  type ShadersSelection,
} from '../WebGLExamples/ShadersExample/gui/shaders-url-dialog';
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
        | 'SolidTextureExample'
        | 'MD2Example'
        | 'ShadersExample';
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
    MD2Example,
    Md2UrlDialog,
    ShadersExample,
    ShadersUrlDialog,
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
        {
          kind: 'file',
          label: 'MD2Example',
          exampleId: 'MD2Example',
        },
        {
          kind: 'file',
          label: 'ShadersExample',
          exampleId: 'ShadersExample',
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

  /**
   * `MD2Example` names no file on its command line: `Md2MeshExample.init()`
   * hard-codes the model and the skin it reads, relative to the program's own
   * directory. A browser has no such directory, so both are named by URL here,
   * with the Java paths as the defaults.
   */
  protected readonly md2Url = signal<string | null>(null);
  protected readonly md2TextureUrl = signal<string | null>(null);
  protected readonly md2UrlDialogOpen = signal<boolean>(false);

  /**
   * `ShadersExample` names no file on its command line either: its interactive
   * `main` ignores `options.CommandLineOptions` altogether, and
   * `ShadersModel.initializeDefaults` hard-codes the texture, the bump map and
   * the microfacet CSV it reads. All three are named by URL here, with the
   * Java paths as the defaults.
   */
  protected readonly shadersTextureUrl = signal<string | null>(null);
  protected readonly shadersBumpMapUrl = signal<string | null>(null);
  protected readonly shadersMicroFacetCsvUrl = signal<string | null>(null);
  protected readonly shadersUrlDialogOpen = signal<boolean>(false);

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
    if (item.kind === 'file' && item.exampleId === 'MD2Example' && this.md2Url() === null) {
      this.openMd2UrlDialog();
      return;
    }
    if (
      item.kind === 'file' &&
      item.exampleId === 'ShadersExample' &&
      this.shadersTextureUrl() === null
    ) {
      this.openShadersUrlDialog();
      return;
    }
    this.selectedItem.set(item.kind === 'file' ? item.exampleId : item.label);
  }

  /**
   * Right-clicking `MeshExample`, `SolidTextureExample`, `MD2Example` or
   * `ShadersExample`
   * reopens that module's own chooser, which is how a running module is pointed at another
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
      return;
    }
    if (item.exampleId === 'MD2Example') {
      event.preventDefault();
      this.openMd2UrlDialog();
      return;
    }
    if (item.exampleId === 'ShadersExample') {
      event.preventDefault();
      this.openShadersUrlDialog();
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

  protected acceptMd2Url(selection: Md2Selection): void {
    this.md2UrlDialogOpen.set(false);
    this.md2TextureUrl.set(selection.textureUrl);
    this.md2Url.set(selection.md2Url);
    this.selectedItem.set('MD2Example');
  }

  protected cancelMd2Url(): void {
    this.md2UrlDialogOpen.set(false);
    if (this.md2Url() === null) {
      console.error('File not specified');
    }
  }

  private openMd2UrlDialog(): void {
    this.md2UrlDialogOpen.set(true);
  }

  protected acceptShadersUrls(selection: ShadersSelection): void {
    this.shadersUrlDialogOpen.set(false);
    this.shadersBumpMapUrl.set(selection.bumpMapUrl);
    this.shadersMicroFacetCsvUrl.set(selection.microFacetCsvUrl);
    this.shadersTextureUrl.set(selection.textureUrl);
    this.selectedItem.set('ShadersExample');
  }

  protected cancelShadersUrls(): void {
    this.shadersUrlDialogOpen.set(false);
  }

  private openShadersUrlDialog(): void {
    this.shadersUrlDialogOpen.set(true);
  }

  protected clearSelection(): void {
    this.selectedItem.set(null);
  }

  protected isSelected(item: ExplorerItem): boolean {
    return this.selectedItem() === (item.kind === 'file' ? item.exampleId : item.label);
  }
}
