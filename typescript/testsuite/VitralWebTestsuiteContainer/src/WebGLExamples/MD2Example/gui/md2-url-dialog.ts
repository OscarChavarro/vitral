import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';

/**
 * What the user chooses before `MD2Example` starts: the Quake II MD2 model and
 * the skin image to texture it with.
 */
export interface Md2Selection {
  readonly md2Url: string;
  readonly textureUrl: string;
}

/**
 * The browser's place for what the Java program names in its own source.
 *
 * `Md2MeshExample.init()` hard-codes two paths relative to an `ASSETS_PATH` of
 * `../../../../`, `etc/md2/samourai.md2` and `etc/md2/samourai.jpg`, and hands
 * both to `io.DebuggerReader`; its constructor takes a file name from the
 * command line and ignores it, so this project has no `awt.FileSelectorDialog`
 * of its own, as `MeshExample` and `SolidTextureExample` do.
 *
 * A browser has no `etc` directory beside the program, and the two resources
 * are URLs served from `public/etc`. Those URLs are what this dialog asks for,
 * reached by right-clicking `MD2Example` in the explorer tree, and its defaults
 * are the two resources the Java program names. Nothing else crosses: this
 * program reads no command line and opens no tangible-interface connection, so
 * the dialog has neither section.
 *
 * A model and its skin are two separate fields because the Java reader takes
 * two separate paths: an MD2 file names its skins internally, but
 * `Md2Persistence` ignores those names and reads the image the caller hands
 * it, in both editions.
 */
@Component({
  selector: 'app-md2-url-dialog',
  templateUrl: './md2-url-dialog.html',
  styleUrl: './md2-url-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Md2UrlDialog {
  /** `ASSETS_PATH + "etc/md2/samourai.md2"`, as the container serves it. */
  protected static readonly DEFAULT_MD2_URL = '/etc/md2/samourai.md2';

  /** `ASSETS_PATH + "etc/md2/samourai.jpg"`, as the container serves it. */
  protected static readonly DEFAULT_TEXTURE_URL = '/etc/md2/samourai.jpg';

  protected readonly md2Url = signal<string>(Md2UrlDialog.DEFAULT_MD2_URL);
  protected readonly textureUrl = signal<string>(Md2UrlDialog.DEFAULT_TEXTURE_URL);

  @Input()
  set initialMd2Url(value: string | null) {
    this.md2Url.set(value ?? Md2UrlDialog.DEFAULT_MD2_URL);
  }

  @Input()
  set initialTextureUrl(value: string | null) {
    this.textureUrl.set(value ?? Md2UrlDialog.DEFAULT_TEXTURE_URL);
  }

  @Output()
  readonly accept = new EventEmitter<Md2Selection>();

  @Output()
  readonly cancel = new EventEmitter<void>();

  protected onMd2UrlInput(event: Event): void {
    this.md2Url.set((event.target as HTMLInputElement).value);
  }

  protected onTextureUrlInput(event: Event): void {
    this.textureUrl.set((event.target as HTMLInputElement).value);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    const md2Url = this.md2Url().trim();
    if (md2Url.length === 0) {
      return;
    }
    this.accept.emit({ md2Url, textureUrl: this.textureUrl().trim() });
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
