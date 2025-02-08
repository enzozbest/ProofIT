package kcl.seg.rtt.utils.core

import aws.sdk.kotlin.services.sts.StsClient
import io.ktor.server.application.*
import kcl.seg.rtt.utils.aws.S3Manager
import kcl.seg.rtt.utils.aws.S3Service
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import kcl.seg.rtt.utils.json.PoCJSON

fun Application.configureUtils() {
    val awsRegion = EnvironmentLoader.get("AWS_REGION")
    val s3Manager =
        S3Manager(PoCJSON.readJsonFile("utils/src/main/resources/s3_config.json"), StsClient { region = awsRegion })
    S3Service.init(s3Manager)
}