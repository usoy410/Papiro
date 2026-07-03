import re

with open('app/src/main/java/com/usoy/papiro/ui/components/MarkdownRenderer.kt', 'r') as f:
    content = f.read()

# Replace Edit mode TextBlock
edit_search = """                                    Box(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(vertical = 4.dp)) {
                                        BasicTextField("""

edit_replace = """                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        if (textValue.text.contains("Table of Contents", ignoreCase = true)) {
                                            Box(
                                                modifier = Modifier
                                                    .width(startPadding)
                                                    .padding(top = 16.dp),
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                TableOfContentDoodle()
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(startPadding))
                                        }
                                        Box(modifier = Modifier.weight(1f).padding(end = endPadding)) {
                                            BasicTextField("""

content = content.replace(edit_search, edit_replace)

# We also need to fix the alignment of IconButton in edit mode.
# Right now it's:
#                                         if (enhanceBlock != null && textValue.text.isNotBlank()) {
#                                             IconButton(
#                                                 onClick = enhanceBlock,
#                                                 modifier = Modifier.align(Alignment.TopEnd).size(24.dp)

icon_search = """                                        if (enhanceBlock != null && textValue.text.isNotBlank()) {
                                            IconButton(
                                                onClick = enhanceBlock,
                                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                            ) {"""

icon_replace = """                                        if (enhanceBlock != null && textValue.text.isNotBlank()) {
                                            IconButton(
                                                onClick = enhanceBlock,
                                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                            ) {"""
# The box we added is Box(modifier = Modifier.weight(1f).padding(end = endPadding)) { ... }
# Wait, let's close the Row after the Box.
# The original code had:
#                                     Box(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(vertical = 4.dp)) {
#                                         BasicTextField(...)
#                                         if (...) { IconButton(...) }
#                                     }
# We changed it to:
#                                     Row(...) {
#                                         Box(doodle/spacer)
#                                         Box(...) {
#                                             BasicTextField(...)
#                                             if (...) { IconButton(...) }
#                                         }
#                                     }
# So the closing brackets remain the same!
# Wait, the closing bracket of the Row matches the closing bracket of the original Box.
# But wait, we added an extra `Box(modifier = Modifier.weight(1f)...)` so we need an extra `}` before the end of the `if (onContentChanged != null)` branch!

# Let's do it with regex to be safer.
