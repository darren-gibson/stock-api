package org.darren.stock.steps.helpers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SnapshotData(
    val stockLevels: Map<String, Double>,
)

class TestSnapshotManager {
    private val snapshotFile = File("build/test-snapshot.json")

    fun createSnapshot(data: Map<String, Double>) {
        val snapshot = SnapshotData(data)
        val json = Json.encodeToString(snapshot)
        snapshotFile.parentFile.mkdirs()
        snapshotFile.writeText(json)
    }

    fun readSnapshot(): Map<String, Double> {
        if (!snapshotFile.exists()) return emptyMap()
        val json = snapshotFile.readText()
        val snapshot = Json.decodeFromString<SnapshotData>(json)
        return snapshot.stockLevels
    }

    fun deleteSnapshot() {
        if (snapshotFile.exists()) {
            snapshotFile.delete()
        }
    }

    fun snapshotExists(): Boolean = snapshotFile.exists()
}
