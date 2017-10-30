package com.fblife.qa.util.ext

import android.app.Activity
import android.support.annotation.PluralsRes
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast

/**
 * Created by daixun on 17-9-8.
 */


fun getStringEx(res: Int): String = Ext.ctx.resources.getString(res)

fun getStringEx(res: Int, vararg formatArg: Any): String {
    //kotlin传递可变参数给java时需要使用*来传递参数
    return Ext.ctx.resources.getString(res, *formatArg)
}


fun getColorEx(res: Int): Int = ContextCompat.getColor(Ext.ctx, res)

fun View.setVisibility(isVisible: Boolean) {
    if (isVisible) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}

fun Activity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Activity.toast(msg: Int) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}