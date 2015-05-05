Bixie - Inconsistent Code Detection for Java
=====
[![Build Status](https://travis-ci.org/martinschaef/bixie.png)](https://travis-ci.org/martinschaef/bixie)

For further information on what inconsistent code is and an online demo to play with, please see the [Bixie website](http://csl.sri.com/projects/bixie/).

Stable releases and experimental setups to repeat the experiments from our papers are in the [Release](https://github.com/martinschaef/bixie/releases) section. 

We recently changed our build system to [Gradle](https://gradle.org/).  After clonging the project, type:

    gradle check

to compile and run the unit tests. If you don't have gradle installed, used the gradle wrappers `gradlew` or `gradlew.bat`. If you haven't used gradle before, you should look run `gradle tasks` to see what you can do with it. E.g., `gradle eclipse` builds an Eclipse project for Bixie, and `gradle jacocoTestReport` generates a test coverage report.

Bixe uses [Jar2Bpl](https://github.com/martinschaef/jar2bpl) to turn Java into Boogie.
