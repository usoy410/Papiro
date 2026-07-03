import re

with open('app/src/main/java/com/usoy/papiro/ui/components/DrawingComponents.kt', 'r') as f:
    content = f.read()

# We need to find `fun BlueprintDoodle(modifier: Modifier = Modifier) {` and replace everything from there until `@OptIn(ExperimentalMaterial3Api::class)`

new_doodles = """@Composable
fun BlueprintDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.1f, size.height), Offset(size.width * 0.1f, size.height * 0.4f), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.9f, size.height), Offset(size.width * 0.9f, size.height * 0.4f), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.1f, size.height * 0.4f), Offset(size.width / 2f, size.height * 0.1f), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.9f, size.height * 0.4f), Offset(size.width / 2f, size.height * 0.1f), 2f)
    }
}

@Composable
fun TableOfContentDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            // Header line
            moveTo(size.width * 0.1f, size.height * 0.15f)
            lineTo(size.width * 0.7f, size.height * 0.15f)
            
            // First item (bullet + text + page)
            moveTo(size.width * 0.1f, size.height * 0.4f)
            lineTo(size.width * 0.2f, size.height * 0.4f)
            moveTo(size.width * 0.3f, size.height * 0.4f)
            lineTo(size.width * 0.75f, size.height * 0.4f)
            moveTo(size.width * 0.85f, size.height * 0.4f)
            lineTo(size.width * 0.95f, size.height * 0.4f)
            
            // Second item
            moveTo(size.width * 0.1f, size.height * 0.65f)
            lineTo(size.width * 0.2f, size.height * 0.65f)
            moveTo(size.width * 0.3f, size.height * 0.65f)
            lineTo(size.width * 0.7f, size.height * 0.65f)
            moveTo(size.width * 0.85f, size.height * 0.65f)
            lineTo(size.width * 0.95f, size.height * 0.65f)
            
            // Third item
            moveTo(size.width * 0.1f, size.height * 0.9f)
            lineTo(size.width * 0.2f, size.height * 0.9f)
            moveTo(size.width * 0.3f, size.height * 0.9f)
            lineTo(size.width * 0.8f, size.height * 0.9f)
            moveTo(size.width * 0.9f, size.height * 0.9f)
            lineTo(size.width * 0.95f, size.height * 0.9f)
        }
        drawPath(path, Color.Gray, style = Stroke(2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
fun TableDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            // Outer rectangle
            moveTo(size.width * 0.05f, size.height * 0.15f)
            lineTo(size.width * 0.95f, size.height * 0.15f)
            lineTo(size.width * 0.95f, size.height * 0.85f)
            lineTo(size.width * 0.05f, size.height * 0.85f)
            close()
            
            // Header separator
            moveTo(size.width * 0.05f, size.height * 0.35f)
            lineTo(size.width * 0.95f, size.height * 0.35f)
            
            // Row separator
            moveTo(size.width * 0.05f, size.height * 0.6f)
            lineTo(size.width * 0.95f, size.height * 0.6f)
            
            // Column separator
            moveTo(size.width * 0.35f, size.height * 0.15f)
            lineTo(size.width * 0.35f, size.height * 0.85f)
            
            moveTo(size.width * 0.65f, size.height * 0.15f)
            lineTo(size.width * 0.65f, size.height * 0.85f)
        }
        drawPath(path, Color.Gray, style = Stroke(2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

"""

# Regex to find the broken part and replace it
content = re.sub(r'fun BlueprintDoodle\(modifier: Modifier = Modifier\) \{.*?@OptIn\(ExperimentalMaterial3Api::class\)', new_doodles + '@OptIn(ExperimentalMaterial3Api::class)', content, flags=re.DOTALL)

with open('app/src/main/java/com/usoy/papiro/ui/components/DrawingComponents.kt', 'w') as f:
    f.write(content)
