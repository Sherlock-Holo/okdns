package okdns

import okdns.client.Client
import okdns.config.Config

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("give me a config file")
        System.exit(1)
    }

    val config = Config(args[0])

    val client = Client(config.upstream, config.bootstrap, config.listen)

    client.start()
}