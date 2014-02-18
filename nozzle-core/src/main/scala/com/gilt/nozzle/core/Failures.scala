package com.gilt.nozzle.core

trait ValidationException extends Exception

case class AuthorizationFailedException(message: String, cause: Throwable = null) extends ValidationException

case class AccessForbiddenException(message: String, cause: Throwable = null) extends ValidationException