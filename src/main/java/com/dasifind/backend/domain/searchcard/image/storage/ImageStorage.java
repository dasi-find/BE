package com.dasifind.backend.domain.searchcard.image.storage;

public interface ImageStorage {

    void upload(String storageKey, String contentType, byte[] content);

    String createDownloadUrl(String storageKey);

    void delete(String storageKey);
}
