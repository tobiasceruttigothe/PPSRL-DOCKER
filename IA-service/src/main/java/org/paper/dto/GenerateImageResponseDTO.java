package org.paper.dto;

public class GenerateImageResponseDTO {
    private String imageBase64;
    private String status;

    public GenerateImageResponseDTO() {
    }

    public GenerateImageResponseDTO(String imageBase64, String status) {
        this.imageBase64 = imageBase64;
        this.status = status;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

