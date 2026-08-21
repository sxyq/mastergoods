package com.zhihuiji.backend.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {
    /**
     * Store a file and return the generated object key.
     */
    String store(MultipartFile file) throws Exception;

    /**
     * Load a file as byte array by object key.
     */
    byte[] load(String objectKey) throws Exception;

    /** Delete a stored object after its metadata has been removed or when registration fails. */
    void delete(String objectKey) throws Exception;
}
