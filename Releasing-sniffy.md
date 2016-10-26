## sniffy-ui

1. `git hf release start <VERSION>`
1. Remove SNAPSHOT from version in `package.json` and `pom.xml`
1. `grunt`
1. `git commit -am 'version bumped to <VERSION>'`
1. `git push`
1. Wait for successful [Travis build](https://travis-ci.org/sniffy/sniffy-ui)
1. `mvn clean install`
1. `git hf release finish <VERSION>`
1. `mvn clean install deploy`
1. Go to [Sonatype OSS Nexus](https://oss.sonatype.org/) close and release the iosniffy repository
1. Bump version in `package.json` and `pom.xml`
1. `grunt`
1. `git commit -am 'Version bumped to <VERSION+1>-SNAPSHOT'`
1. `git push`

## sniffy

1. `git hf release start <VERSION>`
1. Update sniffy UI version in `pom.xml`
1. Remove SNAPSHOT from version in `pom.xml` of **ALL** modules and in `SnifferServlet.java`
1. `mvn clean test package javadoc:javadoc`
1. `git commit -am 'version bumped to <VERSION>'`
1. `git push`
1. Wait for successful [Travis build](https://travis-ci.org/sniffy/sniffy)
1. `git hf release finish <VERSION>`
1. `mvn clean deploy -Psonatype-oss-release` - if it doesn't work make a release manually as described [here](http://central.sonatype.org/pages/manual-staging-bundle-creation-and-deployment.html#bundle-creation)
1. Bump version in `pom.xml` of **ALL** modules
1. `git commit -am 'Version bumped to <VERSION+1>-SNAPSHOT'`
1. `git push`
 
