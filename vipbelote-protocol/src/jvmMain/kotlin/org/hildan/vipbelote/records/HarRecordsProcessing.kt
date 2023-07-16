package org.hildan.vipbelote.records

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.hildan.har.*
import java.net.*
import java.nio.file.*
import kotlin.io.path.*

/*
 VIP Belote HTTP upgrade request URL:
 wss://ws.vipbelote.fr/socket.io/?token=360807d6-e75f-4c75-8b18-b9c15c112684&EIO=3&transport=websocket

 This EIO=3 means we're using Engine.IO protocol version 3, with web socket transport.
 */

private val isRootProject = Path(".").absolute().name == "vipbelote"
private val dataDir = if (isRootProject) Path("./data") else Path("../data")
private val rawHarRecordingsDir = dataDir.resolve("har-raw").createDirectories()
private val trimmedHarRecordingsDir = dataDir.resolve("har-trimmed").createDirectories()
private val decodedDataDir = dataDir.resolve("decoded").createDirectories()

fun main() {
    trimRawHarFiles()
    trimmedHarRecordingsDir.listDirectoryEntries().forEach(::processHarFile)
}

private fun trimRawHarFiles() {
    val json = Json { prettyPrint = true }
    rawHarRecordingsDir.listDirectoryEntries().forEach { harFile ->
        println("Trimming raw HAR file $harFile")
        val trimmedHar = harFile.parseHar().filterRequestsWithWsMessages()
        trimmedHarRecordingsDir.resolve(harFile.fileName).writeText(json.encodeToString<Har>(trimmedHar))
        harFile.deleteExisting()
    }
}

private fun Har.filterRequestsWithWsMessages(): Har = copy(log = log.filterRequestsWithWsMessages())

private fun HarLog.filterRequestsWithWsMessages(): HarLog = copy(entries = entries.filter { it.webSocketMessages != null })

private fun processHarFile(harFilePath: Path) {
    println("Processing HAR File $harFilePath...")
    harFilePath.parseHar()
        .recordsSequence()
        .onEach { println(it.formatted()) }
        .groupBy { it.namespace }
        .forEach { (namespace, records) ->
            writeRecordsToFile(harFile = harFilePath, namespace = namespace, records = records)
        }
}

private fun writeRecordsToFile(
    harFile: Path,
    namespace: String,
    records: List<Record>,
) {
    val sanitizedNamespace = sanitizeForPath(namespace)
    val decodedRecordsFile = decodedDataDir.resolve("${harFile.nameWithoutExtension}/$sanitizedNamespace.txt")
    decodedRecordsFile.createParentDirectories()
    val formattedRows = records.map {it.formatted() }
    decodedRecordsFile.writeLines(formattedRows)
}

private fun Record.formatted() = "$direction\t$namespace\t$message"

private fun sanitizeForPath(namespace: String): String? =
    if (namespace == "/") "default" else URLEncoder.encode(namespace.drop(1), Charsets.UTF_8)
