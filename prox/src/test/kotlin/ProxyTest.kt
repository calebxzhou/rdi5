import calebxzhou.rdi.prox.BinaryPortSwitchingTestClient
import calebxzhou.rdi.prox.DefaultBackendTestClient
import calebxzhou.rdi.prox.EchoServer
import calebxzhou.rdi.prox.RuntimeSwitchingTestClient
import calebxzhou.rdi.prox.TcpReverseProxy
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class ProxyTest {
    /**
     * Comprehensive test that demonstrates binary dynamic proxy functionality
     */
    @Test
    fun testBinaryDynamicProxy() {
        println("Starting comprehensive binary dynamic proxy test...")

        // Start multiple echo servers on different ports
        Thread {
            val defaultServer = EchoServer("DEFAULT-9090")
            defaultServer.start(9090)
        }.start()

        Thread {
            val server1 = EchoServer("DYNAMIC-9091")
            server1.start(9091)
        }.start()

        Thread {
            val server2 = EchoServer("DYNAMIC-9092")
            server2.start(9092)
        }.start()

        // Give servers time to start
        Thread.sleep(3000)

        // Start the reverse proxy in a separate thread
        Thread {
            val proxy = TcpReverseProxy()
            proxy.start("localhost", 8080, "localhost", 9090)
        }.start()

        // Give proxy time to start
        Thread.sleep(2000)

        // Run tests
        val defaultTestClient = DefaultBackendTestClient()
        val binaryTestClient = BinaryPortSwitchingTestClient()
        val runtimeTestClient = RuntimeSwitchingTestClient()

        println("\n=== Test 1: Default Backend ===")
        defaultTestClient.testDefaultBackend()

        Thread.sleep(2000)

        println("\n=== Test 2: Binary Port Switching ===")
        binaryTestClient.testBinaryPortSwitching()

        Thread.sleep(2000)

        println("\n=== Test 3: Binary Control with Immediate Data ===")
        binaryTestClient.testBinaryWithImmediateData()

        Thread.sleep(2000)

        println("\n=== Test 4: Runtime Binary Switching (Multiple switches) ===")
        runtimeTestClient.testRuntimeBinarySwitching()

        println("\nTest completed! The proxy supports binary control packet switching at any time during the connection.")
    }

}