package com.zhihuiji.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuiji.core.designsystem.GlassBorder
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceMedium
import com.zhihuiji.core.designsystem.LiquidGlassSurface
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.TextTertiary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary
import com.zhihuiji.feature.products.ProductListScreen
import com.zhihuiji.feature.customers.CustomerListScreen
import com.zhihuiji.feature.suppliers.SupplierListScreen

@Composable
fun ArchivesScreen(
    initialTab: Int = 0,
    onNavigateToProductDetail: (Long) -> Unit,
    onNavigateToProductCreate: () -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit,
    onNavigateToCustomerCreate: () -> Unit,
    onNavigateToSupplierDetail: (Long) -> Unit,
    onNavigateToSupplierCreate: () -> Unit,
) {
    val tabs = listOf("商品", "客户", "供应商")
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(tabs.indices)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val onCreateClick = when (selectedTab) {
        0 -> onNavigateToProductCreate
        1 -> onNavigateToCustomerCreate
        else -> onNavigateToSupplierCreate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArchivesTopBar(onCreateClick = onCreateClick)
        ArchivesSearchAndTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            searchQuery = searchQuery,
            searchHint = when (selectedTab) {
                0 -> "搜索商品名称、编码..."
                1 -> "搜索客户名称、手机号..."
                else -> "搜索供应商名称、联系人..."
            },
            onSearchChange = { searchQuery = it },
            onTabSelected = { selectedTab = it },
        )
        when (selectedTab) {
            0 -> ProductListScreen(
                onNavigateToDetail = onNavigateToProductDetail,
                searchQuery = searchQuery
            )
            1 -> CustomerListScreen(
                onNavigateToDetail = onNavigateToCustomerDetail,
                searchQuery = searchQuery
            )
            2 -> SupplierListScreen(
                onNavigateToDetail = onNavigateToSupplierDetail,
                searchQuery = searchQuery
            )
        }
    }
}

@Composable
private fun ArchivesTopBar(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        surfaceColor = GlassSurfaceMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "档案管理",
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            IconButton(onClick = onCreateClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增档案",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ArchivesSearchAndTabs(
    tabs: List<String>,
    selectedIndex: Int,
    searchQuery: String,
    searchHint: String,
    onSearchChange: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        text = searchHint,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GlassSurfaceHigh,
                    unfocusedContainerColor = GlassSurfaceHigh,
                    disabledContainerColor = GlassSurfaceHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ZhihuijiPrimary
                )
            )
            LiquidGlassSurface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { },
                shape = RoundedCornerShape(12.dp),
                surfaceColor = GlassSurfaceHigh
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "扫码",
                        tint = ZhihuijiPrimary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (selected) ZhihuijiPrimary else GlassSurfaceMedium)
                        .border(0.5.dp, if (selected) Color.Transparent else GlassBorder, RoundedCornerShape(100.dp))
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}
