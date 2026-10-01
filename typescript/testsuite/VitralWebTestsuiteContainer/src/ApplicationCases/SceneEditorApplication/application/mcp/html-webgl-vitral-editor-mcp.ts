import type { HtmlWebGLSceneEditorApplication } from '../html-webgl-scene-editor-application';
import { HtmlWebGLVitralEditorMCPProtocol } from './html-webgl-vitral-editor-mcp-protocol';

/**
 * The function an agent calls in the page: one JSON-RPC request line in, one
 * response line out (see `MCP.md` of the Java example).
 */
export interface VitralEditorMCPEndpoint {
  request(line: string): Promise<string>;
}

declare global {
  interface Window {
    /** Agent API of the scene editor, installed with the `-s` argument */
    vitralEditorMCP?: VitralEditorMCPEndpoint;
  }
}

/**
 * Port of `application.mcp.AwtJogl4VitralEditorMCP`.
 *
 * Automation service of the editor (MCP-like JSON-RPC, see `MCP.md`). Java
 * listens on local TCP port 1234 and serves each connection with an
 * `AwtJogl4VitralEditorMCPProtocol`; a page can not listen to a socket, so the
 * same protocol is served by `window.vitralEditorMCP.request(line)`, which an
 * agent driving the browser (i.e. through the DevTools protocol) calls with
 * each request line and awaits for the response line.
 */
export class HtmlWebGLVitralEditorMCP {
  private readonly protocol: HtmlWebGLVitralEditorMCPProtocol;

  constructor(parent: HtmlWebGLSceneEditorApplication) {
    this.protocol = new HtmlWebGLVitralEditorMCPProtocol(parent);
    window.vitralEditorMCP = {
      request: (line: string): Promise<string> => this.protocol.handle(line),
    };
    console.log('Waiting for MCP requests on window.vitralEditorMCP.request(line)');
  }

  /**
   * Removes the endpoint (the application closed).
   */
  dispose(): void {
    if (window.vitralEditorMCP !== undefined) {
      delete window.vitralEditorMCP;
    }
  }
}
