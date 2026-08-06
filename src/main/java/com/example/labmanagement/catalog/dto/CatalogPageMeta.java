package com.example.labmanagement.catalog.dto;

public record CatalogPageMeta(int page, int size, long totalElements, int totalPages, String sort) {
}
