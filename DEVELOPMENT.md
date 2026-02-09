# Development

## Release Checklist

* create a release branch called `release-v<version>` like `release-v0.1.1`
* rename every occurrence of the old version, say `2.2.0-SNAPSHOT` into the new version, say `2.2.0`
* rename every occurrence of old Docker images like `ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.0` 
into the new image, say `ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.1`
* update the CHANGELOG based on the milestone
* create a commit with the title `Release v<version>`
* create a PR from the release branch into main
* merge that PR
* create and push a tag called `v<version>` like `v0.1.1` on main at the merge commit
* change the version in the POM to the next SNAPSHOT version which usually increments the minor version
* create release notes on GitHub
