package okdns.bootstrap

import ktdns.core.message.Message
import ktdns.core.parse.Parse
import okhttp3.Dns
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class BootstrapDns(private val bootstrapNameserverAddress: InetSocketAddress) : Dns {
    private var address: MutableList<InetAddress>? = null
    private val bootstrapNameserver = DatagramSocket()
    private val parse = Parse()

    override fun lookup(hostname: String): MutableList<InetAddress> {
        if (address == null) {
            val aQuestion = Message.Question().apply {
                this.QCLASS = 1
                this.QTYPE = 1
                this.QNAME = hostname
            }

            val aaaaQuestion = Message.Question().apply {
                this.QCLASS = 1
                this.QTYPE = 28
                this.QNAME = hostname
            }

            val message = Message.buildQuery(arrayOf(aQuestion, aaaaQuestion))
            val byteArray = message.byteArray
            val datagramPacket = DatagramPacket(byteArray, byteArray.size, bootstrapNameserverAddress)
            bootstrapNameserver.send(datagramPacket)
            val answerByteArray = ByteArray(512)
            val answerPacket = DatagramPacket(answerByteArray, answerByteArray.size, bootstrapNameserverAddress)
            bootstrapNameserver.receive(answerPacket)
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
            return if (result.isEmpty()) Dns.SYSTEM.lookup(hostname).apply { address = this }
            else {
                address = result.toMutableList()
                address!!
            }
        } else {
            return address!!
        }
    }
}