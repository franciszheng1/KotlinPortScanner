// Kotlin Imports -
// kotlin.concurrent.thread: allows launching multiple tasks concurrently (threads)
// java.net.InetSocketAddress: combines IP address and port for network connections
// java.net.Socket: enables creating TCP connections to check if a port is open
// java.net.SocketTimeoutException: handles cases where a port does not respond in time
// java.io.File: used to write the scan results into an HTML report file

import kotlin.concurrent.thread
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.io.File

// Data Class:
// PortResult holds all information about a single scanned port
// Data classes in Kotlin automatically provide useful methods like toString(), equals(), and copy()
data class PortResult(
    val port: Int,                      // Numeric port being scanned
    val service: String,                // Detected service (HTTP, SSH)
    val status: String,                 // "OPEN" if the port is responsive
    val recommendation: String,         // Suggested security action for this service
    val exampleLocalCommand: String     // Sample local command to secure this port/service
)
