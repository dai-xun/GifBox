package com.daixun.gifbox.util;

/**
 * @author daixun
 *         Created on 17-10-26.
 */
public class FFmpegCmd {
    /**
     * 加载所有相关链接库
     */
    static {
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("swresample");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("avfilter");
//        System.loadLibrary("avdevice");
        System.loadLibrary("ffmpegcmd");
    }

    public static native int cmdRun(String[] cmd);

    public static int cmdRun(String cmd) {
        String regulation = "[ \\t]+";
        final String[] split = cmd.split(regulation);
        return cmdRun(split);
    }


}