import { createWorkerMessageHandler } from '@vitral/base/java/concurrent/WorkerProtocol';
import { assembleReaderPlyMeshPart } from '@vitral/base/vsdk/toolkit/io/geometry/ReaderPlyMeshWorker';

self.addEventListener(
  'message',
  createWorkerMessageHandler(assembleReaderPlyMeshPart, (response) => {
    self.postMessage(response);
  }),
);
