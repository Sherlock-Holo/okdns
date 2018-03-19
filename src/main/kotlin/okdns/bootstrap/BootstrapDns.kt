package okdns.bootstrap

import ktdns.core.message.Message
import ktdns.core.message.Record
import ktdns.example.SimpleQuery
import okdns.OkdnsException
import okhttp3.Dns
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class BootstrapDns(private val bootstrapNameserverAddress: InetSocketAddress, private val v6: Boolean = false) : Dns {
    private val addressMap = ConcurrentHashMap<String, MutableList<InetAddress>>()

    override fun lookup(hostname: String) = addressMap[hostname] ?: realLookup(hostname)

    private fun realLookup(hostname: String): MutableList<InetAddress> {
        val question = Message.Question().apply {
            this.QCLASS = 1
            this.QNAME = "$hostname."
            this.QTYPE =
                    if (v6) 28
                    else 1
        }

        val queryMessage = Message.buildQuery(question)
        val answerMessage = try {
            SimpleQuery.query(queryMessage, bootstrapNameserverAddress)
        } catch (e: IOException) {
            return lookup(hostname)
        }

        var aAnswer: Record.AAnswer? = null
        var aaaaAnswer: Record.AAAAAnswer? = null
        answerMessage.answers.forEach {
            when (it.TYPE) {
                Record.RecordType.A -> aAnswer = Record.AAnswer(it.NAME, it.CLASS, it.TTL, InetAddress.getByAddress(it.RDATA))

                Record.RecordType.AAAA -> aaaaAnswer = Record.AAAAAnswer(it.NAME, it.CLASS, it.TTL, InetAddress.getByAddress(it.RDATA))

                else -> {
                }
            }
        }

        val result = ArrayList<InetAddress>()
        if (aaaaAnswer != null) result.add(aaaaAnswer!!.address)
        if (aAnswer != null) result.add(aAnswer!!.address)

        if (result.isEmpty()) throw OkdnsException("bootstrap failed")
        else {
            addressMap[hostname] = result.toMutableList()
            return addressMap[hostname]!!
        }
    }
}