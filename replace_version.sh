sed -i '238,300c\
@Composable\
fun VersionHistoryDialog(\
    showHistoryDialog: Boolean,\
    snapshots: List<com.usoy.papiro.ui.components.EditorMemento>,\
    onDismiss: () -> Unit,\
    onRestore: (androidx.compose.ui.text.input.TextFieldValue) -> Unit\
) {\
    if (!showHistoryDialog) return\
    AlertDialog(\
        onDismissRequest = onDismiss,\
        title = {\
            Text(\
                text = "NOTE VERSION HISTORY",\
                fontFamily = FontFamily.Monospace,\
                fontWeight = FontWeight.Bold,\
                fontSize = 18.sp,\
                color = MaterialTheme.colorScheme.primary\
            )\
        },\
        text = {\
            if (snapshots.isEmpty()) {\
                Text("No version history available.", style = MaterialTheme.typography.bodyMedium)\
            } else {\
                androidx.compose.foundation.lazy.LazyColumn(\
                    verticalArrangement = Arrangement.spacedBy(8.dp),\
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)\
                ) {\
                    items(snapshots.size) { index ->\
                        val snapshot = snapshots[snapshots.size - 1 - index]\
                        Card(\
                            modifier = Modifier.fillMaxWidth(),\
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))\
                        ) {\
                            Row(\
                                modifier = Modifier.fillMaxWidth().padding(12.dp),\
                                horizontalArrangement = Arrangement.SpaceBetween,\
                                verticalAlignment = Alignment.CenterVertically\
                            ) {\
                                Column(modifier = Modifier.weight(1f)) {\
                                    Text(\
                                        text = "Version ${snapshots.size - index}",\
                                        style = MaterialTheme.typography.labelMedium,\
                                        fontWeight = FontWeight.Bold,\
                                        color = MaterialTheme.colorScheme.onSurface\
                                    )\
                                    Text(\
                                        text = "${snapshot.state.text.length} characters",\
                                        style = MaterialTheme.typography.bodySmall,\
                                        color = MaterialTheme.colorScheme.onSurfaceVariant\
                                    )\
                                }\
                                Button(\
                                    onClick = {\
                                        onRestore(snapshot.state)\
                                        onDismiss()\
                                    },\
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),\
                                    modifier = Modifier.height(32.dp)\
                                ) {\
                                    Text("RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)\
                                }\
                            }\
                        }\
                    }\
                }\
            }\
        },\
        confirmButton = {\
            TextButton(onClick = onDismiss) {\
                Text("CLOSE")\
            }\
        }\
    )\
}\
' app/src/main/java/com/usoy/papiro/ui/components/EditorDialogs.kt
