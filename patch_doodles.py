import re

with open('app/src/main/java/com/usoy/papiro/ui/components/DrawingComponents.kt', 'r') as f:
    content = f.read()

new_doodles = """@Composable
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
}"""

old_doodle_pattern = re.compile(r'@Composable\s*\nfun TableDoodle.*?\n}', re.DOTALL)
if old_doodle_pattern.search(content):
    content = old_doodle_pattern.sub(new_doodles, content)
else:
    print("Could not find TableDoodle to replace")

with open('app/src/main/java/com/usoy/papiro/ui/components/DrawingComponents.kt', 'w') as f:
    f.write(content)
