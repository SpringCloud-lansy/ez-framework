package com.asto.ez.framework

import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

/**
  * 全局数据存储类
  */
object EZGlobal extends LazyLogging {

  var vertx: Vertx = _
  //config.json数据
  var config: JsonObject = _

  lazy val ez = config.getJsonObject("ez")
  lazy val appName = if (ez.containsKey("appName")) ez.getString("appName") else "EZ-Framework"

  lazy val ez_rpc = if (ez.containsKey("rpc")) ez.getJsonObject("rpc") else null
  lazy val ez_rpc_http = if (ez_rpc != null && ez_rpc.containsKey("http")) ez_rpc.getJsonObject("http") else null
  lazy val ez_rpc_http_public_url = if (ez_rpc_http != null && ez_rpc_http.containsKey("publicUrl")) ez_rpc_http.getString("publicUrl") else null
  lazy val ez_rpc_http_public_uri_prefix_path = if (ez_rpc_http != null &&ez_rpc_http.containsKey("publicUriPrefix")) ez_rpc_http.getString("publicUriPrefix") else null
  lazy val ez_rpc_http_resource_path = if (ez_rpc_http != null && ez_rpc_http.containsKey("resourcePath")) ez_rpc_http.getString("resourcePath") else ""
  lazy val ez_rpc_http_access_control_allow_origin = if (ez_rpc_http != null && ez_rpc_http.containsKey("accessControlAllowOrigin")) ez_rpc_http.getString("accessControlAllowOrigin") else "*"

  lazy val ez_storage = if (ez.containsKey("storage")) ez.getJsonObject("storage") else null

  lazy val ez_cache = if (ez.containsKey("cache")) ez.getJsonObject("cache") else null
  lazy val ez_mail = if (ez.containsKey("mail")) ez.getJsonObject("mail") else null
  lazy val ez_scheduler = if (ez.containsKey("scheduler")) ez.getBoolean("scheduler") else null
  lazy val ez_auth = if (ez.containsKey("auth")) ez.getJsonObject("auth") else null
  lazy val ez_auth_allow_register: Boolean = if (ez_auth != null && ez_auth.containsKey("allow_register")) ez_auth.getBoolean("allow_register") else false
  lazy val ez_auth_active_url = if (ez_auth != null && ez_auth.containsKey("active_url")) ez_auth.getString("active_url") else ""
  lazy val ez_auth_rest_password_url = if (ez_auth != null && ez_auth.containsKey("rest_password_url")) ez_auth.getString("rest_password_url") else ""

  lazy val ez_auth_oauth2 = if (ez_auth != null && ez_auth.containsKey("oauth2")) ez_auth.getJsonObject("oauth2") else null

  lazy val args = config.getJsonObject("args")


}
