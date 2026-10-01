import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
  signal,
} from '@angular/core';
import { HtmlWebGLSceneEditorApplication } from './application/html-webgl-scene-editor-application';

/**
 * Hosts `SceneEditorApplication` in the workspace of the testsuite container.
 *
 * Java's command line arguments are read from the `arg` parameters of the
 * page URL (i.e. `?arg=-s` installs the agent API, `window.vitralEditorMCP`),
 * as a page has no command line. When the application closes itself (Java's
 * `System.exit`), the module is deactivated.
 */
@Component({
  selector: 'app-scene-editor-application',
  templateUrl: './scene-editor-application.html',
  styleUrl: './scene-editor-application.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SceneEditorApplication implements AfterViewInit, OnDestroy {
  @ViewChild('root', { static: true })
  private readonly rootRef!: ElementRef<HTMLDivElement>;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private application: HtmlWebGLSceneEditorApplication | null = null;
  private destroyed = false;

  async ngAfterViewInit(): Promise<void> {
    const args: string[] = new URLSearchParams(window.location.search).getAll('arg');

    try {
      const application: HtmlWebGLSceneEditorApplication = await HtmlWebGLSceneEditorApplication.create(
        this.rootRef.nativeElement,
        args,
        (): void => {
          this.application = null;
          this.deactivate.emit();
        },
      );
      if (this.destroyed) {
        application.closeApplication();
        return;
      }
      this.application = application;
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'SceneEditorApplication initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    const application: HtmlWebGLSceneEditorApplication | null = this.application;

    this.application = null;
    if (application !== null) {
      // Leaving the module closes the application, without deactivating twice
      this.deactivate.complete();
      application.closeApplication();
    }
  }
}
