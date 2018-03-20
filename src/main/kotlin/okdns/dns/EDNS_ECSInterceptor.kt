package okdns.dns

import ktdns.core.message.Message
import ktdns.core.message.Record
import ktdns.interceptor.Interceptor
import ktdns.interceptor.chain.Chain

class EDNS_ECSInterceptor : Interceptor {
    override fun intercept(chain: Chain): Message {
        val message = chain.message

        message.additional.forEach {
            if (it.TYPE == Record.RecordType.EDNS) {
                it as Record.EDNSRecord
                it.optionDatas.forEach {
                    if (Record.EDNSRecord.ECS_DATA::class.java.isInstance(it)) {
                        it as Record.EDNSRecord.ECS_DATA
                        it.scopeNetMask = it.sourceNetMask
                    }
                }
            }
        }

        return chain.proceed(message)
    }
}