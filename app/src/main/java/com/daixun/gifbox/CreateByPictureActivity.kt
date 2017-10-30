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
import java.util.ArrayList

/**
 * @author daixun
 * Created on 17-10-27.
 */

class CreateByPictureActivity : Activity() {


    companion object {
        const val EXTRA_PICS = "pictures"
        fun startAction(pics: ArrayList<String>, act: Activity) {
            var intent = Intent(act, CreateByPictureActivity::class.java)
            intent.putStringArrayListExtra(EXTRA_PICS, pics)
            act.startActivity(intent)
        }
    }

    private var mPicList = mutableListOf<String>()

    private var gifWidth = 600
    private var gifHeight = 600
    private var currentFrame = 0
    var frameDelay = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_by_picture)

        var pics = intent.getStringArrayListExtra(EXTRA_PICS)
        if (pics == null || pics.size == 0) {
            finish()
            return
        }
        pics?.forEach { mPicList.add(it) }

        vCreate.setOnClickListener {
            builderGif()
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
    }

    override fun onResume() {
        super.onResume()
        loadFrame(currentFrame, 0)
    }

    override fun onPause() {
        super.onPause()
        handle.removeCallbacks(gifRunnable)
    }

    private fun setDelay(d: Int) {
        frameDelay = d
        tvDelayLabel.text = getStringEx(R.string.frameDelay, d)
    }

    private fun showAsGif() {
        loadFrame(0, 0)
    }


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

    private fun builderGif() {
        var outPutFile = File(Environment.getExternalStorageDirectory(), "1GIF")
        if (!outPutFile.exists()) outPutFile.mkdirs()

        val gifEncoder = AnimatedGifEncoder()
        val gifPath = File(outPutFile, SystemClock.currentThreadTimeMillis().toString() + ".gif").absolutePath
        gifEncoder.start(gifPath)
        gifEncoder.setDelay(frameDelay)   // 1 frame per sec
        var begin = SystemClock.elapsedRealtime()
        Observable.fromIterable(mPicList)
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
}
