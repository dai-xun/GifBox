package com.daixun.gifbox.util;

import android.os.SystemClock;
import android.util.Log;

import com.fblife.qa.util.ext.Ext;

import java.io.File;

/**
 * @author daixun
 *         Created on 17-10-26.
 */

public class GifUtil {

    public static boolean mp4ToGif(int startTime, int duration, String videoPath, String gifPath) {
        long begin = SystemClock.currentThreadTimeMillis();
        String globalPalettePicPath = new File(Ext.INSTANCE.getPicDirFile(), "globalPalettePicPath.png").getAbsolutePath();
        String command = "ffmpeg -ss " + startTime + " -t " + duration + " -i " + videoPath + " -b 568k -r 15 -vf fps=15,scale=320:-1:flags=lanczos,palettegen -y " + globalPalettePicPath;
        FFmpegCmd.cmdRun(command);
        String commandGif = "ffmpeg -v warning -ss " + startTime + " -t " + duration + " -i " + videoPath + " -i " + globalPalettePicPath + " -r 15 -lavfi fps=15,scale=270:-1:flags=lanczos[x];[x][1:v]paletteuse -y " + gifPath;
        FFmpegCmd.cmdRun(commandGif);
        Log.e("TAG", "处理耗时" + (SystemClock.currentThreadTimeMillis() - begin));
        return true;
    }

    /*生成全局调色板命令：
    @param startTime 开始时间 秒/s
    @param duration  间隔时间 秒/s（用来截取视频）
    @param videoPath 源视频路径
    @param globalPalettePicPath 输出的全局调色板的路径
    String command = "ffmpeg -ss " + startTime + " -t " + duration + " -i " + videoPath + " -b 568k -r 20 -vf fps=20,scale=320:-1:flags=lanczos,palettegen -y " + globalPalettePicPath;
*/

    /*利用调色板图片和视频源文件同时处理生成 gif 命令：
    @param startTime 开始时间 秒/s
    @param duration  间隔时间 秒/s（用来截取视频）
    @param videoPath 源视频路径
    @param globalPalettePicPath 全局调色板的路径
    @param outFilePath Gif输出路径
    String command = "ffmpeg -v warning -ss " + startTime + " -t " + duration + " -i " + videoPath + " -i " + globalPalettePicPath + " -r 15 -lavfi fps=15,scale=270:-1:flags=lanczos[x];[x][1:v]paletteuse -y " + outFilePath;
*/

}
