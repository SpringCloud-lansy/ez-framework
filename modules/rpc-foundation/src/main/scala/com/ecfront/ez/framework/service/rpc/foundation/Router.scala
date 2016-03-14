package com.ecfront.ez.framework.service.rpc.foundation

import java.util.regex.Pattern

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * 路由操作对象
  *
  * @param requestClass 请求对象的类型
  * @param fun          业务方法
  */
case class Fun[E](requestClass: Class[E], private val fun: (Map[String, String], E, EZRPCContext) => Resp[Any]) {

  /**
    * 执行业务方法
    */
  private[rpc] def execute(parameters: Map[String, String], body: Any, context: EZRPCContext): Resp[Any] = {
    fun(parameters, body.asInstanceOf[E], context)
  }

}

/**
  * 路由表
  * <p>
  * 不同的Server实例对应不同的路由实例
  */
private[rpc] class Router extends LazyLogging {

  /**
    * 业务方法容器，非正则
    * <p>
    * 结构为：method -> ( path -> ( 业务方法，服务实例 ) )
    */
  private val funContainer = collection.mutable.Map[String, collection.mutable.Map[String, Fun[_]]]()
  funContainer += ("POST" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("GET" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("DELETE" -> collection.mutable.Map[String, Fun[_]]())
  funContainer += ("PUT" -> collection.mutable.Map[String, Fun[_]]())
  /**
    * 业务方法容器，正则
    * <p>
    *
    */
  private val funContainerR = collection.mutable.Map[String, ArrayBuffer[RouterRContext]]()
  funContainerR += ("POST" -> ArrayBuffer[RouterRContext]())
  funContainerR += ("GET" -> ArrayBuffer[RouterRContext]())
  funContainerR += ("DELETE" -> ArrayBuffer[RouterRContext]())
  funContainerR += ("PUT" -> ArrayBuffer[RouterRContext]())

}

/**
  * 正则路由上下文
  *
  * @param originalPath 原始的Path（注册时用的Path）
  * @param pattern      正则对象
  * @param param        从原始Path中抽取的变量，如 /index/:id/ 则获取到 seq("id")
  * @param fun          业务方法
  */
case class RouterRContext(originalPath: String, pattern: Pattern, param: Seq[String], fun: Fun[_])

object Router extends LazyLogging {

  private val ROUTERS = collection.mutable.Map[String, Router]()

  /**
    * 获取对应的路由信息，先按非正则匹配，匹配再从正则容器中查找
    */
  private[rpc] def getFunction(channel: String, method: String, path: String, parameters: Map[String, String]): (Resp[_], Fun[_], Map[String, String], String) = {
    val newParameters = collection.mutable.Map[String, String]()
    newParameters ++= parameters
    var urlTemplate = path
    var fun: Fun[_] = ROUTERS(channel).funContainer(method.toUpperCase).get(path).orNull
    if (fun == null) {
      //使用正则路由
      ROUTERS(channel).funContainerR(method).foreach {
        item =>
          val matcher = item.pattern.matcher(path)
          if (matcher.matches()) {
            //匹配到正则路由
            //获取原始（注册时的）Path
            urlTemplate = item.originalPath
            fun = item.fun
            //从Path中抽取变量
            item.param.foreach(name => newParameters += (name -> matcher.group(name)))
          }
      }
    }
    if (fun != null) {
      //匹配到路由
      (Resp.success(null), fun, newParameters.toMap, urlTemplate)
    } else {
      //没有匹配到路由且没有any实现
      (Resp.notImplemented("[ %s ] %s".format(method, path)), null, newParameters.toMap, null)
    }
  }

  /**
    * 注册路由规则
    *
    * @param method       资源操作方式
    * @param path         资源路径
    * @param requestClass 请求对象的类型
    * @param fun          业务方法
    */
  private[rpc] def add[E](channel: String, method: String, path: String, requestClass: Class[E], fun: => (Map[String, String], E, EZRPCContext) => Resp[Any]) {
    if (!ROUTERS.contains(channel)) {
      ROUTERS += channel -> new Router
    }
    //格式化URL
    logger.info(s"Register [${channel.toString}] method [$method] path : $path.")
    if (path.contains(":")) {
      //regular
      val r = Router.getRegex(path)
      //注册到正则路由表
      ROUTERS(channel).funContainerR(method) += RouterRContext(path, r._1, r._2, Fun[E](requestClass, fun))
    } else {
      //注册到非正则路由表
      ROUTERS(channel).funContainer(method) += (path -> Fun[E](requestClass, fun))
    }
  }

  private val matchRegex =""":\w+""".r

  /**
    * 将非规范正则转成规范正则
    * <p>
    * 如 输入 /index/:id/  输出 （^/index/(?<id>[^/]+)/$ 的正则对象，Seq("id") ）
    *
    * @param path 非规范正则，用 :x 表示一个变量
    * @return （规范正则，变更列表）
    */
  private def getRegex(path: String): (Pattern, Seq[String]) = {
    var pathR = path
    var named = mutable.Buffer[String]()
    matchRegex.findAllMatchIn(path).foreach {
      m =>
        val name = m.group(0).substring(1)
        pathR = pathR.replaceAll(m.group(0), """(?<""" + name + """>[^/]+)""")
        named += name
    }
    (Pattern.compile("^" + pathR + "$"), named)
  }

}