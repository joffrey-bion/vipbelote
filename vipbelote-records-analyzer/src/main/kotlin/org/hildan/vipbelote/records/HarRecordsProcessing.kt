package org.hildan.vipbelote.records

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.hildan.har.*
import org.hildan.vipbelote.protocol.messages.*
import org.hildan.vipbelote.state.*
import java.net.*
import java.nio.file.*
import kotlin.io.path.*

private val isRootProject = Path(".").absolute().normalize().name == "vipbelote"
private val dataDir = if (isRootProject) Path("./data") else Path("../data")
private val rawHarRecordingsDir = dataDir.resolve("har-raw").createDirectories()
private val filteredHarRecordingsDir = dataDir.resolve("har-filtered").createDirectories()
private val decodedDataDir = dataDir.resolve("decoded").createDirectories()

fun main() {
    check(dataDir.exists()) { "The data directory $dataDir was not found" }
    filterRawHarFiles()
    filteredHarRecordingsDir.listDirectoryEntries().forEach(::processHarFile)
}

private fun filterRawHarFiles() {
    val json = Json { prettyPrint = true }
    rawHarRecordingsDir.listDirectoryEntries().forEach { harFile ->
        println("Filtering requests with WS traffic from raw HAR file $harFile")
        val filteredHar = harFile.parseHar().filterRequestsWithWsMessages()
        filteredHarRecordingsDir.resolve(harFile.fileName).writeText(json.encodeToString<Har>(filteredHar))
        harFile.deleteExisting()
    }
}

private fun Har.filterRequestsWithWsMessages(): Har = copy(log = log.filterRequestsWithWsMessages())

private fun HarLog.filterRequestsWithWsMessages(): HarLog = copy(entries = entries.filter { it.webSocketMessages != null })

private fun processHarFile(harFilePath: Path) {
    println("Processing HAR File $harFilePath...")
    var gameState: GameState = GameState.Initial(HildanPlayerId)
    harFilePath.parseHar()
        .recordsSequence()
        .onEach {
            if (it.message is GameMessage) {
                val newState = gameState.updatedWith(it.message)
                if (newState != gameState) {
                    gameState = newState
                    println(gameState)
                    println("_________________________________")
                }
            }
        }
        .groupBy { it.namespace }
        .forEach { (namespace, records) ->
            val unknownMessages = records.map { it.message }.filterIsInstance<UnknownMessage>()
            if (unknownMessages.isNotEmpty()) {
                error("Unknown message types need to be added:\n${unknownMessages.joinToString("\n")}")
            }
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
    val formattedRows = records.map { it.formatted() }
    decodedRecordsFile.writeLines(formattedRows)
}

private fun Record.formatted() = "$direction\t$namespace\t$message"

private fun sanitizeForPath(namespace: String): String? =
    if (namespace == "/") "default" else URLEncoder.encode(namespace.drop(1), Charsets.UTF_8)
