import re

with open('app/src/main/java/com/usoy/papiro/ui/components/MarkdownRenderer.kt', 'r') as f:
    content = f.read()

# Replace Edit mode TextBlock
edit_search = r'Box\(modifier = Modifier\.fillMaxWidth\(\)\.padding\(contentPadding\)\.padding\(vertical = 4\.dp\)\) \{\s*BasicTextField\('

edit_replace = """Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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

content = re.sub(edit_search, edit_replace, content)


# Now fix the end of the if (onContentChanged != null) block to add the closing brace for the Box inside the Row.
# The original was:
#                                         if (enhanceBlock != null && textValue.text.isNotBlank()) {
#                                             IconButton(...) { ... }
#                                         }
#                                     }
#                                 } else {

icon_end_search = r'(if \(enhanceBlock != null && textValue\.text\.isNotBlank\(\)\) \{\s*IconButton\([\s\S]*?\}\s*\})(\s*\})'
# We need to add one more closing brace for the Box before the closing brace of the Row.
icon_end_replace = r'\1\n                                        }\2'

content = re.sub(icon_end_search, icon_end_replace, content, count=1)


# Now for the view mode:
#                                 } else {
#                                     Box(
#                                         modifier = Modifier
#                                             .fillMaxWidth()
#                                             .padding(contentPadding)
#                                             .padding(vertical = 6.dp)
#                                     ) {
#                                         if (block.text.isEmpty()) {

view_search = r'Box\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.padding\(contentPadding\)\s*\.padding\(vertical = 6\.dp\)\s*\)\s*\{'
view_replace = """Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        if (block.text.contains("Table of Contents", ignoreCase = true)) {
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
                                        Box(modifier = Modifier.weight(1f).padding(end = endPadding)) {"""

content = re.sub(view_search, view_replace, content)

view_end_search = r'(is MarkdownBlock\.HorizontalRule -> HorizontalRuleView\(\)\s*else -> \{\}\s*\}\s*\}\s*\})(\s*\})(\s*\})(\s*\})'
view_end_replace = r'\1\n                                        }\2\3\4'
# Wait, let's just insert one `}` at the very end of the else block.
# Actually, the view mode ends with:
#                                         }
#                                     }
#                                 }
#                             }
#                             is MarkdownBlock.Header, is MarkdownBlock.ListItem, ...
view_end_search2 = r'(// Empty in preview, show nothing\s*\} else \{\s*Column \{\s*val subBlocks = remember\(block\.text\) \{ parseMarkdownElements\(block\.text\) \}[\s\S]*?\}\s*\}\s*\})(\s*\})(\s*\})(\s*\})'
view_end_replace2 = r'\1\n                                        }\2\3\4'

content = re.sub(view_end_search2, view_end_replace2, content, count=1)


with open('app/src/main/java/com/usoy/papiro/ui/components/MarkdownRenderer.kt', 'w') as f:
    f.write(content)
