package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StickerRepository(private val db: AppDatabase) {
    private val dao = db.stickerDao()

    // Flows
    val allStickerPacks: Flow<List<StickerPack>> = dao.getAllStickerPacksFlow()
    val approvedStickerPacks: Flow<List<StickerPack>> = dao.getApprovedStickerPacksFlow()
    val allCreators: Flow<List<Creator>> = dao.getAllCreatorsFlow()
    val allPurchases: Flow<List<Purchase>> = dao.getAllPurchasesFlow()
    val allAnnouncements: Flow<List<Announcement>> = dao.getAllAnnouncementsFlow()
    val userSession: Flow<UserSession?> = dao.getActiveSessionFlow()

    fun getStickerPacksByCreator(creatorId: String): Flow<List<StickerPack>> {
        return dao.getStickerPacksByCreatorFlow(creatorId)
    }

    fun getStickersFlow(packId: Long): Flow<List<StickerItem>> {
        return dao.getStickersByPackFlow(packId)
    }

    suspend fun getStickers(packId: Long): List<StickerItem> {
        return dao.getStickersByPack(packId)
    }

    suspend fun getStickerPack(id: Long): StickerPack? {
        return dao.getStickerPackById(id)
    }

    suspend fun getActiveUserSession(): UserSession? {
        return dao.getActiveSession()
    }

    suspend fun loginUser(email: String, name: String, avatarUrl: String, role: String) {
        val session = UserSession(
            email = email,
            name = name,
            avatarUrl = avatarUrl,
            role = role,
            isLoggedIn = true
        )
        dao.insertSession(session)

        // If log in as creator, ensure creator record exists in DB
        if (role == "CREATOR") {
            val existingCreator = dao.getCreatorById(email)
            if (existingCreator == null) {
                dao.insertCreator(
                    Creator(
                        id = email,
                        name = name,
                        avatarUrl = avatarUrl,
                        email = email,
                        balanceEgp = 0.0
                    )
                )
            }
        }
    }

    suspend fun logout() {
        dao.deleteActiveSession()
    }

    // Purchase full package
    suspend fun purchaseFullPack(buyerEmail: String, packId: Long): Boolean {
        val pack = dao.getStickerPackById(packId) ?: return false
        val creatorId = pack.creatorId
        val amount = pack.priceEgp

        // 50% to creator, 50% to platform
        val creatorEarnings = amount * 0.5

        // Record purchase
        val purchase = Purchase(
            buyerEmail = buyerEmail,
            packId = packId,
            isCustomBundle = false,
            amountPaidEgp = amount,
            creatorId = creatorId
        )
        dao.insertPurchase(purchase)

        // Update creator balance
        dao.updateCreatorBalance(creatorId, creatorEarnings)

        // Increment downloads
        dao.incrementDownloads(packId)

        return true
    }

    // Purchase custom selected stickers
    // Receives a map of packId to List of selected StickerItem elements
    suspend fun purchaseCustomPack(buyerEmail: String, selectedStickers: List<StickerItem>): Boolean {
        if (selectedStickers.isEmpty()) return false

        // Group selected stickers by pack to calculate proportional payments
        val stickersByPack = selectedStickers.groupBy { it.packId }
        var totalCost = 0.0

        val stickerIdCsv = selectedStickers.map { it.id }.joinToString(",")

        // Create transaction breakdown
        for ((packId, selectedList) in stickersByPack) {
            val pack = dao.getStickerPackById(packId) ?: continue
            val allStickersInPack = dao.getStickersByPack(packId)
            
            val totalStickerCountInPack = if (allStickersInPack.isNotEmpty()) allStickersInPack.size else 1
            val pricePerSticker = pack.priceEgp / totalStickerCountInPack
            
            // Cost of selected stickers from this pack
            val costFromPack = pricePerSticker * selectedList.size
            totalCost += costFromPack

            // Creator gets 50% of the proportional purchase amount
            val creatorEarnings = costFromPack * 0.5
            dao.updateCreatorBalance(pack.creatorId, creatorEarnings)
            dao.incrementDownloads(packId)
        }

        // Insert purchase record (store custom purchase with packId 0)
        val purchase = Purchase(
            buyerEmail = buyerEmail,
            packId = 0, // 0 indicates a custom pack
            isCustomBundle = true,
            customStickerIdsCsv = stickerIdCsv,
            amountPaidEgp = totalCost,
            creatorId = "mixed"
        )
        dao.insertPurchase(purchase)

        return true
    }

    // Creator: Add custom sticker pack
    suspend fun addStickerPack(
        title: String,
        description: String,
        creatorId: String,
        creatorName: String,
        priceEgp: Double,
        coverUrl: String,
        stickers: List<String> // list of emojis/titles or mock paths
    ): Long {
        val pack = StickerPack(
            title = title,
            description = description,
            creatorId = creatorId,
            creatorName = creatorName,
            priceEgp = priceEgp,
            coverUrl = coverUrl,
            isApproved = false // needs admin approval
        )
        val packId = dao.insertStickerPack(pack)

        val items = stickers.map { stickerTitle ->
            StickerItem(
                packId = packId,
                title = stickerTitle,
                // Assign a placeholder or fun sticker asset/emoji representation
                imageUrl = stickerTitle
            )
        }
        dao.insertStickerItems(items)
        return packId
    }

    // Admin commands
    suspend fun approvePack(packId: Long) {
        dao.updateApprovalStatus(packId, true)
    }

    suspend fun rejectPack(packId: Long) {
        val pack = dao.getStickerPackById(packId)
        if (pack != null) {
            dao.deleteStickerPack(pack)
        }
    }

    suspend fun createAnnouncement(title: String, content: String) {
        val announcement = Announcement(
            title = title,
            content = content
        )
        dao.insertAnnouncement(announcement)
    }

    // Check if purchased
    suspend fun isPackPurchased(buyerEmail: String, packId: Long): Boolean {
        if (buyerEmail.isEmpty()) return false
        val purchase = dao.getPurchaseForPack(buyerEmail, packId)
        return purchase != null
    }

    suspend fun getStickersByIds(ids: List<Long>): List<StickerItem> {
        return dao.getStickersByIds(ids)
    }

    // Populates initial Egyptian & Mr. Bean sticker packs
    suspend fun seedDatabaseIfNeeded() {
        val existingPacks = allStickerPacks.first()
        if (existingPacks.isEmpty()) {
            // Setup default creators
            val creator1 = Creator("adel@memes.com", "عادل الكوميدي", "👨‍🎨", "adel@memes.com", 250.0)
            val creator2 = Creator("henedy@memes.com", "أبو الفوارس الكوميك", "🤠", "henedy@memes.com", 180.0)
            val creator3 = Creator("bean@memes.com", "Mr Bean Fanatic", "🇬🇧", "bean@memes.com", 120.0)
            
            dao.insertCreator(creator1)
            dao.insertCreator(creator2)
            dao.insertCreator(creator3)

            // Pack 1: Adel Emam Reactions
            val pack1Id = dao.insertStickerPack(StickerPack(
                title = "أفشات الزعيم عادل إمام",
                description = "كوميك رياكشنات استثنائية من مسرحيات وأفلام الزعيم عادل إمام جاهزة للواتس اب!",
                creatorId = creator1.id,
                creatorName = creator1.name,
                coverUrl = "🎭",
                priceEgp = 15.0,
                isApproved = true,
                downloads = 142
            ))
            
            val pack1Stickers = listOf(
                "متعودة دايماً! 💃",
                "يا شيخ إسماعيل بص بقى.. 👀",
                "بلد شهادات صحيح 📜",
                "أنا شربت حشيش يا سعادة البيه 🚬",
                "الجوازة باظت يا رجالة! 💍",
                "ده أنا غلبان ومثلي الأعلى مستر بين 🤖"
            ).map { StickerItem(packId = pack1Id, title = it, imageUrl = it) }
            dao.insertStickerItems(pack1Stickers)

            // Pack 2: Mr Bean Meme Special (Fulfills the bean motif)
            val pack2Id = dao.insertStickerPack(StickerPack(
                title = "ملصقات مستر بين الكوميدية",
                description = "تشكيلة مضحكة لمستر بين في مواجهة مشاكل الحياة والامتحانات وبصحبة دبدوبه تيدي!",
                creatorId = creator3.id,
                creatorName = creator3.name,
                coverUrl = "🐻",
                priceEgp = 20.0,
                isApproved = true,
                downloads = 310
            ))
            
            val pack2Stickers = listOf(
                "ضحكة مستر بين الخبيثة 😏",
                "تيدي حبيبي! 🧸",
                "لما تشوف ورقة الامتحان 😱",
                "تسجيل الدخول بطريقة مستر بين 😎",
                "إشارة النصر العبيطة ✌️",
                "مستر بين وهو عارض خدوده 😁",
                "يا نهار أزرق ومقندل! 🫠"
            ).map { StickerItem(packId = pack2Id, title = it, imageUrl = it) }
            dao.insertStickerItems(pack2Stickers)

            // Pack 3: Mohamed Henedy Super reactions
            val pack3Id = dao.insertStickerPack(StickerPack(
                title = "رياكشنز الكوميديان محمد هنيدي",
                description = "الجاحد والخال والمحترم! أحسن قفشات محمد هنيدي بأعلى جودة.",
                creatorId = creator2.id,
                creatorName = creator2.name,
                coverUrl = "🎬",
                priceEgp = 12.0,
                isApproved = true,
                downloads = 88
            ))

            val pack3Stickers = listOf(
                "الحب الحب.. الشوق الشوق! 🥰",
                "والله العظيم قعدتي معاك خسارة 😡",
                "الواحد مضطر يبتسم وكأنه سعيد 🥲",
                "مين فينا مغلطش؟ مين يا فخري؟ 😂",
                "الناس لبعضيها يا وحش الكون 🤝"
            ).map { StickerItem(packId = pack3Id, title = it, imageUrl = it) }
            dao.insertStickerItems(pack3Stickers)

            // Seed an Admin notification announcement
            dao.insertAnnouncement(Announcement(
                title = "🎉 أهلاً بكم في تطبيق مميز memes للقرن!",
                content = "المنصة الأولى والوحيدة بمصر للاستيكرات والميمز المربحة. تم تفكيك الأرباح 50% للكريتورز تلقائياً مع نظام الدفع الأسهل بالجنيه المصري! صانع الاستيكرات مستر بين يرحب بكم."
            ))
        }
    }
}
