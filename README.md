# File Parameters Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/file-parameters-plugin-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/file-parameters-plugin-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/file-parameters-plugin-plugin.svg)](https://github.com/jenkinsci/file-parameters-plugin-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/file-parameters-plugin.svg)](https://plugins.jenkins.io/file-parameters-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/file-parameters-plugin-plugin.svg?label=changelog)](https://github.com/jenkinsci/file-parameters-plugin-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/file-parameters-plugin.svg?color=blue)](https://plugins.jenkins.io/file-parameters-plugin)

## Introduction

Offers alternative types of file parameter that are compatible with Pipeline and do not suffer from the architectural flaws of the type built into Jenkins core.

See [JENKINS-27413](https://issues.jenkins-ci.org/browse/JENKINS-27413) and [JENKINS-29289](https://issues.jenkins-ci.org/browse/JENKINS-29289) for background.

## Usage in declarative pipeline

You can now declare file parameters as is in declarative pipeline:

```groovy
pipeline {
  agent any
  parameters {
    base64File(name: 'FILE')
    stashedFile(name: 'FILE-STASH')
  }
  stages {
    stage('Example') {
      steps {
        withFileParameter(name:'FILE', allowNoFile: true) {
          sh 'cat $FILE'
        }
        unstash 'FILE-STASH'
        echo(/loaded '${readFile('FILE-STASH')}'/)
      }
    }
  }
}
```

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
  - [ ] manual test
  - [X] automated test
  - [ ] sanity check against `artifact-manager-s3`
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
- [ ] `POST` submission
  - [X] implementation
  - [ ] manual test
  - [ ] automated test
- [X] `withFileParameter` wrapper step
  - [X] implementation
  - [X] inline help text
  - [X] manual test
  - [X] automated test
  - [X] option to tolerate undefined parameter
- [ ] `input` step submission
  - [ ] design
  - [ ] manual test
  - [ ] automated test
- [ ] `build` step submission
  - [ ] design
  - [ ] manual test
  - [ ] automated test
- [ ] tests using Declarative syntax
- [ ] tests using `build-token-root`

## Getting started

TODO

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
