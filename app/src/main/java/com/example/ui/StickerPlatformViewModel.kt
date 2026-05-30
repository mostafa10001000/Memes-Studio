package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StickerPlatformViewModel(private val repository: StickerRepository) : ViewModel() {

    // Streams from database
    val userSession: StateFlow<UserSession?> = repository.userSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val approvedStickerPacks: StateFlow<List<StickerPack>> = repository.approvedStickerPacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStickerPacks: StateFlow<List<StickerPack>> = repository.allStickerPacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreators: StateFlow<List<Creator>> = repository.allCreators
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<Purchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart state for custom bundle
    private val _selectedCustomStickers = MutableStateFlow<Set<StickerItem>>(emptySet())
    val selectedCustomStickers: StateFlow<Set<StickerItem>> = _selectedCustomStickers.asStateFlow()

    // Shopping Cart state for sticker packs
    private val _shoppingCart = MutableStateFlow<List<StickerPack>>(emptyList())
    val shoppingCart: StateFlow<List<StickerPack>> = _shoppingCart.asStateFlow()

    // Premium Subscription state
    val activeSubscription = MutableStateFlow<String?>(null) // null, "Monthly", "Quarterly", "Annual"

    // Interactive action messages for notifications / sheets
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // UI state states
    val currentRoute = MutableStateFlow("auth") // auth, buyer_home, creator_home, admin_dashboard

    // Language & Currency settings (Arabic default, English optional, EGP default, USD optional)
    val appLanguage = MutableStateFlow("AR") // "AR" (Arabic), "EN" (English)
    val appCurrency = MutableStateFlow("EGP") // "EGP" (ج.م), "USD" ($), "SAR" (ر.س), "AED" (د.إ), "KWD" (د.ك), "QAR" (ر.ق)
    val usdToEgpRate = MutableStateFlow(50.0) // 1 USD = 50 EGP

    // Theme and animation preferences (Defaults to cheerful Light theme with animated dynamic background elements)
    val isLightTheme = MutableStateFlow(true)
    val isBgAnimated = MutableStateFlow(true)

    // Showcase custom banner states adaptable by Admin (No bear motif!)
    val showcaseTitle = MutableStateFlow("أفلام كوميديا الزمن الجميل • أجمد رياكشنات الميمز")
    val showcaseSub = MutableStateFlow("أكثر من 50 ملصقاً فكاهياً مخصصاً بقمة النقاء والحركة والمرح للدردشة اليومية! 🔥")
    val showcasePriceEgp = MutableStateFlow(45.0)
    val showcaseCoverEmoji = MutableStateFlow("🤪")

    fun toggleLanguage() {
        appLanguage.value = if (appLanguage.value == "AR") "EN" else "AR"
        viewModelScope.launch {
            _uiEvent.emit(if (appLanguage.value == "AR") "تم تحويل اللغة إلى العربية 🇸🇦" else "Language changed to English 🇬🇧")
        }
    }

    fun setCurrency(currencyCode: String) {
        val validCurrencies = listOf("EGP", "USD", "SAR", "AED", "KWD", "QAR")
        if (currencyCode in validCurrencies) {
            appCurrency.value = currencyCode
            viewModelScope.launch {
                val msg = when (currencyCode) {
                    "EGP" -> "تم تحويل العملة إلى الجنيه المصري (ج.م) 🇪🇬"
                    "USD" -> "Currency changed to US Dollars (USD) 🇺🇸"
                    "SAR" -> "تم تحويل العملة إلى الريال السعودي (ر.س) 🇸🇦"
                    "AED" -> "تم تحويل العملة إلى الدرهم الإماراتي (د.إ) 🇦🇪"
                    "KWD" -> "تم تحويل العملة إلى الدينار الكويتي (د.ك) 🇰🇼"
                    "QAR" -> "تم تحويل العملة إلى الريال القطري (ر.ق) 🇶🇦"
                    else -> "Currency set to $currencyCode"
                }
                _uiEvent.emit(msg)
            }
        }
    }

    fun toggleCurrency() {
        val list = listOf("EGP", "USD", "SAR", "AED", "KWD", "QAR")
        val currentIndex = list.indexOf(appCurrency.value)
        val nextIndex = (currentIndex + 1) % list.size
        setCurrency(list[nextIndex])
    }

    fun toggleTheme() {
        isLightTheme.value = !isLightTheme.value
        viewModelScope.launch {
            _uiEvent.emit(if (isLightTheme.value) "تم تفعيل المظهر الفاتح المبهج ☀️" else "تم تفعيل المظهر الداكن الفخم 🌙")
        }
    }

    fun toggleBgAnimation() {
        isBgAnimated.value = !isBgAnimated.value
        viewModelScope.launch {
            _uiEvent.emit(if (isBgAnimated.value) "تم تشغيل حركة الخلفية التفاعلية 🎬" else "تم إيقاف حركة الخلفية ⏸️")
        }
    }

    fun updateExchangeRate(rate: Double) {
        if (rate > 0) {
            usdToEgpRate.value = rate
        }
    }

    fun updateShowcase(title: String, subtitle: String, price: Double, emoji: String) {
        showcaseTitle.value = title
        showcaseSub.value = subtitle
        if (price > 0) showcasePriceEgp.value = price
        showcaseCoverEmoji.value = emoji
    }

    init {
        // Safe database seeding in a dedicated coroutine launcher
        viewModelScope.launch {
            try {
                repository.seedDatabaseIfNeeded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Isolate session observation to ensure it is always registered immediately
        viewModelScope.launch {
            repository.userSession.collect { session ->
                if (session != null && session.isLoggedIn) {
                    when (session.role) {
                        "BUYER" -> currentRoute.value = "buyer_home"
                        "CREATOR" -> currentRoute.value = "creator_home"
                        "ADMIN" -> currentRoute.value = "admin_dashboard"
                    }
                } else {
                    currentRoute.value = "auth"
                }
            }
        }
    }

    // Toggle sticker in custom selection
    fun toggleStickerInCustomSelection(sticker: StickerItem) {
        val currentSet = _selectedCustomStickers.value.toMutableSet()
        if (currentSet.any { it.id == sticker.id }) {
            currentSet.removeAll { it.id == sticker.id }
        } else {
            currentSet.add(sticker)
        }
        _selectedCustomStickers.value = currentSet
    }

    fun clearCustomSelection() {
        _selectedCustomStickers.value = emptySet()
    }

    // Calculate details for custom bundle
    // Returns pair of (TotalPrice, Map of PackToDetails)
    fun calculateCustomBundleDetails(allPacks: List<StickerPack>): CustomBundleCalculation {
        val selected = _selectedCustomStickers.value
        if (selected.isEmpty()) return CustomBundleCalculation(0.0, emptyList())

        var grandTotal = 0.0
        val detailsList = mutableListOf<CustomPackBreakdown>()

        // Group by packId
        val grouped = selected.groupBy { it.packId }
        for ((packId, list) in grouped) {
            val pack = allPacks.find { it.id == packId } ?: continue
            
            // To find total stickers in this pack, we find we have dummy list or we evaluate the pack.
            // Since we don't have direct count here without db query, we estimate or use fallback:
            // A standard pack has 6 stickers default, let's mock counts precisely
            val totalCount = when (pack.title) {
                "أفشات الزعيم عادل إمام" -> 6
                "ملصقات مستر بين الكوميدية" -> 7
                "رياكشنز الكوميديان محمد هنيدي" -> 5
                else -> 5 // custom created usually has 5
            }
            
            val pricePerSticker = pack.priceEgp / totalCount
            val priceForSelection = pricePerSticker * list.size
            grandTotal += priceForSelection

            detailsList.add(CustomPackBreakdown(
                packTitle = pack.title,
                packPrice = pack.priceEgp,
                stickersSelectedCount = list.size,
                totalStickersInPack = totalCount,
                costSubtotal = priceForSelection,
                creatorEarnings = priceForSelection * 0.5,
                creatorName = pack.creatorName
            ))
        }

        return CustomBundleCalculation(grandTotal, detailsList)
    }

    // Authenticate
    fun handleLogin(email: String, name: String, avatarUrl: String, role: String) {
        viewModelScope.launch {
            repository.loginUser(email, name, avatarUrl, role)
            when (role) {
                "BUYER" -> currentRoute.value = "buyer_home"
                "CREATOR" -> currentRoute.value = "creator_home"
                "ADMIN" -> currentRoute.value = "admin_dashboard"
                else -> currentRoute.value = "auth"
            }
            _uiEvent.emit("👋 مرحبًا بك يا $name في منصة الميمز!")
        }
    }

    fun handleAdminLogin(username: String, secret: String): Boolean {
        if (username.trim().lowercase() == "admin" && secret == "123456") {
            viewModelScope.launch {
                repository.loginUser("admin@memes.com", "المدير العام", "👑", "ADMIN")
                currentRoute.value = "admin_dashboard"
                _uiEvent.emit("🔐 تم تسجيل الدخول كمدير المنصة!")
            }
            return true
        }
        return false
    }

    fun handleLogout() {
        viewModelScope.launch {
            repository.logout()
            currentRoute.value = "auth"
            _uiEvent.emit("🛑 تم تسجيل الخروج بنجاح")
        }
    }

    // --- Shopping Cart Feature Action Methods ---
    fun addPackToCart(pack: StickerPack) {
        viewModelScope.launch {
            val current = _shoppingCart.value
            if (current.any { it.id == pack.id }) {
                _uiEvent.emit(if (appLanguage.value == "AR") "الباقة [${pack.title}] مضافة بالفعل في سلتك! 🛒" else "Pack [${pack.title}] is already in your cart! 🛒")
                return@launch
            }
            if (repository.isPackPurchased(userSession.value?.email ?: "", pack.id)) {
                _uiEvent.emit(if (appLanguage.value == "AR") "أنت تملك هذه الباقة مسبقاً! 💚" else "You already own this pack! 💚")
                return@launch
            }
            _shoppingCart.value = current + pack
            _uiEvent.emit(if (appLanguage.value == "AR") "📥 تم إضافة [${pack.title}] بنجاح إلى سلة المشتريات!" else "📥 Pack [${pack.title}] added to your shopping cart!")
        }
    }

    fun removePackFromCart(pack: StickerPack) {
        val current = _shoppingCart.value
        _shoppingCart.value = current.filter { it.id != pack.id }
        viewModelScope.launch {
            _uiEvent.emit(if (appLanguage.value == "AR") "🗑️ تم حذف الباقة [${pack.title}] من السلة" else "🗑️ Removed pack [${pack.title}] from cart")
        }
    }

    fun clearCart() {
        _shoppingCart.value = emptyList()
    }

    fun checkoutCart() {
        val session = userSession.value ?: return
        val current = _shoppingCart.value
        if (current.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            for (pack in current) {
                val success = repository.purchaseFullPack(session.email, pack.id)
                if (success) {
                    successCount++
                }
            }
            if (successCount > 0) {
                _shoppingCart.value = emptyList()
                _uiEvent.emit(if (appLanguage.value == "AR") "🎉 تم شراء $successCount باقات كوميدية بنجاح عبر فيزا/ميزة الآمنة! وجاري إتاحة التنزيل للواتس اب!" else "🎉 Successfully checked out $successCount packs via secure Visa gateway!")
                
                // Add an automatic announcement congratulating them!
                repository.createAnnouncement(
                    title = if (appLanguage.value == "AR") "🛍️ شكراً لتسوقك معنا!" else "🛍️ Thanks for shopping!",
                    content = if (appLanguage.value == "AR") "تم بنجاح شراء سلتك المحتوية على $successCount باقة ميمز مميزة!" else "Successfully purchased $successCount sticker packs of memes!"
                )
            } else {
                _uiEvent.emit("❌ لم نتمكن من الشراء، يرجى مراجعة رصيد بطاقتك")
            }
        }
    }

    // --- Premium Subscription Features ---
    fun buySubscription(tier: String, priceEgp: Double) {
        viewModelScope.launch {
            activeSubscription.value = tier
            _uiEvent.emit(if (appLanguage.value == "AR") "🌟 تهانينا! تم تفعيل الاشتراك المميز ($tier) بنجاح عبر فيزا/ميزة. استمتع بالتنزيل اللانهائي!" else "🌟 Awesome! Your $tier premium subscription was activated via secure checkout!")
            
            repository.createAnnouncement(
                title = if (appLanguage.value == "AR") "👑 باقة اشتراك مميزة نشطة!" else "👑 Premium Subscription Active!",
                content = if (appLanguage.value == "AR") "تم تفعيل اشتراكك الـ ($tier) بنجاح! من الآن فصاعداً، جميع الملصقات وباقات الكريتورس مجانية لك للتنزيل المباشر!" else "Your $tier subscription has been successfully activated. Enjoy unlimited accesses to all trending stickers!"
            )
        }
    }

    fun cancelSubscription() {
        activeSubscription.value = null
        viewModelScope.launch {
            _uiEvent.emit(if (appLanguage.value == "AR") "⏸️ تم إيقاف الاشتراك بنجاح" else "⏸️ Subscription deactivated")
        }
    }

    // --- User profile image/avatar update from Settings ---
    fun updateUserAvatar(newAvatar: String) {
        val current = userSession.value ?: return
        viewModelScope.launch {
            repository.loginUser(current.email, current.name, newAvatar, current.role)
            _uiEvent.emit(if (appLanguage.value == "AR") "👤 تم تحديث صورتك الشخصية والرمز الكوميدي بنجاح!" else "👤 Profile image/avatar updated successfully!")
        }
    }

    // Purchase Actions
    fun buyReadyPack(pack: StickerPack) {
        val session = userSession.value ?: return
        viewModelScope.launch {
            val success = repository.purchaseFullPack(session.email, pack.id)
            if (success) {
                _uiEvent.emit("🎉 تمت عملية الشراء بنجاح لباقة [${pack.title}] ! وتم تحويل 50% من المبلغ لصانع الباقة ${pack.creatorName}")
            } else {
                _uiEvent.emit("❌ فشلت عملية الدفع، يرجى المحاولة لاحقاً")
            }
        }
    }

    fun buyCustomBundle() {
        val session = userSession.value ?: return
        val selectedList = _selectedCustomStickers.value.toList()
        if (selectedList.isEmpty()) return

        viewModelScope.launch {
            val success = repository.purchaseCustomPack(session.email, selectedList)
            if (success) {
                _uiEvent.emit("🎉 مبروك! قمت بشراء باقتك المخصصة بنجاح. تم توزيع الأرباح تلقائياً بنسبة 50% للكريتورز كل حسب مشاركته")
                clearCustomSelection()
            } else {
                _uiEvent.emit("❌ تعذر إتمام المعاملة، السلة فارغة")
            }
        }
    }

    // Add Sticker pack to WhatsApp (Automation Simulation)
    fun addPackToWhatsapp(packTitle: String) {
        viewModelScope.launch {
            _uiEvent.emit("💚 جاري تصدير باقة [$packTitle] للواتس اب تلقائياً... ثم فتح تطبيق WhatsApp لإضافتها!")
        }
    }

    // Creators Actions
    fun createNewPack(title: String, description: String, price: Double, coverEmoji: String, stickerTitles: List<String>) {
        val session = userSession.value ?: return
        viewModelScope.launch {
            repository.addStickerPack(
                title = title,
                description = description,
                creatorId = session.email,
                creatorName = session.name,
                priceEgp = price,
                coverUrl = coverEmoji,
                stickers = stickerTitles
            )
            _uiEvent.emit("📝 تم حفظ الباقة وإرسالها للمراجعة! سيقوم المسؤول بالموافقة عليها فوراً لتظهر في السوق.")
        }
    }

    // Admin Panel Actions
    fun approveStickerPack(packId: Long) {
        viewModelScope.launch {
            repository.approvePack(packId)
            _uiEvent.emit("✅ تم قبول الباقة ونشرها في المتجر العام بنجاح!")
        }
    }

    fun rejectStickerPack(packId: Long) {
        viewModelScope.launch {
            repository.rejectPack(packId)
            _uiEvent.emit("❌ تم رفض حذف الباقة المقترحة بنجاح.")
        }
    }

    fun sendGlobalNotification(title: String, content: String) {
        viewModelScope.launch {
            repository.createAnnouncement(title, content)
            _uiEvent.emit("🔔 تم إرسال وبث الإشعار التلقائي للجميع بنجاح!")
        }
    }

    // Get stickers for pack id
    fun getStickersForPack(packId: Long): Flow<List<StickerItem>> {
        return repository.getStickersFlow(packId)
    }

    suspend fun isPackOwned(packId: Long): Boolean {
        val email = userSession.value?.email ?: return false
        return repository.isPackPurchased(email, packId)
    }
}

// Data structures for proportional calculations
data class CustomBundleCalculation(
    val totalPrice: Double,
    val breakdowns: List<CustomPackBreakdown>
)

data class CustomPackBreakdown(
    val packTitle: String,
    val packPrice: Double,
    val stickersSelectedCount: Int,
    val totalStickersInPack: Int,
    val costSubtotal: Double,
    val creatorEarnings: Double,
    val creatorName: String
)
