@file:OptIn(DelicateCoroutinesApi::class)

package moe.fuqiuluo.http.action.helper

import com.tencent.mobileqq.transfile.FileMsg
import com.tencent.mobileqq.transfile.TransferRequest
import com.tencent.mobileqq.transfile.api.ITransFileController
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mqq.app.AppRuntime
import mqq.app.MobileQQ
import oicq.wlogin_sdk.tools.MD5
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.random.Random

internal object HighwayHelper {
    const val SEND_MSG_BUSINESS_TYPE_AIO_ALBUM_PIC = 1031
    const val SEND_MSG_BUSINESS_TYPE_AIO_KEY_WORD_PIC = 1046
    const val SEND_MSG_BUSINESS_TYPE_AIO_QZONE_PIC = 1045
    const val SEND_MSG_BUSINESS_TYPE_ALBUM_PIC = 1007
    const val SEND_MSG_BUSINESS_TYPE_BLESS = 1056
    const val SEND_MSG_BUSINESS_TYPE_CAPTURE_PIC = 1008
    const val SEND_MSG_BUSINESS_TYPE_COMMEN_FALSH_PIC = 1040
    const val SEND_MSG_BUSINESS_TYPE_CUSTOM = 1006
    const val SEND_MSG_BUSINESS_TYPE_DOUTU_PIC = 1044
    const val SEND_MSG_BUSINESS_TYPE_FALSH_PIC = 1039
    const val SEND_MSG_BUSINESS_TYPE_FAST_IMAGE = 1037
    const val SEND_MSG_BUSINESS_TYPE_FORWARD_EDIT = 1048
    const val SEND_MSG_BUSINESS_TYPE_FORWARD_PIC = 1009
    const val SEND_MSG_BUSINESS_TYPE_FULL_SCREEN_ESSENCE = 1057
    const val SEND_MSG_BUSINESS_TYPE_GALEERY_PIC = 1041
    const val SEND_MSG_BUSINESS_TYPE_GAME_CENTER_STRATEGY = 1058
    const val SEND_MSG_BUSINESS_TYPE_HOT_PIC = 1042
    const val SEND_MSG_BUSINESS_TYPE_MIXED_PICS = 1043
    const val SEND_MSG_BUSINESS_TYPE_PIC_AIO_ALBUM = 1052
    const val SEND_MSG_BUSINESS_TYPE_PIC_CAMERA = 1050
    const val SEND_MSG_BUSINESS_TYPE_PIC_FAV = 1053
    const val SEND_MSG_BUSINESS_TYPE_PIC_SCREEN = 1027
    const val SEND_MSG_BUSINESS_TYPE_PIC_SHARE = 1030
    const val SEND_MSG_BUSINESS_TYPE_PIC_TAB_CAMERA = 1051
    const val SEND_MSG_BUSINESS_TYPE_QQPINYIN_SEND_PIC = 1038
    const val SEND_MSG_BUSINESS_TYPE_RECOMMENDED_STICKER = 1047
    const val SEND_MSG_BUSINESS_TYPE_RELATED_EMOTION = 1054
    const val SEND_MSG_BUSINESS_TYPE_SHOWLOVE = 1036
    const val SEND_MSG_BUSINESS_TYPE_SOGOU_SEND_PIC = 1034
    const val SEND_MSG_BUSINESS_TYPE_TROOP_BAR = 1035
    const val SEND_MSG_BUSINESS_TYPE_WLAN_RECV_NOTIFY = 1055
    const val SEND_MSG_BUSINESS_TYPE_ZHITU_PIC = 1049
    const val SEND_MSG_BUSINESS_TYPE_ZPLAN_EMOTICON_GIF = 1060
    const val SEND_MSG_BUSINESS_TYPE_ZPLAN_PIC = 1059

    suspend fun transTroopVoice(
        groupId: String,
        file: File,
        wait: Boolean = true
    ): Boolean {
        val runtime = MobileQQ.getMobileQQ().waitAppRuntime()
        val transferRequest = TransferRequest()
        transferRequest.mSelfUin = runtime.currentAccountUin
        transferRequest.mPeerUin = groupId
        transferRequest.mUinType = FileMsg.UIN_TROOP
        transferRequest.mFileType = FileMsg.TRANSFILE_TYPE_PTT
        transferRequest.mUniseq = createMessageUniseq()
        transferRequest.mIsUp = true
        transferRequest.mLocalPath = file.absolutePath
        transferRequest.mBusiType = 1002
        transferRequest.mPttCompressFinish = true
        transferRequest.mPttUploadPanel = 3
        transferRequest.mIsPttPreSend = true
        return transAndWait(runtime, transferRequest, wait)
    }

    suspend fun transTroopPic(
        groupId: String,
        file: File,
        wait: Boolean = true
    ): Boolean {
        val runtime = MobileQQ.getMobileQQ().waitAppRuntime()
        val transferRequest = TransferRequest()
        transferRequest.needSendMsg = false
        transferRequest.mSelfUin = runtime.account
        transferRequest.mPeerUin = groupId
        transferRequest.mSecondId = runtime.currentAccountUin
        transferRequest.mUinType = FileMsg.UIN_TROOP
        transferRequest.mFileType = FileMsg.TRANSFILE_TYPE_PIC
        transferRequest.mUniseq = createMessageUniseq()
        transferRequest.mIsUp = true
        transferRequest.mBusiType = SEND_MSG_BUSINESS_TYPE_PIC_SHARE
        transferRequest.mMd5 = MD5.getFileMD5(file)
        transferRequest.mLocalPath = file.absolutePath
        val picUpExtraInfo = TransferRequest.PicUpExtraInfo()
        picUpExtraInfo.mIsRaw = true
        transferRequest.mPicSendSource = 8
        transferRequest.mExtraObj = picUpExtraInfo
        return transAndWait(runtime, transferRequest, wait)
    }

    private suspend fun transAndWait(
        runtime: AppRuntime,
        transferRequest: TransferRequest,
        wait: Boolean
    ): Boolean {
        val service = runtime.getRuntimeService(ITransFileController::class.java, "all")
        if(service.transferAsync(transferRequest)) {
            if (wait) {
                return true
            }
            return suspendCancellableCoroutine { continuation ->
                val waiter = GlobalScope.launch {
                    while (
                        service.findProcessor(transferRequest.keyForTransfer) != null
                    ) {
                        delay(1000)
                    }
                    continuation.resume(true)
                }
                continuation.invokeOnCancellation {
                    waiter.cancel()
                    continuation.resume(false)
                }
            }
        }
        return false
    }

    private fun createMessageUniseq(time: Long = System.currentTimeMillis()): Long {
        var uniseq = (time / 1000).toInt().toLong()
        uniseq = uniseq shl 32 or abs(Random.nextInt()).toLong()
        return uniseq
    }

}