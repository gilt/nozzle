package com.gilt.nozzle.core.modules

import com.gilt.nozzle.core.PolicyValidator.ValidatePolicy
import com.gilt.nozzle.core.{AuthorizationFailedException, AccessForbiddenException}
import com.gilt.nozzle.core.DevInfo.Anonymous

import scala.util.{Success, Failure}

object AtLeastOneRoleValidationPolicy {
  val success = Success({})
  def hasAtLeastOneRequiredRole: ValidatePolicy = { (request, devInfo, targetInfo) =>

    if(targetInfo.roles.isEmpty)
      success
    else if(devInfo.roles.toSet.intersect(targetInfo.roles.toSet).isEmpty) {
      devInfo match {
        case Anonymous => Failure(AuthorizationFailedException(s"User authentication required"))
        case _ => Failure(AccessForbiddenException(s"User ${devInfo.id} with roles ${devInfo.roles} don't have access to ${request.uri}"))
      }
    } else
      success
  }
}
