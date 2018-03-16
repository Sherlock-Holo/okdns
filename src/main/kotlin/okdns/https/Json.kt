package okdns.https

import com.google.gson.Gson

class Json {

    /**
     * Status : 0
     * TC : false
     * RD : true
     * RA : true
     * AD : false
     * CD : false
     * Question : [{"name":"www.google.com.","type":28}]
     * Answer : [{"name":"www.google.com.","type":28,"TTL":299,"data":"2607:f8b0:4007:803::2004"}]
     * Comment : Response from 216.239.36.10.
     */

    var Status: Int = -1
    var TC: Boolean = false
    var RD: Boolean = false
    var RA: Boolean = false
    var AD: Boolean = false
    var CD: Boolean = false
    var Comment: String? = null
    var Question: List<QuestionBean>? = null
    var Answer: List<AnswerBean>? = null

    class QuestionBean {
        /**
         * name : www.google.com.
         * type : 28
         */

        var name: String? = null
        var type: Int = -1
    }

    class AnswerBean {
        /**
         * name : www.google.com.
         * type : 28
         * TTL : 299
         * data : 2607:f8b0:4007:803::2004
         */

        var name: String? = null
        var type: Int = -1
        var TTL: Int = -1
        var data: String? = null
    }

    companion object {
        private val gson = Gson()
        fun parse(json: String) = gson.fromJson(json, Json::class.java)!!
    }
}