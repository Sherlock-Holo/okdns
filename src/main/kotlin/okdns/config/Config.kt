package okdns.config

import com.moandjiezana.toml.Toml
import java.io.File
import java.net.InetSocketAddress

class Config(path: String) {
    private val file = File(path)
    private val toml = Toml().read(file)!!.toMap()

    init {
        if (!file.exists()) System.exit(1)
    }

    val upstream get() = toml["upstream"] as String

    private val bootstrapInfo get() = toml["bootstrap"] as String
    private val listenInfo get() = toml["listen"] as String

    val bootstrap = InetSocketAddress(bootstrapInfo.split('#')[0], bootstrapInfo.split('#')[1].toInt())
    val listen = InetSocketAddress(listenInfo.split('#')[0], listenInfo.split('#')[1].toInt())

    val edns_ecs get() = toml["edns_ecs"] as Boolean
}