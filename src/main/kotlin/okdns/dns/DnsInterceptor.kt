package okdns.dns

import ktdns.core.message.Message
import ktdns.core.message.Record
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.Chain
import okdns.https.Json
import okdns.logger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress

class DnsInterceptor(private val client: OkHttpClient, private val url: HttpUrl) : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message

        val question = message.questions[0]
        val queryUrl = url.newBuilder()
                .addQueryParameter("name", question.QNAME)
                .addQueryParameter("type", question.QTYPE.toString())
                .build()

        val request = Request.Builder().url(queryUrl).build()

        val call = client.newCall(request)
        val response = try {
            call.execute()
        } catch (e: IOException) {
            logger.warning(e.message)
            return chain.proceed(message.apply {
                this.header.RCODE = 2
                logger.warning("dns over https server error")
            })
        }

        val jsonString = response.body()!!.string()
        val json = Json.parse(jsonString)

        logger.info("json string: $jsonString")

        if (json.Status != 0) return chain.proceed(message.apply {
            this.header.RCODE = 3
            logger.warning("dns over https status is ${json.Status}, not 0")
        })
        if (json.Answer == null && json.Authority == null) return chain.proceed(message.apply {
            this.header.RCODE = 3
            logger.warning("no answer")
        })

        val header = message.header

        header.TC =
                if (json.TC) 1
                else 0

        header.RA =
                if (json.RA) 1
                else 0



        json.Answer?.forEach {
            message.addAnswer(
                    when (it.type) {
                        1 -> Record.AAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!))

                        28 -> Record.AAAAAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!))

                        5 -> Record.CNAMEAnswer(it.name!!, 1, it.TTL, it.data!!)

                        else -> TODO("other type is not implement")
                    }
            )
        }

        json.Authority?.forEach {
            message.addAnswer(
                    when (it.type) {
                        1 -> Record.AAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!))

                        28 -> Record.AAAAAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!))

                        5 -> Record.CNAMEAnswer(it.name!!, 1, it.TTL, it.data!!)

                        else -> TODO("other type is not implement")
                    }
            )
        }

        logger.fine("return dns over https result")
        return chain.proceed(message)
    }
}