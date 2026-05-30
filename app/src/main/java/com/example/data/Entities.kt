package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creators")
data class Creator(
    @PrimaryKey val id: String, // email or generated ID
    val name: String,
    val avatarUrl: String,
    val email: String,
    val balanceEgp: Double = 0.0,
    val registrationDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "sticker_packs")
data class StickerPack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // title of pack
    val description: String,
    val creatorId: String, // ID of creator
    val creatorName: String,
    val coverUrl: String, // Cover image
    val priceEgp: Double, // Price in Egyptian Pounds
    val isApproved: Boolean = false, // Approved by admin
    val downloads: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sticker_items")
data class StickerItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: Long,
    val title: String,
    val imageUrl: String // Image URL or local resource key
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val buyerEmail: String,
    val packId: Long, // 0 if custom sticker pack
    val isCustomBundle: Boolean = false,
    val customStickerIdsCsv: String = "", // comma-separated ids if custom
    val amountPaidEgp: Double,
    val creatorId: String, // Who made it (or "platform" if mixed, but we calculate creator's percentage)
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val date: Long = System.currentTimeMillis(),
    val sentBy: String = "Admin"
)

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val id: Int = 1,
    val email: String,
    val name: String,
    val avatarUrl: String,
    val role: String, // "BUYER", "CREATOR", "ADMIN"
    val isLoggedIn: Boolean = false
)
