package com.acmestack.chat;

import java.util.Map;
import java.util.Set;

public final class FileTypes {

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"), Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip"));

    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "gif", "webp");

    public static final String ACCEPT = ".pdf,.png,.jpg,.jpeg,.gif,.webp,.txt,.csv,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip";

    private FileTypes() {}

    public static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    public static boolean allowed(String name)  { return TYPES.containsKey(extension(name)); }
    public static boolean isImage(String name)  { return IMAGES.contains(extension(name)); }

    public static String contentType(String name) {
        return TYPES.getOrDefault(extension(name), "application/octet-stream");
    }

    public static String icon(String name) {
        return switch (extension(name)) {
            case "pdf" -> "bi-file-earmark-pdf";
            case "doc", "docx" -> "bi-file-earmark-word";
            case "xls", "xlsx", "csv" -> "bi-file-earmark-excel";
            case "ppt", "pptx" -> "bi-file-earmark-slides";
            case "zip" -> "bi-file-earmark-zip";
            case "txt" -> "bi-file-earmark-text";
            case "png", "jpg", "jpeg", "gif", "webp" -> "bi-file-earmark-image";
            default -> "bi-file-earmark";
        };
    }

    public static String color(String name) {
        return switch (extension(name)) {
            case "pdf" -> "red";
            case "doc", "docx" -> "blue";
            case "xls", "xlsx", "csv" -> "green";
            case "ppt", "pptx" -> "orange";
            case "png", "jpg", "jpeg", "gif", "webp" -> "indigo";
            default -> "gray";
        };
    }

    public static String cleanName(String raw) {
        if (raw == null) return "file";
        String n = raw.substring(Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\')) + 1);
        n = n.replaceAll("\\p{Cntrl}", "").strip();
        if (n.length() > 150) n = n.substring(n.length() - 150);   // запазва разширението
        return n.isEmpty() ? "file" : n;
    }
}