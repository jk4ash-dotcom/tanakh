package com.tanakhpoc.learner

import android.app.Application
import com.tanakhpoc.learner.data.PackRepository

class TanakhApp : Application() {
    lateinit var repository: PackRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PackRepository.getInstance(this)
    }

    companion object {
        fun from(context: android.content.Context): TanakhApp =
            context.applicationContext as TanakhApp
    }
}
