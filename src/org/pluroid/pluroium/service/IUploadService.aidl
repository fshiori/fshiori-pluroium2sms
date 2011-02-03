package org.pluroid.pluroium.service;

import org.pluroid.pluroium.service.IUploadServiceCallback;

interface IUploadService {

    void registerCallback(IUploadServiceCallback callback);
    
    void unregisterCallback(IUploadServiceCallback callback);

}