package com.daixun.gifbox

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.*
import android.os.Handler.Callback
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.daixun.gifbox.gif.AnimatedGifEncoder
import com.fblife.qa.util.ext.getStringEx
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.compress.Luban
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_create_by_picture.*
import java.io.File

/**
 * @author daixun
 * Created on 17-10-27.
 */

class CreateByPictureActivity : Activity() {

    private var mPicList = mutableListOf<String>("/storage/emulated/0/DCIM/Camera/IMG_20171024_172239_2.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172239_1.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172239.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172238_2.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172238_1.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172238.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172237_1.jpg"
            , "/storage/emulated/0/DCIM/Camera/IMG_20171024_172237.jpg")

    private var isLoop = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_by_picture)
//        ivGifView.setImageDrawable()


        vCreate.setOnClickListener {
            builderGif(mPicList)
        }
        setDelay(frameDelay)
        sbDelay.progress = frameDelay

        sbDelay.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentFrame = 0;
                    setDelay(progress + 10)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                handle.removeCallbacks(gifRunnable)
                showAsGif()
            }

        })
//        choosePics()


    }

    override fun onResume() {
        super.onResume()
        loadFrame(currentFrame,0)
    }

    override fun onPause() {
        super.onPause()
        handle.removeCallbacks(gifRunnable)
    }

    private fun setDelay(d: Int) {
        frameDelay = d
        tvDelayLabel.text = getStringEx(R.string.frameDelay, d)
    }

    private var gifWidth = 600
    private var gifHeight = 600
    private var currentFrame = 0
    private fun showAsGif() {
        loadFrame(0, 0)
    }

    var frameDelay = 300
    private fun loadFrame(i: Int, showTime: Long) {
        GlideApp.with(this).load(mPicList.get(getCurrentFrame()))
                .override(gifWidth, gifHeight).into(object : SimpleTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable?, transition: Transition<in Drawable>?) {
                currentDrawable = resource
                if (showTime == 0L) {
                    handle.post(gifRunnable)
                } else {
                    handle.postAtTime(gifRunnable, showTime)
                }
            }
        })
    }


    val handle: Handler by lazy {
        Handler(object : Callback {
            override fun handleMessage(msg: Message?): Boolean {
                msg?.let {
                    ivGifView.setImageDrawable(msg.obj as Drawable)
                    currentFrame++
                    if (currentFrame == mPicList.size) {
                        currentFrame = 0
                    }
                    loadFrame(getCurrentFrame(), SystemClock.uptimeMillis() + frameDelay)
                    /*when(it.what){

                    }*/
                }
                return false
            }
        })
    }

    var currentDrawable: Drawable? = null
    var gifRunnable = object : Runnable {
        override fun run() {
            ivGifView.setImageDrawable(currentDrawable)
            currentFrame++
            if (currentFrame == mPicList.size) {
                currentFrame = 0
            }
            loadFrame(getCurrentFrame(), SystemClock.uptimeMillis() + frameDelay)
        }
    }

    override fun onDestroy() {
        handle.removeCallbacks(gifRunnable)
        super.onDestroy()
    }

    fun getCurrentFrame(): Int {
        return currentFrame
    }

    private fun builderGif(picList: List<String>) {
        var outPutFile = File(Environment.getExternalStorageDirectory(), "1GIF")
        if (!outPutFile.exists()) outPutFile.mkdirs()

        val gifEncoder = AnimatedGifEncoder()
        val gifPath = File(outPutFile, SystemClock.currentThreadTimeMillis().toString() + ".gif").absolutePath
        gifEncoder.start(gifPath)
        gifEncoder.setDelay(frameDelay)   // 1 frame per sec
        var begin = SystemClock.elapsedRealtime()
        Observable.fromIterable(picList)
                .map {
                    var begin = System.currentTimeMillis()
                    val bitmap = Glide.with(this).asBitmap().load(it).submit(600, 600).get()
                    val marker = Glide.with(this).asBitmap().load(R.drawable.marker).submit().get()

                    var canvas = Canvas(bitmap)
                    canvas.drawBitmap(marker, 0F, 0F, null)

                    Log.d("TAG", "图片大小${bitmap.width}X${bitmap.height}")
                    Log.e("TAG", "解码帧耗时${(System.currentTimeMillis() - begin)} 毫秒")
                    gifEncoder.addFrame(bitmap)
                    Log.e("TAG", "写入帧耗时${(System.currentTimeMillis() - begin)} 毫秒")
                    return@map true
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {
                    it.printStackTrace()
                }, {
                    gifEncoder.finish()
                    Log.e("TAG", "耗时：${SystemClock.elapsedRealtime() - begin}")
                    Toast.makeText(this, "合并完成", Toast.LENGTH_SHORT).show()

                    Glide.with(this).load(gifPath).into(ivGifView)
                })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    // 图片选择结果回调
                    var selectList = PictureSelector.obtainMultipleResult(data)
                    // 例如 LocalMedia 里面返回两种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true
//                    adapter.setList(selectList)
//                    adapter.notifyDataSetChanged()
                    mPicList.clear()
                    selectList?.forEach {
                        Log.e("TAG", it.path)
                        mPicList.add(it.path)
                    }
                    builderGif(selectList.map { it.path })
                }
            }
        }
    }


    private fun choosePics() {
        PictureSelector.create(this)
                .openGallery(PictureMimeType.ofImage())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
//                    .theme()//主题样式(不设置为默认样式) 也可参考demo values/styles下 例如：R.style.picture.white.style
                .maxSelectNum(100)// 最大图片选择数量 int
                .minSelectNum(2)// 最小选择数量 int
                .imageSpanCount(4)// 每行显示个数 int
                .selectionMode(PictureConfig.MULTIPLE)// 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                .previewImage(true)// 是否可预览图片 true or false
                .compressGrade(Luban.THIRD_GEAR)// luban压缩档次，默认3档 Luban.THIRD_GEAR、Luban.FIRST_GEAR、Luban.CUSTOM_GEAR
                .isCamera(false)// 是否显示拍照按钮 true or false
                .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                .sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
//                    .enableCrop()// 是否裁剪 true or false
                .compress(false)// 是否压缩 true or false
                .compressMode(PictureConfig.LUBAN_COMPRESS_MODE)//系统自带 or 鲁班压缩 PictureConfig.SYSTEM_COMPRESS_MODE or LUBAN_COMPRESS_MODE
                .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
    }
}
