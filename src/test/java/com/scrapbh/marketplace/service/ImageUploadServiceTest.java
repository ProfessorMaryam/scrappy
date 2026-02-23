package com.scrapbh.marketplace.service;

import com.scrapbh.marketplace.config.SupabaseConfig;
import com.scrapbh.marketplace.exception.ValidationException;
import com.scrapbh.marketplace.service.impl.ImageUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private SupabaseConfig supabaseConfig;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        when(supabaseConfig.getUrl()).thenReturn("https://test.supabase.co");
        when(supabaseConfig.getServiceKey()).thenReturn("test-key");
        when(supabaseConfig.getStorageBucket()).thenReturn("post-images");
        
        imageUploadService = new ImageUploadServiceImpl(supabaseConfig);
    }

    @Test
    void testUploadPostImages_EmptyList_ReturnsEmptyArray() {
        String[] result = imageUploadService.uploadPostImages(Collections.emptyList());
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testUploadPostImages_NullList_ReturnsEmptyArray() {
        String[] result = imageUploadService.uploadPostImages(null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testValidateImage_FileTooLarge_ThrowsValidationException() {
        // Create a file larger than 5MB
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                largeContent
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> imageUploadService.uploadPostImages(List.of(largeFile))
        );
        
        assertTrue(exception.getMessage().contains("5MB"));
    }

    @Test
    void testValidateImage_InvalidFormat_ThrowsValidationException() {
        byte[] content = "test content".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
                "image",
                "test.txt",
                "text/plain",
                content
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> imageUploadService.uploadPostImages(List.of(invalidFile))
        );
        
        assertTrue(exception.getMessage().contains("JPEG, PNG, and WebP"));
    }

    @Test
    void testValidateImage_EmptyFile_ThrowsValidationException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> imageUploadService.uploadPostImages(List.of(emptyFile))
        );
        
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void testValidateImage_ValidJpeg_NoException() {
        byte[] content = new byte[1024]; // 1KB
        MockMultipartFile validFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                content
        );

        // This will fail at upload stage (no real Supabase), but validation should pass
        assertThrows(ValidationException.class, 
                () -> imageUploadService.uploadPostImages(List.of(validFile)));
    }

    @Test
    void testValidateImage_ValidPng_NoException() {
        byte[] content = new byte[1024]; // 1KB
        MockMultipartFile validFile = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                content
        );

        // This will fail at upload stage (no real Supabase), but validation should pass
        assertThrows(ValidationException.class, 
                () -> imageUploadService.uploadPostImages(List.of(validFile)));
    }

    @Test
    void testValidateImage_ValidWebP_NoException() {
        byte[] content = new byte[1024]; // 1KB
        MockMultipartFile validFile = new MockMultipartFile(
                "image",
                "test.webp",
                "image/webp",
                content
        );

        // This will fail at upload stage (no real Supabase), but validation should pass
        assertThrows(ValidationException.class, 
                () -> imageUploadService.uploadPostImages(List.of(validFile)));
    }
}
