package net.opentechnology

def build() {
    node {
        stage('Clone sources') {
            git url: 'https://github.com/donaldmcintosh/triki.git'
        }

        stage('Gradle build') {
            sh "./gradlew clean build"
	    cucumber fileIncludePattern: 'build/cucumber/report.json'
        }

        stage('Deploy local') {
            sh "unzip build/distributions/*.zip -d ~/deploy"
        }
    }
}

build()
