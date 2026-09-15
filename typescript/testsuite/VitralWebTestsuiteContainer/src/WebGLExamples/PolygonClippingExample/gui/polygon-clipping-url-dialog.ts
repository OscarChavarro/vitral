import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';
import { PolygonClippingReader } from '../io/polygon-clipping-reader';

/**
 * What the user chooses before `PolygonClippingExample` starts: the directory
 * `model.PolygonClippingModelingTools` reads its contour files from.
 */
export interface PolygonClippingSelection {
  readonly polygonsBaseUrl: string;
}

/**
 * The browser's place for what the Java program names in its own source.
 *
 * `PolygonClippingModelingTools.POLYGONS_PATH` hard-codes
 * `"../../../../etc/polygons/"`, relative to the program's working directory,
 * and `PolygonClippingFixtures` hard-codes the forty-three pairs of file names
 * read from it. The program has no file selector of its own:
 * `options.CommandLineOptions` is read only by the offline path, and the
 * interactive `main` never reaches it.
 *
 * A browser has no `etc` directory beside the program, so what is asked for is
 * the URL that directory is served from, reached by right-clicking
 * `PolygonClippingExample` in the explorer tree; the default is the tree the
 * container publishes. The fixture table is not asked for, because the Java
 * program compiles it in and the `[1]` and `[2]` keys walk it.
 */
@Component({
  selector: 'app-polygon-clipping-url-dialog',
  templateUrl: './polygon-clipping-url-dialog.html',
  styleUrl: './polygon-clipping-url-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolygonClippingUrlDialog {
  /** `"../../../../etc/polygons/"`, as the container serves it. */
  protected static readonly DEFAULT_POLYGONS_BASE_URL = '/etc/polygons/';

  protected readonly polygonsBaseUrl = signal<string>(
    PolygonClippingUrlDialog.DEFAULT_POLYGONS_BASE_URL,
  );

  /** How many contour files the fixture table names, shown as a hint. */
  protected readonly referencedFileCount = PolygonClippingReader.referencedFileNames().length;

  @Input()
  set initialPolygonsBaseUrl(value: string | null) {
    this.polygonsBaseUrl.set(value ?? PolygonClippingUrlDialog.DEFAULT_POLYGONS_BASE_URL);
  }

  @Output()
  readonly accept = new EventEmitter<PolygonClippingSelection>();

  @Output()
  readonly cancel = new EventEmitter<void>();

  protected onPolygonsBaseUrlInput(event: Event): void {
    this.polygonsBaseUrl.set((event.target as HTMLInputElement).value);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    const polygonsBaseUrl = this.polygonsBaseUrl().trim();
    if (polygonsBaseUrl.length === 0) {
      return;
    }
    this.accept.emit({ polygonsBaseUrl });
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
