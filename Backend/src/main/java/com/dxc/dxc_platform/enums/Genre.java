package com.dxc.dxc_platform.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Genre {
    FEMME(1),
    HOMME(2);

    private final int value;

    Genre(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Genre fromValue(Integer value) {
        if (value == null) {
            return null;
        }

        for (Genre genre : Genre.values()) {
            if (genre.value == value) {
                return genre;
            }
        }

        throw new IllegalArgumentException("Valeur invalide pour Genre: " + value);
    }

    @Converter(autoApply = false)
    public static class GenreConverter implements AttributeConverter<Genre, Integer> {

        @Override
        public Integer convertToDatabaseColumn(Genre genre) {
            return genre != null ? genre.getValue() : null;
        }

        @Override
        public Genre convertToEntityAttribute(Integer dbData) {
            return Genre.fromValue(dbData);
        }
    }
}