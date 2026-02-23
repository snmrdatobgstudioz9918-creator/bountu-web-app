import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStylesheet)
        App()
    }
}

@Composable
fun App() {
    Div({ classes(AppStylesheet.container) }) {
        H1 { Text("Bountu Web") }
        P { Text("Hello from Compose HTML on Kotlin/JS.") }
        Button(attrs = { onClick { window.alert("It works!") } }) { Text("Click me") }
    }
}

object AppStylesheet : StyleSheet() {
    val container by style {
        padding(16.px)
        fontFamily("Inter", "Segoe UI", "Roboto", "Helvetica", "Arial", "sans-serif")
        property("-webkit-font-smoothing", "antialiased")
        property("-moz-osx-font-smoothing", "grayscale")
    }
}
