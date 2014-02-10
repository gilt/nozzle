package com.gilt.nozzle.examples.basic

import com.gilt.nozzle.core.{TargetInfo, PolicyValidator, DevInfo, NozzleServer}
import com.gilt.nozzle.core.modules.RegexTargetInfo.regexExtractTargetInfo
import com.gilt.nozzle.core.modules.AtLeastOneRoleValidationPolicy.hasAtLeastOneRequiredRole
import com.gilt.nozzle.modules.devinfo.mongo.MongoDevInfo.mongoExtractDevInfo
import com.gilt.nozzle.core.DefaultDevKeyExtractors.queryParamDevKeyExtractors

object MongoNozzle extends NozzleServer {

  def extractDevInfo: DevInfo.DevInfoExtractor = mongoExtractDevInfo(queryParamDevKeyExtractors)

  def extractTargetInfo: TargetInfo.TargetInfoExtractor = regexExtractTargetInfo

  def policyValidator: PolicyValidator.ValidatePolicy = hasAtLeastOneRequiredRole
}
