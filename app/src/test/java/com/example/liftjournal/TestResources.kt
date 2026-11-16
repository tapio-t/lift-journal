package com.example.liftjournal

/** Reads a file from /testdata, which is on the unit-test classpath. */
fun readTestResource(name: String): String {
    val stream =
        requireNotNull(object {}.javaClass.classLoader?.getResourceAsStream(name)) { "Missing test resource $name" }
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

/** Reads a file from /testdata as raw bytes. */
fun readTestResourceBytes(name: String): ByteArray {
    val stream =
        requireNotNull(object {}.javaClass.classLoader?.getResourceAsStream(name)) { "Missing test resource $name" }
    return stream.use { it.readBytes() }
}
