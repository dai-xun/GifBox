package com.daixun.gifbox

import okio.BufferedSource
import okio.Okio
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.file.OpenOption

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        val source = Okio.source(File("/home/daixun/Desktop/mk.gif"))
        val buffer = Okio.buffer(source)

//        buffer.readByteArray(255*3+6+7)
//        println("${buffer.readByte().toBit()}")

        val header = buffer.readByteArray(6)

        var headerStr = StringBuilder()
        headerStr.append(header[0].toChar())
        headerStr.append(header[1].toChar())
        headerStr.append(header[2].toChar())
        headerStr.append(header[3].toChar())
        headerStr.append(header[4].toChar())
        headerStr.append(header[5].toChar())

        println("GIF头：${headerStr}")
        println("============================")


        println("逻辑屏幕大小 ${buffer.readShortLe()}X${buffer.readShortLe()}")
//        println("${buffer.readByte().toBit()}")
        var cv = buffer.readUnsignedByte()
//        var cv=buffer.readByte().toInt()
        println("cv:${cv.toByte().toBit()}")
        println("m:${cv shr 7}  全局颜色列表标志(Global Color Table Flag)，当置位时表示有全局颜色列表，pixel值有意义.")
        println("cr:${((247 and 112) shr 4).toByte()}  颜色深度(Color ResoluTion)，cr+1确定图象的颜色深度.")
        println("s:${(cv and 0b00001000) shr 3} 分类标志(Sort Flag)，如果置位表示全局颜色列表分类排列")
        var pixel = cv and 0b00000111
        var gct = Math.pow(2.toDouble(), (pixel + 1).toDouble()).toInt()
        println("pixel:${pixel}  全局颜色列表大小${gct}，pixel+1确定颜色列表的索引数（2的pixel+1次方）.")


        println("背景颜色${buffer.readByte()}  (在全局颜色列表中的索引，如果没有全局颜色列表，该值没有意义)")
        println("像素宽高比${buffer.readByte()}(Pixel Aspect Radio)")


        buffer.readByteArray((gct * 3).toLong())
        println("读取全局颜色表 ")

        var frameCount = 0
        var end = false
        while (!end) {
            val byte = buffer.readUnsignedByte()
            when (byte) {
            //图像标示符
                0x2C -> {
                    frameCount++
                    println("======================图像标示符=========================")
                    println("x方向偏移量${buffer.readShortLe()} Y方向偏移量${buffer.readShortLe()}")
                    println("图像宽高${buffer.readShortLe()}x${buffer.readShortLe()}")
                    var pCtr = buffer.readByte().toInt() and 0x000000FF
                    var m = pCtr and 0b10000000
                    println("m ${m} 局部颜色列表标志(Local Color Table Flag) 置位时标识紧接在图象标识符之后有一个局部颜色列表，供紧跟在它之后的一幅图象使用；值否时使用全局颜色列表，忽略pixel值。")
                    println("i ${pCtr and 0b01000000} - 交织标志(Interlace Flag)，置位时图象数据使用连续方式排列，否则使用顺序排列。")
                    println("s ${pCtr and 0b00100000} - 分类标志(Sort Flag)，如果置位表示紧跟着的局部颜色列表分类排列.")
                    println("r ${pCtr and 0b00011000} - 保留，必须初始化为0.")
                    var pixel = pCtr and 0b00000111
                    if (m == 1) {
                        println("pixel ${pixel}- 局部颜色列表大小(Size of Local Color Table)，pixel+1就为颜色列表的位数")
                        println("读取局部颜色表 ")
                        var gct = Math.pow(2.toDouble(), (pixel + 1).toDouble()).toInt()
                        buffer.readByteArray((gct * 3).toLong())
                    } else {
                        println("使用全局颜色表")
                    }

                    var codeTable = buffer.readUnsignedByte()
                    var blockTotalSize = 0
                    do {
                        val blockSize = buffer.readUnsignedByte()
                        blockTotalSize += blockSize
                        if (blockSize > 0) {
                            buffer.skip(blockSize.toLong())
                        }
                    } while (blockSize > 0)

                    println("数据块大小${blockTotalSize} byte")
                }
            //扩展符
                0x21 -> {
                    var label = buffer.readUnsignedByte()
                    when (label) {
                    //图形控制扩展块
                        0xF9 -> {
                            println("=================图形控制扩展(Graphic Control Extension)")
                            val blockSize = buffer.readByte() //Block Size - 不包括块终结器，固定值4
                            assert(blockSize.toInt() == 4)
                            buffer.readByte()
                            val delay = buffer.readByte()
                            println("delay is ${delay * 10} 毫秒")
                            buffer.readByte()
                            assert(buffer.readByte().toInt() == 0)
                            println("===================================================")
                        }
                        0x21 -> {
                            val cl = buffer.readUnsignedByte()//Comment Label - 标识这是一个注释块，固定值0xFE
                            assert(cl == 0xFE)
                        }
                    //图形控制扩展标签
                        0x01 ->{
                            assert(buffer.readUnsignedByte()==12)
                        }

                    }

                }
            //结束符
                0x3B -> {
                    print("文件结尾")
                    end = true
                }
            }
        }
        println("图像共${frameCount}帧")
        //println("${(0x2c).toByte().toBit()}")
//        println("图像标识符${buffer.readByte().toInt() and 0x000000FF}   ${buffer.readByte().toInt() and 0x000000FF}")
        buffer.close()
    }

    /*fun byteToBit(b: Byte): String {


        return "" + (b shr 7 and 0x1).toByte() +
                (b shr 6 and 0x1).toByte() +
                (b shr 5 and 0x1).toByte() +
                (b shr 4 and 0x1).toByte() +
                (b shr 3 and 0x1).toByte() +
                (b shr 2 and 0x1).toByte() +
                (b shr 1 and 0x1).toByte() +
                (b shr 0 and 0x1).toByte()
    }*/

    fun BufferedSource.readUnsignedByte(): Int {
        return (readByte().toInt() and 0x000000FF)
    }


    fun Byte.toBit(): String {
        val b = this.toInt() and 0x000000FF
        return "" + (b shr 7 and 0x1).toByte() +
                (b shr 6 and 0x1).toByte() +
                (b shr 5 and 0x1).toByte() +
                (b shr 4 and 0x1).toByte() +
                (b shr 3 and 0x1).toByte() +
                (b shr 2 and 0x1).toByte() +
                (b shr 1 and 0x1).toByte() +
                (b shr 0 and 0x1).toByte()
    }
}