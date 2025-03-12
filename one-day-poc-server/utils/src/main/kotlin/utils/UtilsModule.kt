package utils

import aws.sdk.kotlin.services.sts.StsClient
import io.ktor.server.application.Application
import utils.aws.S3Manager
import utils.aws.S3Service
import utils.environment.EnvironmentLoader
import utils.json.PoCJSON

fun Application.configureUtils() {
    val awsRegion = EnvironmentLoader.get("AWS_REGION")
    val s3Manager =
        S3Manager(PoCJSON.readJsonFile("utils/src/main/resources/s3_config.json"), StsClient { region = awsRegion })
    S3Service.init(s3Manager)
}
