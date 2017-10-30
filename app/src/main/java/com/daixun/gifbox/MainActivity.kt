package com.daixun.gifbox

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
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
import com.fblife.qa.util.ext.toast
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.compress.Luban
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : Activity() {

    companion object {
        const val REQ_GET_VIDEO = 0x0111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ext.with(this.application)
        setContentView(R.layout.activity_main)
        vRecord.setOnClickListener {
            RxPermissions(this)
                    .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    .subscribe {
                        if (it) {
                            var intent = Intent(this, VideoRecorderActivity::class.java)
                            startActivityForResult(intent, REQ_GET_VIDEO)
                        }
                    }
        }

        vPicSelect.setOnClickListener {
            RxPermissions(this)
                    .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe {
                        if (it) {
                            choosePics()
                        } else {
                            toast("需要读取本地存储的权限")
                        }
                    }
        }
        /** Tasks
         * 1 视频拍摄转Gif
         * 2 多图合成Gif
         * 3 gif加水印
         */
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    // 图片选择结果回调
                    var selectList = PictureSelector.obtainMultipleResult(data)
                    CreateByPictureActivity.startAction(arrayListOf<String>().apply {
                        this.addAll(selectList.map { it.path })
                    }, this);
                }
                REQ_GET_VIDEO -> {
                    val videoPath = data?.getStringExtra(VideoRecorderActivity.EXTRA_DATA)
                    videoPath?.let {
                        convertVideoToGif(it)
                    }
                }
            }
        }
    }

    private fun convertVideoToGif(mp4Path: String): Disposable? {
        toast("开始转换")
        return Single.fromCallable {
            var gifPath = File(Ext.getPicDirFile(), "test.gif").absolutePath
            GifUtil.mp4ToGif(0, 10, mp4Path, gifPath)
            return@fromCallable gifPath
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast("转换成功,文件保存为${it}")
                }, {
                    toast("转换失败")
                })
    }

    private fun choosePics() {
        PictureSelector.create(this)
                .openGallery(PictureMimeType.ofImage())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                .maxSelectNum(100)// 最大图片选择数量 int
                .minSelectNum(2)// 最小选择数量 int
                .imageSpanCount(4)// 每行显示个数 int
                .selectionMode(PictureConfig.MULTIPLE)// 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                .previewImage(true)// 是否可预览图片 true or false
                .compressGrade(Luban.THIRD_GEAR)// luban压缩档次，默认3档 Luban.THIRD_GEAR、Luban.FIRST_GEAR、Luban.CUSTOM_GEAR
                .isCamera(false)// 是否显示拍照按钮 true or false
                .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                .sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                .compress(false)// 是否压缩 true or false
                .compressMode(PictureConfig.LUBAN_COMPRESS_MODE)//系统自带 or 鲁班压缩 PictureConfig.SYSTEM_COMPRESS_MODE or LUBAN_COMPRESS_MODE
                .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
    }

}
