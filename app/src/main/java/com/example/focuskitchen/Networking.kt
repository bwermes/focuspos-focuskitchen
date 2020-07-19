package com.example.focuskitchen

import okhttp3.*
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody

class Networking {

    companion object {
        fun fetchJson(url: String, headerName: String, headerValue: String, performOnCallback: (Call, Response?, String) -> Unit) {
            val request = Request.Builder()
                .url(url)
                .header(headerName, headerValue)
                .build()
            println("GET $url")
            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.code != 200) {
                        println("${response.code} for GET $url")
                    } else {
                        println("${response.code} OK for GET $url")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        performOnCallback(call, response, responseBody)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    var errorMessage = ""
                    if (e.message != null) {
                        errorMessage = e.message as String
                    }
                    performOnCallback(call, null, errorMessage)
                    println("Failed on GET to $url")
                }
            })
        }


        fun postData(url: String, headerName: String, headerValue: String, payload: String, performOnCallback: (Call, Response?, String) -> Unit) {
            val requestBody = payload.toRequestBody()
            val request = Request.Builder()
                .url(url)
                .header(headerName, headerValue)
                .method("POST", requestBody)
                .build()
            println("POST $url")

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
//                    if (responseBody != null && response.isSuccessful) {
//                        performOnCallback(call, response, responseBody)
//                    }
                    if (response.code != 200) {
                        println("${response.code} for POST $url\nPayload:\n$payload")
                    } else {
                        println("${response.code} OK for POST $url")
                    }

                    performOnCallback(call, response, responseBody!!)
                }

                override fun onFailure(call: Call, e: IOException) {
                    println(e)
                    println("Failed on POST to $url")
                    performOnCallback(call, null, e.message.toString())
                }
            })
        }

        fun putData(url: String, headerName: String, headerValue: String, payload: String, performOnCallback: (Call, Response, String) -> Unit) {
            val requestBody = payload.toRequestBody()
            val request = Request.Builder()
                .url(url)
                .header(headerName, headerValue)
                .method("PUT", requestBody)
                .build()
            println("PUT $url")

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.code != 200) {
                        println("${response.code} for PUT $url\nPayload:\n$payload")
                    } else {
                        println("${response.code} OK for PUT $url")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null && response.isSuccessful) {
                        performOnCallback(call, response, responseBody)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed on PUT to $url")
                }
            })
        }
    }
}