package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.EZ
import com.ecfront.ez.framework.core.rpc.Method
import com.ecfront.ez.framework.service.auth.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * RBAC 实体初始化器
  *
  * 添加默认的多个资源，2个角色、1号系统管理员账号、1个组织
  *
  */
object Initiator extends LazyLogging {

  def init(): Unit = {
    updateCache()
    val exist = EZ_Resource.existByCond(s"""code = ?""", List(EZ_Resource.assembleCode(Method.GET.toString, "/ez/auth/manage/organization/")))
    if (!exist.body) {
      EZ_Resource.save(EZ_Resource("*", "/ez/auth/manage/*", s"Manage ALL"))
      EZ_Resource.save(EZ_Resource("*", "/ez/auth/manage/organization/*", s"Organization CRUD"))
      EZ_Resource.save(EZ_Resource(Method.GET.toString, "/ez/auth/manage/organization/:id/", s"Fetch Organization By Id"))
      EZ_Resource.save(EZ_Resource("*", "/ez/auth/manage/account/", s"Account CRUD"))
      EZ_Resource.save(EZ_Resource(Method.GET.toString, "/ez/auth/manage/account/:id/", s"Fetch Account By Id"))
      EZ_Resource.save(EZ_Resource("*", "/ez/auth/manage/role/", s"Role CRUD"))
      EZ_Resource.save(EZ_Resource("*", "/ez/auth/manage/resource/", s"Resource CRUD"))
      EZ_Resource.save(EZ_Resource("*", "/ez/auth/manage/menu/", s"Menu CRUD"))
      EZ_Resource.save(EZ_Resource(Method.GET.toString, "/ez/auth/manage/menu/", s"Fetch Menus"))
      EZ_Role.save(EZ_Role(EZ_Role.SYSTEM_ROLE_FLAG, "System", Set(EZ_Resource.assembleCode("*", "/ez/auth/manage/*")), ""))

      val org = EZ_Organization(ServiceAdapter.defaultOrganizationCode, "default")
      EZ_Organization.save(org)
      initOrganization(EZ_Organization.getByCode(ServiceAdapter.defaultOrganizationCode).body)

      val account = EZ_Account(EZ_Account.SYSTEM_ACCOUNT_LOGIN_ID, "sysadmin" + EZ_Account.VIRTUAL_EMAIL, "Sys Admin", "admin", Set(
        EZ_Role.assembleCode(EZ_Role.SYSTEM_ROLE_FLAG, ServiceAdapter.defaultOrganizationCode)), "")
      EZ_Account.save(account)
      logger.info("Initialized auth basic data.")
    }
  }

  def initOrganization(org: EZ_Organization): Unit = {
    EZ_Role.save(EZ_Role(EZ_Role.ORG_ADMIN_ROLE_FLAG, "Admin", Set(EZ_Resource.assembleCode("*", "/ez/auth/manage/*")), org.code), skipFilter = true)
    val account = EZ_Account(EZ_Account.ORG_ADMIN_ACCOUNT_LOGIN_ID, "admin" + EZ_Account.VIRTUAL_EMAIL, "Admin", "admin", Set(
      EZ_Role.assembleCode(EZ_Role.ORG_ADMIN_ROLE_FLAG, org.code)
    ), org.code)
    EZ_Account.save(account, skipFilter = true)
    EZ.eb.pubReq(ServiceAdapter.EB_ORG_INIT_FLAG, org)
  }

  def updateCache(): Unit = {
    EZ.eb.ack(ServiceAdapter.EB_FLUSH_FLAG, "")
    EZ_Organization.findEnabled("").body.foreach(CacheManager.RBAC.addOrganization)
    EZ_Resource.find("").body.foreach(CacheManager.RBAC.addResource)
    EZ_Role.findEnabled("").body.foreach(CacheManager.RBAC.addRole)
  }

}
