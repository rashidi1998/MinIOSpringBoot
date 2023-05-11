package com.example.minioclient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UploadedFileInfo {
    private final String etag;
    private final String path;

}
