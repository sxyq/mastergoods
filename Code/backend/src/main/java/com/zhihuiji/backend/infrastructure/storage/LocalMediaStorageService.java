package com.zhihuiji.backend.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalMediaStorageService implements MediaStorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalMediaStorageService.class);

    private final Path storagePath;

    public LocalMediaStorageService(@Value("${media.storage.local.base-path:./data/media}") String basePath) {
        this.storagePath = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storagePath);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建媒体存储目录: " + storagePath, ex);
        }
        log.info("Local media storage initialized at {}", storagePath);
    }

    @Override
    public String store(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String objectKey = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = storagePath.resolve(objectKey).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Stored media file {} ({} bytes) as {}", originalName, file.getSize(), objectKey);
        return objectKey;
    }

    @Override
    public byte[] load(String objectKey) throws Exception {
        Path source = storagePath.resolve(objectKey).normalize();
        if (!source.startsWith(storagePath)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        return Files.readAllBytes(source);
    }

    @Override
    public void delete(String objectKey) throws Exception {
        Path target = storagePath.resolve(objectKey).normalize();
        if (!target.startsWith(storagePath)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        Files.deleteIfExists(target);
    }
}
