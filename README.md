This is a Java8 servlet for Tomcat7. A custom proxy that leaves the parameters and request body unchanged, adding an authorization header. If the response code is OK (200) or Redirection (302), it sends the response without changes. Otherwise, it returns a 500

Fill _config.properties_ with your real data

