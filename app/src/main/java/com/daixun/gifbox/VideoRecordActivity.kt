package com.daixun.gifbox

import android.app.Activity
import android.content.pm.ActivityInfo
import android.hardware.Camera
import android.hardware.Camera.Parameters
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ProgressBar
import android.widget.Toast
import com.daixun.gifbox.util.GifUtil
import kotlinx.android.synthetic.main.activity_video_record.*
import java.io.File
import java.io.IOException
import java.util.*


/**
 * Created by daixun on 17-10-23.
 */

class VideoRecordActivity : Activity(), Callback, OnTouchListener {
    private var mCamera: Camera? = null
    //底部"按住拍"按钮
    private var mStartButton: View? = null
    //进度条
    private var mProgressBar: ProgressBar? = null
    //进度条线程
    private var mProgressThread: Thread? = null
    //录制视频
    private var mMediaRecorder: MediaRecorder? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    //屏幕分辨率
    private var videoWidth: Int = 640
    private var videoHeight: Int = 480
    //判断是否正在录制
    private var isRecording: Boolean = false
    //段视频保存的目录
    private var mTargetFile: File? = null
    //当前进度/时间
    private var mProgress: Int = 0
    //是否上滑取消
    private var isCancel: Boolean = false
    //手势处理, 主要用于变焦 (双击放大缩小)
    private var mDetector: GestureDetector? = null
    //是否放大
    private var isZoomIn = false

    //    private var mHandler: MyHandler? = null
    private var isRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        setContentView(R.layout.activity_video_record)
        initView()
    }

    private fun initView() {

        mDetector = GestureDetector(this, ZoomGestureListener())
        /**
         * 单独处理mSurfaceView的双击事件
         */
        svPreview.setOnTouchListener(object : View.OnTouchListener {

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                mDetector!!.onTouchEvent(event)
                return true
            }
        })

        mSurfaceHolder = svPreview!!.holder
        //设置屏幕分辨率
        mSurfaceHolder!!.setFixedSize(videoWidth, videoHeight)
        mSurfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mSurfaceHolder!!.addCallback(this)
//        mStartButton = findViewById(R.id.main_press_control)
//        mTvTip = findViewById(R.id.main_tv_tip) as TextView

//        vStart.setOnTouchListener(this)
        vStart.setOnClickListener {
            if (isRecording) {
                stopRecordSave()
            } else {
                startRecord()
            }
        }
        //自定义双向进度条    (这个地方差点把我急疯了!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
