package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {

    // Sticker Packs
    @Query("SELECT * FROM sticker_packs ORDER BY id DESC")
    fun getAllStickerPacksFlow(): Flow<List<StickerPack>>

    @Query("SELECT * FROM sticker_packs WHERE isApproved = 1 ORDER BY id DESC")
    fun getApprovedStickerPacksFlow(): Flow<List<StickerPack>>

    @Query("SELECT * FROM sticker_packs WHERE creatorId = :creatorId ORDER BY id DESC")
    fun getStickerPacksByCreatorFlow(creatorId: String): Flow<List<StickerPack>>

    @Query("SELECT * FROM sticker_packs WHERE id = :packId")
    suspend fun getStickerPackById(packId: Long): StickerPack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickerPack(stickerPack: StickerPack): Long

    @Update
    suspend fun updateStickerPack(stickerPack: StickerPack)

    @Delete
    suspend fun deleteStickerPack(stickerPack: StickerPack)

    @Query("UPDATE sticker_packs SET isApproved = :approved WHERE id = :packId")
    suspend fun updateApprovalStatus(packId: Long, approved: Boolean)

    @Query("UPDATE sticker_packs SET downloads = downloads + 1 WHERE id = :packId")
    suspend fun incrementDownloads(packId: Long)


    // Sticker Items
    @Query("SELECT * FROM sticker_items WHERE packId = :packId ORDER BY id ASC")
    fun getStickersByPackFlow(packId: Long): Flow<List<StickerItem>>

    @Query("SELECT * FROM sticker_items WHERE packId = :packId")
    suspend fun getStickersByPack(packId: Long): List<StickerItem>

    @Query("SELECT * FROM sticker_items WHERE id IN (:stickerIds)")
    suspend fun getStickersByIds(stickerIds: List<Long>): List<StickerItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickerItem(stickerItem: StickerItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickerItems(stickerItems: List<StickerItem>)


    // Creators
    @Query("SELECT * FROM creators WHERE id = :creatorId")
    suspend fun getCreatorById(creatorId: String): Creator?

    @Query("SELECT * FROM creators ORDER BY balanceEgp DESC")
    fun getAllCreatorsFlow(): Flow<List<Creator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreator(creator: Creator)

    @Query("UPDATE creators SET balanceEgp = balanceEgp + :amount WHERE id = :creatorId")
    suspend fun updateCreatorBalance(creatorId: String, amount: Double)


    // Purchases
    @Query("SELECT * FROM purchases WHERE buyerEmail = :email")
    fun getPurchasesByBuyerFlow(email: String): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE buyerEmail = :email AND packId = :packId")
    suspend fun getPurchaseForPack(email: String, packId: Long): Purchase?

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchasesFlow(): Flow<List<Purchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long


    // Announcements
    @Query("SELECT * FROM announcements ORDER BY date DESC")
    fun getAllAnnouncementsFlow(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement)


    // User Sessions (Single Active Session)
    @Query("SELECT * FROM user_sessions WHERE id = 1 LIMIT 1")
    fun getActiveSessionFlow(): Flow<UserSession?>

    @Query("SELECT * FROM user_sessions WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession)

    @Query("DELETE FROM user_sessions WHERE id = 1")
    suspend fun deleteActiveSession()
}
