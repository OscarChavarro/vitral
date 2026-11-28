import { BrowserWorkerExecutor, type ReaderPlyWorkerFactory } from '@vitral/base';

// Angular discovers the worker entry through its application's source files.
export const createPlyWorker: ReaderPlyWorkerFactory = () =>
  new BrowserWorkerExecutor(
    new Worker(new URL('./ply-mesh.worker', import.meta.url), { type: 'module' }),
  );
