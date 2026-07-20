package com.example.online_food_delivery.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryCleanupJob {

    private final Cloudinary cloudinary;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    private static final String FOLDER_PREFIX = "food-delivery/";

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanedImages() {
        log.info("Starting Cloudinary orphaned image cleanup...");

        try {
            Set<String> dbPublicIds = collectDbPublicIds();
            log.info("Collected {} referenced image public_ids from database", dbPublicIds.size());

            Set<String> cloudinaryPublicIds = collectCloudinaryPublicIds();
            log.info("Found {} total images in Cloudinary", cloudinaryPublicIds.size());

            Set<String> orphaned = new HashSet<>(cloudinaryPublicIds);
            orphaned.removeAll(dbPublicIds);

            if (orphaned.isEmpty()) {
                log.info("No orphaned images found — all Cloudinary images are referenced in DB");
                return;
            }

            log.info("Found {} orphaned images to delete", orphaned.size());
            for (String publicId : orphaned) {
                try {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    log.info("Deleted orphaned image: {}", publicId);
                } catch (Exception e) {
                    log.error("Failed to delete orphaned image: {}", publicId, e);
                }
            }
            log.info("Cloudinary cleanup completed — deleted {} orphaned images", orphaned.size());

        } catch (Exception e) {
            log.error("Cloudinary cleanup job failed", e);
        }
    }

    private Set<String> collectDbPublicIds() {
        Set<String> publicIds = new HashSet<>();

        restaurantRepository.findAll().forEach(r -> {
            if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
                String pid = extractPublicId(r.getImageUrl());
                if (pid != null) publicIds.add(pid);
            }
        });

        menuItemRepository.findAll().forEach(m -> {
            if (m.getImageUrl() != null && !m.getImageUrl().isBlank()) {
                String pid = extractPublicId(m.getImageUrl());
                if (pid != null) publicIds.add(pid);
            }
        });

        userRepository.findAll().forEach(u -> {
            if (u.getProfileImageUrl() != null && !u.getProfileImageUrl().isBlank()) {
                String pid = extractPublicId(u.getProfileImageUrl());
                if (pid != null) publicIds.add(pid);
            }
        });

        return publicIds;
    }

    private Set<String> collectCloudinaryPublicIds() throws Exception {
        Set<String> publicIds = new HashSet<>();
        String nextCursor = null;

        do {
            Map<String, Object> params = ObjectUtils.asMap(
                    "type", "upload",
                    "prefix", FOLDER_PREFIX,
                    "max_results", 500
            );
            if (nextCursor != null) {
                params.put("next_cursor", nextCursor);
            }

            ApiResponse response = cloudinary.api().resources(params);

            List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");
            if (resources != null) {
                for (Map<String, Object> resource : resources) {
                    publicIds.add((String) resource.get("public_id"));
                }
            }

            nextCursor = (String) response.get("next_cursor");
        } while (nextCursor != null);

        return publicIds;
    }

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        if (!imageUrl.contains("cloudinary")) return null;

        String marker = "/upload/";
        int idx = imageUrl.indexOf(marker);
        if (idx == -1) return null;

        String after = imageUrl.substring(idx + marker.length());
        after = after.replaceFirst("^v?\\d+/", "");
        int dot = after.lastIndexOf('.');
        if (dot != -1) after = after.substring(0, dot);

        return after.isBlank() ? null : after;
    }
}
