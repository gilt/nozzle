Nozzle
======

API Gateway that follows a simple pipeline:

 - Get developer info from the request (possibly using a database or remote service)
 - Get target service endpoint from the request
 - Validate the dev is entitled to send the request to the final endpoint
 - Manipulate the request so that it is correct for the final endpoint
 - Send the manipulated request to the endpoint
 - Manipulate and send the response to the client
