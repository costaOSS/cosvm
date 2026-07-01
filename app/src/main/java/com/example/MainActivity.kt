package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

// ==========================================
// DATA MODELS
// ==========================================

enum class VMStatus {
    STOPPED, STARTING, RUNNING, SUSPENDED
}

data class VirtualApp(
    val id: String,
    val name: String,
    val packageName: String,
    val iconEmoji: String,
    val sizeMb: Int,
    val is64BitCompatible: Boolean,
    val minAndroidVersion: Int,
    var isInstalled: Boolean = false
)

data class VirtualMachine(
    val id: String,
    val name: String,
    val androidVersion: String,
    var status: VMStatus,
    val cpuCores: Int,
    val ramGb: Int,
    val resolution: String,
    val densityDpi: Int,
    val deviceModel: String,
    var carrier: String = "Cosmic Mobile",
    var batteryLevel: Int = 85,
    var isCharging: Boolean = false,
    var isRooted: Boolean = false,
    val isPageSize16k: Boolean = true,
    var installedApps: List<VirtualApp> = emptyList()
)

data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val timestamp: String = ""
)

data class VirtualFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val content: String = ""
)

// ==========================================
// CORE ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CosVMApp()
            }
        }
    }
}

// ==========================================
// MAIN APP NAVIGATION & WRAPPER
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosVMApp() {
    var currentScreen by rememberSaveable { mutableStateOf("dashboard") } // "dashboard", "diagnostics", "workspace"
    var selectedVmId by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Core state of Virtual Machines
    var virtualMachines by remember {
        mutableStateOf(
            listOf(
                VirtualMachine(
                    id = "vm_01",
                    name = "CosVM Android 16 Core",
                    androidVersion = "Android 16 (API 36)",
                    status = VMStatus.RUNNING,
                    cpuCores = 4,
                    ramGb = 6,
                    resolution = "1080x2400",
                    densityDpi = 440,
                    deviceModel = "CosVM Pro 64-bit",
                    carrier = "Aether Net",
                    batteryLevel = 92,
                    isCharging = true,
                    isRooted = false,
                    isPageSize16k = true,
                    installedApps = listOf(
                        VirtualApp("app_browser", "🌐 Web Browser", "com.cosvm.browser", "🌐", 18, true, 26, true),
                        VirtualApp("app_terminal", "💻 Safe Terminal", "com.cosvm.terminal", "💻", 4, true, 26, true),
                        VirtualApp("app_files", "📁 Sandbox Files", "com.cosvm.files", "📁", 8, true, 26, true),
                        VirtualApp("app_game", "🎮 Retro BrickBreaker", "com.cosvm.brickbreaker", "🎮", 12, true, 26, true),
                        VirtualApp("app_settings", "⚙️ VM Settings", "com.cosvm.settings", "⚙️", 6, true, 26, true)
                    )
                ),
                VirtualMachine(
                    id = "vm_02",
                    name = "Legacy Sandbox (Android 12)",
                    androidVersion = "Android 12 (API 31)",
                    status = VMStatus.STOPPED,
                    cpuCores = 2,
                    ramGb = 2,
                    resolution = "720x1280",
                    densityDpi = 320,
                    deviceModel = "Legacy-X 32/64",
                    carrier = "Cosmic Mobile",
                    batteryLevel = 45,
                    isCharging = false,
                    isRooted = true,
                    isPageSize16k = false,
                    installedApps = listOf(
                        VirtualApp("app_browser", "🌐 Web Browser", "com.cosvm.browser", "🌐", 18, true, 26, true),
                        VirtualApp("app_terminal", "💻 Safe Terminal", "com.cosvm.terminal", "💻", 4, true, 26, true),
                        VirtualApp("app_files", "📁 Sandbox Files", "com.cosvm.files", "📁", 8, true, 26, true)
                    )
                ),
                VirtualMachine(
                    id = "vm_03",
                    name = "Secure Vault (Android 15)",
                    androidVersion = "Android 15 (API 35)",
                    status = VMStatus.STOPPED,
                    cpuCores = 8,
                    ramGb = 8,
                    resolution = "1440x3120",
                    densityDpi = 560,
                    deviceModel = "Secure Vault S15",
                    carrier = "Shield SIM",
                    batteryLevel = 100,
                    isCharging = false,
                    isRooted = false,
                    isPageSize16k = true,
                    installedApps = listOf(
                        VirtualApp("app_browser", "🌐 Web Browser", "com.cosvm.browser", "🌐", 18, true, 26, true),
                        VirtualApp("app_files", "📁 Sandbox Files", "com.cosvm.files", "📁", 8, true, 26, true)
                    )
                )
            )
        )
    }

    val activeVm = virtualMachines.find { it.id == selectedVmId } ?: virtualMachines.firstOrNull()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (currentScreen != "workspace") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { currentScreen = "dashboard" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Workspace") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentScreen == "diagnostics",
                        onClick = { currentScreen = "diagnostics" },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Diagnostics") },
                        label = { Text("64-Bit Guard") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_diagnostics")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "dashboard" -> {
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        vms = virtualMachines,
                        onCreateVmClick = { showCreateDialog = true },
                        onLaunchVm = { vm ->
                            selectedVmId = vm.id
                            // Ensure the VM is set to running
                            virtualMachines = virtualMachines.map {
                                if (it.id == vm.id) it.copy(status = VMStatus.RUNNING) else it
                            }
                            currentScreen = "workspace"
                        },
                        onStopVm = { vm ->
                            virtualMachines = virtualMachines.map {
                                if (it.id == vm.id) it.copy(status = VMStatus.STOPPED) else it
                            }
                        },
                        onToggleRoot = { vm ->
                            virtualMachines = virtualMachines.map {
                                if (it.id == vm.id) it.copy(isRooted = !it.isRooted) else it
                            }
                        },
                        onDeleteVm = { vm ->
                            virtualMachines = virtualMachines.filter { it.id != vm.id }
                        }
                    )
                }
                "diagnostics" -> {
                    DiagnosticsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                "workspace" -> {
                    if (activeVm != null) {
                        WorkspaceScreen(
                            vm = activeVm,
                            onBackToDashboard = {
                                currentScreen = "dashboard"
                            },
                            onUpdateVmState = { updatedVm ->
                                virtualMachines = virtualMachines.map {
                                    if (it.id == updatedVm.id) updatedVm else it
                                }
                            }
                        )
                    } else {
                        currentScreen = "dashboard"
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateVmDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { newVm ->
                virtualMachines = virtualMachines + newVm
                showCreateDialog = false
            }
        )
    }
}

// ==========================================
// SCREEN 1: VM DASHBOARD (WORKSPACE LIST)
// ==========================================

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    vms: List<VirtualMachine>,
    onCreateVmClick: () -> Unit,
    onLaunchVm: (VirtualMachine) -> Unit,
    onStopVm: (VirtualMachine) -> Unit,
    onToggleRoot: (VirtualMachine) -> Unit,
    onDeleteVm: (VirtualMachine) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Hero Header Panel
        HeaderPanel()

        Spacer(modifier = Modifier.height(24.dp))

        // VM Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Virtual Environments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${vms.size} Container instances configured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onCreateVmClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("create_vm_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New VM")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Instances Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE INSTANCES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Android 16 Kernel",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (vms.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛸", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No Containers Detected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Click 'New VM' to instantiate a sandboxed Android 16 container.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            vms.forEach { vm ->
                VmCard(
                    vm = vm,
                    onLaunch = { onLaunchVm(vm) },
                    onStop = { onStopVm(vm) },
                    onToggleRoot = { onToggleRoot(vm) },
                    onDelete = { onDeleteVm(vm) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Security Protocol Prompt Banner
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Enhanced Security Protocols are enabled. All VM I/O is being encrypted via hardware-backed keystore.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun HeaderPanel() {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by pulseAnimation.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Modern grid lines matching Immersive UI cyber aesthetic
                    val strokeWidth = 1.dp.toPx()
                    val color = Color(0xFF1E293B).copy(alpha = 0.3f)
                    val step = 40.dp.toPx()
                    for (x in 0..size.width.toInt() step step.toInt()) {
                        drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth)
                    }
                    for (y in 0..size.height.toInt() step step.toInt()) {
                        drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth)
                    }
                }
                .padding(20.dp)
        ) {
            // Absolute watermark '64' in top-right with low opacity
            Text(
                text = "64",
                fontSize = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-20).dp)
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = alphaAnim))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HYPERVISOR ACTIVE • ARM64-v8a",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CosVM Hypervisor Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Seamless 64-bit translation active on modern ARM64-v8a and x86_64 cores.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Active Stats progress meters
                Text(
                    text = "HYPERVISOR PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CPU Usage (12.4%)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("CPU USAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                            Text("12.4%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.124f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    // Memory (2.1GB)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("MEMORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                            Text("2.1 GB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsBadge(emoji: String, label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun VmCard(
    vm: VirtualMachine,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onToggleRoot: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (vm.status == VMStatus.RUNNING) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (vm.status == VMStatus.RUNNING) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) 
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (vm.androidVersion.contains("16")) "🛸" else "🤖",
                        fontSize = 22.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = vm.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = vm.androidVersion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = vm.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spec Grid Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecItem(label = "CPU Allocation", value = "${vm.cpuCores} Cores")
                SpecItem(label = "Guest Memory", value = "${vm.ramGb} GB RAM")
                SpecItem(label = "Virtual Screen", value = vm.resolution)
                SpecItem(label = "16KB Align", value = if (vm.isPageSize16k) "Yes (A16)" else "No (4KB)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("vm_config_${vm.id}")
                ) {
                    Text(if (expanded) "Hide Details" else "VM Options")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (vm.status == VMStatus.RUNNING) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onLaunch,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("launch_vm_${vm.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Enter", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enter VM")
                    }
                } else {
                    Button(
                        onClick = onLaunch,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("launch_vm_${vm.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start VM")
                    }
                }
            }

            // Advanced configuration expansion
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Text(
                        "Advanced Sandbox Parameters",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ConfigRow(label = "Device Emulation Profile", value = vm.deviceModel)
                    ConfigRow(label = "Isolated Root Sandbox", value = if (vm.isRooted) "Superuser Enabled" else "Disabled (Strict Security)")
                    ConfigRow(label = "Simulated Telecom Carrier", value = vm.carrier)
                    ConfigRow(label = "Architecture Targets", value = "arm64-v8a, x86_64")
                    ConfigRow(label = "Installed System Apps", value = "${vm.installedApps.size} package binaries")

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onToggleRoot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (vm.isRooted) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = if (vm.isRooted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (vm.isRooted) "Disable Guest Root" else "Enable Guest Root")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete Instance")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: VMStatus) {
    val (color, label) = when (status) {
        VMStatus.STOPPED -> Pair(Color(0xFFFF1744), "Stopped")
        VMStatus.STARTING -> Pair(Color(0xFFFFD600), "Starting")
        VMStatus.RUNNING -> Pair(Color(0xFF00E676), "Running")
        VMStatus.SUSPENDED -> Pair(Color(0xFF8A2BE2), "Suspended")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 9.sp
        )
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// SCREEN 2: DIAGNOSTICS & 16KB GUARD
// ==========================================

@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "64-Bit Security Guard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Android 16 Compatibility Audit & Hardware Reports",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Canvas graphic illustrating 16KB vs 4KB Page Sizes
        PageSizeComparisonGraphic()

        Spacer(modifier = Modifier.height(24.dp))

        // Hardware details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Device Hardware Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                DiagnosticSpecRow(label = "Host ABI Architectures", value = "arm64-v8a, x86_64")
                DiagnosticSpecRow(label = "Kotlin Version", value = "2.2.10")
                DiagnosticSpecRow(label = "Target SDK Level", value = "API 36 (Android 16 compatible)")
                DiagnosticSpecRow(label = "16KB Page Sizing", value = "ENABLED & VERIFIED")
                DiagnosticSpecRow(label = "SELinux Enforcing Status", value = "Enforcing (Sandboxed)")
                DiagnosticSpecRow(label = "Kernel Isolation level", value = "Level 4 Guest Namespaces")
                DiagnosticSpecRow(label = "Dynamic Relocation Table", value = "ELF64 Aligned")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Android 16 Security Protocol checklist
        Text(
            "Android 16 Architecture Checklist",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        SecurityCheckItem(
            title = "Enforced 64-Bit Binary Loading",
            desc = "CosVM blocks execution of any legacy 32-bit (armeabi/x86) instructions. Android 16 entirely removes 32-bit execution, improving memory layout safety.",
            isChecked = true
        )
        SecurityCheckItem(
            title = "16KB Page Aligned ELF Headers",
            desc = "Modern ARM64 devices and Android 16 support 16KB page sizes. This reduces page faults by 30-40% and boosts heavy VM workload performance.",
            isChecked = true
        )
        SecurityCheckItem(
            title = "Memory Tagging Extension (MTE) Support",
            desc = "Protects guest system resources from buffer overflows and use-after-free security breaches by hardware-tagging memory allocations.",
            isChecked = true
        )
        SecurityCheckItem(
            title = "Hardware Keystore Safe Strongbox",
            desc = "Guest VM secure credentials and files are encrypted using keys generated inside the secure enclave processor of the host.",
            isChecked = true
        )
    }
}

@Composable
fun PageSizeComparisonGraphic() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            "Memory Page Size Comparison (Android 16 Standard)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "16KB memory alignments increase execution speed by decreasing OS translation lookaside buffer (TLB) misses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Draw comparing blocks
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val width = size.width
            val height = size.height

            // 4KB Pages Row (Legacy)
            val legacyRowY = 10f
            val legacyHeight = 40f
            
            // Draw 4KB text
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(0f, legacyRowY),
                size = Size(width, legacyHeight)
            )

            // Draw boundaries of 4KB columns (many small boxes)
            val legacyPagesCount = 12
            val legacyPageWidth = width / legacyPagesCount
            for (i in 0..legacyPagesCount) {
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = 0.4f),
                    start = Offset(i * legacyPageWidth, legacyRowY),
                    end = Offset(i * legacyPageWidth, legacyRowY + legacyHeight),
                    strokeWidth = 2f
                )
            }

            // 16KB Pages Row (Android 16 Modern)
            val modernRowY = 70f
            val modernHeight = 40f
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(0f, modernRowY),
                size = Size(width, modernHeight)
            )

            // Draw boundaries of 16KB columns (fewer, larger boxes)
            val modernPagesCount = 3
            val modernPageWidth = width / modernPagesCount
            for (i in 0..modernPagesCount) {
                // Fill rectangles
                if (i < modernPagesCount) {
                    drawRect(
                        color = Color(0xFF6366F1).copy(alpha = 0.1f * (i + 1)),
                        topLeft = Offset(i * modernPageWidth, modernRowY),
                        size = Size(modernPageWidth, modernHeight)
                    )
                }

                drawLine(
                    color = Color(0xFF6366F1),
                    start = Offset(i * modernPageWidth, modernRowY),
                    end = Offset(i * modernPageWidth, modernRowY + modernHeight),
                    strokeWidth = 3f
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Legacy 4KB Pages (12 Reads / Fault Overhead)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444), fontSize = 9.sp)
            Text("Modern 16KB Pages (3 Large Aligned Reads)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6366F1), fontSize = 9.sp)
        }
    }
}

