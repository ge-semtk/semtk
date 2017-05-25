REM
REM This script must be executed in a node.js command prompt
REM 	See https://nodejs.org/en/download
REM
REM Where jsdoc has been installed
REM 	https://www.npmjs.com/package/jsdoc 
REM
REM Instructions on jsdoc annotations: 
REM 	http://usejsdoc.org/about-getting-started.html

SET USERDIR=%UserProfile%
SET DOC=%~dp0


call %USERDIR%\node_modules\.bin\jsdoc.cmd -t templates\minami-master -d %DOC% ^
           %DOC%\..\sparqlGraph\js\importcolumn.js ^
           %DOC%\..\sparqlGraph\js\importtext.js ^
           %DOC%\..\sparqlGraph\js\importtrans.js ^
           %DOC%\..\sparqlGraph\js\importmapping.js ^
           %DOC%\..\sparqlGraph\js\mappingitem.js ^
           %DOC%\..\sparqlGraph\js\semtk_api.js ^
           %DOC%\..\sparqlGraph\js\semtk_api_import.js ^
           %DOC%\..\sparqlGraph\js\semtk_api_loader.js                                

jsdoc.sh %DOC%
