package com.fblife.qa.util.ext

import android.app.Application

/**
 * Created by daixun on 17-9-8.
 */
object Ext {
    lateinit var ctx: Application

    fun with(app: Application) {
        this.ctx = app
    }
}