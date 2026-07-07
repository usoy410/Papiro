import re

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteEditorScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    // Initialize editor fields when a note is loaded''',
'''    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            snackbarHostState.showSnackbar(event)
        }
    }

    // Initialize editor fields when a note is loaded'''
)

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteEditorScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('Icons.Default.Description', 'androidx.compose.material.icons.filled.Description')

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteListScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/usoy/papiro/viewmodel/NoteViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    val uiEvent = _uiEvent.asSharedFlow()''',
'''    val uiEvent = _uiEvent.kotlinx.coroutines.flow.asSharedFlow()'''
)

with open('app/src/main/java/com/usoy/papiro/viewmodel/NoteViewModel.kt', 'w') as f:
    f.write(content)

