package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.StickerPlatformViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Language Translator Helper mapping for English / Arabic toggles
fun t(ar: String, en: String, lang: String): String {
    return if (lang == "AR") ar else en
}

// Global currency dynamic conversion and formatting helper
fun formatPrice(priceEgp: Double, lang: String, currency: String, rate: Double): String {
    return if (currency == "USD") {
        val usdPrice = priceEgp / rate
        val formattedUsd = String.format("%.2f", usdPrice)
        if (lang == "AR") {
            "$$formattedUsd دولار"
        } else {
            "$$formattedUsd"
        }
    } else {
        val formattedEgp = String.format("%.2f", priceEgp)
        if (lang == "AR") {
            "$formattedEgp ج.م"
        } else {
            "$formattedEgp EGP"
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: StickerRepository
    private lateinit var viewModel: StickerPlatformViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local Room database and repository
        database = AppDatabase.getDatabase(this)
        repository = StickerRepository(database)
        viewModel = StickerPlatformViewModel(repository)

        enableEdgeToEdge()

        setContent {
            val isLight by viewModel.isLightTheme.collectAsStateWithLifecycle()
            MemesTheme(isLight = isLight) {
                val context = LocalContext.current
                
                // Collect and show events like purchased, WhatsApp added, etc.
                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainLayout(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout(viewModel: StickerPlatformViewModel) {
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val route by viewModel.currentRoute.collectAsStateWithLifecycle()
    val announcements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appCurrency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val isLight by viewModel.isLightTheme.collectAsStateWithLifecycle()
    val isAnimated by viewModel.isBgAnimated.collectAsStateWithLifecycle()
    val activeSubscription by viewModel.activeSubscription.collectAsStateWithLifecycle()
    val shoppingCartItems by viewModel.shoppingCart.collectAsStateWithLifecycle()
    
    var showNotifDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCartDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (userSession != null && userSession?.isLoggedIn == true) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Immersive UI Mascot Badge
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(1.5.dp, Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(userSession?.avatarUrl ?: "🤵", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MEMES",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp,
                                    modifier = Modifier.testTag("app_logo_title")
                                )
                                Text(
                                    text = "WHATSAPP STUDIO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Small premium role pill
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (activeSubscription != null) PlayfulYellow else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = (when (userSession?.role) {
                                        "BUYER" -> "مشتري🛒"
                                        "CREATOR" -> "كريتور🎨"
                                        "ADMIN" -> "المدير👑"
                                        else -> ""
                                    }) + if (activeSubscription != null) " • VIP🌟" else "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (activeSubscription != null) Color.Black else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { showNotifDialog = true },
                            modifier = Modifier.testTag("notification_bell_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (announcements.isNotEmpty()) {
                                        Badge { Text(announcements.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "الإشعارات")
                            }
                        }
                    },
                    actions = {
                        // Interactive Language Toggle Switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.toggleLanguage() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "AR") "AR 🇸🇦" else "EN 🇬🇧",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Interactive Currency Toggle Switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.toggleCurrency() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (appCurrency) {
                                    "EGP" -> t("ج.م EGP", "EGP", appLanguage)
                                    "USD" -> "$ USD"
                                    "SAR" -> t("ر.س SAR", "SAR", appLanguage)
                                    "AED" -> t("د.إ AED", "AED", appLanguage)
                                    "KWD" -> t("د.ك KWD", "KWD", appLanguage)
                                    "QAR" -> t("ر.ق QAR", "QAR", appLanguage)
                                    else -> appCurrency
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Fully Functional Shopping Cart action button with badge
                        if (userSession?.role == "BUYER") {
                            IconButton(
                                onClick = { showCartDialog = true },
                                modifier = Modifier.testTag("app_cart_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (shoppingCartItems.isNotEmpty()) {
                                            Badge(
                                                containerColor = PlayfulYellow,
                                                contentColor = Color.Black
                                            ) {
                                                Text(shoppingCartItems.size.toString(), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = ComicBlue)
                                }
                            }
                        }

                        // Fully Functional Settings / Controls Dialog launch button
                        IconButton(
                            onClick = { 
                                showSettingsDialog = true
                            },
                            modifier = Modifier.testTag("app_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Simulated user ID avatar button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { 
                                    viewModel.sendGlobalNotification(
                                        title = "معرف الجلسة المميز 🆔",
                                        content = "بريدك الإلكتروني النشط حالياً: ${userSession?.email ?: "m.fat7iii@gmail.com"}"
                                    )
                                    showNotifDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userSession?.avatarUrl ?: "🎭", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { viewModel.handleLogout() },
                            modifier = Modifier.testTag("logout_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل الخروج", tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Live animated light/cheerful or immersive dark scenic background!
            AnimatedScenicBackground(isAnimated = isAnimated, isLight = isLight)

            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "ScreenTransition"
            ) { targetRoute ->
                when (targetRoute) {
                    "auth" -> AuthScreen(viewModel)
                    "buyer_home" -> BuyerHomeScreen(viewModel)
                    "creator_home" -> CreatorHomeScreen(viewModel)
                    "admin_dashboard" -> AdminDashboardScreen(viewModel)
                    else -> AuthScreen(viewModel)
                }
            }
        }
    }

    // Shopping Cart dialog interface
    if (showCartDialog) {
        val usdToEgpRate by viewModel.usdToEgpRate.collectAsStateWithLifecycle()
        val appCurrency by viewModel.appCurrency.collectAsStateWithLifecycle()
        val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
        
        AlertDialog(
            onDismissRequest = { showCartDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ComicBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t("سلة المشتريات والملصقات المميزة 🛒", "Your Premium Sticker Cart 🛒", appLanguage),
                        fontWeight = FontWeight.Bold,
                        textAlign = if (appLanguage == "AR") TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (shoppingCartItems.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🛒", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = t("سلة المشتريات فارغة حالياً!", "Your cart is currently empty!", appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = t("تصفح ملصقات المتجر وأضف ما يحلو لك بلمسة واحدة لشرائها كباقة متكاملة.", "Explore the shop and add premium sticker packs to checkout securely.", appLanguage),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Header info
                        item {
                            Text(
                                text = t("أنت على وشك شراء الباقات التالية دفعة واحدة عبر الدفع الآمن بالفيزا/ميزة:", "Checkout the following packs together using secure credit card:", appLanguage),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )
                        }

                        items(shoppingCartItems) { pack ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ComicBlue.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(pack.coverUrl, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pack.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = "${formatPrice(pack.priceEgp, appLanguage, appCurrency, usdToEgpRate)} • ${pack.creatorName}",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removePackFromCart(pack) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                                    }
                                }
                            }
                        }

                        // Calculate grand total EGP first, then convert it using formatPrice
                        val totalEgp = shoppingCartItems.sumOf { it.priceEgp }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PlayfulYellow.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().border(1.2.dp, PlayfulYellow, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t("المجموع الكلي للحساب:", "Grand Total Amount:", appLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = formatPrice(totalEgp, appLanguage, appCurrency, usdToEgpRate),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = ComicBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Visa Payment Mock input form
                                    Text(
                                        text = t("💳 تفاصيل بطاقة الفيزا / ميزة للدفع الفوري والتفعيل الآمن:", "💳 Enter Card details for secure instant activation:", appLanguage),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Mock Inputs
                                    var cardNumber by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = cardNumber,
                                        onValueChange = { if (it.length <= 16) cardNumber = it },
                                        placeholder = { Text("4000 1234 5678 9010", fontSize = 10.sp) },
                                        label = { Text(t("رقم فيزا كارت", "Visa Card Number", appLanguage), fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        var expiry by remember { mutableStateOf("") }
                                        var cvv by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = expiry,
                                            onValueChange = { if (it.length <= 5) expiry = it },
                                            placeholder = { Text("MM/YY", fontSize = 10.sp) },
                                            label = { Text(t("الانتهاء", "Expiry", appLanguage), fontSize = 9.sp) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = cvv,
                                            onValueChange = { if (it.length <= 3) cvv = it },
                                            placeholder = { Text("123", fontSize = 10.sp) },
                                            label = { Text("CVV", fontSize = 9.sp) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Button(
                                        onClick = {
                                            viewModel.checkoutCart()
                                            showCartDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = t("تأكيد الدفع بالفيزا 💳", "Confirm Secure Checkout 💳", appLanguage),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCartDialog = false }
                ) {
                    Text(t("الرجوع للمتجر", "Back to Store", appLanguage))
                }
            }
        )
    }

    // Unread push notifications simulation dialog
    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = PlayfulYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مركز الإشعارات التلقائية 🔔",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (announcements.isEmpty()) {
                        item {
                            Text(
                                "لا توجد إشعارات جديدة حالياً.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        items(announcements) { notice ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = notice.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = notice.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "مرسل من: ${notice.sentBy}", fontSize = 10.sp, color = Color.Gray)
                                        Text(text = "نشط الآن", fontSize = 10.sp, color = WhatsappGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Fully Functional Settings / Controls Dialog for users or administrators
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = ComicBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t("إعدادات المتجر والتحكم ⚙️", "Store Settings & Controls ⚙️", appLanguage),
                        fontWeight = FontWeight.Bold,
                        textAlign = if (appLanguage == "AR") TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 0. Profile Photo/Avatar Customize Section
                    item {
                        Text(
                            text = t("تخصيص صورتك الشخصية والرمز الكوميدي 🎭:", "Your Profile Mascot & Avatar 🎭:", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current selected avatar display
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(ComicBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(userSession?.avatarUrl ?: "🎭", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("اختر رمزك الكوميدي 🎭🎨👑", "Choose comedy mascot 🎭🎨👑", appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("اضغط على أي استيكر أدناه للتعديل الفوري لمظهرك", "Tap any sticker below to update your profile", appLanguage),
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Selection of funny avatars
                        val comedyAvatars = listOf("🎭", "🤡", "🤪", "🐸", "🥸", "😎", "👾", "🤖", "🦊", "👑", "🍿", "🤵", "🦁")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            comedyAvatars.take(7).forEach { emoji ->
                                val isSelected = userSession?.avatarUrl == emoji
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) PlayfulYellow else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.updateUserAvatar(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            comedyAvatars.takeLast(6).forEach { emoji ->
                                val isSelected = userSession?.avatarUrl == emoji
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) PlayfulYellow else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.updateUserAvatar(emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }

                    // 1. Language Row
                    item {
                        Text(t("لغة التطبيق:", "App Language:", appLanguage), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ElevatedFilterChip(
                                selected = appLanguage == "AR",
                                onClick = { viewModel.toggleLanguage() },
                                label = { Text("العربية 🇸🇦") }
                            )
                            ElevatedFilterChip(
                                selected = appLanguage == "EN",
                                onClick = { viewModel.toggleLanguage() },
                                label = { Text("English 🇬🇧") }
                            )
                        }
                    }

                    // 2. Bright Light Theme Toggle
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("المظهر الفاتح المبهج ☀️", "Cheerful Light Theme ☀️", appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("ألوان دافئة وفاتحة ذو طابع فكاهي مبهج", "Bright, warm, and playful colors", appLanguage),
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isLight,
                                onCheckedChange = { viewModel.toggleTheme() }
                            )
                        }
                    }

                    // 3. Dynamic Background Animation
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("حركة الخلفية التفاعلية 🎬", "Animated Background 🎬", appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("عناصر متحركة تطفو بخلفية الشاشة لتجربة تفاعلية", "Floating pastel circles and funny stickers", appLanguage),
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isAnimated,
                                onCheckedChange = { viewModel.toggleBgAnimation() }
                            )
                        }
                    }

                    // 4. Gulf and Arab Currencies Selection
                    item {
                        Text(
                            text = t("عملة المتجر والدفع 💱:", "Store Currency & Payment 💱:", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Display currency selection chips
                        val currencies = listOf(
                            "EGP" to "ج.م 🇪🇬",
                            "USD" to "$ 🇺🇸",
                            "SAR" to "ر.س 🇸🇦",
                            "AED" to "د.إ 🇦🇪",
                            "KWD" to "د.ك 🇰🇼",
                            "QAR" to "ر.ق 🇶🇦"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            currencies.take(3).forEach { (code, label) ->
                                val isSelected = appCurrency == code
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ComicBlue else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setCurrency(code) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                        Text(code, fontSize = 8.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            currencies.takeLast(3).forEach { (code, label) ->
                                val isSelected = appCurrency == code
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ComicBlue else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setCurrency(code) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                        Text(code, fontSize = 8.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Visa and Global Payments Status Gateway
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WhatsappGreen.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WhatsappGreen, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💳", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = t("بوابة الدفع الإلكتروني (فيزا / ميزة) نشطة ✅", "Visa & Card Payment Gateway Active ✅", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsappGreen
                                    )
                                    Text(
                                        text = t("تم تفعيل بروتوكول الدفع بالفيزا والبطاقات الخليجية بنجاح بنظام التجربة الآمنة.", "Visa cards are fully enabled for Gulf/Arab secure mock checkout.", appLanguage),
                                        fontSize = 8.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // 6. Quick Administrator Role bypass shortcut / credentials instructions
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PlayfulYellow.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PlayfulYellow, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = t("👤 لوحة الإدارة السريعة وحساب المدير", "👑 Quick Administrator Dashboard Access", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlayfulYellow
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = t("بيانات التجربة للمدير الفوري: username هو admin مع كلمة مرور 123456.", "Default admin username: admin, password: 123456", appLanguage),
                                    fontSize = 9.sp,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (userSession != null) {
                                    Button(
                                        onClick = {
                                            viewModel.handleAdminLogin("admin", "123456")
                                            showSettingsDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PlayfulYellow),
                                        modifier = Modifier.fillMaxWidth().testTag("direct_admin_bypass_settings_btn"),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            t("كبسة تجاوز فوري لحساب الأدمن 👑", "Bypass directly to Admin Mode 👑", appLanguage),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false }
                ) {
                    Text(t("حفظ وإغلاق 💾", "Save & Close 💾", appLanguage))
                }
            }
        )
    }
}

@Composable
fun AnimatedScenicBackground(isAnimated: Boolean, isLight: Boolean) {
    if (isLight) {
        val baseColor = Color(0xFFFFFCEE) // Soft light cheerful cream butter base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseColor)
        ) {
            if (isAnimated) {
                val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
                val offsetY1 by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 60f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubble1"
                )
                val offsetY2 by infiniteTransition.animateFloat(
                    initialValue = 40f,
                    targetValue = -30f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(5000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubble2"
                )
                val scale1 by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale1"
                )

                // Beautiful pastel dynamic circles decoration
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFFFCC80).copy(alpha = 0.45f), // Playful soft orange/yellow
                        radius = 220f * scale1,
                        center = androidx.compose.ui.geometry.Offset(
                            x = size.width * 0.15f,
                            y = size.height * 0.28f + offsetY1
                        )
                    )
                    drawCircle(
                        color = Color(0xFF90CAF9).copy(alpha = 0.35f), // Soft bubbly blue
                        radius = 290f,
                        center = androidx.compose.ui.geometry.Offset(
                            x = size.width * 0.85f,
                            y = size.height * 0.72f + offsetY2
                        )
                    )
                    drawCircle(
                        color = Color(0xFFA5D6A7).copy(alpha = 0.35f), // Soft cheerful WhatsApp-styled green
                        radius = 160f * scale1,
                        center = androidx.compose.ui.geometry.Offset(
                            x = size.width * 0.5f,
                            y = size.height * 0.14f - offsetY2
                        )
                    )
                }

                // Beautiful floating emojis for comical sense of humor matching the stickers theme
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "😂", fontSize = 28.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 45.dp, top = 140.dp)
                            .graphicsLayer {
                                translationY = offsetY1
                                alpha = 0.35f
                            }
                    )
                    Text(
                        "🎭", fontSize = 32.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 40.dp, bottom = 190.dp)
                            .graphicsLayer {
                                translationY = offsetY2
                                alpha = 0.3f
                            }
                    )
                    Text(
                        "🤪", fontSize = 26.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                translationX = offsetY1 * 0.5f
                                translationY = offsetY2
                                alpha = 0.3f
                            }
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFFFCC80).copy(alpha = 0.35f),
                        radius = 230f,
                        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.2f, y = size.height * 0.25f)
                    )
                    drawCircle(
                        color = Color(0xFF90CAF9).copy(alpha = 0.25f),
                        radius = 280f,
                        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.8f, y = size.height * 0.75f)
                    )
                }
            }
        }
    } else {
        // Immersive theme background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF221345))
        ) {
            if (isAnimated) {
                val infiniteTransition = rememberInfiniteTransition(label = "dark_bg_anim")
                val offsetY1 by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 50f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubble1"
                )
                val offsetY2 by infiniteTransition.animateFloat(
                    initialValue = 30f,
                    targetValue = -30f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(5500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubble2"
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF412C78).copy(alpha = 0.3f),
                        radius = 300f,
                        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.15f, y = size.height * 0.30f + offsetY1)
                    )
                    drawCircle(
                        color = Color(0xFF5B419C).copy(alpha = 0.25f),
                        radius = 250f,
                        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.85f, y = size.height * 0.70f + offsetY2)
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF412C78).copy(alpha = 0.25f),
                        radius = 280f,
                        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.2f, y = size.height * 0.3f)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// 1. AUTHENTICATION SCREEN (GOOGLE, FACEBOOK, AND ADMIN FLOWS)
// ------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: StickerPlatformViewModel) {
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("BUYER") } // BUYER or CREATOR
    var isGoogleFlow by remember { mutableStateOf(false) }
    var isFbFlow by remember { mutableStateOf(false) }

    // Admin login inputs
    var showAdminLogin by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminLoginError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Header
        item {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFA000), // Original golden light
                                Color(0xFFEA580C)  // Vibrant deep gradient
                            )
                        )
                    )
                    .border(2.5.dp, Color.White, RoundedCornerShape(32.dp))
                    .shadow(12.dp, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Large elegant emoji representing the Immersive mascot
                Text("🤵", fontSize = 72.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MEMES Studio",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "المنصة العربية الأولى لأقوى ملصقات وريأكشنات الواتس اب بالتصدير التلقائي ونظام الربح ٥٠٪!",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Choice selection cards
        if (!isGoogleFlow && !isFbFlow && !showAdminLogin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تسجيل دخول سريع وآمن 🔐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Google Sign In Simulated button
                        Button(
                            onClick = {
                                isGoogleFlow = true
                                nameInput = "أحمد المصري"
                                emailInput = "ahmed.egy@gmail.com"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_login_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4437)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("الدخول بحساب جوجل G", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Facebook Sign In Simulated button
                        Button(
                            onClick = {
                                isFbFlow = true
                                nameInput = "مي منصور"
                                emailInput = "mai.mansour@facebook.com"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("facebook_login_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("الدخول بحساب فيسبوك F", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Switch to Admin triggers
                        Text(
                            text = "دخول لوحة الإدارة 👑",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { showAdminLogin = true }
                                .padding(8.dp)
                                .testTag("admin_flow_trigger")
                        )
                    }
                }
            }
        } else if (showAdminLogin) {
            // Admin Panel credentials checker
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لوحة دخول الإدارة العامة 👑",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = ComicYellowDark
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = adminUsername,
                            onValueChange = { adminUsername = it },
                            label = { Text("اسم المستخدم للادمن") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_username_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            label = { Text("كلمة المرور الباسورد") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input"),
                            singleLine = true
                        )

                        if (adminLoginError) {
                            Text(
                                "البيانات خاطئة! تأكد من كتابة admin و 123456",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val success = viewModel.handleAdminLogin(adminUsername, adminPassword)
                                if (!success) {
                                    adminLoginError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ComicYellowDark)
                        ) {
                            Text("تسجيل دخول المشرف", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "الرجوع للخلف",
                            modifier = Modifier
                                .clickable { showAdminLogin = false }
                                .padding(8.dp)
                        )
                    }
                }
            }
        } else {
            // Prompt user role setup to explore app modes
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                isGoogleFlow = false
                                isFbFlow = false
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGoogleFlow) "تسجيل الدخول مع Google" else "تسجيل الدخول مع Facebook",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isGoogleFlow) Color(0xFFDB4437) else Color(0xFF1877F2)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("الاسم الكريم") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("البريد الإلكتروني") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "اختر دورك في المنصة للبدء الأساسي:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Buyer button selection
                            val isBuyer = selectedRole == "BUYER"
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBuyer) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isBuyer) 2.dp else 1.dp,
                                        color = if (isBuyer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedRole = "BUYER" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🛒", fontSize = 28.sp)
                                    Text("مشتري ومستكشف", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("أشتري باقات أو أركّب باقتي", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                                }
                            }

                            // Creator button selection
                            val isCreator = selectedRole == "CREATOR"
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCreator) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isCreator) 2.dp else 1.dp,
                                        color = if (isCreator) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedRole = "CREATOR" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🎨", fontSize = 28.sp)
                                    Text("كريتور ومصمم", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("أرفع باقات وأربح 50% دائماً", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (nameInput.isNotBlank() && emailInput.isNotBlank()) {
                                    val avatar = if (selectedRole == "BUYER") "🎭" else "🎨"
                                    viewModel.handleLogin(emailInput, nameInput, avatar, selectedRole)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("simulated_auth_confirm"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = nameInput.isNotBlank() && emailInput.isNotBlank()
                        ) {
                            Text("تأكيد الدخول الآمن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// 2. BUYER EXPLORE & CUSTOM WORKSHOP HOME SCREEN
// ------------------------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuyerHomeScreen(viewModel: StickerPlatformViewModel) {
    val approvedPacks by viewModel.approvedStickerPacks.collectAsStateWithLifecycle()
    val allStickersInPacks = remember(approvedPacks) { mutableStateMapOf<Long, List<StickerItem>>() }
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appCurrency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val usdToEgpRate by viewModel.usdToEgpRate.collectAsStateWithLifecycle()

    val showcaseTitle by viewModel.showcaseTitle.collectAsStateWithLifecycle()
    val showcaseSub by viewModel.showcaseSub.collectAsStateWithLifecycle()
    val showcasePriceEgp by viewModel.showcasePriceEgp.collectAsStateWithLifecycle()
    val showcaseCoverEmoji by viewModel.showcaseCoverEmoji.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Explore Packs, 1 = Make Custom Bundle, 2 = My Library

    // Active sticker inspection state
    var selectedPackForDetails by remember { mutableStateOf<StickerPack?>(null) }
    var showCheckoutDialog by remember { mutableStateOf<StickerPack?>(null) }
    var showCustomCheckoutConfirm by remember { mutableStateOf(false) }

    // Filter packs
    val filteredPacks = approvedPacks.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    // Launch coroutine to load stickers for each pack to display overview
    LaunchedEffect(approvedPacks) {
        approvedPacks.forEach { pack ->
            launch {
                viewModel.getStickersForPack(pack.id).collectLatest { items ->
                    allStickersInPacks[pack.id] = items
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Featured Trending Pack Card: "Bean Moments"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                .clickable {
                    searchQuery = t("مستر بين", "mr bean", appLanguage)
                    viewModel.sendGlobalNotification(showcaseTitle, "تمت التصفية السريعة لمحتويات مجلة العرض!")
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFBBF24), // Amber
                                Color(0xFFEA580C)  // Orange
                            )
                        )
                    )
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                t("الباقة الأكثر تداولاً 🔥 ميزة اليوم", "🔥 TODAY'S FEATURED SHOWCASE", appLanguage),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(showcaseCoverEmoji, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = showcaseTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = showcaseSub,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatPrice(showcasePriceEgp, appLanguage, appCurrency, usdToEgpRate),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t("تسوّق الآن 🛒", "SHOP NOW 🛒", appLanguage),
                                color = Color(0xFFEA580C),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Creator Profit Promo: "Earn 50% Profit per Sale" matching the Indigo design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable {
                    // Clicking automatically navigates creators or buyer to the creation zone / alert switch role
                    viewModel.sendGlobalNotification(
                        "برنامج الكريتورز المعتمدين 🎨",
                        "يمكنك في أي وقت تبديل الحساب لكريتور لإطلاق باقاتك والحصول على حصة ٥٠٪ فورياً لكل عملية تحميل!"
                    )
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF6366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Payments",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مركز صناع الإبداع CREATOR HUB",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC7D2FE) // Indigo 200
                        )
                        Text(
                            text = "ارفع باقاتك الخاصة واربح ٥٠٪ عمولة مبيعات تلقائية!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF6366F1))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "اشتراك JOIN",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Search Bar (Always visible on top of panels)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("ابحث عن باقات كـ عادل إمام، مستر بين ... 🔍") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_sticker_input"),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation tab bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PlayfulYellow,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(t("العام 🎪", "Store 🎪", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(t("الاشتراكات 🌟", "VIP 🌟", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(t("صناعة باقة 🛠️", "Custom 🛠️", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(t("مشترياتي 🗂️", "Library 🗂️", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content switcher
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Explore Ready packs
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (filteredPacks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🕵️‍♂️", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا تتوفر باقات استيكرات مطابقة للحبث حالياً.", color = Color.Gray)
                                    }
                                }
                            }
                        }

                        items(filteredPacks) { pack ->
                            var alreadyOwned by remember { mutableStateOf(false) }
                            LaunchedEffect(pack.id, userSession) {
                                alreadyOwned = viewModel.isPackOwned(pack.id)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                    .clickable { selectedPackForDetails = pack }
                                    .testTag("sticker_pack_card_${pack.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(pack.coverUrl, fontSize = 32.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pack.title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                            Text("صنع بواسطة: ${pack.creatorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${pack.priceEgp} ج.م",
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 16.sp
                                            )
                                            Text("باقة كاملة", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(pack.description, fontSize = 12.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    // Display preview of some stickers in pack
                                    val stickers = allStickersInPacks[pack.id] ?: emptyList()
                                    if (stickers.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            stickers.take(4).forEach { sticker ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(45.dp)
                                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.background)
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = sticker.title.take(2), // Emoji or start text
                                                        fontSize = 18.sp,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            if (stickers.size > 4) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(45.dp)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("+${stickers.size - 4}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Buy or Add to WhatsApp action button
                                    if (alreadyOwned) {
                                        Button(
                                            onClick = { viewModel.addPackToWhatsapp(pack.title) },
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("add_wa_owned_${pack.id}"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تنزيل وإضافة حيوية للواتس اب 💚", color = Color.White)
                                        }
                                    } else {
                                        Button(
                                            onClick = { showCheckoutDialog = pack },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("buy_pack_btn_${pack.id}"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("شراء الباقة كاملة 🛒")
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }

                1 -> {
                    // Premium Subscriptions and Subscription selection panel
                    val activeSub by viewModel.activeSubscription.collectAsStateWithLifecycle()
                    var selectedSubTierForPayment by remember { mutableStateOf<Pair<String, Double>?>(null) }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // VIP status banner
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeSub != null) PlayfulYellow.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().border(
                                width = if (activeSub != null) 2.dp else 1.dp,
                                color = if (activeSub != null) PlayfulYellow else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (activeSub != null) "👑 أنت عضو ذهبي نشط!" else "🌟 اشترك في باقة الميمز الذهبية", fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (activeSub != null) PlayfulYellow else Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (activeSub != null) 
                                        "نوع عضويتك النشطة حالياً: [$activeSub VIP] • صلاحيتك غير محدودة!" 
                                        else "احصل على وصول مجاني لانهائي لكافة ملصقات المتجر وباقات الترند اليومية بدون حدود!",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                if (activeSub != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.cancelSubscription() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.height(36.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("إلغاء الاشتراك النشط ⏸️", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Display plans
                        val subscriptionPlans = listOf(
                            Triple("الباقة الذهبية السنوية", 199.0, t("التوفير الأقصى • أفضل خيار مالي 👑", "Ultimate Value • Best Choice 👑", appLanguage)),
                            Triple("الباقة الربع سنوية المميزة", 69.0, t("الخيار الأكثر شعبية ⚡", "Most Popular Option ⚡", appLanguage)),
                            Triple("الباقة الشهرية المرنة", 29.0, t("مرونة تامة للدردشة 🍃", "Full Flexibility to chat 🍃", appLanguage))
                        )

                        subscriptionPlans.forEach { (tierName, basePriceEgp, badge) ->
                            val isCurrentTier = activeSub == tierName
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isCurrentTier) 2.5.dp else 1.dp,
                                        color = if (isCurrentTier) PlayfulYellow else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            if (badge.isNotEmpty()) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = ComicBlue),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                ) {
                                                    Text(badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White)
                                                }
                                            }
                                            Text(tierName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = formatPrice(basePriceEgp, appLanguage, appCurrency, usdToEgpRate),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = PlayfulYellow
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = when (tierName) {
                                            "الباقة الذهبية السنوية" -> "• توفير أكثر من 45% سنوياً\n• تنزيل وتصدير غير محدود لكافة ملصقات الكريتورس والمشاهير\n• شارة الملف الشخصي وعلامات الاسم الذهبية اللامعة مجاناً\n• دعم مخصص لمزج الاستيكرات بالذكاء الاصطناعي"
                                            "الباقة الربع سنوية المميزة" -> "• توفير 20%، وتجديد فوري كل 3 أشهر\n• وصول مجاني وبدون إعلانات لكافة باقات الترند وقسم صناعة ملصق خاص\n• تنزيل فخم وتكامل مباشر مع واتساب بلمسة واحدة"
                                            else -> "• فاتورة مرنة كل شهر للاكتشاف والتجربة\n• وصول مجاني مؤقت للأقسام الحصرية والمحاكاة الذكية"
                                        },
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Button(
                                        onClick = { selectedSubTierForPayment = tierName to basePriceEgp },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrentTier) WhatsappGreen else ComicBlue
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isCurrentTier
                                    ) {
                                        Icon(if (isCurrentTier) Icons.Default.Check else Icons.Default.Star, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isCurrentTier) "عضويتك النشطة حالياً ✅" else "اشترك واستمتع بالملصقات 💳"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Secure Visa payment gateway simulator for subscriptions
                    if (selectedSubTierForPayment != null) {
                        val (tier, priceInEgp) = selectedSubTierForPayment!!
                        AlertDialog(
                            onDismissRequest = { selectedSubTierForPayment = null },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = PlayfulYellow)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("دفع آمن بالفيزا لشراء الاشتراك 🔒")
                                }
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("أنت على وشك تفعيل العضوية والاشتراك بـ [$tier] والتمتع بمميزات VIP.", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("المبلغ المطلوب للدفع:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            formatPrice(priceInEgp, appLanguage, appCurrency, usdToEgpRate),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = PlayfulYellow
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("💳 أدخل بيانات الدفع لتفعيل اشتراكك الفوري الآمن:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    var cardNum by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = cardNum,
                                        onValueChange = { if (it.length <= 16) cardNum = it },
                                        placeholder = { Text("4000 1234 5678 9010", fontSize = 10.sp) },
                                        label = { Text("رقم بطاقة الفيزا", fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        var subExpiry by remember { mutableStateOf("") }
                                        var subCvv by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = subExpiry,
                                            onValueChange = { if (it.length <= 5) subExpiry = it },
                                            placeholder = { Text("MM/YY", fontSize = 10.sp) },
                                            label = { Text("الانتهاء", fontSize = 9.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = subCvv,
                                            onValueChange = { if (it.length <= 3) subCvv = it },
                                            placeholder = { Text("123", fontSize = 10.sp) },
                                            label = { Text("CVV", fontSize = 9.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.buySubscription(tier, priceInEgp)
                                        selectedSubTierForPayment = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
                                ) {
                                    Text("تأكيد دفع الفيزا الآمن 💳")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { selectedSubTierForPayment = null }) {
                                    Text("إلغاء وإغلاق الدفع")
                                }
                            }
                        )
                    }
                }

                2 -> {
                    // Custom bundle workshop builder! Select individual sticker combinations
                    val selectedCustomStickers by viewModel.selectedCustomStickers.collectAsStateWithLifecycle()
                    val customBreakdown = viewModel.calculateCustomBundleDetails(approvedPacks)

                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("صندوق تجميع استيكراتك المخصصة 🛠️", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text("اختر ملصقات مفردة من أي باقة في السوق، وسيقوم النظام بتجميعها وحساب سعرها العادل مع إرسال 50% لكل كريتور الكترونياً!", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "عدد الملصقات المحددة حالياً: ${selectedCustomStickers.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid of all stickers across approved packs
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            approvedPacks.forEach { pack ->
                                val stickers = allStickersInPacks[pack.id] ?: emptyList()
                                items(stickers) { sticker ->
                                    val isSelected = selectedCustomStickers.any { it.id == sticker.id }
                                    Box(
                                        modifier = Modifier
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.toggleStickerInCustomSelection(sticker) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(sticker.title, fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 2, color = Color.White)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                pack.title.take(10) + "..",
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Purchase customized bundle section
                        if (selectedCustomStickers.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("السعر الإجمالي للباقة الخاصة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        Text(
                                            "${String.format("%.2f", customBreakdown.totalPrice)} ج.م",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { showCustomCheckoutConfirm = true },
                                        modifier = Modifier.fillMaxWidth().testTag("buy_custom_bundle_btn"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("شراء الباقة المجمعة مخصصة 🛒")
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // My Library / Purchases
                    val allPurchases by viewModel.allPurchases.collectAsStateWithLifecycle()
                    val myPurchases = allPurchases.filter { it.buyerEmail == userSession?.email }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (myPurchases.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🛒", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لم تقم بشراء أي ملصقات أو باقات بعد.", color = Color.Gray)
                                    }
                                }
                            }
                        }

                        items(myPurchases) { transaction ->
                            val isCustom = transaction.isCustomBundle
                            val matchedPack = approvedPacks.find { it.id == transaction.packId }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Black),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isCustom) "باقة مخصصة من تجميعك 🛠️" else (matchedPack?.title ?: "باقة جاهزة"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "ممتلكة ✅",
                                            color = WhatsappGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("قيمة عملية الشراء: ${transaction.amountPaidEgp} ج.م", fontSize = 11.sp, color = Color.Gray)

                                    if (isCustom) {
                                        Text("تحتوي على ملصقات مركبة عبر باقات السوق.", fontSize = 11.sp, color = Color.DarkGray)
                                    } else {
                                        Text(matchedPack?.description ?: "", fontSize = 11.sp, color = Color.DarkGray)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { viewModel.addPackToWhatsapp(if (isCustom) "باقة مخصصة" else (matchedPack?.title ?: "الباقة")) },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تصدير تلقائي فوري لواتساب 💚", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // INSPECTION SHEET / DETAIL VIEW DIALOG
    if (selectedPackForDetails != null) {
        val pack = selectedPackForDetails!!
        val stickers by viewModel.getStickersForPack(pack.id).collectAsStateWithLifecycle(emptyList())
        var owned by remember { mutableStateOf(false) }

        LaunchedEffect(pack) {
            owned = viewModel.isPackOwned(pack.id)
        }

        AlertDialog(
            onDismissRequest = { selectedPackForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pack.coverUrl, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(pack.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(pack.description, fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الصانع: ${pack.creatorName}", fontSize = 10.sp, color = Color.Gray)
                    Text("سعر الباقة: ${pack.priceEgp} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlayfulYellow)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("محتويات الباقة (${stickers.size} ملصق):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stickers) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (owned) {
                        Button(
                            onClick = {
                                viewModel.addPackToWhatsapp(pack.title)
                                selectedPackForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تنسيب فوري لواتس اب")
                        }
                    } else {
                        Button(
                            onClick = {
                                showCheckoutDialog = pack
                                selectedPackForDetails = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("شراء الباقة")
                        }
                    }
                    TextButton(onClick = { selectedPackForDetails = null }, modifier = Modifier.weight(0.5f)) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }

    // READY PACKS BILLING PIPELINE
    if (showCheckoutDialog != null) {
        ReadyPackCheckoutDialog(
            pack = showCheckoutDialog!!,
            onDismiss = { showCheckoutDialog = null },
            viewModel = viewModel,
            appLanguage = appLanguage,
            appCurrency = appCurrency,
            usdRate = usdToEgpRate
        )
    }

    // CUSTOM BUNDLE BILLING PIPELINE
    if (showCustomCheckoutConfirm) {
        CustomBundleCheckoutDialog(
            onDismiss = { showCustomCheckoutConfirm = false },
            viewModel = viewModel,
            appLanguage = appLanguage,
            appCurrency = appCurrency,
            usdRate = usdToEgpRate,
            approvedPacks = approvedPacks
        )
    }
}

@Composable
fun ReadyPackCheckoutDialog(
    pack: StickerPack,
    onDismiss: () -> Unit,
    viewModel: StickerPlatformViewModel,
    appLanguage: String,
    appCurrency: String,
    usdRate: Double
) {
    var step by remember { mutableStateOf(1) } // 1: Info & Choose, 2: Insert Billing, 3: Receipt Success
    var paymentMethod by remember { mutableStateOf("card") } // card, vodafone, fawry
    
    // Billing inputs
    var cardNumber by remember { mutableStateOf("") }
    var cardName by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    
    var vodafonePhone by remember { mutableStateOf("") }
    var vodafoneOtp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    
    var fawryCode by remember { mutableStateOf((1000000000L..9999999999L).random().toString()) }
    
    var isProcessing by remember { mutableStateOf(false) }
    
    val totalAmount = pack.priceEgp
    val creatorShare = totalAmount * 0.5
    
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            kotlinx.coroutines.delay(2000)
            isProcessing = false
            step = 3
            viewModel.buyReadyPack(pack)
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (step != 3) onDismiss() },
        title = {
            Text(
                text = if (step == 1) t("تأكيد الشراء الفوري وتوزيع الأرباح 🛒", "Confirm Instant Purchase & Splits 🛒", appLanguage)
                       else if (step == 2) t("بوابة الدفع الآمنة والتحقق 🔒", "Secure Billing & Authentication 🔒", appLanguage)
                       else t("فاتورة الدفع المعتمدة والتحميل ✅", "Certified Payment Receipt ✅", appLanguage),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (step == 1) {
                    Text(
                        text = t("لقد اخترت الباقة المميزة التالية للبيع والتصدير مباشرة:", "You selected the following premium pack for export:", appLanguage),
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pack.coverUrl, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(pack.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = t("صانع الملصق: ", "Creator: ", appLanguage) + pack.creatorName,
                                        fontSize = 10.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(t("القيمة الإجمالية:", "Total cost:", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatPrice(totalAmount, appLanguage, appCurrency, usdRate),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlayfulYellow
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WhatsappGreen.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = t("⚡ حقن حصص الأرباح المباشرة (الكريتور يحصل على 50%):", "⚡ Live profit splitting calculation (50% Creator split):", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsappGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(t("• حصة المبدع (${pack.creatorName}):", "• Creator (${pack.creatorName}) cut:", appLanguage), fontSize = 10.sp)
                                Text(formatPrice(creatorShare, appLanguage, appCurrency, usdRate), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhatsappGreen)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(t("• حصة الخادم والمنصة للتشغيل التلقائي:", "• Platform maintenance fee:", appLanguage), fontSize = 10.sp)
                                Text(formatPrice(creatorShare, appLanguage, appCurrency, usdRate), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhatsappGreen)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(t("اختر وسيلة الدفع المدعومة بمصر بمعدل فوري:", "Select a payment gateway network:", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (paymentMethod == "card") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = "card" }
                                .border(1.dp, if (paymentMethod == "card") PlayfulYellow else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💳 Card", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(t("فيزا / ميزة", "Visa/Meza", appLanguage), fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (paymentMethod == "vodafone") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = "vodafone" }
                                .border(1.dp, if (paymentMethod == "vodafone") PlayfulYellow else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔴 Cash", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(t("فودافون كاش", "Vodafone", appLanguage), fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (paymentMethod == "fawry") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = "fawry" }
                                .border(1.dp, if (paymentMethod == "fawry") PlayfulYellow else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡ Fawry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(t("فوري كود", "Fawry Pay", appLanguage), fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                } else if (step == 2) {
                    if (isProcessing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PlayfulYellow)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (paymentMethod == "card") t("جاري التحقق من كارت السداد ومعالجة حصة المبدع الآمنة... 🔐", "Validating card details and securing creator share... 🔐", appLanguage)
                                       else if (paymentMethod == "vodafone") t("جاري سحب الأموال لمشغل المحفظة فودافون كاش... 💸", "Withdrawing from Vodafone wallet securely... 💸", appLanguage)
                                       else t("جاري معالجة الكود وتسجيل الدفع النقدي الفوري... ⚡", "Completing cash payment registering code... ⚡", appLanguage),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        if (paymentMethod == "card") {
                            Text(t("الرجاء إدخال بيانات البطاقة البنكية لمتابعة السداد الآمن:", "Enter bank card elements to securely pay:", appLanguage), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { if (it.length <= 16) cardNumber = it },
                                label = { Text(t("رقم البطاقة (16 رقم)", "Card Number (16-digits)", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                label = { Text(t("اسم كامل", "Cardholder Name", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text(t(" MM/YY", "Expiry MM/YY", appLanguage)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { if (it.length <= 3) cardCvv = it },
                                    label = { Text("CVV") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        } else if (paymentMethod == "vodafone") {
                            Text(t("أدخل هاتف فودافون كاش السليم المحتوي للمحفظة للخصم:", "Enter active wallet phone number:", appLanguage), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = vodafonePhone,
                                onValueChange = { vodafonePhone = it },
                                label = { Text(t("رقم المحفظة (010...)", "Vodafone Cash Number", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                            if (isOtpSent) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(t("تم إرسال كود سري للموافقة بالرسائل. ادخله هنا لتفعيل التحويل:", "We sent a confirmation text code:", appLanguage), fontSize = 10.sp, color = WhatsappGreen)
                                OutlinedTextField(
                                    value = vodafoneOtp,
                                    onValueChange = { if (it.length <= 6) vodafoneOtp = it },
                                    label = { Text("PIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        } else {
                            Text(t("كود مرجعي فوري للتحصيل صالح لـ 24 ساعة:", "Fawry Pay code reference. Present this to cashiers to pay:", appLanguage), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, PlayfulYellow, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(t("رقم مبيعات خدمة فوري فورا ⚡", "Fawry Service Code", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = fawryCode.chunked(4).joinToString("-"),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PlayfulYellow,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(t("قم بزيارة أي مكان سداد واطلب شحن كود مالي 788 فوري نقدي", "Instruct cashier to pay via code 788 under financial solutions", appLanguage), fontSize = 9.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Text(
                            text = t("تم السداد بنجاح مكتمل! 🥳", "Payment Succeeded Fully! 🥳", appLanguage),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = WhatsappGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(t("رقم الفاتورة المرجعية:", "Invoice ID Reference:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text("#STK-${(1000L..9999L).random()}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(t("الباقة المصدرة للواتس:", "Sticker Item Source:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text(pack.title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(t("المبلغ الإجمالي المقتطع:", "Amount paid:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text(formatPrice(totalAmount, appLanguage, appCurrency, usdRate), fontSize = 11.sp, fontWeight = FontWeight.Black, color = PlayfulYellow)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(WhatsappGreen.copy(alpha = 0.15f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = t(
                                            "✅ تم تحويل نسبة 50% للكريتور (${pack.creatorName}) ليكون رصيد محفظتها الحالية: +${formatPrice(creatorShare, appLanguage, appCurrency, usdRate)}",
                                            "✅ Clean 50% split transferred to Creator (${pack.creatorName})! Cut: +${formatPrice(creatorShare, appLanguage, appCurrency, usdRate)}",
                                            appLanguage
                                        ),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsappGreen,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(2.dp, Color.Red, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(t("مدفوع الكترونياً 🤝 PAID", "ELECTRONICALLY PAID 🤝", appLanguage), fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t("المتابعة لتسجيل بيانات السداد 💳", "Proceed to Payment 💳", appLanguage), fontWeight = FontWeight.Bold)
                }
            } else if (step == 2 && !isProcessing) {
                Button(
                    onClick = {
                        if (paymentMethod == "vodafone" && !isOtpSent) {
                            if (vodafonePhone.isNotBlank()) {
                                isOtpSent = true
                            }
                        } else {
                            isProcessing = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulYellow),
                    enabled = when(paymentMethod) {
                        "card" -> cardNumber.isNotBlank() && cardCvv.isNotBlank()
                        "vodafone" -> vodafonePhone.isNotBlank()
                        else -> true
                    }
                ) {
                    Text(
                        text = if (paymentMethod == "vodafone" && !isOtpSent) t("أرسل كود التحقق للمحفظة 💬", "Send Verification OTP 💬", appLanguage)
                               else t("تأكيد دفع القيمة وتحويل الأموال 🔒", "Confirm Secure Transfer 🔒", appLanguage),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (step == 3) {
                Button(
                    onClick = {
                        viewModel.addPackToWhatsapp(pack.title)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("تحميل وتنشيط على واتساب 💚", "Install & Export to WhatsApp 💚", appLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            if (step != 3) {
                TextButton(onClick = { onDismiss() }) {
                    Text(t("إلغاء المعاملة", "Cancel Transaction", appLanguage))
                }
            }
        }
    )
}

@Composable
fun CustomBundleCheckoutDialog(
    onDismiss: () -> Unit,
    viewModel: StickerPlatformViewModel,
    appLanguage: String,
    appCurrency: String,
    usdRate: Double,
    approvedPacks: List<StickerPack>
) {
    var step by remember { mutableStateOf(1) } // 1: Info, 2: Insert Billing, 3: Receipt Success
    var paymentMethod by remember { mutableStateOf("card") } // card, vodafone, fawry
    
    var cardNumber by remember { mutableStateOf("") }
    var cardName by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    
    var vodafonePhone by remember { mutableStateOf("") }
    var vodafoneOtp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    
    var fawryCode by remember { mutableStateOf((1000000000L..9999999999L).random().toString()) }
    
    var isProcessing by remember { mutableStateOf(false) }
    
    val customBreakdown = viewModel.calculateCustomBundleDetails(approvedPacks)
    val totalAmount = customBreakdown.totalPrice
    
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            kotlinx.coroutines.delay(2000)
            isProcessing = false
            step = 3
            viewModel.buyCustomBundle()
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (step != 3) onDismiss() },
        title = {
            Text(
                text = if (step == 1) t("تأكيد شراء الباقة المخصصة 🛠️", "Confirm Custom Bundle Purchase 🛠️", appLanguage)
                       else if (step == 2) t("بوابة الدفع العادل والتسوق الآمن 🛡️", "Secure Fair-Share Billing 🛡️", appLanguage)
                       else t("فاتورة التحصيل وباقة المفرود الجاهزة ✅", "Custom Bundle Receipt ✅", appLanguage),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (step == 1) {
                    Text(t("تفصيل وتقسيم أرباح الباقة المفرودة المستقلة على الكريتوز بالتساوي:", "Fair-share calculation distribution map to creators:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                    ) {
                        items(customBreakdown.breakdowns) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = t("الباقة الأصلية: ", "Original Pack: ", appLanguage) + item.packTitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PlayfulYellow)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(t("محدد ${item.stickersSelectedCount} من أصل ${item.totalStickersInPack}", "Selected ${item.stickersSelectedCount} of ${item.totalStickersInPack}", appLanguage), fontSize = 9.sp, color = Color.LightGray)
                                        Text(formatPrice(item.costSubtotal, appLanguage, appCurrency, usdRate), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = t("• الكريتور ${item.creatorName} يحصل على: ", "• Creator ${item.creatorName} earnings: ", appLanguage) + formatPrice(item.creatorEarnings, appLanguage, appCurrency, usdRate),
                                        fontSize = 9.sp,
                                        color = WhatsappGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("التكلفة الكلية العادلة:", "Total fair cost value:", appLanguage), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text(formatPrice(totalAmount, appLanguage, appCurrency, usdRate), fontWeight = FontWeight.Black, fontSize = 16.sp, color = PlayfulYellow)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(t("اختر وسيلة الدفع المدعومة بمصر بمعدل فوري:", "Select a payment gateway network:", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (paymentMethod == "card") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = "card" }
                                .border(1.dp, if (paymentMethod == "card") PlayfulYellow else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💳 Card", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(t("فيزا / ميزة", "Visa/Meza", appLanguage), fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (paymentMethod == "vodafone") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = "vodafone" }
                                .border(1.dp, if (paymentMethod == "vodafone") PlayfulYellow else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔴 Cash", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(t("فودافون كاش", "Vodafone", appLanguage), fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (paymentMethod == "fawry") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMethod = "fawry" }
                                .border(1.dp, if (paymentMethod == "fawry") PlayfulYellow else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡ Fawry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(t("فوري كود", "Fawry Pay", appLanguage), fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                } else if (step == 2) {
                    if (isProcessing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PlayfulYellow)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = t("جاري سداد حزمة الكريتورز المخصّصة وقسمة أرباحهم تلقائياً... 🔐", "Processing secure payment allocation split for all selected creators... 🔐", appLanguage),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        if (paymentMethod == "card") {
                            Text(t("الرجاء إدخال بيانات البطاقة البنكية لمتابعة السداد الآمن:", "Enter your bank card credentials to secure pay:", appLanguage), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { if (it.length <= 16) cardNumber = it },
                                label = { Text(t("رقم البطاقة (16 رقم)", "Card Number (16-digits)", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                label = { Text(t("اسم حامل البطاقة", "Cardholder Name", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text(t("صلاحية MM/YY", "Expiry MM/YY", appLanguage)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { if (it.length <= 3) cardCvv = it },
                                    label = { Text("CVV") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        } else if (paymentMethod == "vodafone") {
                            Text(t("ادخل رقم الهاتف الفعال لوادافون كاش للدفع من المحفظة مسبقة لربطها:", "Enter active wallet phone number:", appLanguage), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = vodafonePhone,
                                onValueChange = { vodafonePhone = it },
                                label = { Text(t("رقم فودافون كاش (مثلاً: 01012345678)", "Mobile Wallet Number", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                            if (isOtpSent) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(t("تم إرسال كود سري للموافقة بالرسائل. ادخله هنا لتفعيل التحويل:", "We sent a confirmation text PIN:", appLanguage), fontSize = 10.sp, color = WhatsappGreen)
                                OutlinedTextField(
                                    value = vodafoneOtp,
                                    onValueChange = { if (it.length <= 6) vodafoneOtp = it },
                                    label = { Text("PIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        } else {
                            Text(t("كود مرجعي فوري صالح لمدة يوم كامل. ادفعه بأي كشك أو كارفور:", "Fawry Pay code reference. Present this to cashiers to pay:", appLanguage), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, PlayfulYellow, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(t("رقم مبيعات خدمة فوري فورا ⚡", "Official Fawry Service Code", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = fawryCode.chunked(4).joinToString("-"),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PlayfulYellow,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(t("قم بسداد القيمة المذكورة في كود 788 بالخدمات المالية فوري", "Instruct cashier to pay via code 788 under financial solutions", appLanguage), fontSize = 9.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Text(
                            text = t("تم تكوين وتخلص الباقة المفرودة المخصصة بنجاح! 🥳", "Custom Bundle Paid & Cleared! 🥳", appLanguage),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = WhatsappGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(t("رقم الفاتورة لحزمة المفرود:", "Custom Invoice ID:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text("#BNDL-${(1000L..9999L).random()}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(t("الملصقات الكلية المودعة:", "Total stickers selected:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text("${customBreakdown.breakdowns.sumOf { it.stickersSelectedCount }} " + t("ملصقات مخصصة", "custom stickers", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(t("إجمالي الحساب المفصول:", "Total paid amount:", appLanguage), fontSize = 10.sp, color = Color.Gray)
                                    Text(formatPrice(totalAmount, appLanguage, appCurrency, usdRate), fontSize = 11.sp, fontWeight = FontWeight.Black, color = PlayfulYellow)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(WhatsappGreen.copy(alpha = 0.15f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = t(
                                            "✅ تم تحويل المستحقات الفردية وتوزيعها على كافة صنّاع الباقات المختارة بنجاح بنسبة 50% لكل منهم!",
                                            "✅ All individual cuts were safely dispatched to the respective creators!",
                                            appLanguage
                                        ),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsappGreen,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(2.dp, Color.Red, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(t("تم السداد العادل بالخوادم 🤝 PAID", "FAIR-SHARE ELECTRONICALLY PAID 🤝", appLanguage), fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t("المتابعة لتسديد قيمة الحزمة 💳", "Proceed to Payment 💳", appLanguage), fontWeight = FontWeight.Bold)
                }
            } else if (step == 2 && !isProcessing) {
                Button(
                    onClick = {
                        if (paymentMethod == "vodafone" && !isOtpSent) {
                            if (vodafonePhone.isNotBlank()) {
                                isOtpSent = true
                            }
                        } else {
                            isProcessing = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PlayfulYellow),
                    enabled = when(paymentMethod) {
                        "card" -> cardNumber.isNotBlank() && cardCvv.isNotBlank()
                        "vodafone" -> vodafonePhone.isNotBlank()
                        else -> true
                    }
                ) {
                    Text(
                        text = if (paymentMethod == "vodafone" && !isOtpSent) t("أرسل كود التحقق للمحفظة 💬", "Send Verification OTP 💬", appLanguage)
                               else t("تأكيد دفع القيمة وتحويل الأموال 🔒", "Confirm Secure Transfer 🔒", appLanguage),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (step == 3) {
                Button(
                    onClick = {
                        viewModel.addPackToWhatsapp("My Custom Bundle")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("تصدير وتثبيت حزمتك على WhatsApp 💚", "Install Custom Bundle to WhatsApp 💚", appLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            if (step != 3) {
                TextButton(onClick = { onDismiss() }) {
                    Text(t("إلغاء المعاملة", "Cancel Transaction", appLanguage))
                }
            }
        }
    )
}

// ------------------------------------------------------------------------------------------
// 3. CREATOR HOME & UPLOADING FLOW SCREEN WITH RETRANSLATED REVENUE CALCULATORS
// ------------------------------------------------------------------------------------------
@Composable
fun CreatorHomeScreen(viewModel: StickerPlatformViewModel) {
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val creators by viewModel.allCreators.collectAsStateWithLifecycle()
    val allStickerPacks by viewModel.allStickerPacks.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val myCreatorRecord = creators.find { it.id == userSession?.email }
    val myPacks = allStickerPacks.filter { it.creatorId == userSession?.email }

    // Navigation and tab selector
    var activeTab by remember { mutableStateOf(1) } // 1: My Packs, 2: Sticker Maker Workbench

    // Upload new pack state (for tab 1)
    var showUploadPackDialog by remember { mutableStateOf(false) }
    var packTitleInput by remember { mutableStateOf("") }
    var packDescInput by remember { mutableStateOf("") }
    var packPriceInput by remember { mutableStateOf("") }
    var packCoverEmoji by remember { mutableStateOf("🎭") }

    // Dynamic sticker lines adding
    var currentStickerText by remember { mutableStateOf("") }
    val listOfStickerItems = remember { mutableStateListOf<String>() }

    // Workbench Form States (for tab 2)
    var workbenchImageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=500&q=80") }
    var workbenchEmojiCover by remember { mutableStateOf("🎬") }
    var workbenchTopText by remember { mutableStateOf("أنا لما المدونة تخلص!") }
    var workbenchBottomText by remember { mutableStateOf("يا فتاح يا عليم يا رزاق يا كريم!") }
    var workbenchOutlineType by remember { mutableStateOf("yellow") } // none, white, yellow, black
    var workbenchTransparentBg by remember { mutableStateOf(true) }
    var workbenchDestinationType by remember { mutableStateOf("custom_solo") } // custom_solo, first_pack
    var workbenchTextColorName by remember { mutableStateOf("yellow") } // white, yellow, neon_green

    // Generation animator states
    var isGenerating by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf(0f) }
    var generationStatusMessage by remember { mutableStateOf("") }

    // Processing simulator
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            generationProgress = 0.1f
            generationStatusMessage = "⚙️ جاري قراءة معلومات الصورة وتفريع الخلفية..."
            kotlinx.coroutines.delay(800)
            
            generationProgress = 0.5f
            generationStatusMessage = "⚡️ جاري حسم مسارات بيكسل الملصق وحفر القنوات الشفافة..."
            kotlinx.coroutines.delay(1000)
            
            generationProgress = 0.8f
            generationStatusMessage = "🎨 جاري دمج النصوص والحدود الكرتونية المحددة بدقة 512x512..."
            kotlinx.coroutines.delay(800)
            
            generationProgress = 1.0f
            generationStatusMessage = "🎉 تم دمج وتوليد الملصق وإدراجه في حساب الكريتور بنجاح!"
            kotlinx.coroutines.delay(600)
            
            // Actually insert into current user packs
            if (workbenchDestinationType == "custom_solo") {
                viewModel.createNewPack(
                    title = if (appLanguage == "AR") "ملصقات مخصصة مفرودة من الصانع" else "My Custom Creator Indiv Solo",
                    description = if (appLanguage == "AR") "حزمة ملصقات مستقلة تم دمجها تفاعلياً" else "Independently generated sticker items",
                    price = 7.0,
                    coverEmoji = workbenchEmojiCover,
                    stickerTitles = listOf("${workbenchTopText} • ${workbenchBottomText}")
                )
            } else {
                viewModel.createNewPack(
                    title = if (workbenchTopText.length > 3) workbenchTopText.substring(0, kotlin.math.min(workbenchTopText.length, 12)) + " Pack" else "Meme Pack",
                    description = "${workbenchTopText} - ${workbenchBottomText}",
                    price = 15.0,
                    coverEmoji = workbenchEmojiCover,
                    stickerTitles = listOf("${workbenchTopText} • ${workbenchBottomText}", "ردة فعل عفوية 🎭", "أفشات الميمز 💬")
                )
            }

            // reset generator state
            isGenerating = false
            activeTab = 1 // Switch to view packs
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and dashboard statistics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ComicBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(userSession?.avatarUrl ?: "🎨", fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "مرحباً بصانع الابتسامة المعتمد! 🎨",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                            Text("البريد: ${userSession?.email}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("محفظتك وأرباحك المباشرة (ج.م):", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format("%.2f", myCreatorRecord?.balanceEgp ?: 0.0)} ج.م",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = PrimaryYellow
                        )
                        Button(
                            onClick = { /* simulated withdrawal logic */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("سحب الأرباح 💸", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "💡 ستحصل على حصة كاملة قدرها 50% عن كل عملية بيع كاملة للباقة الخاصة بك، أو النسبة الصحيحة والعادلة لكل ملصق مخصص يختاره المشتري ضمن حزمته المفرودة!",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // TABS SELECTOR (Tabs are clickable, have ripple effect, safe touch target size)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { activeTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 1) ComicBlue else Color.Transparent,
                        contentColor = if (activeTab == 1) Color.White else Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("باقاتي المرفوعة 🗂️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { activeTab = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == 2) PlayfulYellow else Color.Transparent,
                        contentColor = if (activeTab == 2) Color.Black else Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ورشة الملصقات 🛠️🎨", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // TAB CONTENT
        if (activeTab == 1) {
            // Action header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حزم وملصقات قمت بصنعها (${myPacks.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Button(
                        onClick = { showUploadPackDialog = true },
                        modifier = Modifier.testTag("add_sticker_pack_trigger")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع باقة ميمز جديدة 🎨")
                    }
                }
            }

            // Packs list
            if (myPacks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧑‍🎨", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لم ترفع أي باقات للبيع بعد. ابدأ بصناعة باقتك الأولى!", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(myPacks) { pack ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pack.coverUrl, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pack.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("السعر المحدد: ${pack.priceEgp} ج.م", fontSize = 11.sp, color = PlayfulYellow, fontWeight = FontWeight.Bold)
                                }
                                // Status tag
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (pack.isApproved) WhatsappGreen.copy(alpha = 0.15f) else PlayfulYellow.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Text(
                                        text = if (pack.isApproved) "تمت الموافقة ونشطة ✅" else "قيد مراجعة الإدارة ⏳",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = if (pack.isApproved) WhatsappGreen else PlayfulYellow
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(pack.description, fontSize = 11.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تاريخ الرفع: نشط الآن", fontSize = 9.sp, color = Color.Gray)
                                Text("إجمالي التنزيلات: ${pack.downloads} مرة", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 2: STICKER GENERATION WORKBENCH
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color.Black, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🛠️ ورشة صنع وتعديل الملصقات التفاعلية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = PlayfulYellow
                        )
                        Text(
                            text = "صمم ملصقك الفكاهي بالكامل: ارفع صورتك الخاصة، أفرغ الخلفية بضغطة زر، واصنع تايبوغرافي الميمز الكوميدي!",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PHOTO TRANSPARENT CANVAS PREVIEW
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                                .background(Color.White), // Fallback white frame
                            contentAlignment = Alignment.Center
                        ) {
                            // Sub-layers representing checkerboard layout for transparent stickers
                            if (workbenchTransparentBg) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizeX = 16.dp.toPx()
                                    val rows = (size.height / sizeX).toInt() + 1
                                    val cols = (size.width / sizeX).toInt() + 1
                                    for (r in 0..rows) {
                                        for (c in 0..cols) {
                                            if ((r + c) % 2 == 0) {
                                                drawRect(
                                                    color = Color.LightGray.copy(alpha = 0.35f),
                                                    topLeft = androidx.compose.ui.geometry.Offset(c * sizeX, r * sizeX),
                                                    size = androidx.compose.ui.geometry.Size(sizeX, sizeX)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // The Active Sticker Graphic Layout
                            val stickerContourColor = when(workbenchOutlineType) {
                                "white" -> Color.White
                                "yellow" -> PlayfulYellow
                                "black" -> Color.Black
                                else -> Color.Transparent
                            }
                            val textHexColor = when(workbenchTextColorName) {
                                "white" -> Color.White
                                "yellow" -> PlayfulYellow
                                else -> WhatsappGreen
                            }

                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (workbenchTransparentBg) Color.Transparent else Color.White)
                                    .border(
                                        width = if (workbenchOutlineType != "none") 4.dp else 0.dp,
                                        color = stickerContourColor,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Top Caption Overlay
                                    if (workbenchTopText.isNotBlank()) {
                                        Text(
                                            text = workbenchTopText.uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = textHexColor,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    // Inner Sticker Character Frame
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(workbenchEmojiCover, fontSize = 54.sp)
                                        Text(
                                            text = if (workbenchTransparentBg) "PNG • مفرغ الشفافية" else "JPG • صلب",
                                            fontSize = 8.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Bottom Caption Overlay
                                    if (workbenchBottomText.isNotBlank()) {
                                        Text(
                                            text = workbenchBottomText.uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = textHexColor,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }

                            // Watermark / Format Indicator
                            Text(
                                text = "512x512 px (طبعة واتساب 🛡️)",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black, RoundedCornerShape(topStart = 8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PRESET MEME PICKERS
                        Text("💡 اختر نموذج ميمي مصري شهير للتحميل السريع:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presets = listOf(
                                Triple("عادل إمام 😠", "🎬", "أنا لما الكود يشتغل من أول مرة!"),
                                Triple("مستر بين 🐻", "🐻", "إيه ده يا هيروم؟"),
                                Triple("هنيدي سعيد 🎓", "🎓", "يا عم صلي على النبي!"),
                                Triple("قطة كيتي 🐱", "🐱", "نعم يا فندم؟! كيف ذلك؟")
                            )
                            presets.forEach { (label, emj, defaultTxt) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            workbenchEmojiCover = emj
                                            workbenchTopText = label
                                            workbenchBottomText = defaultTxt
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(emj, fontSize = 20.sp)
                                        Text(label, fontSize = 8.sp, maxLines = 1, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // FILE SELECTOR SIMULATOR BUTTON
                        Button(
                            onClick = {
                                viewModel.updateShowcase(
                                    "Sticker Generator Preview Pro $workbenchEmojiCover",
                                    "تصميم مفرود: $workbenchTopText",
                                    8.0,
                                    workbenchEmojiCover
                                )
                                workbenchEmojiCover = listOf("🔥", "🤪", "😂", "🤯", "👽").random()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ComicBlue),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("رفع صورة مخصصة من الهاتف واستيرادها 📁", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                        }

                        // CAPTION CONTROLS INPUTS
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("✍️ تحرير نصوص وتأثيرات الصاق الميم:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = workbenchTopText,
                            onValueChange = { workbenchTopText = it },
                            label = { Text("النص العلوي (Header Caption)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = workbenchBottomText,
                            onValueChange = { workbenchBottomText = it },
                            label = { Text("النص السفلي (Footer Caption)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // EFFECT CHOOSERS
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Text color chooser
                            Column(modifier = Modifier.weight(1f)) {
                                Text("لون خط الميمز 🎨", fontSize = 10.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("white", "yellow", "green").forEach { col ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (col) {
                                                        "white" -> Color.White
                                                        "yellow" -> PlayfulYellow
                                                        else -> WhatsappGreen
                                                    }
                                                )
                                                .border(
                                                    width = if (workbenchTextColorName == col) 2.dp else 0.dp,
                                                    color = Color.Black
                                                )
                                                .clickable { workbenchTextColorName = col }
                                        )
                                    }
                                }
                            }

                            // Outline chooser
                            Column(modifier = Modifier.weight(1f)) {
                                Text("لون تحديد حدود الاستيكر ✒️", fontSize = 10.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("none", "white", "yellow", "black").forEach { ot ->
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                .background(
                                                    when (ot) {
                                                        "white" -> Color.White
                                                        "yellow" -> PlayfulYellow
                                                        "black" -> Color.Black
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (workbenchOutlineType == ot) 2.dp else 0.dp,
                                                    color = if (ot == "black" || ot == "none") PlayfulYellow else Color.Black,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable { workbenchOutlineType = ot },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (ot == "none") Text("❌", fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // TRANSPARENCY TOGGLE SWITCH
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("تفريع الخلفية لشفافية الواتس اب 👥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("إزالة مساحة الخلفية تلقائياً وعزل المظهر كاستيكر شفاف", fontSize = 8.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = workbenchTransparentBg,
                                onCheckedChange = { workbenchTransparentBg = it }
                            )
                        }

                        // CONFIGURING EXPORT DESTINATION
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("🚀 وجهة حفظ وتصدير الملصق المصنع:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { workbenchDestinationType = "custom_solo" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (workbenchDestinationType == "custom_solo") PlayfulYellow else Color.DarkGray,
                                    contentColor = if (workbenchDestinationType == "custom_solo") Color.Black else Color.White
                                ),
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("ملصق مخصص فردي 🎨", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { workbenchDestinationType = "first_pack" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (workbenchDestinationType == "first_pack") ComicBlue else Color.DarkGray,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("باقة استيكرات متكاملة 📦", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // GENERATE BUTTON
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isGenerating) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = generationProgress,
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = PlayfulYellow,
                                    trackColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(generationStatusMessage, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { isGenerating = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("workbench_generate_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (workbenchDestinationType == "custom_solo") "تصدير وحفظ الملصق المخصص الفردي ⚡️"
                                           else "بناء باقة ميمز مدمجة بالكامل 🚀",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // UPLOAD STICKERS/MEMES PACKAGE DIALOG FORM
    if (showUploadPackDialog) {
        val emojis = listOf("🎭", "🐻", "🎬", "🤪", "🤠", "🤖", "🔥", "😎", "😂")

        AlertDialog(
            onDismissRequest = { showUploadPackDialog = false },
            title = {
                Text(
                    "تصميم باقة ميمز جديدة للبيع والتصدير 🎨",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = packTitleInput,
                            onValueChange = { packTitleInput = it },
                            label = { Text("عنوان باقة الملصقات (مثلاً: رياكشنات عادل إمام)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pack_title_input"),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = packDescInput,
                            onValueChange = { packDescInput = it },
                            label = { Text("وصف مختصر للباقة (سيسهل العثور عليه بالبحث)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = packPriceInput,
                            onValueChange = { packPriceInput = it },
                            label = { Text("السعر بالجنيه المصري (مثلاً: 12)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pack_price_input"),
                            singleLine = true
                        )
                    }

                    item {
                        Text("اختر رمزاً من تعبيرات مستر بين كأيقونة للباقة:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            emojis.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(
                                            width = if (packCoverEmoji == item) 2.dp else 1.dp,
                                            color = if (packCoverEmoji == item) PlayfulYellow else Color.LightGray,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { packCoverEmoji = item },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "الملصقات المضافة حالياً لباقة الكريتوز (${listOfStickerItems.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    if (listOfStickerItems.isEmpty()) {
                        item {
                            Text("⚠️ لم تقم بكتابة أو إضافة أي ملصقات للباقة بعد. اكتب ملصق في الحقل أدناه لإضافته للباقة الخاصة بك.", fontSize = 9.sp, color = Color.Gray)
                        }
                    } else {
                        items(listOfStickerItems) { sticker ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = sticker, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { listOfStickerItems.remove(sticker) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentStickerText,
                                onValueChange = { currentStickerText = it },
                                label = { Text("اكتب العبارة أو رياكشن الملصق!") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sticker_text_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (currentStickerText.isNotBlank()) {
                                        listOfStickerItems.add(currentStickerText.trim())
                                        currentStickerText = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("add_sticker_sub_item_btn")
                            ) {
                                Text("إضافة")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceNum = packPriceInput.toDoubleOrNull() ?: 0.0
                        if (packTitleInput.isNotBlank() && priceNum > 0 && listOfStickerItems.isNotEmpty()) {
                            viewModel.createNewPack(
                                title = packTitleInput.trim(),
                                description = packDescInput.trim(),
                                price = priceNum,
                                coverEmoji = packCoverEmoji,
                                stickerTitles = listOfStickerItems.toList()
                            )
                            // reset
                            packTitleInput = ""
                            packDescInput = ""
                            packPriceInput = ""
                            listOfStickerItems.clear()
                            showUploadPackDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("رفع الباقة وإرسالها لموافقة الإدارة 🚀", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadPackDialog = false }) {
                    Text("إلغاء وتعديل")
                }
            }
        )
    }
}

// ------------------------------------------------------------------------------------------
// 4. ADMIN DASHBOARD DESK SCREEN (APPROVINGS, BROADCASTERS, SYSTEMS REVENUE STATS)
// ------------------------------------------------------------------------------------------
@Composable
fun AdminDashboardScreen(viewModel: StickerPlatformViewModel) {
    val allPacks by viewModel.allStickerPacks.collectAsStateWithLifecycle()
    val allPurchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val pendingPacks = allPacks.filter { !it.isApproved }

    val showcaseTitleState by viewModel.showcaseTitle.collectAsStateWithLifecycle()
    val showcaseSubState by viewModel.showcaseSub.collectAsStateWithLifecycle()
    val showcasePriceState by viewModel.showcasePriceEgp.collectAsStateWithLifecycle()
    val showcaseCoverEmojiState by viewModel.showcaseCoverEmoji.collectAsStateWithLifecycle()
    val usdToEgpRateState by viewModel.usdToEgpRate.collectAsStateWithLifecycle()

    var editShowcaseTitle by remember(showcaseTitleState) { mutableStateOf(showcaseTitleState) }
    var editShowcaseSub by remember(showcaseSubState) { mutableStateOf(showcaseSubState) }
    var editShowcasePrice by remember(showcasePriceState) { mutableStateOf(showcasePriceState.toString()) }
    var editShowcaseEmoji by remember(showcaseCoverEmojiState) { mutableStateOf(showcaseCoverEmojiState) }
    var editExchangeRate by remember(usdToEgpRateState) { mutableStateOf(usdToEgpRateState.toString()) }

    var announceTitle by remember { mutableStateOf("") }
    var announceContent by remember { mutableStateOf("") }

    // Calc system totals
    val systemTotalSales = allPurchases.sumOf { it.amountPaidEgp }
    val platformEarnings = systemTotalSales * 0.5
    val creatorsEarningsTotal = systemTotalSales * 0.5

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and high-contrast system details stats
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ComicYellowDark, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "ممر قيادة الإدارة والتحليلات العامة 👑",
                        fontWeight = FontWeight.Black,
                        color = PrimaryYellow,
                        fontSize = 18.sp
                    )
                    Text("نظام الرصد التلقائي ومصادقة حزم ملصقات الكريتورز المربحة.", fontSize = 11.sp, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 1: Total volume
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي مبيعات السوق", fontSize = 10.sp, color = Color.White)
                                Text("${String.format("%.2f", systemTotalSales)} ج.م", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlayfulYellow)
                            }
                        }

                        // Stat 2: Platform profits
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أرباح المنصة (50%)", fontSize = 10.sp, color = Color.White)
                                Text("${String.format("%.2f", platformEarnings)} ج.م", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WhatsappGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("أرباح الكريتورز الكلية المستحقة للتوزيع (50%)", fontSize = 10.sp, color = Color.White)
                            Text("${String.format("%.2f", creatorsEarningsTotal)} ج.م", fontWeight = FontWeight.Black, fontSize = 16.sp, color = PrimaryYellow)
                        }
                    }
                }
            }
        }

        // Section 1: Pending Approvals Queue
        item {
            Text(
                "طلبات الموافقة المعلقة لحزم وملصقات جديدة (${pendingPacks.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PlayfulYellow
            )
        }

        if (pendingPacks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👋 لا توجد باقات ملصقات جديدة في قائمة المراجعة حالياً.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(pendingPacks) { pack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color.Black, RoundedCornerShape(16.dp))
                        .testTag("admin_pending_pack_card_${pack.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PlayfulYellow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pack.coverUrl, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pack.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("مقدم من: ${pack.creatorName} • السعر: ${pack.priceEgp} ج.م", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(pack.description, fontSize = 11.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.approveStickerPack(pack.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("approve_btn_${pack.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قبول وبث الباقة ✅", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.rejectStickerPack(pack.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reject_btn_${pack.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفض وحذف ❌", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Public Notification Broadcast panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "إرسال إعلان / إشعار تلقائي للجميع 📢",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PlayfulYellow
                    )
                    Text("سيظهر هذا كشعار منبثق فوري ورقمي لجميع مستخدمي المنصة.", fontSize = 10.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = announceTitle,
                        onValueChange = { announceTitle = it },
                        label = { Text("عنوان التنبيه") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = announceContent,
                        onValueChange = { announceContent = it },
                        label = { Text("نص الإشعار الكوميدي أو الإداري") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_content_input"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (announceTitle.isNotBlank() && announceContent.isNotBlank()) {
                                viewModel.sendGlobalNotification(announceTitle, announceContent)
                                announceTitle = ""
                                announceContent = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_send_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بث إشعار مع مروحية إشعارات memes 🚀", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Manage Homepage Display Magazine 📺
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "إدارة مجلة العرض واللافتة الترويجية بالرئيسية 📺",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PlayfulYellow
                    )
                    Text("تحكم بمحتويات البانر الترويجي المثبت أعلى الصفحة الرئيسية للمشترين.", fontSize = 10.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editShowcaseTitle,
                        onValueChange = { editShowcaseTitle = it },
                        label = { Text("عنوان البانر الرئيسي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editShowcaseSub,
                        onValueChange = { editShowcaseSub = it },
                        label = { Text("الوصف الترويجي وحجم الملصقات") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editShowcasePrice,
                            onValueChange = { editShowcasePrice = it },
                            label = { Text("السعر ج.م") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editShowcaseEmoji,
                            onValueChange = { editShowcaseEmoji = it },
                            label = { Text("إيموجي الغلاف") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val pr = editShowcasePrice.toDoubleOrNull() ?: 45.0
                            viewModel.updateShowcase(editShowcaseTitle, editShowcaseSub, pr, editShowcaseEmoji)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_save_banner_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحديث وتغيير محتويات مجلة العرض البانر 🚀", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 4: Global Multi-Currency Exchange Panel 💱
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "بوابة أسعار الصرف الرقمي وتعدد العملات 💱",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PlayfulYellow
                    )
                    Text("قم بتحديث سعر الصرف لتحديث تسعير الملصقات بالدولار تلقائياً.", fontSize = 10.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editExchangeRate,
                        onValueChange = { editExchangeRate = it },
                        label = { Text("سعر صرف الدولار مقابل الجنيه المصري (1 USD = ? EGP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val rt = editExchangeRate.toDoubleOrNull() ?: 50.0
                            viewModel.updateExchangeRate(rt)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_save_rate_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ComicBlue)
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعديل وتعميم سعر الصرف في المتجر 💾", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
