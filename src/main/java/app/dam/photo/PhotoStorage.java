package app.dam.photo;

import app.dam.config.DamProperties;
import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 사진은 오브젝트 스토리지에 올라간다. 서버는 올릴 자리에 서명만 해주고 바이트는
 * 만지지 않는다. 오라클 Object Storage 가 S3 호환 API 를 줘서 엔드포인트만 바꿔
 * 끼우면 나중에 S3 나 R2 로 옮겨도 이 클래스가 그대로 간다.
 */
@Component
public class PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorage.class);

    private final DamProperties.Storage storage;

    /**
     * 자격증명이 아직 없어도 서버는 떠야 한다. 여기서 클라이언트를 미리 만들면
     * 스토리지 설정 하나 때문에 나머지 API 까지 손도 못 대게 된다.
     */
    private volatile S3Presigner presigner;
    private volatile S3Client client;

    PhotoStorage(DamProperties properties) {
        this.storage = properties.storage();
    }

    public String uploadUrl(String key, String contentType) {
        var put = PutObjectRequest.builder()
                .bucket(storage.bucket())
                .key(key)
                .contentType(contentType)
                .build();
        return presigner().presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(storage.uploadTtl())
                .putObjectRequest(put)
                .build()).url().toString();
    }

    /** 매번 새로 서명한다. 유효기간이 짧아야 주소가 새어도 오래 안 간다. */
    public String readUrl(String key) {
        var get = GetObjectRequest.builder().bucket(storage.bucket()).key(key).build();
        return presigner().presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(storage.readTtl())
                .getObjectRequest(get)
                .build()).url().toString();
    }

    public void delete(String key) {
        try {
            client().deleteObject(DeleteObjectRequest.builder()
                    .bucket(storage.bucket()).key(key).build());
        } catch (RuntimeException e) {
            // 여기서 예외를 올리면 탈퇴나 기록 삭제가 스토리지 사정으로 막힌다.
            log.warn("사진을 지우지 못했다: {}", key, e);
        }
    }

    private S3Presigner presigner() {
        S3Presigner built = presigner;
        if (built == null) {
            synchronized (this) {
                if (presigner == null) {
                    presigner = S3Presigner.builder()
                            .credentialsProvider(credentials())
                            .region(Region.of(storage.region()))
                            .endpointOverride(endpoint())
                            .serviceConfiguration(pathStyle())
                            .build();
                }
                built = presigner;
            }
        }
        return built;
    }

    private S3Client client() {
        S3Client built = client;
        if (built == null) {
            synchronized (this) {
                if (client == null) {
                    client = S3Client.builder()
                            .credentialsProvider(credentials())
                            .region(Region.of(storage.region()))
                            .endpointOverride(endpoint())
                            .serviceConfiguration(pathStyle())
                            .build();
                }
                built = client;
            }
        }
        return built;
    }

    private StaticCredentialsProvider credentials() {
        requireConfigured();
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.accessKey(), storage.secretKey()));
    }

    private URI endpoint() {
        requireConfigured();
        return URI.create(storage.endpoint());
    }

    /** 오라클은 가상 호스트 방식 주소를 안 쓴다. 경로 방식이어야 버킷을 찾는다. */
    private static S3Configuration pathStyle() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    private void requireConfigured() {
        if (blank(storage.endpoint()) || blank(storage.accessKey()) || blank(storage.secretKey())) {
            throw ApiException.of(ErrorCode.STORAGE_NOT_CONFIGURED);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
