package scc.kube;

import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.Result;
import scc.ServiceError;

public class KubeMediaService implements MediaService {

    @Override
    public Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<byte[], ServiceError> downloadMedia(MediaId mediaId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void, ServiceError> deleteMedia(MediaId mediaId) {
        // TODO Auto-generated method stub
        return null;
    }

}
