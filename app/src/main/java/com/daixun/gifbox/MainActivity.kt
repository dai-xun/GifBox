package com.daixun.gifbox

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.daixun.gifbox.gif.AnimatedGifEncoder
import com.daixun.gifbox.util.GifUtil
import com.fblife.qa.util.ext.Ext
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.compress.Luban
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ext.with(this.application)
        setContentView(R.layout.activity_main)
        vRecord.setOnClickListener {
            RxPermissions(this)
                    .request(Manifest.permission.CAMERA)
                    .subscribe {
                        if (it) {
                            var intent = Intent(this, VideoRecordActivity::class.java)
                            startActivity(intent)
                        }
                    }

            GifUtil.mp4ToGif(0, 11, "/sdcard/2.mp4", "/sdcard/3.gif")

        }

        vPicSelect.setOnClickListener {
            startActivity(Intent(this, CreateByPictureActivity::class.java))
        }
        /**
         * 1 视频拍摄转Gif
         * 2 多图合成Gif
         * 3 gif加水印
         */
    }




}
