package com.gilt.nozzle.core.modules

import com.gilt.nozzle.core.PolicyValidator.ValidatePolicy
import com.gilt.nozzle.core.AccessForbiddenException
import scala.util.{Success, Failure}

object AtLeastOneRoleValidationPolicy {
  def hasAtLeastOneRequiredRole: ValidatePolicy = { (request, devInfo, targetInfo) =>
    if(devInfo.roles.toSet.intersect(targetInfo.roles.toSet).isEmpty)
      Failure(AccessForbiddenException(s"User ${devInfo.id} with roles ${devInfo.roles} don't have access to ${request.uri}"))

    Success({})
  }
}
