package com.example.labmanagement.scheduling.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Converter
public class LocalTimeAttributeConverter implements AttributeConverter<LocalTime, String> {

	private static final DateTimeFormatter DATABASE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Override
	public String convertToDatabaseColumn(LocalTime value) {
		return value == null ? null : DATABASE_FORMAT.format(value);
	}

	@Override
	public LocalTime convertToEntityAttribute(String value) {
		return value == null ? null : LocalTime.parse(value, DATABASE_FORMAT);
	}
}
