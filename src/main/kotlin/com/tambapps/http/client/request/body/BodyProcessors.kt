package com.tambapps.http.client.request.body

import com.tambapps.http.client.response.HttpHeaders
import com.tambapps.http.client.util.IOUtils
import com.tambapps.http.client.util.ISSupplier
import java.io.*
import java.net.URLConnection

/**
 * Util class implementing different [BodyProcessor]
 */
object BodyProcessors {

    @JvmStatic
    fun string(content: String): BodyProcessor {
        return StringBodyProcessor(content)
    }

    @JvmOverloads
    fun multipartFile(file: File, key: String = file.name, bufferSize: Int = IOUtils.DEFAULT_BUFFER_SIZE): BodyProcessor {
        return MultipartFileBodyProcessor(file, key, bufferSize)
    }

    @JvmOverloads
    fun multipartBytes(bytes: ByteArray, name: String, key: String = name, bufferSize: Int = IOUtils.DEFAULT_BUFFER_SIZE): BodyProcessor {
        return MultipartBytesBodyProcessor(bytes, name, key, bufferSize)
    }

    @JvmOverloads
    fun multipartStream(isSupplier: ISSupplier, name: String, bufferSize: Int = IOUtils.DEFAULT_BUFFER_SIZE): BodyProcessor {
        return multipartStream(isSupplier, name, name, bufferSize)
    }

    @JvmOverloads
    fun multipartStream(isSupplier: ISSupplier, name: String, key: String, bufferSize: Int = IOUtils.DEFAULT_BUFFER_SIZE): BodyProcessor {
        return MultipartInputStreamBodyProcessor(isSupplier, key, name, bufferSize)
    }

    @JvmStatic
    fun bytes(bytes: ByteArray): BodyProcessor {
        return BytesBodyProcessor(bytes)
    }

    @JvmStatic
    fun stream(isSupplier: ISSupplier): BodyProcessor {
        return InputStreamBodyProcessor(isSupplier)
    }

    @JvmStatic
    fun file(file: File): BodyProcessor {
        return FileBodyProcessor(file)
    }

    private class StringBodyProcessor(private val content: String) : AbstractBodyProcessor() {
        @Throws(IOException::class)
        public override fun writeContent(oStream: OutputStream) {
            OutputStreamWriter(oStream).use { wr ->
                wr.write(content)
                wr.flush()
            }
        }
    }

    private abstract class MultipartBodyProcessor internal constructor(private val name: String, private val key: String, private val bufferSize: Int) : AbstractBodyProcessor() {
        private val boundary = "*****"
        private val crlf = "\r\n"
        private val twoHyphens = "--"

        @Throws(IOException::class)
        override fun writeContent(oStream: OutputStream) {
            DataOutputStream(
                    oStream).use { request ->
                request.writeBytes(twoHyphens + boundary + crlf)
                request.writeBytes("Content-Disposition: form-data; name=\"" +
                        key + "\";filename=\"" +
                        name + "\"" + crlf)
                request.writeBytes(crlf)
                writeMultipart(request, bufferSize)
                request.writeBytes(crlf)
                request.writeBytes(twoHyphens + boundary + twoHyphens + crlf)
                request.flush()
            }
        }

        @Throws(IOException::class)
        abstract fun writeMultipart(request: DataOutputStream, bufferSize: Int)
        override fun prepareURLConnection(connection: URLConnection) {
            connection.useCaches = false
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty(
                    HttpHeaders.CONTENT_TYPE_HEADER, "multipart/form-data;boundary=$boundary")
        }

    }

    private abstract class MultipartStreamBodyProcessor internal constructor(name: String, key: String, bufferSize: Int) : MultipartBodyProcessor(name, key, bufferSize) {
        @Throws(IOException::class)
        override fun writeMultipart(request: DataOutputStream, bufferSize: Int) {
            inputStream.use { iStream -> IOUtils.copy(iStream, request, bufferSize) }
        }

        @get:Throws(IOException::class)
        abstract val inputStream: InputStream
    }

    private class MultipartFileBodyProcessor internal constructor(private val file: File, key: String, bufferSize: Int) : MultipartStreamBodyProcessor(file.name, key, bufferSize) {
        @get:Throws(IOException::class)
        override val inputStream: InputStream
            get() = FileInputStream(file)
    }

    private class MultipartInputStreamBodyProcessor internal constructor(private val isSupplier: ISSupplier, name: String, key: String, bufferSize: Int) : MultipartStreamBodyProcessor(name, key, bufferSize) {
        @get:Throws(IOException::class)
        override val inputStream: InputStream
            get() = isSupplier.get()
    }

    private class MultipartBytesBodyProcessor internal constructor(private val bytes: ByteArray,
                                                                           name: String, key: String, bufferSize: Int) : MultipartBodyProcessor(name, key, bufferSize) {
        @Throws(IOException::class)
        override fun writeMultipart(request: DataOutputStream, bufferSize: Int) {
            request.write(bytes)
        }
    }

    private class BytesBodyProcessor(private val bytes: ByteArray) : AbstractBodyProcessor() {
        @Throws(IOException::class)
        override fun writeContent(oStream: OutputStream) {
            oStream.write(bytes, 0, bytes.size)
        }
    }

    private class FileBodyProcessor(private val file: File) : AbstractBodyProcessor() {
        @Throws(IOException::class)
        override fun writeContent(oStream: OutputStream) {
            FileInputStream(file).use { iStream -> IOUtils.copy(iStream, oStream) }
        }
    }

    private class InputStreamBodyProcessor(private val supplier: ISSupplier) : AbstractBodyProcessor() {
        @Throws(IOException::class)
        override fun writeContent(oStream: OutputStream) {
            supplier.get().use { iStream -> IOUtils.copy(iStream, oStream) }
        }
    }
}