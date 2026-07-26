package com.ccb.lighting.common;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用文件上传 Controller
 *
 * <p>路径前缀 /common/upload，支持上传图片、视频等文件。
 * 文件按日期分目录存储，返回可访问的 URL。</p>
 *
 * <p>上传路径由 application.yml 的 lighting.upload-path 配置。
 * 静态资源映射由 WebMvcConfig.addResourceHandlers 完成。</p>
 */
@Slf4j
@RestController
@RequestMapping("/common/upload")
public class UploadController {

    @Value("${lighting.upload-path:./uploads}")
    private String uploadPath;

    /** 允许的图片扩展名 */
    private static final String[] IMAGE_EXT = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
    /** 允许的视频扩展名 */
    private static final String[] VIDEO_EXT = {"mp4", "avi", "mov", "wmv", "flv", "mkv"};

    @PostConstruct
    public void init() {
        log.info("文件上传路径：{}", uploadPath);
    }

    /**
     * 通用文件上传
     *
     * @param file 上传的文件
     * @return { url: "http://.../uploads/2026/07/xxx.jpg", originalName: "xxx.jpg", size: 12345 }
     */
    @PostMapping
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 1. 校验文件扩展名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!isAllowedExt(ext)) {
            return Result.error("不支持的文件类型：" + ext + "，仅支持图片和视频文件");
        }

        // 2. 按日期分目录，避免单目录文件过多
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            Path targetDir = Paths.get(uploadPath, dateDir);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(newFileName);
            file.transferTo(targetFile.toFile());

            // 3. 构造可访问 URL（由 addResourceHandlers 映射 /uploads/** → upload-path）
            //    spring.servlet.context-path=/api，所以完整路径是 /api/uploads/...
            String url = "/api/uploads/" + dateDir + "/" + newFileName;
            log.info("文件上传成功：{} ({} bytes)", url, file.getSize());

            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("originalName", originalName);
            result.put("size", file.getSize());
            result.put("ext", ext);
            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 判断扩展名是否允许（图片或视频）
     */
    private boolean isAllowedExt(String ext) {
        for (String e : IMAGE_EXT) {
            if (e.equals(ext)) return true;
        }
        for (String e : VIDEO_EXT) {
            if (e.equals(ext)) return true;
        }
        return false;
    }
}
