# Image Upload Service

## Overview

The `ImageUploadService` provides functionality to upload images to Supabase Storage with validation and error handling.

## Features

- **Image Validation**: Validates image size (max 5MB) and format (JPEG, PNG, WebP)
- **Unique Filenames**: Generates UUID-based filenames to prevent collisions
- **Public URLs**: Returns HTTPS URLs for uploaded images
- **Cleanup on Failure**: Automatically cleans up partial uploads if any upload fails
- **Thread-safe**: Uses Java's HttpClient for concurrent uploads

## Usage

### Basic Usage

```java
@Autowired
private ImageUploadService imageUploadService;

public void createPost(List<MultipartFile> images) {
    try {
        // Upload images and get URLs
        String[] imageUrls = imageUploadService.uploadPostImages(images);
        
        // Store URLs in database
        post.setImages(imageUrls);
        postRepository.save(post);
        
    } catch (ValidationException e) {
        // Handle validation errors
        log.error("Image upload failed: {}", e.getMessage());
    }
}
```

### Validation Rules

1. **File Size**: Each image must be under 5MB
2. **File Format**: Only JPEG, PNG, and WebP formats are supported
3. **Content Type**: Validates both content type header and file extension

### Error Handling

The service throws `ValidationException` in the following cases:

- Image file is empty
- Image size exceeds 5MB
- Image format is not JPEG, PNG, or WebP
- Upload to Supabase Storage fails

### Cleanup Behavior

If any image in a batch fails to upload:
1. All previously uploaded images in the batch are deleted
2. A `ValidationException` is thrown with details
3. No partial data is left in storage

## Configuration

Required environment variables:

```properties
supabase.url=https://your-project.supabase.co
supabase.service-key=your-service-key
supabase.storage.bucket=post-images
```

## Implementation Details

### Filename Generation

Filenames are generated using UUID v4 to ensure uniqueness:
```
{uuid}.{original-extension}
Example: 550e8400-e29b-41d4-a716-446655440000.jpg
```

### Public URL Format

```
https://your-project.supabase.co/storage/v1/object/public/{bucket}/{filename}
```

### Supported Formats

| Format | Content Type | Extensions |
|--------|-------------|------------|
| JPEG   | image/jpeg  | .jpg, .jpeg |
| PNG    | image/png   | .png |
| WebP   | image/webp  | .webp |

## Testing

Unit tests are provided in `ImageUploadServiceTest.java`:

- Empty/null list handling
- File size validation
- Format validation
- Empty file detection
- Valid format acceptance

## Security Considerations

1. **Service Key**: Uses Supabase service key for authenticated uploads
2. **File Validation**: Validates both content type and file extension
3. **Size Limits**: Enforces 5MB limit to prevent abuse
4. **Public Access**: Uploaded images are publicly accessible via HTTPS

## Future Enhancements

- Image compression/optimization
- Thumbnail generation
- Batch upload optimization
- Progress tracking for large uploads
- Image metadata extraction
