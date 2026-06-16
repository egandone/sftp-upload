package com.egandone.sftpupload.web;

import com.egandone.sftpupload.storage.StorageService;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("files", storageService.listFiles());
        } catch (Exception exception) {
            model.addAttribute("files", Collections.emptyList());
            model.addAttribute("error", "Unable to list files: " + exception.getMessage());
        }
        return "index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/";
        }

        try {
            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
            storageService.store(filename, file.getInputStream());
            redirectAttributes.addFlashAttribute("message", "Uploaded " + filename);
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + exception.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) throws IOException {
        try {
            byte[] content = storageService.read(filename);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(encodedFilename, StandardCharsets.UTF_8).build().toString())
                    .body(content);
        } catch (Exception exception) {
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Download failed: " + exception.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
