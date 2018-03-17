package okdns.client

import ktdns.server.Server
import okdns.bootstrap.BootstrapDns
import okdns.dns.DnsInterceptor
import okdns.logger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.net.InetSocketAddress

class Client(url: String, bootsrapAddress: InetSocketAddress, bindAddress: InetSocketAddress) {
    private val httpsClient = OkHttpClient.Builder()
            .dns(BootstrapDns(bootsrapAddress))
            .retryOnConnectionFailure(true)
            .build()

    private val dnsServer = Server()

    init {
        logger.fine("initing okdns")
        dnsServer.bindAddress = bindAddress
        dnsServer.addInterceptor(DnsInterceptor(httpsClient, HttpUrl.parse(url)!!))
    }

    fun start() = dnsServer.start().apply { logger.fine("start okdns") }
}