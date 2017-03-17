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
           %DOC%\..\sparqlGraph\js\semtk_api.js 
                                           

jsdoc.sh %DOC%
