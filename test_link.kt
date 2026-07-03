import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
fun test() {
    buildAnnotatedString {
        withLink(LinkAnnotation.Clickable("tag") { }) {
            append("text")
        }
    }
}
