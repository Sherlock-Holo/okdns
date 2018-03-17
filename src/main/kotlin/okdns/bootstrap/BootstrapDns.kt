package okdns.bootstrap

import ktdns.core.message.Message
import ktdns.core.parse.Parse
import okdns.logger
import okhttp3.Dns
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class BootstrapDns(private val bootstrapNameserverAddress: InetSocketAddress, private val v6: Boolean = false) : Dns {
    private var address: MutableList<InetAddress>? = null
    private val bootstrapNameserver = DatagramSocket()
    private val parse = Parse()

    override fun lookup(hostname: String): MutableList<InetAddress> {
        if (address == null) {
            val question = Message.Question().apply {
                this.QCLASS = 1
                this.QNAME = "$hostname."
                this.QTYPE =
                        if (v6) 28
                        else 1
            }

            val message = Message.buildQuery(question)
            val byteArray = message.byteArray
            val datagramPacket = DatagramPacket(byteArray, byteArray.size, bootstrapNameserverAddress)

            val answerByteArray = ByteArray(512)
            val answerPacket: DatagramPacket

//            logger.info(message.header.QDCOUNT.toString())

            try {
                bootstrapNameserver.send(datagramPacket)

                answerPacket = DatagramPacket(answerByteArray, answerByteArray.size, bootstrapNameserverAddress)
                bootstrapNameserver.receive(answerPacket)
                logger.info("get https IP from custom nameserver")
            } catch (e: IOException) {
                logger.warning("get https IP from custom nameserver failed, use system nameserver")
                return Dns.SYSTEM.lookup(hostname).apply { address = this }
            }

            val answerMessage = parse.parseAnswer(answerByteArray)

            var aAnswer: Message.Companion.AAnswer? = null
            var aaaaAnswer: Message.Companion.AAAAAnswer? = null
            answerMessage.answers.forEach {
                when (it.TYPE) {
                    Message.Companion.AnswerType.A -> aAnswer = Message.Companion.AAnswer(it.NAME, it.CLASS, it.TTL, InetAddress.getByAddress(it.RDATA))

                    Message.Companion.AnswerType.AAAA -> aaaaAnswer = Message.Companion.AAAAAnswer(it.NAME, it.CLASS, it.TTL, InetAddress.getByAddress(it.RDATA))

                    else -> {
                    }
                }
            }

            val result = ArrayList<InetAddress>()
            if (aaaaAnswer != null) result.add(aaaaAnswer!!.address)
            if (aAnswer != null) result.add(aAnswer!!.address)
            return if (result.isEmpty()) Dns.SYSTEM.lookup(hostname).apply {
                address = this
                logger.warning("custom nameserver return empty result")
            }
            else {
                address = result.toMutableList()
                address!!
            }
        } else {
            return address!!
        }
    }
}