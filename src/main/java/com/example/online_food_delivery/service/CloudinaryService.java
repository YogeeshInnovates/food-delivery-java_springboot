package com.example.online_food_delivery.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.online_food_delivery.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Upload an image to Cloudinary and return the secure URL.
     *
     * @param file   the multipart file
     * @param folder the Cloudinary folder (e.g. "restaurants" or "menu-items")
     * @return the secure URL string
     */
    public String uploadImage(MultipartFile file, String folder) {
        // Validate file type
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Invalid file type. Only JPG, PNG, and WEBP are allowed.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 5MB limit.");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "food-delivery/" + folder,
                            "resource_type", "image"
                    ));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Delete an image from Cloudinary by its secure URL.
     *
     * @param imageUrl the secure Cloudinary URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }

    private String extractPublicId(String imageUrl) {
        String uploadMarker = "/upload/";
        int idx = imageUrl.indexOf(uploadMarker);
        if (idx == -1) return imageUrl;
        String afterUpload = imageUrl.substring(idx + uploadMarker.length());
        afterUpload = afterUpload.replaceFirst("^v?\\d+/", "");
        int dot = afterUpload.lastIndexOf('.');
        if (dot != -1) afterUpload = afterUpload.substring(0, dot);
        return afterUpload;
    }
}
