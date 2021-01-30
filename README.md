# File Parameters Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/file-parameters-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/file-parameters-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/file-parameters-plugin.svg)](https://github.com/jenkinsci/file-parameters-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/file-parameters-plugin.svg)](https://plugins.jenkins.io/file-parameters-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/file-parameters-plugin.svg?label=changelog)](https://github.com/jenkinsci/file-parameters-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/file-parameters-plugin.svg?color=blue)](https://plugins.jenkins.io/file-parameters-plugin)

## Introduction

Offers alternative types of file parameter that are compatible with Pipeline and do not suffer from the architectural flaws of the type built into Jenkins core.

See [JENKINS-27413](https://issues.jenkins-ci.org/browse/JENKINS-27413) and [JENKINS-29289](https://issues.jenkins-ci.org/browse/JENKINS-29289) for background.

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

## Implementation status

- [X] Base64 file parameter (simple and suitable for small files)
  - [X] implementation
  - [X] inline help text
  - [X] `withFileParameter` compatibility
  - [X] manual test
  - [X] automated test
- [ ] stashed file parameter (suitable for larger files used in Pipeline)
  - [X] implementation
  - [X] inline help text
  - [X] `withFileParameter` compatibility
  - [X] manual test
  - [X] automated test
  - [ ] sanity check against `artifact-manager-s3`
  - [ ] suppress download link when stash is missing
- [ ] archived file parameter (compatible with freestyle, and suitable if you want to ensure parameters are kept after the build ends)
  - [ ] implementation
  - [ ] inline help text
  - [ ] `withFileParameter` compatibility
  - [ ] manual test
  - [ ] automated test
- [ ] GUI submission
  - [X] implementation
  - [X] manual test
  - [ ] automated test
- [ ] CLI submission
  - [X] implementation
  - [ ] manual test
  - [X] automated test
- [X] `POST` submission
  - [X] implementation
  - [X] manual test
  - [X] automated test
- [X] `withFileParameter` wrapper step
  - [X] implementation
  - [X] inline help text
  - [X] manual test
  - [X] automated test
  - [X] option to tolerate undefined parameter
- [ ] `input` step submission
  - [ ] design
  - [X] manual test
  - [ ] automated test
- [ ] `build` step submission
  - [ ] design
  - [ ] manual test
  - [ ] automated test
- [X] tests using Declarative syntax
- [ ] tests using `build-token-root`

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
