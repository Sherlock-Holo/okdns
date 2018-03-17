package okdns.client

import okdns.logger
import java.net.InetSocketAddress
import java.util.logging.Level

fun main(args: Array<String>) {
    val doh_client = Client(
            "https://neatdns.ustclug.org/resolve",
            InetSocketAddress("101.6.6.6", 53),
            InetSocketAddress(5454)
    )

    logger.level = Level.ALL

    doh_client.start()
}