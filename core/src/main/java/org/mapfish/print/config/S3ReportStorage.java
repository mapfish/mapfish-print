package org.mapfish.print.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Configuration for storing the reports in a S3 compatible storage.
 * <p>
 * By default, authentication is done using the {@link DefaultAWSCredentialsProviderChain} which uses
 * Environment Variables, Java System Properties, Credential profiles file, ...
 */
public class S3ReportStorage implements ReportStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3ReportStorage.class);
    private String bucket = null;
    private String prefix = "";
    private String accessKey = null;
    private String secretKey = null;
    private String region = null;
    private String endpointUrl = null;

    private AmazonS3 connect() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        if (accessKey != null) {
            final AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        }

        if (endpointUrl != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpointUrl, region));
        } else if (region != null) {
            builder.withRegion(region);
        }

        return builder.build();
    }

    @Override
    public URL save(
            final String ref, final String filename, final String extension, final String mimeType,
            final File file) {
        final PutObjectRequest request = createPutRequest(ref, filename, extension, mimeType, file);
        final AmazonS3 client = connect();
        client.putObject(request);
        final URL url = client.getUrl(bucket, request.getKey());
        LOGGER.info("Report stored on S3: {}", url);
        return url;
    }

    private PutObjectRequest createPutRequest(
            final String ref, final String filename, final String extension, final String mimeType,
            final File file) {
        final PutObjectRequest request =
                new PutObjectRequest(bucket, prefix + ref + "/" + filename + "." + extension, file);
        request.withCannedAcl(CannedAccessControlList.PublicRead);
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(mimeType);
        request.withMetadata(metadata);
        return request;
    }

    @Override
    public void validate(
            final List<Throwable> validationErrors, final Configuration configuration) {
        if (bucket == null) {
            validationErrors.add(new ConfigurationException("You must define a bucket"));
        }
        if (accessKey != null && secretKey == null) {
            validationErrors.add(new ConfigurationException(
                    "If you define the accessKey, you must define the secretKey"));
        }

        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(final String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(final String bucket) {
        this.bucket = bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    // TODO: doc on setters
}
