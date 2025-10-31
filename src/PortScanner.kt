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
    val service: String,                // Detected service (HTTP, SSH, MySQL, and more)
    val status: String,                 // "OPEN" or "CLOSED"
    val recommendation: String,         // Suggested security action for this service
    val exampleLocalCommand: String     // Sample local command to secure this port/service
)

// Function that attempts to connect to a given IP address and port to check if it is open
fun scanPort(ip: String, port: Int) : PortResult {
    // Determine the likely service running on the port
    // Helps provide meaningful recommendations later
    val service = when (port) {
        22 -> "SSH" // Secure Shell, used for remote administration
        80 -> "HTTP" // HyperText Transfer Protocol, used for web traffic
        443 -> "HTTPS" // Secure HTTP, encrypted web traffic
        3306 -> "MySQL" // MySQL database server
        21 -> "FTP" // File Transfer Protocol
        25 -> "SMTP" // Simple Mail Transfer Protocol, email sending
        else -> "Unknown" // Any port not explicitly recognized
    }

    return try {
        // Attempts to open a transmission control protocol (TCP) connection to the IP address and port with a 200ms timeout
        // A short timeout (200ms) is used to avoid long delays for closed ports
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), 200)
        }

        // If connection succeeds, the port is open, so we return recommendations
        // Provide security recommendations and example commands
        PortResult(
            port,
            service,
            "OPEN",
            when (port) { // Security recommendations for known services
                22 -> "Restrict SSH access to trusted IPs." // Limit SSH to specific IPs for security
                80, 443 -> "Keep web server updated and monitor traffic." // Patch updates + monitor traffic
                3306 -> "Restrict database access to local only." // Prevent external access to database
                21 -> "Secure FTP access or disable if unused." // Avoid open file transfer protocol, unless necessary
                25 -> "Monitor SMTP traffic for spam or abuse." // Check outgoing emails to avoid blacklisting
                else -> "Monitor service regularly." // Generic advice for unknown ports
            },
            when (port) { // Example commands for securing each service
                22 -> "sudo ufw allow from <trusted_IP> to any port 22" // Example firewall rule for SSH
                80, 443 -> "sudo apt update && sudo apt upgrade" // Update web server packages
                3306 -> "sudo ufw allow from 127.0.0.1 to any port 3306" // Restrict MySQL to localhost
                21 -> "sudo ufw deny 21" // Disable FTP access via firewall
                25 -> "sudo ufw deny 25" // Block SMTP access via firewall if not needed
                else -> "" // No command for unknown ports
            }
        )
    } catch (e: SocketTimeoutException) {
        // Port did not respond within 200ms, mark as CLOSED
        PortResult(port, service, "CLOSED", "", "")
    } catch (e: Exception) {
        // Any other error (connection refused, network error) is treated as CLOSED
        PortResult(port, service, "CLOSED", "", "")
    }
}

// Main function
// Coordinates scanning, collects results, and generates an HTML report
fun main() {
    val targetIP = "127.0.0.1" // Safe localhost scanning
    val portsToScan = listOf(21, 22, 25, 80, 443, 3306) // Common ports for demonstration

    val useRealScan = false // Set false to use demo mode with predefined results

    val results = mutableListOf<PortResult>() // Holds all scan results
    val threads = mutableListOf<Thread>() // For running concurrent scans

    if (useRealScan) {
        // Real scan mode
        for (port in portsToScan) {
            val t = thread {
                val result = scanPort(targetIP, port)
                // Thread-safe addition to the shared results list
                synchronized(results) { results.add(result) }
            }
            threads.add(t)
        }
        // Wait for all threads to complete before generating report
        threads.forEach { it.join() }
    } else {
        // Demo mode with predefined results
        results.addAll(
            listOf(
                PortResult(22, "SSH", "OPEN", "Restrict SSH access to trusted IPS.",
                    "sudo ufw allow from <trusted_IP> to any port 22"),
                PortResult(80, "HTTP", "OPEN", "Keep web server updated and monitor traffic.",
                    "sudo apt update && sudo apt upgrade"),
                PortResult(443, "HTTPS", "OPEN", "Keep web server updated and monitor traffic.",
                    "sudo apt update && sudo apt upgrade"),
                PortResult(21, "FTP", "CLOSED", "", ""),
                PortResult(25, "SMTP", "CLOSED", "", ""),
                PortResult(3306, "MYSQL", "CLOSED", "", "")
            )
        )
    }

    // Separate results into open and closed ports
    val openPorts = results.filter { it.status == "OPEN" }
    val closedPorts = results.filter { it.status == "CLOSED" }

    // Generate HTML report
    val report = StringBuilder()
    report.append("<html><body>")
    report.append("<h2>Port Scan Report - $targetIP</h2>")

    // Open Ports Table
    report.append("<h3>Open Ports</h3>")
    report.append("<table border='1' style='border-collapse: collapse;'>")
    report.append("<tr><th>Port</th><th>Service</th><th>Status</th><th>Recommendation</th><th>Example Command</th></tr>")
    for (r in openPorts) {
        report.append("<tr><td>${r.port}</td><td>${r.service}</td><td style='color:green;'>${r.status}</td><td>${r.recommendation}</td><td>${r.exampleLocalCommand}</td></tr>")
    }
    report.append("</table>")

    // Closed Ports Table
    report.append("<h3>Closed Ports</h3>")
    report.append("<table border='1' style='border-collapse: collapse;'>")
    report.append("<tr><th>Port</th><th>Service</th><th>Status</th></tr>")
    for (r in closedPorts) {
        report.append("<tr><td>${r.port}</td><td>${r.service}</td><td style='color:red;'>${r.status}</td></tr>")
    }
    report.append("</table>")
    report.append("<p>Total closed ports: ${closedPorts.size}</p>")

    // Save HTML report to disk
    val reportFile = File("PortScanReport.html")
    reportFile.writeText(report.toString())
    println("Report generated: ${reportFile.absolutePath}")

    println("NOTE: This scan runs safely on localhost only. No external networks were scanned.")
}


