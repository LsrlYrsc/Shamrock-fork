package moe.fuqiuluo.http.action.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MessageResult(
    @SerialName("message_id") val msgId: Long,
    @SerialName("time") val time: Double
)