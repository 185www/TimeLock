package com.timelock.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelock.app.data.AppListProvider

@Composable
fun AppSelectScreen(
    current: Set<String>,
    onBack: () -> Unit,
    onSave: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember { AppListProvider(context).getLaunchableApps() }
    var selected by remember { mutableStateOf(current.toMutableSet()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .padding(16.dp),
    ) {
        Text(
            text = "选择要限制的应用",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Text(
            text = "勾选你希望限制使用时长的应用。打开它们时会先让你设定本次可用时间。",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (apps.isEmpty()) {
                item { Text("没有找到可显示的应用", color = MaterialTheme.colorScheme.onSurface) }
            }
            items(apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toggle(selected, app.packageName) { selected = it } }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selected.contains(app.packageName),
                        onCheckedChange = { toggle(selected, app.packageName) { selected = it } },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = Color.Gray,
                        ),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(app.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                        Text(app.packageName, color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onSave(selected.toSet()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
            ),
        ) {
            Text("完成（已选 ${selected.size} 个）", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2230), contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text("返回")
        }
    }
}

private fun toggle(
    current: MutableSet<String>,
    packageName: String,
    update: (MutableSet<String>) -> Unit,
) {
    val next = current.toMutableSet()
    if (next.contains(packageName)) next.remove(packageName) else next.add(packageName)
    update(next)
}
