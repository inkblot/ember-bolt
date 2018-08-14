#!/bin/bash
mvn clean verify
aws cloudformation package --template-file template.yaml --s3-bucket bootstrap.movealong.org --s3-prefix lambda/bolt --output-template target/deployed.yaml
aws cloudformation deploy --template-file target/deployed.yaml --stack-name ember-bolt --capabilities CAPABILITY_IAM
