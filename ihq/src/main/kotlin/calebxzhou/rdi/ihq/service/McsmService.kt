package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.CONF
import calebxzhou.rdi.ihq.model.mcsm.CreateInstanceResponse
import calebxzhou.rdi.ihq.model.mcsm.DeleteInstanceRequest
import calebxzhou.rdi.ihq.model.mcsm.InstanceConfig
import calebxzhou.rdi.ihq.model.mcsm.InstanceDetail
import calebxzhou.rdi.ihq.model.mcsm.InstanceListData
import calebxzhou.rdi.ihq.model.mcsm.InstanceOperationResponse
import calebxzhou.rdi.ihq.model.mcsm.BatchOperationRequest
import calebxzhou.rdi.ihq.model.mcsm.InstallInstanceRequest
import calebxzhou.rdi.ihq.model.mcsm.McsmResponse
import calebxzhou.rdi.ihq.model.mcsm.Overview
import calebxzhou.rdi.ihq.model.mcsm.UpdateInstanceResponse
import calebxzhou.rdi.ihq.net.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object McsmService {
    val baseUrl = "http://${CONF.mcsm.host}:${CONF.mcsm.port}/api"
    val ofUrl = { path: String ->
        // Check if path already contains query parameters
        val separator = if (path.contains("?")) "&" else "?"
        "$baseUrl$path${separator}apikey=${CONF.mcsm.apiKey}"
    }
    private val String.mcsmUrl
        get() = ofUrl(this)

    suspend fun overview(): McsmResponse<Overview> {
        return httpClient.use { client ->
            client.get("/overview".mcsmUrl).body()
        }
    }

    suspend fun instanceList(): McsmResponse<Overview> {
        return httpClient.use { client ->
            client.get("/instance".mcsmUrl).body()
        }
    }

    suspend fun deleteInstance(instanceIds: List<String>, deleteFile: Boolean = false): McsmResponse<List<String>> {
        return httpClient.use { client ->
            client.delete("/instance?daemonId=${CONF.mcsm.daemonId}".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(DeleteInstanceRequest(instanceIds, deleteFile))
            }.body()
        }
    }

    suspend fun createInstance(instanceConfig: InstanceConfig): McsmResponse<CreateInstanceResponse> {
        return httpClient.use { client ->
            client.post("/instance?daemonId=${CONF.mcsm.daemonId}".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(instanceConfig)
            }.body()
        }
    }

    suspend fun updateInstance(instanceUuid: String, instanceConfig: InstanceConfig): McsmResponse<UpdateInstanceResponse> {
        return httpClient.use { client ->
            client.put("/protected_instance/instance_update?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(instanceConfig)
            }.body()
        }
    }

    // Instance Control Operations
    suspend fun startInstance(instanceUuid: String): McsmResponse<InstanceOperationResponse> {
        return httpClient.use { client ->
            client.get("/protected_instance/open?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}".mcsmUrl).body()
        }
    }

    suspend fun stopInstance(instanceUuid: String): McsmResponse<InstanceOperationResponse> {
        return httpClient.use { client ->
            client.get("/protected_instance/stop?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}".mcsmUrl).body()
        }
    }

    suspend fun restartInstance(instanceUuid: String): McsmResponse<InstanceOperationResponse> {
        return httpClient.use { client ->
            client.get("/protected_instance/restart?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}".mcsmUrl).body()
        }
    }

    suspend fun killInstance(instanceUuid: String): McsmResponse<InstanceOperationResponse> {
        return httpClient.use { client ->
            client.get("/protected_instance/kill?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}".mcsmUrl).body()
        }
    }

    // Batch Operations
    suspend fun batchStartInstances(instances: List<BatchOperationRequest>): McsmResponse<Boolean> {
        return httpClient.use { client ->
            client.post("/instance/multi_start".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(instances)
            }.body()
        }
    }

    suspend fun batchStopInstances(instances: List<BatchOperationRequest>): McsmResponse<Boolean> {
        return httpClient.use { client ->
            client.post("/instance/multi_stop".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(instances)
            }.body()
        }
    }

    suspend fun batchRestartInstances(instances: List<BatchOperationRequest>): McsmResponse<Boolean> {
        return httpClient.use { client ->
            client.post("/instance/multi_restart".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(instances)
            }.body()
        }
    }

    suspend fun batchKillInstances(instances: List<BatchOperationRequest>): McsmResponse<Boolean> {
        return httpClient.use { client ->
            client.post("/instance/multi_kill".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(instances)
            }.body()
        }
    }

    // Instance Update
    suspend fun updateInstanceAsync(instanceUuid: String): McsmResponse<Boolean> {
        return httpClient.use { client ->
            client.post("/protected_instance/asynchronous?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}&task_name=update".mcsmUrl).body()
        }
    }

    // Send Command
    suspend fun sendCommand(instanceUuid: String, command: String): McsmResponse<InstanceOperationResponse> {
        return httpClient.use { client ->
            client.get("/protected_instance/command?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}&command=${java.net.URLEncoder.encode(command, "UTF-8")}".mcsmUrl).body()
        }
    }

    // Get Output Log
    suspend fun getOutputLog(instanceUuid: String, size: Int? = null): McsmResponse<String> {
        val sizeParam = size?.let { "&size=$it" } ?: ""
        return httpClient.use { client ->
            client.get("/protected_instance/outputlog?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}$sizeParam".mcsmUrl).body()
        }
    }

    // Reinstall Instance
    suspend fun reinstallInstance(instanceUuid: String, installRequest: InstallInstanceRequest): McsmResponse<Boolean> {
        return httpClient.use { client ->
            client.post("/protected_instance/install_instance?daemonId=${CONF.mcsm.daemonId}&uuid=$instanceUuid".mcsmUrl) {
                contentType(ContentType.Application.Json)
                setBody(installRequest)
            }.body()
        }
    }

    // Instance List with pagination and filtering
    suspend fun getInstanceList(
        page: Int,
        pageSize: Int,
        instanceName: String? = null,
        status: String
    ): McsmResponse<InstanceListData> {
        val instanceNameParam = instanceName?.let { "&instance_name=${java.net.URLEncoder.encode(it, "UTF-8")}" } ?: ""
        return httpClient.use { client ->
            client.get("/service/remote_service_instances?daemonId=${CONF.mcsm.daemonId}&page=$page&page_size=$pageSize&status=$status$instanceNameParam".mcsmUrl).body()
        }
    }

    // Get Instance Detail
    suspend fun getInstanceDetail(instanceUuid: String): McsmResponse<InstanceDetail> {
        return httpClient.use { client ->
            client.get("/instance?uuid=$instanceUuid&daemonId=${CONF.mcsm.daemonId}".mcsmUrl).body()
        }
    }
}