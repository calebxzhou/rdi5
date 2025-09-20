package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.model.Team
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.elemMatch
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Indexes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId

fun Route.teamRoutes()= route("/team") {
    get("/{id}"){

    }
    post("/create") {

    }
    post("/delete"){

    }
    post("/invite") {

    }
    post("/join") {

    }
}
object TeamService {
    val dbcl = DB.getCollection<Team>("team")
    init {
        runBlocking {
            RoomService.dbcl.createIndex(
                Indexes.ascending("members.id"),
            )
        }
    }
    //玩家拥有的团队
    suspend fun getOwnTeam(uid: ObjectId): Team? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid),
                eq("role", Team.Role.OWNER)
            )
        )
    ).firstOrNull()

    //玩家所在的团队
    suspend fun getJoinedTeam(uid: ObjectId): Team? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid)
            )
        )
    ).firstOrNull()
    suspend fun get(id: ObjectId){

    }
    suspend fun create(name: String, info: String){

    }
}