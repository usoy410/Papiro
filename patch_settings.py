import re

with open('app/src/main/java/com/usoy/papiro/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    LaunchedEffect(connectionTestMessage) {
        connectionTestMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            connectionTestMessage = null
        }
    }''',
'''    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(connectionTestMessage) {
        connectionTestMessage?.let {
            snackbarHostState.showSnackbar(it)
            connectionTestMessage = null
        }
    }'''
)

content = content.replace(
'''    Scaffold(
        topBar = {''',
'''    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {'''
)

with open('app/src/main/java/com/usoy/papiro/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
