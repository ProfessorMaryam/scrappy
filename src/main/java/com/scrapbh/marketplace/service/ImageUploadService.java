package com.scrapbh.marketplace.service;

import com.scrapbh.marketplace.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for handling image uploads to Supabase Storage.
 */
public interface ImageUploadService {
    
    /**
     * Upload multiple images to Supabase Storage.
     * 
     * @param images List of image files to upload
     * @return Array of public HTTPS URLs for the uploaded images
     * @throws ValidationException if validation fails (size, format) or upload fails
     */
    String[] uploadPostImages(List<MultipartFile> images) throws ValidationException;
}
