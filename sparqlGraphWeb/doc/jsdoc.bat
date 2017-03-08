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

%USERDIR%\node_modules\.bin\jsdoc.cmd %DOC%\..\sparqlGraph\js\semtk_api.js -d %DOC%