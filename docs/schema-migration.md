# 스키마 변경 안내 (프론트 계약 맞추기)

`feat/fe-contract`에서 추가·변경된 스키마를 정리한다.
로컬/테스트는 `ddl-auto`가 스키마를 새로 만들어 주지만, **이미 데이터가 있는 dev·운영 DB는
아래 ALTER가 필요하다.** dev는 `ddl-auto=update`라 컬럼과 테이블은 자동으로 추가되지만
**기존 컬럼의 타입 변경은 하지 않는다.**

## 1. campaign.category — ENUM → VARCHAR (필수)

카테고리를 프론트의 9종과 1:1로 맞추면서 값이 늘었다. 기존 컬럼이 MySQL `ENUM`이라
새 값(`PLANNING_DESIGN` 등)을 넣으면 실패한다.

```sql
ALTER TABLE campaign
  MODIFY COLUMN category VARCHAR(30) NOT NULL;
```

기존에 `SCIENCE`로 저장된 행은 그대로 읽힌다(enum에 `@Deprecated`로 남겨 둠).
정리하려면 아래를 실행한다. 예전 코드가 `SCIENCE`를 '예술/디자인'으로 보여주고 있었으므로
같은 의미로 옮긴다.

```sql
UPDATE campaign SET category = 'ART_DESIGN' WHERE category = 'SCIENCE';
```

## 2. campaign.apply_url — NOT NULL 완화 (필수)

수집된 공고는 신청 링크를 못 찾는 경우가 있어 nullable로 바꿨다.
링크가 없으면 프론트가 신청 버튼을 비활성화한다.

```sql
ALTER TABLE campaign
  MODIFY COLUMN apply_url VARCHAR(500) NULL;
```

## 3. campaign 신규 컬럼 (ddl-auto=update가 자동 추가)

`capacity`, `book_format`, `review_due_date`, `requirements`,
`source`, `source_url`, `collected_at`, `publish_status`, `dedupe_key`

`source`와 `publish_status`는 `NOT NULL`이라 기존 행에 기본값이 필요하다.
자동 추가에 맡기면 기본값 없이 컬럼이 생겨 실패할 수 있으므로, 데이터가 있는 DB에서는
아래처럼 먼저 채운다.

```sql
ALTER TABLE campaign
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  ADD COLUMN publish_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  ADD COLUMN source_url VARCHAR(500) NULL,
  ADD COLUMN collected_at DATETIME NULL,
  ADD COLUMN capacity INT NULL,
  ADD COLUMN book_format VARCHAR(20) NULL,
  ADD COLUMN review_due_date DATE NULL,
  ADD COLUMN requirements TEXT NULL,
  ADD COLUMN dedupe_key VARCHAR(500) NULL;

CREATE INDEX idx_campaign_publish_status ON campaign (publish_status);
CREATE INDEX idx_campaign_publisher_name ON campaign (publisher_name);
```

> `dedupe_key`는 엔티티를 저장할 때 채워진다. 기존 행은 비어 있으므로 한 번 갱신하거나,
> 중복 검사에서 빠지는 것을 감수한다.

## 4. 신규 테이블 (ddl-auto=update가 자동 생성)

| 테이블 | 용도 |
| --- | --- |
| `campaign_review_channel` | 공고별 서평 의무 채널 (`@ElementCollection`) |
| `review` | 참여자가 제출한 서평 |
| `notification` | 사용자별 알림 큐 |
| `application` | 사용자가 표시한 신청 기록 (자기 신고) |

`review`에는 `uk_review_campaign_member`, `application`에는
`uk_application_member_campaign` 유니크 제약이 있다(한 공고에 각각 1건).

## 5. 업로드 디렉터리

`app.upload.directory`(기본 `uploads`)에 이미지를 쓰고 `/uploads/**`로 서빙한다.
**컨테이너에 배포하면 재시작 시 사라진다.** 볼륨을 붙이거나,
`ImageStorage`를 오브젝트 스토리지 구현으로 교체해야 한다.

```yaml
app:
  upload:
    directory: ${UPLOAD.DIRECTORY:uploads}
```
