package com.daixun.gifbox

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast


import com.daixun.gifbox.util.MediaUtils
import com.daixun.gifbox.widget.SendView
import com.daixun.gifbox.widget.VideoProgressBar
import com.fblife.qa.util.ext.Ext

import java.util.UUID

/**
 * Created by wanbo on 2017/1/18.
 */

class VideoRecorderActivity : Activity() {

    private var mediaUtils: MediaUtils? = null
    private var isCancel: Boolean = false
    private var progressBar: VideoProgressBar? = null
    private var mProgress: Int = 0
    private var btnInfo: TextView? = null
    private var btn: TextView? = null
    private val view: TextView? = null
    private var send: SendView? = null
    private var recordLayout: RelativeLayout? = null
    private var switchLayout: RelativeLayout? = null
    companion object {
        const val EXTRA_DATA="data"
    }

    internal var btnTouch: View.OnTouchListener = View.OnTouchListener { v, event ->
        var ret = false
        val downY = 0f
        val action = event.action

        when (v.id) {
            R.id.main_press_control -> {
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        mediaUtils!!.record()
                        startView()
                        ret = true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isCancel) {
                            if (mProgress == 0) {
                                stopView(false)
                            } else
                                if (mProgress < 10) {
                                    //时间太短不保存
                                    mediaUtils!!.stopRecordUnSave()
                                    Toast.makeText(this@VideoRecorderActivity, "时间太短", Toast.LENGTH_SHORT).show()
                                    stopView(false)
                                } else {
                                    //停止录制
                                    mediaUtils!!.stopRecordSave()
                                    stopView(true)
                                }
                        } else {
                            //现在是取消状态,不保存
                            mediaUtils!!.stopRecordUnSave()
                            Toast.makeText(this@VideoRecorderActivity, "取消保存", Toast.LENGTH_SHORT).show()
                            stopView(false)
                        }
                        ret = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val currentY = event.y
                        isCancel = downY - currentY > 10
                        moveView()
                    }
                }
            }
        }
        ret
    }

    internal var listener: VideoProgressBar.OnProgressEndListener = VideoProgressBar.OnProgressEndListener {
        progressBar!!.setCancel(true)
        mediaUtils!!.stopRecordSave()
    }

    internal var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    progressBar!!.setProgress(mProgress)
                    if (mediaUtils!!.isRecording) {
                        mProgress = mProgress + 1
                        sendMessageDelayed(this.obtainMessage(0), 100)
                    }
                }
            }
        }
    }

    private val backClick = View.OnClickListener {
        send!!.stopAnim()
        recordLayout!!.visibility = View.VISIBLE
        mediaUtils!!.deleteTargetFile()
    }

    private val selectClick = View.OnClickListener {
        val path = mediaUtils!!.targetFilePath
//        Toast.makeText(this@VideoRecorderActivity, "文件以保存至：" + path, Toast.LENGTH_SHORT).show()
        send!!.stopAnim()
        recordLayout!!.visibility = View.VISIBLE
        var intent=Intent()
        intent.putExtra(EXTRA_DATA,path)
        setResult(RESULT_OK,intent)
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        val surfaceView = findViewById<View>(R.id.main_surface_view) as SurfaceView
        // setting
        mediaUtils = MediaUtils(this)
        mediaUtils!!.setRecorderType(MediaUtils.MEDIA_VIDEO)
        mediaUtils!!.setTargetDir(Ext.getPicDirFile())
        mediaUtils!!.setTargetName(UUID.randomUUID().toString() + ".mp4")
        mediaUtils!!.setSurfaceView(surfaceView)
        // btn
        send = findViewById<View>(R.id.view_send) as SendView
        //        view = (TextView) findViewById(R.id.view);
        btnInfo = findViewById<View>(R.id.tv_info) as TextView
        btn = findViewById<View>(R.id.main_press_control) as TextView
        btn!!.setOnTouchListener(btnTouch)
        findViewById<View>(R.id.btn_close).setOnClickListener { finish() }
        send!!.backLayout.setOnClickListener(backClick)
        send!!.selectLayout.setOnClickListener(selectClick)
        recordLayout = findViewById<View>(R.id.record_layout) as RelativeLayout
        switchLayout = findViewById<View>(R.id.btn_switch) as RelativeLayout
        switchLayout!!.setOnClickListener { mediaUtils!!.switchCamera() }
        // progress
        progressBar = findViewById<View>(R.id.main_progress_bar) as VideoProgressBar
        progressBar!!.setOnProgressEndListener(listener)
        progressBar!!.setCancel(true)
    }

    override fun onResume() {
        super.onResume()
        progressBar!!.setCancel(true)
    }

    private fun startView() {
        startAnim()
        mProgress = 0
        handler.removeMessages(0)
        handler.sendMessage(handler.obtainMessage(0))
    }

    private fun moveView() {
        if (isCancel) {
            btnInfo!!.text = "松手取消"
        } else {
            btnInfo!!.text = "上滑取消"
        }
    }

    private fun stopView(isSave: Boolean) {
        stopAnim()
        progressBar!!.setCancel(true)
        mProgress = 0
        handler.removeMessages(0)
        btnInfo!!.text = "双击放大"
        if (isSave) {
            recordLayout!!.visibility = View.GONE
            send!!.startAnim()
        }
    }

    private fun startAnim() {
        val set = AnimatorSet()
        set.playTogether(
                ObjectAnimator.ofFloat(btn, "scaleX", 1f, 0.5f),
                ObjectAnimator.ofFloat(btn, "scaleY", 1f, 0.5f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1f, 1.3f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1f, 1.3f)
        )
        set.setDuration(250).start()
    }

    private fun stopAnim() {
        val set = AnimatorSet()
        set.playTogether(
                ObjectAnimator.ofFloat(btn, "scaleX", 0.5f, 1f),
                ObjectAnimator.ofFloat(btn, "scaleY", 0.5f, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1.3f, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1.3f, 1f)
        )
        set.setDuration(250).start()
    }

}
