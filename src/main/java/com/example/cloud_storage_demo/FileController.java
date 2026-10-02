package com.example.cloud_storage_demo;// نام پکیج باید با پکیج اصلی پروژه شما یکی باشد

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files") // تمام APIهای این کلاس با این آدرس شروع می‌شوند
public class FileController {

    // تعیین مسیر ذخیره‌سازی فایل‌ها (یک پوشه به نام uploads در ریشه پروژه ایجاد می‌شود)
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    // سازنده کلاس (Constructor): هنگام بالا آمدن برنامه به صورت خودکار اجرا می‌شود
    public FileController() {
        try {
            // ساخت پوشه uploads روی سیستم، اگر از قبل وجود نداشته باشد
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("امکان ساخت پوشه ذخیره‌سازی فایل وجود ندارد!", ex);
        }
    }

    // ==========================================
    // بخش اول: API آپلود فایل (POST)
    // ==========================================
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // ۱. دریافت نام اصلی فایل ارسال‌شده
            String fileName = file.getOriginalFilename();

            // بررسی خالی نبودن نام فایل
            if (fileName == null || fileName.contains("..")) {
                return ResponseEntity.badRequest().body("نام فایل نامعتبر است!");
            }

            // ۲. مشخص کردن مسیر دقیق ذخیره فایل داخل پوشه uploads
            Path targetLocation = this.fileStorageLocation.resolve(fileName);

            // ۳. کپی و ذخیره فایل روی دیسک (در صورت وجود فایل هم‌نام، آن را جایگزین می‌کند)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("فایل با موفقیت آپلود شد: " + fileName);

        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body("خطا در ذخیره‌سازی فایل: " + ex.getMessage());
        }
    }

    // ==========================================
    // بخش دوم: API دانلود فایل (GET)
    // ==========================================
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            // ۱. پیدا کردن مسیر فایل بر اساس نام درخواستی
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // ۲. بررسی وجود داشتن فایل روی دیسک
            if (resource.exists() && resource.isReadable()) {
                // ارسال فایل به کاربر همراه با هدرهای استاندارد دانلود
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM) // مشخص کردن جریان بایت
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"") // دستور مرورگر برای دانلود
                        .body(resource);
            } else {
                // اگر فایل پیدا نشد
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
