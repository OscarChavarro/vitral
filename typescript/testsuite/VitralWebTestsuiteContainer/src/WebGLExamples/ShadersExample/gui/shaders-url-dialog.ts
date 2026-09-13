import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';

/**
 * What the user chooses before `ShadersExample` starts: the three resources
 * `model.ShadersModel` and `render.SoftwareRaycaster` read at startup.
 */
export interface ShadersSelection {
  readonly textureUrl: string;
  readonly bumpMapUrl: string;
  readonly microFacetCsvUrl: string;
}

/**
 * The browser's place for what the Java program names in its own source.
 *
 * `ShadersModel.initializeDefaults` hard-codes three paths relative to the
 * working directory — `../../../../etc/textures/miniearth.png`,
 * `../../../../etc/bumpmaps/earth.bw` and
 * `../../../../etc/materials/microFacetMAterials.csv` — and
 * `SoftwareRaycaster.loadBumpNormalMap` names the second one again. The
 * program has no file selector of its own: `options.CommandLineOptions` is
 * read only by the offline path, and the interactive `main` ignores it
 * entirely.
 *
 * A browser has no `etc` directory beside the program, and the three resources
 * are URLs served from `public/etc`. Those URLs are what this dialog asks for,
 * reached by right-clicking `ShadersExample` in the explorer tree, and its
 * defaults are the three the Java program names. The offline switches of
 * `CommandLineOptions` have no counterpart here, since the module is the
 * interactive program; the same settings are reachable from the keyboard,
 * which is where the Java program puts them too.
 */
@Component({
  selector: 'app-shaders-url-dialog',
  templateUrl: './shaders-url-dialog.html',
  styleUrl: './shaders-url-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShadersUrlDialog {
  /** `"../../../../etc/textures/miniearth.png"`, as the container serves it. */
  protected static readonly DEFAULT_TEXTURE_URL = '/etc/textures/miniearth.png';

  /** `"../../../../etc/bumpmaps/earth.bw"`, as the container serves it. */
  protected static readonly DEFAULT_BUMP_MAP_URL = '/etc/bumpmaps/earth.bw';

  /** `"../../../../etc/materials/microFacetMAterials.csv"`, likewise. */
  protected static readonly DEFAULT_MICROFACET_CSV_URL = '/etc/materials/microFacetMAterials.csv';

  protected readonly textureUrl = signal<string>(ShadersUrlDialog.DEFAULT_TEXTURE_URL);
  protected readonly bumpMapUrl = signal<string>(ShadersUrlDialog.DEFAULT_BUMP_MAP_URL);
  protected readonly microFacetCsvUrl = signal<string>(ShadersUrlDialog.DEFAULT_MICROFACET_CSV_URL);

  @Input()
  set initialTextureUrl(value: string | null) {
    this.textureUrl.set(value ?? ShadersUrlDialog.DEFAULT_TEXTURE_URL);
  }

  @Input()
  set initialBumpMapUrl(value: string | null) {
    this.bumpMapUrl.set(value ?? ShadersUrlDialog.DEFAULT_BUMP_MAP_URL);
  }

  @Input()
  set initialMicroFacetCsvUrl(value: string | null) {
    this.microFacetCsvUrl.set(value ?? ShadersUrlDialog.DEFAULT_MICROFACET_CSV_URL);
  }

  @Output()
  readonly accept = new EventEmitter<ShadersSelection>();

  @Output()
  readonly cancel = new EventEmitter<void>();

  protected onTextureUrlInput(event: Event): void {
    this.textureUrl.set((event.target as HTMLInputElement).value);
  }

  protected onBumpMapUrlInput(event: Event): void {
    this.bumpMapUrl.set((event.target as HTMLInputElement).value);
  }

  protected onMicroFacetCsvUrlInput(event: Event): void {
    this.microFacetCsvUrl.set((event.target as HTMLInputElement).value);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    const textureUrl = this.textureUrl().trim();
    const bumpMapUrl = this.bumpMapUrl().trim();
    const microFacetCsvUrl = this.microFacetCsvUrl().trim();
    if (textureUrl.length === 0 || bumpMapUrl.length === 0 || microFacetCsvUrl.length === 0) {
      return;
    }
    this.accept.emit({ textureUrl, bumpMapUrl, microFacetCsvUrl });
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
