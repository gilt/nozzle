Nozzle
======

API Gateway that follows a simple pipeline:

 - Get developer info from the request (possibly using a database or remote service)
 - Get target service endpoint from the request
 - Validate the dev is entitled to send the request to the final endpoint
 - Manipulate the request so that it is correct for the final endpoint
 - Send the manipulated request to the endpoint
 - Manipulate and send the response to the client

Usage
-----

NozzleServer provides the basic pipeline with some default functionality. To run a minimal API Gateway you
will need to override these methods:

  - def extractDevInfo: DevInfoExtractor
  - def extractTargetInfo: TargetInfoExtractor
  - def policyValidator: ValidatePolicy

You can also override default functions if you need so:

  - def enrichRequest: RequestEnricher = noopRequestEnricher(system.log)
  - def enrichResponse: ResponseEnricher = noopResponseEnricher(system.log)
  - def errorHandler: ValidationFailureHandler = defaultErrorHandler
  - def forwardRequest: ForwardRequest = sendReceive


See nozzle-examples for minimal working examples