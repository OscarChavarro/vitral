import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';

/**
 * What the user chooses before `PolyhedralBoundedSolidExample` starts: the two
 * files `models.GeneralModelsBuilder` reads.
 */
export interface PolyhedralBoundedSolidSelection {
  readonly stepFileUrl: string;
  readonly fontFileUrl: string;
}

/**
 * The browser's place for what the Java program names in its own source.
 *
 * `GeneralModelsBuilder` hard-codes two paths relative to the working
 * directory — `../../../../etc/solids/kurlanderBowl.step`, which the default
 * `STEP_IMPORT` model imports, and `../../../../etc/fonts/cyrvetic.ttf`, whose
 * `A` the `FONT_BLOCK` model extrudes. The interactive program has no file
 * selector of its own: `options.CommandLineOptions` and the
 * `polySolidModel` system property only feed the offline and scripted paths.
 *
 * A browser has no `etc` directory beside the program, so the two files are
 * named by URL, reached by right-clicking `PolyhedralBoundedSolidExample` in
 * the explorer tree, with the two Java paths, as the container serves them, as
 * the defaults.
 */
@Component({
  selector: 'app-polyhedral-bounded-solid-url-dialog',
  templateUrl: './polyhedral-bounded-solid-url-dialog.html',
  styleUrl: './polyhedral-bounded-solid-url-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolyhedralBoundedSolidUrlDialog {
  /** `"../../../../etc/solids/kurlanderBowl.step"`, as the container serves it. */
  protected static readonly DEFAULT_STEP_FILE_URL = '/etc/solids/kurlanderBowl.step';

  /** `"../../../../etc/fonts/cyrvetic.ttf"`, as the container serves it. */
  protected static readonly DEFAULT_FONT_FILE_URL = '/etc/fonts/cyrvetic.ttf';

  protected readonly stepFileUrl = signal<string>(
    PolyhedralBoundedSolidUrlDialog.DEFAULT_STEP_FILE_URL,
  );
  protected readonly fontFileUrl = signal<string>(
    PolyhedralBoundedSolidUrlDialog.DEFAULT_FONT_FILE_URL,
  );

  @Input()
  set initialStepFileUrl(value: string | null) {
    this.stepFileUrl.set(value ?? PolyhedralBoundedSolidUrlDialog.DEFAULT_STEP_FILE_URL);
  }

  @Input()
  set initialFontFileUrl(value: string | null) {
    this.fontFileUrl.set(value ?? PolyhedralBoundedSolidUrlDialog.DEFAULT_FONT_FILE_URL);
  }

  @Output()
  readonly accept = new EventEmitter<PolyhedralBoundedSolidSelection>();

  @Output()
  readonly cancel = new EventEmitter<void>();

  protected onStepFileUrlInput(event: Event): void {
    this.stepFileUrl.set((event.target as HTMLInputElement).value);
  }

  protected onFontFileUrlInput(event: Event): void {
    this.fontFileUrl.set((event.target as HTMLInputElement).value);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    const stepFileUrl = this.stepFileUrl().trim();
    const fontFileUrl = this.fontFileUrl().trim();
    if (stepFileUrl.length === 0 || fontFileUrl.length === 0) {
      return;
    }
    this.accept.emit({ stepFileUrl, fontFileUrl });
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
