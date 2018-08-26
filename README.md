# ember-bolt
An AWS Lambda function for ember lightning deployments

Ember Bolt is an implementation of the server side component of the [ember lightning|http://ember-cli-deploy.com/docs/v0.6.x/the-lightning-strategy/] strategy for AWS Lambda. The Ember Bolt maven artifact contains just the `org.movealong.bolt.Bolt` class which is the Lambda handler. The artifact is meant to be included into a maven project as a dependency in order to be deployed to your preconfigured AWS Lambda function.