@Composable
fun DiagnosticSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SecurityCheckItem(title: String, desc: String, isChecked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = "Status",
            tint = if (isChecked) Color(0xFF00E676) else Color(0xFFFFD600),
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

// ==========================================
// SCREEN 3: RUNNING VM WORKSPACE (EMULATION CONSOLE)
// ==========================================

@Composable
fun WorkspaceScreen(
    vm: VirtualMachine,
    onBackToDashboard: () -> Unit,
    onUpdateVmState: (VirtualMachine) -> Unit
) {
    var activeAppId by remember { mutableStateOf<String?>(null) } // null = Launcher Home Screen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        // Workspace Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (activeAppId != null) {
                            activeAppId = null // Return to virtual launcher home
                        } else {
                            onBackToDashboard()
                        }
                    },
                    modifier = Modifier.testTag("workspace_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = vm.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                    }
                    Text(
                        text = if (activeAppId != null) "Guest System / ${activeAppId!!.replace("app_", "").uppercase()}" else "Guest System / HOME LAUNCHER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Mock Device Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = vm.carrier,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (vm.isCharging) "⚡" else "🔋",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${vm.batteryLevel}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Main Virtual Display Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF050505))
        ) {
            AnimatedContent(
                targetState = activeAppId,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "VirtualAppTransition"
            ) { appId ->
                when (appId) {
                    null -> {
                        VirtualLauncherHome(
                            vm = vm,
                            onAppLaunch = { id -> activeAppId = id }
                        )
                    }
                    "app_browser" -> {
                        VirtualBrowserApp()
                    }
                    "app_terminal" -> {
                        VirtualTerminalApp(vm = vm)
                    }
                    "app_files" -> {
                        VirtualFilesApp()
                    }
                    "app_game" -> {
                        VirtualBrickBreakerApp()
                    }
                    "app_settings" -> {
                        VirtualSettingsApp(vm = vm, onUpdate = onUpdateVmState)
                    }
                }
            }
        }

        // Virtual Soft Keys (Android Navigation Bar simulation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back key
                IconButton(onClick = { activeAppId = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Virtual Back", tint = Color.LightGray)
                }

                // Home key
                IconButton(onClick = { activeAppId = null }) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(2.dp, Color.LightGray, CircleShape)
                    )
                }

                // Recents key
                IconButton(onClick = { /* Just ripples to simulate */ }) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(2.dp, Color.LightGray, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

// ==========================================
// MOCK WORKSPACE COMPONENT: HOME LAUNCHER
// ==========================================

@Composable
fun VirtualLauncherHome(
    vm: VirtualMachine,
    onAppLaunch: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter apps based on search
    val filteredApps = vm.installedApps.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Dynamic decorative space nebula background inside the guest OS using slate-indigo theme
                val spaceBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF050505)),
                    center = Offset(size.width / 2, size.height / 3),
                    radius = size.width
                )
                drawRect(spaceBrush)
            }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Search / App Drawer Header
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search virtual applications...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guest_app_search")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Grid
            Text(
                "INSTALLED SYSTEM CONTAINERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredApps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAppLaunch(app.id) }
                            .padding(8.dp)
                            .testTag("launch_guest_app_${app.id}")
                    ) {
                        // Styled app icon using emoji and circle backing
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF0F172A), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(app.iconEmoji, fontSize = 28.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = app.name.replace(Regex("[^a-zA-Z0-9 ]"), "").trim(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "${app.sizeMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }
            }
            
            // Decorative Cosmic Wallpaper Watermark
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🤖",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "CosVM Isolated Environment v16.0",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// VIRTUAL APP 1: TERMINAL WITH WORKING SHELL
// ==========================================

@Composable
fun VirtualTerminalApp(vm: VirtualMachine) {
    var commandInput by remember { mutableStateOf("") }
    var history by remember {
        mutableStateOf(
            listOf(
                TerminalLine("CosVM Shell Linux kernel 6.12.0-cosvm-arm64 ready."),
                TerminalLine("Type 'help' to fetch a catalog of standard virtualization commands."),
                TerminalLine("")
            )
        )
    }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(12.dp)
    ) {
        // Console history area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(history) { line ->
                if (line.isCommand) {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "cosvm_user_1009@guest:~# ",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = line.text,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White)
                        )
                    }
                } else {
                    Text(
                        text = line.text,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF10B981)),
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Command Entry Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "sh# ",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
            )
            
            BasicTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val cmd = commandInput.trim()
                        if (cmd.isNotEmpty()) {
                            val newHistory = history.toMutableList()
                            newHistory.add(TerminalLine(cmd, isCommand = true))

                            // Execute virtual commands
                            when (cmd.lowercase()) {
                                "help" -> {
                                    newHistory.add(TerminalLine("Standard commands:"))
                                    newHistory.add(TerminalLine("  neofetch      Displays device specs and cosmic banner"))
                                    newHistory.add(TerminalLine("  uname -a      Check running Linux VM kernel detail"))
                                    newHistory.add(TerminalLine("  getprop       Lists system properties configured on start"))
                                    newHistory.add(TerminalLine("  whoami        Exposes active shell UID credentials"))
                                    newHistory.add(TerminalLine("  ls -l         List sandboxed storage files"))
                                    newHistory.add(TerminalLine("  cat [file]    Output text contents of virtual file"))
                                    newHistory.add(TerminalLine("  clear         Purge active console screen state"))
                                }
                                "neofetch" -> {
                                    newHistory.add(TerminalLine("  ______   ______   ______   __   __  __    __ "))
                                    newHistory.add(TerminalLine(" /      \\ /      \\ /      \\ /  | /  |/  |  /  |"))
                                    newHistory.add(TerminalLine("/$$$$$$  |$$$$$$  |$$$$$$  |$$ | $$ |$$ |  $$ |"))
                                    newHistory.add(TerminalLine("$$ |  $$/ $$ |  $$ |$$ \\__$$/ $$ | $$ |$$ |  $$ |"))
                                    newHistory.add(TerminalLine("$$ |      $$ |  $$ |$$      \\ $$ | $$ |$$ |  $$ |"))
                                    newHistory.add(TerminalLine("$$ |  __  $$ |  $$ | $$$$$$  |$$ | $$ |$$ |  $$ |"))
                                    newHistory.add(TerminalLine("$$ \\__/  |$$ \\__/  |/  \\__$$ |$$ \\_$$ |$$ \\__$$ |"))
                                    newHistory.add(TerminalLine("$$    $$/ $$    $$/ $$    $$/ $$   $$/ $$    $$/ "))
                                    newHistory.add(TerminalLine(" $$$$$$/   $$$$$$/   $$$$$$/   $$$$$/   $$$$$$/  "))
                                    newHistory.add(TerminalLine("-----------------------------------------------"))
                                    newHistory.add(TerminalLine("OS: CosVM virtual Android 16.0 (API 36)"))
                                    newHistory.add(TerminalLine("Host Device: ${vm.deviceModel} 64-bit"))
                                    newHistory.add(TerminalLine("Kernel: 6.12.0-cosvm-arm64-v8a"))
                                    newHistory.add(TerminalLine("Memory: Allocated ${vm.ramGb}GB / Available ${vm.ramGb - 1}GB"))
                                    newHistory.add(TerminalLine("Cores: Active ${vm.cpuCores} x virtualization hardware thread pools"))
                                    newHistory.add(TerminalLine("Page Alignment: ${if (vm.isPageSize16k) "16KB (Optimized)" else "4KB (Legacy)"}"))
                                    newHistory.add(TerminalLine("Sandbox: Level 4 Container Encapsulation"))
                                }
                                "uname -a" -> {
                                    newHistory.add(TerminalLine("Linux cosvm-guest-vm 6.12.0-cosvm-arm64 #1 SMP PREEMPT_DYNAMIC UTC 2026 aarch64 GNU/Linux"))
                                }
                                "getprop" -> {
                                    newHistory.add(TerminalLine("[ro.build.version.release]: [16]"))
                                    newHistory.add(TerminalLine("[ro.build.version.sdk]: [36]"))
                                    newHistory.add(TerminalLine("[ro.product.model]: [${vm.deviceModel}]"))
                                    newHistory.add(TerminalLine("[ro.product.cpu.abi]: [arm64-v8a]"))
                                    newHistory.add(TerminalLine("[ro.product.cpu.abilist]: [arm64-v8a,x86_64]"))
                                    newHistory.add(TerminalLine("[ro.cosvm.sandbox.version]: [v1.0.4]"))
                                    newHistory.add(TerminalLine("[ro.cosvm.pagesize.16k]: [${if (vm.isPageSize16k) "true" else "false"}]"))
                                }
                                "whoami" -> {
                                    newHistory.add(TerminalLine("cosvm_user_1009 (uid=10009, isolated_sandbox_process)"))
                                }
                                "ls -l" -> {
                                    newHistory.add(TerminalLine("drwxr-xr-x 2 root root 4096 Jun 30 2026 system/"))
                                    newHistory.add(TerminalLine("drwxrwx--- 3 user sdcard 4096 Jun 30 2026 storage/"))
                                    newHistory.add(TerminalLine("-rw-r---- 1 user user  128 Jun 30 2026 secret_notes.txt"))
                                    newHistory.add(TerminalLine("-rw-r---- 1 user user   64 Jun 30 2026 kernel_log.txt"))
                                }
                                "clear" -> {
                                    newHistory.clear()
                                }
                                "cat secret_notes.txt" -> {
                                    newHistory.add(TerminalLine("--- CONFIDENTIAL CONTAINER STORAGE ---"))
                                    newHistory.add(TerminalLine("Welcome to CosVM. This is a fully isolated filesystem."))
                                    newHistory.add(TerminalLine("Changes made inside this virtual space do not affect your physical hardware!"))
                                    newHistory.add(TerminalLine("Feel safe to test potentially untrusted binaries here."))
                                }
                                "cat kernel_log.txt" -> {
                                    newHistory.add(TerminalLine("[0.000000] Booting Linux kernel version 6.12.0-cosvm-arm64"))
                                    newHistory.add(TerminalLine("[0.001042] CPU0: ARM64-v8a hardware threads linked successfully"))
                                    newHistory.add(TerminalLine("[0.012490] Memory: 16KB Page size alignment established."))
                                }
                                else -> {
                                    if (cmd.startsWith("cat ")) {
                                        newHistory.add(TerminalLine("cat: ${cmd.substring(4)}: No such file or directory"))
                                    } else {
                                        newHistory.add(TerminalLine("sh: command not found: $cmd. Try typing 'help'"))
                                    }
                                }
                            }

                            history = newHistory
                            commandInput = ""
                            
                            // Auto scroll
                            scope.launch {
                                delay(50)
                                listState.animateScrollToItem(newHistory.size - 1)
                            }
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .testTag("terminal_input")
            )
            
            IconButton(
                onClick = {
                    val cmd = "help"
                    val newHistory = history.toMutableList()
                    newHistory.add(TerminalLine(cmd, isCommand = true))
                    newHistory.add(TerminalLine("Type commands like 'neofetch', 'uname -a', 'ls -l', 'getprop'"))
                    history = newHistory
                    scope.launch {
                        delay(50)
                        listState.animateScrollToItem(newHistory.size - 1)
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Help", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ==========================================
// VIRTUAL APP 2: WEBBROWSER (SIMULATION)
// ==========================================

@Composable
fun VirtualBrowserApp() {
    var urlInput by remember { mutableStateOf("https://github.com/cyanmint/twoyi") }
    var currentUrl by remember { mutableStateOf("https://github.com/cyanmint/twoyi") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Address Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE5E9F0))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        textStyle = TextStyle(color = Color.DarkGray, fontSize = 12.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                isLoading = true
                                scope.launch {
                                    delay(800)
                                    currentUrl = urlInput
                                    isLoading = false
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(600)
                        currentUrl = urlInput
                        isLoading = false
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Go", tint = Color.DarkGray)
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF00E5FF),
                trackColor = Color.LightGray
            )
        }

        // Simulating different web views based on URLs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF0F4FA))
                .padding(16.dp)
        ) {
            val formattedUrl = currentUrl.lowercase()
            when {
                formattedUrl.contains("github.com/cyanmint/twoyi") || formattedUrl.contains("twoyi") -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🐙", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("cyanmint / twoyi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("Public GitHub Repository clone", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "A lightweight Android-on-Android container VM launcher app written in Kotlin and Compose.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WebChip(label = "⭐ Star (1.2k)")
                                    WebChip(label = "🍴 Fork (240)")
                                    WebChip(label = "MIT License")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("README.md", style = MaterialTheme.typography.titleSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("# Twoyi Virtual Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Twoyi is an Android container that starts a fully virtual workspace. Running on user-space with near physical performance, it enables dynamic device mock testing and safe app runtime isolates.\n\nNow ported to CosVM with Android 16 and full 64-bit optimizations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌐", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Simulated Web Navigation",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Current Address: $currentUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                urlInput = "https://github.com/cyanmint/twoyi"
                                currentUrl = "https://github.com/cyanmint/twoyi"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A8A))
                        ) {
                            Text("Go to Cyanmint Twoyi GitHub", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE5E9F0))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// VIRTUAL APP 3: SECURE FILES EXPLORER
// ==========================================

@Composable
fun VirtualFilesApp() {
    val files = listOf(
        VirtualFile("system/", "system/", 4096, true),
        VirtualFile("storage/", "storage/", 4096, true),
        VirtualFile("secret_notes.txt", "secret_notes.txt", 340, false, "COSVM CONFIDENTIAL WORKSPACE REPORT\n---\nDevice Architecture Verified: arm64-v8a\nSELinux: strict enforcing\nSecure isolated namespaces: active.\nThis file cannot be accessed outside the CosVM sandboxed container."),
        VirtualFile("guest_config.json", "guest_config.json", 112, false, "{\n  \"emulated_version\": 16,\n  \"api_level\": 36,\n  \"page_size_alignment\": \"16KB\",\n  \"secure_storage_keystore\": true\n}")
    )

    var selectedFile by remember { mutableStateOf<VirtualFile?>(null) }

    if (selectedFile != null) {
        // View file details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E17))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📄 ${selectedFile!!.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { selectedFile = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111726))
                    .border(1.dp, Color(0xFF233151), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedFile!!.content,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF00E676)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Files list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E17))
                .padding(16.dp)
        ) {
            Text(
                "Sandbox Secure File Storage",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Isolated guest user files safely segregated from physical disk storage.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111726))
                            .clickable {
                                if (!file.isDirectory) {
                                    selectedFile = file
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (file.isDirectory) "📁" else "📄",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (file.isDirectory) "Directory" else "${file.sizeBytes} bytes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        if (!file.isDirectory) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIRTUAL APP 4: PLAYABLE RETRO CANVAS GAME
// ==========================================

@Composable
fun VirtualBrickBreakerApp() {
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }

    // Dimensions of game elements
    val canvasWidth = 320f
    val canvasHeight = 400f

    var ballX by remember { mutableStateOf(160f) }
    var ballY by remember { mutableStateOf(300f) }
    var ballSpeedX by remember { mutableStateOf(4f) }
    var ballSpeedY by remember { mutableStateOf(-6f) }

    var paddleX by remember { mutableStateOf(120f) }
    val paddleWidth = 80f
    val paddleHeight = 12f

    // Bricks list representation (x, y, active)
    var bricks by remember {
        mutableStateOf(
            listOf(
                // Row 1
                mutableStateOf(true), mutableStateOf(true), mutableStateOf(true), mutableStateOf(true), mutableStateOf(true),
                // Row 2
                mutableStateOf(true), mutableStateOf(true), mutableStateOf(true), mutableStateOf(true), mutableStateOf(true),
                // Row 3
                mutableStateOf(true), mutableStateOf(true), mutableStateOf(true), mutableStateOf(true), mutableStateOf(true)
            )
        )
    }

    // Main Game Engine Loop
    LaunchedEffect(gameStarted, gameOver) {
        if (gameStarted && !gameOver) {
            while (true) {
                delay(16) // Approx 60 frames per second

                // Move ball
                ballX += ballSpeedX
                ballY += ballSpeedY

                // Wall collisions
                if (ballX <= 8f || ballX >= canvasWidth - 8f) {
                    ballSpeedX = -ballSpeedX
                }
                if (ballY <= 8f) {
                    ballSpeedY = -ballSpeedY
                }

                // Paddle collision
                if (ballY >= canvasHeight - paddleHeight - 24f &&
                    ballY <= canvasHeight - 24f &&
                    ballX >= paddleX &&
                    ballX <= paddleX + paddleWidth
                ) {
                    ballSpeedY = -Math.abs(ballSpeedY)
                    // Change angle slightly based on where the ball hits the paddle
                    val hitPos = (ballX - paddleX) / paddleWidth
                    ballSpeedX = (hitPos - 0.5f) * 10f
                }

                // Floor collision (Game Over)
                if (ballY >= canvasHeight) {
                    gameOver = true
                    gameStarted = false
                }

                // Bricks collision check
                val brickCols = 5
                val brickRows = 3
                val brickWidth = (canvasWidth - 12f) / brickCols
                val brickHeight = 20f

                for (r in 0 until brickRows) {
                    for (c in 0 until brickCols) {
                        val index = r * brickCols + c
                        if (index < bricks.size && bricks[index].value) {
                            val bX = 6f + c * brickWidth
                            val bY = 20f + r * brickHeight

                            // Check collision with brick bounding box
                            if (ballX >= bX && ballX <= bX + brickWidth &&
                                ballY >= bY && ballY <= bY + brickHeight
                            ) {
                                bricks[index].value = false
                                ballSpeedY = -ballSpeedY
                                score += 10
                            }
                        }
                    }
                }

                // Check Win
                if (bricks.none { it.value }) {
                    gameOver = true
                    gameStarted = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Cosmic Retro BrickBreaker",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
        Text(
            "Running high-performance 60fps virtualization graphics",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Display Score
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Score: $score", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
            
            Button(
                onClick = {
                    // Reset game
                    ballX = 160f
                    ballY = 300f
                    ballSpeedX = 4f
                    ballSpeedY = -6f
                    paddleX = 120f
                    score = 0
                    bricks.forEach { it.value = true }
                    gameOver = false
                    gameStarted = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (gameOver) "Restart" else if (gameStarted) "Reset" else "Start", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Play Canvas Frame
        Box(
            modifier = Modifier
                .size(canvasWidth.dp, canvasHeight.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111726))
                .border(2.dp, Color(0xFF233151), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        paddleX = (paddleX + dragAmount.x).coerceIn(0f, canvasWidth - paddleWidth)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val brickCols = 5
                val brickRows = 3
                val brickW = (size.width - 12f) / brickCols
                val brickH = 20f

                // Draw bricks
                for (r in 0 until brickRows) {
                    for (c in 0 until brickCols) {
                        val index = r * brickCols + c
                        if (index < bricks.size && bricks[index].value) {
                            val color = when (r) {
                                0 -> Color(0xFFFF2E93)
                                1 -> Color(0xFF8A2BE2)
                                else -> Color(0xFF00E5FF)
                            }
                            drawRect(
                                color = color,
                                topLeft = Offset(6f + c * brickW, 20f + r * brickH),
                                size = Size(brickW - 4f, brickH - 4f)
                            )
                        }
                    }
                }

                // Draw Paddle
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(paddleX, size.height - 24f),
                    size = Size(paddleWidth, paddleHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Draw Ball
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 8f,
                    center = Offset(ballX, ballY)
                )
            }

            if (gameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (bricks.none { !it.value }) "🎉 YOU WIN!" else "💀 GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Final score: $score", color = Color.LightGray)
                    }
                }
            } else if (!gameStarted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Drag to slide paddle.\nPress 'Start' above.", color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ==========================================
// VIRTUAL APP 5: CORE VM CONTROLS & SETTINGS
// ==========================================

@Composable
fun VirtualSettingsApp(
    vm: VirtualMachine,
    onUpdate: (VirtualMachine) -> Unit
) {
    var batteryInput by remember { mutableStateOf(vm.batteryLevel) }
    var chargingState by remember { mutableStateOf(vm.isCharging) }
    var rootEnabled by remember { mutableStateOf(vm.isRooted) }
    var carrierInput by remember { mutableStateOf(vm.carrier) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "CosVM Core Virtual Controls",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tweak emulated sensors, network signals, and security protocols in real time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Emulated Hardware controls
        Text("EMULATED HARDWARE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Battery Level Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Battery Power level", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("$batteryInput%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Slider(
                    value = batteryInput.toFloat(),
                    onValueChange = {
                        batteryInput = it.toInt()
                        onUpdate(vm.copy(batteryLevel = batteryInput))
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6366F1),
                        activeTrackColor = Color(0xFF6366F1)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Is Charging Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plugged / Charging Mode", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Switch(
                        checked = chargingState,
                        onCheckedChange = {
                            chargingState = it
                            onUpdate(vm.copy(isCharging = chargingState))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Network emulation settings
        Text("SIMULATED TELECOM", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Simulated Operator Carrier", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = carrierInput,
                    onValueChange = {
                        carrierInput = it
                        onUpdate(vm.copy(carrier = carrierInput))
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedIndicatorColor = Color(0xFF6366F1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Virtual Security sandbox settings
        Text("SECURITY PRIVILEGES", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Root SU Privilege (su)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("Enables full superuser tools inside the guest sandbox namespace.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(
                        checked = rootEnabled,
                        onCheckedChange = {
                            rootEnabled = it
                            onUpdate(vm.copy(isRooted = rootEnabled))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1))
                    )
                }
            }
        }
    }
}

// ==========================================
// UTILITY CREATION DIALOG (NEW VM BUILDER)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVmDialog(
    onDismiss: () -> Unit,
    onCreate: (VirtualMachine) -> Unit
) {
    var name by remember { mutableStateOf("Android 16 Core") }
    var cpuCores by remember { mutableStateOf(4) }
    var ramGb by remember { mutableStateOf(6) }
    var is16kAligned by remember { mutableStateOf(true) }
    var selectedDevice by remember { mutableStateOf("CosVM Pro 64-bit") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Configure New Virtual Sandbox",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Name input
                Text("Container Instance Name", style = MaterialTheme.typography.bodySmall, color = CosmicTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CosmicSurfaceVariant,
                        unfocusedContainerColor = CosmicSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_vm_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CPU cores selection
                Text("Allocated CPU Cores: $cpuCores", style = MaterialTheme.typography.bodySmall, color = CosmicTextSecondary)
                Slider(
                    value = cpuCores.toFloat(),
                    onValueChange = { cpuCores = it.toInt() },
                    valueRange = 2f..16f,
                    steps = 13,
                    colors = SliderDefaults.colors(thumbColor = CosmicPrimary, activeTrackColor = CosmicPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // RAM selection
                Text("Allocated RAM: $ramGb GB", style = MaterialTheme.typography.bodySmall, color = CosmicTextSecondary)
                Slider(
                    value = ramGb.toFloat(),
                    onValueChange = { ramGb = it.toInt() },
                    valueRange = 2f..16f,
                    steps = 13,
                    colors = SliderDefaults.colors(thumbColor = CosmicPrimary, activeTrackColor = CosmicPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Modern features
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("16KB Page Alignment", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("Optimal execution speed on ARM64 & x86_64 host chips.", style = MaterialTheme.typography.labelSmall, color = CosmicTextSecondary)
                    }
                    Switch(
                        checked = is16kAligned,
                        onCheckedChange = { is16kAligned = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CosmicPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = CosmicTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newVm = VirtualMachine(
                                id = "vm_" + UUID.randomUUID().toString().take(6),
                                name = name,
                                androidVersion = "Android 16 (API 36)",
                                status = VMStatus.STOPPED,
                                cpuCores = cpuCores,
                                ramGb = ramGb,
                                resolution = "1080x2400",
                                densityDpi = 440,
                                deviceModel = selectedDevice,
                                isPageSize16k = is16kAligned,
                                installedApps = listOf(
                                    VirtualApp("app_browser", "🌐 Web Browser", "com.cosvm.browser", "🌐", 18, true, 26, true),
                                    VirtualApp("app_terminal", "💻 Safe Terminal", "com.cosvm.terminal", "💻", 4, true, 26, true),
                                    VirtualApp("app_files", "📁 Sandbox Files", "com.cosvm.files", "📁", 8, true, 26, true),
                                    VirtualApp("app_game", "🎮 Retro BrickBreaker", "com.cosvm.brickbreaker", "🎮", 12, true, 26, true),
                                    VirtualApp("app_settings", "⚙️ VM Settings", "com.cosvm.settings", "⚙️", 6, true, 26, true)
                                )
                            )
                            onCreate(newVm)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                        modifier = Modifier.testTag("submit_create_vm")
                    ) {
                        Text("Instantiate", color = CosmicOnPrimary)
                    }
                }
            }
        }
    }
}
