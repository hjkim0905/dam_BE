# dam_BE

담(dam) 서버. 하루 한 장의 사진에서 뽑은 색을 기록하고, 둘이 한 방에서 나란히 채워간다.

설계 문서는 노션의 `[BE] Spring Boot 서버 구축` 아래에 있다. DB 스키마, ERD, API 명세서.

## 돌리기

MySQL 에 스키마를 하나 만든다. 테이블은 Flyway 가 만든다.

```sh
mysql -u root -p -e "CREATE DATABASE dam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

접속 정보를 넣고 띄운다.

```sh
cp .env.example .env
DB_PASSWORD=... ./gradlew bootRun
```

```sh
./gradlew test
```

## 로컬에서 확인하기

로그인은 애플 하나뿐이라 앱에서 받은 identityToken 이 있어야 그 뒤가 돈다.

```sh
API=http://localhost:8080/api/v1

# 로그인
curl -s -X POST $API/auth/apple -H 'Content-Type: application/json' \
  -d '{"identityToken":"...","authorizationCode":"...","fullName":"지호"}'

# 온보딩
curl -s -X POST $API/me/onboarding -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"지호","termsAgreed":true,"privacyAgreed":true}'

# 사진 자리 받고, 그 자리에 직접 올리고, 기록으로 저장
curl -s -X POST $API/photos/upload-url -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"contentType":"image/jpeg","contentLength":48213}'
curl -s -X PUT "$UPLOAD_URL" --data-binary @photo.jpg
curl -s -X POST $API/entries -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"date":"2026-09-03","color":"#8A9A7B","photoKey":"'"$KEY"'","memo":"안개가 걷히고 있었다"}'
```

## 알아둘 것

- **로그인은 애플 하나뿐이다.** 개발용 우회 로그인을 두지 않는다. 문이 하나면 그 문만 잠그면 된다.
- **사진은 오브젝트 스토리지로 간다.** 서버는 올릴 자리에 서명만 하고 바이트는 안 만진다. 그래서 어느 기기로 로그인해도 같은 사진이 보인다.
- **스토리지 설정이 비어 있어도 서버는 뜬다.** 사진 관련 호출만 준비되지 않았다고 답하고 나머지는 그대로 돈다.
- **스키마는 손으로 고치지 않는다.** `ddl-auto` 가 validate 라 엔티티와 테이블이 어긋나면 뜨지 않는다. 바꿀 게 있으면 `db/migration` 에 다음 번호 파일을 더한다.
- **회원탈퇴는 행을 진짜로 지운다.** `apple_sub` 이 UNIQUE 라 탈퇴한 행을 남겨두면 같은 애플 계정으로 다시 가입할 수 없다.
