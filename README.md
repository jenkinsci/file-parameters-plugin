# File Parameters Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/file-parameters-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/file-parameters-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/file-parameters-plugin.svg)](https://github.com/jenkinsci/file-parameters-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/file-parameters-plugin.svg)](https://plugins.jenkins.io/file-parameters-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/file-parameters-plugin.svg?label=changelog)](https://github.com/jenkinsci/file-parameters-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/file-parameters-plugin.svg?color=blue)](https://plugins.jenkins.io/file-parameters-plugin)

## Introduction

Offers alternative types of file parameter that are compatible with Pipeline and do not suffer from the architectural flaws of the type built into Jenkins core.

See [JENKINS-27413](https://issues.jenkins-ci.org/browse/JENKINS-27413) and [JENKINS-29289](https://issues.jenkins-ci.org/browse/JENKINS-29289) for background.

## Minimal usage

If you defined a Base64 file parameter named `FILE` in the GUI configuration for a Pipeline project, you can access it in a couple of ways:

```groovy
node {
    sh 'echo $FILE | base64 -d'
    withFileParameter('FILE') {
        sh 'cat $FILE'
    }
}
```

That is: as a Base64-encoded environment variable; or via a temporary file with the decoded content.

A stashed file parameter can also be accessed in a couple of ways:

```groovy
node {
    unstash 'FILE'
    sh 'cat FILE'
    withFileParameter('FILE') {
        sh 'cat $FILE'
    }
}
```

That is: as a stash of the same name with a single file of the same name; or, again, via a temporary file.

## Usage in Declarative Pipeline

You can now declare and use file parameters via Declarative Pipeline syntax:

```groovy
pipeline {
  agent any
  parameters {
    base64File 'small'
    stashedFile 'large'
  }
  stages {
    stage('Example') {
      steps {
        withFileParameter('small') {
          sh 'cat $small'
        }
        unstash 'large'
        sh 'cat large'
      }
    }
  }
}
```

## Usage with `input`

You can use Base64 parameters for uploading _small_ files in the middle of the build:

```groovy
def fb64 = input message: 'upload', parameters: [base64File('file')]
node {
    withEnv(["fb64=$fb64"]) {
        sh 'echo $fb64 | base64 -d'
    }
}
```

Currently there is no mechanism for doing this with stashed files.
Nor can you use the `withFileParameter` wrapper here.

## Usage with `build`

You can use Base64 parameters for passing _small_ files to downstream builds:

```groovy
build job: 'downstream', parameters: [base64File(name: 'file', base64: 'aGVsbG8=')]
```

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
