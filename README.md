ready-3scale-plugin
===================

A plugin for Ready! API that allows you to import APIs directly from a 3Scale hosted developer portal. 

Installation
------------

Either install via the integrated Plugin Repository or build locally with

```maven clean install assembly:single```

and install in Ready! API from the Plugin Manager. Please note that if you need the plugin to work with SoapUI Pro 
or Ready! API 1.0 you will need to build the SoapUIPro-compatible branch instead of master.

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


