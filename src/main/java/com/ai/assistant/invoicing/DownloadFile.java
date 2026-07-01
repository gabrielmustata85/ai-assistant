package com.ai.assistant.invoicing;

/** Un fișier gata de descărcat (originalul stocat sau PDF-ul generat). */
public record DownloadFile(byte[] data, String filename, String contentType) {
}
