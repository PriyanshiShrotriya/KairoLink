package com.KairoLink.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Component
public class VehiclePhotoStorage {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path storageDirectory;

    public VehiclePhotoStorage(
            @Value("${kairolink.uploads.vehicle-directory:uploads/vehicles}")
            String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        ImageType imageType = validate(file);
        try {
            Files.createDirectories(storageDirectory);
            String filename = UUID.randomUUID() + imageType.extension;
            Path target = storageDirectory.resolve(filename).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new IllegalArgumentException("Invalid vehicle photo path");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/vehicle/photos/" + filename;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Vehicle photo could not be saved", exception);
        }
    }

    public Path resolve(String reference) {
        if (reference == null || !reference.startsWith("/vehicle/photos/")) {
            return null;
        }
        String filename = reference.substring("/vehicle/photos/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            return null;
        }
        Path path = storageDirectory.resolve(filename).normalize();
        return path.startsWith(storageDirectory) ? path : null;
    }

    public void delete(String reference) {
        Path path = resolve(reference);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Previous vehicle photo could not be removed", exception);
        }
    }

    private ImageType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a vehicle photo");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Vehicle photos must be 5 MB or smaller");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            ImageType type = ImageType.detect(header);
            if (type == null) {
                throw new IllegalArgumentException("Vehicle photo must be JPG, PNG, or WebP");
            }
            if (type != ImageType.WEBP) {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new IllegalArgumentException("The selected vehicle photo is not a valid image");
                }
            }
            return type;
        } catch (IOException exception) {
            throw new IllegalArgumentException("The selected vehicle photo could not be read", exception);
        }
    }

    private enum ImageType {
        JPEG(".jpg"), PNG(".png"), WEBP(".webp");

        private final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }

        private static ImageType detect(byte[] header) {
            if (header.length >= 3
                    && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff) {
                return JPEG;
            }
            if (header.length >= 8
                    && (header[0] & 0xff) == 0x89
                    && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
                return PNG;
            }
            if (header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                    && header[3] == 'F' && header[8] == 'W' && header[9] == 'E'
                    && header[10] == 'B' && header[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
