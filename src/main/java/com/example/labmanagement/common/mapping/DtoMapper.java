package com.example.labmanagement.common.mapping;

@FunctionalInterface
public interface DtoMapper<S, T> {

	T toDto(S source);
}
