package net.opentechnology

def build() {
    node {
        stage('Clone sources') {
            git url: 'https://github.com/donaldmcintosh/triki.git'
        }

        stage('Gradle build') {
            sh "./gradlew clean build"
	    cucumber fileIncludePattern: 'build/dev.json'
        }

        stage('Deploy local') {
            sh "unzip -o build/distributions/*.zip -d $LOCAL_DEPLOY"
        }
    }
}

build()
