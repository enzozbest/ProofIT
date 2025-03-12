package kcl.seg.rtt.utils.aws

import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

object STSInteractor {
    suspend fun assumeRole(
        stsClient: StsClient,
        request: AssumeRoleRequest,
    ): Credentials {
        if (request.roleArn == null) {
            return Credentials("", "", "")
        }

        stsClient.use { sts ->
            val response = sts.assumeRole(request)
            return Credentials(
                response.credentials!!.accessKeyId,
                response.credentials!!.secretAccessKey,
                response.credentials!!.sessionToken,
            )
        }
    }
}
