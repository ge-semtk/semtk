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
				            //sh 'mvn clean install -DskipTests'
					    sh 'mvn clean install'
			           }
		       }

	  }
}
