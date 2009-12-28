package org.ericsk.pluroid;

import org.ericsk.pluroid.IUploadServiceCallback;

interface IUploadService {

    void registerCallback(IUploadServiceCallback callback);
    
    void unregisterCallback(IUploadServiceCallback callback);

}