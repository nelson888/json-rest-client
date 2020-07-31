package com.tambapps.http.restclient.request

import com.tambapps.http.restclient.request.body.BodyProcessor
import com.tambapps.http.restclient.response.HttpHeaders
import java.util.*

/**
 * Class that holds REST request data
 */
class RestRequest private constructor(endpoint: String?, headers: Map<String, String>, method: String?, timeout: Int?,
                                      bodyProcessor: BodyProcessor?) {
    /**
     * Returns the endpoint
     * @return the endpoint
     */
    val endpoint: String

    /**
     * Returns the headers
     * @return the headers
     */
    val headers: Map<String, String>

    /**
     * Returns the method
     * @return the method
     */
    val method: String

    /**
     * Returns the timeout
     * @return the timeout
     */
    val timeout: Int?
    val outputProcessor: BodyProcessor?

    /**
     * Returns whether the output of the request will be handled or not
     * @return whether the output of the request will be handled or not
     */
    fun hasOutput(): Boolean {
        return outputProcessor != null
    }

    /**
     * Class allowing to build a REST request
     */
    class Builder(endpoint: String?) {
        private val headers: MutableMap<String, String> = HashMap()
        private val parameters: MutableMap<String, Any> = HashMap()
        private val endpoint: String
        private var method: String = HttpMethods.GET
        private var timeout: Int? = null
        private var bodyProcessor: BodyProcessor? = null

        /**
         * Sets the time out of the request (null means no timeout)
         * @param durationInMillis the duration in milliseconds
         * @return this
         */
        fun timeout(durationInMillis: Int?): Builder {
            timeout = durationInMillis
            return this
        }

        /**
         * Sets the method of the request
         * @param method the method
         * @return this
         */
        fun method(method: String): Builder {
            this.method = method
            return this
        }

        /**
         * Sets the method of the request to 'GET'
         * @return this
         */
        fun GET(): Builder {
            return method(HttpMethods.GET)
        }

        /**
         * Sets the method of the request to 'DELETE'
         * @return this
         */
        fun DELETE(): Builder {
            return method(HttpMethods.DELETE)
        }

        /**
         * Sets the method of the request to 'PUT'
         * @return this
         */
        fun PUT(): Builder {
            return method(HttpMethods.PUT)
        }

        /**
         * Sets the method of the request to 'POST'
         * @return this
         */
        fun POST(): Builder {
            return method(HttpMethods.POST)
        }

        /**
         * Sets the method of the request to 'PATCH'
         * @return this
         */
        fun PATCH(): Builder {
            return method(HttpMethods.PATCH)
        }

        /**
         * Sets the body processor of this request
         * @param bodyProcessor the body processor
         * @return this
         */
        fun body(bodyProcessor: BodyProcessor?): Builder {
            this.bodyProcessor = bodyProcessor
            return this
        }

        /**
         * Sets a header for this request
         * @param name the name of the header
         * @param value the value of the header
         * @return this
         */
        fun header(name: String, value: String): Builder {
            headers[name] = value
            return this
        }

        /**
         * Sets the 'Content-Type' header to json
         * @return this
         */
        fun jsonBody(): Builder {
            headers[HttpHeaders.CONTENT_TYPE_HEADER] = HttpHeaders.JSON_TYPE
            return this
        }

        /**
         * Sets the 'Accept' header to json
         * @return this
         */
        fun acceptJson(): Builder {
            headers[HttpHeaders.ACCEPT_HEADER] = HttpHeaders.JSON_TYPE
            return this
        }

        /**
         * Sets the 'Accept' and 'Content-Type' header to json
         * @return this
         */
        fun json(): Builder {
            jsonBody()
            acceptJson()
            return this
        }

        /**
         * Adds the given headers to the request
         * @param headers the headers
         * @return this
         */
        fun headers(headers: Map<String, String>?): Builder {
            this.headers.putAll(headers!!)
            return this
        }

        /**
         * Add the pairs (name, value) as headers to this request
         * @param args the pairs (name, value)
         */
        fun headers(vararg args: String) {
            require(args.size % 2 == 0) { "Should have pairs of (entry, value)" }
            for (i in 0 until args.size / 2) {
                header(args[i], args[i + 1])
            }
        }

        /**
         * Adds an url parameter to this request
         * @param urlParameter the url parameter
         * @param value the value of this parameter
         * @return this
         */
        fun parameter(urlParameter: String, value: Any): Builder {
            parameters[urlParameter] = value
            return this
        }

        /**
         * Adds url parameters to this request
         * @param parameters the url parameters an their values
         * @return this
         */
        fun parameters(parameters: Map<String, Any>?): Builder {
            this.parameters.putAll(parameters!!)
            return this
        }

        /**
         * Build the rest request
         * @return the rest request
         */
        fun build(): RestRequest {
            return RestRequest(endpointWithParameters(), headers, method, timeout, bodyProcessor)
        }

        private fun endpointWithParameters(): String {
            val builder = StringBuilder().append(endpoint)
            if (parameters.isNotEmpty()) {
                builder.append('?')
                for ((key, value) in parameters) {
                    builder.append(String.format("%s=%s", key, value))
                    builder.append('&')
                }
                builder.deleteCharAt(builder.length - 1) //delete last '&'
            }
            return builder.toString()
        }

        init {
            this.endpoint = endpoint ?: ""
        }
    }

    companion object {
        /**
         * Returns a new request builder for a given endpoint
         * @param endpoint the endpoint of the request
         * @return the request builde
         */
        @JvmStatic
        fun builder(endpoint: String?): Builder {
            return Builder(endpoint)
        }
    }

    init {
        requireNotNull(endpoint) { "URL cannot be null" }
        requireNotNull(method) { "method cannot be null" }
        this.endpoint = endpoint
        this.headers = Collections.unmodifiableMap(headers)
        this.method = method
        this.timeout = timeout
        outputProcessor = bodyProcessor
    }
}