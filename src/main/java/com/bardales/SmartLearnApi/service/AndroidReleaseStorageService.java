package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AndroidReleaseStorageService {

    private final Path storageDir;

    public AndroidReleaseStorageService(@Value("${app.mobile.android.storage-dir:./data/android-releases}") String storageDir) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo inicializar el almacenamiento de APK Android.", ex);
        }
    }

    public StoredApk storeApk(MultipartFile apkFile) {
        if (apkFile == null || apkFile.isEmpty()) {
            throw new BadRequestException("Selecciona un archivo APK valido.");
        }
        String originalName = normalizeFileName(apkFile.getOriginalFilename());
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
            throw new BadRequestException("El archivo debe tener extension .apk.");
        }
        String storageKey = UUID.randomUUID() + ".apk";
        Path target = storageDir.resolve(storageKey).normalize();
        if (!target.startsWith(storageDir)) {
            throw new BadRequestException("Ruta de almacenamiento invalida.");
        }

        try (InputStream inputStream = apkFile.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo guardar el archivo APK.");
        }

        String contentType = apkFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/vnd.android.package-archive";
        }
        return new StoredApk(storageKey, originalName, apkFile.getSize(), contentType);
    }

    public Resource loadAsResource(String storageKey) {
        String normalizedKey = storageKey == null ? "" : storageKey.trim();
        if (normalizedKey.isBlank()) {
            throw new NotFoundException("Archivo APK no disponible.");
        }
        Path path = storageDir.resolve(normalizedKey).normalize();
        if (!path.startsWith(storageDir) || !Files.exists(path)) {
            throw new NotFoundException("Archivo APK no disponible.");
        }
        return new FileSystemResource(path);
    }

    public void deleteIfExists(String storageKey) {
        String normalizedKey = storageKey == null ? "" : storageKey.trim();
        if (normalizedKey.isBlank()) {
            return;
        }
        Path path = storageDir.resolve(normalizedKey).normalize();
        if (!path.startsWith(storageDir)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    private String normalizeFileName(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isBlank()) {
            return "smartlearn-release.apk";
        }
        String sanitized = candidate.replace("\\", "_").replace("/", "_");
        return sanitized.length() > 255 ? sanitized.substring(sanitized.length() - 255) : sanitized;
    }

    public record StoredApk(String storageKey, String fileName, long fileSizeBytes, String contentType) {
    }
}
