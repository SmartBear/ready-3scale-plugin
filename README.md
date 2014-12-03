ready-3scale-plugin
===================

A plugin for Ready! API that allows you to import APIs directly from a 3Scale hosted developer portal. 

Installation
------------

Install the plugin via the integrated Plugin Repository available via the Plugin Manager in SoapUI Pro 5.X or Ready! API 1.X


Build it yourself
-----------------

You can build the plugin locally by cloning this repository locally - make sure you have java and maven 3.X correctly 
installed - and run 

```mvn clean install assembly:single```

in the project folder. The plugin dist.jar will be created in the target folder and can be installed via the 
Plugin Managers' "Load from File" action. 

Usage
-----

Once installed there will have two ways to import an API from a 3Scale developer portal:

* Via the "Add API From 3Scale" option on the Project menu in the "Projects" tab
* Via the "3Scale Developer Portal" option in the "Create project from..." drop-down when creating a new project

In either case you will be prompted for the URL to a 3Scale Developer Portal that exposes API metadata, go to 
[http://www.3scale.net/our-customers/](http://www.3scale.net/our-customers/) for a directory. Please note that not
all these expose API-metadata, in which case you will get an error.

Once a valid developer portal URL has been specified (for example https://dev.truecaller.com/) you will be presented
with a list of available APIs and import options, configure as desired and proceed with the import - which will download
the APIs underlying Swagger description and configure a corresponding REST API in Ready! API. Now you can easily:

* send ad-hoc requests to the API to explore its functionality
* create functional tests of the API which you can further use to create Load Tests, Security Tests and API Monitors 
(in the SoapUI NG module)
* create a virtualized version of the API for sandboxing/simulation purposes (in the ServiceV module).

Have fun!

Version History
---------------

*  1.0.1 - 20141202 - Improved error messages and endpoint handling


