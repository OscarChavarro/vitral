/**
Port of `vsdk.toolkit.render.swing.CollapsablePanel`: a panel with a header
that shows or hides its content when clicked.
*/
export class HtmlCollapsablePanel {
    public readonly element: HTMLDivElement;
    private readonly content: HTMLElement;
    private selected: boolean;

    /**
    @param text title of the header
    @param content content shown or hidden
    */
    public constructor(text: string, content: HTMLElement) {
        this.selected = false;
        this.content = content;
        this.element = document.createElement("div");
        this.element.className = "vitral-collapsable-panel";

        const header: HTMLDivElement = document.createElement("div");
        header.className = "vitral-collapsable-panel-header";
        header.textContent = text;
        header.addEventListener("click", (): void => this.toggleSelection());

        this.element.appendChild(header);
        this.element.appendChild(content);
        this.content.hidden = true;
    }

    /**
    Shows the content if hidden, hides it if shown.
    */
    public toggleSelection(): void {
        this.selected = !this.selected;
        this.content.hidden = !this.selected;
        this.element.classList.toggle("vitral-collapsable-panel-open", this.selected);
    }
}
