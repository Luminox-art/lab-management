package com.example.labmanagement.catalog.application;

public record CatalogPageMeta(int page, int size, long totalElements, int totalPages, String sort) {
}
