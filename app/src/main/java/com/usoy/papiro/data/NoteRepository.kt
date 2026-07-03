package com.usoy.papiro.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Long): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    suspend fun insert(note: NoteEntity): Long {
        return noteDao.insertNote(note)
    }

    suspend fun deleteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    fun getHistoryForNote(noteId: Long): Flow<List<NoteHistoryEntity>> {
        return noteDao.getHistoryForNote(noteId)
    }

    suspend fun insertHistory(history: NoteHistoryEntity) {
        noteDao.insertHistory(history)
    }

    suspend fun deleteHistoryForNote(noteId: Long) {
        noteDao.deleteHistoryForNote(noteId)
    }
}
