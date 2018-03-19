package okdns.dns

import ktdns.core.message.Message
import ktdns.core.message.Record
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.Chain
import okdns.bootstrap.BootstrapDns
import okdns.https.Json
import okdns.logger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress

class HttpsInterceptor(
        private val client: OkHttpClient,
        private val url: HttpUrl,
        private val EDNS_ECS: Boolean
) : Interceptor {

    override fun intercept(chain: Chain): Message {
        val message = chain.message

        val question = message.questions[0]
        val queryUrl = url.newBuilder()
                .addQueryParameter("name", question.QNAME)
                .addQueryParameter("type", question.QTYPE.toString())
                .apply {
                    var addEDNS_ECS = false
                    if (EDNS_ECS) {
                        message.additional.forEach {
                            if (Record.EDNS_ECS::class.java.isInstance(it)) {
                                it as Record.EDNS_ECS
                                this.addQueryParameter("edns_client_subnet", it.realEDNSIP.hostAddress + "/${it.sourceNetMask}")
                                addEDNS_ECS = true
                            }
                        }

                        if (!addEDNS_ECS) {
                            this.addQueryParameter("edns_client_subnet", okdns.dns.HttpsInterceptor.myIP.hostAddress + "/24")
                        }
                    }
                }
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
            when (it.type) {
                1 -> message.addAnswer(Record.AAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!)))

                2 -> message.addNSRecord(Record.NSRecord(it.name!!, 1, it.TTL, it.data!!))

                5 -> message.addAnswer(Record.CNAMEAnswer(it.name!!, 1, it.TTL, it.data!!))

                28 -> message.addAnswer(Record.AAAAAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!)))

                else -> TODO("other type is not implement")
            }
        }

        json.Authority?.forEach {
            when (it.type) {
                1 -> message.addAnswer(Record.AAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!)))

                2 -> message.addNSRecord(Record.NSRecord(it.name!!, 1, it.TTL, it.data!!))

                5 -> message.addAnswer(Record.CNAMEAnswer(it.name!!, 1, it.TTL, it.data!!))

                28 -> message.addAnswer(Record.AAAAAnswer(it.name!!, 1, it.TTL, InetAddress.getByName(it.data!!)))

                else -> TODO("other type is not implement")
            }
        }

        logger.fine("return dns over https result")
        return chain.proceed(message)
    }

    companion object {
        private var myIPTTL = 0L

        private val ipClient = OkHttpClient.Builder()
                .dns(BootstrapDns(InetSocketAddress("101.6.6.6", 53)))
                .retryOnConnectionFailure(true)
                .build()

        private var myIP: InetAddress = InetAddress.getLocalHost()
            get() {
                return if (myIPTTL == 0L || (System.currentTimeMillis() - myIPTTL) > 60 * 1000) {
                    field = InetAddress.getByName(getNewMyIP())
                    myIPTTL = System.currentTimeMillis()
                    field
                } else field
            }

        private fun getNewMyIP(): String {
            val request = Request.Builder()
                    .url("http://myip.ipip.net")
                    .build()

            val callback = ipClient.newCall(request)
            return callback.execute().body()!!.string().split("  ")[0].run { return@run this.substring(6 until this.length) }
        }
    }
}