//        mProgressBar = findViewById(R.id.main_progress_bar) as BothWayProgressBar
//        mProgressBar!!.setOnProgressEndListener(this)
//        mHandler = MyHandler(this)
        mMediaRecorder = MediaRecorder()
    }

    protected override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
    }

    ///////////////////////////////////////////////////////////////////////////
    // SurfaceView回调
    ///////////////////////////////////////////////////////////////////////////
    override fun surfaceCreated(holder: SurfaceHolder?) {
        var hd = holder
        hd = mSurfaceHolder
//        mSurfaceHolder = holder
        startPreView(holder!!)
    }

    /**
     * 开启预览
     *
     * @param holder
     */
    private fun startPreView(holder: SurfaceHolder) {
        Log.d(TAG, "startPreView: ")

        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        }
        if (mMediaRecorder == null) {
            mMediaRecorder = MediaRecorder()
        }
        if (mCamera != null) {
            mCamera!!.setDisplayOrientation(90)
            try {
                mCamera!!.setPreviewDisplay(holder)
                val parameters = mCamera!!.parameters
                //实现Camera自动对焦
                val focusModes = parameters.supportedFocusModes
                if (focusModes != null) {
                    for (mode in focusModes) {
                        mode.contains("continuous-video")
                        parameters.focusMode = "continuous-video"
                    }
                }
                setPreviewSize(parameters)
                mCamera!!.parameters = parameters
                mCamera!!.startPreview()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

    }

    /**
     * 根据手机支持的视频分辨率，设置预览尺寸
     *
     * @param params
     */
    private fun setPreviewSize(params: Parameters) {
        if (mCamera == null) {
            return
        }
        //获取手机支持的分辨率集合，并以宽度为基准降序排序
        val previewSizes = params.getSupportedPreviewSizes()
        Collections.sort(previewSizes, object : Comparator<Camera.Size> {
            override fun compare(lhs: Camera.Size, rhs: Camera.Size): Int {
                return if (lhs.width > rhs.width) {
                    -1
                } else if (lhs.width == rhs.width) {
                    0
                } else {
                    1
                }
            }
        })

        var tmp = 0f
        var minDiff = 100f
        val ratio = 3.0f / 4.0f//TODO 高宽比率3:4，且最接近屏幕宽度的分辨率，可以自己选择合适的想要的分辨率
        var best: Camera.Size? = null
        for (s in previewSizes) {
            tmp = Math.abs(s.height.toFloat() / s.width.toFloat() - ratio)
            Log.e(TAG, "setPreviewSize: width:" + s.width + "...height:" + s.height)
            //            LogUtil.e(LOG_TAG,"tmp:" + tmp);
            if (tmp < minDiff) {
                minDiff = tmp
                best = s
            }
        }
        //        LogUtil.e(LOG_TAG, "BestSize: width:" + best.width + "...height:" + best.height);
        //        List<int[]> range = params.getSupportedPreviewFpsRange();
        //        int[] fps = range.get(0);
        //        LogUtil.e(LOG_TAG,"min="+fps[0]+",max="+fps[1]);
        //        params.setPreviewFpsRange(3,7);

        params.setPreviewSize(best!!.width, best.height)//预览比率

        //        params.setPictureSize(480, 720);//拍照保存比率

        Log.e(TAG, "setPreviewSize BestSize: width:" + best.width + "...height:" + best.height)

        //TODO 大部分手机支持的预览尺寸和录制尺寸是一样的，也有特例，有些手机获取不到，那就把设置录制尺寸放到设置预览的方法里面
        if (params.getSupportedVideoSizes() == null || params.getSupportedVideoSizes().size === 0) {
            mWidth = best.width
            mHeight = best.height
        } else {
            setVideoSize(params)
        }
    }

    private var mWidth: Int = 640//视频录制分辨率宽度
    private var mHeight: Int = 360//视频录制分辨率高度
    /**
     * 根据手机支持的视频分辨率，设置录制尺寸
     *
     * @param params
     */
    private fun setVideoSize(params: Parameters) {
        if (mCamera == null) {
            return
        }
        //获取手机支持的分辨率集合，并以宽度为基准降序排序
        val previewSizes = params.supportedVideoSizes
        Collections.sort(previewSizes, object : Comparator<Camera.Size> {
            override fun compare(lhs: Camera.Size, rhs: Camera.Size): Int {
                return if (lhs.width > rhs.width) {
                    -1
                } else if (lhs.width == rhs.width) {
                    0
                } else {
                    1
                }
            }
        })

        var tmp = 0f
        var minDiff = 100f
        val ratio = 3.0f / 4.0f//高宽比率3:4，且最接近屏幕宽度的分辨率
        var best: Camera.Size? = null
        for (s in previewSizes) {
            tmp = Math.abs(s.height.toFloat() / s.width.toFloat() - ratio)
            Log.e(TAG, "setVideoSize: width:" + s.width + "...height:" + s.height)
            if (tmp < minDiff) {
                minDiff = tmp
                best = s
            }
        }
        Log.e(TAG, "setVideoSize BestSize: width:" + best!!.width + "...height:" + best.height)
        //设置录制尺寸
        mWidth = best.width
        mHeight = best.height
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        var hd = holder
        hd = mSurfaceHolder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (mCamera != null) {
            Log.d(TAG, "surfaceDestroyed: ")
            //停止预览并释放摄像头资源
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
        if (mMediaRecorder != null) {
            mMediaRecorder!!.release()
            mMediaRecorder = null
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // 进度条结束后的回调方法
    ///////////////////////////////////////////////////////////////////////////
    fun onProgressEndListener() {
        //视频停止录制
        stopRecordSave()
    }

    /**
     * 开始录制
     */
    private fun startRecord() {
        if (mMediaRecorder != null) {
            //没有外置存储, 直接停止录制
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return
            }
            try {
                //mMediaRecorder.reset();
                mCamera!!.unlock()
                mMediaRecorder!!.setCamera(mCamera)
                //从相机采集视频
                mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                // 从麦克采集音频信息
                mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                // TODO: 2016/10/20  设置视频格式
                mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mMediaRecorder!!.setVideoSize(videoWidth, videoHeight)
                //每秒的帧数
                mMediaRecorder!!.setVideoFrameRate(24)
                //编码格式
                mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
                mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                // 设置帧频率，然后就清晰了
                mMediaRecorder!!.setVideoEncodingBitRate(1024 * 512)
                // TODO: 2016/10/20 临时写个文件地址, 稍候该!!!
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                mTargetFile = File(targetDir,
                        SystemClock.currentThreadTimeMillis().toString() + ".mp4")
                mMediaRecorder!!.setOutputFile(mTargetFile!!.getAbsolutePath())
                mMediaRecorder!!.setPreviewDisplay(mSurfaceHolder!!.surface)
                //解决录制视频, 播放器横向问题
                mMediaRecorder!!.setOrientationHint(90)

                mMediaRecorder!!.prepare()
                //正式录制
                mMediaRecorder!!.start()
                isRecording = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    /**
     * 停止录制 并且保存
     */
    private fun stopRecordSave() {
        if (isRecording) {
            isRunning = false
            mMediaRecorder!!.stop()
            isRecording = false

            Toast.makeText(this, "视频已经放至" + mTargetFile!!.getAbsolutePath(), Toast.LENGTH_SHORT).show()


        }
    }

    /**
     * 停止录制, 不保存
     */
    private fun stopRecordUnSave() {
        if (isRecording) {
            isRunning = false
            mMediaRecorder!!.stop()
            isRecording = false
            if (mTargetFile!!.exists()) {
                //不保存直接删掉
                mTargetFile!!.delete()
            }
        }
    }

    /**
     * 相机变焦
     *
     * @param zoomValue
     */
    fun setZoom(zoomValue: Int) {
        var zoomValue = zoomValue
        if (mCamera != null) {
            val parameters = mCamera!!.parameters
            if (parameters.isZoomSupported) {//判断是否支持
                val maxZoom = parameters.maxZoom
                if (maxZoom == 0) {
                    return
                }
                if (zoomValue > maxZoom) {
                    zoomValue = maxZoom
                }
                parameters.zoom = zoomValue
                mCamera!!.parameters = parameters
            }
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Handler处理
    ///////////////////////////////////////////////////////////////////////////
    /*private class MyHandler(activity: MainActivity) : Handler() {
        private val mReference: WeakReference<MainActivity>
        private val mActivity: MainActivity

        init {
            mReference = WeakReference<MainActivity>(activity)
            mActivity = mReference.get()
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> mActivity.mProgressBar!!.setProgress(mActivity.mProgress)
            }

        }
    }*/

    /**
     * 触摸事件的触发
     *
     * @param v
     * @param event
     * @return
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        var ret = false
        val action = event.action
        val ey = event.y
        val ex = event.x
        //只监听中间的按钮处
        val vW = v.getWidth()
        val left = LISTENER_START
        val right = vW - LISTENER_START

        var downY = 0f

        when (v.getId()) {
            R.id.vStart -> {
                when (action) {
                    MotionEvent.ACTION_DOWN -> if (ex > left && ex < right) {
//                        mProgressBar!!.setCancel(false)
                        //显示上滑取消
                        mTvTip!!.visibility = View.VISIBLE
                        mTvTip!!.text = "↑ 上滑取消"
                        //记录按下的Y坐标
                        downY = ey
                        // TODO: 2016/10/20 开始录制视频, 进度条开始走
                        mProgressBar!!.setVisibility(View.VISIBLE)
                        //开始录制
                        Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show()
                        startRecord()

                        /*mProgressThread = object : Thread() {
                            override fun run() {
                                super.run()
                                try {
                                    mProgress = 0
                                    isRunning = true
                                    while (isRunning) {
                                        mProgress++
                                        mHandler!!.obtainMessage(0).sendToTarget()
                                        Thread.sleep(20)
                                    }
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }

                            }
                        }

                        mProgressThread!!.start()*/
                        ret = true
                    }
                    MotionEvent.ACTION_UP -> if (ex > left && ex < right) {
                        mTvTip!!.visibility = View.INVISIBLE
                        mProgressBar!!.setVisibility(View.INVISIBLE)
                        //判断是否为录制结束, 或者为成功录制(时间过短)
                        if (!isCancel) {
                            if (mProgress < 50) {
                                //时间太短不保存
                                stopRecordUnSave()
                                Toast.makeText(this, "时间太短", Toast.LENGTH_SHORT).show()
                            } else
                            //停止录制
                                stopRecordSave()
                        } else {
                            //现在是取消状态,不保存
                            stopRecordUnSave()
                            isCancel = false
                            Toast.makeText(this, "取消录制", Toast.LENGTH_SHORT).show()
//                            mProgressBar!!.setCancel(false)
                        }

                        ret = false
                    }
                    MotionEvent.ACTION_MOVE -> if (ex > left && ex < right) {
                        val currentY = event.y
                        if (downY - currentY > 10) {
                            isCancel = true
//                            mProgressBar!!.setCancel(true)
                        }
                    }
                }

            }
        }
        return ret
    }

    ///////////////////////////////////////////////////////////////////////////
    // 变焦手势处理类
    ///////////////////////////////////////////////////////////////////////////
    internal inner class ZoomGestureListener : GestureDetector.SimpleOnGestureListener() {
        //双击手势事件
        override fun onDoubleTap(e: MotionEvent): Boolean {
            super.onDoubleTap(e)
            Log.d(TAG, "onDoubleTap: 双击事件")
            if (mMediaRecorder != null) {
                if (!isZoomIn) {
                    setZoom(20)
                    isZoomIn = true
                } else {
                    setZoom(0)
                    isZoomIn = false
                }
            }
            return true
        }
    }

    companion object {

        private val LISTENER_START = 200
        private val TAG = "MainActivity"
        //录制最大时间
        val MAX_TIME = 10
    }
}
