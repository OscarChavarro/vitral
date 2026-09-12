import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { Vector3Dd } from '@vitral/base';
import { WebGLHelloWorld } from '../_APITests/_WebGLHelloWorld/webgl-hello-world';

type ExplorerItem =
  | {
      kind: 'folder';
      label: string;
      children?: readonly ExplorerItem[];
    }
  | {
      kind: 'file';
      label: string;
      exampleId: '_WebGLHelloWorld';
    };

@Component({
  selector: 'app-root',
  imports: [WebGLHelloWorld],
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
    { kind: 'folder', label: 'WebGLExamples' },
    { kind: 'folder', label: 'WebGPUExamples' },
    { kind: 'folder', label: 'Tools' },
    { kind: 'folder', label: 'ApplicationCases' },
  ];

  protected readonly selectedItem = signal<string | null>(null);
  protected readonly workspaceOrigin = new Vector3Dd();

  protected selectItem(item: ExplorerItem): void {
    this.selectedItem.set(item.kind === 'file' ? item.exampleId : item.label);
  }

  protected isSelected(item: ExplorerItem): boolean {
    return this.selectedItem() === (item.kind === 'file' ? item.exampleId : item.label);
  }
}
