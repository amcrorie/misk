package misk.dynamodb

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.google.inject.Provides
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import misk.cloud.aws.AwsRegion
import misk.healthchecks.HealthCheck
import misk.inject.KAbstractModule

/**
 * Install this module to have access to an AmazonDynamoDB client. This can be
 * used to create a DynamoDbMapper for querying of a DynamoDb table.
 */
class RealDynamoDbModule constructor(
  private val clientConfig: ClientConfiguration = ClientConfiguration(),
  private val requiredTables: List<RequiredDynamoDbTable>
) : KAbstractModule() {
  /** @param requiredTableTypes a list of mapper classes annotated [DynamoDBTable]. */
  constructor(
    clientConfig: ClientConfiguration,
    vararg requiredTableTypes: KClass<*>
  ) : this(
    clientConfig,
    requiredTableTypes.map {
      val annotation = it.findAnnotation<DynamoDBTable>()
        ?: throw IllegalArgumentException("no @DynamoDBTable on $it")
      RequiredDynamoDbTable(annotation.tableName)
    }
  )

  override fun configure() {
    requireBinding<AWSCredentialsProvider>()
    requireBinding<AwsRegion>()
    multibind<HealthCheck>().to<DynamoDbHealthCheck>()
  }

  @Provides @Singleton
  fun provideRequiredTables(): List<RequiredDynamoDbTable> = requiredTables

  @Provides @Singleton
  fun providesAmazonDynamoDB(
    awsRegion: AwsRegion,
    awsCredentialsProvider: AWSCredentialsProvider
  ): AmazonDynamoDB {
    return AmazonDynamoDBClientBuilder
      .standard()
      .withRegion(awsRegion.name)
      .withCredentials(awsCredentialsProvider)
      .withClientConfiguration(clientConfig)
      .build()
  }

  @Provides @Singleton
  fun providesAmazonDynamoDBStreams(
    awsRegion: AwsRegion,
    awsCredentialsProvider: AWSCredentialsProvider
  ): AmazonDynamoDBStreams {
    return AmazonDynamoDBStreamsClientBuilder
      .standard()
      .withRegion(awsRegion.name)
      .withCredentials(awsCredentialsProvider)
      .withClientConfiguration(clientConfig)
      .build()
  }
}
