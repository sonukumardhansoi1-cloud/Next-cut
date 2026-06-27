package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository

class NextCutApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ProjectRepository(database.projectDao(), database.exportDao()) }
}
