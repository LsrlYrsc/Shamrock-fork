package moe.fuqiuluo.http.action.helper.codec

import android.media.MediaExtractor
import android.media.MediaFormat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.arthenica.ffmpegkit.ReturnCode
import de.robv.android.xposed.XposedBridge
import moe.fuqiuluo.http.action.helper.FileHelper
import oicq.wlogin_sdk.tools.MD5
import java.io.File
import kotlin.math.roundToInt

enum class MediaType {
    Mp3,
    Amr,
    M4a,
    Pcm,
    Silk,
    Wav,
    Flac,
}

internal object AudioUtils {
    private val SampleRateMap = intArrayOf(8000, 12000, 16000, 24000, 36000, 44100, 48000)
    private val sampleRate: Int
        get() = SampleRateMap[3]

    internal fun audioToSilk(audio: File): Pair<Int, File> {
        val md5 = MD5.getFileMD5(audio)
        val silkFile = FileHelper.getFile("silk_$md5")
        if (silkFile.exists()) {
            return getDurationSec(audio) to silkFile
        }
        val pcmFile = audioToPcm(audio)
        var duration: Int
        pcmToSilk(pcmFile).let {
            pcmFile.delete()
            it.first.renameTo(silkFile)
            duration = (it.second).roundToInt()
        }
        if (duration < 1000) {
            duration = 1000
        }
        return duration to silkFile
    }

    internal fun pcmToSilk(file: File): Pair<File, Double> {
        val tmpFile = FileHelper.getTmpFile("silk", false)
        return tmpFile to pcmToSilk(sampleRate, 2, file.absolutePath, tmpFile.absolutePath)
    }

    fun audioToPcm(audio: File): File {
        val tmp = FileHelper.getTmpFile("pcm", false)
        val ffmpegCommand = "-y -i $audio -f s16le -acodec pcm_s16le -ac 1 -ar $sampleRate $tmp"
        val session = FFmpegKit.execute(ffmpegCommand)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            error("mp3 to pcm error: ${session.allLogsAsString}")
        }
        return tmp
    }

    fun getDurationSec(audio: File): Int {
        return getDuration(audio)
    }

    fun getDuration(audio: File): Int {
        val session = FFprobeKit.getMediaInformation(audio.absolutePath)
        val mediaInformation = session.mediaInformation
        val returnCode: ReturnCode = session.returnCode
        return if (ReturnCode.isSuccess(returnCode) && mediaInformation.duration != null) {
            mediaInformation.duration.split(".")[0].toInt()
        } else {
            1
        }
    }

    fun getMediaType(file: File): MediaType {
        if(FileHelper.isSilk(file)) {
            return MediaType.Silk
        }

        kotlin.runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            var formatMime = ""
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                when(val mime = format.getString(MediaFormat.KEY_MIME)) {
                    "audio/mp4a-latm" -> return MediaType.M4a
                    "audio/amr-wb", "audio/amr", "audio/3gpp" -> return MediaType.Amr
                    "audio/raw" , "audio/wav" -> return MediaType.Wav
                    "audio/mpeg_L2", "audio/mpeg_L1", "audio/mpeg", "audio/mpeg2" -> return MediaType.Mp3
                    "audio/flac" -> return MediaType.Flac
                    else -> {
                        if (mime?.startsWith("audio/") == true) formatMime = mime
                    }
                }
            }
            extractor.release()
        }

        return MediaType.Pcm
    }

    private external fun pcmToSilk(rate: Int, type: Byte, pcmFile: String, silkFile: String): Double
}