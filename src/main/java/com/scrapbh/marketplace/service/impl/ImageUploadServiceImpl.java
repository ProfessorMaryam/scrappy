package com.scrapbh.marketplace.service.impl;

import com.scrapbh.marketplace.config.SupabaseConfig;
import com.scrapbh.marketplace.exception.ValidationException;
import com.scrapbh.marketplace.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of ImageUploadService for uploading images to Supabase Storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    private final SupabaseConfig supabaseConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String[] uploadPostImages(List<MultipartFile> images) throws ValidationException {
        if (images == null || images.isEmpty()) {
            return new String[0];
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<String> uploadedFilenames = new ArrayList<>();

        try {
            for (MultipartFile image : images) {
                validateImage(image);
                String filename = generateUniqueFilename(image.getOriginalFilename());
                String url = uploadToSupabase(image, filename);
                uploadedUrls.add(url);
                uploadedFilenames.add(filename);
            }

            return uploadedUrls.toArray(new String[0]);

        } catch (Exception e) {
            // Cleanup partial uploads on failure
            cleanupPartialUploads(uploadedFilenames);
            
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("Failed to upload images: " + e.getMessage(), e);
        }
    }

    /**
     * Validate image size and format.
     */
    private void validateImage(MultipartFile image) throws ValidationException {
        if (image.isEmpty()) {
            throw new ValidationException("Image file is empty");
        }

        // Validate file size
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("Each image must be under 5MB");
        }

        // Validate content type
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
                    "Invalid image format. Only JPEG, PNG, and WebP are supported"
            );
        }

        // Validate file extension
        String originalFilename = image.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new ValidationException(
                        "Invalid image format. Only JPEG, PNG, and WebP are supported"
                );
            }
        }
    }

    /**
     * Generate a unique filename to prevent collisions.
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension;
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Upload image to Supabase Storage and return public URL.
     */
    private String uploadToSupabase(MultipartFile image, String filename) throws IOException, InterruptedException {
        String bucket = supabaseConfig.getStorageBucket();
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
                supabaseConfig.getUrl(), bucket, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseConfig.getServiceKey())
                .header("Content-Type", image.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(image.getBytes()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Failed to upload image to Supabase. Status: {}, Response: {}",
                    response.statusCode(), response.body());
            throw new ValidationException("Failed to upload image to storage");
        }

        // Return public URL
        return String.format("%s/storage/v1/object/public/%s/%s",
                supabaseConfig.getUrl(), bucket, filename);
    }

    /**
     * Cleanup partial uploads when an error occurs.
     */
    private void cleanupPartialUploads(List<String> filenames) {
        if (filenames.isEmpty()) {
            return;
        }

        log.info("Cleaning up {} partial uploads", filenames.size());
        
        for (String filename : filenames) {
            try {
                deleteFromSupabase(filename);
            } catch (Exception e) {
                log.error("Failed to cleanup file {}: {}", filename, e.getMessage());
            }
        }
    }

    /**
     * Delete a file from Supabase Storage.
     */
    private void deleteFromSupabase(String filename) throws IOException, InterruptedException {
        String bucket = supabaseConfig.getStorageBucket();
        String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
                supabaseConfig.getUrl(), bucket, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("Authorization", "Bearer " + supabaseConfig.getServiceKey())
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Failed to delete file {} from Supabase. Status: {}, Response: {}",
                    filename, response.statusCode(), response.body());
        }
    }
}
