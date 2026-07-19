package com.example.nzreceiptapp.data.local;

import androidx.room.TypeConverter;

import com.example.nzreceiptapp.domain.model.ItemDiscount;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Converters {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @TypeConverter
    public static LocalDateTime fromTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value, formatter);
    }

    @TypeConverter
    public static String dateToTimestamp(LocalDateTime date) {
        return date == null ? null : date.format(formatter);
    }

    @TypeConverter
    public static String fromDiscountType(ItemDiscount.DiscountType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static ItemDiscount.DiscountType toDiscountType(String value) {
        return value == null ? null : ItemDiscount.DiscountType.valueOf(value);
    }
}
