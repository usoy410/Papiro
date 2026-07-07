import re

with open('app/src/main/java/com/usoy/papiro/viewmodel/NoteViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''import kotlinx.coroutines.flow.StateFlow''',
'''import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow'''
)

content = content.replace(
'''    val uiEvent = _uiEvent.kotlinx.coroutines.flow.asSharedFlow()''',
'''    val uiEvent = _uiEvent.asSharedFlow()'''
)

with open('app/src/main/java/com/usoy/papiro/viewmodel/NoteViewModel.kt', 'w') as f:
    f.write(content)
