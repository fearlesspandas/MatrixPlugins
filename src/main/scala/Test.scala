import scala.sys.process._
import scalaj.http.{Http, HttpOptions}
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
object Test{
  case class Server(name:String){
    val login_url = s"https://$name/_matrix/client/r0/login"
    val msg_url = (roomid:RoomId,access_token:AccessToken) => s"https://$name/_matrix/client/r0/rooms/${roomid.value}/send/m.room.message?access_token=${access_token.value}"
    val msgEvents = (roomid:RoomId) => s"https://$name/_matrix/client/r0/rooms/${roomid.value}/messages"
  }
  case class Url(value:String)
  case class AccessToken(value:String)
  case class RoomId(value:String)
  case class Content(msgtype:String,body:String)
  case class Unsigned(age:Int)
  case class Event(`type`:String,room_id: String,sender:String,content:Content,origin_server_ts:Long,unsigned: Unsigned,event_id:String,user_id:String,age:Int)
  case class Response(chunk:List[Event],start:String,end:String)
  case class MessageEvent(event_id:String)
  var lastprocessedevent:String = ""
  var responseEvents:Set[String] = Set()

  def main(args:Array[String]):Unit = {
    implicit val server = Server(sys.env("SERVER"))
    val username = sys.env("MATRIX_USER")
    println(username)
    val password = sys.env("MATRIX_PASSWORD")

    val token = login(username,password)
    val room_id = RoomId(sys.env("ROOM_ID"))
    while(true){
      println(s"using room id$room_id:")
      val res = decode[Response](getMessageEvents(room_id,token))
      val ret = res match {
        case Right(res) if res.chunk.head.event_id != lastprocessedevent &&
        !responseEvents.contains(res.chunk.head.event_id) &&
          iscmd(res)
        => {
          lastprocessedevent = res.chunk.head.event_id
          val r = sendmessage(runCommand(res.chunk.head.content.body.replaceAll("cmd ","")),token,room_id).event_id
          responseEvents = responseEvents ++ Set(r)
          r
        }
        case _ => "err"
      }
      println(ret)
      Thread.sleep(1000)
    }

  }

  def logindata(username:String, pass:String) = s"""{"type":"m.login.password","user":"$username","password":"$pass"}"""
  def login(username:String,password:String)(implicit server:Server):AccessToken =
    //send request
    AccessToken(
      Http(server.login_url)
        .postData(logindata(username,password))
        .header("Content-Type", "application/json")
        .header("Charset", "UTF-8")
        .option(HttpOptions.readTimeout(10000)).asString
        //format response to get token
        .body
        .split(",")
        .filter(_.contains("access_token"))
        .map(_.replaceAll(""""access_token":""",""))
        .head
        .replaceAll("\"","")
    )
  def messagedata(msg:String) = s"""{"msgtype":"m.text", "body":"${msg.filter(c => c.isLetterOrDigit || c==' ')}"}"""
  def sendmessage(msg:String,access_token:AccessToken,roomid:RoomId)(implicit server: Server):MessageEvent = {
    decode[MessageEvent](
    Http(server.msg_url(roomid,access_token))
      .postData(messagedata(msg))
      .header("Content-Type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.readTimeout(10000)).asString
      .body
    ).right.get
  }

  def iscmd(str:Response) = str.chunk.head.content.body.startsWith("cmd")
  def getMessageEvents(roomId: RoomId,accessToken: AccessToken)(implicit server: Server):String = {
    Http(server.msgEvents(roomId))
      .param("access_token",accessToken.value)
      .param("from","END")
      .param("dir","b")
      .param("limit","1")
      .option(HttpOptions.readTimeout(10000)).asString
      .body
  }

  def runCommand(cmd:String):String = {
    val splitByPipes = cmd.split('|').map(s => if(s.head == ' ') s.replaceFirst(" ","") else s)
    if(splitByPipes.size == 1) splitByPipes.head.!!
    else
    splitByPipes.tail.foldLeft[ProcessBuilder](splitByPipes.head)((a,c) => a #| c).!!
  }


}