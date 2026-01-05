package calebxzhou.rdi.client

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Compose for Desktop calebxzhou.rdi.client.App") {
        App()
    }
}

@Composable
fun App() {
    var count by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Hello, Compose for Desktop!", style = MaterialTheme.typography.h3)
        Button(onClick = { count++ }) {
            Text("Click me: $count")
        }
    }
}
