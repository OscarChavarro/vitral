import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { Vector3Dd } from '@vitral/base';
import { WebGLHelloWorld } from '../_APITests/_WebGLHelloWorld/webgl-hello-world';
import { CameraExample } from '../WebGLExamples/CameraExample/camera-example';
import { ImageExample } from '../WebGLExamples/ImageExample/image-example';

type ExplorerItem =
  | {
      kind: 'folder';
      label: string;
      children?: readonly ExplorerItem[];
    }
  | {
      kind: 'file';
      label: string;
      exampleId: '_WebGLHelloWorld' | 'CameraExample' | 'ImageExample';
    };

@Component({
  selector: 'app-root',
  imports: [WebGLHelloWorld, CameraExample, ImageExample],
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
      ],
    },
    { kind: 'folder', label: 'WebGPUExamples' },
    { kind: 'folder', label: 'Tools' },
    { kind: 'folder', label: 'ApplicationCases' },
  ];

  protected readonly selectedItem = signal<string | null>(null);
  protected readonly workspaceOrigin = new Vector3Dd();

  protected selectItem(item: ExplorerItem): void {
    this.selectedItem.set(item.kind === 'file' ? item.exampleId : item.label);
  }

  protected clearSelection(): void {
    this.selectedItem.set(null);
  }

  protected isSelected(item: ExplorerItem): boolean {
    return this.selectedItem() === (item.kind === 'file' ? item.exampleId : item.label);
  }
}
