package com.fblife.qa.util.ext

import android.app.Application
import android.os.Environment
import java.io.File

/**
 * Created by daixun on 17-9-8.
 */
object Ext {
    lateinit var ctx: Application

    fun with(app: Application) {
        this.ctx = app
    }

    fun getPicDir(): String {
        return getPicDirFile().absolutePath
    }

    fun getPicDirFile(): File {
        var picDir = File(Environment.getExternalStorageDirectory(), "1GIF")
        if (!picDir.exists()) picDir.mkdirs()
        return picDir
    }
}