import re

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('androidx.compose.material.icons.Icons.Default.Description', 'androidx.compose.material.icons.filled.Description')

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteListScreen.kt', 'w') as f:
    f.write(content)
