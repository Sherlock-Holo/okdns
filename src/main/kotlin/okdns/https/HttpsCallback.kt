package okdns.https

import ktdns.core.message.Message
import ktdns.core.message.Record
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress

@Deprecated("useless class")
class HttpsCallback(private val message: Message) : Callback {
    override fun onFailure(call: Call, e: IOException) {
        e.printStackTrace()
    }

    override fun onResponse(call: Call, response: Response) {
        val jsonString = response.body()!!.string()
        val json = Json.parse(jsonString)
        if (json.Status != 0) return
        if (json.Answer == null) return
        json.Answer!!.forEach {
            when (it.type) {
                1 -> message.addAnswer(Record.AAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!)))

                2 -> message.addNSRecord(Record.NSRecord(it.name!!, 1, it.TTL, it.data!!))

                5 -> message.addAnswer(Record.CNAMEAnswer(it.name!!, 1, it.TTL, it.data!!))

                28 -> message.addAnswer(Record.AAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!)))

                else -> TODO("other type is not implement")
            }
        }
    }
}