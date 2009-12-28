package org.pluroid.pluroium;

import org.pluroid.pluroium.IUploadServiceCallback;

interface IUploadService {

    void registerCallback(IUploadServiceCallback callback);
    
    void unregisterCallback(IUploadServiceCallback callback);

}