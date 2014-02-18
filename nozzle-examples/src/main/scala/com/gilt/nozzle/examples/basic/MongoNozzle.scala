package com.gilt.nozzle.examples.basic

import com.gilt.nozzle.core.NozzleServer
import com.gilt.nozzle.core.modules.RegexTargetInfo.regexExtractTargetInfo
import com.gilt.nozzle.core.modules.AtLeastOneRoleValidationPolicy.hasAtLeastOneRequiredRole
import com.gilt.nozzle.modules.devinfo.mongo.MongoDevInfo.mongoExtractDevInfo
import com.gilt.nozzle.core.DefaultDevKeyExtractors.queryParamDevKeyExtractors
import com.gilt.nozzle.core.TargetInfo.TargetInfoExtractor
import com.gilt.nozzle.core.PolicyValidator.ValidatePolicy
import com.gilt.nozzle.core.DevInfo.DevInfoExtractor

object MongoNozzle extends NozzleServer {

  def extractDevInfo: DevInfoExtractor = mongoExtractDevInfo(queryParamDevKeyExtractors)

  def extractTargetInfo: TargetInfoExtractor = regexExtractTargetInfo

  def policyValidator: ValidatePolicy = hasAtLeastOneRequiredRole
}
