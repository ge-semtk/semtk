pipeline {
	agent { label 'master' }
    stages {
    		  stage('Checkout') {
			          steps {
                     checkout scm
                }
           }
	   
		       stage('build') {
			           steps {
				            sh 'mvn -f clean install -DskipTests'
			           }
		       }

	  }
}
