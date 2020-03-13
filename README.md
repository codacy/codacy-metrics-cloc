# Codacy Metrics cloc

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/11ebf162681e4d45beb0a975926ac34b)](https://www.codacy.com/gh/codacy/codacy-metrics-cloc?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-metrics-cloc&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/11ebf162681e4d45beb0a975926ac34b)](https://www.codacy.com/gh/codacy/codacy-metrics-cloc?utm_source=github.com&utm_medium=referral&utm_content=codacy/codacy-metrics-cloc&utm_campaign=Badge_Coverage)
[![CircleCI](https://circleci.com/gh/codacy/codacy-metrics-cloc.svg?style=svg)](https://circleci.com/gh/codacy/codacy-metrics-cloc)
[![Docker Version](https://images.microbadger.com/badges/version/codacy/codacy-metrics-cloc.svg)](https://microbadger.com/images/codacy/codacy-metrics-cloc "Get your own version badge on microbadger.com")

This is the docker engine we use at Codacy to have [cloc](hhttps://github.com/AlDanial/cloc/) support.

## Usage

You can create the docker by doing:

```bash
sbt docker:publishLocal
```

The docker is ran with the following command:

```bash
docker run -it -v $srcDir:/src  <DOCKER_NAME>:<DOCKER_VERSION>
docker run -it -v $PWD/src/test/resources:/src codacy/codacy-metrics-cloc:latest
```

## Test

Before running the tests, you need to install cloc:

```bash
npm install -g cloc
```

After that, you can run the tests:

```bash
sbt test
```

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
