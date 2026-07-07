import re

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteEditorScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    // Load note content on open
    LaunchedEffect(Unit) {
        currentNote?.let { note ->''',
'''    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            snackbarHostState.showSnackbar(event)
        }
    }

    // Load note content on open
    LaunchedEffect(Unit) {
        currentNote?.let { note ->'''
)

content = content.replace(
'''    Scaffold(
        topBar = {''',
'''    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {'''
)

with open('app/src/main/java/com/usoy/papiro/ui/screens/NoteEditorScreen.kt', 'w') as f:
    f.write(content)
