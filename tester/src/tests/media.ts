import axios from "axios";
import * as Scc from '../scc';
import * as Utils from '../utils';
import { TestEnv, Tester, TestNamespace } from "../tester";

export function register(): TestNamespace {
    const ns = new TestNamespace('media');
    ns.register('upload-image', uploadImageTest);
    ns.register('upload-download-image', uploadDownloadImageTest);
    ns.register('download-missing-image', downloadMissingImage);
    return ns;
}

async function uploadImageTest(env: TestEnv) {
    const requests = new Scc.Requests(env.endpoints);
    const response = await requests.uploadMedia(Scc.randomUploadMediaParams());
    env.validate(response, (response, assertions) => {
        assertions.status(response.status, 200);
        assertions.true(response.data !== undefined, 'Response data is undefined');
        assertions.true((response.data as string).length > 0, 'Response data is empty');
    });
}

async function uploadDownloadImageTest(env: TestEnv) {
    const requests = new Scc.Requests(env.endpoints);
    const client = new Scc.Client(env.endpoints);

    const uploadParams = Scc.randomUploadMediaParams();
    const mediaId = await client.uploadMedia(uploadParams);
    const response = await requests.downloadMedia(mediaId);
    env.validate(response, (response, assertions) => {
        assertions.status(response.status, 200);
        assertions.true(response.data !== undefined, 'Response data is undefined');

        const responseData = response.data as ArrayBuffer;
        assertions.true(responseData.byteLength > 0, 'Response data is empty');
        assertions.true(Utils.compareArrayBuffers(responseData, uploadParams), 'Response data is not equal to uploaded data');
    });
}

async function downloadMissingImage(env: TestEnv) {
    const requests = new Scc.Requests(env.endpoints);
    const response = await requests.downloadMedia('missing-media-id');
    env.validate(response, (response, assertions) => {
        assertions.status(response.status, 404);
    });
}