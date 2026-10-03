package com.salehtl.dubainownext.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle

/** Temporary placeholder used until a feature's real screen lands. */
@Composable
fun StubScreen(title: String, pushed: Boolean = true) {
    Column(Modifier.fillMaxSize()) {
        if (!LocalInSheet.current) { if (pushed) PushedHeader() else MainHeader() }
        Column(Modifier.fillMaxSize().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = DNTextStyle.Title2.style, color = DNColor.textPrimary)
            Text("Being built", style = DNTextStyle.Body.style, color = DNColor.textSecondary)
        }
    }
}
