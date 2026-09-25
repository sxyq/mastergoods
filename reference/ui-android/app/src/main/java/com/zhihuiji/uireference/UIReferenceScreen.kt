package com.zhihuiji.uireference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.designsystem.*

/**
 * 历史 APP 视觉语言参考页：单页滚动展示旧工程 designsystem 的全部核心组件。
 * 仅用于风格参考，纯静态示例，不包含任何业务逻辑、网络或数据层代码。
 */
@Composable
fun UIReferenceScreen() {
    GlassScaffold(
        topBar = {
            GlassTopBar(
                title = "智慧记 UI Reference",
                subtitle = "历史视觉语言参考 · 非生产代码",
                onNavigationClick = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "筛选",
                            tint = TextPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                primaryText = "新建单据",
                onPrimaryClick = {},
                secondaryText = "取消",
                totalAmount = "¥1,286.00",
                totalLabel = "合计（示例）"
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 200.dp)
        ) {
            ColorSection()
            TypographySection()
            SpacingShapeSection()
            GlassSurfaceSection()
            TopBarNoteSection()
            ButtonSection()
            TextFieldSection()
            StatusPillSection()
            FilterTabsSection()
            KpiSection()
            ListItemSection()
            QuantityStepperSection()
            EmptyStateSection()
            BottomActionBarNoteSection()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = ZhihuijiPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerLight.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

private val referenceColors = listOf(
    "ZhihuijiPrimary" to ZhihuijiPrimary,
    "PrimaryLight" to ZhihuijiPrimaryLight,
    "PrimaryDark" to ZhihuijiPrimaryDark,
    "PrimaryBright" to ZhihuijiPrimaryBright,
    "SuccessGreen" to SuccessGreen,
    "WarningOrange" to WarningOrange,
    "DangerRed" to DangerRed,
    "InfoBlue" to InfoBlue,
    "TextPrimary" to TextPrimary,
    "TextSecondary" to TextSecondary,
    "TextTertiary" to TextTertiary,
    "TextQuaternary" to TextQuaternary,
    "BackgroundLight" to BackgroundLight,
    "SurfaceWhite" to SurfaceWhite,
    "SurfaceGray" to SurfaceGray,
    "SurfaceSoft" to SurfaceSoft,
    "DividerLight" to DividerLight,
    "GlassSurfaceLow" to GlassSurfaceLow,
    "GlassSurfaceMedium" to GlassSurfaceMedium,
    "GlassSurfaceHigh" to GlassSurfaceHigh,
    "GlassBorder" to GlassBorder,
    "AuroraBlue" to AuroraBlue,
    "AuroraCyan" to AuroraCyan,
    "AuroraIndigo" to AuroraIndigo
)

@Composable
private fun ColorSection() {
    Section("1. Theme · Color 色板") {
        referenceColors.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (name, color) ->
                    ColorSwatch(name = name, color = color)
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ColorSwatch(
    name: String,
    color: Color
) {
    Column(modifier = Modifier.weight(1f)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(color, RoundedCornerShape(8.dp))
                .border(0.5.dp, DividerLight, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun TypographySection() {
    Section("2. Theme · Typography 字体层级") {
        val styles = listOf(
            "displayMedium" to MaterialTheme.typography.displayMedium,
            "headlineLarge" to MaterialTheme.typography.headlineLarge,
            "headlineMedium" to MaterialTheme.typography.headlineMedium,
            "titleLarge" to MaterialTheme.typography.titleLarge,
            "titleMedium" to MaterialTheme.typography.titleMedium,
            "bodyLarge" to MaterialTheme.typography.bodyLarge,
            "bodyMedium" to MaterialTheme.typography.bodyMedium,
            "labelLarge" to MaterialTheme.typography.labelLarge,
            "labelMedium" to MaterialTheme.typography.labelMedium,
            "labelSmall" to MaterialTheme.typography.labelSmall
        )
        styles.forEach { (name, style) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.width(120.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "智慧记 ¥1,286.00",
                    style = style,
                    color = TextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "金额专用样式",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "¥12,860.00", style = AmountHeroTextStyle, color = ZhihuijiPrimary)
        Text(text = "¥1,286.00", style = AmountTextStyle, color = TextPrimary)
        Text(text = "¥320.00", style = AmountSmallTextStyle, color = TextSecondary)
    }
}

@Composable
private fun SpacingShapeSection() {
    Section("3. Theme · Spacing 间距 / Shape 形状") {
        listOf(4.dp, 8.dp, 12.dp, 16.dp, 24.dp, 32.dp).forEach { space ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(space)
                        .size(12.dp)
                        .background(ZhihuijiPrimary, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "padding $space（MainBottomBarHeight = ${MainBottomBarHeight}）",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val shapes = listOf(
            "extraSmall 4dp" to ZhihuijiShapes.extraSmall,
            "small 8dp" to ZhihuijiShapes.small,
            "medium 12dp" to ZhihuijiShapes.medium,
            "roundedCardShape 12dp" to roundedCardShape,
            "large 16dp" to ZhihuijiShapes.large,
            "extraLarge 24dp" to ZhihuijiShapes.extraLarge
        )
        shapes.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (name, shape) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(SurfaceWhite, shape)
                                .border(1.dp, ZhihuijiPrimary.copy(alpha = 0.35f), shape)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GlassSurfaceSection() {
    Section("4. Glass surface · 液态玻璃容器") {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            blurRadius = 20.dp,
            shape = RoundedCornerShape(16.dp),
            surfaceColor = GlassSurfaceMedium
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LiquidGlassSurface",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LiquidGlassCard(
                modifier = Modifier.weight(1f),
                surfaceColor = GlassSurfaceLow
            ) {
                Text(
                    text = "LiquidGlassCard · Low",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
            LiquidGlassCard(
                modifier = Modifier.weight(1f),
                surfaceColor = GlassSurfaceHigh
            ) {
                Text(
                    text = "LiquidGlassCard · High",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TopBarNoteSection() {
    Section("5. Top bar · 玻璃顶栏（固定于屏幕顶部）") {
        Text(
            text = "GlassTopBar 是各页面顶栏母版：LiquidGlassSurface(blurRadius = 24.dp) + 状态栏占位，" +
                "支持 title / subtitle / largeTitle / navigationIcon / actions 插槽。" +
                "本页通过 GlassScaffold.topBar 固定展示，滚动时始终可见。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ButtonSection() {
    Section("6. Buttons · 主按钮 / 次按钮 / 危险按钮") {
        PrimaryButton(text = "PrimaryButton 主按钮", onClick = {})
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryOutlineButton(text = "SecondaryOutlineButton 次按钮", onClick = {})
        Spacer(modifier = Modifier.height(12.dp))
        DangerOutlineButton(text = "DangerOutlineButton 危险按钮", onClick = {})
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(text = "PrimaryButton（disabled）", onClick = {}, enabled = false)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryOutlineButton(text = "SecondaryOutlineButton（disabled）", onClick = {}, enabled = false)
    }
}

@Composable
private fun TextFieldSection() {
    Section("7. TextField · GlassTextField") {
        var text by remember { mutableStateOf("山泉矿泉水 550ml") }
        GlassTextField(
            value = text,
            onValueChange = { text = it },
            label = "商品名称",
            placeholder = "请输入商品名称",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatusPillSection() {
    Section("8. Status pill · 状态胶囊（7 种状态色）") {
        val pills = listOf(
            "正常" to StatusType.NORMAL,
            "低库存" to StatusType.LOW_STOCK,
            "缺货" to StatusType.OUT_OF_STOCK,
            "待收款" to StatusType.PENDING,
            "已完成" to StatusType.COMPLETED,
            "已归档" to StatusType.ARCHIVED,
            "作废" to StatusType.CANCELLED
        )
        pills.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (label, status) ->
                    StatusPill(text = label, status = status)
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FilterTabsSection() {
    Section("9. Filter chip / Tabs · 筛选胶囊与分段标签") {
        var selectedChipId by remember { mutableStateOf("all") }
        val chips = listOf(
            FilterChip("all", "全部"),
            FilterChip("new", "待开单"),
            FilterChip("pending", "待审核"),
            FilterChip("done", "已完成"),
            FilterChip("archived", "已归档")
        )
        FilterChipRow(
            chips = chips.map { it.copy(selected = it.id == selectedChipId) },
            onChipClick = { clicked -> selectedChipId = clicked.id }
        )
        Spacer(modifier = Modifier.height(8.dp))
        var tabIndex by remember { mutableIntStateOf(0) }
        SegmentedTabs(
            tabs = listOf("全部", "待付款", "已完成"),
            selectedIndex = tabIndex,
            onTabSelected = { tabIndex = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KpiSection() {
    Section("10. KPI card · 指标卡") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "今日销售额",
                value = "¥12,860.00",
                changePercent = "+12.4%",
                isPositive = true
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "本月毛利",
                value = "¥4,203.50",
                changePercent = "-3.1%",
                isPositive = false
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        KpiCard(
            title = "库存周转",
            value = "1,286 件",
            changePercent = null
        )
    }
}

@Composable
private fun ListItemSection() {
    Section("11. List item · 列表项") {
        BusinessListItem(
            title = "销售单 XS20260925001",
            subtitle = "客户：杭州云启贸易 · 3 件商品",
            trailing = "¥2,580.00",
            statusPill = { StatusPill(text = "待收款", status = StatusType.PENDING) },
            onClick = {}
        )
        Spacer(modifier = Modifier.height(10.dp))
        BusinessListItem(
            title = "采购单 CG20260925007",
            subtitle = "供应商：明辉食品批发 · 12 件商品",
            trailing = "¥8,420.00",
            statusPill = { StatusPill(text = "已完成", status = StatusType.COMPLETED) },
            onClick = {}
        )
        Spacer(modifier = Modifier.height(10.dp))
        DocumentListCard(
            title = "销售单 XS20260924015",
            subtitle = "客户：森屿便利店",
            meta = "09-24 16:20 · 5 件商品",
            amount = "¥1,145.00",
            statusLabel = "待发货",
            statusTone = DocumentStatusTone.PRIMARY
        )
        Spacer(modifier = Modifier.height(10.dp))
        DocumentListCard(
            title = "退货单 TH20260924002",
            subtitle = "客户：杭州云启贸易",
            meta = "09-24 11:05 · 2 件商品",
            amount = "-¥320.00",
            amountColor = DangerRed,
            statusLabel = "已退款",
            statusTone = DocumentStatusTone.DANGER
        )
    }
}

@Composable
private fun QuantityStepperSection() {
    Section("12. Quantity stepper · 数量步进器") {
        var quantity by remember { mutableIntStateOf(3) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuantityStepper(
                value = quantity,
                minValue = 1,
                maxValue = 99,
                onValueChange = { quantity = it }
            )
            Text(
                text = "$quantity 件",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun EmptyStateSection() {
    Section("13. Empty state · 空状态") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            EmptyState(
                icon = Icons.Outlined.Inbox,
                title = "暂无销售单",
                subtitle = "当前筛选条件下没有数据",
                action = {
                    PrimaryButton(
                        modifier = Modifier.width(180.dp),
                        text = "新建销售单",
                        onClick = {}
                    )
                }
            )
        }
    }
}

@Composable
private fun BottomActionBarNoteSection() {
    Section("14. Bottom action bar · 底部操作区（固定于屏幕底部）") {
        Text(
            text = "BottomActionBar：合计金额行 + 次按钮 / 主按钮 + 导航栏 inset，支持 dangerMode 危险模式。" +
                "本页通过 GlassScaffold.bottomBar 固定展示，滚动时始终可见。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
