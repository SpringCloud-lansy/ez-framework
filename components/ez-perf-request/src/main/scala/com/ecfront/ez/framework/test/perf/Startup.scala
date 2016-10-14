package com.ecfront.ez.framework.test.perf

import java.util.concurrent.atomic.AtomicLong

import com.ecfront.common.{JsonHelper, StandardCode}
import com.mdataset.excavator.Excavator


object Startup extends App {

  val excavator = Excavator

  var token: String = ""

  def u(path: String): String = s"http://host.wangzifinance.cn:8070/$path?__ez_token__=$token"

  token = JsonHelper.toJson(excavator.getHttpClient.post(u("public/ez/auth/login/"),
    s"""
       |{
       |  "id":"sysadmin",
       |  "password":"admin"
       |}
     """.stripMargin)).get("body").get("token").asText()

  val counter = new AtomicLong(0)

  val threads = for (i <- 0 until 1000)
    yield new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          try {
          //  assert(JsonHelper.toJson(excavator.getHttpClient.get(u("test1/longtime/"))).get("code").asText() == StandardCode.SUCCESS)
            var saved = JsonHelper.toJson(excavator.getHttpClient.post(u("test1/"),
              s"""
                 |{
                 |   "code":"${System.nanoTime() + "1" + i}",
                 |   "name":"n"
                 |}
             """.stripMargin))
            assert(saved.get("code").asText() == StandardCode.SUCCESS)
            var uuid = saved.get("body").get("bus_uuid").asText()
            var page = JsonHelper.toJson(excavator.getHttpClient.get(u("test2/page/1/10/"))).get("code").asText()
            assert(page == StandardCode.SUCCESS)
            assert(JsonHelper.toJson(excavator.getHttpClient.get(u(s"test1/uuid/$uuid"))).get("code").asText() == StandardCode.SUCCESS)
            assert(JsonHelper.toJson(excavator.getHttpClient.delete(u(s"test1/uuid/$uuid"))).get("code").asText() == StandardCode.SUCCESS)

            saved = JsonHelper.toJson(excavator.getHttpClient.post(u("test1/"),
              s"""
                 |{
                 |   "code":"${System.nanoTime() + "2" + i}",
                 |   "name":"n"
                 |}
             """.stripMargin))
            println(counter.incrementAndGet())
            assert(saved.get("code").asText() == StandardCode.SUCCESS)
            uuid = saved.get("body").get("bus_uuid").asText()
            page = JsonHelper.toJson(excavator.getHttpClient.get(u("test1/page/1/10/"))).get("code").asText()
            assert(page == StandardCode.SUCCESS)
            assert(JsonHelper.toJson(excavator.getHttpClient.get(u(s"test2/uuid/$uuid"))).get("code").asText() == StandardCode.SUCCESS)
            assert(JsonHelper.toJson(excavator.getHttpClient.delete(u(s"test2/uuid/$uuid"))).get("code").asText() == StandardCode.SUCCESS)
          } catch {
            case e: Throwable =>
          }
        }
      }
    })
  threads.foreach(_.start())
  threads.foreach(_.join())

}
