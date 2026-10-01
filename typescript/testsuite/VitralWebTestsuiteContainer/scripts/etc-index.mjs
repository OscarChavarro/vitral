// Writes `index.json` at the root of a folder: the tree of its folders and
// files, which a page fetches to browse the folder (a browser can not list
// the folders of a server). Folders are listed before files, each group
// sorted by name.
//
// usage: node etc-index.mjs <folder>
import { readdirSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

function list(folder) {
  const entries = readdirSync(folder, { withFileTypes: true })
    .filter((entry) => !entry.name.startsWith('.') && entry.name !== 'index.json');
  const folders = entries.filter((entry) => entry.isDirectory())
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  const files = entries.filter((entry) => entry.isFile())
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  return [
    ...folders.map((entry) => ({ name: entry.name, children: list(join(folder, entry.name)) })),
    ...files.map((entry) => ({ name: entry.name, size: statSync(join(folder, entry.name)).size })),
  ];
}

const root = process.argv[2];
if (!root) {
  console.error('usage: node etc-index.mjs <folder>');
  process.exit(1);
}
writeFileSync(join(root, 'index.json'), JSON.stringify({ name: '', children: list(root) }));
