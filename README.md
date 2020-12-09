# alt-file-parameter-plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/alt-file-parameter-plugin-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/alt-file-parameter-plugin-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/alt-file-parameter-plugin-plugin.svg)](https://github.com/jenkinsci/alt-file-parameter-plugin-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/alt-file-parameter-plugin.svg)](https://plugins.jenkins.io/alt-file-parameter-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/alt-file-parameter-plugin-plugin.svg?label=changelog)](https://github.com/jenkinsci/alt-file-parameter-plugin-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/alt-file-parameter-plugin.svg?color=blue)](https://plugins.jenkins.io/alt-file-parameter-plugin)

## Introduction

Offers alternative types of file parameter that are compatible with Pipeline and do not suffer from the architectural flaws of the type built into Jenkins core.

See [JENKINS-27413](https://issues.jenkins-ci.org/browse/JENKINS-27413) and [JENKINS-29289](https://issues.jenkins-ci.org/browse/JENKINS-29289) for background.

## Implementation status

- [X] Base64 file parameter (simple and suitable for small files)
  - [X] implementation
  - [X] manual test
  - [X] automated test
- [ ] stashed file parameter (suitable for larger files used in Pipeline)
  - [X] implementation
  - [X] manual test
  - [ ] automated test
- [ ] archived file parameter (compatible with freestyle, and suitable if you want to ensure parameters are kept after the build ends)
  - [ ] implementation
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
- [ ] `withFileParameter` wrapper step
  - [ ] implementation
  - [ ] manual test
  - [ ] automated test
- [ ] `input` step submission
  - [ ] design
  - [ ] manual test
  - [ ] automated test
- [ ] `build` step submission
  - [ ] design
  - [ ] manual test
  - [ ] automated test
- [ ] inline help text

## Getting started

TODO

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
