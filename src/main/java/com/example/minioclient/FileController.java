package com.example.minioclient;

import io.minio.ObjectWriteResponse;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final MinIOService minioService;

    @Autowired
    public FileController(MinIOService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadedFileInfo> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Get the input stream from the uploaded file
            InputStream inputStream = file.getInputStream();


            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
            String folderName = getFolderName();
            String objectName = folderName + "/" + uniqueFileName;
            // Generate a unique object name based on the file name
//            String objectName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Call the MinIO service to upload the file
            UploadedFileInfo response =  minioService.uploadFile("my-bucket", objectName, inputStream);



            // Return the path and ETag in the response
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            // Handle exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (MinioException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFolderName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return dateFormat.format(new Date());
    }

    private String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        return uniqueFileName;
    }

    private String getFileExtension(String originalFilename) {
        int extensionIndex = originalFilename.lastIndexOf(".");
        if (extensionIndex >= 0) {
            return originalFilename.substring(extensionIndex);
        }
        return "";
    }

    @GetMapping("/{bucketName}/{objectName}")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String bucketName, @PathVariable String objectName) {
        try {
            InputStream fileStream = minioService.getFile(bucketName, objectName);

            // Convert the file stream to a byte array (adjust according to your needs)


            // Set appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", objectName);
            String[] objectParts = objectName.split("/");
            String uniqueFileName= objectParts[objectParts.length -1];
            String folderName = getFolderName();
//            String objName = folderName + "/" + uniqueFileName;
            InputStreamResource downloadLink = minioService.downloadImage(getFolderName(), uniqueFileName,bucketName).getBody();
           if (downloadLink!= null){
               headers.setContentDisposition(ContentDisposition.attachment().filename(objectName).build());
               return ResponseEntity.ok().headers(headers).body(downloadLink);
           } else {
               return ResponseEntity.notFound().build();
           }
//            headers.add("X-Download-Link", downloadLink);
//            headers.setContentType(Media/Type.ALL);
        } catch (MinioException | IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @DeleteMapping("/{bucketName}/{objectName}")
    public ResponseEntity<String> deleteFile(@PathVariable String bucketName, @PathVariable String objectName) {
        try {
            minioService.deleteFile(bucketName, objectName);
            return ResponseEntity.ok("File deleted successfully");
        } catch (MinioException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete file");
        }
    }

    }
