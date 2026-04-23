package com.dxc.dxc_platform.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String filePath) {
        try {
            String decodedPath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);

            // on enlève /uploads/ si présent
            if (decodedPath.startsWith("/uploads/")) {
                decodedPath = decodedPath.substring("/uploads/".length());
            }

            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path fullPath = basePath.resolve(decodedPath).normalize();

            if (!fullPath.startsWith(basePath)) {
                return ResponseEntity.badRequest().build();
            }

            File file = fullPath.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String fileName = StringUtils.getFilename(file.getName());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}