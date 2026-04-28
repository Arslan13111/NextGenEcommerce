package com.example.nextgenecommerce.data.local

import androidx.room.TypeConverter
import com.example.nextgenecommerce.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromOrderItems(value: List<OrderItem>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toOrderItems(value: String): List<OrderItem> {
        val listType = object : TypeToken<List<OrderItem>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromAddress(value: Address?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAddress(value: String?): Address? {
        if (value == null || value == "null") return null
        return gson.fromJson(value, Address::class.java)
    }

    @TypeConverter
    fun fromProductCategory(value: ProductCategory): String {
        return value.name
    }

    @TypeConverter
    fun toProductCategory(value: String): ProductCategory {
        return ProductCategory.valueOf(value)
    }

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String {
        return value.name
    }

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus {
        return OrderStatus.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus {
        return PaymentStatus.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod {
        return PaymentMethod.valueOf(value)
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String {
        return value.name
    }

    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return NotificationType.valueOf(value)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
}
