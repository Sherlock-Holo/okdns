package okdns.dns

import ktdns.core.message.Message
import ktdns.core.message.Record
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.Chain

class EDNS_ECSInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message

        message.additional.forEach {
            if (Record.EDNS_ECS::class.java.isInstance(it)) {
                it as Record.EDNS_ECS
                it.scopeNetMask = it.sourceNetMask
            }
        }

        return chain.proceed(message)
    }
}