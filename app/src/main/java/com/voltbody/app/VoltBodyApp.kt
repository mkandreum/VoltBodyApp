package com.voltbody.app

import android.app.Application
import com.voltbody.app.data.Repository

class VoltBodyApp : Application() {
    lateinit var repo: Repository
    override fun onCreate() { super.onCreate(); repo = Repository(this) }
}
