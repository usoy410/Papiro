import re

with open('app/src/main/java/com/usoy/papiro/viewmodel/NoteViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '''            } catch (e: Exception) {
                Log.e(TAG, "Failed local generation in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                }
            }''',
    '''            } catch (e: Exception) {
                Log.e(TAG, "Failed local generation in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error generating note: ${e.message}")
                }
            }'''
)

content = content.replace(
    '''            } catch (e: Exception) {
                Log.e(TAG, "Generate custom prompt failed", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                }
            }''',
    '''            } catch (e: Exception) {
                Log.e(TAG, "Generate custom prompt failed", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error generating custom content: ${e.message}")
                }
            }'''
)

content = content.replace(
    '''            } catch (e: Exception) {
                Log.e(TAG, "TOC failed in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                }
            }''',
    '''            } catch (e: Exception) {
                Log.e(TAG, "TOC failed in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error generating table of contents: ${e.message}")
                }
            }'''
)

content = content.replace(
    '''            } catch (e: Exception) {
                Log.e(TAG, "Enhance failed in app-scope", e)
            }''',
    '''            } catch (e: Exception) {
                Log.e(TAG, "Enhance failed in app-scope", e)
                withContext(Dispatchers.Main) {
                    _uiEvent.emit("Error enhancing content: ${e.message}")
                }
            }'''
)

content = content.replace(
    '''            } catch (e: Exception) {
                Log.e(TAG, "Generate title failed", e)
                withContext(Dispatchers.Main) {
                    onComplete("Untitled Note")
                }
            }''',
    '''            } catch (e: Exception) {
                Log.e(TAG, "Generate title failed", e)
                withContext(Dispatchers.Main) {
                    _uiEvent.emit("Error generating title: ${e.message}")
                    onComplete("Untitled Note")
                }
            }'''
)

with open('app/src/main/java/com/usoy/papiro/viewmodel/NoteViewModel.kt', 'w') as f:
    f.write(content)
