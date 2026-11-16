package vn.shoegroup.sd11.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    @Value("${app.upload.dir:uploads/products}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path directory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String location = directory.toUri().toString();
            if (!location.endsWith("/")) location += "/";
            registry.addResourceHandler("/uploads/products/**")
                    .addResourceLocations(location);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể tạo thư mục lưu ảnh: " + uploadDirectory, ex);
        }
    }
}
