package org.hildan.vipbelote.records

import kotlinx.serialization.json.*
import org.hildan.har.*
import org.hildan.vipbelote.protocol.messages.*
import org.hildan.vipbelote.state.*
import java.net.*
import java.nio.file.*
import kotlin.io.path.*

private val isRootProject = Path(".").absolute().normalize().name == "vipbelote"
private val dataDir = if (isRootProject) Path("./data") else Path("../data")

private val rawHarRecordingsDir = dataDir / "har-raw"
private val filteredHarRecordingsDir = dataDir / "har-filtered"
private val decodedDataDir = dataDir / "decoded"

private val prettyJson = Json { prettyPrint = true }

fun main() {
    check(dataDir.exists()) { "The data directory $dataDir was not found" }
    if (rawHarRecordingsDir.exists()) {
        rawHarRecordingsDir.listDirectoryEntries().forEach(::filterWebSocketTraffic)
    }
    filteredHarRecordingsDir.listDirectoryEntries().forEach(::decodeHarFile)
}

private fun filterWebSocketTraffic(harFile: Path) {
    println("Filtering requests with WS traffic from raw HAR file $harFile")
    val filteredHar = harFile.parseHar(prettyJson).filterWebSocketTraffic()
    filteredHarRecordingsDir.resolve(harFile.name).writeHar(filteredHar, prettyJson)
    harFile.deleteExisting()
}

private fun Har.filterWebSocketTraffic(): Har = copy(log = log.filterWebSocketTraffic())
private fun HarLog.filterWebSocketTraffic(): HarLog = copy(entries = entries.filter { it.webSocketMessages != null })

private fun decodeHarFile(harFilePath: Path) {
    println("Decoding VIPBelote messages from HAR File $harFilePath...")
    val outputDir = (decodedDataDir / harFilePath.nameWithoutExtension).createDirectories()
    val statePrinter = StatePrinter(HildanPlayerId)
    harFilePath.parseHar()
        .recordsSequence()
        .onEach { statePrinter.feed(it.message) }
        .groupBy { it.namespace }
        .forEach { (namespace, records) ->
            val unknownMessages = records.map { it.message }.filterIsInstance<UnknownMessage>()
            if (unknownMessages.isNotEmpty()) {
                error("Unknown message types need to be added:\n${unknownMessages.joinToString("\n")}")
            }
            val recordsFile = outputDir / "${sanitizeForPath(namespace = namespace)}.txt"
            recordsFile.writeRecords(records = records)
        }
}

private class StatePrinter(selfPlayerId: String) {
    private var state: GameState = GameState.Initial(selfPlayerId)

    fun feed(message: VipBeloteMessage) {
        if (message is GameMessage) {
            val newState = state.updatedWith(message)
            println(message)
            if (newState != state) {
                println(">>")
                state = newState
                println(state)
                println("──────────────────────────────────")
            }
        }
    }
}

private fun sanitizeForPath(namespace: String): String? =
    if (namespace == "/") "default" else URLEncoder.encode(namespace.drop(1), Charsets.UTF_8)

private fun Path.writeRecords(records: List<Record>) {
    writeLines(records.map { "${it.direction}\t${it.namespace}\t${it.message}" })
}
