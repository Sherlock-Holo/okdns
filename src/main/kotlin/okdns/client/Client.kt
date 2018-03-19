package okdns.client

import ktdns.server.Server
import okdns.bootstrap.BootstrapDns
import okdns.dns.EDNS_ECSInterceptor
import okdns.dns.HttpsInterceptor
import okdns.logger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.net.InetSocketAddress

class Client(url: String, bootsrapAddress: InetSocketAddress, bindAddress: InetSocketAddress, EDNS_ECS: Boolean) {
    private val httpsClient = OkHttpClient.Builder()
            .dns(BootstrapDns(bootsrapAddress))
            .retryOnConnectionFailure(true)
            .build()

    private val dnsServer = Server()

    init {
        logger.info("initing okdns")
        dnsServer.bindAddress = bindAddress
        dnsServer
                .addInterceptor(HttpsInterceptor(httpsClient, HttpUrl.parse(url)!!, EDNS_ECS))
                .addInterceptor(EDNS_ECSInterceptor())
    }

    fun start() = dnsServer.start().apply { logger.info("start okdns") }
}