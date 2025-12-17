package calebxzhou.rdi.prox

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil
import java.net.Socket
import kotlin.test.Test

/**
 * Simple echo server for testing the reverse proxy
 */
class EchoServer(private val serverName: String) {
    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()

    fun start(port: Int) {
        try {
            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(EchoServerHandler(serverName))
                    }
                })

            val future = bootstrap.bind(port).sync()
            println("$serverName Echo server started on port $port")
            future.channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}

class EchoServerHandler(private val serverName: String) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val buffer = msg as ByteBuf
        val message = buffer.toString(CharsetUtil.UTF_8)
        println("[$serverName] Echo server received: $message")

        // Echo back the message with server identification
        val response = "[$serverName] ECHO: $message"
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(response.toByteArray()))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        println("[$serverName] Echo server error: ${cause.message}")
        ctx.close()
    }
}

/**
 * Test client that demonstrates default backend functionality
 */
class DefaultBackendTestClient {
    fun testDefaultBackend() {
        try {
            // Connect to the proxy without sending control packet
            val socket = Socket("localhost", 8080)
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            println("Connected to proxy, testing default backend...")

            // Send a regular message (no control packet)
            println("Sending message to default backend...")
            val message = "Hello from client to default backend!".toByteArray()
            out.write(message)
            out.flush()

            // Read response
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                println("Received response: $response")
            }

            socket.close()

        } catch (e: Exception) {
            println("Default backend test error: ${e.message}")
        }
    }
}

/**
 * Test client that demonstrates binary port switching
 */
class BinaryPortSwitchingTestClient {
    fun testBinaryPortSwitching() {
        try {
            // Connect to the proxy
            val socket = Socket("localhost", 8080)
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            println("Connected to proxy, testing binary port switching...")

            // Create binary control packet: 0x01 02 03 04 AA BB CC DD + port (big-endian)
            val controlPacket = byteArrayOf(
                0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(),
                0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(),
                0x23.toByte(), 0x93.toByte()  // Port 9091 in big-endian (0x2393 = 9091)
            )

            println("Sending binary control packet to switch to port 9091...")
            out.write(controlPacket)
            out.flush()

            Thread.sleep(1000) // Give time for backend connection

            // Send a test message
            println("Sending test message...")
            val testMessage = "Hello from binary client to port 9091!".toByteArray()
            out.write(testMessage)
            out.flush()

            // Read response
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                println("Received response: $response")
            }

            socket.close()

        } catch (e: Exception) {
            println("Binary test client error: ${e.message}")
        }
    }

    fun testBinaryWithImmediateData() {
        try {
            // Connect to the proxy
            val socket = Socket("localhost", 8080)
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            println("Connected to proxy, testing binary control packet with immediate data...")

            // Create control packet + immediate data in one send
            val controlPacketWithData = byteArrayOf(
                // Control header
                0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(),
                0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(),
                0x23.toByte(), 0x94.toByte()  // Port 9092 in big-endian (0x2394 = 9092)
            ) + "Immediate data after control packet!".toByteArray()

            println("Sending binary control packet (port 9092) with immediate data...")
            out.write(controlPacketWithData)
            out.flush()

            // Read response
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                println("Received response: $response")
            }

            socket.close()

        } catch (e: Exception) {
            println("Binary test with immediate data error: ${e.message}")
        }
    }
}

/**
 * Test client that demonstrates runtime backend switching
 */
class RuntimeSwitchingTestClient {
    fun testRuntimeBinarySwitching() {
        try {
            // Connect to the proxy
            val socket = Socket("localhost", 8080)
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            println("Connected to proxy, testing runtime binary switching...")

            // Send initial data to default backend
            println("Sending initial message to default backend...")
            val initialMessage = "Initial message to default backend".toByteArray()
            out.write(initialMessage)
            out.flush()

            // Read response from default backend
            var buffer = ByteArray(1024)
            var bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                println("Response from default: $response")
            }

            Thread.sleep(1000)

            // Switch to port 9091 using binary control packet
            println("Switching to port 9091 using binary control packet...")
            val controlPacket = byteArrayOf(
                0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(),
                0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(),
                0x23.toByte(), 0x93.toByte()  // Port 9091 in big-endian
            )
            out.write(controlPacket)
            out.flush()

            Thread.sleep(1000) // Give time for backend switch

            // Send message to new backend
            println("Sending message to switched backend (9091)...")
            val switchedMessage = "Message after switching to 9091".toByteArray()
            out.write(switchedMessage)
            out.flush()

            // Read response from switched backend
            buffer = ByteArray(1024)
            bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                println("Response from 9091: $response")
            }

            Thread.sleep(1000)

            // Switch again to port 9092
            println("Switching again to port 9092...")
            val controlPacket2 = byteArrayOf(
                0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(),
                0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(),
                0x23.toByte(), 0x94.toByte()  // Port 9092 in big-endian
            )
            out.write(controlPacket2)
            out.flush()

            Thread.sleep(1000)

            // Send final message
            println("Sending final message to 9092...")
            val finalMessage = "Final message to 9092".toByteArray()
            out.write(finalMessage)
            out.flush()

            // Read final response
            buffer = ByteArray(1024)
            bytesRead = input.read(buffer)
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                println("Response from 9092: $response")
            }

            socket.close()

        } catch (e: Exception) {
            println("Runtime switching test error: ${e.message}")
        }
    }
}

