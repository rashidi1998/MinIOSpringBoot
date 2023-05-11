package com.example.minioclient;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class MinIOService {
    private final MinioClient minioClient;

    @Autowired
    public MinIOService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public UploadedFileInfo uploadFile(String bucketName, String objectName, InputStream inputStream) throws MinioException {
        try {

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
            String etag = minioClient.statObject(bucketName, objectName).etag();
            String path = objectName;
            return new UploadedFileInfo(etag,path);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            // Handle exception
            throw new MinioException();
        }
    }

    public InputStream getFile(String bucketName, String objectName) throws MinioException {
        try {
            return minioClient.getObject(bucketName, objectName);
        } catch (InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public String getDownloadLink(String bucketName, String objectName) throws MinioException {
        try {
            return minioClient.presignedGetObject(bucketName, objectName);
        } catch (Exception e) {
            throw new MinioException();
        }
    }
    public void deleteFile(String bucketName, String objectName) throws MinioException {
        try {
            minioClient.removeObject(bucketName, objectName);
        } catch (InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public ResponseEntity<InputStreamResource> downloadImage(String folderPath, String fileName, String bucketName) throws IOException, MinioException {
        String objectName = StringUtils.cleanPath(folderPath + "/" + fileName);
        InputStream inputStream = null;
        try {
            inputStream = minioClient.getObject(bucketName, objectName);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    public String getImagePreviewURL(String folderPath, String fileName, String bucketName) {
        String objectName = StringUtils.cleanPath(folderPath + "/" + fileName);
        try {
            return minioClient.presignedGetObject(bucketName, objectName);
        } catch (ErrorResponseException | XmlParserException | ServerException | NoSuchAlgorithmException |
                 IOException | InvalidResponseException | InvalidKeyException | InvalidExpiresRangeException |
                 InvalidBucketNameException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }
}
